package decodes.tsdb;

import java.io.IOException;
import java.util.Properties;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.database.DatabaseService;
import org.slf4j.LoggerFactory;

import opendcs.dai.LoadingAppDAI;
import ilex.cmdline.*;
import ilex.util.AuthException;
import ilex.util.Logger;
import ilex.util.Pair;
import ilex.util.PropertiesUtil;
import ilex.util.StderrLogger;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;
import decodes.util.PropertySpec;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.launcher.Profile;
import decodes.sql.DbKey;
import decodes.util.DecodesException;
import decodes.util.PropertiesOwner;
import lrgs.gui.DecodesInterface;

/**
This is a convenient base class for any application that uses the time
series database. It contains template methods for connecting to the database,
reading the configuration, etc.
<p>
Have your program extend this class. Write a main method that calls the
execute method. Then consider overriding the following methods:
<ul>
  <li>addCustomArgs - to add custom command-line arguments</li>
  <li>initConfig - Initializes configuration by reading properties files</li>
  <li>createDatabase - Creates the actual TimeSeriesDb object and connects
      to the underlying SQL database.</li>
  <li>initDecodes - Initializes common DECODES resources needed by most
	  applications.</li>
  <li>runApp - called after all initialization, add your app code here.</li>
</ul>
<p>
*/
public abstract class TsdbAppTemplate
	implements PropertiesOwner
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(TsdbAppTemplate.class.getName());
	// Static command line arguments and initialization for main method.
	protected CmdLineArgs cmdLineArgs;

	/** The name of the configuration file */
	protected StringToken cfgFileArg;

	/** True if we are running in test mode (don't modify DB) */
	protected BooleanToken testModeArg;

	/** Model Run ID. */
	protected IntegerToken modelRunArg;

	/** Application name - used to determine LOADING_APPLICATION_ID. */
	protected StringToken appNameArg;

	/** The time series database to use in this application. */
	public static TimeSeriesDb theDb = null;
	protected static Database decodesDb = null;
	private DecodesSettings settings = null;
	private String appName = null;

	/** The application ID determined when connecting to the database. */
	private DbKey appId = DbKey.NullKey;

	/**
	 * Subclass can set this to true to cause application to restart if
	 * the execute method exits due to database going down.
	 */
	protected boolean surviveDatabaseBounce = false;

	/**
	 * Subclass can set this to true to cause application to restart if
	 * the execute method exits due to database going down.
	 */
	protected boolean databaseFailed = false;

	/**
	 * Most apps do the work in the runApp() method. Others, like GUIs
	 * start threads and then allow the runApp method to exit. GUIs
	 * should set noExitAfterRunApp to true.
	 */
	protected boolean noExitAfterRunApp = false;

	/**
	 * Determined at startup, available via getPID();
	 */
	private int pid = -1;

	protected int appDebugMinPriority = Logger.E_INFORMATION;

	protected static TsdbAppTemplate appInstance = null;


	/**
	 * Base class constructor. Pass it the default name of the log file.
	 */
	public TsdbAppTemplate(String logname)
	{
		if (logname == null)
			logname = "test.log";
		cmdLineArgs = new CmdLineArgs(false, logname);
		cfgFileArg = new StringToken("c", "comp-config-file",
			"", TokenOptions.optSwitch, "");
		testModeArg = new BooleanToken("t", "test-mode",
			"", TokenOptions.optSwitch, false);
		modelRunArg = new IntegerToken("m",
			"output-model-run-ID", "", TokenOptions.optSwitch, -1);
		appNameArg = new StringToken("a", "Application-Name", "", TokenOptions.optSwitch, "utility");
		cmdLineArgs.addToken(cfgFileArg);
		cmdLineArgs.addToken(testModeArg);
		cmdLineArgs.addToken(modelRunArg);
		cmdLineArgs.addToken(appNameArg);
		if (appInstance == null)
			appInstance = this;
	}

	/**
	 * The sub-class main method should call this.
	 * It calls the following methods in the following order:
	 * <ul>
	 *   <li>addCustomArgs</li>
	 *   <li>parseArgs</li>
	 *   <li>readDecodesProperties</li>
	 *   <li>initConfig</li>
	 *   <li>initDecodes</li>
	 *   <li>createDatabase</li>
	 *   <li>tryConnect</li>
	 *   <li>runApp</li>
	 * </ul>
	 */
	public void execute(String args[])
		throws Exception
	{
		pid = determinePID();
		addCustomArgs(cmdLineArgs);
		parseArgs(args);
		startupLogMessage();
		appName = appNameArg.getValue();
		Profile profile = cmdLineArgs.getProfile();
		settings = DecodesSettings.instance();
		settings.loadFromProfile(profile);
		oneTimeInit();

		// Only daemons will set surviveDatabaseBounce=true.
		// For other programs, like GUIs and utilities, the code will be
		// executed only once.
		// The loop below gives daemons the ability to periodically attempt to
		// restart if the database goes down.
		boolean firstRun = true;
		while(firstRun || (surviveDatabaseBounce && databaseFailed))
		{
			if (!firstRun)
				try { Thread.sleep(15000L); } catch(InterruptedException ex) {}
			firstRun = false;
			databaseFailed = false;
			try
			{
				createDatabase();
				tryConnect();
			}
			catch(BadConnectException ex)
			{
				if (ex.getCause() instanceof NullPointerException)
				{
					// Just bail
					throw (NullPointerException)ex.getCause();
				}
				log.atError().setCause(ex).log("Cannot connect to TSDB.");
				// CWMS-10402 don't keep trying if the failure was because the
				// app name is invalid.
				databaseFailed = !ex.toString().contains("Cannot determine app ID");
				continue;
			}
			// Note: App must handle its own exceptions, detect database failure
			// and set databaseFailed if it wants a restart. Any exception thrown
			// from runApp will terminate the program.
			runApp();
			if (!noExitAfterRunApp)
			{
				closeDb();
				shutdownDecodes();
			}
		}

		if (!noExitAfterRunApp)
		{
			log.info("{} exiting.",appNameArg.getValue() );
			System.exit(0);
		}
	}

	/**
	 * Subclass can override this method if it has any one-time initialization
	 * to do prior to instanting database connections for DECODES and TSDB.
	 */
	protected void oneTimeInit()
	{
		// Empty stub
	}

	protected void startupLogMessage()
	{
		log.info("===============================================");
		log.info("{} starting. {}, pid={}", appNameArg.getValue(), DecodesVersion.startupTag(), getPID());
	}

	/**
	 * Override this and add any program-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		// Default impl does nothing.
	}

	/**
	 * Override this with the guts of your program.
	 */
	protected abstract void runApp()
		throws Exception;

	/**
	 * Parses the command line arguments.
	 * You probably don't need to override this method. This calls the
	 * parseArgs method in the CmdLineArgs class. Your app can retrieve the
	 * results later.
	 * @param args the argument from the main method.
	 */
	protected void parseArgs(String args[])
		throws Exception
	{
		if (!cmdLineArgs.isNoInit())
		{
			// eventually remove
			Logger.setLogger(new StderrLogger(appNameArg.getValue()));
		}
		try
		{
			cmdLineArgs.parseArgs(args);
		}
		catch(IllegalArgumentException ex)
		{
			log.error("Error parsing command line arguments",ex);
			System.exit(1);
		}
	}

	/**
	 * Creates the (Database and TimeSeriesDb) objects and connects to the underlying SQL
	 * database.  Initilizes the cooresponding static fields (decodesDb and theDb).
	 * This is also why this method is synchronized.
	 * @throws ClassNotFoundException if can't find database class name
	 * @throws InstantiationException if can't instantiate database object
	 * @throws IllegalAccessException if no permission to access database class
	 */
	public synchronized void createDatabase()
		throws ClassNotFoundException,
		InstantiationException, IllegalAccessException
	{
		try
		{

			Pair<Database,TimeSeriesDb> databases= DatabaseService.getDatabaseFor(appName, settings);
			decodesDb = databases.first;
			decodesDb.initializeForDecoding();
			theDb = databases.second;
		}
		catch (DecodesException ex)
		{
			throw new RuntimeException("Unable to create database.", ex);
		}
	}

	/**
	 * Attempt to connect to the database.
	 * @throws BadConnectException if failure to connect.
	 */
	public void tryConnect() throws BadConnectException
	{
		try (LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO())
		{
			CompAppInfo thisApp = loadingAppDAO.getComputationApp(appName);
			this.appId = thisApp.getAppId();
			// CWMS-8979 Allow settings in the database to override values in user.properties.
			String settingsApp = cmdLineArgs.getCmdLineProps().getProperty("settings");
			if (settingsApp != null)
			{
				log.info("Overriding Decodes Settings with properties in Process Record '{}'", settingsApp );
				try
				{
					CompAppInfo cai = loadingAppDAO.getComputationApp(settingsApp);
					this.appId = cai.getAppId();
					PropertiesUtil.loadFromProps(settings, cai.getProperties());
				}
				catch (DbIoException ex)
				{
					log.warn("Cannot load settings from app '{}'", settingsApp , ex);
				}
				catch (NoSuchObjectException ex)
				{
					log.warn("Cannot load settings from non-existent app '{}'", settingsApp , ex);
				}
			}
		}
		catch (DbIoException ex)
		{
			throw new BadConnectException("Unable to get AppId", ex);
		}
		catch (NoSuchObjectException ex)
		{
			throw new BadConnectException("unable to get database instance.", ex);
		}

		// Set test-mode flag & model run ID in the database interface.
		theDb.setTestMode(testModeArg.getValue());
		int modelRunId = modelRunArg.getValue();
		if (modelRunId != -1)
		{
			theDb.setWriteModelRunId(modelRunId);
		}
	}

	/**
	 * @param afn auth filename
	 * @param ex exception
	 */
	protected void authFileEx(String afn, Exception ex)
	{
		log.atError().setCause(ex).log("Cannot read DB auth from file '{}'", afn ,ex);
	}

	protected void badConnect(String appName, BadConnectException ex)
	{
		log.error("Cannot read DB auth from file '{}'", appName ,ex);
	}

	public void initDecodes()
		throws DecodesException
	{
	}

	public void shutdownDecodes()
	{
	//	DecodesInterface.shutdownDecodes();
	}

	public void closeDb()
	{
		if (theDb != null)
		{
			if (log.isTraceEnabled())
			{
				log.info("Closing database connection.", new Exception());
			}
			else
			{
				log.info("Closing database connection.");
			}
		}
		theDb = null;
	}

	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void info(String msg)
	{
		log.info("{} {}",appNameArg.getValue() , msg);
	}

	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void warning(String msg)
	{
		log.warn("{} {}",appNameArg.getValue(), msg);
	}

	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void failure(String msg)
	{
		log.error("{} {}",appNameArg.getValue(), msg);
	}

	public void setSilent(boolean silent)
	{
		DecodesInterface.silent = silent;
	}

	/**
	 * @return the PID assigned by the underlying VM and determined at startup.
	 */
	public int getPID() { return pid; }

	public static int determinePID()
	{
		String pids = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
		if (pids != null)
		{
			// String will be of the form 12345@username
			int idx = pids.indexOf('@');
			if (idx > 0)
			{
				try { return Integer.parseInt(pids.substring(0, idx)); }
				catch(Exception ex)
				{
					log.info("could not parse process id from '{}'",pids);
				}
			}
		}
		return -1;
	}

	public static int determineEventPort(CompAppInfo appInfo)
	{
		int evtPort = -1;
		// Legacy: If EventPort is specified, use it.
		String evtPorts = appInfo.getProperty("EventPort");
		if (evtPorts != null)
		{
			try { evtPort = Integer.parseInt(evtPorts.trim()); }
			catch(NumberFormatException ex)
			{
				log.warn("Bad EventPort property '{}' must be integer -- will derive from PID", evtPorts);
			}
		}
		if (evtPort == -1)
			evtPort = 20000 + (determinePID() % 10000);
		return evtPort;
	}


	/**
	 * {@inheritDoc}
	 * Base class always returns an empty array.
	 */
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return new PropertySpec[0];
	}

	/**
	 * {@inheritDoc}
	 * Base class always returns true. Allows any properties.
	 */
	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}

	public int getAppDebugMinPriority()
	{
		return appDebugMinPriority;
	}

	public DbKey getAppId()
	{
		return appId;
	}

	public void setAppId(DbKey appId)
	{
		this.appId = appId;
	}

	public static TsdbAppTemplate getAppInstance()
	{
		return appInstance;
	}

	public CmdLineArgs getCmdLineArgs()
	{
		return cmdLineArgs;
	}

	public void setNoExitAfterRunApp(boolean noExitAfterRunApp)
	{
		this.noExitAfterRunApp = noExitAfterRunApp;
	}

}
