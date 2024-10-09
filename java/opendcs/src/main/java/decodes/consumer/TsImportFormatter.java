/**
 * $Id: TsImportFormatter.java,v 1.1 2020/02/20 15:32:38 mmaloney Exp $
 * 
 * $Log: TsImportFormatter.java,v $
 * Revision 1.1  2020/02/20 15:32:38  mmaloney
 * Final fixes.
 *
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
import ilex.var.TimedVariable;
import opendcs.opentsdb.OpenTsdbConsumer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import decodes.cwms.CwmsDbConfig;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSensorCnvt;
import decodes.util.PropertySpec;

/**
 * This class formats a DCP message for ingest into OpenTSDB.
 * Since OpenTSDB uses CWMS Time Series Identifiers, enough information must
 * be present in the sensor records to create a full 6-part CWMS TSID.
 *
 * @author Michael Maloney, Cove Software, LLC
 */
public class TsImportFormatter 
	extends OutputFormatter
{
	public static final String module = "OpenTsdbFormatter";
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	
	/** Can be set by tzName property. Defaults to abbreviation from Routing Spec TZ */
	private String tzName = null;
	/** Can be set by "includeCNAME" property */
	/** Can be set by "SiteNameType" property */
	private String siteNameType = Constants.snt_CWMS;
	/** Can be set by "SiteNameTypeAlt" property */
	private String siteNameTypeAlt = Constants.snt_local;
	/** Can be set by "DataTypeStandard" property */
	private String dataTypeStandard = Constants.datatype_CWMS;
	/** Can be set by "DataTypeStandardAlt" property */
	private int bufferTimeSec = 0;
	private DataConsumer theConsumer = null;
	private String newline = "\n";
	
	// Used, if needed, to construct CWMS TSIDs from Sensor Info.
	private OpenTsdbConsumer openTsdbConsumer = null;
	
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
	public TsImportFormatter()
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
		
		String s = PropertiesUtil.getIgnoreCase(rsProps, "SiteNameType");
		if (s != null)
			siteNameType = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "SiteNameTypeAlt");
		if (s != null)
			siteNameTypeAlt = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "DataTypeStandard");
		if (s != null)
			dataTypeStandard = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "tzName");
		if (s != null)
		{
			TimeZone tz2 = TimeZone.getTimeZone(s);
			if (tz2 == null)
			{
				logger.warning(module + " Invalid tzName propertye '" + s + "': will use " + tzName);
			}
			else
				tzName = s;
		}
		s = PropertiesUtil.getIgnoreCase(rsProps, "bufferTimeSec");
		if (s != null)
		{
			try { bufferTimeSec = Integer.parseInt(s.trim()); }
			catch(Exception ex)
			{
				logger.warning(module + " Invalid bufferTimeSec property '" + s 
					+ "' ignored. Buffering disabled.");
				bufferTimeSec = 0;
			}
		}
		newline = System.getProperty("line.separator");
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
		
		// If a message is nothing but missing, don't output anything.
		boolean hasData = false;

		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts == null || ts.size() == 0)
				continue;
			hasData = true;
		}
		if (!hasData)
		{
			logger.debug1(module + " Skipping message with no non-missing data");
			return;
		}
		
		
		// Get the site associated with the Platform that generated the message.
		Site platformSite = null;
		try
		{
			platformSite = msg.getRawMessage().getPlatform().getSite();
			if (platformSite == null)
				logger.warning(module + " No site associated with platform.");
		}
		catch(UnknownPlatformException ex)
		{
			logger.warning(module + " Cannot determine platform.");
		}

		if (bufferTimeSec <= 0)
			consumer.startMessage(msg);

		// For each time series in the message ...
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts == null || ts.size() == 0)
				continue;
			Sensor sensor = ts.getSensor();

			// Determine the site name
			Site site = sensor.getSensorSite();
			if (site == null)
				site = platformSite;

			if (site == null)
			{
				logger.warning(module + " No platform site and no site associated with " +
					"sensor " + sensor.getName() + " -- skipped.");
				continue;
			}
			
			SiteName siteName = site.getName(siteNameType);
			if (siteName == null)
				if ((siteName = site.getName(siteNameTypeAlt)) == null)
					siteName = site.getPreferredName();
			
			DataType dt = sensor.getDataType(dataTypeStandard);
			if (dt == null)
				dt = sensor.getDataType();

			StringBuilder headerBuilder = new StringBuilder();
			headerBuilder.append("SET:TZ=" + tzName + newline);
			headerBuilder.append("TSID:" + makeTsid(sensor, ts, platformSite) + newline);
			headerBuilder.append("SET:UNITS=" + ts.getUnits());
			// Note: No newline after units because it will be printed with println.
			
			String header = headerBuilder.toString();
			if (bufferTimeSec <= 0)
				// output data directly -- no buffering.
				consumer.println(header);

			ts.sort();
			for(int idx=0; idx<ts.size(); idx++)
			{
				TimedVariable tv = ts.sampleAt(idx);
				String sampVal = ts.formattedSampleAt(idx);
				Date sampTime = tv.getTime();
				tv.resetChanged();
				int flags = tv.getFlags();
				
				String dataLine = sdf.format(sampTime) + "," + sampVal + ",0x" 
					+ Integer.toHexString(flags);
				if (bufferTimeSec <= 0)
					consumer.println(dataLine); // No buffering
				else
				{
					addToBuffer(header, dataLine, sampTime.getTime(), msg);
				}
			}
		}
		if (bufferTimeSec <= 0)
			consumer.endMessage();
		else if (bufferingStarted > 0L
			&& (System.currentTimeMillis() - bufferingStarted)/1000L > bufferTimeSec)
			flushBuffer();
	}
	
	private String makeTsid(Sensor sensor, TimeSeries ts, Site platformSite)
	{
		if (sensor instanceof DecodesSensorCnvt)
			return ((DecodesSensorCnvt)sensor).getDbTsId();
		
		if (openTsdbConsumer == null)
		{
			openTsdbConsumer = new OpenTsdbConsumer();
			openTsdbConsumer.loadShefCwmsParamMapping(CwmsDbConfig.instance().getShefCwmsParamFile());
		}
		
		return openTsdbConsumer.buildTSID(ts, platformSite);
	}

	protected PropertySpec kfPropSpecs[] = 
	{
		new PropertySpec("tzName", PropertySpec.STRING, 
			"Default=null. Set to override the TZ name in the routing spec."),
		new PropertySpec("SiteNameType", PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Specifies which DECODES site name to use in the header (default=CWMS)."),
		new PropertySpec("SiteNameTypeAlt", PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"Specifies the alternate DECODES site name to use in the header " +
			"(if the primary is undefined)."),
		new PropertySpec("DataTypeStandard", PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"Specifies which DECODES data type to use in the header (default=CWMS Param)."),
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
