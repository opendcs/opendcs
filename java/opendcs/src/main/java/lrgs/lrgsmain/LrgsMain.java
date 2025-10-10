/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.lrgsmain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.TimeZone;

import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.opendcs.tls.TlsMode;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
import ilex.jni.SignalHandler;
import ilex.util.EnvExpander;
import ilex.util.FileServerLock;
import ilex.util.NoOpServerLock;
import ilex.util.Pair;
import ilex.util.ProcWaiterCallback;
import ilex.util.ProcWaiterThread;
import ilex.util.QueueLogger;
import ilex.util.ServerLock;
import ilex.util.ServerLockable;
import lrgs.archive.InvalidArchiveException;
import lrgs.archive.MsgArchive;
import lrgs.db.LrgsDatabaseThread;
import lrgs.ddsrecv.DdsRecv;
import lrgs.ddsserver.DdsServer;
import lrgs.dqm.DapsDqmInterface;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.edl.EdlInputInterface;
import lrgs.gui.DecodesInterface;
import lrgs.gui.LrgsApp;
import lrgs.iridiumsbd.IridiumSbdInterface;
import lrgs.ldds.PasswordChecker;
import lrgs.lrgsmon.DetailReportGenerator;
import lrgs.lrit.LritDamsNtReceiver;
import lrgs.networkdcp.NetworkDcpRecv;
import lrgs.noaaportrecv.NoaaportRecv;
import lrgs.statusxml.LrgsStatusSnapshotExt;
import opendcs.dai.LoadingAppDAI;

