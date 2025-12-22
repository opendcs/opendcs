/*
* Opens source software by Cove Software, LLC.
*
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
package decodes.datasource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.util.PropertySpec;


/**
 * This class is modeled on the generic WebAbstractDataSource. It is designed
 * specifically for USGS data.
 * 
 * Here is an example URL:
 * <a href="https://waterservices.usgs.gov/nwis/iv/?format=rdb,1.0&amp;sites=01646500&amp;startDT=2020-10-11T12:00-0400&amp;endDT=2020-10-12T08:00-0400&amp;parameterCd=00060,00065">USGS RDB Link</a>
 * 
 * The "sites" argument provides a USGS Site Number. The data source gets the platform record from
 * the network list, and then the site record from the platform record, and the USGS site name from the
 * site record.
 * 
 * startDT and endDT are taken from the since/until time provided to the routing spec. If until
 * is missing (i.e. real-time), then it is set to now. This DataSource cannot be a real-time data
 * source, it is intended to run periodically on a schedule.
 * 
 * parameterCd is a comma-separated list of USGS data types. The data source takes this from the
 * Configuration record associated with the platform. The list will include all sensors that have
 * a USGS data type assigned. Any sensors that have a property "omit" set to "true" will be skipped.
 * 
 * Properties:
 * 	baseUrl - The base URL for USGS data.
 *  dataTypeStandard - default="usgs". This determines which sensor data types to include in the URL.
 */
public class UsgsWebDataSource extends DataSourceExec
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	
	// aggregate list of IDs from all network lists.
	private ArrayList<String> aggIds = new ArrayList<String>();
	
	// And aggregate list of Platforms corresponding to the IDs above
	private ArrayList<Platform> platforms = new ArrayList<Platform>();
	
	// retrieved from property
	private String baseUrl = 
		"https://waterservices.usgs.gov/nwis/iv/?format=rdb&";

	private String dataTypeStandard = "usgs";
	
	private Properties myProps = new Properties();
	
	Date dSince = null, dUntil = null;
	
	private WebDataSource currentWebDs = null;
	private int xportIdx = 0;
	private int urlsGenerated = 0;
	private String currentMediumId = null;
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmXX");
	
	private static final PropertySpec[] UTprops =
	{
		new PropertySpec("baseUrl", PropertySpec.STRING, 
			"Base URL to the USGS NWIS"),
		new PropertySpec("dataTypeStandard", PropertySpec.DECODES_ENUM + Constants.enum_DataTypeStd,
			"To select which sensor data type to use in the URL, default=usgs")
	};

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param ds
	 * @param db
	 */
	public UsgsWebDataSource(DataSource ds, Database db)
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
			log.warn(" No platform for transport ID '{}' -- skipped.", currentMediumId);
			return buildNextWebAddr();
		}
		
		// If the medium ID is all digits, assume it's a USGS site number and use it directly.
		boolean isAllDigits = true;
		String siteNum = currentMediumId;
		for(int idx = 0; idx < siteNum.length(); idx++)
			if (!Character.isDigit(siteNum.charAt(idx)))
			{
				isAllDigits = false;
				break;
			}
		if (!isAllDigits)
		{
			log.warn("Medium ID '{}' is not a valid USGS site number. " +
			         "Will attempt to use USGS Site Name from site record.",
				     currentMediumId);
			
			Site site = p.getSite();
			if (site == null)
			{
				log.warn("Platform for transport ID '{}' has no site record -- skipped.", currentMediumId);
				return buildNextWebAddr();
			}
			SiteName sn = site.getName(Constants.snt_USGS);
			if (sn == null)
			{
				log.warn("Platform for transport ID '{}' has no USGS site name -- will try medium ID.",
						 currentMediumId);
			}
			else
				siteNum = sn.getNameValue();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(baseUrl);
		
		// Add the USGS site ID.
		sb.append("sites=" + siteNum + "&");

		// Add time range
		sb.append("startDT=" + sdf.format(dSince) + "&endDT=" + sdf.format(dUntil));
		
		// add list of comma-separated USGS data type codes
		sb.append("&parameterCd=");
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
				// USGS and EPA data types are equivalent.
				if (dataTypeStandard.equalsIgnoreCase(Constants.datatype_USGS)
				 || dataTypeStandard.equalsIgnoreCase("usgs"))
					dt = cs.getDataType(Constants.datatype_EPA);
				else if (dataTypeStandard.equalsIgnoreCase(Constants.datatype_EPA))
				{
					dt = cs.getDataType(Constants.datatype_USGS);
					if (dt == null)
						dt = cs.getDataType("usgs");
				}
				if (dt == null)
				{
					log.info("trans id '{}' sensor {} has no {} data type -- skipping.",
							 currentMediumId, sensNum, dataTypeStandard);
					continue;
				}
			}
			if (numElements++ > 0)
				sb.append(",");
			sb.append(dt.getCode());
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
		log.info("Initializing ...");
		PropertiesUtil.copyProps(myProps, rsProps);
		
		if (routingSpecThread.getRoutingSpec().outputTimeZone != null)
			sdf.setTimeZone(routingSpecThread.getRoutingSpec().outputTimeZone);

		String s = PropertiesUtil.getIgnoreCase(myProps, "baseUrl");
		if (s != null)
			baseUrl = s;
		
		s = PropertiesUtil.getIgnoreCase(myProps, "dataTypeStandard");
		if (s != null)
			dataTypeStandard = s;
		
		// Default since time to 1 day.
		dSince = since != null ? IDateFormat.parse(since) : 
			new Date(System.currentTimeMillis() - 3600000L * 24);

		dUntil = until != null ? IDateFormat.parse(until) : new Date();
		log.info("since={}, until={}", dSince, dUntil);
		
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
							String msg = "Cannot search database for platform '" + tid;
							throw new DataSourceException(msg,ex);
						}
					}
			}
		
		if (aggIds.size() == 0)
		{
			throw new DataSourceException("init() No medium ids.");
		}
		xportIdx = 0;
		urlsGenerated = 0;

		// Make the web data source that will do the actual IO. In the getRawMessage
		// loop we re-initialize it for each URL that we build.
		try
		{
			DataSource dsrec = new DataSource("absWebReader", "web");
			currentWebDs = (WebDataSource)dsrec.makeDelegate();
			currentWebDs.processDataSource();
			currentWebDs.setAllowNullPlatform(this.getAllowNullPlatform());
		}
		catch(InvalidDatabaseException ex) 
		{
			throw new DataSourceException("Unable to create WebDataSource to retrieve USGS Data", ex);
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
			try { return currentWebDs.getSourceRawMessage(); }
			catch(DataSourceEndException ex)
			{
				log.info("End of '{}'", currentWebDs.getActiveSource());
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
				log.atWarn().setCause(ex).log("Unable to Retrieve next raw from '" + url + "'", ex);
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
