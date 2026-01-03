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
package decodes.tsdb;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import decodes.sql.DbKey;
import decodes.tsdb.alarm.AlarmLimitSet;
import decodes.tsdb.alarm.AlarmManager;
import decodes.tsdb.alarm.AlarmScreening;
import decodes.tsdb.alarm.AlarmScreeningAlgorithm;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.TextUtil;
import lrgs.gui.DecodesInterface;
import opendcs.dai.ComputationDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.dao.DbObjectCache;
import opendcs.opentsdb.Interval;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.slf4j.MDC.MDCCloseable;


/**
ComputationApp is the main module for the background comp processor.
TODO: There are 3 sections in this class that detrmine if group comp and either loop over the expanded group or just
execute it. The logic should be gathered in one place.
*/
public class ComputationApp extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	// wait 30 seconds between runs by default. Drastically reduces system load
	// especially with multiple instances pointing to a single database.
	private static final long COMP_RUN_WAIT_TIME =
		Long.parseLong(System.getProperty("opendcs.computations.getNew.interval.milliseconds", "30000"));
	// maximum number of records to take at a time from the task list.
	private static final int COMP_RUN_MAX_TAKE =
		Integer.parseInt(System.getProperty("opendcs.computations.getNew.maxTake", "20000"));
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

	private BooleanToken regressionTestModeArg = new BooleanToken("T", "Regression Test Mode",
		"", TokenOptions.optSwitch, false);
	private StringToken officeIdArg = new StringToken(
		"O", "OfficeID", "", TokenOptions.optSwitch, "");
	private CompEventSvr compEventSvr = null;

	private ArrayList<DbComputation> timedComps = new ArrayList<DbComputation>();
	private int checkTimedCompsSec = 600;
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

	
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

	@Override
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

		if (officeIdArg.getValue() != null && !officeIdArg.getValue().isEmpty())
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
		log.debug("runApp starting");
		_instance = this;
		shutdownFlag = false;

		TimeZone dbtz = TimeZone.getTimeZone(theDb.databaseTimezone);
		timedCompCal = Calendar.getInstance();
		timedCompCal.setTimeZone(dbtz);

		runAppInit();
		log.debug("runAppInit done, shutdownFlag={}, surviveDatabaseBounce={}",
				  shutdownFlag, surviveDatabaseBounce);

		long lastDataTime = System.currentTimeMillis();
		long lastLockCheck = 0L;
		long lastCacheMaintenance = System.currentTimeMillis();
		long lastTimedCompCheck = 0L;

		String action="starting";


		try
		{
			while(!shutdownFlag)
			{
				log.trace("ComputationApp start of main loop.");
				try(
					TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
					LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
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
								log.warn("Cannot compute nextRun for computation {}: {} with interval '{}' " +
										 "-- is this no longer a timed computation?",
										 tc.getId(), tc.getName(), tc.getProperty("timedCompInterval"));
								tcit.remove();
							}
							else
							{
								tc.setNextRunTime(nextRun);
								log.trace("Computation {}:{} scheduled for {}",
										  tc.getKey(), tc.getName(), nextRun);
							}
						}
					}
					if (numTimed > 0)
					{
						action = "Saving timed results";
						List<CTimeSeries> tsList = dataCollection.getAllTimeSeries();
						log.trace("{} {} time series in data.", action, tsList.size());

						for(CTimeSeries cts : tsList)
						{
							try { timeSeriesDAO.saveTimeSeries(cts); }
							catch(BadTimeSeriesException ex)
							{
								log.atWarn()
								   .setCause(ex)
								   .log("Cannot save time series '{}'", cts.getNameString());
							}
						}

					}

					action = "Getting new data";
					dataCollection = timeSeriesDAO.getNewData(getAppId(), COMP_RUN_MAX_TAKE);
					// In Regression Test Mode, exit after 5 sec of idle
					if (!dataCollection.isEmpty())
						lastDataTime = System.currentTimeMillis();
					else if (regressionTestModeArg.getValue()
					 && System.currentTimeMillis() - lastDataTime > 10000L)
					{
						log.info("Regression Test Mode - Exiting after 10 sec idle.");
						shutdownFlag = true;
						loadingAppDAO.releaseCompProcLock(myLock);
					}


					if (!dataCollection.isEmpty())
					{
						action = "Resolving computations";
						DbComputation[] comps = resolver.resolve(dataCollection);

						action = "Applying computations";
						ComputationExecution execution = new ComputationExecution(db);

						ComputationExecution.CompResults results = execution.execute(List.of(comps), dataCollection);
						compsTried += results.getNumComputesTried();
						compErrors += results.getNumErrors();

						action = "Saving results";
						List<CTimeSeries> tsList = dataCollection.getAllTimeSeries();
						log.trace("{} {} time series in data.", action, tsList.size());
						for(CTimeSeries ts : tsList)
						{
							try
							{
								timeSeriesDAO.saveTimeSeries(ts);
							}
							catch(Exception ex)
							{
								log.atWarn()
								   .setCause(ex)
								   .log("Cannot save time series '{}'", ts.getNameString());
							}
						}

						action = "Releasing new data";
						theDb.releaseNewData(dataCollection, timeSeriesDAO);
						lastDataTime = System.currentTimeMillis();
					}
					else // MJM 6.4 RC08 Only sleep if data was empty.
					{
						try
						{
							Thread.sleep(COMP_RUN_WAIT_TIME);
						}
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
			log.atError().setCause(ex).log("No Lock - Application exiting.");
			shutdownFlag = true;
		}
		catch(DbIoException ex)
		{
			log.atError().setCause(ex).log("Database Error while {}", action);
			shutdownFlag = true;
			databaseFailed = true;
		}
		catch(Throwable ex)
		{
			log.atError().setCause(ex).log("Unexpected exception while {}", action);
			shutdownFlag = true;
		}
		resolver = null;
		log.debug("runApp() exiting.");
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
					log.atError()
					   .setCause(ex)
					   .log("Cannot create Event server -- no events available to external clients." +
					   		"NOTE: This component is no longer supported. Expect JMX features to be " +
							"enabled in the future.");
				}
			}

			try { myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); }
			catch(LockBusyException ex)
			{
				shutdownFlag = true;
				log.atError().setCause(ex).log("runAppInit: lock busy.");
			}

			// MJM 6.4 RC08 reclaimTasklistSec
			String s = appInfo.getProperty("reclaimTasklistSec");
			if (s != null)
			{
				try { theDb.reclaimTasklistSec = Integer.parseInt(s.trim()); }
				catch(NumberFormatException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Bad app property 'reclaimTasklistSec' -- should be integer -- ignored.");
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
					log.atWarn()
					   .setCause(ex)
					   .log("Bad checkTimedCompsSec property '{}' -- should be " +
					   		"integer number of seconds -- will use default of 600.",
							s);
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
			log.atError().setCause(ex).log("runAppInit failed.");
			shutdownFlag = true;
		}
		catch(DbIoException ex)
		{
			log.atError().setCause(ex).log("Error in runAppInit().");
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

	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		if (DecodesInterface.isInitialized())
			return;
		DecodesInterface.initDecodesMinimal(cmdLineArgs.getPropertiesFile());
		decodes.db.Database.getDb().enumList.read();
		decodes.db.Database.getDb().dataTypeSet.read();
		decodes.db.Database.getDb().presentationGroupList.read();
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
		if (timedCompInterval == null || timedCompInterval.trim().isEmpty())
			return null;
		if (timedCompOffset != null && timedCompOffset.trim().isEmpty())
			timedCompOffset = null;


		IntervalIncrement intv = IntervalIncrement.parse(timedCompInterval);
		if (intv == null)
		{
			log.warn("Cannot parse timedCompInterval '{}'", timedCompInterval);
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

		HashMap<DbKey,Date> timedCompsLMT = new HashMap<DbKey,Date>();
		ComputationDAI compDAO = theDb.makeComputationDAO();

		HashSet<DbKey> checkedComps = new HashSet<DbKey>();
		try
		{
			try (ResultSet rs = compDAO.doQuery(q);)
			{
				while(rs.next())
				{
					timedCompsLMT.put(DbKey.createDbKey(rs, 1), theDb.getFullDate(rs, 2));
				}
				log.trace("{} timed computations found for this process.", timedCompsLMT.size());
			}
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Error reading timed computation IDs.");
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
						log.trace("Computation {}:{} scheduled for {}", comp.getKey(), comp.getName(), nrt);
					}
					catch (Exception ex)
					{
						log.atWarn().setCause(ex).log("Error retrieving timed computation. -- skipped.");
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
				log.info("Computation {} '{}' has either been deleted or is " +
						 "no longer timed. Ceasing timed execution of it.",
						 tc.getId(), tc.getName());
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
		if (incS == null || incS.trim().isEmpty())
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
		if (incS != null && !incS.trim().isEmpty() && (inc = IntervalIncrement.parse(incS)) != null)
		{
			if (inc.getCount() < 0)
				inc.setCount(-inc.getCount());
			timedCompCal.setTime(until);
			timedCompCal.add(inc.getCalConstant(), -inc.getCount());
			until = timedCompCal.getTime();
		}

		ComputationExecution execution = new ComputationExecution(db);

		log.debug("Executing comp '{}' over time period {} to {}", tc.getName(), since, until);
		if (!DbKey.isNull(tc.getGroupId()))
		{
			ArrayList<DbComputation> executeList = expandForGroup(tc, timeSeriesDAO, tsGroupDAO);
			execution.execute(executeList, dataCollection, since, until);
		}
		else // Not a group computation, just execute.
		{
			execution.executeSingleComp(tc, since, until, dataCollection);
		}

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
				log.trace("parm '{}'", parm.getRoleName());
				if (!parm.isInput())
				{
					log.trace("Not an input. Skipping.");
					continue;
				}

				// Transform the group TSID by the parm
				log.atTrace()
				   .log(() -> "Checking input parm " + parm.getRoleName()
						+ " sdi=" + parm.getSiteDataTypeId() + " intv=" + parm.getInterval()
						+ " tabsel=" + parm.getTableSelector() + " modelId=" + parm.getModelId()
						+ " dt=" + parm.getDataType() + " siteId=" + parm.getSiteId()
						+ " siteName=" + parm.getSiteName());
				TimeSeriesIdentifier tmpTsid = tsid.copyNoKey();
				log.trace("group tsid={}", tmpTsid.getUniqueString());
				theDb.transformUniqueString(tmpTsid, parm);
				log.trace("After transform, param TSID='{}'", tmpTsid.getUniqueString());

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
					log.trace("===> TSID '{}' not defined in db and MissingAction={}" +
							  ", therefore cannot execute this clone.", tmpTsid.getUniqueString(), ma);
					continue nextGroupTSID;
				}
			}
			if (numInputsDefined == 0)
			{
				log.trace("===> There are NO input TSIDs defined. Cannot execute this clone.");
				continue nextGroupTSID;
			}
			// ELSE I can execute this clone. Make it concrete and add it to the execute list.
			try
			{
				// Use the resolver's method to avoid duplicates (multiple TSIDs in the group that
				// result in the same set of computation params.)
				DbComputation concreteClone = DbCompResolver.makeConcrete(theDb, timeSeriesDAO, tsid, tc, true);
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
				log.atWarn().setCause(ex).log("Could not make concrete computation.");
			}
		}

		return executeList;
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

			log.trace("checkMissingChecks there are {} screening computations.", screeningComps.size());
			for(DbComputation comp : screeningComps)
			{
				// If it's a group comp, expand it.
				if (!DbKey.isNull(comp.getGroupId()))
				{
					ArrayList<DbComputation> expanded = expandForGroup(comp, timeSeriesDAO, tsGroupDAO);
					for(DbComputation ecomp : expanded)
					{
						try (MDCCloseable mdc = MDC.putCloseable("computation", ecomp.getName()))
						{
							doCMC(ecomp, now);
						}
						catch(DbCompException ex)
						{
							log.atWarn().setCause(ex).log("CMC: Error in group comp.");
						}
					}
				}
				else // single comp
				{
					try (MDCCloseable mdc = MDC.putCloseable("computation", comp.getName()))
					{
						doCMC(comp, now);
					}
					catch (DbCompException ex)
					{
						log.atWarn().setCause(ex).log("CMC: Error in comp.");
					}
				}
			}

			// A computation or group may have been changed such that an existing check no longer
			// exists. Remove these.
			for(Iterator<MissingCheck> mcit = missingChecks.iterator(); mcit.hasNext(); )
				if (!mcit.next().checked)
					mcit.remove();

			sortMissingChecks();
			log.info("CMC: There are {} missing checks.", missingChecks.size());
			if (!missingChecks.isEmpty())
			{
				log.info("CMC: The next missing check scheduled for {}",
						 new Date(missingChecks.get(0).nextRunMsec));
			}
		}
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("CMC: Error checking for missing checks.");
		}
		finally
		{
			compDAO.close();
		}
	}

	private void sortMissingChecks()
	{
		Collections.sort(missingChecks,
				(o1, o2) ->
				{
					if (o1.nextRunMsec < o2.nextRunMsec)
						return -1;
					else if (o1.nextRunMsec > o2.nextRunMsec)
						return 1;

					return 0;
				});
	}

	private void doCMC(DbComputation ecomp, Date now)
		throws DbCompException, DbIoException
	{
		log.trace("doCMC screeing comp '{}', date={}", ecomp.getName(), now);
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
			log.trace("doCMC screening already in DB with no changes.");
			cmpMissingChk.checked = true;
			return;
		}

		AlarmLimitSet limitSet = exec.gettLimitSet();
		String mIntv = limitSet.getMissingInterval();
		String mPer = limitSet.getMissingPeriod();
		if (mIntv == null || mIntv.trim().isEmpty() || mPer == null || mPer.trim().isEmpty())
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
		log.trace("doCMC missing check next run time will be {}", new Date(nextRunMsec));

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
		log.trace("doCMC missing check interval='{}', const={}, mult={}",
				  mIntv, intv.getCalConstant(), intv.getCalMultiplier());
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
			log.info("Checking for missing data for {} from {} to {}.", chk.tsid.getUniqueString(), from, until);
			int numValues = tsDAO.fillTimeSeries(cts, from, until, true, true, false);

			Interval intv = IntervalCodes.getInterval(chk.limitSet.getMissingInterval());
			if (intv.getCalMultiplier() == 0)
			{
				log.warn("doMissingCheck -- Cannot do missing check on zero interval: tsid='{}', screening '{}'",
						 chk.tsid.getUniqueString(), chk.screening.getScreeningName());
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
			log.atWarn().setCause(ex).log("doMissingCheck for TSID={}", chk.tsid.getUniqueString());
		}
		catch (BadTimeSeriesException ex)
		{
			log.atWarn().setCause(ex).log("doMissingCheck for TSID={}", chk.tsid.getUniqueString());
		}

	}
}