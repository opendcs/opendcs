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

* Opens source software by Cove Software, LLC.
*/
package decodes.datasource;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.IDateFormat;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.db.ConfigSensor;
import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.PlatformSensor;
import decodes.util.PropertySpec;


/**
 * This class is modeled on the generic WebAbstractDataSource. It is designed
 * specifically for the data mart at www.nrcs.usda.gov. This is a clearing house
 * for many kinds of data including SNOTEL and USGS data.
 * 
 * Here is an example URL:
 *  <a href="https://wcc.sc.egov.usda.gov/reportGenerator/view_csv/customMultiTimeSeriesGroupByStationReport/hourly/id=&quot;806&quot;|name/-31,-7/BATT::value,TOBS::value">Link text</a>
 *
 *
 * 
 * The data source uses a property to specify the interval ("hourly" in the above.)
 * the IDs are retrieved one at a time and supplied by a networklist.
 * The time range ("-31,-7") in the above is expressed as a number of intervals and is calculated
 * from the specified interval property and the routing spec's since &amp; until times.
 * The sensor list (BATT, TOBS) is taken from the platform's configuration. Any sensor with an
 * NRCS sensor will be retrieved.
 * 
 * Properties:
 * 	baseUrl - The base URL for the data mart. d
 *     the default baseUrl is:
 *     <a href="https://wcc.sc.egov.usda.gov/reportGenerator/view_csv/customMultiTimeSeriesGroupByStationReport/"/>
 *  interval - one of "hourly", "daily", "monthly"
 *  dataTypeStandard - default="nrcs". This determines which sensor data types to include in the URL.
 *  
 */
