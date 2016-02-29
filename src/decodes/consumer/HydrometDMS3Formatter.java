/**
 * $Id$
 * 
 * $Log$
 * Revision 1.3  2014/05/30 13:15:32  mmaloney
 * dev
 *
 * Revision 1.2  2014/05/28 13:09:27  mmaloney
 * dev
 *
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
import ilex.var.Variable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import lrgs.archive.MsgValidator;
import lrgs.archive.XmitWindow;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import decodes.datasource.GoesPMParser;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSettings;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

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
	public final static String OldValueConst = "998877.00";
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
		consumer.startMessage(msg);

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
			consumer.println(
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
//					Logger.instance().warning(module + " unexpected " + ex);
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
				consumer.println(formatValue(tv.getTime(), siteName.getNameValue(), dataType.getCode(),
					valueFormat.format(d), dms3Flag));
			}
		}
		
		// Now output the performance measurements if they are available.
		DcpMsg dcpMsg = msg.getRawMessage().getOrigDcpMsg();
		if (dcpMsg != null)
		{
			String sn = platformSiteName.getNameValue();
			int np = 0;
			byte []msgData = msg.getRawMessage().getMessageData();
			for(int idx = 0; idx < msgData.length; idx++)
				if (((char)msgData[idx]) == '$')
					np++;
			consumer.println(formatValue(msg.getMessageTime(), sn, "PARITY",
				valueFormat.format((double)np), FLAG_GOOD));
			
			Variable v = msg.getRawMessage().getPM(GoesPMParser.SIGNAL_STRENGTH);
			if (v != null)
			{
				try
				{
					double d = v.getDoubleValue();
					consumer.println(formatValue(msg.getMessageTime(), sn, "POWER",
						valueFormat.format(d), FLAG_GOOD));
				}
				catch (NoConversionException e)
				{
					Logger.instance().warning("Site " + sn + " has signal strength '" + v.toString() 
						+ "' that cannot be expressed as a number.");
				}
			}
			
			consumer.println(formatValue(msg.getMessageTime(), sn, "MSGLEN",
				valueFormat.format((double)msgData.length), FLAG_GOOD));
			
			String s = msg.getPlatform().getProperty("expectLength");
			if (s != null)
			{
				try
				{
					int el = Integer.parseInt(s.trim());
					consumer.println(formatValue(msg.getMessageTime(), sn, "LENERR",
						valueFormat.format((double)(el - msgData.length)), FLAG_GOOD));
				}
				catch(NumberFormatException ex)
				{
					Logger.instance().warning("Site " + sn + " has expectLength property '" + s 
						+ "' that cannot be expressed as an integer.");

				}
			}
			
			if (!Pdt.instance().isLoaded())
			{
				Logger.instance().info(module + " loading PDT.");
				Pdt.instance().startMaintenanceThread(DecodesSettings.instance().pdtUrl, 
					DecodesSettings.instance().pdtLocalFile);
				// Give it up to two minutes to load.
				long start = System.currentTimeMillis();
				while(System.currentTimeMillis() - start < 120000L && !Pdt.instance().isLoaded())
					try { Thread.sleep(1000L); } catch(InterruptedException ex){}
			}

			DcpAddress dcpAddress = new DcpAddress(msg.getRawMessage().getMediumId());
			PdtEntry pdtEntry = Pdt.instance().find(dcpAddress);
			if (pdtEntry != null)
			{
				if (dcpMsg.getGoesChannel() != pdtEntry.st_channel)
					Logger.instance().warning("Site " + sn + " DCP " + dcpAddress + " not on correct channel"
						+ " for self timed message. This channel=" + dcpMsg.getGoesChannel()
						+ ", ST Channel = " + pdtEntry.st_channel);
				else // we have a ST message on the correct channel.
				{
					// Compute expected start & end time for this message.
					Date xmitTime = dcpMsg.getXmitTime();
					Date cstart = dcpMsg.getCarrierStart();
					if (cstart != null)
						xmitTime = cstart;
					long xmitmsec = xmitTime.getTime();
					long xmit_timet = xmitmsec/1000L;
					long base_timet = (xmit_timet/MsgValidator.SEC_PER_DAY - 1) * MsgValidator.SEC_PER_DAY;
					int xi = pdtEntry.st_xmit_interval;
					long windows_since_base =
						((xmit_timet - base_timet - pdtEntry.st_first_xmit_sod) + xi/2)
						/ xi;
					long expected_start_tt = base_timet + pdtEntry.st_first_xmit_sod
						+ windows_since_base * xi;
					
					long terr = xmitmsec - (expected_start_tt * 1000L);
					consumer.println(formatValue(msg.getMessageTime(), sn, "TIMEERR",
						valueFormat.format((double)terr/1000.), FLAG_GOOD));
				}
			}
			else
				Logger.instance().warning("Site " + sn + " has no PDT entry for dcp address " + dcpAddress);
				
		}
		
	}
	
	private String formatValue(Date t, String siteName, String dtCode, String v, String flag)
	{
		StringBuilder line = new StringBuilder();
		
		line.append(dateFormat.format(t).toUpperCase());
		line.append(' ');
		line.append(TextUtil.setLengthLeftJustify(siteName, 8));
		line.append(' ');
		line.append(TextUtil.setLengthLeftJustify(dtCode, 9));
		line.append(' ');
		line.append(TextUtil.setLengthLeftJustify(v, 10));
		line.append(' ');
		line.append(TextUtil.setLengthLeftJustify(OldValueConst, 10));
		line.append(' ');
		line.append(flag);

		return line.toString();
	}
}
