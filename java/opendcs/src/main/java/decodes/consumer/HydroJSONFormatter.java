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

import ilex.util.Location;
import ilex.util.PropertiesUtil;
import ilex.util.Strftime;
import ilex.var.IFlags;
import ilex.var.NoConversionException;
import ilex.var.TimedVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.cwms.CwmsConstants;
import decodes.cwms.CwmsConsumer;
import decodes.cwms.CwmsDbConfig;
import decodes.cwms.CwmsFlags;
import decodes.cwms.CwmsSqlDatabaseIO;
import decodes.cwms.CwmsTsId;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.PresentationGroup;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;
import decodes.util.DecodesSensorCnvt;
import decodes.util.DecodesSettings;
import decodes.util.PropertySpec;

/**
 * HydroJSON format is described at https://github.com/gunnarleffler/hydroJSON
 *
 *
 */
public class HydroJSONFormatter extends OutputFormatter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private String module = "HydroJSONFormatter";
	private String timeFormat = "%Y-%m-%dT%H:%M:%S%z";
	private String indent = "  ";
	private String siteNameTypePreference = Constants.snt_CWMS;
	private String officeId = "";
	private TimeZone tz = null;
	private CwmsConsumer cwmsConsumer = null;
	private Strftime strftime = null;

	private static PropertySpec propSpecs[] =
	{
		new PropertySpec("siteNameTypePreference",
			PropertySpec.DECODES_ENUM + Constants.enum_SiteName,
			"(default='local' specify site name type to use in output"),
		new PropertySpec("timeFormat", PropertySpec.STRING,
			"(default=%Y-%m-%dT%H:%M:%S%z) SimpleDateFormat time/date format string.")
	};

	@Override
	protected void initFormatter(String type, TimeZone tz, PresentationGroup presGrp,
		Properties rsProps)
		throws OutputFormatterException
	{
		ofPropSpecs = propSpecs;

		this.tz = tz;
		String s = PropertiesUtil.getIgnoreCase(rsProps, "timeFormat");
		if (s != null)
			timeFormat = s;
		strftime = new Strftime(timeFormat);
		strftime.setTimeZone(tz);

		s = PropertiesUtil.getIgnoreCase(rsProps, "siteNameTypePreference");
		if (s != null)
			siteNameTypePreference = s;
		if (Database.getDb() != null
		 && Database.getDb().getDbIo() != null
		 && Database.getDb().getDbIo() instanceof CwmsSqlDatabaseIO)
		{
			officeId = ((CwmsSqlDatabaseIO)Database.getDb().getDbIo()).getOfficeId();
			if (officeId == null || officeId.trim().length() == 0)
				officeId = DecodesSettings.instance().CwmsOfficeId;
		}

		// HydroJSONFormatter follows the same rules as CwmsConsumer for creating TSIDs
		// from info in the DECODES database. We use the methods in CwmsConsumer
		// to accomplish this.
		cwmsConsumer = new CwmsConsumer();
		cwmsConsumer.initCwmsConfig(rsProps);
		try
		{
			CwmsDbConfig.instance().loadFromProperties(CwmsConstants.CONFIG_FILE_NAME);
		}
		catch (IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Cannot load CWMS Config from '{}' -- will proceed with defaults.",
			   	    CwmsConstants.CONFIG_FILE_NAME);
		}
		cwmsConsumer.loadShefCwmsParamMapping(CwmsDbConfig.instance().getShefCwmsParamFile());
	}

	@Override
	public void shutdown()
	{
	}

	@Override
	public void formatMessage(DecodedMessage msg, DataConsumer consumer)
		throws DataConsumerException,
		OutputFormatterException
	{
		consumer.startMessage(msg);

		// Construct a list of distinct sites to include in the output
		ArrayList<Site> sites = new ArrayList<Site>();
		for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
		{
			TimeSeries ts = tsit.next();
			if (ts.size() == 0)
				continue;
			Site site = ts.getSensor().getSite();
			if (!sites.contains(site))
				sites.add(site);
		}

		// Put out a separate block of JSON for each site
		for(int siteIdx = 0; siteIdx < sites.size(); siteIdx++)
		{
			Site site = sites.get(siteIdx);

			consumer.println("{");
			SiteName sn = site.getName(siteNameTypePreference);
			if (sn == null)
				sn = site.getPreferredName();
			if (sn == null)
				sn = site.getNameAt(0);
			consumer.println(indent + "\"" + sn.getNameValue() + "\": {");

			consumer.println(indent+indent + "\"HUC\": \"\",");
			consumer.println(indent+indent + "\"active_flag\": \"T\",");

			// coordinates block
			String lat = site.latitude;
			if (lat == null || lat.trim().length() == 0)
				lat = "";
			else if (lat.contains(":"))
			{
				try
				{
					double d = Location.parseLatitude(lat);
					lat = "" + d;
				}
				catch(NumberFormatException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Site {} -- cannot parse latitude '{}' to double -- set latitude to empty.",
					   	    sn, lat);
					lat = "";
				}
			}
			String lon = site.longitude;
			if (lon == null || lon.trim().length() == 0)
				lon = "";
			else if (lon.contains(":"))
			{
				try
				{
					double d = Location.parseLongitude(lon);
					lon = "" + d;
				}
				catch(NumberFormatException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Site {} -- cannot parse longitude '{}' to double -- set longitude to empty.",
					        sn, lon);
					lon = "";
				}

			}

			String datum = site.getProperty("horizontal_datum");
			if (datum == null)
				datum = "";
			consumer.println(indent+indent + "\"coordinates\": {");
			consumer.println(indent+indent+indent + "\"latitude\": " + lat + ",");
			consumer.println(indent+indent+indent + "\"longitude\": " + lon + ",");
			consumer.println(indent+indent+indent + "\"datum\": \"" + datum + "\"");
			consumer.println(indent+indent + "},");

			// elevation block
			double e = site.getElevation();
			String elev = (e == Constants.undefinedDouble) ? "0.0" : ("" + e);
			datum = site.getProperty("vertical_datum");
			if (datum == null)
				datum = "";
			consumer.println(indent+indent + "\"elevation\": {");
			consumer.println(indent+indent+indent + "\"accuracy\": 0.0,");
			consumer.println(indent+indent+indent + "\"datum\": \"" + datum + "\",");
			consumer.println(indent+indent+indent + "\"method\": \"\",");
			consumer.println(indent+indent+indent + "\"value\": " + elev);
			consumer.println(indent+indent + "},");

			String locType = site.getProperty("location_type");
			if (locType == null)
				locType = "";
			consumer.println(indent+indent + "\"location_type\": \"" + locType + "\",");
			String publicName = site.getPublicName();
			if (publicName == null)
				publicName = "";
			consumer.println(indent+indent + "\"name\": \"" + publicName + "\",");

			consumer.println(indent+indent + "\"responsibility\": \"" + officeId + "\",");

			consumer.println(indent+indent + "\"time_format\": \"" + timeFormat + "\",");


			consumer.println(indent+indent + "\"timeseries\": {");
			ArrayList<TimeSeries> ts2process = new ArrayList<TimeSeries>();
			for(Iterator<TimeSeries> tsit = msg.getAllTimeSeries(); tsit.hasNext(); )
			{
				TimeSeries ts = tsit.next();
				if (ts.size() == 0)
					continue;
				String tsid = null;
				if (ts.getSensor() instanceof DecodesSensorCnvt)
					tsid = ((DecodesSensorCnvt)ts.getSensor()).getDbTsId();
				// Otherwise (this is from DECODES) construct using same rules as CwmsConsumer.
				if (tsid == null)
					tsid = cwmsConsumer.createTimeSeriesDesc(ts, site);
				if (tsid == null)
				{
					log.warn("Cannot make CWMS TSID for sensor[{}] '{}' -- Make sure CWMS param is defined.",
							 ts.getSensorNumber(), ts.getSensorName());
					continue;
				}

				ts2process.add(ts);
			}



			for(int tsIdx = 0; tsIdx < ts2process.size(); tsIdx ++)
			{
				TimeSeries ts = ts2process.get(tsIdx);
				ts.sort();

				// If this is from OutputTs, we will have read real CWMS tsids
				// from the database. Use these
				String tsid = null;
				if (ts.getSensor() instanceof DecodesSensorCnvt)
					tsid = ((DecodesSensorCnvt)ts.getSensor()).getDbTsId();
				// Otherwise (this is from DECODES) construct using same rules as CwmsConsumer.
				if (tsid == null)
					tsid = cwmsConsumer.createTimeSeriesDesc(ts, site);
				CwmsTsId cwmsTsId = new CwmsTsId();
				cwmsTsId.setUniqueString(tsid);

				consumer.println(indent+indent+indent + "\"" + tsid + "\": {");

				consumer.println(indent+indent+indent+indent + "\"values\": [");
				int count = 0;
				int minIdx = -1, maxIdx = -1;
				Date startTime = null, endTime = null;
				for(int idx = 0; idx < ts.size(); idx++)
				{
					TimedVariable tv = ts.sampleAt(idx);
					if ((tv.getFlags() & (IFlags.IS_ERROR|IFlags.IS_MISSING)) != 0)
						continue;

					consumer.println(indent+indent+indent+indent+indent + "[");
					consumer.println(indent+indent+indent+indent+indent+indent + "\""
						+ formatTime(tv.getTime()) + "\",");
					consumer.println(indent+indent+indent+indent+indent+indent +
						ts.formattedSampleAt(idx) + ",");
					consumer.println(indent+indent+indent+indent+indent+indent +
						CwmsFlags.flag2CwmsQualityCode(tv.getFlags()));

					consumer.println(indent+indent+indent+indent+indent + "]"
						+ (idx < ts.size()-1 ? "," : ""));

					try
					{
						double v = tv.getDoubleValue();
						if (minIdx == -1)
							minIdx = idx;
						else if (v < ts.sampleAt(minIdx).getDoubleValue())
							minIdx = idx;
						if (maxIdx == -1)
							maxIdx = idx;
						else if (v > ts.sampleAt(minIdx).getDoubleValue())
							maxIdx = idx;
					}
					catch(NoConversionException ex) { /* won't happen */ }
					if (startTime == null || tv.getTime().before(startTime))
						startTime = tv.getTime();
					if (endTime == null || tv.getTime().after(endTime))
						endTime = tv.getTime();
					count++;
				}
				consumer.println(indent+indent+indent+indent + "],");

				consumer.println(indent+indent+indent+indent + "\"quality_type\": \"string\",");
				consumer.println(indent+indent+indent+indent + "\"parameter\": \"" +
					cwmsTsId.getDataType().getCode() + "\",");
				consumer.println(indent+indent+indent+indent + "\"duration\": \"" +
					cwmsTsId.getDuration() + "\",");
				consumer.println(indent+indent+indent+indent + "\"interval\": \"" +
					cwmsTsId.getInterval() + "\",");
				consumer.println(indent+indent+indent+indent + "\"units\": \"" +
					ts.getUnits() + "\",");
				consumer.println(indent+indent+indent+indent + "\"count\": " + count + ",");
				consumer.println(indent+indent+indent+indent + "\"min_value\": "
					+ (minIdx==-1?"null":ts.formattedSampleAt(minIdx)) + ",");
				consumer.println(indent+indent+indent+indent + "\"max_value\": "
					+ (maxIdx==-1?"null":ts.formattedSampleAt(maxIdx)) + ",");
				consumer.println(indent+indent+indent+indent + "\"start_time\": "
					+ (startTime==null?"null": ("\"" + formatTime(startTime) + "\"")) + ",");
				consumer.println(indent+indent+indent+indent + "\"end_time\": "
					+ (endTime==null?"null": ("\"" + formatTime(endTime) + "\"")));

				consumer.println(indent+indent+indent + "}"
					+ (tsIdx < ts2process.size()-1 ? "," : ""));
			}


			// no comma if this is the last site.
			consumer.println(indent+indent + "}" + (siteIdx < sites.size()-1 ? "," : ""));

			consumer.println(indent + "}");
			consumer.println("}");

		}

		consumer.endMessage();
	}

	private String formatTime(Date d)
	{
		return strftime.format(d);
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

}
