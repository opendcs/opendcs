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
package decodes.util;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import ilex.cmdline.*;
import ilex.util.TextUtil;
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
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsdbAppTemplate;

public class ExportTimeSeries extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private StringToken sinceArg = new StringToken("S", "Since Time dd-MMM-yyyy/HH:mm", "", TokenOptions.optSwitch, "");
	private StringToken untilArg = new StringToken("U", "Until Time dd-MMM-yyyy/HH:mm", "", TokenOptions.optSwitch, "");
	private StringToken fmtArg = new StringToken("F", "OutputFormat", "", TokenOptions.optSwitch, "Human-Readable");
	private StringToken presArg = new StringToken("G", "PresentationGroup", "", TokenOptions.optSwitch, "");
	private StringToken timezoneArg = new StringToken("Z", "Time Zone", "", TokenOptions.optSwitch, "UTC");
	private StringToken transportIdArg = new StringToken("I", "TransportID", "", TokenOptions.optSwitch, "");
	private StringToken lookupTypeArg = new StringToken("L", "Lookup Type", "", TokenOptions.optSwitch, "id");
	private StringToken tsidArg = new StringToken("", "time-series-IDs | all | group:groupname", "",
		TokenOptions.optArgument|TokenOptions.optRequired |TokenOptions.optMultiple, "");

	private static TimeZone tz = null;
	private static final String GROUP_LABEL = "group:";

	private OutputFormatter outputFormatter = null;
	private static SimpleDateFormat timeSdf = null;
	private static SimpleDateFormat dateSdf = null;
	private PresentationGroup presGroup = null;
	private Properties props = new Properties();
	private DataConsumer consumer = null;
	private final static long MS_PER_DAY = 3600 * 24 * 1000L;
	private PrintStream outputStream = null;
	private boolean forceTsIdCacheReload;


	public ExportTimeSeries()
	{
		this(false);
	}

	public ExportTimeSeries(boolean forceTsIdCacheReload)
	{
		super("util.log");
		setSilent(true);
		this.forceTsIdCacheReload = forceTsIdCacheReload;
	}

	public static void main(String args[])
		throws Exception
	{
		new ExportTimeSeries().execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(sinceArg);
		cmdLineArgs.addToken(untilArg);
		cmdLineArgs.addToken(fmtArg);
		cmdLineArgs.addToken(timezoneArg);
		cmdLineArgs.addToken(lookupTypeArg);
		cmdLineArgs.addToken(presArg);
		cmdLineArgs.addToken(transportIdArg);
		cmdLineArgs.addToken(tsidArg);
	}

	public void setOutputStream(PrintStream ps)
	{
		outputStream = ps;
	}

	@Override
	protected void runApp()
		throws OutputFormatterException, DataConsumerException,
		DbIoException, BadTimeSeriesException, NoSuchObjectException,
		UnknownPlatformException, IOException
	{
		tz = TimeZone.getTimeZone(timezoneArg.getValue());

		String pgName = presArg.getValue();
		if (pgName != null && pgName.length() > 0)
			setPresentationGroup(pgName);

		this.props = cmdLineArgs.getCmdLineProps();

		consumer = new PipeConsumer();
		consumer.open("", props);
		if (outputStream != null)
			((PipeConsumer)consumer).setOutputStream(outputStream);

		outputFormatter = OutputFormatter.makeOutputFormatter(
			fmtArg.getValue(), tz, presGroup, props, null);

		String s = sinceArg.getValue().trim();
		Date since = null, until = null;
		if (s.equalsIgnoreCase("all"))
			since = new Date(0L);
		else
			since = convert2Date(s, false);

		s = untilArg.getValue().trim();
		until = convert2Date(s, true);

		ArrayList<CTimeSeries> ctss = new ArrayList<CTimeSeries>();
		try (TimeSeriesDAI tsDAO = theDb.makeTimeSeriesDAO())
		{
			for(int n = tsidArg.NumberOfValues(), i=0; i<n; i++)
			{
				String outTS = tsidArg.getValue(i);
				if (outTS.equalsIgnoreCase("all"))
				{
					ArrayList<TimeSeriesIdentifier> tsids = tsDAO.listTimeSeries();
					for(TimeSeriesIdentifier tsid : tsids)
					{
						CTimeSeries ts = theDb.makeTimeSeries(tsid);
						int nvalues = tsDAO.fillTimeSeries(ts, since, until);
						log.info("Read {} values for time series '{}'", nvalues, tsid.getUniqueString());
						ctss.add(ts);
					}
					break; // No need to continue, we have all time series now.
				}
				else if (TextUtil.startsWithIgnoreCase(outTS, GROUP_LABEL))
				{
					String groupName = outTS.substring(GROUP_LABEL.length());
					TsGroupDAI groupDAO = theDb.makeTsGroupDAO();
					TsGroup grp = groupDAO.getTsGroupByName(groupName);
					groupDAO.close();
					if (grp == null)
					{
						log.warn("No such time series group: {} -- skipped.", groupName);
						continue;
					}
					ArrayList<TimeSeriesIdentifier> tsids = theDb.expandTsGroup(grp);
					for(TimeSeriesIdentifier tsid : tsids)
					{
						CTimeSeries ts = theDb.makeTimeSeries(tsid);
						int nvalues = tsDAO.fillTimeSeries(ts, since, until);
						log.info("Read {} values for time series '{}'", nvalues, tsid.getUniqueString());
						ctss.add(ts);
					}
				}
				else // Should be a time series ID
				{
					try
					{
						CTimeSeries ts = theDb.makeTimeSeries(outTS);
						int nvalues = tsDAO.fillTimeSeries(ts, since, until);
						log.info("Read {} values for time series '{}'", nvalues, outTS);
						ctss.add(ts);
					}
					catch(NoSuchObjectException ex)
					{
						log.atWarn().setCause(ex).log("No time series for '{}'", outTS);
					}
				}
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
			try(SiteDAI siteDAO = theDb.makeSiteDAO())
			{
				if (ttype.equalsIgnoreCase("site"))
				{
					DbKey siteId = siteDAO.lookupSiteID(tidArg);
					Site site = siteDAO.getSiteById(siteId);
					p = Database.getDb().platformList.findPlatform(site, null);
				}
				else
				{
					p = Database.getDb().platformList.findPlatform(ttype, tidArg, now);
				}
			}
		}

		byte[] dummyData = new byte[0];
		RawMessage rawMsg = new RawMessage(dummyData);
		rawMsg.setPlatform(p);
		rawMsg.setTransportMedium(new TransportMedium(p, ttype, tidArg));
		rawMsg.setTimeStamp(now);
		rawMsg.setHeaderLength(0);
		rawMsg.setPM(GoesPMParser.MESSAGE_TIME, new Variable(now));
		rawMsg.setPM(GoesPMParser.MESSAGE_LENGTH, new Variable(0L));

		outputTimeSeries(rawMsg, ctss);

		outputFormatter.shutdown();
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
			log.atWarn().setCause(ex).log("Cannot initialize presentation group '{}'", groupName);
			presGroup = null;
		}
	}

	private void outputTimeSeries(RawMessage rawMsg, Collection<CTimeSeries> ctss)
		throws OutputFormatterException, IOException,
		DataConsumerException, UnknownPlatformException
	{
		DecodedMessage decmsg = new DecodedMessage(rawMsg);
		for(CTimeSeries cts : ctss)
		{
			TimeSeries ts = TSUtil.convert2DecodesTimeSeries(cts);
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
						log.trace("Omitting sensor '{}' as per Presentation Group.", sensor.getName());
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

}
