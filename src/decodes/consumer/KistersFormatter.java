/**
 * $Id$
 * 
 * $Log$
 * Revision 1.5  2014/09/15 13:55:32  mmaloney
 * Updates for AESRD
 *
 * Revision 1.4  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.3  2014/05/30 13:15:33  mmaloney
 * dev
 *
 * Revision 1.2  2014/05/28 13:09:29  mmaloney
 * dev
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Copyright 2014 Cove Software, LLC
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.consumer;

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import ilex.var.IFlags;
import ilex.var.TimedVariable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.PropertySpec;

/**
 * This class formats a DCP message for ingest into Kisters' WISKI database
 * by dropping files into a know directory in the Kisters ZRXP format.
 *
 * @author Michael Maloney, Cove Software, LLC
 */
public class KistersFormatter 
	extends OutputFormatter
{
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	
	/** Can be set by "includeTZ" property */
	private boolean includeTZ = true;
	/** Can be set by tzName property. Defaults to abbreviation from Routing Spec TZ */
	private String tzName = null;
	/** Can be set by "includeCNAME" property */
	private boolean includeCNAME = false;
	/** Can be set by "includeCUNIT" property. */
	private boolean includeCUNIT = false;
	/** Can be set by "includeRINVAL" property. */
	private boolean includeRINVAL = true;
	/** Can be set by "RINVAL" property. */
	private String RINVAL = "-777";
	/** Can be set by "SiteNameType" property */
	private String siteNameType = Constants.snt_NWSHB5;
	/** Can be set by "SiteNameTypeAlt" property */
	private String siteNameTypeAlt = Constants.snt_local;
	/** Can be set by "DataTypeStandard" property */
	private String dataTypeStandard = Constants.datatype_SHEF;
	/** Can be set by "DataTypeStandardAlt" property */
	private String dataTypeStandardAlt = null;
	/** Can be set by "SitePrefix" property */
	private String sitePrefix = "";
	/** Can be set by "includeLayout" property */
	private boolean includeLayout = false;
	private int bufferTimeSec = 0;
	private DataConsumer theConsumer = null;
	
	public static final String headerDelim = "|*|";
	
	class SampleValue
	{
		long t;
		String v;
		SampleValue(long t, String v) { this.t = t; this.v = v; }
	};
	class SensorData
	{
		String header;
		DecodedMessage msg;
		ArrayList<SampleValue> samples = new ArrayList<SampleValue>();
		SensorData(String header, DecodedMessage msg)
		{
			this.header = header;
			this.msg = msg;
		}
		void addSample(long t, String v)
		{
			for(SampleValue sv : samples)
				if (sv.t == t)
				{
					// Don't overwrite good data with RINVAL data.
					if (!v.contains(RINVAL))
						sv.v = v;
					return;
				}
			samples.add(new SampleValue(t,v));
		}
		void sort()
		{
			Collections.sort(samples,
				new Comparator<SampleValue>()
				{
					@Override
					public int compare(SampleValue o1, SampleValue o2)
					{
						return o1.t - o2.t < 0 ? -1 : o1.t - o2.t > 0 ? 1 : 0;
					}
				
				});
		}
	}
	long bufferingStarted = -1L;
	ArrayList<SensorData> sensorDataArray = new ArrayList<SensorData>();
	
	/**
	 * Constructor for Kisters ZRXP Formatter
	 */
	public KistersFormatter()
	{
		super();
	}
	
	@Override
	public boolean requiresDecodedMessage() { return true; }

	@Override
	public boolean acceptRealDcpMessagesOnly() { return true; }

	@Override
	public boolean attemptDecode() { return true; }

	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp,
		Properties rsProps) 
		throws OutputFormatterException
	{
		sdf.setTimeZone(tz);
		tzName = tz.getID();
		
		String s = PropertiesUtil.getIgnoreCase(rsProps, "includeTZ");
		if (s != null)
			includeTZ = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "includeCNAME");
		if (s != null)
			includeCNAME = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "includeCUNIT");
		if (s != null)
			includeCUNIT = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "includeRINVAL");
		if (s != null)
			includeRINVAL = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "RINVAL");
		if (s != null)
			RINVAL = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "SiteNameType");
		if (s != null)
			siteNameType = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "SiteNameTypeAlt");
		if (s != null)
			siteNameTypeAlt = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "DataTypeStandard");
		if (s != null)
			dataTypeStandard = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "DataTypeStandardAlt");
		if (s != null)
			dataTypeStandardAlt = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "SitePrefix");
		if (s != null)
			sitePrefix = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "tzName");
		if (s != null)
			tzName = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "includeLayout");
		if (s != null)
			includeLayout = TextUtil.str2boolean(s);
		s = PropertiesUtil.getIgnoreCase(rsProps, "bufferTimeSec");
		if (s != null)
		{
			try { bufferTimeSec = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				logger.warning("Invalid bufferTimeSec property '" + s + "' ignored. Buffering disabled.");
				bufferTimeSec = 0;
			}
		}
	}

	@Override
	public void shutdown()
	{
		if (bufferTimeSec <= 0)
			return;
		
		// If any data is accumulated in the buffer, flush it now.
		try
		{
			this.flushBuffer();
		}
		catch (DataConsumerException ex)
		{
			logger.warning("Error shutting down formatter: " + ex);
		}
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		theConsumer = consumer;
		
		//MJM 20150227 If a message is nothing but missing, don't output anything.
		boolean hasData = false;
//		logger.debug3("KistersFormatter msg from " + msg.getPlatform().makeFileName() + " with time " + 
//			sdf.format(msg.getMessageTime()));
	  ts_loop:
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts == null || ts.size() == 0)
				continue;
			for(int idx=0; idx<ts.size(); idx++)
			{
				TimedVariable tv = ts.sampleAt(idx);
				if ((tv.getFlags() & (IFlags.IS_ERROR | IFlags.IS_MISSING)) == 0)
				{
//					logger.debug1("Found first data: " + ts.getSensorName() + " " 
//						+ sdf.format(tv.getTime()) + " " + tv.getStringValue());
					hasData = true;
					break ts_loop;
				}
			}
		}
		if (!hasData)
		{
			logger.debug1("Skipping message with no non-missing data");
			return;
		}
		
		
		// Get the site associated with the Platform that generated the message.
		Site platformSite = null;
		try
		{
			platformSite = msg.getRawMessage().getPlatform().getSite();
			if (platformSite == null)
				logger.warning("No site associated with platform.");
		}
		catch(Exception ex)
		{
			throw new OutputFormatterException(
				"Cannot determine platform site: " + ex.toString());
		}

		if (bufferTimeSec <= 0)
			consumer.startMessage(msg);

		// For each time series in the message ...
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts == null || ts.size() == 0)
				continue;

			// Determine the site name
			Site site = platformSite;
			Sensor sensor = ts.getSensor();
			Site sensorSite = sensor.getSensorSite();
			if (sensorSite != null)
				site = sensorSite;

			if (site == null)
			{
				logger.warning("No platform site and no site associated with " +
					"sensor " + sensor.getName() + " -- skipped.");
				continue;
			}
			
			SiteName siteName = site.getName(siteNameType);
			if (siteName == null)
				if ((siteName = site.getName(siteNameTypeAlt)) == null)
					siteName = sensorSite.getPreferredName();
			
			DataType dt = sensor.getDataType(dataTypeStandard);
			if (dt == null)
				if (dataTypeStandardAlt == null
					|| (dt = sensor.getDataType(dataTypeStandardAlt)) == null)
					dt = sensor.getDataType();

			StringBuilder headerLineBuilder = new StringBuilder();
			headerLineBuilder.append("#REXCHANGE");
			headerLineBuilder.append(sitePrefix);
			headerLineBuilder.append(siteName.getNameValue());
			headerLineBuilder.append("." + dt.getCode());
			headerLineBuilder.append(headerDelim);
			
			if (includeTZ)
			{
				headerLineBuilder.append("TZ" + tzName);
				headerLineBuilder.append(headerDelim);
			}
			if (includeRINVAL)
			{
				headerLineBuilder.append("RINVAL" + RINVAL);
				headerLineBuilder.append(headerDelim);
			}
			if (includeCNAME)
			{
				headerLineBuilder.append("CNAME" + sensor.getName());
				headerLineBuilder.append(headerDelim);
			}
			if (includeCUNIT)
			{
				String eustr = "RAW";
				EngineeringUnit eu = ts.getEU();
				if (eu != null)
					eustr = eu.getAbbr();
				headerLineBuilder.append("CUNIT" + eustr);
				headerLineBuilder.append(headerDelim);
			}
			
			String headerLine = headerLineBuilder.toString();
			if (bufferTimeSec <= 0)
			{
				// output data directly -- no buffering.
				consumer.println(headerLine);
				if (includeLayout)
					consumer.println("#LAYOUT(timestamp,value)");
			}

			ts.sort();
			for(int idx=0; idx<ts.size(); idx++)
			{
				String samp = ts.formattedSampleAt(idx);
				boolean inval = samp.equals("error") || samp.equals("missing");
				if (inval)
				{
					if (!includeRINVAL)
						continue;
					else 
						samp = RINVAL;
				}
				Date sampTime = ts.timeAt(idx);
				String dataLine = sdf.format(sampTime) + " " + samp;
				if (bufferTimeSec <= 0)
					consumer.println(dataLine); // No buffering
				else
				{
					addToBuffer(headerLine, dataLine, sampTime.getTime(), msg);
				}
			}
		}
		if (bufferTimeSec <= 0)
			consumer.endMessage();
		else if (bufferingStarted > 0L
			&& (System.currentTimeMillis() - bufferingStarted)/1000L > bufferTimeSec)
			flushBuffer();
	}
	
	protected PropertySpec kfPropSpecs[] = 
	{
		new PropertySpec("includeTZ", PropertySpec.BOOLEAN, 
			"Default=true. Set to false to exclude TZ specifier from the header."),
		new PropertySpec("tzName", PropertySpec.STRING, 
			"Default=null. Set to override the TZ name in the routing spec."
			+ " Caution: this will not change the time values, just the name in the header."),
		new PropertySpec("includeCNAME", PropertySpec.BOOLEAN, 
			"Default=false. Set to true to include CNAME in the header with the DECODES sensor name."),
		new PropertySpec("includeCUNIT", PropertySpec.BOOLEAN, 
			"Default=false. Set to true to include CUNIT in the header with the " +
			"engineering units."),
		new PropertySpec("includeRINVAL", PropertySpec.BOOLEAN, 
			"Default=true. Set to false to exclude RINVAL from the header. " +
			"RINVAL specifies the special value to be used for missing or erroneous values."),
		new PropertySpec("RINVAL", PropertySpec.STRING, 
			"Default=-777. Specifies a special value to be used for missing or erroneous " +
			"values in the output data."),
		new PropertySpec("SiteNameType", PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Specifies which DECODES site name to use in the header."),
		new PropertySpec("SiteNameTypeAlt", PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Specifies the alternate DECODES site name to use in the header " +
			"(if the primary is undefined)."),
		new PropertySpec("DataTypeStandard", PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"Specifies which DECODES data type to use in the header."),
		new PropertySpec("DataTypeStandardAlt", PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"Specifies the alternate DECODES data type to use in the header if the primary" +
			" is undefined."),
		new PropertySpec("SitePrefix", PropertySpec.STRING, 
			"A constant string to be placed before the site name in REXCHANGE values."),
		new PropertySpec("includeLayout", PropertySpec.BOOLEAN, 
			"Default=true. Set to true to include a separate header line with layout."),
		new PropertySpec("bufferTimeSec", PropertySpec.INT,
			"(# seconds, default=0) Set this to positive number to have data lines buffered and combined."
			+ " This may result in fewer ZRXP headers with more data lines.")
	};
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return kfPropSpecs;
	}


	@Override
	public boolean additionalPropsAllowed()
	{
		return false;
	}
	
	private void flushBuffer()
		throws DataConsumerException
	{
		if (bufferingStarted <= 0)
			return;
		
		// Sort the SensrData array by header. This will put all data for same platform together.
		Collections.sort(sensorDataArray,
			new Comparator<SensorData>()
			{
				@Override
				public int compare(SensorData o1, SensorData o2)
				{
					return o1.header.compareTo(o2.header);
				}
			});
		

		String platformName = "";
		for(SensorData sd : sensorDataArray)
		{
			// Within each SensorData struct, sort the samples ascending by time
			Collections.sort(sd.samples,
				new Comparator<SampleValue>()
				{
					@Override
					public int compare(SampleValue o1, SampleValue o2)
					{
						long deltat = o1.t - o2.t;
						return deltat < 0 ? -1 : deltat > 0 ? 1 : 0;
					}
				});

			String pn = sd.msg.getPlatform().makeFileName();
			if (!platformName.equals(pn))
			{
				// Platform Name is different. Start a new message.
				if (platformName.length() > 0)
					theConsumer.endMessage(); // End the previous message if one was started.
				
				theConsumer.startMessage(sd.msg);
				platformName = pn;
			}
			
			// output the header line
			theConsumer.println(sd.header);
			if (includeLayout)
				theConsumer.println("#LAYOUT(timestamp,value)");
			
			// output each data line
			for(SampleValue sv : sd.samples)
				theConsumer.println(sv.v);
		}
		
		// End the last message we were working on.
		if (platformName.length() > 0)
			theConsumer.endMessage();
		sensorDataArray.clear();
		bufferingStarted = -1;
	}
	
	private void addToBuffer(String header, String dataLine, long t, DecodedMessage dm)
	{
		if (bufferingStarted < 0)
			bufferingStarted = System.currentTimeMillis();
		SensorData sensorData = null;
		for(SensorData sd : sensorDataArray)
			if (header.equals(sd.header))
			{
				sensorData = sd;
				break;
			}
		if (sensorData == null)
			sensorDataArray.add(sensorData = new SensorData(header, dm));
		sensorData.addSample(t, dataLine);
	}
}
