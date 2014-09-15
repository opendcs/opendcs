/**
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2014/08/22 17:23:06  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 *
 * Copyright 2014 Cove Software, LLC
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
package decodes.dcpmon;

import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.gui.DecodesInterface;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.RoutingSpec;
import decodes.db.ScheduleEntry;
import decodes.routing.RoutingScheduler;
import decodes.routing.ScheduleEntryExecutive;
import decodes.tsdb.DbIoException;
import decodes.util.CmdLineArgs;

/**
Main class for DCP Monitor Server
*/
public class DcpMonitor
	extends RoutingScheduler
{
	private XRWriteThread xrWriteThread = null;
	private DcpMonitorConfig dcpMonitorConfig = new DcpMonitorConfig();
	private static DcpMonitor _instance = null;
	private String status = "";
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z");
	
	/**
	 * Singleton instance method for DcpMonitor
	 */
	public static DcpMonitor instance()
	{
		if (_instance == null)
			_instance = new DcpMonitor();
		return _instance;
	}

	private DcpMonitor()
	{
		super("dcpmon");
		module = "DcpMonitor";
		debugSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

    public static void main(String args[]) throws Exception
    {
		DcpMonitor dcpmon = instance();		
		dcpmon.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		appNameArg.setDefaultValue("dcpmon");
	}
	
	@Override
	protected void oneTimeInit()
	{
    	setStatus("oneTimeInit");

    	surviveDatabaseBounce = true;

		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "unknown"; }

		// Initialize the PDT and Channel Map. */
		setStatus("Initializing PDT Maintenance");
		DecodesInterface.maintainGoesPdt();
	}

	@Override
	protected void runAppInit()
	{
		super.runAppInit();
		
		// DCP Mon specific run app init here...
		setStatus("Starting xrWriteThread");
		xrWriteThread = new XRWriteThread();
		xrWriteThread.start();
		
		// Put my consumer type into the consumer type enum.
		Database db = Database.getDb();
		decodes.db.DbEnum ctenum = db.getDbEnum(Constants.enum_DataConsumer);
		ctenum.replaceValue("DcpMonitorConsumer", 
			"Custom consumer for the DCP Monitor Application",
			"decodes.dcpmon.DcpMonitorConsumer", null);
		
		info("Initializing Routing Spec.");
		
		RoutingSpec dbRoutingSpec = Database.getDb().routingSpecList.find("dcpmon");
		DcpMonRoutingSpec rtRoutingSpec = null;
		if (dbRoutingSpec == null)
		{
			failure("Cannot find required routing spec 'dcpmon'. Create this in the" +
				" DECODES database before starting the daemon.");
			shutdownFlag = true;
			return;
		}
		else if (dbRoutingSpec instanceof DcpMonRoutingSpec)
		{
			// This is not the first time. We have already subclassed it.
			rtRoutingSpec = (DcpMonRoutingSpec)dbRoutingSpec;
		}
		else // this IS the first time. Make the subclass
		{
			rtRoutingSpec = new DcpMonRoutingSpec();
			rtRoutingSpec.setName("dcpmon-realtime");
			RoutingSpec.copy(rtRoutingSpec, dbRoutingSpec);
			// This will replace the one in the list with the DcpMonRoutingSpec
			Database.getDb().routingSpecList.add(rtRoutingSpec);
		}
		
		// Make sure the fields are appropriate for DCP Monitor
		rtRoutingSpec.outputFormat = "null";
		rtRoutingSpec.outputTimeZoneAbbr = "UTC";
		rtRoutingSpec.consumerType = "DcpMonitorConsumer";
		
		// Real time since is no more than an hour ago.
		Date rtSince = new Date(System.currentTimeMillis() - 3600000L);
		Date lastLocalRecvTime = xrWriteThread.getLastLocalRecvTime();
		
		// If this is the first startup for dcpmon. Get specified # days of data.
		Date recoverLimit = new Date(System.currentTimeMillis() - 
			3600000L * 24 * dcpMonitorConfig.numDaysStorage);
		if (lastLocalRecvTime == null || lastLocalRecvTime.before(recoverLimit))
		{
			lastLocalRecvTime = recoverLimit;
			info("First run, defaulting lastLocalRecvTime to " 
				+ debugSdf.format(lastLocalRecvTime));
		}
		if (lastLocalRecvTime.after(rtSince))
			rtSince = lastLocalRecvTime;
		debug("lastLocalRecvTime=" + debugSdf.format(lastLocalRecvTime)
			+", rtSince=" + debugSdf.format(rtSince));
		
		rtRoutingSpec.sinceTime = IDateFormat.toString(rtSince, false);
		rtRoutingSpec.untilTime = null;
		rtRoutingSpec.setProperty("sc:RT_SETTLE_DELAY", "true");
		rtRoutingSpec.setProperty("sc:DAPS_STATUS", "A");
		
		ScheduleEntry rtScheduleEntry = null;
		// read schedule entries assigned to DCP Mon.
		// Can be 0, 1, or 2. for realtime and recover.
		// If they don't exist, they will be created.
		ArrayList<ScheduleEntry> dcpMonSched = null;
		try
		{
			dcpMonSched = scheduleEntryDAO.listScheduleEntries(appInfo);
			info("Read DCP Mon Schedule: " + dcpMonSched.size() + " entries:");
			for(ScheduleEntry se : dcpMonSched)
				info("\tid=" + se.getId() + ", name='" + se.getName() + "'");
		}
		catch (DbIoException ex)
		{
			failure("Cannot read DCP Mon Schedule Entries: " + ex);
			databaseFailed = true;
			shutdownFlag = true;
			return;
		}
		
		for(ScheduleEntry se : dcpMonSched)
			if (se.getName().equalsIgnoreCase("dcpmon-realtime"))
			{	rtScheduleEntry = se;
				break;
			}
		if (rtScheduleEntry == null)
		{
			rtScheduleEntry = new ScheduleEntry("dcpmon-realtime");
			info("Creating new schedule entry '" + rtScheduleEntry.getName() + "'");
		}
		rtScheduleEntry.setLoadingAppId(appId);
		rtScheduleEntry.setRoutingSpecId(rtRoutingSpec.getId());
		rtScheduleEntry.setStartTime(null); // null start time means real-time schedule entry.
		rtScheduleEntry.setEnabled(true);
		rtScheduleEntry.setLoadingAppName(appNameArg.getValue());
		rtScheduleEntry.setRoutingSpecName(rtRoutingSpec.getName());
		try { scheduleEntryDAO.writeScheduleEntry(rtScheduleEntry); }
		catch(DbIoException ex)
		{
			failure("Cannot write rtScheduleEntry: " + ex);
			databaseFailed = true;
			shutdownFlag = true;
			return;
		}
		
		executives.clear();
		ScheduleEntryExecutive see = new ScheduleEntryExecutive(rtScheduleEntry, this);
		see.setRereadRsBeforeExec(false);
		executives.add(see);

		// If we've been down for too long, we have to make a recover routing spec too.
		if (lastLocalRecvTime.before(rtSince))
		{
			RoutingSpec recRoutingSpec = new DcpMonRoutingSpec();
			recRoutingSpec.setName("dcpmon-recover");
			RoutingSpec.copy(recRoutingSpec, rtRoutingSpec);
			info("Creating recovery routing spec '" + recRoutingSpec.getName()
				+ "' to handle data from " + debugSdf.format(lastLocalRecvTime)
				+ " to " + debugSdf.format(rtSince));
			Database.getDb().routingSpecList.add(recRoutingSpec);
			recRoutingSpec.sinceTime = IDateFormat.toString(lastLocalRecvTime, false);
			recRoutingSpec.untilTime = IDateFormat.toString(rtSince, false);
			recRoutingSpec.setProperty("sc:RT_SETTLE_DELAY", "false");

			ScheduleEntry recScheduleEntry = null;
			for(ScheduleEntry se : dcpMonSched)
				if (se.getName().equalsIgnoreCase("dcpmon-recover"))
				{	recScheduleEntry = se;
					break;
				}
			if (recScheduleEntry == null)
			{
				recScheduleEntry = new ScheduleEntry("dcpmon-recover");
				info("Creating new schedule entry '" + recScheduleEntry.getName() + "'");
			}
			recScheduleEntry.setLoadingAppId(appId);
			recScheduleEntry.setRoutingSpecId(recRoutingSpec.getId());
			recScheduleEntry.setStartTime(new Date()); // start time means run once.
			recScheduleEntry.setEnabled(true);
			recScheduleEntry.setLoadingAppName(appNameArg.getValue());
			recScheduleEntry.setRoutingSpecName(recRoutingSpec.getName());
			try { scheduleEntryDAO.writeScheduleEntry(recScheduleEntry); }
			catch(DbIoException ex)
			{
				failure("Cannot write recScheduleEntry: " + ex);
				databaseFailed = true;
				shutdownFlag = true;
				return;
			}
			ScheduleEntryExecutive rsee = new ScheduleEntryExecutive(recScheduleEntry, this);
			rsee.setRereadRsBeforeExec(false);
			executives.add(rsee);
		}
	}
	
	@Override
	protected void refreshSchedule() 
		throws DbIoException
	{
		// TODO check to see if the real time routing spec has died because of some kind of error.
		// If so, restart it.
		
	}

	@Override
	public void runAppShutdown()
	{
		super.runAppShutdown();
		if (xrWriteThread != null)
		{
			xrWriteThread.shutdown();
			xrWriteThread = null;
		}
	}

	public void handleDbIoException(String doingWhat, Throwable ex)
	{
		String msg = "Error " + doingWhat + ": " + ex;
		failure(msg);
		System.err.println("\n" + (new Date()) + " " + msg);
		ex.printStackTrace();
		shutdownFlag = true;
		databaseFailed = true;
	}

    public void info(String msg)
    {
    	Logger.instance().info("DCPMON " + msg);
    }
    public void warning(String msg)
    {
    	Logger.instance().warning("DCPMON " + msg);
    }
    public void failure(String msg)
    {
    	Logger.instance().failure("DCPMON " + msg);
    }
    public void debug(String msg)
    {
    	Logger.instance().debug1("DCPMON " + msg);
    }
    public void setStatus(String status)
    {
    	this.status = status;
    	info(status);
    }

    public XRWriteThread getXrWriteThread()
	{
		return xrWriteThread;
	}
    
    @Override
    public void makeThreadLogger(Thread thread)
    {
    	// Don't want thread loggers for dcpmon. Do nothing.
    }
    @Override
    protected void loadConfig(Properties properties)
    {
    	DcpMonitorConfig.instance().loadFromProperties(properties);
    }

}
