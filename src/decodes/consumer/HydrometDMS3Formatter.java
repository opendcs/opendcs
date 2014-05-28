/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package decodes.consumer;

import ilex.util.Logger;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;


import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSettings;

/**
 * Hydromet DMS3 Formatter for US Bureau of Reclamation Boise, ID.
 * <br>Prints data in the following format:
<pre>
yyyyMMMdd hhmm cbtt     PC        NewValue   OldValue   Flag user:jmaxon
2014JAN07 1400 HGHM     WF        38.69      32.00      -03
2014JAN07 1415 HGHM     WF        38.72      32.00      -03
</pre>
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class HydrometDMS3Formatter extends OutputFormatter
{
	public final String module = "HydrometDMS3Formatter";
	
	private NumberFormat valueFormat = NumberFormat.getNumberInstance();
	private SimpleDateFormat dateFormat = 
		new SimpleDateFormat("yyyyMMMdd HHmm");
	
	public final static String FLAG_GOOD = "-01";
	public final static String FLAG_NO_VALUE = "-02";
	public final static String FLAG_LOW_LIMIT = "-20";
	public final static String FLAG_HIGH_LIMIT = "-22";
	public final static double MISSING_VALUE = 998877.;
	private boolean justOpened = true;
	private TimeZone rsTimeZone = null;
	private Platform lastPlatform = null;
	
	public HydrometDMS3Formatter()
	{
		super();
		// Always print 2 decimal points
		valueFormat.setMaximumFractionDigits(2);
		valueFormat.setMinimumFractionDigits(2);
		valueFormat.setGroupingUsed(false);
	}

	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		// If a TZ is specified this becomes the default if none is specifed for the site.
		if (tz != null)
			rsTimeZone = tz;
		else // no RS timezone specified, default to whatever the system is set to.
			rsTimeZone = TimeZone.getDefault();
		justOpened = true;
	}

	@Override
	public void shutdown()
	{
		// No action needed
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		Site platformSite = msg.getPlatform().getSite();
		SiteName platformSiteName = platformSite.getName("cbtt");
		if (platformSiteName == null)
			platformSiteName = platformSite.getPreferredName();
		
		TimeZone platformTZ = null;
		if (platformSite.timeZoneAbbr != null 
		 && platformSite.timeZoneAbbr.trim().length() > 0)
			platformTZ = TimeZone.getTimeZone(platformSite.timeZoneAbbr.trim());


		if (justOpened)
		{
			consumer.printLine(
				"yyyyMMMdd hhmm cbtt     PC        NewValue   OldValue   Flag user:"
				+ System.getProperty("user.name")
				+ " # DECODES output");
			justOpened = false;
		}
		if (msg.getPlatform() != lastPlatform)
		{
//			consumer.println("# Platform " + platformSiteName.getNameValue()
//				+ (platformTZ == null ? "" : ", Timezone=" + platformTZ.getID()));
			lastPlatform = msg.getPlatform();
		}
		
		StringBuilder line = new StringBuilder();
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext();)
		{
			TimeSeries ts = tsit.next();
			for (int idx = 0; idx < ts.size(); idx++)
			{
				TimedVariable tv = ts.sampleAt(idx);
				
				// Determine the value and flag.
				double d = MISSING_VALUE;
				String dms3Flag = FLAG_GOOD;
				try
				{
					d = tv.getDoubleValue();
				}
				catch(NoConversionException ex)
				{
					Logger.instance().warning(module + " unexpected " + ex);
					dms3Flag = FLAG_NO_VALUE;
				}
				if ((tv.getFlags() & IFlags.LIMIT_VIOLATION) != 0)
				{
					double min = ts.getSensor().getMinimum();
					if (min != Constants.undefinedDouble && d < min)
						dms3Flag = FLAG_LOW_LIMIT;
					double max = ts.getSensor().getMaximum();
					if (max != Constants.undefinedDouble && d > max)
						dms3Flag = FLAG_HIGH_LIMIT;
				}
				else if ((tv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
				{
					dms3Flag = FLAG_NO_VALUE;
					d = MISSING_VALUE;
				}
				
				// Get the site name
				Site site = ts.getSensor().getSensorSite();
				if (site == null)
					// No sensor-specific site. Get the one assigned to the platform.
					site = platformSite;
				SiteName siteName = site.getName("cbtt");
				if (siteName == null)
					siteName = site.getPreferredName();
				
				// get the data type. If CBTT data type is defined, use it. Else use
				// preference in decodes settings. Else use the first one defined in the
				// config sensor record.
				DataType dataType = ts.getSensor().getDataType("cbtt");
				if (dataType == null)
					if ((dataType = ts.getSensor().getDataType(
						DecodesSettings.instance().dataTypeStdPreference)) == null)
						ts.getSensor().getDataType();
				
				line.setLength(0);

				// Set time zone to whatever is defined for the site.
				// If none, fallback to the routing spec time zone.
				TimeZone tz = rsTimeZone;
				if (site.timeZoneAbbr != null && site.timeZoneAbbr.trim().length() > 0)
				{
					tz = TimeZone.getTimeZone(site.timeZoneAbbr.trim());
					if (tz == null)
						tz = rsTimeZone;
				}
				dateFormat.setTimeZone(tz);
				
				line.append(dateFormat.format(tv.getTime()).toUpperCase());
				line.append(' ');
				line.append(TextUtil.setLengthLeftJustify(
					siteName.getNameValue(), 8));
				line.append(' ');
				line.append(TextUtil.setLengthLeftJustify(
					dataType.getCode(), 9));
				line.append(' ');
				line.append(TextUtil.setLengthLeftJustify(valueFormat.format(d), 10));
				line.append(' ');
				line.append(TextUtil.setLengthLeftJustify("998877.00", 10));
				line.append(' ');
				line.append(dms3Flag);
				consumer.printLine(line.toString());
			}
		}
	}
}