public class NrcsDataSource	extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	// aggregate list of IDs from all network lists.
	private ArrayList<String> aggIds = new ArrayList<String>();
	
	// And aggregate list of Platforms corresponding to the IDs above
	private ArrayList<Platform> platforms = new ArrayList<Platform>();
	
	// retrieved from property
	private String baseUrl = 
		"https://wcc.sc.egov.usda.gov/reportGenerator/view_csv/customMultiTimeSeriesGroupByStationReport/";

	/** one of hourly, daily, monthly */
	private String interval = "hourly";
	
	private String dataTypeStandard = "nrcs";
	
	private Properties myProps = new Properties();
	
	private int sinceInc = -1, untilInc = 0;
	
	private WebDataSource currentWebDs = null;
	private int xportIdx = 0;
	private int urlsGenerated = 0;
	private String currentMediumId = null;
	
	private static final PropertySpec[] UTprops =
	{
		new PropertySpec("baseUrl", PropertySpec.STRING, 
			"Base URL to the NRCS data mart"),
		new PropertySpec("interval", PropertySpec.STRING, 
			"Interval of data to retrieve"),
		new PropertySpec("dataTypeStandard", PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"To select which sensor data type to use in the URL, default=nrcs")
	};

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds data source
	 * @param db database
	 */
	public NrcsDataSource(DataSource ds, Database db)
	{
		super(ds,db);
	}

	/**
	 * Re-evaluate the abstract URL with the next medium ID in the aggregate list.
	 */
	private String buildNextWebAddr()
		throws DataSourceException
	{
		// Processed all DCPs in the netlists and at least one URL was generated.
		if (xportIdx >= aggIds.size())
			return null;
		
		currentMediumId = aggIds.get(xportIdx);
		Platform p = platforms.get(xportIdx);
		xportIdx++;
		
		if (p == null)
		{
			log.warn("No platform for transport ID '{}' -- skipped.", currentMediumId);
			return buildNextWebAddr();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(baseUrl);
		sb.append(interval + "/id=\"");
		sb.append(currentMediumId);
		sb.append("\"|name/" + sinceInc + "," + untilInc + "/");
		

		// add list of comma-separated sensors:   datatype::value
		int numElements = 0;
		for(ConfigSensor cs : p.getConfig().getSensorVec())
		{
			int sensNum = cs.sensorNumber;
			PlatformSensor ps = p.getPlatformSensor(sensNum);

			// Skip sensor if omit property == true
			String s = cs.getProperty("omit");
			if (ps != null && ps.getProperty("omit") != null)
				s = ps.getProperty("omit");
			if (TextUtil.str2boolean(s))
			{
				log.debug("omit=true for sensor {}", sensNum);
				continue;
			}
			
			DataType dt = cs.getDataType(dataTypeStandard);
			if (dt == null)
			{
				log.warn("Trans id '{}' sensor {} has no {} data type -- skipping.",
				         currentMediumId, sensNum, dataTypeStandard);
				continue;
			}
			if (numElements++ > 0)
				sb.append(",");
			sb.append(dt.getCode() + "::value");
		}
		
		urlsGenerated++;
		return sb.toString();
	}

	@Override
	public void processDataSource()
	{
		PropertiesUtil.copyProps(myProps, getDataSource().getArguments());
	}

	@Override
	public void init(Properties rsProps, String since, 
			String until, Vector<NetworkList> netlists) 
		throws DataSourceException
	{
		log.info("initializing ...");
		PropertiesUtil.copyProps(myProps, rsProps);

		String s = PropertiesUtil.getIgnoreCase(myProps, "baseUrl");
		if (s != null)
			baseUrl = s;
		s = PropertiesUtil.getIgnoreCase(myProps, "interval");
		if (s != null)
			interval = s;
		s = PropertiesUtil.getIgnoreCase(myProps, "dataTypeStandard");
		if (s != null)
			dataTypeStandard = s;
		
		int calConst = Calendar.HOUR_OF_DAY;
		if (interval.equalsIgnoreCase("daily"))
			calConst = Calendar.DAY_OF_YEAR;
		else if (interval.equalsIgnoreCase("monthly"))
			calConst = Calendar.MONTH;
		
		// Default since time to 7 days. Convert to a number of intervals
		Date dSince = since != null ? IDateFormat.parse(since) : 
			new Date(System.currentTimeMillis() - 3600000L * 24 * 7);

		Calendar cal = Calendar.getInstance();
		Date now = new Date();
		cal.setTime(now);
		for(sinceInc = 0; dSince.before(cal.getTime()); sinceInc--, cal.add(calConst, -1));
		
		// Default until time to now.
		cal.setTime(now);
		Date dUntil = until != null ? IDateFormat.parse(until) : new Date();
		if (until.equalsIgnoreCase("now"))
			untilInc = 0;
		else
		{
			for(untilInc = 0; 
				dUntil.before(cal.getTime()); 
				untilInc--, cal.add(calConst,  -1));
		}
		log.info(" since={}, sinceInc={}, until={}, untilInc={}", dSince, sinceInc, dUntil, untilInc);
		
		aggIds.clear();
		platforms.clear();
		if (netlists != null)
			for(NetworkList nl : netlists)
			{
				for (NetworkListEntry nle : nl.values())
					if (!aggIds.contains(nle.getTransportId()))
					{
						String tid = nle.getTransportId();
						// Same ID might be in multiple lists. Guard against dups.
						aggIds.add(tid);
						try
						{
							// will be null placeholder if platform doesn't exist in the db.
							platforms.add(nl.getDatabase().platformList.getPlatform(
								nl.transportMediumType, tid));
						}
						catch (DatabaseException ex)
						{
							String msg = "Cannot search database for platform '" + tid + "'";
							throw new DataSourceException(msg,ex);
						}
					}
			}
		
		if (aggIds.size() == 0)
		{
			log.info("init() No medium ids. Will only execute once.");
		}
		xportIdx = 0;
		urlsGenerated = 0;

		try
		{
			DataSource dsrec = new DataSource("absWebReader", "web");
			currentWebDs = (WebDataSource)dsrec.makeDelegate();
			currentWebDs.processDataSource();
			currentWebDs.setAllowNullPlatform(this.getAllowNullPlatform());
		}
		catch(InvalidDatabaseException ex) 
		{
			throw new DataSourceException("Unable to create WebData Source", ex);
		}
	}
	
	@Override
	public void close()
	{
		if (currentWebDs != null)
			currentWebDs.close();
		currentWebDs = null;
	}

	@Override
	protected RawMessage getSourceRawMessage() 
		throws DataSourceException
	{
		if (currentWebDs.isOpen())
		{
			try { return currentWebDs.getRawMessage(); }
			catch(DataSourceEndException ex)
			{
				log.info("End of '{}'.", currentWebDs.getActiveSource());
			}
		}

		String url;
		while((url = buildNextWebAddr()) != null)
		{
			log.debug("Next url '{}'", url);
			myProps.setProperty("url", url);
			myProps.setProperty("mediumid", currentMediumId);
			try
			{
				currentWebDs.init(myProps, "", "", null);
				RawMessage ret = currentWebDs.getSourceRawMessage();
				return ret;
			}
			catch(Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot open '{}'.", url);
			}
		}
		// No more medium IDs
		throw new DataSourceEndException("" + aggIds.size() + " medium IDs processed.");
	}
	
	@Override
	public PropertySpec[] getSupportedProps()
	{
		return PropertiesUtil.combineSpecs(super.getSupportedProps(), 
			PropertiesUtil.combineSpecs(UTprops, StreamDataSource.SDSprops));
	}
	
	@Override
	public boolean supportsTimeRanges()
	{
		return true;
	}

}
