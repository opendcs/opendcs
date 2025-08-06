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
package decodes.consumer;

import ilex.util.PropertiesUtil;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Properties;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.PropertySpec;

/**
  This class formats decoded data into the intermediate SHEFIT format used by
  the USACE HEC.
*/
public class ShefitFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private SimpleDateFormat dateFormat;
	private NumberFormat numberFormat;
	private String siteNameType = null;

	/** default constructor */
	public ShefitFormatter()
	{
		super();
		dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setGroupingUsed(false);
		numberFormat.setMaximumFractionDigits(3);
		numberFormat.setMinimumFractionDigits(3);
		numberFormat.setMaximumIntegerDigits(6);
		numberFormat.setMinimumIntegerDigits(6);
	}

	private PropertySpec propSpecs[] =
	{
		new PropertySpec("sitenametype",
			PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Preferred site name type (default is hex DCP Address)")

	};

	/**
	  Initializes the Formatter. This method is called from the static
	  makeOutputFormatter method in this class. The RoutingSpec does not
	  need to call it explicitly.
	  @param type the type of this output formatter.
	  @param tz the time zone as specified in the routing spec.
	  @param presGrp The presentation group to handle rounding &amp; EU conversions.
	  @param rsProps the routing-spec properties.
	*/
	@Override
	protected void initFormatter(String type, java.util.TimeZone tz,
		PresentationGroup presGrp, Properties rsProps)
		throws OutputFormatterException
	{
		Calendar cal = Calendar.getInstance(tz);
		dateFormat.setCalendar(cal);
		String s = PropertiesUtil.getIgnoreCase(rsProps, "sitenametype");
		if (s != null && s.trim().length() > 0)
			siteNameType = s.trim();
	}

	/** Does nothing. */
	@Override
	public void shutdown()
	{
	}

	/**
	  Writes the passed DecodedMessage to the passed consumer, using
	  a concrete format.
	  @param msg The message to output.
	  @param consumer The DataConsumer to output to.
	  @throws OutputFormatterException if there was a problem formatting data.
	  @throws DataConsumerException, passed through from consumer methods.
	*/
	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);

		StringBuffer sb = new StringBuffer();
		RawMessage rawmsg = msg.getRawMessage();

		TransportMedium tm;
		Platform platform;

		char platformType = 'I';
		String dcpId = "unknown";
		Site platformSite = null;
		try
		{
			tm = rawmsg.getTransportMedium();
			if (tm.getMediumType().equalsIgnoreCase(Constants.medium_GoesRD))
				platformType = 'R';
			dcpId = tm.getMediumId();
			platform = rawmsg.getPlatform();
			platformSite = platform.getSite();
			if (siteNameType != null && platformSite != null)
			{
				SiteName sn = platformSite.getName(siteNameType);
				if (sn == null)
					sn = platformSite.getPreferredName();
				if (sn != null)
				{
					dcpId = sn.getNameValue().trim();
					if (dcpId.length() > 8)
						dcpId = dcpId.substring(0, 8);
				}
			}
		}
		catch(UnknownPlatformException ex)
		{
			log.atError().setCause(ex).log("Unknown platform in format Message");
		}



		for(Iterator<TimeSeries> it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = it.next();
			Sensor sensor = ts.getSensor();
			String lineStationId = dcpId; // Default is to use the ID from Platform

			if (siteNameType != null)
			{
				Site ss = sensor.getSensorSite();
				if (ss != null && ss != platformSite)
				{
					SiteName sn = ss.getName(siteNameType);
					if (sn == null)
						sn = ss.getPreferredName();
					if (sn != null)
					{
						lineStationId = sn.getNameValue().trim();
						if (lineStationId.length() > 8)
							lineStationId = lineStationId.substring(0, 8);
					}
				}
			}

			DataType dt = sensor.getDataType(Constants.datatype_SHEF);
			String shefCode = dt != null ? dt.getCode() : "XX";


			int sz = ts.size();
			for(int i=0; i<sz; i++)
			{
				TimedVariable tv = ts.sampleAt(i);
				if ((tv.getFlags() & (IFlags.IS_MISSING | IFlags.IS_ERROR)) != 0)
					continue;

				double value;
				try { value = tv.getDoubleValue(); }
				catch(NoConversionException ex)
				{
					log.atError()
					   .setCause(ex)
					   .log("Bad sample '{}' in sensor '{}': should be a number.", tv.toString(), shefCode);
					continue;
				}

				sb.setLength(0);
				sb.append(lineStationId);
				for(int j=sb.length(); j<8; j++)
					sb.append(' ');

				// creation date in YYYYMMDDhhmmss
				sb.append(dateFormat.format(tv.getTime()));

				// Hard coded fields:
				sb.append("    0 0 0 0 0 0");

				sb.append(' ');

				// Sensor Code length check
				// Source and Type code depends on the length
				if(shefCode.length() > 2) {
					sb.append(shefCode.substring(0, 2));
					sb.append(' ');
					int end = shefCode.length() > 5 ? 5 : shefCode.length();
					sb.append(shefCode.substring(3, end));
					sb.append("Z");
				} else {
					sb.append(shefCode);
					sb.append(' ');
					sb.append("RZZ");
				}
				String tstr = numberFormat.format(value);
				boolean negative = tstr.charAt(0) == '-';
				if (negative)
					tstr = tstr.substring(1);

				StringBuffer vsb=new StringBuffer(tstr);

				for(int j = 0; j<vsb.length()-1; j++)
				{
					char c = vsb.charAt(j);
					if (c == '0')
						vsb.setCharAt(j, ' ');
					else
					{
						if (negative && j>0)
							vsb.setCharAt(j-1, '-');
						break;
					}
				}

				sb.append(vsb.toString());

				sb.append(" Z -1.00 ");

				char mode = sensor.getRecordingMode();
				int dur = sensor.getRecordingInterval();

				if (mode == Constants.recordingModeUndefined
				 || dur <= 0)
					sb.append("5005");
				else if (mode == Constants.recordingModeVariable)
					sb.append("0000");  // Means instantaneous?
				else
				{
					if (dur % (24*60*60) == 0)
					{
						dur = 2000 + (dur / (24*60*60));
						sb.append("" + dur); // specify in days
					}
					else if (dur % (60*60) == 0)
					{
						dur = 1000 + (dur / (60*60));
						sb.append("" + dur); // specify in hours
					}
					else // specify in minutes;
					{
						int min = (dur+30)/60; // Round to nearest minute
						sb.append("0");
						if (min >= 100)
							sb.append("" + (min % 1000));
						else if (min >= 10)
							sb.append("0" + min);
						else
							sb.append("00" + min);
					}
				}
				sb.append(" 0         ");

				if (i == 0) // first in set?
					sb.append("1");
				else
					sb.append("2");

				consumer.println(sb.toString());
			}
		}
		consumer.endMessage();
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}
