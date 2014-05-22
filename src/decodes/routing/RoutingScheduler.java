package decodes.routing;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PassThruLogger;
import ilex.util.PropertiesUtil;
import ilex.util.ServerLock;
import ilex.util.StderrLogger;
import ilex.util.ThreadLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

import lrgs.gui.DecodesInterface;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.ScheduleEntry;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;
import decodes.util.ResourceFactory;

/**
 * The main class for scheduling routing specs
 * @author mmaloney, Mike Maloney Cove Software, LLC
 */
public class RoutingScheduler 
	extends TsdbAppTemplate
	implements PropertiesOwner
{
	private static final String module = "RoutingScheduler";
	static StringToken lockFileArg = new StringToken("k", 
		"Optional Lock File", "", TokenOptions.optSwitch, "");
	/** Holds app name, id, & description. */
	CompAppInfo appInfo;

	/** My lock */
	private TsdbCompLock myLock = null;
	private boolean shutdownFlag = false;

	private int pid = -1;
	private String hostname = null;
	private int evtPort = -1;
	
	/*** Purge entries older than this many days */
	public long purgeBeforeDays = 30;
	public static long MSEC_PER_DAY = 24 * 3600 * 1000L;
	
	/** Number of seconds at which to purge old status record from the database */
	public long oldStatusPurgeInterval = 3600L; // default = 1 hr.

	/** Number of seconds at which to refresh schedule entries from the database */
	public long refreshSchedInterval = 60L;   // default = 1 minutes
	
	private PropertySpec[] myProps =
	{
		new PropertySpec("EventPort", PropertySpec.INT,
			"Open listening socket on this port to serve out app events."),
		new PropertySpec("purgeBeforeDays", PropertySpec.INT, 
			"Purge status entries older than this number of days (def=30)"),
		new PropertySpec("oldStatusPurgeInterval", PropertySpec.INT, 
			"Interval (sec) at which to purge old statuses (def=3600 or 1 hour)"),
		new PropertySpec("refreshSchedInterval", PropertySpec.INT,
			"Interval (sec) at which to check for schedule entry changes (def=60 or 1 min)"),
	};

	private ArrayList<ScheduleEntryExecutive> executives = new ArrayList<ScheduleEntryExecutive>();
	private ScheduleEntryDAI scheduleEntryDAO = null;
	private ThreadLogger appLogger = null;
	private Logger origLogger = null;
	
	public RoutingScheduler()
	{
		super("routsched.log");
		setSilent(true);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		lockFileArg.setType("filename");
		cmdLineArgs.addToken(lockFileArg);
		appNameArg.setDefaultValue("RoutingScheduler");
	}
	
	@Override
	protected void runApp() throws Exception
	{
		Logger.instance().info("============== " + getAppName() 
			+", appId=" + appId + " Starting ==============");
		
		runAppInitialization();

		long lastOldStatusPurge = 0L; // Cause it to happen right away
		long lastSchedRefresh = 0L;
		long lastLockCheck = 0L;
		
		while(!shutdownFlag)
		{
			String action="";
			LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
			try
			{
				long now = System.currentTimeMillis();

				// Make sure this process's lock is still valid.
				action = "Checking lock";
				if (myLock != null && now - lastLockCheck > 5000L)
				{
					myLock.setStatus(getStatistics());
					loadingAppDAO.checkCompProcLock(myLock);
					lastLockCheck = now;
				}

				if (now - lastSchedRefresh > (refreshSchedInterval*1000L))
				{
					action = "Refreshing Schedule";
					refreshSchedule();
					lastSchedRefresh = now;
				}
				if (now - lastOldStatusPurge >= (oldStatusPurgeInterval*1000L))
				{
					action = "Purging old status";
					scheduleEntryDAO.deleteScheduleStatusBefore(appInfo,
						new Date(System.currentTimeMillis() - purgeBeforeDays*MSEC_PER_DAY));
					lastOldStatusPurge = now;
				}
				
				// Call each executive's check method so that it can start/stop
				// routing specs as appropriate for the state and schedule.
				action = "Check Executives";
				for(ScheduleEntryExecutive executive : executives)
					executive.check();
			}
			catch(LockBusyException ex)
			{
				Logger.instance().info(module + " No Lock - Application exiting: " + ex);
				shutdownFlag = true;
			}
			catch(Exception ex)
			{
				Logger.instance().warning(module + " Exception while " + action + ": " + ex);
				shutdownFlag = true;
			}
			finally
			{
				loadingAppDAO.close();
			}
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		for(ScheduleEntryExecutive executive : executives)
			executive.shutdown();
		try { Thread.sleep(5000L); }
		catch(InterruptedException ex) {}

		scheduleEntryDAO.close();
		Logger.instance().info(module + " exiting.");
		System.exit(0);
	}
	
	/**
	 * This method is called at the beginning of the runApp() method
	 * to perform one-time initialization. This includes:
	 * <ul>
	 *   <li>Reading the Loading App info from the database</li>
	 *   <li>Return process ID passed on the command line</li>
	 *   <li>Determine the hostname</li>
	 *   <li>Open an event listening socket if one is specified in the app info</li>
	 * </ul>
	 * @throws LockBusyException if unable to secure a lock for this daemon
	 * @throws DbIoException on SQL error in the TSDB
	 * @throws NoSuchObjectException if app ID or app name is invalid.
	 */
	private void runAppInitialization()
		throws LockBusyException, DbIoException, NoSuchObjectException
	{
		// Set up a logger that will add a prefix rs name to each log message
		// generated from within the threads.
		origLogger = Logger.instance();
		origLogger.debug1("Before creating thread-specific logger.");
		appLogger = new ThreadLogger(module, null, null, false);
		appLogger.setDefaultLogger(origLogger);
		appLogger.setMinLogPriority(origLogger.getMinLogPriority());
		Logger.setLogger(appLogger);
		Logger.instance().debug1("log to thread logger.");
		origLogger.debug1("log to orig logger.");
		
		// Get the loading app info from the DECODES database, not TSDB.
		DatabaseIO dbio = decodes.db.Database.getDb().getDbIo();
		LoadingAppDAI loadingAppDao = dbio.makeLoadingAppDAO();

		try
		{
			appInfo = loadingAppDao.getComputationApp(appNameArg.getValue());

			PropertiesUtil.loadFromProps(this, appInfo.getProperties());

			// Determine process ID. Note -- We can't really do this in Java
			// without assuming a particular OS. Therefore, we rely on the
			// script that started us to set an environment variable PPID
			// for parent-process-ID. If not present, we default to 1.
			pid = 1;
			String ppids = System.getProperty("PPID");
			if (ppids != null)
			{
				try { pid = Integer.parseInt(ppids); }
				catch(NumberFormatException ex) { pid = 1; }
			}

			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { hostname = "unknown"; }

			// Look for EventPort and EventPriority properties. If found,
			String evtPorts = appInfo.getProperty("EventPort");
			if (evtPorts != null)
			{
				try 
				{
					evtPort = Integer.parseInt(evtPorts.trim());
					CompEventSvr compEventSvr = new CompEventSvr(evtPort);
					compEventSvr.startup();
				}
				catch(NumberFormatException ex)
				{
					Logger.instance().warning("App Name " + getAppName()
						+ ": Bad EventPort property '" + evtPorts
						+ "' must be integer. -- ignored.");
				}
				catch(IOException ex)
				{
					Logger.instance().failure(
						"Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}

			if (loadingAppDao.supportsLocks())
				myLock = loadingAppDao.obtainCompProcLock(appInfo, pid, hostname);
		}
		catch(NoSuchObjectException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + ": " + ex);
			throw ex;
		}
		catch(DbIoException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + ": " + ex);
			throw ex;
		}
		finally
		{
			loadingAppDao.close();
		}
		scheduleEntryDAO = dbio.makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
		{
			Logger.instance().fatal("App Name " + getAppName() 
				+ " cannot run routing scheduler -- schedule entries not supported in this database.");
			System.exit(1);
		}
	}
	
	public String getAppName()
	{
		return appInfo != null ? appInfo.getAppName() : appNameArg.getValue();
	}

	
	private void refreshSchedule() 
		throws DbIoException
	{
		// read list of ScheduleEntry's for my loading app.
		ArrayList<ScheduleEntry> dbEntries = scheduleEntryDAO.listScheduleEntries(appInfo);
		ArrayList<ScheduleEntryExecutive> newExecs = new ArrayList<ScheduleEntryExecutive>();
		for(ScheduleEntry dbEntry : dbEntries)
		{
			boolean running = false;
			for(Iterator<ScheduleEntryExecutive> execit = executives.iterator();
				execit.hasNext(); )
			{
				ScheduleEntryExecutive exec = execit.next();
				// Have to match on name to accommodate xml.
				if (exec.getScheduleEntry().getName().equalsIgnoreCase(dbEntry.getName()))
				{
					if (exec.getScheduleEntry().getLastModified().before(
						dbEntry.getLastModified()))
					{
						// Changed! Shutdown the old executive and init a new one.
						exec.shutdown();
						execit.remove();
					}
					else // currently running executive is okay!
						running = true;
					break;
				}
			}
			if (!running)
				newExecs.add(new ScheduleEntryExecutive(dbEntry, this));
		}
		executives.addAll(newExecs);
	}
	
	private String getStatistics()
	{
		int run, wait, complete, init, down;
		run = wait = complete = init = down = 0;
		for (ScheduleEntryExecutive exec : executives)
		{
			switch(exec.getRunState())
			{
			case initializing:
				init++;
				break;
			case waiting:
				wait++;
				break;
			case running:
				run++;
				break;
			case complete:
				complete++;
				break;
			case shutdown:
				down++;
				break;
			}
		}
		String ret = "run=" + run + ", wait=" + wait + ", cmpl=" + complete;
		if (init > 0)
			ret = ret + ", init=" + init;
		if (down > 0)
			ret = ret + ", down=" + down;
		return ret;
	}
	
	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
		DecodesInterface.initializeForDecoding();
	}
	
	@Override
	public synchronized void createDatabase()
		throws ClassNotFoundException,
		InstantiationException, IllegalAccessException
	{
		// Do nothing. The scheduler must not use the TSDB.
	}
	
	@Override
	public boolean tryConnect()
	{
		// Do nothing. The scheduler must not use the TSDB.
		return true;
	}

	/**
	* Main method.
	* @param args the command line arguments
	*/
	public static void main(String args[])
		throws Exception
	{
		Logger.setLogger(new StderrLogger(module));
		
		final RoutingScheduler routsched = new RoutingScheduler();

		/** Optional server lock ensures only one instance runs at a time. */
		String lockpath = lockFileArg.getValue();
		if (lockpath != null && lockpath.trim().length() > 0)
		{
			lockpath = EnvExpander.expand(lockpath.trim());
			final ServerLock mylock = new ServerLock(lockpath);

			if (mylock.obtainLock() == false)
			{
				Logger.instance().log(Logger.E_FAILURE,
					module + " started: lock file busy: " + lockpath);
				Database db = Database.getDb();
				db.getDbIo().close();
				System.exit(0);
			}

			mylock.releaseOnExit();
			Runtime.getRuntime().addShutdownHook(
				new Thread()
				{
					public void run()
					{
						Logger.instance().log(Logger.E_INFORMATION,
							module + " exiting " +
							(mylock.wasShutdownViaLock() ? "(lock file removed)"
							: ""));
					}
				});
		}

		routsched.execute(args);
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myProps;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
	
	/**
	 * Called from a ScheduleEntryExecutive when it create a RoutingSpecThread
	 * to execute the routing spec. This method associates a pass-through logger
	 * with the thread so that it can have its own log priority, set by property.
	 * @param thread the ScheduleEntryThread that is about to start.
	 */
	public void makeThreadLogger(Thread thread)
	{
		appLogger.setLogger(thread, new PassThruLogger(origLogger));
	}
	
	public void threadFinished(Thread thread)
	{
		appLogger.setLogger(thread, null);
	}

	public String getHostname()
	{
		return hostname;
	}
}
