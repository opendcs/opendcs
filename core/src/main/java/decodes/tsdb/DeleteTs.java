/*
* $Id$
*
* $Log$
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.7  2012/10/12 19:50:41  mmaloney
* Added -T argument to completely delete a time series from the DB
* including all its data and meta data.
*
* Revision 1.6  2012/08/29 14:20:22  mmaloney
* refactor silent mode.
*
* Revision 1.5  2012/08/29 13:59:06  mmaloney
* code cleanup.
*
* Revision 1.4  2012/08/09 15:42:13  mmaloney
* Use makeTimeSeries in TSDB.
*
* Revision 1.3  2012/07/25 19:47:30  mmaloney
* debug DeleteTs
*
* Revision 1.2  2012/07/25 18:09:10  mmaloney
* Bug fix for HDB.
*
* Revision 1.1  2010/12/28 17:56:12  mmaloney
* Created
*
* 
*/
package decodes.tsdb;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import opendcs.dai.TimeSeriesDAI;

import ilex.cmdline.*;
import ilex.util.Logger;

import decodes.util.CmdLineArgs;

/**
Delete Time Series Data.

Main Command Line Args include:
	-S Since-Time
	-U Until-Time
	-Z Time Zone (defaults to tz specified for database)
	-D (from base class) any number of properties to set. See docs on the
	   output formatter you select for which properties are appropriate.
	-L [id|name] specifies how time-series are specified (either ID or NAME)

Following the options are any number of data IDs.
*/
public class DeleteTs
	extends TsdbAppTemplate
{
	private StringToken sinceArg = null;
	private StringToken untilArg = null;
	private StringToken tzArg = null;
	private static TimeZone tz = null;
	private static SimpleDateFormat timeSdf = null;
	private static SimpleDateFormat dateSdf = null;
	private final static long MS_PER_DAY = 3600 * 24 * 1000L;
	private StringToken lookupTypeArg;
	private StringToken outArg;
	private BooleanToken deleteTsidArg;


	public DeleteTs()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		sinceArg = new StringToken("S", "Since Time dd-MMM-yyyy/HH:mm", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(sinceArg);
		untilArg = new StringToken("U", "Until Time dd-MMM-yyyy/HH:mm", "", 
			TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(untilArg);
		tzArg = new StringToken("Z", "Time Zone", "", 
			TokenOptions.optSwitch, "UTC");
		cmdLineArgs.addToken(tzArg);
		lookupTypeArg = new StringToken("L", "Lookup Type", "", TokenOptions.optSwitch, "id");
		cmdLineArgs.addToken(lookupTypeArg);
		deleteTsidArg = new BooleanToken("T", "Delete all data and TSID", "", 
			TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(deleteTsidArg);

		outArg = new StringToken("", "time-series-IDs", "", 
			TokenOptions.optArgument|TokenOptions.optRequired
			|TokenOptions.optMultiple, "");
		cmdLineArgs.addToken(outArg);
	}

	protected void runApp()
		throws Exception
	{
		setTimeZone(TimeZone.getTimeZone(tzArg.getValue()));

		String s = sinceArg.getValue().trim();
		Date since = s != null && s.length() > 0 ? convert2Date(s, false) : null;
//System.out.println("since string='" + s + "', tz='" + tz.getID() + "', converted since=" + since);
		
		s = untilArg.getValue().trim();
		Date until = s != null && s.length() > 0 ? convert2Date(s, true) : null;

		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		for(int n = outArg.NumberOfValues(), i=0; i<n; i++)
		{
			String outTS = outArg.getValue(i);
			try
			{
				CTimeSeries ts = theDb.makeTimeSeries(outTS);
				if (since != null && until != null)
					timeSeriesDAO.deleteTimeSeriesRange(ts, since, until);
				else if (deleteTsidArg.getValue())
					timeSeriesDAO.deleteTimeSeries(ts.getTimeSeriesIdentifier());
			}
			catch(NoSuchObjectException ex)
			{
				Logger.instance().warning("No such time series for '" + outTS + "': " + ex);
			}
		}
		timeSeriesDAO.close();
	}

	public static void setTimeZone(TimeZone _tz)
	{
		tz = _tz;
	}

	public void finalize()
	{
		shutdown();
	}

	public void shutdown()
	{
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
		DeleteTs tp = new DeleteTs();
		tp.execute(args);
	}
}
