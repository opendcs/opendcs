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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.decodes.api.DataMessage;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.IDateFormat;
import ilex.util.PropertiesUtil;
import ilex.var.TimedVariable;

import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.Sensor;
import decodes.decoder.TimeSeries;
import decodes.util.PropertySpec;

/**
 * Data source that retrieves USGS water data using the USGS Water Data API
 * (api.waterdata.usgs.gov).
 *
 * <p>This source overrides {@link #getDataMessage()} to return a DecodedMessage.
 * Each call fetches all Daily or Continuous (instantaneous) data.
 * Daily data is fetched when Config-Sensor interval is 24:00:00; otherwise continuous.</p>
 *
 * <p>Platform sites are identified by USGS site number from the network list transport IDs.
 * Sensor parameter codes are matched from ConfigSensor USGS/EPA data types.
 * Statistic codes come from the ConfigSensor usgsStatCode field.
 * Sublocation comes from the ConfigSensor/PlatformSensor "usgsSubLocation" property.
 * </p>
 *
 * <p>Properties:</p>
 * <ul>
 *   <li>dataTypeStandard - default="usgs". Determines which sensor data types to match.</li>
 *   <li>apiKey - Optional USGS Water Data API key.</li>
 * </ul>
 */
public class UsgsWaterDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	private final ArrayList<String> aggIds = new ArrayList<>();
	private final ArrayList<Platform> platforms = new ArrayList<>();
	private final ArrayList<String> mediumTypes = new ArrayList<>();

	private String dataTypeStandard = "usgs";

	private final Properties myProps = new Properties();

	private Date dSince = null;
	private Date dUntil = null;

	private int xportIdx = 0;

	private List<UsgsWaterDataAdapter.SensorResult> currentResults = null;
	private Platform currentPlatform = null;

    private static final PropertySpec[] myPropSpecs =
	{
		new PropertySpec("dataTypeStandard",
			PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"To select which sensor data type to match, default=usgs"),
		new PropertySpec("apiKey", PropertySpec.STRING,
			"Optional USGS Water Data API key (or set USGS_WATER_API_KEY env var)")
	};

	public UsgsWaterDataSource(DataSource ds, Database db)
	{
		super(ds, db);
	}

	@Override
	public void processDataSource()
	{
		PropertiesUtil.copyProps(myProps, getDataSource().getArguments());
	}

	@Override
	public void init(Properties rsProps, String since, String until,
		Vector<NetworkList> netlists) throws DataSourceException
	{
		log.info("Initializing UsgsWaterDataSource ...");
		PropertiesUtil.copyProps(myProps, rsProps);

		String s = PropertiesUtil.getIgnoreCase(myProps, "dataTypeStandard");
		if (s != null)
			dataTypeStandard = s;

		s = PropertiesUtil.getIgnoreCase(myProps, "usgsApiKey");
		if (s != null && !s.isEmpty())
			UsgsWaterDataAdapter.setApiKey(s);

		dSince = since != null ? IDateFormat.parse(since) :
			new Date(System.currentTimeMillis() - 3600000L * 24);
		dUntil = until != null ? IDateFormat.parse(until) : new Date();
		log.info("since={}, until={}", dSince, dUntil);

		aggIds.clear();
		platforms.clear();
		mediumTypes.clear();
		if (netlists != null)
		{
			for (NetworkList nl : netlists)
			{
				for (NetworkListEntry nle : nl.values())
				{
					String tid = nle.getTransportId();
					if (!aggIds.contains(tid))
					{
						aggIds.add(tid);
						mediumTypes.add(nl.transportMediumType);
						try
						{
							platforms.add(nl.getDatabase().platformList.getPlatform(
								nl.transportMediumType, tid));
						}
						catch (DatabaseException ex)
						{
							throw new DataSourceException(
								"Cannot find platform for '" + tid + "'", ex);
						}
					}
				}
			}
		}

		if (aggIds.isEmpty())
			throw new DataSourceException("init() No medium ids.");

		UsgsWaterDataAdapter.prefetchMetadata(aggIds);
		xportIdx = 0;
	}

	@Override
	public void close()
	{
		UsgsWaterDataAdapter.clearMetadataCache();
		currentResults = null;
		currentPlatform = null;
	}

	@Override
	public boolean supportsTimeRanges()
	{
		return true;
	}

	/**
	 * Fetches data for the next platform using the USGS Water Data API
	 * via {@link UsgsWaterDataAdapter}.
	 * Returns a stub RawMessage with platform linkage set.
	 * The actual time-series data is held in {@link #currentResults}
	 * and mapped into a DecodedMessage by {@link #getDataMessage()}.
	 */
	@Override
	protected RawMessage getSourceRawMessage() throws DataSourceException
	{
		while (xportIdx < aggIds.size())
		{
            String currentMediumId = aggIds.get(xportIdx);
			currentPlatform = platforms.get(xportIdx);
            String currentMediumType = mediumTypes.get(xportIdx);
			xportIdx++;

			if (currentPlatform == null)
			{
				log.warn("No platform for transport ID '{}' -- skipped.",
                        currentMediumId);
				continue;
			}

			String siteNum = resolveSiteNumber(currentPlatform, currentMediumId);
			if (siteNum == null)
			{
				log.error("Cannot resolve site number for transport ID '{}'.",
                        currentMediumId);
				continue;
			}

			try
			{
				currentResults = UsgsWaterDataAdapter.fetchPlatformData(
					currentPlatform, siteNum, dSince, dUntil, dataTypeStandard);
			}
			catch (Exception ex)
			{
				log.error("Error fetching data for site {}: {}",
					siteNum, ex.getMessage());
				continue;
			}

			if (currentResults.isEmpty())
			{
				log.info("No data returned for site {} -- skipped.", siteNum);
				continue;
			}

			RawMessage rm = new RawMessage();
			rm.setPlatform(currentPlatform);
			TransportMedium tm = findTransportMedium(
				currentPlatform, currentMediumId, currentMediumType);
			if (tm != null)
				rm.setTransportMedium(tm);
			rm.setTimeStamp(new Date());
			rm.setMediumId(currentMediumId);

			log.info("Fetched {} sensor(s) for site {} ({})",
				currentResults.size(), siteNum,
				currentPlatform.getSiteName(false));
			return rm;
		}

		throw new DataSourceEndException(aggIds.size() + " platforms processed.");
	}

	/**
	 * Overrides getDataMessage to return a DecodedMessage directly.
	 * Calls getRawMessage() to advance to the next platform and fetch data
	 * via the adapter, then maps the structured time-series results into a
	 * DecodedMessage with populated TimeSeries/TimedVariable samples.
	 */
	@Override
	public DataMessage getDataMessage() throws DataSourceException
	{
		RawMessage rm = getRawMessage();
		if (rm == null)
			return null;

		if (currentResults == null || currentResults.isEmpty())
			throw new DataSourceException(
				"getDataMessage called with no fetched data");

		try
		{
			DecodedMessage dm = new DecodedMessage(rm, false);

			for (UsgsWaterDataAdapter.SensorResult sr : currentResults)
			{
				List<TimedVariable> samples = sr.toTimedVariables();
				if (samples.isEmpty())
					continue;

				TimeSeries ts = new TimeSeries(sr.sensorNumber);
				ts.setSensor(new Sensor(sr.configSensor, null,
					sr.platformSensor, currentPlatform));
				ts.setUnits(sr.getUnitOfMeasure());

				for (TimedVariable tv : samples)
					ts.addSample(tv);

				dm.addTimeSeries(ts);
				log.debug("Sensor {} ({}): {} samples, units={}",
					sr.sensorNumber,
					sr.daily ? "daily" : "continuous",
					ts.size(), sr.getUnitOfMeasure());
			}

			return dm;
		}
		catch (Exception ex)
		{
			throw new DataSourceException(
				"Failed to build DecodedMessage: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Find the TransportMedium on the platform matching the medium ID
	 * and type from the network list.
	 */
	private TransportMedium findTransportMedium(Platform p, String mediumId,
		String mediumType)
	{
		for (TransportMedium tm : p.transportMedia)
		{
			if (mediumId.equalsIgnoreCase(tm.getMediumId())
				&& (mediumType == null
					|| mediumType.equalsIgnoreCase(tm.getMediumType())))
			{
				return tm;
			}
		}
		// Fallback: match by medium ID only
		for (TransportMedium tm : p.transportMedia)
		{
			if (mediumId.equalsIgnoreCase(tm.getMediumId()))
				return tm;
		}
		return null;
	}

	/**
	 * Resolve the USGS site number from the medium ID or the platform's
	 * site record.
	 */
	private String resolveSiteNumber(Platform p, String mediumId)
	{
		// If medium ID is all digits, use it directly as USGS site number
		boolean allDigits = true;
		for (int i = 0; i < mediumId.length(); i++)
		{
			if (!Character.isDigit(mediumId.charAt(i)))
			{
				allDigits = false;
				break;
			}
		}
		if (allDigits)
			return mediumId;

		log.warn("Medium ID '{}' is not a USGS site number. "
			+ "Checking site record.", mediumId);
		Site site = p.getSite();
		if (site == null)
		{
			log.warn("Platform for '{}' has no site record -- skipped.",
				mediumId);
			return null;
		}
		SiteName sn = site.getName(Constants.snt_USGS);
		if (sn == null)
		{
			log.warn("Platform for '{}' has no USGS site name -- skipped.",
				mediumId);
			return null;
		}
		return sn.getNameValue();
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), myPropSpecs);
	}
}