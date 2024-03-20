/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.lrgsmain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.net.InetAddress;

import opendcs.dai.LoadingAppDAI;
import ilex.util.Logger;
import ilex.util.FileServerLock;
import ilex.util.ServerLockable;
import ilex.util.NoOpServerLock;
import ilex.util.EnvExpander;
import ilex.util.ServerLock;
import ilex.util.FileLogger;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.QueueLogger;
import ilex.util.TextUtil;
import ilex.jni.SignalHandler;
import ilex.jni.SignalTrapper;
import lrgs.archive.MsgArchive;
import lrgs.archive.InvalidArchiveException;
import lrgs.ddsserver.DdsServer;
import lrgs.ddsrecv.DdsRecv;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.gui.LrgsApp;
import lrgs.gui.DecodesInterface;
import lrgs.iridiumsbd.IridiumSbdInterface;
import lrgs.ldds.PasswordChecker;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.lrit.LritDamsNtReceiver;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import lrgs.dqm.DapsDqmInterface;
import lrgs.db.LrgsDatabaseThread;
import lrgs.edl.EdlInputInterface;
import lrgs.noaaportrecv.NoaaportRecv;
import lrgs.networkdcp.NetworkDcpRecv;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.routing.DacqEventLogger;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

/**
Main class for LRGS process.
*/
public class LrgsMain
    implements Runnable, ServerLockable, SignalHandler, ProcWaiterCallback
{
    private static final Logger logger = Logger.instance();
    public static final String module = "LrgsMain";

    public static final int EVT_TIMEOUT = 1;
    public static final int EVT_CONFIG_CHANGE = 2;
    public static final int EVT_INPUT_INIT = 3;
    public static final int EVT_DDS_INIT = 4;
    public static final int EVT_BAD_CONFIG = 5;
    public static final int EVT_LOCK_BUSY = 6;
    public static final int EVT_ARC_INIT = 7;

    /** The Archiver */
    public MsgArchive msgArchive;

    /** The lock to prevent multiple instances */
    protected ServerLock myServerLock;

    /** Causes application to exit. */
    protected boolean shutdownFlag;

    /** Vector of input interfaces. */
    private LrgsInputInterface lrgsInputs[];

    /** The DDS Server */
    private DdsServer ddsServer;

    /** Used by DDS to distributed status to clients. */
    protected JavaLrgsStatusProvider statusProvider;

    /** The DDS Receive Module, used for backup. */
    private DdsRecv ddsRecv;

    /** Handles reception of data from DRGS DAMS-NT interfaces. */
    private DrgsRecv drgsRecv;

    /** Used to write period status snapshots. */
    private DetailReportGenerator statusRptGen;


    /** The singleton configuration object. */
    private LrgsConfig cfg;

    /** The thread handling alarms & actions. */
    private AlarmHandler alarmHandler;

    /** The interface to the LRGS database */
    private LrgsDatabaseThread dbThread;

    /** flag used to wait for startup command. */
    private boolean onStartupCmdFinished = false;

    /** The noaaport receive module */
    NoaaportRecv noaaportRecv;

    /** The network DCP receive module */
    private NetworkDcpRecv networkDcpRecv;

    /** The DDS Receive Module, used for secondary group. */
    private DdsRecv ddsRecv2;

    /** To use is writeDacqEvents = true in configuration */
    private DacqEventLogger dacqEventLogger = null;

    private final String lockFileName;
    private final String configFileName;
    private final QueueLogger queueLogger;
    private final FileLogger fileLogger;

    /** Constructor. */
    public LrgsMain(QueueLogger qLogger, String lockFileName, String configFileName, FileLogger fileLogger)
    {
        msgArchive = null;
        myServerLock = null;
        shutdownFlag = false;
        lrgsInputs = null;
        ddsServer = null;
        statusProvider = null;
        ddsRecv = null;
        drgsRecv = null;
        statusRptGen = new DetailReportGenerator("icons/satdish.jpg");
        alarmHandler = new AlarmHandler(qLogger);
        dbThread = null;
        noaaportRecv = null;
        ddsRecv2=null;
        this.lockFileName = lockFileName;
        this.configFileName = configFileName;
        this.queueLogger = qLogger;
        this.fileLogger = fileLogger;
    }

    public void run()
    {
        try
        {
            ResourceFactory.instance().initDbResources();
        }
        catch (DatabaseException e)
        {
            e.printStackTrace();
        }

        shutdownFlag = false;
        Logger.instance().info("============ " + getAppName()
            + " Starting ============");

        // Establish a server lock file & start the server lock monitor

        String lockName = EnvExpander.expand(lockFileName);
        Logger.instance().info("Lock File =" + lockName);
        if (lockName.equals("-"))
        {        
            myServerLock = new NoOpServerLock();
        }
        else
        {
            myServerLock = new FileServerLock(lockName);
            if (!myServerLock.obtainLock(this))
            {
                Logger.instance().fatal(module + ":" + EVT_LOCK_BUSY
                    + "- Lock file '" + lockName + "' already taken. "
                    + "Is another instance of '" + LrgsCmdLineArgs.progname
                    + "' already running?");
                System.exit(1);
            }
        }

        // Do all of the initialization & exit on fatal error.
        if (!initLRGS())
        {
            Logger.instance().fatal("============ " + getAppName()
                + " INIT FAILED -- Exiting. ============");
            myServerLock.releaseLock();
            System.exit(0);
        }

        long lastStatusSnapshot = 0L;

        // New for LRGS 6.0: On Linux systems, trap SIGHUP and rotate logs.
        if (System.getProperty("os.name").toLowerCase().startsWith("linux"))
        {
            Logger.instance().info("Trapping SIGHUP");
            try { SignalTrapper.setSignalHandler(SIGHUP, this); }
            catch(Throwable ex)
            {
                Logger.instance().warning("Could not trap SIGHUP: " + ex);
            }
        }

        // main loop
        statusProvider.setSystemStatus("Running");
        long now = System.currentTimeMillis();
        int lastDay = 0;
        while(!shutdownFlag)
        {
            cfg.checkConfig();
            statusProvider.updateStatusSnapshot();
            now = System.currentTimeMillis();
            int day = (int)(now / (3600L * 24 * 1000));
            if (lastDay != day)
            {
                msgArchive.doCheckCurrentArchive();
                lastDay = day;
            }
            if (now - lastStatusSnapshot > cfg.htmlStatusSeconds * 1000L)
            {
                lastStatusSnapshot = now;
                writeStatusSnapshot();
            }

            try { Thread.sleep(1000L); }
            catch(InterruptedException ex) {}
        }

        ddsRecv.shutdown();
        ddsRecv2.shutdown();
        if (ddsServer != null)
            ddsServer.shutdown();

        // Shut down all of the input interfaces.
        for(int i=0; i<lrgsInputs.length; i++)
        {
            LrgsInputInterface lii = lrgsInputs[i];
            if (lii != null)
                lii.shutdownLrgsInput();
        }

        alarmHandler.shutdown();

        dbThread.shutdown();

        // Allow threads 5 seconds to clean up, then issue final status report.
        try { Thread.sleep(5000L); }
        catch(InterruptedException ex) {}
        statusProvider.updateStatusSnapshot();
        statusProvider.setSystemStatus("DEAD");
        writeStatusSnapshot();
        statusProvider.detach();

        myServerLock.releaseLock();
        Logger.instance().info("============ " + LrgsCmdLineArgs.progname
            + " Exiting ============");
    }

    /**
     * Performa all LRGS Initialization tasks.
     * @return true if OK to proceed, false on fatal error.
     */
    private boolean initLRGS()
    {
        cfg = LrgsConfig.instance();

        // Load configuration file.
        String cfgName = EnvExpander.expand(configFileName);
        cfg.setConfigFileName(cfgName);
        try { cfg.loadConfig();}
        catch(IOException ex)
        {
            String msg = module + ":" + EVT_BAD_CONFIG +
                "- Cannot read config file '" + cfgName + "': " + ex;
            Logger.instance().fatal(msg);
            System.err.println(msg);
            return false;
        }
        Logger.instance().info("Config getDoPdtValidation=" + cfg.getDoPdtValidation());

        // If a startup command is specified in properties, run it.
        String onStartupCmd = cfg.getMiscProp("onStartupCmd");
        if (onStartupCmd != null)
        {
            onStartupCmd = onStartupCmd.trim();
            if (onStartupCmd.length() > 0)
                runOnStartupCmd(onStartupCmd);
        }

        lrgsInputs = new LrgsInputInterface[cfg.maxDownlinks];

        if (cfg.getLoadDecodes())
        {
            try
            {
                // MJM 6/13/2019 LRGS should try to initialize from user.properties first.
                String userPath = EnvExpander.expand("$DCSTOOL_USERDIR/user.properties");
                String homePath = EnvExpander.expand("$DCSTOOL_HOME/decodes.properties");
                File propFile = new File(userPath);
                if (!propFile.canRead())
                    propFile = new File(homePath);
                if (!propFile.canRead())
                {
                    Logger.instance().failure("loadDecodes=true, but neither '" + userPath + "' nor '" + homePath
                        + "' is readable. Proceeding with default DECODES settings.");
                }
                else
                {
                    //Load the decodes.properties
                    DecodesSettings settings = DecodesSettings.instance();
                    if (!settings.isLoaded())
                    {
                        Logger.instance().info("Loading DECODES settings from '" + propFile.getPath() + "'");
                        Properties props = new Properties();
                        try
                        {
                            FileInputStream fis = new FileInputStream(propFile);
                            props.load(fis);
                            fis.close();
                        }
                        catch(Exception e)
                        {
                            Logger.instance().log(Logger.E_FAILURE,
                                "Cannot open DECODES Properties File '"+propFile.getPath()+"': "+e);
                        }
                        settings.loadFromProperties(props);
                    }
                }

                DecodesInterface.silent = true;
                DecodesInterface.initDecodes(propFile.getPath());
                // MJM 9/25/2008 - In order for DDS Receive to be able to use
                // network lists <all> and <production>, we have to load the
                // platform lists too:
                DecodesInterface.initializeForDecoding();

                if (cfg.getMiscBooleanProperty("writeDacqEvents", false)
                 && (Database.getDb().getDbIo() instanceof SqlDatabaseIO))
                {
                    // Create the DacqEvent Logger and make it the primary logger.
                    SqlDatabaseIO sqlDbio = (SqlDatabaseIO)Database.getDb().getDbIo();
                    dacqEventLogger = new DacqEventLogger(Logger.instance());

                    LoadingAppDAI appDAO = sqlDbio.makeLoadingAppDAO();
                    try
                    {
                        CompAppInfo appInfo = appDAO.getComputationApp("LRGS");
                        if (appInfo != null)
                            dacqEventLogger.setAppId(appInfo.getAppId());
                    }
                    catch (DbIoException ex)
                    {
                        Logger.instance().warning(module + " Cannot read application ID: " + ex);
                    }
                    catch (NoSuchObjectException ex)
                    {
                        Logger.instance().warning(module + " No such application 'LRGS': " + ex
                            + " -- Please create in Processes GUI.");
                    }
                    finally
                    {
                        appDAO.close();
                    }

                    Logger.setLogger(dacqEventLogger);
                }

            }
            catch(DecodesException ex)
            {
                Logger.instance().info(
                    LrgsCmdLineArgs.progname
                    + " Cannot initialize DECODES DB -- assuming not installed ("
                    + ex + ")");
                decodes.db.Database.setDb(null);
            }
        }

        // Create a tmp dir for various files if it doesn't already exist.
        File lrgsTmp = new File(EnvExpander.expand("$LRGSHOME/tmp"));
        if (!lrgsTmp.isDirectory())
            lrgsTmp.mkdirs();

        // Initialize the LRGS Database Interface
        dbThread = LrgsDatabaseThread.instance();
        dbThread.start();

        alarmHandler.start();

        // Initialize the message archive.
        try { initArchive(cfg); }
        catch(InvalidArchiveException ex)
        {
            String msg = module + ":" + EVT_ARC_INIT
                + "- Cannot initialize Archive: " + ex;
            Logger.instance().fatal(msg);
            System.err.println(msg);
            return false;
        }

        statusProvider = new JavaLrgsStatusProvider(this);
        statusProvider.setSystemStatus("Initializing");
        msgArchive.setStatusProvider(statusProvider);
        if (statusProvider.qualLogLastModified != 0L)
        {
            // Give DB thread a chance to get connection established.
            try { Thread.sleep(2000L); }
            catch(InterruptedException ex) {}
            // Terminate any connections left open from last run.
            dbThread.terminateConnectionsBefore(
                statusProvider.qualLogLastModified);
        }

        // Initialize the DDS server
        if (!initDdsServer())
            return false;

        // Add any custom features and extensions for this particular LRGS.
        addCustomFeatures();

        // If enabled, create the NOAAPORT Receive module
        if (cfg.getMiscBooleanProperty("noaaport.enable", false)
         || cfg.noaaportEnabled)
        {
            Logger.instance().info("Constructing NOAAPORT Receive Module.");
            noaaportRecv = new NoaaportRecv(this, msgArchive);
            addInput(noaaportRecv);
        }
        else Logger.instance().debug1("NOAAPORT _not_ enabled.");

        // If enabled, create the Network DCP Receive Module
        if (cfg.networkDcpEnable)
        {
            Logger.instance().info("Constructing Network DCP Receive Module.");
            networkDcpRecv = new NetworkDcpRecv(this, msgArchive);
            addInput(networkDcpRecv);
            statusProvider.getStatusSnapshot().networkDcpStatusList
                = networkDcpRecv.getStatusList();
        }

        if (cfg.enableLritRecv)
        {
            Logger.instance().info("Enabling DAMS-NT HRIT Receiver");
            LritDamsNtReceiver ldnr = new LritDamsNtReceiver(msgArchive, this);
            ldnr.configure(null);
            addInput(ldnr);
        }
        else
            Logger.instance().info("LRIT is not enabled.");

        if (cfg.hritFileEnabled)
        {
            Logger.instance().info("Enabling HRIT-File Receiver");
            HritFileInterface hfi = new HritFileInterface(this, msgArchive);
            addInput(hfi);
        }

        if (cfg.iridiumEnabled)
        {
            Logger.instance().info("Enabling Iridium Receive Module.");
            addInput(new IridiumSbdInterface(this, msgArchive));
        }

        // Initialize all of the input interfaces.
        for(int i=0; i < lrgsInputs.length; i++)
        {
            LrgsInputInterface lii = lrgsInputs[i];
            if (lii == null)
                continue;
            try
            {
                lii.initLrgsInput();
                lii.enableLrgsInput(true);
            }
            catch(LrgsInputException lie)
            {
                Logger.instance().failure(module + ":" + EVT_INPUT_INIT
                 + "- Cannot initialize input interface '" + lii.getInputName()
                 + "': " + lie.toString());
            }
        }

        // Initialize & Start the DDS Receiver Module
        ddsRecv = new DdsRecv(this, msgArchive);
        addInput(ddsRecv);

        // Initialize & Start the DDS Receiver Module for secondary group
        ddsRecv2 = new DdsRecv(this, msgArchive);

        ddsRecv2.setSecondary(true);
        addInput(ddsRecv2);

        // Initialize the DRGS Receive Module.
        drgsRecv = new DrgsRecv(this, msgArchive);
        addInput(drgsRecv);

        // EDL Input Interface watches hot directories for EDL files.
        if (cfg.edlIngestEnable)
        {
            EdlInputInterface edlII = new EdlInputInterface(this);
            try
            {
                edlII.initLrgsInput();
                addInput(edlII);
            }
            catch (LrgsInputException ex)
            {
                Logger.instance().failure("Cannot start EdlInputInterface: " + ex);
            }
        }

        // We have to initialize the quality log so that the DDS receiver
        // picks up where it left off.
        statusProvider.initQualityLog();

        // If I'm recovering outages, set 'since' time to the last time I got
        // a message on _any_ downlink.
        // MJM OpenDCS 6.2 does not support Outage Recovery
//        if (cfg.recoverOutages)
//        {
//            ddsRecv.setLastMsgRecvTime(statusProvider.getLastReceiveTime());
//            ddsRecv2.setLastMsgRecvTime(statusProvider.getLastReceiveTime()); // for secondary group
//        }
//        else // Otherwise, use last time DDS returned a message
//        {
            ddsRecv.setLastMsgRecvTime(statusProvider.getLastDdsReceiveTime());
            ddsRecv2.setLastMsgRecvTime(statusProvider.getLastSecDdsReceiveTime()); // for secondary group
//        }



        ddsRecv.start();
        ddsRecv2.start();
        drgsRecv.start();

        // Open DDS for business.
        String pcc = cfg.getMiscProp("passwordCheckerClass");
        if (pcc != null && pcc.trim().length() > 0)
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try
            {
                Class<?> pccClass = cl.loadClass(pcc);
                cfg.setPasswordChecker((PasswordChecker)pccClass.newInstance());
            }
            catch (Exception ex)
            {
                Logger.instance().fatal("Cannot load password checker class '" + pcc + "': " + ex);
                return false;
            }

        }
        ddsServer.setEnabled(true);

        if (cfg.enableDapsDqm)
        {
            DapsDqmInterface ddi = new DapsDqmInterface(statusProvider);
            statusProvider.setDqmInterface(ddi);
            ddi.start();
        }

        // Config allows addition of additional input interfaces as follows:
        // LrgsInput.<name>.class=<fully qualified class name>
        // LrgsInput.<name>.enabled=[true|false]
        // LrgsInput.<name>.<paramname>=<value>
        // The following code instantiates the classes, adds them to the
        // list of interfaces, and then sets the enable/disable and other parms.
        // Note that each input interface must have a unique name and must not
        // clash with any of the standard interface type-names defined in
        // LrgsInputInterface.java

        final HashMap<String, Properties> loadableInterfaces = new HashMap<>();
        cfg.getOtherProps()
           .forEach((key,val) -> {
                if (((String)key).startsWith("LrgsInput."))
                {
                    final String keyParts[] = ((String)key).split("\\.");
                    final String name = keyParts[1];
                    final String var = keyParts[2]; // NOTE: consider possible extra hierarchy
                    if (!loadableInterfaces.containsKey(name))
                    {
                        loadableInterfaces.put(name,new Properties());
                    }
                    final Properties props = loadableInterfaces.get(name);
                    props.put(var,val);
                }
            });

        loadableInterfaces.forEach((loadableName,props)-> {
            try
            {
                Class<?> inputClass = Class.forName(props.getProperty("class"));
                LoadableLrgsInputInterface input =
                    (LoadableLrgsInputInterface)inputClass.newInstance();
                input.setInterfaceName(loadableName);
                input.setMsgArchive(msgArchive);
                input.setLrgsMain(this);
                addInput(input);

                props.entrySet()
                        .stream()
                        .filter((k_v)  -> !(((String)k_v.getKey()).equalsIgnoreCase("enable")
                                            ||
                                            ((String)k_v.getKey()).equalsIgnoreCase("enabled")
                        ))
                        .forEach((k_v) -> {
                            input.setConfigParam((String)k_v.getKey(),(String)k_v.getValue());
                        }
                );
                input.initLrgsInput();
                input.enableLrgsInput(Boolean.parseBoolean(props.getProperty("enable",
                                                           props.getProperty("enabled",
                                                           "false"))));
            }
            catch (IllegalAccessException | InstantiationException | ClassNotFoundException | ClassCastException ex)
            {
                String msg = "Unable to create Loadable Input %s, because %s";
                logger.warning(String.format(msg,loadableName,ex.getLocalizedMessage()));
            }
            catch (LrgsInputException ex)
            {
                String msg = "Unable to configure Loadable Input %s, because %s";
                logger.warning(String.format(msg,loadableName,ex.getLocalizedMessage()));
            }
        });

        return true;
    }

    /**
     * Initializes the day-file message archiving code.
     * @param cfg the LRGS Configuration Object.
     */
    private void initArchive(LrgsConfig cfg)
        throws InvalidArchiveException
    {
        msgArchive = new MsgArchive(cfg.archiveDir);
        msgArchive.setPeriodParams(cfg.numDayFiles);
        msgArchive.init();
    }

    /**
     * Initialize the DDS Server.
     */
    private boolean initDdsServer()
    {
        LrgsConfig cfg = LrgsConfig.instance();
        if (cfg.ddsBindAddr != null && cfg.ddsBindAddr.trim().length() == 0)
            cfg.ddsBindAddr = null;

        try
        {
            InetAddress ia =
                (cfg.ddsBindAddr != null && cfg.ddsBindAddr.length() > 0) ?
                InetAddress.getByName(cfg.ddsBindAddr) : null;

            ddsServer = new DdsServer(cfg.ddsListenPort, ia, msgArchive,
                    queueLogger, statusProvider);
            ddsServer.init();
        }
        catch(Exception ex)
        {
            String msg = module + ":" + EVT_DDS_INIT
                + "- Cannot start DDS Server: " + ex;
            Logger.instance().fatal(msg);
            System.err.println(msg);
            ex.printStackTrace(System.err);
            return false;
        }
        return true;
    }


    public void shutdown()
    {
        shutdownFlag = true;
    }

    /** Called from ServerLockable when the lock file is removed. */
    public void lockFileRemoved()
    {
        Logger.instance().info(
            LrgsCmdLineArgs.progname + " Exiting -- Lock File Removed.");
        shutdown();
    }

    public static void main(String args[])
        throws IOException
    {
        final LrgsCmdLineArgs cmdLineArgs = new LrgsCmdLineArgs();
        cmdLineArgs.parseArgs(args);

        /**
         * Using lock files as an IPC mechanism (for status GUI) is unreliable in windoze.
         * Tell server lock never to exit as a result of lock file I/O error.
         */
        if (cmdLineArgs.windowsSvcArg.getValue())
            FileServerLock.setWindowsService(true);

        Logger.instance().setTimeZone(TimeZone.getTimeZone("UTC"));
        final LrgsMain lm = new LrgsMain(cmdLineArgs.qLogger, cmdLineArgs.getLockFile(),
                                         cmdLineArgs.getConfigFile(), cmdLineArgs.fLogger);
        if (cmdLineArgs.runInForGround() )
        {
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook( new Thread() {
                @Override
                public void run()
                {
                    try
                    {
                        Logger.instance().debug3("SIGTERM Caught, Setting shutdown flag to true.");
                        lm.shutdown();
                        mainThread.join(1000);
                    }
                    catch (InterruptedException ex)
                    {
                        System.err.println(ex);
                    }
                }
            });
        }

        lm.run();
    }

    /**
     * Extensions of the LrgsMain class may be implemented that overload this
     * method to add custom features such as additional input interfaces.
     * The base class method defined here does nothing.
     */
    protected void addCustomFeatures()
    {
        Logger.instance().debug1("Default 'addCustomFeatures' doing nothing.");
    }

    /**
     * @return the name of this application.
     */
    public String getAppName()
    {
        return LrgsApp.ShortID + " " + LrgsApp.releasedOn;
    }

    /**
     * @return the DDS Server object.
     */
    public DdsServer getDdsServer()
    {
        return ddsServer;
    }

    /**
     * Get an input interface at specified slot.
     * @param slot the slot.
     * @return an interator into the vector of LRGS input interfaces.
     */
    public LrgsInputInterface getLrgsInput(int slot)
    {
        if (slot >= lrgsInputs.length)
            return null;
        return lrgsInputs[slot];
    }

    public LrgsInputInterface getLrgsInputById(int id)
    {
        for(int i=0; i<lrgsInputs.length; i++)
            if (lrgsInputs[i] != null
             && lrgsInputs[i].getDataSourceId() == id)
                return lrgsInputs[i];
        return null;
    }

    /**
     * Adds an input interface and assigns it a slot.
     * @return the slot used.
     */
    public int addInput(LrgsInputInterface inp)
    {
        for(int i=0; i<lrgsInputs.length; i++)
            if (lrgsInputs[i] == null)
            {
                inp.setSlot(i);
                lrgsInputs[i] = inp;
                return i;
            }
        Logger.instance().failure(module + ":" + EVT_INPUT_INIT
            + "- Cannot add input interface "
            + inp.getInputName() + ": input interface vector full!");
        return -1;
    }

    /**
     * Frees a slot.
     * @param slot the slot number.
     */
    public void freeInput(int slot)
    {
        if (slot >= 0 && slot < lrgsInputs.length)
            lrgsInputs[slot] = null;
    }

    /**
     * Writes the HTML status snapshot.
     */
    private void writeStatusSnapshot()
    {
        File f = new File(EnvExpander.expand(cfg.htmlStatusFile));
        LrgsStatusSnapshotExt lsse = statusProvider.getStatusSnapshot();
        statusRptGen.write(f, lsse.hostname, lsse, cfg.htmlStatusSeconds);
    }

    public DetailReportGenerator getReportGenerator()
    {
        return statusRptGen;
    }

    public JavaLrgsStatusProvider getStatusProvider()
    {
        return this.statusProvider;
    }

    /**
     * Called on Linux when SIGHUP has been received.
     */
    public void handleSignal(int sig)
    {
        Logger.instance().info("SIGHUP received -- rotating logs.");
        fileLogger.rotateLogs();
        if (ddsServer != null
         && DdsServer.statLoggerThread != null)
            DdsServer.statLoggerThread.rotateLogs();
    }

    public LrgsDatabaseThread getDbThread()
    {
        return dbThread;
    }

    private void runOnStartupCmd(String onStartupCmd)
    {
        Logger.instance().info(module + " Running onStartupCmd '"
            + onStartupCmd + "' and will wait 10 sec for completion");
        long start = System.currentTimeMillis();
        onStartupCmdFinished = false;
        try
        {
            ProcWaiterThread.runBackground(onStartupCmd, "onStartupCmd",
                this, null);
            while(System.currentTimeMillis() - start < 10000L
               && !onStartupCmdFinished)
            {
                try { Thread.sleep(500L); }
                catch(InterruptedException ex) {}
            }
            Logger.instance().info(module + " Proceeding.");
        }
        catch(IOException ex)
        {
            Logger.instance().warning(module
                + " Cannot run onStartupCmd '" + onStartupCmd + "': " + ex);
        }
    }

    public void procFinished(String procName, Object obj, int exitStatus)
    {
        Logger.instance().info(module + " Process '" + procName
            + "' finished with exit status " + exitStatus);
        if (procName.equalsIgnoreCase("onStartupCmd"))
            onStartupCmdFinished = true;
    }
}
