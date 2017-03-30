/*
*  $Id$
*  
*  Open Source Software 
*  
*  $Log$
*  Revision 1.12  2017/03/14 18:54:35  mmaloney
*  CWMS-10402 Don't retry connect if failure is due to invalid app name.
*
*  Revision 1.11  2016/10/01 15:00:41  mmaloney
*  CWMS-8979 Allow Database Process Record to override decodes.properties and
*  user.properties setting. Command line arg -Dsettings=appName, where appName is the
*  name of a process record. Properties assigned to the app will override the file(s).
*
*  Revision 1.10  2016/09/29 18:54:37  mmaloney
*  CWMS-8979 Allow Database Process Record to override decodes.properties and
*  user.properties setting. Command line arg -Dsettings=appName, where appName is the
*  name of a process record. Properties assigned to the app will override the file(s).
*
*  Revision 1.9  2016/09/28 15:14:04  mmaloney
*  Fail safe code for daemons to close before attempting to reconnect.
*
*  Revision 1.8  2015/05/14 13:52:18  mmaloney
*  RC08 prep
*
*  Revision 1.7  2015/02/06 18:56:59  mmaloney
*  Added appMinDebugLevel
*
*  Revision 1.6  2014/12/11 20:29:31  mmaloney
*  Added DacqEventLogging capability.
*
*  Revision 1.5  2014/11/19 16:09:48  mmaloney
*  Additions for dcpmon
*
*  Revision 1.4  2014/08/22 17:23:04  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.3  2014/07/10 17:07:54  mmaloney
*  Remove startup log from ComputationApp, and add to TsdbAppTemplate.
*
*  Revision 1.2  2014/07/03 12:44:38  mmaloney
*  Don't call readDecodesProperties() this is done by CmdLineArgs.
*  Also, better consistency for CWMS GUI Apps in retrieving DB Passwords.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.15  2013/03/28 17:29:09  mmaloney
*  Refactoring for user-customizable decodes properties.
*
*  Revision 1.14  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.util.Properties;

import opendcs.dai.LoadingAppDAI;
import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.StderrLogger;
import ilex.util.TextUtil;
import ilex.util.UserAuthFile;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;
import decodes.util.PropertySpec;
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

	/** The application ID determined when connecting to the database. */
	public DbKey appId;
	
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
	
	
	/**
	 * Base class constructor. Pass it the default name of the log file.
	 */
	public TsdbAppTemplate(String logname)
	{
		if (logname == null)
			logname = "test.log";
		cmdLineArgs = new CmdLineArgs(false, logname);
		cfgFileArg = new StringToken("c", "comp-config-file",
			"", TokenOptions.optSwitch, "$DECODES_INSTALL_DIR/comp.conf"); 
		testModeArg = new BooleanToken("t", "test-mode",
			"", TokenOptions.optSwitch, false);
		modelRunArg = new IntegerToken("m", 
			"output-model-run-ID", "", TokenOptions.optSwitch, -1); 
		appNameArg = new StringToken("a", "Application-Name", "",
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(cfgFileArg);
		cmdLineArgs.addToken(testModeArg);
		cmdLineArgs.addToken(modelRunArg);
		cmdLineArgs.addToken(appNameArg);
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
				initDecodes();
			}
			catch(DecodesException ex)
			{
				warning("Cannot init Decodes: " + ex);
				databaseFailed = true;
				continue;
			}
			try
			{
				createDatabase();
				tryConnect();
			}
			catch(BadConnectException ex)
			{
				warning("Cannot connect to TSDB: " + ex);
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
			Logger.instance().info(appNameArg.getValue() + " exiting.");
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
		Logger.instance().info("===============================================");
		Logger.instance().info(appNameArg.getValue() + " starting. "
			+ DecodesVersion.startupTag() + ", pid=" + getPID());
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
		Logger.setLogger(new StderrLogger(appNameArg.getValue()));

		// Parse command line arguments.
		try { cmdLineArgs.parseArgs(args); }
		catch(IllegalArgumentException ex)
		{
			System.exit(1);
		}
		
		// Remember the application-level debug level set from arguments.
		appDebugMinPriority = Logger.instance().getMinLogPriority();
	}

	/**
	 * Creates the TimeSeriesDb object and connects to the underlying SQL
	 * database.
	 * 'theDb' is static in this base-class. Therefore if a sibling in
	 * the launcher has already loaded the database, this method does nothing.
	 * This is also why this method is synchronized.
	 * @throws ClassNotFoundException if can't find database class name
	 * @throws InstantiationException if can't instantiate database object
	 * @throws IllegalAccessException if no permission to access database class
	 */
	public synchronized void createDatabase()
		throws ClassNotFoundException,
		InstantiationException, IllegalAccessException
	{
		if (theDb != null)
			return;

		String className = DecodesSettings.instance().dbClassName;

		try
		{
			Logger.instance().info("Connecting to time series database with class '"
				+ className + "'");
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			Class dbClass = cl.loadClass(className);
			theDb = (TimeSeriesDb)dbClass.newInstance();
		}
		catch(ClassNotFoundException ex)
		{
			String msg = "Check concrete database class name. Can't find '"
				+ className + "': " + ex;
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}
		catch(InstantiationException ex)
		{
			String msg = "Can't instantiate object of type '"
				+ className + "': " + ex;
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}
		catch(IllegalAccessException ex)
		{
			String msg = "Not permitted to instantiate object of type '"
				+ className + "': " + ex;
			System.err.println(msg);
			Logger.instance().fatal(msg);
			throw ex;
		}

		// Set test-mode flag & model run ID in the database interface.
		theDb.setTestMode(testModeArg.getValue());
		int modelRunId = modelRunArg.getValue();
		if (modelRunId != -1)
			theDb.setWriteModelRunId(modelRunId);
	}

	/**
	 * Attempt to connect to the database.
	 * @return true if success, false if not.
	 * @throws BadConnectException if failure to connect.
	 */
	public void tryConnect()
		throws BadConnectException
	{
		if (theDb.isConnected())
		{
			warning("Closing connection before reconnect.");
			theDb.closeConnection();
		}
		Properties credentials = new Properties();
		String nm = appNameArg.getValue();
		if (!DecodesInterface.isGUI() || !theDb.isCwms())
		{
			// Get authorization parameters.
			String afn = EnvExpander.expand(DecodesSettings.instance().DbAuthFile);
			UserAuthFile authFile = new UserAuthFile(afn);
			try { authFile.read(); }
			catch(Exception ex)
			{
				authFileEx(afn, ex);
				throw new BadConnectException("Cannot read auth file: " + ex);
			}
	
			// Connect to the database!
			credentials.setProperty("username", authFile.getUsername());
			credentials.setProperty("password", authFile.getPassword());
		}
		// Else this is a CWMS GUI -- user will be prompted for credentials
		// Leave the property set empty.
		
		appId = theDb.connect(nm, credentials);
		
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			// CWMS-8979 Allow settings in the database to override values in user.properties.
			String settingsApp = cmdLineArgs.getCmdLineProps().getProperty("settings");
			if (settingsApp != null)
			{
				info("Overriding Decodes Settings with properties in Process Record '" + settingsApp + "'");
				try
				{
					CompAppInfo cai = loadingAppDAO.getComputationApp(settingsApp);
					PropertiesUtil.loadFromProps(DecodesSettings.instance(), cai.getProperties());
				}
				catch (DbIoException ex)
				{
					warning("Cannot load settings from app '" + settingsApp + "': " + ex);
				}
				catch (NoSuchObjectException ex)
				{
					warning("Cannot load settings from non-existent app '" + settingsApp + "': " + ex);
				}
			}
		}
		finally
		{
			loadingAppDAO.close();
		}
	}

	/**
	 * @param afn
	 * @param ex
	 */
	protected void authFileEx(String afn, Exception ex)
	{
		String msg = "Cannot read DB auth from file '" + afn + "': " + ex;
		System.err.println(msg);
		Logger.instance().failure(msg);
try { throw new Exception(""); } catch (Exception ex2) { ex2.printStackTrace(); }
	}
	
	protected void badConnect(String appName, BadConnectException ex)
	{
		String msg = appName + " Cannot connect to DB: " + ex.getMessage();
		System.err.println(msg);
		Logger.instance().failure(msg);
	}

	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
		DecodesInterface.initializeForEditing();
	}
	
	public void shutdownDecodes()
	{
		DecodesInterface.shutdownDecodes();
	}

	public void closeDb()
	{
		if (theDb != null)
		{
			Logger.instance().info("Closing database connection.");
			theDb.closeConnection();
		}
		theDb = null;
	}
	
	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void info(String msg)
	{
		Logger.instance().info(appNameArg.getValue() + " " + msg);
	}

	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void warning(String msg)
	{
		Logger.instance().warning(appNameArg.getValue() + " " + msg);
	}

	/**
	 * Convenience method to log warning with app name prefix.
	 * @param msg the message
	 */
	public void failure(String msg)
	{
		Logger.instance().failure(appNameArg.getValue() + " " + msg);
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
				catch(Exception ex) {}
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
				Logger.instance().warning("Bad EventPort property '" + evtPorts
					+ "' must be integer -- will derive from PID");
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

}
