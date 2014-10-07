/*
 * $Id$
 * 
 * $Log$
 * 
 * Copyright 2007 Ilex Engineering, Inc. - All Rights Reserved.
 * No part of this file may be duplicated in either hard-copy or electronic
 * form without specific written permission.
*/
package decodes.tsdb;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import ilex.cmdline.*;
import ilex.util.Logger;
import ilex.var.Variable;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.PipeConsumer;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.datasource.GoesPMParser;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.DataPresentation;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.sql.DbKey;
import decodes.util.CmdLineArgs;

/**
Outputs a time series in a DECODES format.
Can be used as part of another app to write data to a stream, or stand-alone
command-line.

Main Command Line Args include:
	-F DECODES Format Name (from the enumeration)
	-S Since-Time
	-U Until-Time
	-Z Time Zone (defaults to tz specified for database)
	-G Presentation Group (default=none)
	-D (from base class) any number of properties to set. See docs on the
	   output formatter you select for which properties are appropriate.
	-I transportID (many formatters need a platform association. Provide
	   the transport ID of the platform that owns the output data. The ID
	   should be of the form [mediumType:]mediumId. If [mediumType:] is
	   omitted, then goes is assumed.
	-L [id|name] specifies how time-series are specified (either ID or NAME)

Following the options are any number of data IDs.
*/
public class OutputTs
	extends TsdbAppTemplate
{
	private StringToken formatterArg = null;
	private StringToken sinceArg = null;
	private StringToken untilArg = null;
	private StringToken outArg = null;
	private StringToken presentationArg = null;
	private StringToken tzArg = null;
	private StringToken transportIdArg = null;
	private OutputFormatter outputFormatter = null;
	private static TimeZone tz = null;
	private PresentationGroup presGroup = null;
	private Properties props = new Properties();
	private DataConsumer consumer = null;
	private static SimpleDateFormat timeSdf = null;
	private static SimpleDateFormat dateSdf = null;
	private final static long MS_PER_DAY = 3600 * 24 * 1000L;
	private StringToken lookupTypeArg;


	public OutputTs()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		formatterArg = new StringToken("F", "OutputFormat", "", 
			TokenOptions.optSwitch, "Human-Readable");
		cmdLineArgs.addToken(formatterArg);
		sinceArg = new StringToken("S", "Since Time dd-MMM-yyyy/HH:mm", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(sinceArg);
		untilArg = new StringToken("U", "Until Time dd-MMM-yyyy/HH:mm", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(untilArg);
		tzArg = new StringToken("Z", "Time Zone", "", 
			TokenOptions.optSwitch, "UTC");
		cmdLineArgs.addToken(tzArg);
		presentationArg = new StringToken("G", "PresentationGroup", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(presentationArg);
		transportIdArg = new StringToken("I", "TransportID", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(transportIdArg);
		lookupTypeArg = new StringToken("L", "Lookup Type", "", TokenOptions.optSwitch, "id");
		cmdLineArgs.addToken(lookupTypeArg);

		outArg = new StringToken("", "time-series-IDs", "", 
			TokenOptions.optArgument|TokenOptions.optRequired
			|TokenOptions.optMultiple, "");
		cmdLineArgs.addToken(outArg);
	}

	protected void runApp()
		throws Exception
	{
		setTimeZone(TimeZone.getTimeZone(tzArg.getValue()));

		String pgName = presentationArg.getValue();
		if (pgName != null && pgName.length() > 0)
			setPresentationGroup(pgName);

		setProperties(cmdLineArgs.getCmdLineProps());

		setOutputFormatter(formatterArg.getValue());
		
		consumer = new PipeConsumer();
		consumer.open("", props);

		String s = sinceArg.getValue().trim();
		Date since = convert2Date(s, false);
		
		s = untilArg.getValue().trim();
		Date until = convert2Date(s, true);

		ArrayList<CTimeSeries> ctss = new ArrayList<CTimeSeries>();
		for(int n = outArg.NumberOfValues(), i=0; i<n; i++)
		{
			String outTS = outArg.getValue(i);
			TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
			try
			{
				CTimeSeries ts = theDb.makeTimeSeries(outTS);
theDb.debug3("outputts, after makeTimeSeries dn='" + ts.getDisplayName() + "'");
				timeSeriesDAO.fillTimeSeries(ts, since, until);
theDb.debug3("outputts, after fillTimeSeries dn='" + ts.getDisplayName() + "'");
				ctss.add(ts);
			}
			catch(NoSuchObjectException ex)
			{
				Logger.instance().warning("No time series for '" + outTS + "': " + ex);
			}
			finally
			{
				timeSeriesDAO.close();
			}
		}

		String tidArg = transportIdArg.getValue();
		String ttype = "site";
		if (tidArg != null && tidArg.length() > 0)
		{
			int cidx = tidArg.indexOf(':');
			if (cidx > 0)
				ttype = tidArg.substring(0, cidx);
			tidArg = tidArg.substring(cidx+1);
		}
		
		Date now = new Date();
		Platform p = null;
		if (tidArg != null && tidArg.length() > 0)
		{
			SiteDAI siteDAO = theDb.makeSiteDAO();
			if (ttype.equalsIgnoreCase("site"))
			{
				DbKey siteId = siteDAO.lookupSiteID(tidArg);
				Site site = siteDAO.getSiteById(siteId);
				p = Database.getDb().platformList.findPlatform(site, null);
			}
			else
				p = Database.getDb().platformList.findPlatform(ttype, tidArg, now);
		}
//System.out.println("Platform for ttype='" + ttype + "' id='" + tidArg
//+ "' = " + (p == null ? "null" : p.getDisplayName()));

		byte[] dummyData = new byte[0];
		RawMessage rawMsg = new RawMessage(dummyData);
		rawMsg.setPlatform(p);
		rawMsg.setTransportMedium(new TransportMedium(p, ttype, tidArg));
		rawMsg.setTimeStamp(now);
		rawMsg.setHeaderLength(0);
		rawMsg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(now));
		rawMsg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(0L));
		
		outputTimeSeries(rawMsg, ctss);
	}


	/**
	 * Create the formatter for output. You should call setTimeZone,
	 * setPresentationGroup and setProperties before calling this method.
	 */
	public void setOutputFormatter(String formatterName)
		throws OutputFormatterException
	{
		outputFormatter = OutputFormatter.makeOutputFormatter(
			formatterName, tz, presGroup, props);
	}

	public static void setTimeZone(TimeZone _tz)
	{
		tz = _tz;
	}

	public void setPresentationGroup(String groupName)
	{
		presGroup = Database.getDb().presentationGroupList.find(groupName);
		try
		{
			if (presGroup != null)
				presGroup.prepareForExec();
		}
		catch (InvalidDatabaseException ex)
		{
			Logger.instance().warning("Cannot initialize presentation group '" 
				+ groupName + ": " + ex);
			presGroup = null;
		}
	}

	public void setProperties(Properties props)
	{
		this.props = props;
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

	public void setConsumer(DataConsumer consumer)
	{
		this.consumer = consumer;
	}

	public void finalize()
	{
		shutdown();
	}

	public void shutdown()
	{
		if (outputFormatter != null)
		{
			outputFormatter.shutdown();
			outputFormatter = null;
		}
		if (consumer != null)
		{
			consumer.close();
			consumer = null;
		}
	}

	public static Date convert2Date(String s, boolean isTo)
	{
		if (timeSdf == null)
		{
			timeSdf = new SimpleDateFormat("dd-MMM-yyyy/HH:mm");
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
	
	public static void main(String args[])
		throws Exception
	{
		OutputTs tp = new OutputTs();
		tp.execute(args);
	}
}
