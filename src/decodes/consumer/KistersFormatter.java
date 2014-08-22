/**
 * $Id$
 * 
 * $Log$
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
	private String siteNameType = Constants.snt_local;
	/** Can be set by "DataTypeStandard" property */
	private String dataTypeStandard = Constants.datatype_SHEF;
	/** Can be set by "SitePrefix" property */
	private String sitePrefix = "";
	
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
		s = PropertiesUtil.getIgnoreCase(rsProps, "DataTypeStandard");
		if (s != null)
			dataTypeStandard = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "SitePrefix");
		if (s != null)
			sitePrefix = s;
		s = PropertiesUtil.getIgnoreCase(rsProps, "tzName");
		if (s != null)
			tzName = s;
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
				siteName = sensorSite.getPreferredName();
			
			DataType dt = sensor.getDataType(dataTypeStandard);
			if (dt == null)
			{
				dt = sensor.getDataType();
				DataType ndt = dt.findEquivalent(dataTypeStandard);
				if (ndt != null)
					dt = ndt;
			}

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
}
