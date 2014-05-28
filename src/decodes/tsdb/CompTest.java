package decodes.tsdb;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;


import ilex.cmdline.IntegerToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import ilex.var.Variable;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.consumer.PipeConsumer;
import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.DataPresentation;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;

public class CompTest extends TsdbAppTemplate
{
	private StringToken formatterArg = null;
	private StringToken sinceArg = null;
	private StringToken untilArg = null;
	private StringToken tzArg = null;
	private IntegerToken compIdArg = null;
	private static TimeZone tz = null;
	private Properties props = null;
	private OutputFormatter outputFormatter = null;
	private DataConsumer consumer = null;
	private static SimpleDateFormat timeSdf = null;
	private static SimpleDateFormat dateSdf = null;
	private final static long MS_PER_DAY = 3600 * 24 * 1000L;
	private PresentationGroup presGroup = null;


	public CompTest()
	{
		super("comptest.log");
	}

	@Override
	protected void runApp() throws Exception
	{
		DbKey compId = DbKey.createDbKey(compIdArg.getValue());
		if (compId.isNull())
			fatal("-C ComputationID  -- Missing required computation ID argument.");
		tz = TimeZone.getTimeZone(tzArg.getValue());

		setProperties(cmdLineArgs.getCmdLineProps());

		setOutputFormatter(formatterArg.getValue());
		
		consumer = new PipeConsumer();
		consumer.open("", props);

		String s = sinceArg.getValue().trim();
		Date since = convert2Date(s, false);
		
		s = untilArg.getValue().trim();
		Date until = convert2Date(s, true);

		DataCollection dc = new DataCollection();
		
		ComputationDAI computationDao = theDb.makeComputationDAO();

		// Get the input(s) over the requested time range.
		Logger.instance().info("Reading compID=" + compId);
		DbComputation comp = computationDao.getComputationById(compId);
		Logger.instance().info("Read comp, name=" + comp.getName());
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		for(Iterator<DbCompParm> parmIt = comp.getParms(); parmIt.hasNext(); )
		{
			DbCompParm parm = parmIt.next();
			Logger.instance().info("Processing param '" + parm.getRoleName()
				+ "', type=" + parm.getAlgoParmType());
			String pt = parm.getAlgoParmType();
			if (pt == null || pt.length() == 0)
				continue;
			if (pt.charAt(0) != 'i' && pt.charAt(0) != 'I')
				continue;

			Logger.instance().info("Creating time series for role '"
				+ parm.getRoleName() + "' dataId=" + parm.getSiteDataTypeId()
				+ ", filling since=" + since + ", until=" + until);
			CTimeSeries cts = new CTimeSeries(parm.getSiteDataTypeId(), 
				parm.getInterval(), parm.getTableSelector());
			timeSeriesDAO.fillTimeSeries(cts, since, until);
			for(int pos=0; pos<cts.size(); pos++)
				VarFlags.setWasAdded(cts.sampleAt(pos));

			Logger.instance().info("After fill, num values=" + cts.size());
			dc.addTimeSeries(cts);
		}

		comp.prepareForExec(theDb);
		comp.apply(dc, theDb);

		Platform p = null;
		byte[] dummyData = new byte[0];
		Date now = new Date();

		RawMessage rawMsg = new RawMessage(dummyData);
		rawMsg.setPlatform(p);
		rawMsg.setTransportMedium(new TransportMedium(p, "edl", "Test Platform"));
		rawMsg.setTimeStamp(now);
		rawMsg.setHeaderLength(0);
		rawMsg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(now));
		rawMsg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(0L));
		
		outputTimeSeries(rawMsg, dc.getAllTimeSeries());
		timeSeriesDAO.close();
	}

	public void outputTimeSeries(RawMessage rawMsg, Collection<CTimeSeries> ctss)
		throws OutputFormatterException, IOException,
		DataConsumerException, UnknownPlatformException
	{
		DecodedMessage decmsg = new DecodedMessage(rawMsg);
		for(CTimeSeries cts : ctss)
		{
			TimeSeries ts = TimeSeriesHelper.convert2DecodesTimeSeries(cts);
			Sensor sensor = ts.getSensor();
			boolean toAdd = true;
			if (presGroup != null)
			{
				DataPresentation dp = presGroup.findDataPresentation(sensor);
				if (dp != null)
				{
					if (dp.getUnitsAbbr() != null
					 && dp.getUnitsAbbr().equalsIgnoreCase("omit"))
					{
						Logger.instance().log(Logger.E_DEBUG2,
							"Omitting sensor '" + sensor.getName() 
							+ "' as per Presentation Group.");
						toAdd = false;
					}
					else
						ts.formatSamples(dp);
				}
			}
			if (toAdd)
				decmsg.addTimeSeries(ts);
		}
		
		outputFormatter.formatMessage(decmsg, consumer);
	}

	
	public void setProperties(Properties props)
	{
		this.props = props;
	}
	/**
	 * Create the formatter for output. You should call setTimeZone,
	 * setPresentationGroup and setProperties before calling this method.
	 */
	public void setOutputFormatter(String formatterName)
		throws OutputFormatterException
	{
		outputFormatter = OutputFormatter.makeOutputFormatter(
			formatterName, tz, null, props);
	}
	
	public static Date convert2Date(String s, boolean isTo)
	{
		if (timeSdf == null)
		{
			timeSdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm");
			timeSdf.setTimeZone(tz);
			dateSdf = new SimpleDateFormat("dd-MMM-yyyy");
			dateSdf.setTimeZone(tz);
		}
		s = s.trim().toLowerCase();
		if (s.equalsIgnoreCase("now"))
			return new Date();
		if (s.equals("today") || s.equals("yesterday") || s.startsWith("this"))
		{
			GregorianCalendar cal = new GregorianCalendar(
				dateSdf.getTimeZone());
			cal.setTime(new Date());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			if (s.equals("today"))
				return cal.getTime();
			else if (s.equals("yesterday"))
			{
				cal.add(Calendar.DAY_OF_YEAR, -1);
				return cal.getTime();
			}
			else if (s.equals("this_week"))
			{
				cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
				return cal.getTime();
			}
			else if (s.equals("this_month"))
			{
				cal.set(Calendar.DAY_OF_MONTH, 1);
				return cal.getTime();
			}
			else if (s.equals("this_year"))
			{
				cal.set(Calendar.DAY_OF_YEAR, 1);
				return cal.getTime();
			}
			else
			{
				Logger.instance().warning("Unknown time '" + s + "'");
				return isTo ? new Date() : convert2Date("yesterday", false);
			}
		}
		try 
		{
			Date d = timeSdf.parse(s);
			return d;
		}
		catch(ParseException ex) 
		{
			try
			{
				// Only date provided, if this is end-time, add 23hr59min59sec
				Date d = dateSdf.parse(s);
				if (isTo)
					d.setTime(d.getTime() + (MS_PER_DAY-1));
				return d;
			}
			catch(ParseException ex2) 
			{
				Logger.instance().warning("Bad time format '" + s + "'");
				return isTo ? new Date() : convert2Date("yesterday", false);
			}
		}
	}


	private void fatal(String msg)
	{
		Logger.instance().fatal(msg);
		System.err.println(msg);
		System.exit(1);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
		throws Exception
	{
		// TODO Auto-generated method stub
		CompTest compTest = new CompTest();
		compTest.execute(args);
	}

	/**
	 * Override this and add any program-specific arguments.
	 */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		formatterArg = new StringToken("F", "OutputFormat", "", 
			TokenOptions.optSwitch, "human-readable");
		cmdLineArgs.addToken(formatterArg);
		sinceArg = new StringToken("S", "Since Time (dd-MMM-yyyy HH:mm)", "", 
			TokenOptions.optSwitch, "yesterday");
		cmdLineArgs.addToken(sinceArg);
		untilArg = new StringToken("U", "Until Time", "", 
			TokenOptions.optSwitch, "now");
		cmdLineArgs.addToken(untilArg);
		tzArg = new StringToken("Z", "Time Zone", "", 
			TokenOptions.optSwitch, "UTC");
		cmdLineArgs.addToken(tzArg);
		compIdArg = new IntegerToken("C", "Computation ID", "", TokenOptions.optSwitch, -1);
		cmdLineArgs.addToken(compIdArg);
	}
}
