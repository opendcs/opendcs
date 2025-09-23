/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* Copyright 2017 Cove Software, LLC. All rights reserved.
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
package decodes.tsdb.alarm;

import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.TextUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import opendcs.dai.AlarmDAI;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dao.AlarmDAO;
import lrgs.gui.DecodesInterface;
import decodes.polling.DacqEvent;
import decodes.routing.DacqEventLogger;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.tsdb.alarm.mail.AlarmMailer;
import decodes.tsdb.alarm.mail.MailerException;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.PropertySpec;

public class AlarmMonitor extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private boolean shutdownFlag = false;
	private int evtPort = 0;
	private CompEventSvr compEventSvr = null;
	private String hostname = "unknown";
	private AlarmConfig alarmConfig = new AlarmConfig();

	/** Holds app name, id, & description. */
	private CompAppInfo appInfo;

	/** My lock in the time-series database */
	private TsdbCompLock myLock;

	private int periodSec = 60;
	private int numEvents = 0, numAlarms=0, numEmails=0;

	private DbKey lastEventId = DbKey.NullKey;
	private Date startTime = new Date();
	private DacqEventLogger dacqLogger = null;
	private AlarmMailer alarmMailer = null;
	private String myHostname = null;


	private static PropertySpec[] alarmMonProps =
	{
		new PropertySpec("monitor", PropertySpec.BOOLEAN,
			"Set to true to allow monitoring from the GUI."),
		new PropertySpec("periodSec", PropertySpec.INT,
			"(default=60) Check for new alarms every this number of seconds."),
		new PropertySpec("mail.smtp.auth", PropertySpec.BOOLEAN,
			"(default=true) Set to false for unauthenticated SMTP connection for outgoing mail."),
		new PropertySpec("mail.smtp.starttls.enable", PropertySpec.BOOLEAN,
			"(default=true) Set to false for cleartext communication with SMTP server for outgoing"
			+ "mail."),
		new PropertySpec("mail.smtp.host", PropertySpec.HOSTNAME,
			"(required) Host name of SMTP server for outgoing mail."),
		new PropertySpec("mail.smtp.port", PropertySpec.INT,
			"(default=587) Port for connecting to SMTP server for outgoing mail."),
		new PropertySpec("smtp.username", PropertySpec.STRING,
			"(required) user name for connecting to SMTP server for outgoing mail."),
		new PropertySpec("smtp.password", PropertySpec.STRING,
			"(required) password for connecting to SMTP server for outgoing mail."),
		new PropertySpec("fromAddr", PropertySpec.STRING,
			"(required) Email address that outgoing mail will be coming FROM."),
		new PropertySpec("smtp.password", PropertySpec.STRING,
			"(required) plain text name corresponding to the from address."),
		new PropertySpec("fromName", PropertySpec.STRING,
			"(required) Plain text name that outgoing mail will be coming FROM."),
		new PropertySpec("mailer.class", PropertySpec.STRING,
			"Alternate class to use for sending mail.")
	};

	SimpleDateFormat msgSdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss ");

	public AlarmMonitor()
	{
		super("alarmmon.log");
		surviveDatabaseBounce = true;
		msgSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try { myHostname = java.net.InetAddress.getLocalHost().getHostName(); }
		catch(Exception ex)
		{
			myHostname = "unknownServer";
		}
	}

	public static void main(String[] args)
		throws Exception
	{
		AlarmMonitor app = new AlarmMonitor();
		app.execute(args);
	}


	@Override
	public PropertySpec[] getSupportedProps()
	{
		return alarmMonProps;
	}

	/**
	 * Called at the beginning of each call to runApp
	 */
	private void initialize()
		throws LockBusyException, DbIoException, NoSuchObjectException, MailerException
	{
		dacqLogger = new DacqEventLogger(null);

		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			String hostname = "unknown";
			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { }

			appInfo = loadingAppDao.getComputationApp(getAppId());
			if (!appInfo.canRunLocally())
			{
				shutdownFlag = true;
				throw new NoSuchObjectException("The 'allowedHosts' property for application '"
					+ appInfo.getAppName()
					+ "' does not allow this application to run on this machine (hostname="
					+ hostname + ").");
			}
			String s = appInfo.getProperty("periodSec");
			try { periodSec = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				log.atWarn().setCause(ex).log("Bad periodSec property value '{}' -- will use default=60", s);
				periodSec = 60;
			}

			// Look for EventPort and EventPriority properties. If found,
			if (TextUtil.str2boolean(appInfo.getProperty("monitor")))
			{
				try
				{
					if (compEventSvr == null)
					{
						// Try to use PID to pick a randomized number in the 20000-29999 range.
						evtPort = 20000 + (getPID() % 10000);
						// But honor the user's choice if one was made.
						String evtPorts = appInfo.getProperty("EventPort");
						if (evtPorts != null)
						{
							try
							{
								evtPort = Integer.parseInt(evtPorts.trim());
							}
							catch(NumberFormatException ex)
							{
								evtPort = 20000 + (getPID() % 10000);
								log.atWarn()
								   .setCause(ex)
								   .log("Bad EventPort property '{}' must be integer -- will use '{}'.",
								   		evtPorts, evtPort);
							}
						}
						log.info("Starting event server on port {}", evtPort);
						CompEventSvr compEventSvr = new CompEventSvr(evtPort);
						compEventSvr.startup();
					}
				}
				catch(IOException ex)
				{
					log.atError().setCause(ex).log("Cannot create Event server -- no events available to external clients.");
				}
			}
		}
		finally
		{
			loadingAppDao.close();
		}

		String mailerClass = appInfo.getProperty("mailer.class");
		if (mailerClass != null)
		{
			log.info("Loading Mailer class '{}'", mailerClass);
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			try
			{
				Class<?> cls = cl.loadClass(mailerClass);
				alarmMailer = (AlarmMailer)cls.newInstance();
			}
			catch (ClassNotFoundException ex)
			{
				String errmsg = "AlarmMonitor.initialize() cannot load mailer class '"
					+ mailerClass + "'";
				throw new MailerException(errmsg, ex);
			}
			catch (InstantiationException ex)
			{
				String errmsg = "AlarmMonitor.initialize() cannot instantiate mailer class '"
					+ mailerClass + "'";
				throw new MailerException(errmsg, ex);
			}
			catch (IllegalAccessException ex)
			{
				String errmsg = "AlarmMonitor.initialize() Error initializing mailer class '"
					+ mailerClass + "'";
				throw new MailerException(errmsg, ex);
			}
		}
		else
			alarmMailer = new AlarmMailer();
		alarmMailer.configure(appInfo.getProperties());

		File lastEvtIdFile = new File(EnvExpander.expand("$DCSTOOL_USERDIR/last-alarm"));
		try
		{
			String s = FileUtil.getFileContents(lastEvtIdFile);
			lastEventId = DbKey.createDbKey(Long.parseLong(s.trim()));
			log.info("Last event ID = {}", lastEventId);
		}
		catch(Exception ex)
		{
			LoggingEventBuilder le = log.atWarn().setCause(ex);
			if (ex instanceof NumberFormatException)
			{
				le.log("File '{}' has invalid content. Supposed to be numeric last Event ID processed.",
					   lastEvtIdFile.getPath());
			}
			else if (ex instanceof IOException)
			{
				le.log("Cannot read {}", lastEvtIdFile.getPath());
			}

			//Get min evt id in file that is > now - 1 hour.
			DacqEventDAI evtDAO = theDb.makeDacqEventDAO();
			try
			{
				lastEventId =
					evtDAO.getFirstIdAfter(new Date(System.currentTimeMillis() - 3600000L));
				log.info("After now - 1 hour search, last event ID = {}", lastEventId);
			}
			finally
			{
				evtDAO.close();
			}
		}

		log.info("Initialized. periodSec={}", periodSec);

	}

	@Override
	protected void runApp() throws Exception
	{
		shutdownFlag = false;
		initialize();

		String action="";
		long lastWorkDoneMsec = System.currentTimeMillis();
		while(!shutdownFlag)
		{
			long now = System.currentTimeMillis();
			if (now - lastWorkDoneMsec > (periodSec * 1000L))
			{
				lastWorkDoneMsec = now;
				checkMetaData();
				processEvents();
				processFiles();
				sendGeneratedAlarms();
			}

			// Now do the stuff that should be done every second.
			LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
			try
			{
				// Make sure this process's lock is still valid.
				action = "Checking lock";
				if (myLock == null)
					myLock = loadingAppDAO.obtainCompProcLock(appInfo, getPID(), hostname);
				else
				{
					if (myLock != null)
						myLock.setStatus("Evts=" + numEvents + ", Alarms=" + numAlarms
							+ ", email=" + numEmails);
					loadingAppDAO.checkCompProcLock(myLock);
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
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Unexpected exception while {}", action);
				shutdownFlag = true;
				databaseFailed = true;
			}
			finally
			{
				loadingAppDAO.close();
			}
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}
		}

	}



	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("AlarmMonitor");
	}

	@Override
	public void initDecodes()
		throws DecodesException
	{
		DecodesInterface.silent = true;
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
	}

	@Override
	protected void oneTimeInit()
	{
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "unknown"; }

	}

	private void processEvents()
		throws DbIoException
	{
		ArrayList<DacqEvent> events = getBatchOfEvents();
		log.debug("Processing {} new events.", events.size());;
		for(DacqEvent event : events)
		{
			// Ignore events generated by this process (they are file events).
			if (getAppId().equals(event.getAppId()))
				continue;

			log.atTrace().log(() -> "processEvents event: " + event.getEventText());
			numEvents++;
			// Try to match the events against alarm regular expressions.
			for(AlarmGroup group : alarmConfig.getGroups())
				for(ProcessMonitor procmon : group.getProcessMonitors())
					if (procmon.isEnabled())
						for(AlarmEvent def : procmon.defs)
							if (def.matches(event))
								generateAlarm(group, procmon, def, event);
		}
	}


	private void checkMetaData()
	{
		log.debug("Checking metadata");
		AlarmDAI alarmDAO = new AlarmDAO(theDb);
		try
		{
			if (alarmDAO.check(alarmConfig))
			{
				log.info("Metadata changes loaded.");
			}
		}
		catch (DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Cannot update alarm configuration.");
		}
		finally
		{
			alarmDAO.close();
		}
	}

	private ArrayList<DacqEvent> getBatchOfEvents()
		throws DbIoException
	{
		ArrayList<DacqEvent> events = new ArrayList<DacqEvent>();
		DacqEventDAI evtDAO = theDb.makeDacqEventDAO();
		try
		{
			if (!DbKey.isNull(lastEventId))
				// read all events with ID > lastEventId
				evtDAO.readEventsAfter(lastEventId, events);
			else // read all events with time >= start time
				evtDAO.readEventsAfter(startTime, events);
		}
		finally
		{
			evtDAO.close();
		}
		// If we got some, set lastEventId for next time. Otherwise leave it unchanged.
		if (events.size() > 0)
		{
			lastEventId = events.get(events.size()-1).getDacqEventId();
			File lastEvtIdFile = new File(EnvExpander.expand("$DCSTOOL_USERDIR/last-alarm"));
			try (FileWriter fw = new FileWriter(lastEvtIdFile))
			{
				fw.write("" + lastEventId + "\n");
			}
			catch (IOException ex)
			{
				log.atWarn().setCause(ex).log("Cannot save last event ID to '{}'", lastEvtIdFile.getPath());
			}
		}
		return events;
	}

	private void processFiles()
	{
		for(AlarmGroup grp : alarmConfig.getGroups())
		{
			for(FileMonitor fileMon : grp.getFileMonitors())
			{
				if (!fileMon.isEnabled())
					continue;

				File f = new File(EnvExpander.expand(fileMon.getPath()));
				log.debug("Checking file monitor for {}", f.getPath());
				if (fileMon.isAlarmOnDelete())
				{
					if (f.exists())
					{
						if (fileMon.isExistsAsserted())
						{
							fileMon.setExistsAsserted(false);
							generateAlarm(grp, fileMon, f, false, "File exists again.");
						}
					}
					else // file does NOT exist.
					{
						if (!fileMon.isExistsAsserted())
						{
							fileMon.setExistsAsserted(true);
							generateAlarm(grp, fileMon, f, true, fileMon.getAlarmOnDeleteHint());
						}
					}
				}

				// Unclear what this would be used for
				if (fileMon.isAlarmOnExists())
				{
					if (!f.exists())
					{
						if (fileMon.isExistsAsserted())
						{
							fileMon.setExistsAsserted(false);
							generateAlarm(grp, fileMon, f, false, "File is removed again.");
						}
					}
					else // file exists!
					{
						if (!fileMon.isExistsAsserted())
						{
							fileMon.setExistsAsserted(true);
							generateAlarm(grp, fileMon, f, true, fileMon.getAlarmOnExistsHint());
						}
					}
				}

				if (fileMon.getMaxFiles() > 0 && f.isDirectory())
				{
					int numFiles = f.list().length;
					if (numFiles > fileMon.getMaxFiles())
					{
						if (!fileMon.isSizeAsserted())
						{
							fileMon.setSizeAsserted(true);
							generateAlarm(grp, fileMon, f, true, fileMon.getMaxFilesHint());
						}
					}
					else // numFiles is below or equal to the limit
					{
						if (fileMon.isSizeAsserted())
						{
							fileMon.setSizeAsserted(false);
							generateAlarm(grp, fileMon, f, false, "Directory size below limit again.");
						}
					}
				}

				if (fileMon.getMaxSize() > 0 && f.isFile())
				{
					long size = f.length();
					if (size > fileMon.getMaxSize())
					{
						if (!fileMon.isSizeAsserted())
						{
							fileMon.setSizeAsserted(true);
							generateAlarm(grp, fileMon, f, true, fileMon.getMaxSizeHint());
						}
					}
					else // size is below or equal to limit
					{
						if (fileMon.isSizeAsserted())
						{
							fileMon.setSizeAsserted(false);
							generateAlarm(grp, fileMon, f, false, "File size below limit again.");
						}
					}
				}

				if (fileMon.getMaxLMT() != null)
				{
					IntervalIncrement ii = IntervalIncrement.parse(fileMon.getMaxLMT());
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date());
					cal.add(ii.getCalConstant(), -ii.getCount());

					if (new Date(f.lastModified()).before(cal.getTime()))
					{
						if (!fileMon.isLmtAsserted())
						{
							fileMon.setLmtAsserted(true);
							generateAlarm(grp, fileMon, f, true, fileMon.getMaxLMTHint());
						}
					}
					else // File LMT is ok.
					{
						if (fileMon.isLmtAsserted())
						{
							fileMon.setLmtAsserted(false);
							generateAlarm(grp, fileMon, f, false,
								(f.isDirectory() ? "Directory" : "File")
								+ " was modified again on " + new Date(f.lastModified()) + ".");
						}
					}
				}
			}
		}
	}

	private void generateAlarm(AlarmGroup group, ProcessMonitor procmon, AlarmEvent def,
		DacqEvent event)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Process " + procmon.getProcName() + ": "
			+ event.getPriorityStr() + " ");
		if (event.getSubsystem() != null && event.getSubsystem().length() > 0)
			sb.append("(" + event.getSubsystem() + ") ");
		sb.append(event.getTimeStr() + " ");

		sb.append(event.getEventText());
		String txt = sb.toString();
		group.getGeneratedAlarms().add(txt);
		log.atInfo()
		   .addKeyValue("originalPriority", def.getPriority())
		   .log("({}) EVENT ALARM: {}", myHostname, txt);
		numAlarms++;
	}

	private void generateAlarm(AlarmGroup group, FileMonitor fileMon, File file,
		boolean isAssertion, String msg)
	{
		int pri = -1; // TODO: determine replacement after new logger setup.

		String alarmText ="(ALARM (" + myHostname + ") " +
				(isAssertion ?
					("FILE ALARM on " + file.getPath() + " with size " + file.length()
						+ " last modified on " + msgSdf.format(new Date(file.lastModified()))
						+ " (UTC) -- " + msg)
					: ("CANCELLING PREVIOUS ALARM for File " + file.getPath() + ". Size is now "
						+ file.length() + ". Last modify time is now " + msgSdf.format(new Date(file.lastModified()))
						+ " (UTC) -- Alarm message was '"+ msg + "'"));


		group.getGeneratedAlarms().add(alarmText);
		log.atInfo().addKeyValue("isAlarmMessage", true).log(alarmText);
		numAlarms++;
	}

	private void sendGeneratedAlarms()
	{
		for(AlarmGroup group : alarmConfig.getGroups())
			if (group.getGeneratedAlarms().size() > 0)
			{
				try
				{
					alarmMailer.send(group, group.getGeneratedAlarms());
					numEmails++;
				}
				catch (MailerException ex)
				{
					log.atWarn().setCause(ex).log("Cannot send alarm email.");
				}
				group.getGeneratedAlarms().clear();
			}
	}

	public CompAppInfo getAppInfo()
	{
		return appInfo;
	}


}
