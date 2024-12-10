/*
*  $Id: ComputationApp.java,v 1.22 2020/05/07 13:31:33 mmaloney Exp $
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log: ComputationApp.java,v $
*  Revision 1.22  2020/05/07 13:31:33  mmaloney
*  Read presentation groups on init. The TS DAOs use these to determine proper units when creating
*  a new time series.
*
*  Revision 1.21  2019/10/29 13:44:17  mmaloney
*  Only derive missing checks from ENABLED computations for this app.
*
*  Revision 1.20  2019/08/26 20:46:22  mmaloney
*  Implement missing alarm checks.
*
*  Revision 1.19  2019/08/19 14:54:30  mmaloney
*  Added mail server settings to standard compproc properties list.
*
*  Revision 1.18  2019/08/07 14:18:35  mmaloney
*  Added Missing Checks
*
*  Revision 1.17  2019/07/02 13:57:14  mmaloney
*  Rename method for clarity.
*
*  Revision 1.16  2019/05/15 22:27:31  mmaloney
*  HDB 681 null ptr fix
*
*  Revision 1.15  2019/01/10 16:02:28  mmaloney
*  Special case for cloning when the algorithm is GroupAdder.
*
*  Revision 1.14  2018/11/14 15:48:36  mmaloney
*  OpenDCS 6.5 RC03 Support for Timed Computations.
*
*  Revision 1.13  2018/03/30 14:57:11  mmaloney
*  Fix bug whereby DACQ_EVENTS were being written by RoutingScheduler with null appId.
*
*  Revision 1.12  2018/03/30 14:13:32  mmaloney
*  Fix bug whereby DACQ_EVENTS were being written by RoutingScheduler with null appId.
*
*  Revision 1.11  2018/02/19 16:23:57  mmaloney
*  Attempt to reclaim tasklist space if tasklist is empty and feature is enabled.
*
*  Revision 1.10  2018/02/19 15:49:41  mmaloney
*  Do periodic cache maintenance every 2 hours.
*  Only pause for 1 sec in the main loop if the data collection was empty.
*  (otherwise read the next batch of data immediately).
*
*  Revision 1.9  2017/12/14 17:06:27  mmaloney
*  Refactor so that LoadingAppDAO and TimeSeriesDAO are not recreated for each time through the loop.
*
*  Revision 1.8  2017/03/30 21:07:18  mmaloney
*  Refactor CompEventServer to use PID if monitor==true.
*
*  Revision 1.7  2016/06/27 15:26:37  mmaloney
*  Have to read data types as part of decodes init.
*
*  Revision 1.6  2016/04/22 14:38:40  mmaloney
*  Skip resolving and saving results if the tasklist set is empty.
*
*  Revision 1.5  2016/03/24 19:09:18  mmaloney
*  Added instance() method needed by Python Algorithm.
*
*  Revision 1.4  2015/04/02 18:16:19  mmaloney
*  Added property definitions.
*
*  Revision 1.3  2014/08/22 17:23:04  mmaloney
*  6.1 Schema Mods and Initial DCP Monitor Implementation
*
*  Revision 1.2  2014/07/10 17:07:54  mmaloney
*  Remove startup log from ComputationApp, and add to TsdbAppTemplate.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.28  2013/07/12 11:50:53  mmaloney
*  Added tasklist queue stuff.
*
*  Revision 1.27  2013/07/09 19:01:24  mmaloney
*  If database goes away and reconnection is done, also recreate the resolver.
*
*  Revision 1.26  2013/03/28 19:07:24  mmaloney
*  Implement cmd line arg -O OfficeID
*
*  Revision 1.25  2013/03/25 18:15:03  mmaloney
*  Refactor starting event server.
*
*  Revision 1.24  2013/03/25 17:08:43  mmaloney
*  event port fix
*
*  Revision 1.23  2013/03/25 16:58:26  mmaloney
*  Refactor comp lock stale time.
*
*  Revision 1.22  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.net.InetAddress;

import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DbObjectCache;
import opendcs.opentsdb.Interval;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import decodes.sql.DbKey;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmManager;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.alarm.AlarmScreeningAlgorithm;


/**
ComputationApp is the main module for the background comp processor.
*/
public class ComputationApp
	extends TsdbAppTemplate
{
	/** Holds app name, id, & description. */
	CompAppInfo appInfo;

	/** My lock */
	private TsdbCompLock myLock;
	
	/** My resolver */
	private DbCompResolver resolver;
	
	private boolean shutdownFlag;

	private String hostname;
	private int compsTried = 0;
	private int compErrors = 0;
//	private int evtPort = -1;
	
	private BooleanToken regressionTestModeArg = new BooleanToken("T", "Regression Test Mode",
		"", TokenOptions.optSwitch, false);
	private StringToken officeIdArg = new StringToken(
		"O", "OfficeID", "", TokenOptions.optSwitch, "");
	private CompEventSvr compEventSvr = null;
	
	private ArrayList<DbComputation> timedComps = new ArrayList<DbComputation>();
	private int checkTimedCompsSec = 600;
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	private long compRunWaitTime = 30000L; // wait 30 seconds between runs by default. Drastically reduces system load
										   // especially with multiple instances pointing to a single database.
	
	private static ComputationApp _instance = null;
	public static ComputationApp instance() { return _instance; }
	
	private PropertySpec[] myProps =
	{
		new PropertySpec("monitor", PropertySpec.BOOLEAN,
			"Set to true to allow monitoring from the GUI."),
		new PropertySpec("EventPort", PropertySpec.INT,
			"Open listening socket on this port to serve out app events."),
		new PropertySpec("reclaimTasklistSec", PropertySpec.INT,
			"(default=0) if set to a positive # of seconds, then when the tasklist is "
			+ "empty and this # of seconds has elapsed, shrink the allocated space for the "
			+ "tasklist back to something reasonable (Oracle only)."),
		new PropertySpec("checkTimedCompsSec", PropertySpec.INT,
			"(default=600, or 10 minutes) check for changes to timed computations "
			+ "every this number of seconds."),
		
		new PropertySpec("mail.smtp.auth", PropertySpec.BOOLEAN, "(default=false) "
			+ "If true then authenticate when connecting to mail server."),
		new PropertySpec("mail.smtp.starttls.enable", PropertySpec.BOOLEAN, "(default=false) "
			+ "Use TLS for a secure connection to the mail server."),
		new PropertySpec("mail.smtp.host", PropertySpec.HOSTNAME, "(required) "
			+ "Host name or IP address of the mail server."),
		new PropertySpec("mail.smtp.port", PropertySpec.INT, "(default=587) "
			+ "Port number for connecting to the mail server"),
		new PropertySpec("smtp.username", PropertySpec.STRING, 
			"User name for authenticating to the mail server"),
		new PropertySpec("smtp.password", PropertySpec.STRING, 
			"Password for authenticating to the mail server"),
		new PropertySpec("fromAddr", PropertySpec.STRING, 
			"Email address for the 'from' field of the header"),
		new PropertySpec("fromName", PropertySpec.STRING, 
			"Name for the 'from' field of the header"),
		new PropertySpec("resendSeconds", PropertySpec.INT, "(default=86400) "
			+ "Resend email for existing alarms if they remain asserted this long. "
			+ "(-1 to disable resend)"),
		new PropertySpec("notifyMaxAgeDays", PropertySpec.INT, "(default=30) "
			+ "Do not send email notifications for data older than this."),
		new PropertySpec("compRunWaitTime", PropertySpec.INT,"(default=1000"
			+ "Amount of time (in milliseconds) when Idle to wait before checking for new data"
		)
		
		
	};
	
	/**
	 * Holds info about Screening computations that do a Missing Check
	 */
	private class MissingCheck
	{
		TimeSeriesIdentifier tsid;
		long nextRunMsec;
		DbComputation comp;
		AlarmScreening screening;
		AlarmLimitSet limitSet;
		boolean checked = false;
		Date lastModifiedInDb = null;

		public MissingCheck(TimeSeriesIdentifier tsid, long nextRunMsec, DbComputation comp, AlarmScreening screening,
				AlarmLimitSet limitSet, Date lastModifiedInDb)
		{
			super();
			this.tsid = tsid;
			this.nextRunMsec = nextRunMsec;
			this.comp = comp;
			this.screening = screening;
			this.limitSet = limitSet;
			this.lastModifiedInDb = lastModifiedInDb;
		}
	}
	
	ArrayList<MissingCheck> missingChecks = new ArrayList<MissingCheck>();
	private long lastMissingCheckMsec = 0L;

	private Calendar timedCompCal;
	
	/**
	 * Constructor called from main method after parsing arguments.
	 */
	public ComputationApp()
	{
		super("compproc.log");
		myLock = null;
		resolver = null;
		shutdownFlag = false;
	}

	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(regressionTestModeArg);
		appNameArg.setDefaultValue("compproc");
		cmdLineArgs.addToken(officeIdArg);
	}
	
	@Override
	protected void oneTimeInit()
	{
		// Comp Proc can survive DB going down.
		surviveDatabaseBounce = true;
		
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "unknown"; }

		if (officeIdArg.getValue() != null && officeIdArg.getValue().length() > 0)
			DecodesSettings.instance().CwmsOfficeId = officeIdArg.getValue();
	}


	/** @return the application name. */
	public String getAppName() 
	{
		return appInfo.getAppName(); 
	}

	/** @return the application comment. */
	public String getAppComment() 
	{
		return appInfo.getComment(); 
	}

	/**
	 * The application run method. Called after all initialization methods
	 * by the base class.
	 * @throws LockBusyException if another process has the lock
	 * @throws DbIoException on failure to access the database
	 * @throws NoSuchObjectException if the application is invalid.
	 */
	public void runApp( )
		throws LockBusyException, DbIoException, NoSuchObjectException
	{
		Logger.instance().debug1("runApp starting");
		_instance = this;
		shutdownFlag = false;
		
		TimeZone dbtz = TimeZone.getTimeZone(theDb.databaseTimezone);
		timedCompCal = Calendar.getInstance();
		timedCompCal.setTimeZone(dbtz);
		
		runAppInit();
		Logger.instance().debug1("runAppInit done, shutdownFlag=" + shutdownFlag 
			+ ", surviveDatabaseBounce=" + surviveDatabaseBounce);

		long lastDataTime = System.currentTimeMillis();
		long lastLockCheck = 0L;
		long lastCacheMaintenance = System.currentTimeMillis();
		long lastTimedCompCheck = 0L;

		String action="starting";
		
		
		try
		{
			while(!shutdownFlag)
			{
				Logger.instance().debug3("ComputationApp start of main loop.");
				try(// TODO: This is a good place to hook a transaction. Still need to be implemented though before we can demo it
					TimeSeriesDAI timeSeriesDAO = db.getDao(TimeSeriesDAI.class).get();
					LoadingAppDAI loadingAppDAO = db.getDao(LoadingAppDAI.class).get();
					TsGroupDAI tsGroupDAO = theDb.makeTsGroupDAO();
					)
				{
					long now = System.currentTimeMillis();
	
					action = "Checking lock";
					if (myLock != null && now - lastLockCheck > 5000L)
					{
						setAppStatus("Cmps: " + compsTried + "/" + compErrors);
						loadingAppDAO.checkCompProcLock(myLock);
						lastLockCheck = now;
					}
					
					if (now - lastCacheMaintenance > 3600 * 2 * 1000L)
					{
						lastCacheMaintenance = now;
						refillSiteCache();
					}
					
					if (now - lastTimedCompCheck > checkTimedCompsSec * 1000L) 
					{
						checkTimedCompList(now, timedCompCal);
						lastTimedCompCheck = now;
					}
					
					// Check to see if it's time to run any timed computations.
					action = "Running timed computations";
					DataCollection dataCollection = null;
					int numTimed = 0;
					for(Iterator<DbComputation> tcit = timedComps.iterator(); tcit.hasNext(); )
					{
						DbComputation tc = tcit.next();
						if (now >= tc.getNextRunTime().getTime())
						{
							if (dataCollection == null)
								dataCollection = new DataCollection();
							executeTimedComp(tc, timedCompCal, dataCollection, timeSeriesDAO, tsGroupDAO);
							numTimed++;
							Date nextRun = computeNextRunTime(
								tc.getProperty("timedCompInterval"),
								tc.getProperty("timedCompOffset"), timedCompCal, 
								new Date(now + 1000L));
							if (nextRun == null)
							{
								warning("Cannot compute nextRun for computation "
									+ tc.getId() + ": " + tc.getName()
									+ " with interval '" + tc.getProperty("timedCompInterval") + "'"
									+ " -- is this no longer a timed computation?");
								tcit.remove();
							}
							else
							{
								tc.setNextRunTime(nextRun);
								Logger.instance().debug3("Computation " + tc.getKey() + ":" + tc.getName()
									+ " scheduled for " + debugSdf.format(nextRun));
							}
						}
					}
					if (numTimed > 0)
					{
						action = "Saving timed results";
						List<CTimeSeries> tsList = dataCollection.getAllTimeSeries();
	Logger.instance().debug3(action + " " + tsList.size() +" time series in data.");
	
						for(CTimeSeries cts : tsList)
						{
							try { timeSeriesDAO.saveTimeSeries(cts); }
							catch(BadTimeSeriesException ex)
							{
								warning("Cannot save time series " + cts.getNameString()
									+ ": " + ex);
							}
						}
	
					}
	
					action = "Getting new data";
					dataCollection = timeSeriesDAO.getNewData(getAppId());
					// In Regression Test Mode, exit after 5 sec of idle
					if (!dataCollection.isEmpty())
						lastDataTime = System.currentTimeMillis();
					else if (regressionTestModeArg.getValue()
					 && System.currentTimeMillis() - lastDataTime > 10000L)
					{
						Logger.instance().info("Regression Test Mode - Exiting after 10 sec idle.");
						shutdownFlag = true;
						loadingAppDAO.releaseCompProcLock(myLock);
					}
					
	
					if (!dataCollection.isEmpty())
					{
						action = "Resolving computations";
						DbComputation comps[] = resolver.resolve(dataCollection);
		
						action = "Applying computations";
						for(DbComputation comp : comps)
						{
							Logger.instance().debug1("Trying computation '" 
								+ comp.getName() + "' #trigs=" + comp.getTriggeringRecNums().size());
							compsTried++;
							try
							{
								comp.prepareForExec(theDb);
								comp.apply(dataCollection, theDb);
							}
							catch(DbCompException ex)
							{
								String msg = "Computation '" + comp.getName() 
									+ "' DbCompException: " + ex;
								warning(msg);
								compErrors++;
								for(Integer rn : comp.getTriggeringRecNums())
									 dataCollection.getTasklistHandle().markComputationFailed(rn);
							}
							catch(Exception ex)
							{
								compErrors++;
								String msg = "Computation '" + comp.getName() 
									+ "' Exception: " + ex;
								warning(msg);
								System.err.println(msg);
								ex.printStackTrace(System.err);
								for(Integer rn : comp.getTriggeringRecNums())
									 dataCollection.getTasklistHandle().markComputationFailed(rn);
							}
							comp.getTriggeringRecNums().clear();
							Logger.instance().debug1("End of computation '" 
								+ comp.getName() + "'");
						}
		
						action = "Saving results";
						List<CTimeSeries> tsList = dataCollection.getAllTimeSeries();
		Logger.instance().debug3(action + " " + tsList.size() +" time series in data.");
						for(CTimeSeries ts : tsList)
						{
							try { timeSeriesDAO.saveTimeSeries(ts); }
							catch(Exception ex)
							{
								warning("Cannot save time series " + ts.getNameString()
									+ ": " + ex);
							}
						}
		
						action = "Releasing new data";
						theDb.releaseNewData(dataCollection, timeSeriesDAO);
						lastDataTime = System.currentTimeMillis();
					}
					else // MJM 6.4 RC08 Only sleep if data was empty.
					{
						try { Thread.sleep(compRunWaitTime); }
						catch(InterruptedException ex) {}
					}
		
					if (!theDb.isCwms())
					{
						now = System.currentTimeMillis();
						int idx = 0;
						for(; idx < missingChecks.size() && now > missingChecks.get(idx).nextRunMsec; idx++)
							doMissingCheck(missingChecks.get(idx), timeSeriesDAO);
						if (idx > 0)
							sortMissingChecks();
						if (now - lastMissingCheckMsec > 600L * 1000L) // ten minutes
						{
							checkMissingChecks(timeSeriesDAO, tsGroupDAO);
							lastMissingCheckMsec = now;
						}
					}
				}
			}
		}
		catch(LockBusyException ex)
		{
			Logger.instance().fatal("No Lock - Application exiting: " + ex);
			shutdownFlag = true;
		}
		catch(DbIoException ex)
		{
			warning("Database Error while " + action + ": " + ex);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			warning(sw.toString());
			shutdownFlag = true;
			databaseFailed = true;
		}
		catch(Exception ex)
		{
			String msg = "Unexpected exception while " + action + ": " + ex;
			warning(msg);
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			warning(sw.toString());
			shutdownFlag = true;
		}
		resolver = null;
		Logger.instance().debug1("runApp() exiting.");
	}


	/**
	 * MJM Added for 6.4 RC08 to refresh site cache every 2 hours.
	 */
	private void refillSiteCache()
	{
		/* few apps will actually ever need everything. */
	}

	/**
	 * Called at the start of the runApp() method, which is called by the base
	 * class after connecting to the database.
	 * @throws DbIoException
	 * @throws NoSuchObjectException
	 */
	private void runAppInit()
	{
		debugSdf.setTimeZone(TimeZone.getTimeZone(theDb.databaseTimezone));
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		TimeSeriesDAI tsDAO = theDb.makeTimeSeriesDAO();
		TsGroupDAI groupDAO = theDb.makeTsGroupDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(getAppId());

			// Construct the resolver & load it.
			resolver = new DbCompResolver(theDb);
			
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
					failure("Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}

			try { myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); }
			catch(LockBusyException ex)
			{
				shutdownFlag = true;
				Logger.instance().fatal(getAppName() + " runAppInit: lock busy: " + ex);
			}
			
			// MJM 6.4 RC08 reclaimTasklistSec
			String s = appInfo.getProperty("reclaimTasklistSec");
			if (s != null)
			{
				try { theDb.reclaimTasklistSec = Integer.parseInt(s.trim()); }
				catch(NumberFormatException ex)
				{
					warning("Bad app property 'reclaimTasklistSec' -- should be integer -- ignored: " + ex);
					theDb.reclaimTasklistSec = 0;
				}
			}
			else
				theDb.reclaimTasklistSec = 0;
			
			// MJM 6.5 RC03 checkTimedCompsSec
			s = appInfo.getProperty("checkTimedCompsSec");
			if (s != null)
			{
				try { checkTimedCompsSec = Integer.parseInt(s.trim()); }
				catch(NumberFormatException ex)
				{
					warning("Bad checkTimedCompsSec property '" + s 
						+ "' -- should be integer number of seconds -- will use default of 600.");
					checkTimedCompsSec = 600;
				}
			}
			
			if (!theDb.isCwms())
			{
				missingChecks.clear();
				checkMissingChecks(tsDAO, groupDAO);
				lastMissingCheckMsec = System.currentTimeMillis();
			}
			
		}
		catch(NoSuchObjectException ex)
		{
			// This means a bad app name was given on the command line. Exit.
			Logger.instance().fatal(getAppName() + " runAppInit: " + ex);
			shutdownFlag = true;
			return;
		}
		catch(DbIoException ex)
		{
			Logger.instance().fatal("App Name " + getAppName() + " error in runAppInit(): " + ex);
			shutdownFlag = true;
			databaseFailed = true;
		}
		finally
		{
			groupDAO.close();
			tsDAO.close();
			loadingAppDao.close();
		}
	}
	
	public void initDecodes()
		throws DecodesException
	{
		/*
		DecodesInterface.silent = true;
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		decodes.db.Database.getDb().enumList.read();
		decodes.db.Database.getDb().dataTypeSet.read();
		decodes.db.Database.getDb().presentationGroupList.read();
		*/
	}

	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		ComputationApp compApp = new ComputationApp();
		compApp.execute(args);
	}

	/**
	 * Sets the application's status string in its database lock.
	 */
	public void setAppStatus(String status)
	{
		if (myLock != null)
			myLock.setStatus(status);
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return myProps;
	}

	public DbCompResolver getResolver()
	{
		return resolver;
	}
	
	/**
	 * For timed computations, compproc uses this to figure out the next time that
	 * a computation should be run.
	 * @param timedCompInterval
	 * @param timedCompOffset
	 * @return
	 */
	public static Date computeNextRunTime(String timedCompInterval, String timedCompOffset,
		Calendar cal, Date now)
	{
		if (timedCompInterval == null || timedCompInterval.trim().length() == 0)
			return null;
		if (timedCompOffset != null && timedCompOffset.trim().length() == 0)
			timedCompOffset = null;
		
		
		IntervalIncrement intv = IntervalIncrement.parse(timedCompInterval);
		if (intv == null)
		{
			Logger.instance().warning("Cannot parse timedCompInterval '" + timedCompInterval + "'");
			return null;
		}
		
		cal.setTime(now);
		
		switch(intv.getCalConstant())
		{
		case Calendar.YEAR:
			if (intv.getCount() > 1)
				cal.set(Calendar.YEAR, 
					(cal.get(Calendar.YEAR) / intv.getCount()) * intv.getCount());
			cal.set(Calendar.MONTH, 0);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			break;
		case Calendar.MONTH:
			if (intv.getCount() > 1)
				cal.set(Calendar.MONTH, 
					(cal.get(Calendar.MONTH) / intv.getCount()) * intv.getCount());
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			break;
		case Calendar.DAY_OF_MONTH:
			if (intv.getCount() > 1)
				cal.set(Calendar.DAY_OF_MONTH, 
					((cal.get(Calendar.DAY_OF_MONTH)-1) / intv.getCount()) * intv.getCount() + 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			break;
		case Calendar.HOUR_OF_DAY:
			if (intv.getCount() > 1)
				cal.set(Calendar.HOUR_OF_DAY, 
					(cal.get(Calendar.HOUR_OF_DAY) / intv.getCount()) * intv.getCount());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			break;
		case Calendar.MINUTE:
			if (intv.getCount() > 1)
				cal.set(Calendar.MINUTE, 
					(cal.get(Calendar.MINUTE) / intv.getCount()) * intv.getCount());
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
		}
		
		IntervalIncrement offs = timedCompOffset == null ? null : IntervalIncrement.parse(timedCompOffset);
		if (offs != null)
			cal.add(offs.getCalConstant(), offs.getCount());
		
		while (!cal.getTime().after(now))
			cal.add(intv.getCalConstant(), intv.getCount());
		
		// Note: assumes that constant for MONTH < DAY_OF_MONTH < HOUR_OF_DAY ... SECOND
		return cal.getTime();
	}
	
	/**
	 * Called periodically to maintain the list of timed computations. Compare what's in the 
	 * database to what's in my list now. (Re)Load any computations needed. Delete any that
	 * are no longer needed. Compute each computations next run time based on 'now'.
	 * @param now The current time.
	 */
	private void checkTimedCompList(long now, Calendar timedCompCal)
	{
		String q = 
			"select * from "
			+ "(select cmp.computation_id, cmp.date_time_loaded "
				+ "from cp_comp_property cprop, cp_computation cmp "
				+ "where cprop.computation_id = cmp.computation_id "
				+ "and lower(prop_name) = 'timedcompinterval' "
				+ "and cmp.enabled = 'Y' "
				+ "and cmp.loading_application_id = " + getAppId() + ") q1"
			+ " union "
			+ "(select cmp.computation_id, cmp.date_time_loaded "
				+ "from cp_computation cmp, cp_algorithm alg, cp_algo_property aprop "
				+ "where cmp.algorithm_id = alg.algorithm_id and alg.algorithm_id = aprop.algorithm_id "
				+ "and lower(aprop.prop_name) = 'timedcompinterval' "
				+ "and cmp.enabled = 'Y' "
				+ "and cmp.loading_application_id = " + getAppId() + ")";
		ResultSet rs = null;
		HashMap<DbKey,Date> timedCompsLMT = new HashMap<DbKey,Date>();
		ComputationDAI compDAO = theDb.makeComputationDAO();

		HashSet<DbKey> checkedComps = new HashSet<DbKey>();
		try
		{
			try
			{
				rs = compDAO.doQuery(q);
				while(rs.next())
					timedCompsLMT.put(DbKey.createDbKey(rs, 1), theDb.getFullDate(rs, 2));
				Logger.instance().debug3("" + timedCompsLMT.size() 
					+ " timed computations found for this process.");
			}
			catch (Exception ex)
			{
				warning("Error reading timed computation IDs: " + ex.toString());
				return;
			}
	
			Date nowd = new Date(now);
			for(DbKey compId : timedCompsLMT.keySet())
			{
				DbComputation match = null;
				for(DbComputation tc : timedComps)
					if (tc.getKey().equals(compId))
					{
						match = tc;
						break;
					}
				
				// If I don't yet have this comp, or if it has been modified since I loaded it ...
				if (match == null || match.getLastModified().before(timedCompsLMT.get(compId)))
				{
					// If a match was found, remove the old copy from the list.
					if (match != null)
						timedComps.remove(match);
					try
					{
						DbComputation comp = compDAO.getComputationById(compId);
						Date nrt = computeNextRunTime(comp.getProperty("timedCompInterval"),
							comp.getProperty("timedCompOffset"), timedCompCal, nowd);
						if (nrt != null)
							comp.setNextRunTime(nrt);
						timedComps.add(comp);
						Logger.instance().debug3("Computation " + comp.getKey() + ":" + comp.getName()
							+ " scheduled for " + debugSdf.format(nrt));
					}
					catch (Exception ex)
					{
						warning("Error retrieving timed computation: " + ex + " -- skipped.");
					}
				}
				checkedComps.add(compId);
			}
		}
		finally
		{
			compDAO.close();
		}
		// Now, any "unchecked" computation in my list has been deleted or is no longer timed.
		for(Iterator<DbComputation> cit = timedComps.iterator(); cit.hasNext(); )
		{
			DbComputation tc = cit.next();
			if (!checkedComps.contains(tc.getKey()))
			{
				info("Computation " + tc.getId() + "'" + tc.getName() 
					+ "' has either been deleted or is no longer timed. Ceasing timed execution of it.");
				cit.remove();
			}
		}
	}
	
	/**
	 * Called from main loop
	 * @param tc
	 */
	private void executeTimedComp(DbComputation tc, Calendar timedCompCal, DataCollection dataCollection,
		TimeSeriesDAI timeSeriesDAO, TsGroupDAI tsGroupDAO)
		throws DbIoException
	{
		// Compute since & until: previous run time to current run time
		Date until = tc.getNextRunTime();
		timedCompCal.setTime(until);
		
		// Since is controlled by timedCompDataSince property, or, if not defined, by timedCompInterval.
		String incS = tc.getProperty("timedCompDataSince");
		if (incS == null || incS.trim().length() == 0)
			incS = tc.getProperty("timedCompInterval");
		IntervalIncrement inc = IntervalIncrement.parse(incS);
		if (inc == null)
			return;
		if (inc.getCount() < 0)
			inc.setCount(-inc.getCount());
		timedCompCal.add(inc.getCalConstant(), -inc.getCount());
		Date since = timedCompCal.getTime();
		
		// Until is controlled by timedCompDataUntil property, defaults to this execution time.
		incS = tc.getProperty("timedCompDataUntil");
		if (incS != null && incS.trim().length() > 0 && (inc = IntervalIncrement.parse(incS)) != null)
		{
			if (inc.getCount() < 0)
				inc.setCount(-inc.getCount());
			timedCompCal.setTime(until);
			timedCompCal.add(inc.getCalConstant(), -inc.getCount());
			until = timedCompCal.getTime();
		}
		
		Logger.instance().debug1("Executing comp '" + tc.getName() + "' over time period "
			+ debugSdf.format(since) + " to " + debugSdf.format(until));
		
		if (!DbKey.isNull(tc.getGroupId()))
		{
			ArrayList<DbComputation> executeList = expandForGroup(tc, timeSeriesDAO, tsGroupDAO);
			
//			// This is a group computation. The strategy is to expand the group and then
//			// apply every TSID member to the computation's parameter's masks. If all 
//			// input parms are either defined in the db or can be ignored, then I can
//			// execute the concrete computation built from the TSID.
//			// Caveat: At least one parm must be defined.
//			TsGroup grp = tsGroupDAO.getTsGroupById(tc.getGroupId());
//			if (!grp.getIsExpanded())
//				theDb.expandTsGroup(grp);
//			ArrayList<DbComputation> executeList = new ArrayList<DbComputation>();
//			
//		  nextGroupTSID:
//			for(TimeSeriesIdentifier tsid : grp.getExpandedList())
//			{
//				int numInputsDefined = 0;
//				for (DbCompParm parm : tc.getParmList())
//				{
//					Logger.instance().debug3("  parm '" + parm.getRoleName() + "'");
//					if (!parm.isInput())
//					{
//						Logger.instance().debug3("     - Not an input. Skipping.");
//						continue;
//					}
//					
//					// Transform the group TSID by the parm
//					Logger.instance().debug3("Checking input parm " + parm.getRoleName()
//						+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
//						+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
//						+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
//						+ " siteName=" + parm.getSiteName());
//					TimeSeriesIdentifier tmpTsid = tsid.copyNoKey();
//					Logger.instance().debug3("group tsid=" + tmpTsid.getUniqueString());
//					theDb.transformUniqueString(tmpTsid, parm);
//					Logger.instance().debug3("After transform, param TSID='" + tmpTsid.getUniqueString() + "'");
//					TimeSeriesIdentifier parmTsid = 
//						timeSeriesDAO.getCache().getByUniqueName(tmpTsid.getUniqueString());
//					MissingAction ma = MissingAction.fromString(tc.getProperty(parm.getRoleName() + "_MISSING"));
//					// If the transformed TSID exists in the DB, I can execute.
//					if (parmTsid != null)  // Transformed TSID exists in the database
//						numInputsDefined++;
//					else if (ma != MissingAction.IGNORE) // algorithm requires it to be defined.
//					{
//						// This input parm does not exist and it can't be ignored.
//						// Therefore cannot execute this clone.
//						Logger.instance().debug3("===> TSID '" + tmpTsid.getUniqueString() + "' not defined in db and "
//							+ "MissingAction=" + ma + ", therefore cannot execute this clone.");
//						continue nextGroupTSID;
//					}
//				}
//				if (numInputsDefined == 0)
//				{
//					Logger.instance().debug3("===> There are NO input TSIDs defined. Cannot execute this clone.");
//					continue nextGroupTSID;
//				}
//				// ELSE I can execute this clone. Make it concrete and add it to the execute list.
//				try
//				{
//					// Use the resolver's method to avoid duplicates (multiple TSIDs in the group that 
//					// result in the same set of computation params.)
//					DbComputation concreteClone = DbCompResolver.makeConcrete(theDb, tsid, tc, true);
//					resolver.addToResults(executeList, concreteClone, null);
//					
//					// Special case for timed GroupAdder. Only create a single clone. It will expand its
//					// own group.
//					if (concreteClone.getAlgorithm() != null
//					 && concreteClone.getAlgorithm().getExecClass() != null
//					 && concreteClone.getAlgorithm().getExecClass().equals("decodes.tsdb.algo.GroupAdder"))
//						break;
//				}
//				catch (NoSuchObjectException ex)
//				{
//					warning("Could not make concrete computation: " + ex);
//				}
//			}
			for(DbComputation concreteClone : executeList)
				executeSingleComp(concreteClone, since, until, dataCollection, timeSeriesDAO);
		}
		else // Not a group computation, just execute.
			executeSingleComp(tc, since, until, dataCollection, timeSeriesDAO);
		
	}
	
	private ArrayList<DbComputation> expandForGroup(DbComputation tc, TimeSeriesDAI timeSeriesDAO, TsGroupDAI tsGroupDAO)
		throws DbIoException
	{
		ArrayList<DbComputation> executeList = new ArrayList<DbComputation>();

		// This is a group computation. The strategy is to expand the group and then
		// apply every TSID member to the computation's parameter's masks. If all 
		// input parms are either defined in the db or can be ignored, then I can
		// execute the concrete computation built from the TSID.
		// Caveat: At least one parm must be defined.
		TsGroup grp = tsGroupDAO.getTsGroupById(tc.getGroupId());
		if (grp == null)
			return executeList;
			
		if (!grp.getIsExpanded())
			theDb.expandTsGroup(grp);
		
	  nextGroupTSID:
		for(TimeSeriesIdentifier tsid : grp.getExpandedList())
		{
			int numInputsDefined = 0;
			for (DbCompParm parm : tc.getParmList())
			{
				Logger.instance().debug3("  parm '" + parm.getRoleName() + "'");
				if (!parm.isInput())
				{
					Logger.instance().debug3("     - Not an input. Skipping.");
					continue;
				}
				
				// Transform the group TSID by the parm
				Logger.instance().debug3("Checking input parm " + parm.getRoleName()
					+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
					+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
					+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
					+ " siteName=" + parm.getSiteName());
				TimeSeriesIdentifier tmpTsid = tsid.copyNoKey();
				Logger.instance().debug3("group tsid=" + tmpTsid.getUniqueString());
				theDb.transformUniqueString(tmpTsid, parm);
				Logger.instance().debug3("After transform, param TSID='" + tmpTsid.getUniqueString() + "'");

				DbObjectCache<TimeSeriesIdentifier> cache = timeSeriesDAO.getCache();
				TimeSeriesIdentifier parmTsid = null;
				synchronized(cache)
				{
					parmTsid =	cache.getByUniqueName(tmpTsid.getUniqueString());
				}

				MissingAction ma = MissingAction.fromString(tc.getProperty(parm.getRoleName() + "_MISSING"));
				// If the transformed TSID exists in the DB, I can execute.
				if (parmTsid != null)  // Transformed TSID exists in the database
					numInputsDefined++;
				else if (ma != MissingAction.IGNORE) // algorithm requires it to be defined.
				{
					// This input parm does not exist and it can't be ignored.
					// Therefore cannot execute this clone.
					Logger.instance().debug3("===> TSID '" + tmpTsid.getUniqueString() + "' not defined in db and "
						+ "MissingAction=" + ma + ", therefore cannot execute this clone.");
					continue nextGroupTSID;
				}
			}
			if (numInputsDefined == 0)
			{
				Logger.instance().debug3("===> There are NO input TSIDs defined. Cannot execute this clone.");
				continue nextGroupTSID;
			}
			// ELSE I can execute this clone. Make it concrete and add it to the execute list.
			try
			{
				// Use the resolver's method to avoid duplicates (multiple TSIDs in the group that 
				// result in the same set of computation params.)
				DbComputation concreteClone = DbCompResolver.makeConcrete(theDb, tsid, tc, true);
				resolver.addToResults(executeList, concreteClone, null);
				
				// Special case for timed GroupAdder. Only create a single clone. It will expand its
				// own group.
				if (concreteClone.getAlgorithm() != null
				 && concreteClone.getAlgorithm().getExecClass() != null
				 && concreteClone.getAlgorithm().getExecClass().equals("decodes.tsdb.algo.GroupAdder"))
					break;
			}
			catch (NoSuchObjectException ex)
			{
				warning("Could not make concrete computation: " + ex);
			}
		}

		return executeList;
	}
	
	private void executeSingleComp(DbComputation tc, Date since, Date until, DataCollection dataCollection,
		TimeSeriesDAI timeSeriesDAO)
		throws DbIoException
	{
		// Make a data collection with inputs filled from ... until
		ParmRef parmRef = null;
		try
		{
			// The prepare method maps all input parms
			tc.prepareForExec(theDb);
			for(DbCompParm parm : tc.getParmList())
			{
				if (!parm.isInput())
					continue;
				// 'prepare' method doesn't actually create the CTimeSeries. Do that now.
				tc.getExecutive().setDc(dataCollection);
				tc.getExecutive().addTsToParmRef(parm.getRoleName(), false);
				parmRef = tc.getExecutive().getParmRef(parm.getRoleName());
				CTimeSeries cts = parmRef.timeSeries;
				
				// Read values between previous and this run. Then flag them as DB_ADDED
				// Thus, they will be treated as triggers by the computation.
				int numRead = timeSeriesDAO.fillTimeSeries(cts, since, until, true, true, false);
				if (numRead > 0)
				{
					for(int idx = 0; idx < cts.size(); idx++)
					{
						TimedVariable tv = cts.sampleAt(idx);
						if (!tv.getTime().before(since) && !tv.getTime().after(until))
							VarFlags.setWasAdded(tv);
					}
					if (dataCollection.getTimeSeriesByUniqueSdi(cts.getTimeSeriesIdentifier().getKey()) == null)
						try { dataCollection.addTimeSeries(cts); }
						catch (DuplicateTimeSeriesException ex)
						{
							ex.printStackTrace(); // Should not happen! We checked first.
						}
				}
			}
			
			tc.apply(dataCollection, theDb);
		}
		catch (DbCompException ex)
		{
			warning("Cannot initialize computation " + tc.getName() + ": " + ex);
		}
		catch (BadTimeSeriesException ex)
		{
			String msg = "Error in running computation " + tc.getKey() + ":" + tc.getName() + " -- ";
			msg = msg + "No such input time series for parm '" + parmRef.role + "'";
			if (parmRef.tsid == null) 
				msg = msg + " -- No TSID assigned.";
			else
				msg = msg + " -- TSID '" + parmRef.tsid.getUniqueString() + "' does not exist in db.";
			warning(msg);
		}
	}
	
	
	private void checkMissingChecks(TimeSeriesDAI timeSeriesDAO, TsGroupDAI tsGroupDAO)
	{
		Date now = new Date();
		for(MissingCheck mc : missingChecks)
			mc.checked = false;
		ComputationDAI compDAO = theDb.makeComputationDAO();
		try
		{
			// Use a filter to get all enabled screening comps from the DAO's cache for this app.
			CompFilter filter = new CompFilter();
			filter.setExecClassName("decodes.tsdb.alarm.AlarmScreeningAlgorithm");
			filter.setProcessId(getAppId());
			filter.setEnabledOnly(true);
			ArrayList<DbComputation> screeningComps = compDAO.listCompsForGUI(filter);

Logger.instance().debug3("checkMissingChecks there are " + screeningComps.size() + " screening computations.");
			for(DbComputation comp : screeningComps)
			{
				// If it's a group comp, expand it.
				if (!DbKey.isNull(comp.getGroupId()))
				{
					ArrayList<DbComputation> expanded = expandForGroup(comp, timeSeriesDAO, tsGroupDAO);
					for(DbComputation ecomp : expanded)
					{
						try
						{
							doCMC(ecomp, now);
						}
						catch(DbCompException ex)
						{
							Logger.instance().warning("CMC: Error in group comp: " + ex);
							continue;
						}
					}
				}
				else // single comp
				{
					try
					{
						doCMC(comp, now);
					}
					catch (DbCompException ex)
					{
						Logger.instance().warning("CMC: Error in group comp: " + ex);
						continue;
					}
				}
			}
			
			// A computation or group may have been changed such that an existing check no longer
			// exists. Remove these.
			for(Iterator<MissingCheck> mcit = missingChecks.iterator(); mcit.hasNext(); )
				if (!mcit.next().checked)
					mcit.remove();
			
			sortMissingChecks();
			Logger.instance().info("CMC: There are " + missingChecks.size() + " missing checks.");
			if (missingChecks.size() > 0)
				Logger.instance().info("CMC: The next missing check scheduled for "
					+ Logger.instance().formatTime(new Date(missingChecks.get(0).nextRunMsec))
					+ " " + Logger.instance().getTz().getID());
		}
		catch (DbIoException ex)
		{
			Logger.instance().warning("CMC: Error checking for missing checks: " + ex);
			if (Logger.instance().getLogOutput() != null)
				ex.printStackTrace(Logger.instance().getLogOutput());
		}
		finally
		{
			compDAO.close();
		}
	}
	
	private void sortMissingChecks()
	{
		Collections.sort(missingChecks,
			new Comparator<MissingCheck>()
			{
				@Override
				public int compare(MissingCheck o1, MissingCheck o2)
				{
					if (o1.nextRunMsec < o2.nextRunMsec)
						return -1;
					else if (o1.nextRunMsec > o2.nextRunMsec)
						return 1;
					
					return 0;
				}
			});
	}
	
	private void doCMC(DbComputation ecomp, Date now) 
		throws DbCompException, DbIoException
	{
Logger.instance().debug3("doCMC screeing comp '" + ecomp.getName() + "' date=" + now);
		ecomp.prepareForExec(theDb);
		AlarmScreeningAlgorithm exec = (AlarmScreeningAlgorithm)ecomp.getExecutive();
		TimeSeriesIdentifier tsid = exec.getParmTsId("input");
		if (tsid == null)
			throw new DbCompException("doCMC: input has no TSID.");
		
		// If there's already a missing check for this TSID in my cache, get it for update.
		MissingCheck cmpMissingChk = null;
		for(MissingCheck mc : missingChecks)
			if (mc.tsid.getKey().equals(tsid.getKey()))
			{
				cmpMissingChk = mc;
				break;
			}

		exec.getAlarmScreenings(tsid);
		if (!exec.initScreeningAndLimitSet(now))
		{
			// There is no limit set for this time!
			// remove from array if there is one.
			if (cmpMissingChk != null)
				missingChecks.remove(cmpMissingChk);
			return;
		}
		
		// If I already have missing check and there are no changes to the screening, just bail.
		AlarmScreening screening = exec.gettScreening();
		if (cmpMissingChk != null && !cmpMissingChk.lastModifiedInDb.before(screening.getLastModified()))
		{
Logger.instance().debug3("doCMC screening already in DB with no changes.");
			cmpMissingChk.checked = true;
			return;
		}
		
		AlarmLimitSet limitSet = exec.gettLimitSet();
		String mIntv = limitSet.getMissingInterval();
		String mPer = limitSet.getMissingPeriod();
		if (mIntv == null || mIntv.trim().length() == 0 || mPer == null || mPer.trim().length() == 0)
		{
			// The limit set does not do a Missing Check
			// remove from array if there is one.
			if (cmpMissingChk != null)
				missingChecks.remove(cmpMissingChk);
			return;
		}
		if (!TextUtil.strEqualIgnoreCase(mIntv, tsid.getInterval()))
		{
			// This check is for a different interval than the computation's input param.
			if (cmpMissingChk != null)
				missingChecks.remove(cmpMissingChk);
			return;
		}

		// If I get to here, it's either a new missing check or a check for a screening that has
		// been modified in the database.
		
		long nextRunMsec = computeNextRun(now, mIntv);
Logger.instance().debug3("doCMC missing check next run time will be " + debugSdf.format(new Date(nextRunMsec)));
		
		if (cmpMissingChk == null)
		{
			cmpMissingChk = new MissingCheck(tsid, nextRunMsec, ecomp, 
				exec.gettScreening(), exec.gettLimitSet(), screening.getLastModified());
			missingChecks.add(cmpMissingChk);
		}
		else
		{
			cmpMissingChk.nextRunMsec = nextRunMsec;
			cmpMissingChk.comp = ecomp;
			cmpMissingChk.screening = exec.gettScreening();
			cmpMissingChk.limitSet = exec.gettLimitSet();
			cmpMissingChk.lastModifiedInDb = screening.getLastModified();
		}
		cmpMissingChk.checked = true;

	}
	
	private long computeNextRun(Date now, String mIntv)
	{
		mIntv = mIntv.trim();
		timedCompCal.setTime(now);
		
		Interval intv = IntervalCodes.getInterval(mIntv);
Logger.instance().debug3("doCMC missing check interval='" + mIntv
+ ", const=" + intv.getCalConstant() + ", mult=" + intv.getCalMultiplier());
		switch(intv.getCalConstant())
		{
		case Calendar.YEAR:
			timedCompCal.set(Calendar.MONTH, 0);
			// fall through...
		case Calendar.MONTH:
			timedCompCal.set(Calendar.DAY_OF_MONTH, 1);
			// fall through...
		case Calendar.DAY_OF_YEAR:
		case Calendar.DAY_OF_MONTH:
			timedCompCal.set(Calendar.HOUR_OF_DAY, 0);
			// fall through...
		case Calendar.HOUR_OF_DAY:
			timedCompCal.set(Calendar.MINUTE, 0);
			// fall through
		case Calendar.MINUTE:
			timedCompCal.set(Calendar.SECOND, 0);
			timedCompCal.set(Calendar.MILLISECOND, 0);
		}
		timedCompCal.add(intv.getCalConstant(), intv.getCalMultiplier());
		return timedCompCal.getTimeInMillis(); 
	}
	
	private void doMissingCheck(MissingCheck chk, TimeSeriesDAI tsDAO)
		throws DbIoException
	{
		try
		{
			CTimeSeries cts = tsDAO.makeTimeSeries(chk.tsid);
			Date until = new Date(chk.nextRunMsec);
			timedCompCal.setTime(until);
			IntervalIncrement periodII = IntervalIncrement.parse(chk.limitSet.getMissingPeriod());
			if (periodII.getCount() < 0)
				periodII.setCount(periodII.getCount() * -1);
			timedCompCal.add(periodII.getCalConstant(), -periodII.getCount());
			Date from = timedCompCal.getTime();
			Logger.instance().info("Checking for missing data for " + chk.tsid.getUniqueString() 
				+ " from " + debugSdf.format(from) + " to " + debugSdf.format(until));
			int numValues = tsDAO.fillTimeSeries(cts, from, until, true, true, false);
			
			Interval intv = IntervalCodes.getInterval(chk.limitSet.getMissingInterval());
			if (intv.getCalMultiplier() == 0)
			{
				warning("doMissingCheck -- Cannot do missing check on zero interval: tsid='"
					+ chk.tsid.getUniqueString() + "', screening '" + chk.screening.getScreeningName() + "'");
				return;
			}
			
			int numExpected = 0;
			while(!timedCompCal.getTime().after(until))
			{
				numExpected++;
				timedCompCal.add(intv.getCalConstant(), intv.getCalMultiplier());
			}
			
			AlarmManager.instance(theDb, theDb.getAppId()).missingCheckResults(
				chk.tsid, until, numValues, numExpected, chk.screening, chk.limitSet);
			chk.nextRunMsec = computeNextRun(new Date(), chk.limitSet.getMissingInterval());
		}
		catch (NoSuchObjectException ex)
		{
			Logger.instance().warning("doMissingCheck for TSID=" + chk.tsid.getUniqueString() + ": " + ex);
		}
		catch (BadTimeSeriesException ex)
		{
			Logger.instance().warning("doMissingCheck for TSID=" + chk.tsid.getUniqueString() + ": " + ex);
		}
		
	}


}
