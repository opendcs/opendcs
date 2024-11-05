/*
* $Id$
*/
package decodes.decoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;
import decodes.db.*;

/**
This class generates the USGS summary report used by the decoding wizard
and optionally in a routing spec.
*/
public class SummaryReportGenerator
{
	private String newline;
	private StringBuilder sb;
	private String pageDelim;
	private NumberFormat numberFormat;
	private SimpleDateFormat dateFormat = 
		new SimpleDateFormat("yyyy/MM/dd HH:mm");
	private String timeZoneString = "UTC";

	private int maxGapSize = 3;

	/**
	 * Constructor.
	 */
	public SummaryReportGenerator()
	{
		newline = System.getProperty("os.name").toLowerCase().startsWith("win")
			? "\r\n" : "\n";
		pageDelim = "\f"; // form feed.
		sb = new StringBuilder();
		numberFormat = NumberFormat.getInstance();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);
		numberFormat.setGroupingUsed(false);
		dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneString));
	}

	/**
	 * Sets the page delimiter. 
	 * This is printed at the beginnning of each report.
	 * Default is a single form-feed character.
	 * @param pageDelim the page delimiter.
	 */
	public void setPageDelim(String pageDelim)
	{
		this.pageDelim = pageDelim;
	}

	/**
	 * Sets the timezone used for date/time values in the summary.
	 * @param tzs the time zone string.
	 */
	public void setTimeZone(String tzs)
	{
		timeZoneString = tzs;
		dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneString));
	}

	/**
	 * Generate a report and return it as a large String object.
	 * @throws DecoderException if required data is missing from the message.
	 */
	public synchronized String makeReport(DecodedMessage decmsg, String source)
		throws DecoderException
	{
		Platform plat = decmsg.getPlatform();
		if (plat == null)
			throw new DecoderException(
				"Cannot generate summary without platform record.");
		Site platSite = plat.getSite();

		sb.setLength(0);

		// Generate report for the platform-site.
		genForSensorsAt(decmsg, platSite, source, platSite);

		// Generate separate page for each sensor at a different site.
		ArrayList<Site> extraSites = new ArrayList<Site>();
		for (Iterator tsit = decmsg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)tsit.next();
			Site tsSite = ts.getSensor().getSensorSite();
			if (tsSite != null && tsSite != platSite 
			 && !extraSites.contains(tsSite))
				extraSites.add(tsSite);
		}
		for(Site es : extraSites)
			genForSensorsAt(decmsg, es, source, platSite);
		sb.append(newline);
		sb.append(newline);
		return sb.toString();
	}

	private void genForSensorsAt(DecodedMessage decmsg, Site site, 
		String source, Site platSite)
	{
		SiteName siteName = site.getName(Constants.snt_USGS);
		if (siteName == null)
			siteName = site.getPreferredName();

		sb.append(pageDelim);
		sb.append("Summary of Data Converted from <" + source
			+ "> for station <" + siteName.getDisplayName() + ">" 
			+ newline + newline);

		// Generate separate page for each sensor at a different site.
		for (Iterator tsit = decmsg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)tsit.next();
			Site tsSite = ts.getSensor().getSensorSite();
			if (ts.size() > 0 && (
				tsSite == site || (tsSite == null && site == platSite)))
				genForSensor(ts, source, siteName);
		}
	}

	private void genForSensor(TimeSeries ts, String source, SiteName siteName)
	{
		dividerLine();
		Sensor sensor = ts.getSensor();
		String dbno = sensor.getDBNO();
		if (dbno != null)
			sb.append(" Database Number: " + dbno + newline);
		sb.append(    " Station Number : " + siteName.getNameValue() + newline);
		int usgsDdno = sensor.getUsgsDdno();
		if (usgsDdno > 0)
			sb.append(" DD Number      : " + usgsDdno + newline);
		sb.append(    " Sensor         : " + sensor.getName());
		String backup = sensor.getProperty("backup");
		if ( backup != null ) {
			if ( backup.equalsIgnoreCase("true") )
				sb.append("(backup)");
		}
		sb.append(newline);

		DataType dt = sensor.getDataType(Constants.datatype_EPA);
		if (dt == null)
			dt = sensor.getDataType(Constants.datatype_USGS);
		if (dt == null)
			dt = sensor.getDataType();
		sb.append(    " Parameter Code : " + dt.getCode() + newline);
		String statCode = sensor.getUsgsStatCode();
		if (statCode != null)
			sb.append(" Statistic Code : " + statCode + newline);
		dividerLine();

		sb.append(
"|  Data File  |   Begin  Time   |    End  Time    |        Number of Data Values       |  Largest  |          |          |");
		sb.append(newline);
		String tzs = TextUtil.strcenter("("+timeZoneString+")", 15);
		sb.append(
"| Begin   End | " +     tzs + " | " +     tzs + " | Total           In          Out of |   Gap     |  Minimum |  Maximum |");
		sb.append(newline);
		sb.append(
"|  Line  Line |YYYY/MM/DD HH:MM |YYYY/MM/DD HH:MM | Expect Actual  Error Missed Limits | DDD/HH:MM |   Value  |   Value  |");
		sb.append(newline);
		dividerLine();

		// Copy the variables locally & sort them ascending by time-stamp.
		ArrayList<TimedVariable> vars = new ArrayList<TimedVariable>();
		int nsamples = ts.size();
		for(int i=0; i<nsamples; i++)
		{
			TimedVariable tv = ts.sampleAt(i);
			if ((tv.getFlags() & IFlags.IS_MISSING) == 0)
				vars.add(ts.sampleAt(i));
		}
		Collections.sort(vars, 
			new Comparator<TimedVariable>()
			{
				public int compare(TimedVariable tv1, TimedVariable tv2)
				{
					return tv1.getTime().compareTo(tv2.getTime());
				}
				public boolean equals(TimedVariable tv) { return false; }
			});

		long interval = sensor.getRecordingInterval() * 1000L;
		int idx = 0;
		while(idx < vars.size())
		{
			double minValue = Double.MAX_VALUE;
			double maxValue = Double.NEGATIVE_INFINITY;
			long gapSize = 0L;
			long lastMsec = -1L;
			long largestGap = 0L;
			int startLineNum = 0;
			int endLineNum = 0;
			Date startTime = null;
			Date summaryEndTime = null;
			Date endTime = null;
			int numActual = 0;
			int numMissed = 0;
			int numErrors = 0;
			int numOutLim = 0;
			long gapStart = 0L;
			long gapEnd = 0L;
			TimedVariable tv = vars.get(idx);
			summaryEndTime=tv.getTime();
			while(idx < vars.size())
			{
				tv = vars.get(idx);

				// Treat 'missing' samples as if they weren't even here.
				if ((tv.getFlags() & IFlags.IS_MISSING) != 0)
					continue;

				gapStart = 0L;

				// Keep track of start & end line num for this segment.
				endLineNum = tv.getLineNumber();
				endTime = tv.getTime();
				if (startTime == null)
				{
					startLineNum = endLineNum;
					startTime = endTime;
				}

				// Keep track of min/max for this segment.
				if ((tv.getFlags() & IFlags.LIMIT_VIOLATION) != 0)
					numOutLim++;
				else if ((tv.getFlags() & IFlags.IS_ERROR) != 0)
					numErrors++;
				else
				{
					try
					{
						Double d = tv.getDoubleValue();
						if (d < minValue) minValue = d;
						if (d > maxValue) maxValue = d;
					}
					catch(NoConversionException ex)
					{
						Logger.instance().warning("Skipped non numeric sample.");
					}
				}

				// Keep track of largest gap for this segment.
				long msec = tv.getTime().getTime();
				gapSize = (lastMsec == -1 ? 0 : (msec-lastMsec));

				// Excessive gap? (i.e. more than maxGapSize intervals)
				if (interval > 0 && gapSize > interval + 5000L)
				{
					int n = (int)(gapSize / interval) - 1;
					if (n > maxGapSize)
					{
						gapStart = lastMsec + interval;
						gapEnd = msec;
						break;
					}
					numMissed += n;
				}
				
				if (gapSize > largestGap)
					largestGap = gapSize;
				lastMsec = msec;

				idx++;
				numActual++;
				summaryEndTime = tv.getTime();
			}

			int expected = interval == 0 ? -1 : 
				(int)((summaryEndTime.getTime() - startTime.getTime()) / interval + 1);
			String valueString;
			if (minValue == Double.MAX_VALUE)
			{
				valueString = TextUtil.setLengthRightJustify("--",9);
			} else {
				valueString = TextUtil.setLengthRightJustify(numberFormat.format(minValue),9);
			}
			valueString = valueString  + "  ";
			if (maxValue == Double.NEGATIVE_INFINITY)
			{
				valueString = valueString + TextUtil.setLengthRightJustify("--",9);
			} else {
				valueString = valueString + TextUtil.setLengthRightJustify(numberFormat.format(maxValue),9);
			}
			printSampleLine(startLineNum, endLineNum, startTime, summaryEndTime,
				expected, numActual, numErrors, numMissed, numOutLim, largestGap, valueString);

			if (gapStart > 0L)
			{
				int missed = (int)(gapSize / interval) - 1;
				printSampleLine(endLineNum, endLineNum, 
					new Date(gapStart), new Date(gapEnd),
					missed, 0, 0, missed, 0, gapSize, "(** LARGE DATA GAP **)");
			}
		}
	}

	private void dividerLine()
	{
		sb.append(
"==========================================================================================================================" 
			+ newline);
	}

	private void printSampleLine(int startLineNum, int endLineNum, 
		Date startTime, Date endTime, 
		int expected, int numActual, int numErrors,
		int numMissed, int numOutLim,
		long largestGap, String valueString)
	{
		sb.append(
			"| " + TextUtil.setLengthRightJustify("" + startLineNum, 5)
			+ " " + TextUtil.setLengthRightJustify("" + endLineNum, 5)
			+ " |");
		sb.append(dateFormat.format(startTime) + " |");
		sb.append(dateFormat.format(endTime) + " |");
		if (expected == -1)
			sb.append("  N/A  ");
		else
			sb.append(" " + TextUtil.setLengthRightJustify("" + expected, 5)
				+ "  ");

		sb.append(TextUtil.setLengthRightJustify("" + numActual, 5)+"  ");
		sb.append(TextUtil.setLengthRightJustify("" + numErrors, 5)+ "  ");
		sb.append(TextUtil.setLengthRightJustify("" + numMissed, 5)+"  ");
		sb.append(TextUtil.setLengthRightJustify("" + numOutLim, 5)+"  |");

		int lgs = (int)(largestGap/1000L);
		int days = (lgs / (24*3600));
		lgs -= (days * (24*3600));
		sb.append(TextUtil.setLengthRightJustify("" + days, 3) + "/");
		int hours = lgs / 3600;
		sb.append(hours < 10 ? "0" : "");
		sb.append("" + hours + ":");
		lgs -= (hours * 3600);
		int minutes = lgs / 60;
		sb.append(minutes <10 ? "0" : "");
		sb.append("" + minutes + "  |");

		sb.append(valueString);
		sb.append(newline);
	}

	public void setMaxGapSize(int gs)
	{
		maxGapSize = gs;
	}
}
