package decodes.routing;

import java.util.Date;

import opendcs.dai.PlatformStatusDAI;
import lrgs.gui.DecodesInterface;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.IDateFormat;
import decodes.consumer.DirectoryConsumer;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.RoutingSpec;
import decodes.db.TransportMedium;
import decodes.polling.PollingThread;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;

public class Poll
	extends TsdbAppTemplate
{
	private static String module = "Poll";
	private StringToken stationArg = new StringToken("", "Station Name", "",
		TokenOptions.optArgument |TokenOptions.optRequired, "");
	private StringToken sinceArg = new StringToken("S", "Since Time", "",
		TokenOptions.optSwitch, "");



	public Poll()
	{
		super(module);
	}

	public static void main(String[] args)
		throws Exception
	{
		new Poll().execute(args);
	}

	@Override
	protected void runApp() throws Exception
	{
		Database db = Database.getDb();
		
		// Look up station and make sure it has TM of type polled-modem or polled-tcp.
		String station = stationArg.getValue();
		Platform platform = db.platformList.getBySiteNameValue(station);
		if (platform == null)
			throw new Exception("No platform for site '" + station + "'");
		TransportMedium tm = platform.getTransportMedium("polled-modem");
		if (tm == null)
			tm = platform.getTransportMedium("polled-tcp");
		if (tm == null)
			throw new Exception("Station '" + station + "' has no polled-modem or polled-tcp transport medium.");
		
		String pollType = tm.getMediumType();
		
		RoutingSpec rs = Database.getDb().routingSpecList.find(pollType);
		if (rs == null)
			throw new DecodesException(
				"No routing spec named '" + pollType + "' in database. This is needed as a template.");
		
		// Retrieve up station status and set rs.sinceTime to last poll time.
		PlatformStatusDAI platformStatusDAO = theDb.makePlatformStatusDAO();
		PlatformStatus platStat = platformStatusDAO.readPlatformStatus(platform.getId());
		Date lastMsgTime = null;
		if (platStat != null)
			lastMsgTime = platStat.getLastMessageTime();
		else // default to 4 hours.
			lastMsgTime = new Date(System.currentTimeMillis() - 3600000L * 4);
		rs.sinceTime = IDateFormat.time_t2string((int)(lastMsgTime.getTime()/1000L));
		
		// Argument can override since time in station status.
		String s = sinceArg.getValue().trim();
		if (s.length() > 0)
			rs.sinceTime = s;
		
		// Remove the netlists in the prototype and replace with the single station name.
		rs.networkListNames.clear();
		rs.networkLists.clear();
		String dcpname = station 
			+ (platform.getPlatformDesignator() != null && platform.getPlatformDesignator().length() > 0
			? ("-" + platform.getPlatformDesignator()) : "");
		rs.setProperty("sc:DCP_NAME_0000", dcpname);
		
		ScheduleEntryExecutive.setRereadRsBeforeExec(false);
		final RoutingSpecThread rst = RoutingSpecThread.makeInstance(rs);

		// Set a static arg in PollingThread to tell it to use stdout as session logger.
		PollingThread.staticSessionLogger = System.out;

		// Start the routing spec thread to do the work.
		noExitAfterRunApp = true;
		rst.setShutdownHook(
			new Runnable()
			{
				public void run()
				{
					if (rst.consumer != null && rst.consumer instanceof DirectoryConsumer)
					{
						DirectoryConsumer dc = (DirectoryConsumer)rst.consumer;
						if (dc.getActiveOutput() != null)
							System.out.println("Output written to " + dc.getActiveOutput());
					}
				}
			});
		rst.start();
	}

	@Override
	protected void oneTimeInit()
	{
		// TODO Auto-generated method stub
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		super.addCustomArgs(cmdLineArgs);
		cmdLineArgs.addToken(stationArg);
	}

	@Override
	public void initDecodes() throws DecodesException
	{
		DecodesInterface.initDecodes(cmdLineArgs.getPropertiesFile());
		DecodesInterface.initializeForDecoding();
	}

}
