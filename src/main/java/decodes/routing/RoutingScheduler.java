/**
 * $Id$
 * 
 * $Log$
 * Revision 1.15  2019/03/14 19:12:56  mmaloney
 * Ignore DbIoException while purging old status.
 *
 * Revision 1.14  2018/03/30 14:13:32  mmaloney
 * Fix bug whereby DACQ_EVENTS were being written by RoutingScheduler with null appId.
 *
 * Revision 1.13  2017/03/30 21:04:44  mmaloney
 * Refactor CompEventServer to use PID if monitor==true.
 *
 * Revision 1.12  2016/02/23 19:26:39  mmaloney
 * -w arg to support operation as a windows service.
 *
 * Revision 1.11  2016/02/04 18:47:32  mmaloney
 * Ignore "-manual" schedule entries.
 *
 * Revision 1.10  2015/12/02 21:17:43  mmaloney
 * Make getStatistics protected to allow special apps like DcpMonitor to overload.
 *
 * Revision 1.9  2015/06/04 21:39:20  mmaloney
 * Added property spec for allowedHosts
 *
 * Revision 1.8  2015/02/06 18:46:24  mmaloney
 * RC03
 *
 * Revision 1.7  2015/01/16 16:11:04  mmaloney
 * RC01
 *
 * Revision 1.6  2014/12/11 20:28:09  mmaloney
 * Added DacqEventLogging capability.
 *
 * Revision 1.5  2014/09/15 14:00:54  mmaloney
 * Schedule Entry Refresh interval set to 60 seconds.
 *
 * Revision 1.4  2014/08/22 17:23:04  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 *
 * Copyright 2014 Cove Software, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.routing;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.ServerLock;
import ilex.util.StderrLogger;
import ilex.util.TextUtil;
import ilex.util.ThreadLogger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.db.ScheduleEntry;
import decodes.sql.SqlDatabaseIO;
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

/**
 * The main class for scheduling routing specs
 * @author mmaloney, Mike Maloney Cove Software, LLC
 */
