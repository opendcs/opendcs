/*
 * Opens source software by Cove Software, LLC.
 */
package decodes.datasource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;


import ilex.util.IDateFormat;
import ilex.util.Logger;
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
public class UsgsWebDataSource
	extends DataSourceExec
{
	private String module = "UsgsWebDataSource";
	
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
			log(Logger.E_WARNING, module + " No platform for transport ID '" 
				+ currentMediumId + "' -- skipped.");
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
			log(Logger.E_WARNING, "Medium ID '" + currentMediumId + "' is not a valid USGS site number. "
				+ "Will attempt to use USGS Site Name from site record.");
			
			Site site = p.getSite();
			if (site == null)
			{
				log(Logger.E_WARNING, module + " Platform for transport ID '" 
					+ currentMediumId + "' has no site record -- skipped.");
				return buildNextWebAddr();
			}
			SiteName sn = site.getName(Constants.snt_USGS);
			if (sn == null)
			{
				log(Logger.E_WARNING, module + " Platform for transport ID '" 
					+ currentMediumId + "' has no USGS site name -- will try medium ID.");
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
				log(Logger.E_DEBUG1, module + " omit=true for sensor " + sensNum);
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
					log(Logger.E_INFORMATION, module + " trans id '" + currentMediumId 
						+ "' sensor " + sensNum
						+ " has no " + dataTypeStandard + " data type -- skipping.");
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
		log(Logger.E_INFORMATION, module + " initializing ...");
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
		log(Logger.E_INFORMATION, module + " since=" + dSince + ", until=" + dUntil);
		
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
						catch (DatabaseException e)
						{
							String msg = "Cannot search database for platform '" + tid + "': " + e;
							log(Logger.E_WARNING, module + " " + msg);
							throw new DataSourceException(msg);
						}
					}
			}
		
		if (aggIds.size() == 0)
		{
			String msg = module + " init() No medium ids.";
			log(Logger.E_WARNING, msg);
			throw new DataSourceException(msg);
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
			log(Logger.E_INFORMATION, module + " " + ex);
			throw new DataSourceException(module + " " + ex);
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
	public RawMessage getRawMessage() 
		throws DataSourceException
	{
		if (currentWebDs.isOpen())
		{
			try { return currentWebDs.getRawMessage(); }
			catch(DataSourceEndException ex)
			{
				log(Logger.E_INFORMATION, module
					+ " end of '" + currentWebDs.getActiveSource() + "'");
			}
		}

		String url;
		while((url = buildNextWebAddr()) != null)
		{
			log(Logger.E_DEBUG1, module + " next url '" + url + "'");
			myProps.setProperty("url", url);
			myProps.setProperty("mediumid", currentMediumId);
			try
			{
				currentWebDs.init(myProps, "", "", null);
				RawMessage ret = currentWebDs.getRawMessage();
				return ret;
			}
			catch(DataSourceException ex)
			{
				String msg = module + " cannot open '"
					+ url + "': " + ex;
				log(Logger.E_WARNING, msg);
			}
			catch(Exception ex)
			{
				String msg = module + " cannot open '"
					+ url + "': " + ex;
				log(Logger.E_WARNING, msg);
				System.err.println(msg);
				ex.printStackTrace(System.err);
			}
		}
		// No more medium IDs
		throw new DataSourceEndException(module 
			+ " " + aggIds.size() + " medium IDs processed.");
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
