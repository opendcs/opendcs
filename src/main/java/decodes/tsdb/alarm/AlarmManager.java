/**
 * $Id$
 * 
 * $Log$
 * Revision 1.4  2019/08/27 20:14:40  mmaloney
 * Make sure alarms always go forward in time.
 *
 * Revision 1.3  2019/08/26 20:49:52  mmaloney
 * Alarm Implementations.
 *
 * Revision 1.2  2019/08/07 14:18:58  mmaloney
 * 6.6 RC04
 *
 * Revision 1.1  2019/07/02 13:48:03  mmaloney
 * 6.6RC04 First working Alarm Implementation
 *
 */
package decodes.tsdb.alarm;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.text.NumberFormat;

import decodes.hdb.HdbFlags;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.alarm.mail.AlarmMailer;
import decodes.tsdb.alarm.mail.MailerException;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import opendcs.dai.AlarmDAI;
import opendcs.dai.LoadingAppDAI;

public class AlarmManager
	extends Thread
{
	private static final String module = "AlarmManager";
	private static AlarmManager _instance = null;
	private boolean _shutdown = false;
	private TimeSeriesDb tsdb = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	private NumberFormat numFmt = NumberFormat.getNumberInstance();
	private AlarmMailer alarmMailer = new AlarmMailer();
	private boolean mailerEnabled = true;
	private long lastMailerConfig = 0L;
	private static final long MAILER_CONFIG_MS = 10L * 60000L; // Check mailer config every 10 min.
	private static final long MS_PER_DAY = 24L * 3600000L;
	
	private Properties mailProps = null;
	private AlarmConfig alarmConfig = null;
	private long resendSeconds = 24*3600L;
	private int notifyMaxAgeDays = 30;

	
	/** This holds a copy of the ALARM_CURRENT table. It maps TsKey to Alarm */
	private HashMap<DbKey, Alarm> currentAlarms = new HashMap<DbKey, Alarm>();
	
	private long refreshMS = 5 * 60000L;
	private long checkQueueMS = 60000L;
	
	class AlarmMsg
	{
		DbKey groupId;
		TimeSeriesIdentifier tsid;
		Date assertDate;
		String hint;
		String msg;

		public AlarmMsg(DbKey groupId, TimeSeriesIdentifier tsid, Date assertDate, String hint, String msg)
		{
			super();
			this.groupId = groupId;
			this.tsid = tsid;
			this.assertDate = assertDate;
			this.hint = hint;
			this.msg = msg;
		}
	}
	private ConcurrentLinkedQueue<AlarmMsg> msgQ = new ConcurrentLinkedQueue<AlarmMsg>();
	
	
	public static AlarmManager instance(TimeSeriesDb tsdb)
	{
		if (_instance == null)
			_instance = new AlarmManager(tsdb);
	
		long now = System.currentTimeMillis();
Logger.instance().debug3("AlarmManager.instance() now=" + now + ", lastCfg=" + _instance.lastMailerConfig);
		if (now - _instance.lastMailerConfig > MAILER_CONFIG_MS)
		{
			_instance.configureMailer();
			_instance.lastMailerConfig = System.currentTimeMillis();
		}
		
		return _instance;
	}
	
	private AlarmManager(TimeSeriesDb tsdb)
	{
		this.tsdb = tsdb;
		sdf.setTimeZone(TimeZone.getTimeZone(tsdb.getDatabaseTimezone()));
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(5);
		
		alarmConfig = new AlarmConfig();

		// The mailer thread runs in the background.
		this.start();
	}

	/**
	 * Called from instance() every 10 min.
	 */
	private void configureMailer()
	{
Logger.instance().debug3("AlarmManager.configureMailer");
		LoadingAppDAI loadingAppDAO = tsdb.makeLoadingAppDAO();
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		CompAppInfo cai = null;
		String action = " reading mail props";
		try
		{
			cai = loadingAppDAO.getComputationApp(TsdbAppTemplate.getAppInstance().getAppId());
			Properties appProps = cai.getProperties();
			if (!PropertiesUtil.propertiesEqual(mailProps, appProps))
			{
				mailProps = appProps;
				String host = PropertiesUtil.getIgnoreCase(mailProps, "mail.smtp.host");
				if (host == null || host.trim().length() == 0 || host.equals("-"))
				{
					Logger.instance().info(module 
						+ " email alarm output disabled because 'mail.smtp.host' is undefined in properties for "
						+ "loading application '" + cai.getAppName() + "'");
					mailerEnabled = false;
				}
				else
					mailerEnabled = true;
				String s = PropertiesUtil.getIgnoreCase(mailProps, "resendSeconds");
				if (s != null)
				{
					try { resendSeconds = Long.parseLong(s.trim()); }
					catch(NumberFormatException ex)
					{
						resendSeconds = 3600 * 24;
						Logger.instance().warning("Invalid resendSeconds property '" + s + "' -- should be number of seconds "
							+ ": will use default of " + resendSeconds);
					}
				}
				s = PropertiesUtil.getIgnoreCase(mailProps, "notifyMaxAgeDays");
				if (s != null)
				{
					try { notifyMaxAgeDays = Integer.parseInt(s.trim()); }
					catch(NumberFormatException ex)
					{
						notifyMaxAgeDays = 30;
						Logger.instance().warning("Invalid notifyMaxAgeDays property '" + s + "' -- should be number of days "
							+ ": will use default of " + notifyMaxAgeDays);
					}
				}

				action = "configuring mailer";
				alarmMailer.configure(mailProps);
				
				action = "checking alarm groups";
				alarmDAO.check(alarmConfig);
			}
		}
		catch(MailerException ex)
		{
			Logger.instance().failure(module + " Cannot configure alarm mailer: " + ex 
				+ " -- email notifications will be disabled config for loading app '" 
				+ cai.getAppName() + "' is fixed.");
			mailerEnabled = false;
		}
		catch (Exception ex)
		{
			Logger.instance().failure(module + " Error while '" + action + "': " + ex
				+ " -- mailer will be disabled until configuration is fixed.");
			if (Logger.instance().getLogOutput() != null)
				ex.printStackTrace(Logger.instance().getLogOutput());
			mailerEnabled = false;
		}
		finally
		{
			alarmDAO.close();
			loadingAppDAO.close();
		}
	}
	
	/**
	 * Shutdown the thread and destroy the instance. Next call to instance() will
	 * create new instance.
	 */
	public void shutdown()
	{
		_shutdown = true;
		_instance = null;
	}
	
	/**
	 * This is the mailer thread which reads the queue and sends email asynchronously
	 * from the main compproc thread.
	 */
	public void run()
	{
		try { sleep(2000L); } catch(InterruptedException ex) {}
		
		long lastRefresh = 0L;
		long lastQueueCheck = System.currentTimeMillis();
		
		String action = "";
		while(!_shutdown)
		{
			long now = System.currentTimeMillis();
			
			try
			{
				// Periodically refresh my 'current alarms' table from the database in
				// case some other application modified them.
				if (now - lastRefresh > refreshMS)
				{
					action = "refreshing current alarms";
					refreshCurrentAlarms();
					lastRefresh = now;
				}
				
				if (now - lastQueueCheck >= checkQueueMS)
				{
					checkQueue();
					lastQueueCheck = now;
				}
				
				
				try { sleep(1000L); } catch(InterruptedException ex) {}
			}
			catch(Exception ex)
			{
				String msg = module + " unexpected exception while " + action + ": " + ex
					+ " -- will shutdown.";
				Logger.instance().failure(msg);
				System.err.println(msg);
				if (Logger.instance().getLogOutput() != null)
					ex.printStackTrace(Logger.instance().getLogOutput());
				shutdown();
			}
		}
	}

	/**
	 * Called from the mailer thread every checkQueueMS (1 min).
	 */
	private void checkQueue()
	{
		// Flush the queue into an array I can work with.
		ArrayList<AlarmMsg> toSend = new ArrayList<AlarmMsg>();
		while(!msgQ.isEmpty())
			toSend.add(msgQ.poll());
		if (toSend.size() == 0)
			return;
		
Logger.instance().debug3("AlarmManager.checkQueue will attempt to send " + toSend.size() + " alarms.");
		// Sort by Group ID, TSID, and then assertTime
		Collections.sort(toSend,
			new Comparator<AlarmMsg>()
			{
				@Override
				public int compare(AlarmMsg o1, AlarmMsg o2)
				{
					long d = o1.groupId.getValue() - o2.groupId.getValue();
					if (d != 0L)
						return d > 0 ? 1 : -1;
					int r = o1.tsid.compareTo(o2.tsid);
					if (r != 0)
						return r;
					d = o1.assertDate.getTime() - o2.assertDate.getTime();
					return d == 0L ? 0 : d > 0L ? 1 : -1;
				}
			});
		
		StringBuilder sb = new StringBuilder();
		DbKey lastGrpId = DbKey.NullKey;
		DbKey lastTsIdKey = DbKey.NullKey;
		String lastHint = null;
		String lineSep = System.getProperty("line.separator");
		for(Iterator<AlarmMsg> amit = toSend.iterator(); amit.hasNext(); )
		{
			AlarmMsg am = amit.next();
			
			// Starting a new group? Send the message already buffered for the last group.
			if (!am.groupId.equals(lastGrpId))
			{
				if (!DbKey.isNull(lastGrpId) && sb.length() > 0) // i.e. not the first time through the loop.
				{
					sendEmail(lastGrpId, sb.toString());
					lastTsIdKey = DbKey.NullKey;
					lastHint = null;
					sb.setLength(0);
				}
				lastGrpId = am.groupId;
			}
			
			// Starting new time series? Add TSID to the message.
			if (!lastTsIdKey.equals(am.tsid.getKey()))
			{
				sb.append(lineSep + "TSID: " + am.tsid.getUniqueString() 
					+ " (" + am.tsid.getKey() + ")" + lineSep);
				lastTsIdKey = am.tsid.getKey();
			}
			
			// We could traverse multiple limit sets over time with different 'hint's.
			if (!TextUtil.strEqual(lastHint, am.hint) && am.hint != null)
			{
				sb.append(am.hint + lineSep);
				lastHint = am.hint;
			}

			sb.append(am.msg + lineSep);
		}
		
		// Now send the last group in the buffer.
		if (sb.length() > 0)
			sendEmail(lastGrpId, sb.toString());
		
	}

	/**
	 * Called from the AlarmManager thread only!
	 * @param groupId
	 * @param msg
	 */
	private void sendEmail(DbKey groupId, String msg)
	{
Logger.instance().debug3("AlarmManager sendEmail mailerEnabled=" + mailerEnabled + " msg=" + msg);
		if (!mailerEnabled)
			return;
		
		String action = "";
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		try
		{
			action = "getting group from alarmConfig";
			AlarmGroup group = alarmConfig.getGroupById(groupId);
			ArrayList<String> msgwrap = new ArrayList<String>();
			msgwrap.add(msg);
			action = "calling alarmMailer.send";
			alarmMailer.send(group, msgwrap);
		}
		catch (Exception ex)
		{
			String s = module + ".sendEmail failed while " + action + ": " + ex;
			Logger.instance().warning(s);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
		}
		finally
		{
			alarmDAO.close();
		}
	}

	/**
	 * Called from AlarmScreeningAlgorithm. This method manages the assertion/deassertion
	 * of alarm records in the database. Then, if a group is assigned, an email action is
	 * enqueued for the thread running in the background. (We don't want to burden the compproc
	 * main thread with sending email.)
	 * @param tsid
	 * @param tLimitSet
	 * @param t
	 * @param value
	 * @param delta
	 * @param variance
	 * @param flags
	 */
	public void checkAlarms(TimeSeriesIdentifier tsid, AlarmLimitSet limitSet, AlarmScreening scrn,
		Date t, double value, double delta, double variance, int flags)
	{
		Alarm currentAlarm = currentAlarms.get(tsid.getKey());
		
		// Alarm records only store screening bits, not all of the other data flags.
		int scrFlags = flags & HdbFlags.SCREENING_MASK;

		
		String action = "";
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		
		Date now = new Date();
		try
		{
			// Alarm assertions only go forward in time.
			if (currentAlarm != null)
			{
				if (t.before(currentAlarm.getDataTime()))
				return;
			}
			else // no current alarm assertion
			{
				// Don't alarm on values before an existing historical alarm.
				action = "checking last historical alarm date";
				Date lastHist = alarmDAO.lastHistoryAlarmTime(tsid);
				if (lastHist != null && t.before(lastHist))
					return;
				
				// Don't alarm on values older than the threshold.
				if (currentAlarm == null 
				 && System.currentTimeMillis()-t.getTime() > (notifyMaxAgeDays * MS_PER_DAY))
					return;
			}
			
			if (scrFlags == HdbFlags.SCREENED) // This means there are no faults asserted.
			{
				if (currentAlarm != null)
				{
					// The alarm is now cleared. Move the record to ALARM_HISTORY.
					// The following also cancels any previous MISSING alarms. Good.
					action = "moving old alarm to history";
					currentAlarms.remove(tsid.getKey());
					currentAlarm.setEndTime(t);  // END_TIME is the date/time of the value that cancelled the alarm.
					alarmDAO.moveToHistory(currentAlarm);
					
					if (!DbKey.isNull(scrn.getAlarmGroupId()))
					{
						// If a group is used, queue a message indicating that the alarm is cleared.
						String msg = "Value " + numFmt.format(value) + " at time " + sdf.format(t)
							+ " back within limits. All alarms cancelled.";
						msgQ.add(
							new AlarmMsg(scrn.getAlarmGroupId(), tsid, now, limitSet.getHintText(), msg));
					}
				}
				// Else no existing alarms and this value is good. Do nothing.
			}
			else // Some faults are asserted
			{
				action = "checking for resend";
				boolean resend = false;
				if (currentAlarm != null
				 && scrFlags == currentAlarm.getAlarmFlags())
				{
					// an alarm is already asserted and the flags have not changed
					if (resendSeconds <= 0                              // resend feature disabled
					 || !mailerEnabled                                  // email feature disabled
					 || (currentAlarm.getLastNotificationTime() != null  // email WAS sent within the threshold
					  && now.getTime() - currentAlarm.getLastNotificationTime().getTime() < resendSeconds*1000L))
						// This alarm is already asserted and notified. Do nothing.
						return;
					// Otherwise build the alarm message and resend, even though nothing is changed.
					resend = true;
				}
				// Else something has changed. Build and explanatory message.
				
				action = "building alarm message";
				StringBuilder sb = new StringBuilder("Value " + numFmt.format(value) + " at time " + sdf.format(t) + ": ");
				boolean needComma = false;
				if ((scrFlags & HdbFlags.SCR_VALUE_MASK) != 0)
				{
					String range = "GOOD";
					switch(scrFlags & HdbFlags.SCR_VALUE_MASK)
					{
					case HdbFlags.SCR_VALUE_REJECT_HIGH:   range = "REJECT_HIGH "; break;
					case HdbFlags.SCR_VALUE_CRITICAL_HIGH: range = "CRITICAL_HIGH "; break;
					case HdbFlags.SCR_VALUE_WARNING_HIGH:  range = "WARNING_HIGH "; break;
					case HdbFlags.SCR_VALUE_WARNING_LOW:   range = "WARNING_LOW "; break;
					case HdbFlags.SCR_VALUE_CRITICAL_LOW:  range = "CRITICAL_LOW "; break;
					case HdbFlags.SCR_VALUE_REJECT_LOW:    range = "REJECT_LOW "; break;
					}
					
					sb.append("value in " + range + " range");
					needComma = true;
				}
					
				if ((scrFlags & HdbFlags.SCR_ROC_MASK) != 0)
				{
					String range = "GOOD";
					switch(scrFlags & HdbFlags.SCR_ROC_MASK)
					{
					case HdbFlags.SCR_ROC_REJECT_HIGH:   range = "REJECT_HIGH "; break;
					case HdbFlags.SCR_ROC_CRITICAL_HIGH: range = "CRITICAL_HIGH "; break;
					case HdbFlags.SCR_ROC_WARNING_HIGH:  range = "WARNING_HIGH "; break;
					case HdbFlags.SCR_ROC_WARNING_LOW:   range = "WARNING_LOW "; break;
					case HdbFlags.SCR_ROC_CRITICAL_LOW:  range = "CRITICAL_LOW "; break;
					case HdbFlags.SCR_ROC_REJECT_LOW:    range = "REJECT_LOW "; break;
					}
					
					if (needComma)
						sb.append(", ");
					sb.append("rate-of-change=" + numFmt.format(delta) + " over " + limitSet.getRocInterval() + " "
							+ " is in " + range + " range");
					needComma = true;
				}
				
				if ((scrFlags & HdbFlags.SCR_STUCK_SENSOR_DETECTED) != 0)
				{
					if (needComma)
						sb.append(", ");
					sb.append("Stuck Sensor Detected: variance=" + numFmt.format(variance) + " over "
						+ limitSet.getStuckDuration());
					needComma = true;
				}
					
				sb.append(".");
				
				// If a group is used, queue for an email message.
				// This will cancel any previous MISSING assertion. Good!
				action = "enqueuing alarm for email";
				String alarmMsg = sb.toString();
				if (!DbKey.isNull(scrn.getAlarmGroupId()))
				{
Logger.instance().debug3("AlarmManager, group is not null, enqueuing alarm for email '" + alarmMsg + "'");
					msgQ.add(new AlarmMsg(scrn.getAlarmGroupId(), tsid, now, limitSet.getHintText(), alarmMsg));
				}
				
				// Write the alarm to my cache and the database current_alarm table.
				action = "finalizing alarm message";
				if (currentAlarm == null)
				{
					currentAlarm = new Alarm();
					currentAlarm.setTsidKey(tsid.getKey());
					currentAlarm.setTsid(tsid);
					currentAlarms.put(tsid.getKey(), currentAlarm);
				}
				currentAlarm.setLimitSetId(limitSet.getLimitSetId());
				currentAlarm.setLimitSet(limitSet);
				if (!resend)
					currentAlarm.setAssertTime(now);	
				currentAlarm.setDataValue(value);
				currentAlarm.setDataTime(t);
				currentAlarm.setAlarmFlags(scrFlags);
				currentAlarm.setMessage(alarmMsg);
				if (!DbKey.isNull(scrn.getAlarmGroupId()))
					currentAlarm.setLastNotificationTime(now);

				action = "writing message to alarm_current table";
				alarmDAO.writeToCurrent(currentAlarm);
			}
		}
		catch (Exception ex)
		{
			Logger.instance().warning(module + ".checkAlarms tsid=" + tsid + " error while " + action + ": " + ex);
			PrintStream ps = Logger.instance().getLogOutput();
			if (ps != null)
				ex.printStackTrace(ps);
		}
		finally
		{
			alarmDAO.close();
		}
		
	}
	
	public void missingCheckResults(TimeSeriesIdentifier tsid, Date chkTime, int numReceived, int numExpected,
		AlarmScreening scrn, AlarmLimitSet limitSet)
	{
		Alarm currentAlarm = currentAlarms.get(tsid.getKey());
		boolean isMissing = numExpected - numReceived > limitSet.getMaxMissingValues();
Logger.instance().debug3("AlarmManager.missingCheckResults tsid='" + tsid.getUniqueString() + "' chkTime="
+ sdf.format(chkTime) + ", numReceived=" + numReceived + ", numExpected=" + numExpected + ", isMissing=" + isMissing);
		if (!isMissing)
		{
			// MISSING alarms will be cancelled in the 'checkAlarms' method above when new
			// data is received for the TSID. So there's no need to do anything here.
			return;
		}
		
		// Alarm assertions only go forward in time.
		if (currentAlarm != null && chkTime.before(currentAlarm.getDataTime()))
			return;
		if (currentAlarm == null 
		 && System.currentTimeMillis()-chkTime.getTime() > (notifyMaxAgeDays * MS_PER_DAY))
			return;

		String missingMsg =
			"Missing data for " + tsid.getUniqueString() + " at time " + sdf.format(chkTime)
			+ ": expected at least " + numExpected + " values in the past "
			+ limitSet.getMissingPeriod() + " but received " + numReceived;

		if (currentAlarm == null)
		{
			// This is a new alarm condition
			currentAlarm = new Alarm();
			currentAlarm.setTsid(tsid);
			currentAlarm.setTsidKey(tsid.getKey());
			currentAlarm.setLimitSet(limitSet);
			currentAlarm.setLimitSetId(limitSet.getLimitSetId());
			currentAlarm.setAssertTime(chkTime);
			// There is no data value or data time to set!
			currentAlarm.setAlarmFlags(HdbFlags.SCR_MISSING_VALUES_EXCEEDED);
			currentAlarm.setMessage(missingMsg);
			// No end time
			// No cancelledBy
			currentAlarms.put(tsid.getKey(), currentAlarm);
		}
		else // There is already an existing alarm assertion for this tsid.
		{
			if ((currentAlarm.getAlarmFlags() & HdbFlags.SCR_MISSING_VALUES_EXCEEDED) != 0)
				// missing alarm already asserted: do nothing.
				return;
			else
			{
				currentAlarm.setAlarmFlags(currentAlarm.getAlarmFlags() | HdbFlags.SCR_MISSING_VALUES_EXCEEDED);
				currentAlarm.setMessage(currentAlarm.getMessage() + "\n" + missingMsg);
				// fall through to enqueue an alarm message
			}
		}

		// Update/Assert the alarm in the database
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		try { alarmDAO.writeToCurrent(currentAlarm); }
		finally { alarmDAO.close(); }

		// Enqueue a message for the background thread to send via email.
		msgQ.add(new AlarmMsg(scrn.getAlarmGroupId(), tsid, chkTime, limitSet.getHintText(), 
			currentAlarm.getMessage()));
	}
	
	private void refreshCurrentAlarms() 
		throws DbIoException
	{
		AlarmDAI alarmDAO = tsdb.makeAlarmDAO();
		try
		{
			alarmDAO.refreshCurrentAlarms(currentAlarms);
			Logger.instance().debug1(module + " after refresh there are " + currentAlarms.size()
				+ " currently asserted alarms.");
		}
		finally
		{
			alarmDAO.close();
		}
	}
}
