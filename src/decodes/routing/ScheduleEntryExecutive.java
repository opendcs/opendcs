package decodes.routing;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import lrgs.common.DcpMsg;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;

/**
 * This class supervises the execution of a routing spec according to a 
 * ScheduleEntry stored in the database. It handles the scheduling, constructs
 * the ScheduleeEntryThread at the appropriate time(s), and provides a link
 * to the daemon's resources.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class ScheduleEntryExecutive
{
	/** The schedule entry this executive is responsible for */
	private ScheduleEntry scheduleEntry = null;
	
	/** The current run state of this executive */
	private RunState runState = RunState.initializing;
	
	/** The RS Thread or null when no RoutingSpec is running */
	private RoutingSpecThread seThread = null;

	/** Status of latest, or currently running routing spec */
	private ScheduleEntryStatus seStatus = null;
	TimeZone jtz = null;
	
	private RoutingScheduler parent = null;
	
	private long shutdownStarted = 0L;
	private long maxShutdownTime = 20000L; // allow 20 sec to gracefully shut down.
	private long shutdownComplete = 0L;
	
	protected static boolean rereadRsBeforeExec = true;
	private DcpMsg lastDcpMsg = null;
	private DacqEventLogger dacqEventLogger = null;
	private int schedEntryMinLogPriority = Logger.E_INFORMATION;
	private long lastEventsPurge = 0L;
	
	/** Constructor called from RoutingScheduler */
	public ScheduleEntryExecutive(ScheduleEntry scheduleEntry, RoutingScheduler parent)
	{
		this.scheduleEntry = scheduleEntry;
		this.parent = parent;
		if (parent == null && (Database.getDb().getDbIo() instanceof SqlDatabaseIO))
		{
			// This means that this is a stand-alone rs from command line, the DCPmon
			// daemon, or some other single-threaded app.
			SqlDatabaseIO sqlDbio = (SqlDatabaseIO)Database.getDb().getDbIo();
			DacqEventDAI dacqEventDAO = sqlDbio.makeDacqEventDAO();
			dacqEventLogger = new DacqEventLogger(Logger.instance());
			dacqEventLogger.setDacqEventDAO(dacqEventDAO);
			Logger.setLogger(dacqEventLogger);
		}
		else // This is a child of RoutingScheduler
		{
			dacqEventLogger = new DacqEventLogger(parent.origLogger);
			dacqEventLogger.setDacqEventDAO(parent.getDacqEventDAO());
		}
	}
	
	/**
	 * Called periodically from RoutingScheduler, this method checks the status
	 * and schedule of the entry it is managing and takes appropriate action,
	 * such as starting/stopping the thread, etc.
	 */
	public void check()
	{
		if (!scheduleEntry.isEnabled())
		{
			dacqEventLogger.debug1("Checking schedule entry '" + scheduleEntry.getName() 
				+ "' enabled=" + scheduleEntry.isEnabled());
			return;
		}
		dacqEventLogger.debug1("Checking schedule entry '" + scheduleEntry.getName() 
			+ "' enabled=" + scheduleEntry.isEnabled() + ", state=" + runState);

		if (runState == RunState.initializing)
		{
			try
			{
				initialize();
				runState = RunState.waiting;
			}
			catch (DbIoException ex)
			{
				dacqEventLogger.warning(getName() + " Cannot initialize: " + ex);
				// Stay in initialize state and retry later.
			}
			return;
		}
		else if (runState == RunState.waiting)
		{
			if (checkSchedule())
				startThread();
		}
		else if (runState == RunState.running)
		{
		}
		else if (runState == RunState.shutdown)
		{
			if (System.currentTimeMillis() - shutdownStarted > maxShutdownTime
			 && seThread != null)
			{
				dacqEventLogger.warning(getName() + " taking too long to shutdown, "
					+ "will attempt thread interrupt.");
				seThread.interrupt();
			}
			
			// A continuous schedule entry might exit for various transient errors.
			// Example, a timeout on an LRGS data source.
			// Attempt to restart after one minute if it does.
			long now = System.currentTimeMillis();
			if (scheduleEntry.getStartTime() == null                    // means continuous
			 && shutdownComplete != 0L                                  // Completely shutdown
			 && now - shutdownComplete > 60000L  // over a minute ago
			 && seStatus.getRunStatus().contains("ERR")                 // Exited due to error
			 && !dataSourceFinite())                                    // data source not a file
			{
				runState = RunState.initializing;
				RoutingSpec rs = Database.getDb().routingSpecList.find(
					scheduleEntry.getRoutingSpecName());
				if (rs != null && lastDcpMsg != null)
					rs.sinceTime = IDateFormat.toString(lastDcpMsg.getXmitTime(), false);
			}
else
{
dacqEventLogger.debug1("Sched Entry '" + scheduleEntry.getName()
	+ "' startTime=" + scheduleEntry.getStartTime()
	+ ", shutdownComplete=" + shutdownComplete
	+ ", now=" + now
	+ ", seStatus=" + seStatus.toString()
	+ ", dataSourceFinite=" + dataSourceFinite());
}
		}
		else // runState == complete.
		{
		}
	}
	
	/**
	 * A 'finite' data source is like a file. Once it's processed it's done.
	 * @return
	 */
	private boolean dataSourceFinite()
	{
		// A real-time routing spec that is enabled and has a streaming data
		// source like an LRGS or directory should never complete.
		RoutingSpec rs = Database.getDb().routingSpecList.find(
			scheduleEntry.getRoutingSpecName());
		if (rs == null || rs.dataSource == null)
		{
			return true;
		}
		if (rs.untilTime != null && rs.untilTime.trim().length() > 0)
		{
			return true;
		}
		String dsType = rs.dataSource.dataSourceType.toLowerCase();
		if (dsType.equals("lrgs") || dsType.equals("hotbackupgroup") || dsType.equals("directory"))
			return false;
		return true;
	}
		
	/**
	 * One time initialization
	 * @throws DbIoException 
	 */
	void initialize() 
		throws DbIoException
	{
		shutdownComplete = 0L;
		// Lookup the lastRunStart
		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		try
		{
			if (scheduleEntryDAO != null)
				seStatus = scheduleEntryDAO.getLastScheduleStatusFor(scheduleEntry);
			String stz = scheduleEntry.getTimezone();
			if (stz == null)
				stz = "UTC";
			jtz = TimeZone.getTimeZone(stz);
			if (jtz == null)
				jtz = TimeZone.getDefault();
		}
		finally
		{
			if (scheduleEntryDAO != null)
				scheduleEntryDAO.close();
		}
	}
	
	/**
	 * Check the schedule and return true if it's time to start the routing spec
	 */
	private boolean checkSchedule()
	{
		Date startTime = scheduleEntry.getStartTime();
		
		if (startTime == null) // means start immediately
			return true;
		
		Date now = new Date();
		if (now.before(startTime))
			return false;
		
		if (seStatus == null)
			// It is after start time and it has never been run.
			return true;
		
		Date lastRunStart = seStatus.getRunStart();
		Calendar cal = Calendar.getInstance(jtz);
		cal.setTime(lastRunStart);
		
		String intv = scheduleEntry.getRunInterval();
		if (intv == null)
			// Use case: user re-enables a one-time entry that has already run.
			return true;
		
		IntervalIncrement intinc = IntervalIncrement.parse(intv);
		if (intinc.isLessThanDay())
		{
			// Set cal to same time as start time on current day.
			Calendar startCal = Calendar.getInstance(jtz);
			startCal.setTime(startTime);
		
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.SECOND, startCal.get(Calendar.SECOND));
			cal.set(Calendar.MINUTE, startCal.get(Calendar.MINUTE));
			cal.set(Calendar.HOUR_OF_DAY, startCal.get(Calendar.HOUR_OF_DAY));
			
			// Now increment back until we're before last run time
			while (cal.getTime().after(lastRunStart))
				cal.add(intinc.getCalConstant(), -intinc.getCount());
			
			// We are before last run. Now increment forward until we're just after it.
			while (cal.getTime().before(lastRunStart))
				cal.add(intinc.getCalConstant(), intinc.getCount());
		}
		else
		{
			// interval is more than a day. Set to schedule entry start time,
			// then increment forward until we're just after last run.
			cal.setTime(startTime);
			while (!cal.getTime().after(lastRunStart))
				cal.add(intinc.getCalConstant(), intinc.getCount());
		}
		
		Date nextRunTime = cal.getTime();
		dacqEventLogger.debug1(getName() + " next run time = " + nextRunTime);

		// Check if current time is after the next run time we computed.
		return !now.before(nextRunTime);
	}

	/**
	 * Called from RoutingScheduler when either A.) it is exiting, or
	 * B.) the ScheduleEntry for this executive has been modified or deleted.
	 * This method shuts down the routing spec thread if one is running. It
	 * then writes the final status record.
	 */
	public void shutdown()
	{
		if (seThread != null)
		{
			seThread.shutdown();
			runState = RunState.shutdown;
			shutdownStarted = System.currentTimeMillis();
		}
		
		if (parent == null && dacqEventLogger != null)
			dacqEventLogger.getDacqEventDAO().close();
	}

	RoutingSpecThread makeThread()
	{
		// Get the routing spec from the database
		RoutingSpec rs = Database.getDb().routingSpecList.find(
			scheduleEntry.getRoutingSpecName());
		if (rs == null)
		{
			dacqEventLogger.failure("ScheduleEntryExec.makeThread: "
				+ "No such routing spec '" + scheduleEntry.getRoutingSpecName()
				+ "' in database.");
			disableScheduleEntry();
			return null;
		}

		if (rereadRsBeforeExec)
		{
			Logger.instance().info("ScheduleEntryExecutive.makeThread -- rereading Routing Spec.");
			try { rs.read(); }
			catch (DatabaseException ex)
			{
				dacqEventLogger.info("ScheduleEntryExec.makeThread: "
					+ " Routing Spec '" + scheduleEntry.getRoutingSpecName()
					+ "' no longer exists in database, disabling the schedule entry: " + ex);
				disableScheduleEntry();
			}
		}
		
		String s = rs.getProperty("debugLevel");
		if (s != null)
		{
			try
			{
				int dblev = Integer.parseInt(s);
				schedEntryMinLogPriority =
					dblev == 1 ? Logger.E_DEBUG1 :
					dblev == 2 ? Logger.E_DEBUG2 :
					dblev == 3 ? Logger.E_DEBUG3 : Logger.E_INFORMATION;
				// debugLevel property should only be used to increase debug level. Never decrease.
				if (schedEntryMinLogPriority > Logger.instance().getMinLogPriority())
					schedEntryMinLogPriority = Logger.instance().getMinLogPriority();
				dacqEventLogger.setMinLogPriority(schedEntryMinLogPriority);
			}
			catch(NumberFormatException ex)
			{
				dacqEventLogger.warning("RoutingSpec " + rs.getName() + " Ignoring invalid debugLevel property '" 
					+ s + "' -- must be 1, 2, or 3.");
			}
		}

		// Allocate a new status structure
		seStatus = new ScheduleEntryStatus(Constants.undefinedId);
		seStatus.setScheduleEntryId(scheduleEntry.getId());
		seStatus.setScheduleEntryName(scheduleEntry.getName());
		seStatus.setRunStart(new Date());
		if (parent != null)
			seStatus.setHostname(parent.getHostname());
		else
			try
			{
				seStatus.setHostname(InetAddress.getLocalHost().getHostName());
			}
			catch (UnknownHostException ex)
			{
				dacqEventLogger.warning("Cannot get hostname: " + ex);
				seStatus.setHostname("unknown");
			}
		runState = RunState.running;
		writeStatus("Starting");
		
		seThread = new RoutingSpecThread(rs);
		seThread.scheduleEntryStatusId = seStatus.getId();
		seThread.setMyExec(this);
		seThread.setMyStatus(seStatus);
		seThread.closeDbOnQuit = false;
		seThread.doRoutingSpecCheck = rereadRsBeforeExec;

		if (parent != null)
			parent.setThreadLogger(seThread, dacqEventLogger);
		return seThread;
	}
	
	/**
	 * Called from the check() method when it is time to start the routing
	 * spec thread.
	 */
	void startThread()
	{
		makeThread();
		if (seThread != null)
			seThread.start();
	}
	
	/** Write the status structure to the database */
	public void writeStatus(String status)
	{
		seStatus.setLastModified(new Date());
		seStatus.setRunStatus(runState.toString() + " " + status);
		ScheduleEntryDAI scheduleEntryDAO = null;
		try
		{
			scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
			dacqEventLogger.setSchedEntryStatusId(seStatus.getId());
		}
		catch(NullPointerException ex)
		{
			dacqEventLogger.debug1("transient error shutting down: " + ex);
			return;
		}
		if (scheduleEntryDAO == null)
		{
			dacqEventLogger.debug1("Cannot write schedule entry. Not supported on this database.");
			return;
		}

		try
		{
			scheduleEntryDAO.writeScheduleStatus(seStatus);
		}
		catch (DbIoException ex)
		{
			dacqEventLogger.warning(getName() + " Cannot write status: " + ex);
		}
		finally
		{
			scheduleEntryDAO.close();
		}
	}
	
	/** 
	 * Called from my ScheduleEntryThread when it is finished.
	 */
	public void rsFinished()
	{
		// null interval and non-null start time means run-once-only
		if (scheduleEntry.getRunInterval() == null
		 && scheduleEntry.getStartTime() != null)
		{
			// If this is a one-time RS, set runState to complete and disable.
			runState = RunState.complete;
			disableScheduleEntry();
		}
		else if (scheduleEntry.getRunInterval() != null)
		{
			// A periodic SE has finished for now and is waiting for the next run.
			runState = RunState.waiting;
		}
		else // A realtime SE has terminated because the scheduler is going down.
			runState = RunState.shutdown;

		// This will cause it to write the final status to this.writeStatus() below
		seThread.writeStatus();
		if (parent != null)
			parent.threadFinished(seThread);
		lastDcpMsg = seThread.getLastDcpMsg();
		seThread = null;
		shutdownComplete = System.currentTimeMillis();
		dacqEventLogger.setSchedEntryStatusId(DbKey.NullKey);
	}
	
	public RunState getRunState()
	{
		return runState;
	}

	public void setRunState(RunState runState)
	{
		this.runState = runState;
	}

	public ScheduleEntry getScheduleEntry()
	{
		return scheduleEntry;
	}
	
	public String getName()
	{
		return "SchedEntry:" + scheduleEntry.getName();
	}
	
	/**
	 * Disable this ScheduleEntry in the database.
	 */
	private void disableScheduleEntry()
	{
		scheduleEntry.setEnabled(false);
		ScheduleEntryDAI scheduleEntryDAO = Database.getDb().getDbIo().makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
		{
			dacqEventLogger.debug1("Schedule entries not supported on this database.");
			return;
		}

		try
		{
			scheduleEntryDAO.checkScheduleEntry(scheduleEntry);
			scheduleEntryDAO.writeScheduleEntry(scheduleEntry);
		}
		catch (Exception ex)
		{
			dacqEventLogger.failure("Cannot disable schedule entry: " + ex);
		}
		finally
		{
			scheduleEntryDAO.close();
		}
	}

	public static void setRereadRsBeforeExec(boolean tf)
	{
		rereadRsBeforeExec = tf;
	}
	
	public void setPlatform(Platform p)
	{
		if (p == null)
		{
			dacqEventLogger.setPlatformId(DbKey.NullKey);
			dacqEventLogger.setMinLogPriority(schedEntryMinLogPriority);
		}
		else
		{	
			dacqEventLogger.setPlatformId(p.getId());
			int dblev = p.getDebugLevel();
			int newMinPriority =
				dblev == 1 ? Logger.E_DEBUG1 :
				dblev == 2 ? Logger.E_DEBUG2 :
				dblev == 3 ? Logger.E_DEBUG3 : schedEntryMinLogPriority;
			dacqEventLogger.setMinLogPriority(newMinPriority);
		}
	}
	
	public void setSubsystem(String subsystem)
	{
		dacqEventLogger.setSubsystem(subsystem);
	}

	public long getLastEventsPurge()
	{
		return lastEventsPurge;
	}

	public void setLastEventsPurge(long lastEventsPurge)
	{
		this.lastEventsPurge = lastEventsPurge;
	}
}
