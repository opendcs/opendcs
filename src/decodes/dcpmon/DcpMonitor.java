/**
 * $Id$
 * 
 * $Log$
 * Revision 1.7  2015/01/16 16:11:04  mmaloney
 * RC01
 *
 * Revision 1.6  2014/12/11 20:24:07  mmaloney
 * Logging mods.
 *
 * Revision 1.5  2014/11/19 16:05:40  mmaloney
 * dev
 *
 * Revision 1.4  2014/09/15 13:59:42  mmaloney
 * DCP Mon Daemon Impl
 *
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
import decodes.routing.DacqEventLogger;
import decodes.routing.RoutingScheduler;
import decodes.routing.ScheduleEntryExecutive;
import decodes.tsdb.DbIoException;
import decodes.util.CmdLineArgs;
import decodes.util.PropertySpec;

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
	
	private static PropertySpec[] dcpmonProps =
	{
		
		new PropertySpec("numDaysStorage", PropertySpec.INT,
			"Number of days for which to store data (default=5)"),
		new PropertySpec("omitFailureCodes", PropertySpec.STRING,
			"List of character codes to omit"),
		new PropertySpec("redMsgTime", PropertySpec.INT,
			"Limit in seconds for red message time alarms (default=0)"),
		new PropertySpec("yellowMsgTime", PropertySpec.INT,
			"Limit in seconds for yellow message time warnings (default=2)"),
		new PropertySpec("redFailureCodes", PropertySpec.STRING,
			"Any of these characters will show as red failure codes"),
		new PropertySpec("yellowFailureCodes", PropertySpec.STRING,
			"Any of these characters will show as yellow failure codes (default=?UT)"),
		new PropertySpec("redFreqOffset", PropertySpec.INT,
			"Offsets greater than this will show in red (default=6)"),
		new PropertySpec("yellowFreqOffset", PropertySpec.INT,
			"Offsets greater than this will show in yellow (default=5)"),
		new PropertySpec("redSignalStrength", PropertySpec.INT,
			"Values less than this will show as red signal strength."),
		new PropertySpec("yellowSignalStrength", PropertySpec.INT,
			"Values less than this will show as yellow signal strength."),
		new PropertySpec("redBattery", PropertySpec.NUMBER,
			"BV less than this will show in red."),
		new PropertySpec("yellowBattery", PropertySpec.NUMBER,
			"BV less than this will show in yellow."),
		new PropertySpec("maxCarrierMS", PropertySpec.INT,
			"Carrier more than this (ms) will result in 'C' code."),
		new PropertySpec("hadsUse", PropertySpec.BOOLEAN,
			"Deprecated -- replaced by nwisXref params"),
		new PropertySpec("hadsUrl", PropertySpec.STRING,
			"Deprecated -- replaced by nwisXref params"),
		new PropertySpec("hadsLocalFile", PropertySpec.FILENAME,
			"Deprecated -- replaced by nwisXref params"),
		new PropertySpec("allChannels", PropertySpec.BOOLEAN,
			"This is used to tell the Dcp Monitor that we want to monitor "
			+ "all available GOES channels (default=false)."),

		new PropertySpec("grp:<netlist-name>", PropertySpec.STRING,
			"Add properties with name 'grp:' plus the network list name. Prop value is "
			+ "the display name you want shown on the DCP Monitor web page. This "
			+ "allows you to specify which lists are shown for selection on the web page"
			+ " and to supply a descriptive name for each."),
		new PropertySpec("webUsesRoutingSpecNetlists", PropertySpec.BOOLEAN,
			"(default=true) True means that all of the network lists assigned to the "
			+ "'dcpmon' routing spec are also shown as groups for selection on the "
			+ "DCP Monitor web page. To disable this behavior and only show lists "
			+ "defined here with the 'grp:' prefix, set this property to false."),
			
		new PropertySpec("mergeDir", PropertySpec.DIRECTORY,
			"Directory for merging lists and decode specs from different districts."
			+ " (default=$DECODES_INSTALL_DIR/dcptoimport)"),
		new PropertySpec("controlDistList", PropertySpec.STRING,
			"File name of the control-district-list file within the merge dir. "
			+ "(default=controlling-districts.txt)"),
		new PropertySpec("controlDistSuffix", PropertySpec.STRING,
			"This is used in particular for the RiverGages group names. "
			+ "This is used to convert from District to an actual group name "
			+ "in the dcpmon.conf"),
			
		new PropertySpec("pdtLocalFile", PropertySpec.FILENAME,
			"GOES PDT is downloaded to this local file"),
		new PropertySpec("pdtUrl", PropertySpec.STRING,
			"GOES PDT is downloaded from this URL. No download if left blank."),
		new PropertySpec("cdtLocalFile", PropertySpec.FILENAME,
			"GOES Channel Info stored in this local file"),
		new PropertySpec("cdtUrl", PropertySpec.STRING,
			"GOES Channel Info downloaded from this URL. No download if left blank."),
		new PropertySpec("nwsXrefLocalFile", PropertySpec.FILENAME,
			"National Weather Service cross-reference stored in this local file"),
		new PropertySpec("nwsXrefUrl", PropertySpec.STRING,
			"National Weather Service cross-reference downloaded from this URL. "
			+ "No download if left blank."),
		new PropertySpec("dcpmonNameType", PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Site name type to use in display pages on DCP Monitor"),
		new PropertySpec("rtstatURL", PropertySpec.STRING,
			"URL to dynamic HTML file being updated by LRGS Daemon. Set to '-' to disable this feature."),
		new PropertySpec("statusErrorThreshold", PropertySpec.INT,
			"# of seconds. Devices and Platforms with an error within this amount of time are shown in red.")
	};

	/**
	 * Singleton instance method for DcpMonitor
	 */
	public static DcpMonitor instance()
	{
		if (_instance == null)
			_instance = new DcpMonitor();
		return _instance;
	}

	public DcpMonitor()
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

		// Initialize the PDT and Channel Map. */
		setStatus("Initializing PDT Maintenance");
		DecodesInterface.maintainGoesPdt();
		
		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot determine hostname, will use 'localhost': " + ex);
			hostname = "localhost";
		}
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
		
		// We don't want to 'refresh' our mocked-up routing specs from the db template.
		ScheduleEntryExecutive.setRereadRsBeforeExec(false);

		executives.clear();
		ScheduleEntryExecutive see = new ScheduleEntryExecutive(rtScheduleEntry, this);
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
    public void setThreadLogger(Thread thread, Logger logger)
    {
    	// Don't want thread loggers for dcpmon. Do nothing.
    }
    @Override
    protected void loadConfig(Properties properties)
    {
    	DcpMonitorConfig.instance().loadFromProperties(properties);
    }
    
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(dcpmonProps, super.getSupportedProps());
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}


}
