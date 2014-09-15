/**
 * $Id$
 * 
 * $Log$
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

import java.text.SimpleDateFormat;
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
	
	public static final String headerDelim = "|*|";
	
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
	}

	@Override
	public void shutdown()
	{
		// Nothing to do
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException, OutputFormatterException
	{
		consumer.startMessage(msg);
		
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

			StringBuilder headerLine = new StringBuilder();
			headerLine.append("#REXCHANGE");
			headerLine.append(sitePrefix);
			headerLine.append(siteName.getNameValue());
			headerLine.append("." + dt.getCode());
			headerLine.append(headerDelim);
			
			if (includeTZ)
			{
				headerLine.append("TZ" + tzName);
				headerLine.append(headerDelim);
			}
			if (includeRINVAL)
			{
				headerLine.append("RINVAL" + RINVAL);
				headerLine.append(headerDelim);
			}
			if (includeCNAME)
			{
				headerLine.append("CNAME" + sensor.getName());
				headerLine.append(headerDelim);
			}
			if (includeCUNIT)
			{
				String eustr = "RAW";
				EngineeringUnit eu = ts.getEU();
				if (eu != null)
					eustr = eu.getAbbr();
				headerLine.append("CUNIT" + eustr);
				headerLine.append(headerDelim);
			}
				
			consumer.println(headerLine.toString());
			if (includeLayout)
				consumer.println("#LAYOUT(timestamp,value)");

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
				consumer.println(sdf.format(ts.timeAt(idx)) + " " + samp);
			}
		}
		consumer.endMessage();
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
			"Default=true. Set to true to include a separate header line with layout.")
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
}
