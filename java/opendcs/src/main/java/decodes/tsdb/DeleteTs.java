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

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.TimeSeriesDAI;

import ilex.cmdline.*;

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
public class DeleteTs extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
				log.atWarn().setCause(ex).log("No such time series for '{}'", outTS);
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
				log.warn("Unknown time '{}'", s);
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
				ex2.addSuppressed(ex);
				log.atWarn().setCause(ex).log("Bad time format '{}'", s);
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