public class RoutingScheduler 
	extends TsdbAppTemplate
	implements PropertiesOwner
{
	protected static String module = "RoutingScheduler";
	static StringToken lockFileArg = new StringToken("k", 
		"Optional Lock File", "", TokenOptions.optSwitch, "");
	static BooleanToken windowsSvcArg = new BooleanToken("w", "Run as Windows Service", "", 
		TokenOptions.optSwitch, false);

	/** Holds app name, id, & description. */
	protected CompAppInfo appInfo;

	/** My lock */
	protected TsdbCompLock myLock = null;
	protected boolean shutdownFlag = false;

	protected String hostname = null;
	
	/*** Purge entries older than this many days */
	public long purgeBeforeDays = 30;
	public static long MSEC_PER_DAY = 24 * 3600 * 1000L;
	
	/** Number of seconds at which to purge old status record from the database */
	public long oldStatusPurgeInterval = 3600L; // default = 1 hr.

	/** Number of seconds at which to refresh schedule entries from the database */
	public long refreshSchedInterval = 60L;   // default # sec.
	
	private PropertySpec[] myProps =
	{
		new PropertySpec("monitor", PropertySpec.BOOLEAN,
			"Set to true to allow monitoring from the GUI."),
		new PropertySpec("EventPort", PropertySpec.INT,
			"Open listening socket on this port to serve out app events."),
		new PropertySpec("purgeBeforeDays", PropertySpec.INT, 
			"Purge status entries older than this number of days (def=30)"),
		new PropertySpec("oldStatusPurgeInterval", PropertySpec.INT, 
			"Interval (sec) at which to purge old statuses (def=3600 or 1 hour)"),
		new PropertySpec("refreshSchedInterval", PropertySpec.INT,
			"Interval (sec) at which to check for schedule entry changes (def=60 or 1 min)"),
		new PropertySpec("allowedHosts", PropertySpec.STRING, 
			"comma-separated list of hostnames or ip addresses")
	};

	protected ArrayList<ScheduleEntryExecutive> executives = new ArrayList<ScheduleEntryExecutive>();
	protected ScheduleEntryDAI scheduleEntryDAO = null;
	private ThreadLogger appLogger = null;
	Logger origLogger = null;
	private CompEventSvr compEventSvr = null;
	private DacqEventDAI dacqEventDAO = null;
	
	public RoutingScheduler()
	{
		super("routsched.log");
		setSilent(true);
	}
	
	protected RoutingScheduler(String appName)
	{
		super(appName);
		setSilent(true);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		lockFileArg.setType("filename");
		cmdLineArgs.addToken(lockFileArg);
		cmdLineArgs.addToken(windowsSvcArg);
		appNameArg.setDefaultValue("RoutingScheduler");
	}
	
	@Override
	protected void oneTimeInit()
	{
		/** 
		 * Using lock files as an IPC mechanism (for status GUI) is unreliable in windoze.
		 * Tell server lock never to exit as a result of lock file I/O error.
		 */
		if (windowsSvcArg.getValue())
			ServerLock.setWindowsService(true);
		
		// Routing Scheduler can survive DB going down.
		surviveDatabaseBounce = true;

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

		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot determine hostname, will use 'localhost': " + ex);
			hostname = "localhost";
		}

	}
	
	@Override
	protected void runApp() 
	{
		Logger.instance().debug1("runApp starting");
		shutdownFlag = false;
		runAppInit();
		Logger.instance().debug1("runAppInit done, shutdownFlag=" + shutdownFlag 
			+ ", surviveDatabaseBounce=" + surviveDatabaseBounce);

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
					// MJM 20190314 ignore errors purging old status.
					// It was throwing a foreign key exception to DACQ_EVENT at AEP.
					try
					{
						Date purgeDate = new Date(System.currentTimeMillis() - purgeBeforeDays*MSEC_PER_DAY);
						
						if (dacqEventDAO != null)
							dacqEventDAO.deleteBefore(purgeDate);
						scheduleEntryDAO.deleteScheduleStatusBefore(appInfo, purgeDate);
					}
					catch(Exception ex) {}
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
			catch(DbIoException ex)
			{
				Logger.instance().warning(appNameArg.getValue() + " Exception while "
					+ action + ": " + ex);
				shutdownFlag = true;
				databaseFailed = true;
			}
			catch(Exception ex)
			{
				String msg = module + " Unexpected exception while " + action + ": " + ex;
				Logger.instance().warning(msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
				shutdownFlag = true;
			}
			finally
			{
				loadingAppDAO.close();
			}
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		runAppShutdown();
		
		Logger.instance().debug1("runApp() exiting.");
	}
	
	/**
	 * This method is called at the beginning of the runApp() method.
	 * <ul>
	 *   <li>Reading the Loading App info from the database</li>
	 *   <li>Return process ID passed on the command line</li>
	 *   <li>Open an event listening socket if one is specified in the app info</li>
	 * </ul>
	 */
	protected void runAppInit()
	{
		Logger.instance().debug1("runAppInit starting");
		// Get the loading app info from the DECODES database, not TSDB.
		DatabaseIO dbio = decodes.db.Database.getDb().getDbIo();
		LoadingAppDAI loadingAppDao = dbio.makeLoadingAppDAO();

		try
		{
			setAppId(loadingAppDao.lookupAppId(appNameArg.getValue()));
			appInfo = loadingAppDao.getComputationApp(appNameArg.getValue());
			if (!appInfo.canRunLocally())
			{
				Logger.instance().fatal("The 'allowedHosts' property for application '" + appInfo.getAppName()
					+ "' does not allow this application to run on this machine!");
				shutdownFlag = true;
				return;
			}
			loadConfig(appInfo.getProperties());

			// If this process can be monitored, start an Event Server.
			if (TextUtil.str2boolean(appInfo.getProperty("monitor")) && compEventSvr == null)
			{
				try 
				{
					compEventSvr = new CompEventSvr(determineEventPort(appInfo));
					compEventSvr.startup();
				}
				catch(IOException ex)
				{
					Logger.instance().failure(
						"Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}
			
			if (loadingAppDao.supportsLocks())
			{
				try { myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); }
				catch(LockBusyException ex)
				{
					shutdownFlag = true;
					Logger.instance().fatal(getAppName() + " runAppInit: lock busy: " + ex);
				}
			}


		}
		catch(NoSuchObjectException ex)
		{
			Logger.instance().fatal(getAppName() + " runAppInit: " + ex);
			shutdownFlag = true;
			return;
		}
		catch(DbIoException ex)
		{
			Logger.instance().fatal(getAppName() + " runAppInit: " + ex);
			databaseFailed = true;
			shutdownFlag = true;
			return;
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
		if (dbio instanceof SqlDatabaseIO)
		{
			SqlDatabaseIO sqlDbio = (SqlDatabaseIO)dbio;
			dacqEventDAO = sqlDbio.makeDacqEventDAO(); // will be null for < db version 10
		}
	}
	
	protected void loadConfig(Properties properties)
	{
		PropertiesUtil.loadFromProps(this, properties);
	}

	/**
	 * Called from runApp() prior to returning.
	 */
	protected void runAppShutdown()
	{
		for(ScheduleEntryExecutive executive : executives)
			executive.shutdown();
		executives.clear();
		try { Thread.sleep(5000L); }
		catch(InterruptedException ex) {}
		
		if (scheduleEntryDAO != null)
			scheduleEntryDAO.close();
		scheduleEntryDAO = null;
		
		if (dacqEventDAO != null)
		{
			dacqEventDAO.close();
			dacqEventDAO = null;
		}
	}
	
	public String getAppName()
	{
		return appInfo != null ? appInfo.getAppName() : appNameArg.getValue();
	}

	
	protected void refreshSchedule() 
		throws DbIoException
	{
		// read list of ScheduleEntry's for my loading app.
		ArrayList<ScheduleEntry> dbEntries = scheduleEntryDAO.listScheduleEntries(appInfo);
		for(Iterator<ScheduleEntry> seit = dbEntries.iterator(); seit.hasNext(); )
		{
			ScheduleEntry se = seit.next();
			if (se.getName() != null && se.getName().toLowerCase().endsWith("-manual"))
				seit.remove();
		}
		
		ArrayList<ScheduleEntryExecutive> newExecs = new ArrayList<ScheduleEntryExecutive>();
		for(ScheduleEntry dbEntry : dbEntries)
		{
			boolean running = false;
			for(Iterator<ScheduleEntryExecutive> execit = executives.iterator(); execit.hasNext(); )
			{
				ScheduleEntryExecutive exec = execit.next();
				// Have to match on name to accommodate xml.
				if (exec.getScheduleEntry().getName().equalsIgnoreCase(dbEntry.getName()))
				{
					if (exec.getScheduleEntry().getLastModified().before(dbEntry.getLastModified()))
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
	
	protected String getStatistics()
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
		Logger.instance().debug1("initDecodes()");
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
	public void tryConnect()
	{
		// Do nothing. The scheduler must not use the TSDB.
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
	 * to execute the routing spec. Register the thread's specific logger.
	 * @param thread the ScheduleEntryThread that is about to start.
	 */
	public void setThreadLogger(Thread thread, Logger logger)
	{
		appLogger.setLogger(thread, logger);
	}
	
	public void threadFinished(Thread thread)
	{
		if (appLogger != null)
			appLogger.setLogger(thread, null);
	}

	public String getHostname()
	{
		return hostname;
	}

	public DacqEventDAI getDacqEventDAO()
	{
		return dacqEventDAO;
	}
}
