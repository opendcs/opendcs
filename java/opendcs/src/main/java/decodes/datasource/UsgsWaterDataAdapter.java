/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package decodes.datasource;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataType;
import decodes.db.Platform;
import decodes.db.PlatformSensor;
import ilex.util.TextUtil;
import ilex.var.TimedVariable;
import ilex.var.Variable;
import ktarbet.usgs.waterdata.DailyValue;
import ktarbet.usgs.waterdata.InstantaneousValue;
import ktarbet.usgs.waterdata.TimeSeriesFilter;
import ktarbet.usgs.waterdata.TimeSeriesMetadata;
import ktarbet.usgs.waterdata.TimeSeries;
import ktarbet.usgs.waterdata.UsgsWaterDataApi;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
 * Encapsulates calls to {@link UsgsWaterDataApi} for fetching time-series data
 * for a single monitoring location based on platform sensor configuration.
 *
 * <p>For each non-omitted sensor the adapter:</p>
 * <ol>
 *   <li>Resolves the USGS parameter code from ConfigSensor data types</li>
 *   <li>Filters site metadata by parameter code, statistic code, sublocation,
 *       and computation period/type</li>
 *   <li>Fetches daily or continuous time-series data for the matching metadata</li>
 * </ol>
 */
final class UsgsWaterDataAdapter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private UsgsWaterDataAdapter()
	{
		// utility class -- do not instantiate
	}

	/** 24 hours in seconds -- sensors with this recording interval are treated as daily. */
	private static final int DAILY_INTERVAL_SECONDS = 86400;

	/**
	 * Set the USGS Water Data API key.
	 * Delegates to {@link UsgsWaterDataApi#setApiKey(String)}.
	 */
	public static void setApiKey(String apiKey)
	{
		UsgsWaterDataApi.setApiKey(apiKey);
	}

	/**
	 * Set the application name included in the User-Agent header.
	 * Delegates to {@link UsgsWaterDataApi#setApplicationName(String)}.
	 */
	public static void setApplicationName(String name)
	{
		UsgsWaterDataApi.setApplicationName(name);
	}

	/** Cached metadata from {@link #prefetchMetadata}, keyed by monitoring location ID. */
	private static Map<String, List<TimeSeriesMetadata>> metadataCache;

	/**
	 * Batch-fetch time-series metadata for multiple sites and cache internally.
	 * {@link #fetchPlatformData} uses the cache to avoid per-site API calls,
	 * reducing usage against the 1,000 requests/hour API limit.
	 *
	 * @param siteNumbers list of USGS site numbers (digits only, e.g. "13037500")
	 */
	public static void prefetchMetadata(List<String> siteNumbers)
	{
		String[] siteIds = new String[siteNumbers.size()];
		for (int i = 0; i < siteNumbers.size(); i++)
		{
			String id = siteNumbers.get(i);
			siteIds[i] = id.startsWith("USGS-") ? id : "USGS-" + id;
		}
		try
		{
			metadataCache = UsgsWaterDataApi.getTimeSeriesMetadata(siteIds);
			log.info("Batch-fetched metadata for {} of {} sites",
				metadataCache.size(), siteNumbers.size());
		}
		catch (Exception ex)
		{
			log.error("Error batch-fetching metadata for {} sites: {}",
				siteNumbers.size(), ex.getMessage());
			metadataCache = null;
		}
	}

	/**
	 * Clear the metadata cache. Should be called when processing is complete.
	 */
	public static void clearMetadataCache()
	{
		metadataCache = null;
	}

	/**
	 * Holds one fetched time-series result associated with a platform sensor.
	 */
	static final class SensorResult
	{
		public final int sensorNumber;
		public final ConfigSensor configSensor;
		public final PlatformSensor platformSensor;
		public final boolean daily;
		public final TimeSeries<InstantaneousValue> continuousData;
		public final TimeSeries<DailyValue> dailyData;

		private SensorResult(int sensorNumber, ConfigSensor cs, PlatformSensor ps,
			boolean daily,
			TimeSeries<InstantaneousValue> continuous,
			TimeSeries<DailyValue> dailyTs)
		{
			this.sensorNumber = sensorNumber;
			this.configSensor = cs;
			this.platformSensor = ps;
			this.daily = daily;
			this.continuousData = continuous;
			this.dailyData = dailyTs;
		}

		static SensorResult continuous(int sensorNumber, ConfigSensor cs,
			PlatformSensor ps, TimeSeries<InstantaneousValue> data)
		{
			return new SensorResult(sensorNumber, cs, ps, false, data, null);
		}

		static SensorResult daily(int sensorNumber, ConfigSensor cs,
			PlatformSensor ps, TimeSeries<DailyValue> data)
		{
			return new SensorResult(sensorNumber, cs, ps, true, null, data);
		}

		public String getUnitOfMeasure()
		{
			return daily ? dailyData.getUnitOfMeasure() : continuousData.getUnitOfMeasure();
		}

		/**
		 * Convert the fetched data into a list of TimedVariable samples,
		 * filtering out undefined values.
		 */
		public List<TimedVariable> toTimedVariables()
		{
			List<TimedVariable> list = new ArrayList<>();
			if (daily)
			{
				for (int i = 0; i < dailyData.size(); i++)
				{
					DailyValue dv = dailyData.get(i);
					if (dv.value == UsgsWaterDataApi.UNDEFINED_DOUBLE)
						continue;
					Date sampleTime = Date.from(
						dv.date.atStartOfDay(ZoneOffset.UTC).toInstant());
					TimedVariable tv = new TimedVariable(new Variable(dv.value));
					tv.setTime(sampleTime);
					list.add(tv);
				}
			}
			else
			{
				for (int i = 0; i < continuousData.size(); i++)
				{
					InstantaneousValue iv = continuousData.get(i);
					if (iv.value == UsgsWaterDataApi.UNDEFINED_DOUBLE)
						continue;
					Date sampleTime = Date.from(iv.time);
					TimedVariable tv = new TimedVariable(new Variable(iv.value));
					tv.setTime(sampleTime);
					list.add(tv);
				}
			}
			return list;
		}
	}

	/**
	 * Fetch time-series data for all non-omitted sensors on a platform.
	 *
	 * @param platform the platform whose sensors define what to fetch
	 * @param siteNumber the USGS site number (digits only, e.g. "12345678")
	 * @param since start of time range
	 * @param until end of time range
	 * @param dataTypeStandard data type standard to match (typically "usgs")
	 * @return list of sensor results (may be empty, never null)
	 */
	public static List<SensorResult> fetchPlatformData(
		Platform platform,
		String siteNumber,
		Date since,
		Date until,
		String dataTypeStandard)
		throws Exception
	{
		List<TimeSeriesMetadata> allMetadata;
		if (metadataCache != null)
		{
			String key = siteNumber.startsWith("USGS-") ? siteNumber
				: "USGS-" + siteNumber;
			allMetadata = metadataCache.getOrDefault(key, Collections.emptyList());
		}
		else
		{
			allMetadata = UsgsWaterDataApi.getTimeSeriesMetadata(siteNumber);
		}
		return fetchPlatformData(platform, siteNumber, since, until,
			dataTypeStandard, allMetadata);
	}

	/**
	 * Fetch time-series data for all non-omitted sensors on a platform,
	 * using pre-fetched metadata.
	 *
	 * @param platform the platform whose sensors define what to fetch
	 * @param siteNumber the USGS site number (digits only, e.g. "12345678")
	 * @param since start of time range
	 * @param until end of time range
	 * @param dataTypeStandard data type standard to match (typically "usgs")
	 * @param allMetadata pre-fetched time-series metadata for this site
	 * @return list of sensor results (may be empty, never null)
	 */
	public static List<SensorResult> fetchPlatformData(
		Platform platform,
		String siteNumber,
		Date since,
		Date until,
		String dataTypeStandard,
		List<TimeSeriesMetadata> allMetadata)
	{
		List<SensorResult> results = new ArrayList<>();

		if (platform.getConfig() == null)
		{
			log.warn("Platform for site {} has no configuration", siteNumber);
			return results;
		}

		if (allMetadata == null || allMetadata.isEmpty())
		{
			log.warn("No metadata returned from API for site {}", siteNumber);
			return results;
		}

		log.debug("Site {} has {} metadata entries", siteNumber, allMetadata.size());

		String startInstant = since.toInstant()
			.truncatedTo(ChronoUnit.SECONDS).toString();
		String endInstant = until.toInstant()
			.truncatedTo(ChronoUnit.SECONDS).toString();
		String startDate = formatLocalDate(since);
		String endDate = formatLocalDate(until);

		for (ConfigSensor cs : platform.getConfig().getSensorVec())
		{
			int sensorNum = cs.sensorNumber;
			PlatformSensor ps = platform.getPlatformSensor(sensorNum);

			if (isSensorOmitted(cs, ps))
			{
				log.debug("Sensor {} omitted -- skipped", sensorNum);
				continue;
			}

			DataType dt = resolveDataType(cs, dataTypeStandard);
			if (dt == null)
			{
				log.debug("Sensor {} has no '{}' data type -- skipped",
					sensorNum, dataTypeStandard);
				continue;
			}

			String parameterCode = dt.getCode();
			String usgsStatCode = cs.getUsgsStatCode();
			String usgsSubLocation = getProperty(cs, ps, "usgsSubLocation");
			String usgsWebDescription = getProperty(cs, ps, "usgsWebDescription");
			boolean isDaily = cs.recordingInterval == DAILY_INTERVAL_SECONDS;

			TimeSeriesFilter filter = TimeSeriesMetadata.filter(allMetadata)
				.dateRange(
					since.toInstant().atZone(ZoneOffset.UTC).toLocalDate().minusYears(2),
					until.toInstant().atZone(ZoneOffset.UTC).toLocalDate())
				.parameterCode(parameterCode)
				.statisticId(usgsStatCode)
				.webDescriptionContains(usgsWebDescription)
				.sublocation(usgsSubLocation);

			if (isDaily)
				filter = filter.computationPeriod("Daily");
			else
				filter = filter.computation("Instantaneous");

			List<TimeSeriesMetadata> matches = filter.toList();

			if (matches.isEmpty())
			{
				log.debug("No metadata match for sensor {} "
					+ "(param={}, stat={}, subloc={}, daily={})",
					sensorNum, parameterCode, usgsStatCode,
					usgsSubLocation, isDaily);
				continue;
			}

			if (matches.size() > 1)
			{
				log.warn("Multiple metadata matches ({}) for sensor {} "
					+ "(param={}, stat={}, subloc={}). Using first.",
					matches.size(), sensorNum, parameterCode,
					usgsStatCode, usgsSubLocation);
			}

			TimeSeriesMetadata meta = matches.getFirst();

			try
			{
				if (isDaily)
				{
					TimeSeries<DailyValue> tsData =
						UsgsWaterDataApi.getDailyTimeSeries(
							meta, startDate, endDate);
					if (!tsData.isEmpty())
					{
						results.add(SensorResult.daily(sensorNum, cs, ps, tsData));
						log.debug("Sensor {} (param={}): {} daily values",
							sensorNum, parameterCode, tsData.size());
					}
				}
				else
				{
					TimeSeries<InstantaneousValue> tsData =
						UsgsWaterDataApi.getContinuousTimeSeries(
							meta, startInstant, endInstant);
					if (!tsData.isEmpty())
					{
						results.add(SensorResult.continuous(sensorNum, cs, ps, tsData));
						log.debug("Sensor {} (param={}): {} continuous values",
							sensorNum, parameterCode, tsData.size());
					}
				}
			}
			catch (Exception ex)
			{
				log.error("Error fetching data for sensor {} (param={}): {}",
					sensorNum, parameterCode, ex.getMessage());
			}
		}

		return results;
	}

	/**
	 * Check if a sensor should be omitted.
	 * PlatformSensor "omit" property overrides ConfigSensor "omit".
	 */
	private static boolean isSensorOmitted(ConfigSensor cs, PlatformSensor ps)
	{
		String omit = cs.getProperty("omit");
		if (ps != null && ps.getProperty("omit") != null)
			omit = ps.getProperty("omit");
		return TextUtil.str2boolean(omit);
	}

	/**
	 * Get a property value, with PlatformSensor overriding ConfigSensor.
	 */
	private static String getProperty(ConfigSensor cs, PlatformSensor ps,
		String name)
	{
		String val = cs.getProperty(name);
		if (ps != null && ps.getProperty(name) != null)
			val = ps.getProperty(name);
		return val;
	}

	/**
	 * Resolve the USGS or EPA data type for a config sensor.
	 * Falls back between USGS and EPA standards since they use equivalent codes.
	 */
	static DataType resolveDataType(ConfigSensor cs, String dataTypeStandard)
	{
		DataType dt = cs.getDataType(dataTypeStandard);
		if (dt != null)
			return dt;

		if (dataTypeStandard.equalsIgnoreCase(Constants.datatype_USGS)
			|| dataTypeStandard.equalsIgnoreCase("usgs"))
		{
			dt = cs.getDataType(Constants.datatype_EPA);
		}
		else if (dataTypeStandard.equalsIgnoreCase(Constants.datatype_EPA))
		{
			dt = cs.getDataType(Constants.datatype_USGS);
			if (dt == null)
				dt = cs.getDataType("usgs");
		}
		return dt;
	}

	/** Format a Date as ISO local date (YYYY-MM-DD) for daily API calls. */
	private static String formatLocalDate(Date d)
	{
		return DateTimeFormatter.ISO_LOCAL_DATE.format(
			d.toInstant().atZone(ZoneOffset.UTC).toLocalDate());
	}
}