/**
Main class for LRGS process.
*/
public class LrgsMain implements Runnable, ServerLockable, SignalHandler, ProcWaiterCallback
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

    private final QueueLogger queueLogger = new QueueLogger("lrgs");

    private final String lockFileName;
    private final String configFileName;

    /** Constructor. */
    public LrgsMain(String lockFileName, String configFileName)
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
        alarmHandler = new AlarmHandler(queueLogger);
        dbThread = null;
        noaaportRecv = null;
        ddsRecv2=null;
        this.lockFileName = lockFileName;
        this.configFileName = configFileName;
    }

    public void run()
    {
        try
        {
            ResourceFactory.instance().initDbResources();
        }
        catch (DatabaseException ex)
        {
            log.atError().setCause(ex).log("Unable to initialize database resources.");
        }

        shutdownFlag = false;
        log.info("============ {} Starting ============", getAppName());

        // Establish a server lock file & start the server lock monitor

        String lockName = EnvExpander.expand(lockFileName);
        log.info("Lock File ={}", lockName);
        if (lockName.equals("-"))
        {
            myServerLock = new NoOpServerLock();
        }
        else
        {
            myServerLock = new FileServerLock(lockName);
            if (!myServerLock.obtainLock(this))
            {
                log.error("{}:{}- Lock file '{}' already taken. Is another instance of '{}' already running?",
                          module, EVT_LOCK_BUSY, lockName, LrgsCmdLineArgs.progname);
                System.exit(1);
            }
        }

        // Do all of the initialization & exit on fatal error.
        if (!initLRGS())
        {
            log.error("============ {} INIT FAILED -- Exiting. ============", getAppName());
            myServerLock.releaseLock();
            System.exit(0);
        }

        long lastStatusSnapshot = 0L;


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
        log.info("============ {} Exiting ============", LrgsCmdLineArgs.progname);
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
            log.atError()
               .setCause(ex)
               .log("{}:{}- Cannot read config file '{}'", module, EVT_BAD_CONFIG, cfgName);
            return false;
        }
        log.info("Config getDoPdtValidation={}", cfg.getDoPdtValidation());

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
                    log.error("loadDecodes=true, but neither '{}' nor '{}' " +
                              "is readable. Proceeding with default DECODES settings.",
                              userPath, homePath);
                }
                else
                {
                    //Load the decodes.properties
                    DecodesSettings settings = DecodesSettings.instance();
                    if (!settings.isLoaded())
                    {
                        log.info("Loading DECODES settings from '{}'", propFile.getPath());
                        Properties props = new Properties();
                        try (FileInputStream fis = new FileInputStream(propFile))
                        {
                            props.load(fis);
                        }
                        catch(Exception ex)
                        {
                            log.atError()
                               .setCause(ex)
                               .log("Cannot open DECODES Properties File '{}'", propFile.getPath());
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
                    log.warn("Internal DACQ event system is no longer supported. Adjust logger configuration " +
                             "to achieve same or similar behavior.");
                }

            }
            catch(DecodesException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("{} Cannot initialize DECODES DB -- assuming not installed.", LrgsCmdLineArgs.progname);
                decodes.db.Database.setDb(null);
            }
        }

        // Create a tmp dir for various files if it doesn't already exist.
        File lrgsTmp = new File(EnvExpander.expand("$LRGSHOME/tmp"));
        if (!lrgsTmp.isDirectory())
            lrgsTmp.mkdirs();

        // Initialize the LRGS Database Interface
        dbThread = LrgsDatabaseThread.instance();
        if (!dbThread.isAlive()) {
            dbThread.start();
        }

        alarmHandler.start();

        // Initialize the message archive.
        try { initArchive(cfg); }
        catch(InvalidArchiveException ex)
        {
            log.atError().setCause(ex).log("{}:{}- Cannot initialize Archive", module, EVT_ARC_INIT);
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
            log.info("Constructing NOAAPORT Receive Module.");
            noaaportRecv = new NoaaportRecv(this, msgArchive);
            addInput(noaaportRecv);
        }
        else
        {
            log.debug("NOAAPORT _not_ enabled.");
        }

        // If enabled, create the Network DCP Receive Module
        if (cfg.networkDcpEnable)
        {
            log.info("Constructing Network DCP Receive Module.");
            networkDcpRecv = new NetworkDcpRecv(this, msgArchive);
            addInput(networkDcpRecv);
            statusProvider.getStatusSnapshot().networkDcpStatusList
                = networkDcpRecv.getStatusList();
        }

        if (cfg.enableLritRecv)
        {
            log.info("Enabling DAMS-NT HRIT Receiver");
            LritDamsNtReceiver ldnr = new LritDamsNtReceiver(msgArchive, this);
            ldnr.configure(null);
            addInput(ldnr);
        }
        else
        {
            log.info("LRIT is not enabled.");
        }

        if (cfg.hritFileEnabled)
        {
            log.info("Enabling HRIT-File Receiver");
            HritFileInterface hfi = new HritFileInterface(this, msgArchive);
            addInput(hfi);
        }

        if (cfg.iridiumEnabled)
        {
            log.info("Enabling Iridium Receive Module.");
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
                log.atError()
                   .setCause(lie)
                   .log("{}:{}- Cannot initialize input interface '{}'", module, EVT_INPUT_INIT, lii.getInputName());
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
                log.atError().setCause(ex).log("Cannot start EdlInputInterface.");
            }
        }

        // We have to initialize the quality log so that the DDS receiver
        // picks up where it left off.
        statusProvider.initQualityLog();


        ddsRecv.setLastMsgRecvTime(statusProvider.getLastDdsReceiveTime());
        ddsRecv2.setLastMsgRecvTime(statusProvider.getLastSecDdsReceiveTime()); // for secondary group

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
                log.atError().setCause(ex).log("Cannot load password checker class '{}'", pcc);
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
                log.atWarn().setCause(ex).log("Unable to create Loadable Input {}", loadableName);
            }
            catch (LrgsInputException ex)
            {
                log.atWarn().setCause(ex).log("Unable to configure Loadable Input {}", loadableName);
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
            final Pair<ServerSocketFactory,SSLSocketFactory> socketFactories = initSocketFactory(cfg);
            InetAddress ia =
                (cfg.ddsBindAddr != null && cfg.ddsBindAddr.length() > 0) ?
                InetAddress.getByName(cfg.ddsBindAddr) : null;
            ddsServer = new DdsServer(cfg.ddsListenPort, ia, msgArchive, queueLogger, statusProvider, socketFactories);
            ddsServer.init();
            return true;
        }
        catch(Exception ex)
        {
            log.atError().setCause(ex).log("{}:{}- Cannot start DDS Server: ", module, EVT_DDS_INIT);
            throw new RuntimeException("Unable to start DDS server", ex);
        }
    }


    private Pair<ServerSocketFactory,SSLSocketFactory> initSocketFactory(LrgsConfig cfg)
        throws NoSuchAlgorithmException, CertificateException,
               FileNotFoundException, IOException, KeyStoreException, UnrecoverableKeyException, KeyManagementException
    {
        ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
        SSLSocketFactory socketFactory = null;
        if(cfg.getDdsServerTlsMode() != TlsMode.NONE && cfg.keyStoreFile != null && cfg.keyStorePassword != null) {
            SSLContext context = SSLContext.getInstance("TLS");
            //TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("jks");
            ks.load(new FileInputStream(EnvExpander.expand(cfg.keyStoreFile)),cfg.keyStorePassword.toCharArray());
            kmf.init(ks,cfg.keyStorePassword.toCharArray());
            context.init(kmf.getKeyManagers(),null,null);
            if (cfg.getDdsServerTlsMode() == TlsMode.TLS)
            {
                serverSocketFactory = context.getServerSocketFactory();
            }
            else // START_TLS mode requires the BasicServer to have access to an ssl socket factory
                 // to alter the connection
            {
                socketFactory = context.getSocketFactory();
            }
        }
        return Pair.of(serverSocketFactory, socketFactory);
    }

    public void shutdown()
    {
        shutdownFlag = true;
    }

    /** Called from ServerLockable when the lock file is removed. */
    public void lockFileRemoved()
    {
        log.info("{} Exiting -- Lock File Removed.", LrgsCmdLineArgs.progname);
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

        final LrgsMain lm = new LrgsMain(cmdLineArgs.getLockFile(), cmdLineArgs.getConfigFile());
        if (cmdLineArgs.runInForGround() )
        {
            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook( new Thread() {
                @Override
                public void run()
                {
                    try
                    {
                        log.trace("SIGTERM Caught, Setting shutdown flag to true.");
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
        log.debug("Default 'addCustomFeatures' doing nothing.");
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
        log.error("{}:{}- Cannot add input interface {}: input interface vector full!",
                  module, EVT_INPUT_INIT, inp.getInputName());
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
        log.info("SIGHUP received -- rotating logs.");
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
        log.info("Running onStartupCmd '{}' and will wait 10 sec for completion", onStartupCmd);
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
            log.info("Proceeding.");
        }
        catch(IOException ex)
        {
            log.atWarn().setCause(ex).log("Cannot run onStartupCmd '{}'", onStartupCmd);
        }
    }

    public void procFinished(String procName, Object obj, int exitStatus)
    {
        log.info("Process '{}' finished with exit status {}", procName, exitStatus);
        if (procName.equalsIgnoreCase("onStartupCmd"))
            onStartupCmdFinished = true;
    }

    public List<LrgsInputInterface> getInputs()
    {
        List<LrgsInputInterface> list = new ArrayList<>();
        for(LrgsInputInterface lrgsInput: lrgsInputs)
        {
            list.add(lrgsInput);
        }
        return list;
    }
}