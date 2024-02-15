/*
 * Opens source software by Cove Software, LLC.
 */
package decodes.datasource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;

import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.InvalidDatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.util.PropertySpec;


/**
 * The data source name is abstract. It can contain variables
 * like $MEDIUMID, which are evaluated over the contents of a network lists.
 * For each medium ID in all the lists, substitute the ID for the variable
 * in the URL. then construct an UrlDataSource to retrieve the data.
 * 
 * Properties:
 * 	abstractUrl - The URL containing $MEDIUMID or ${MEDIUMID} variable.
 */
public class WebAbstractDataSource
	extends DataSourceExec
{
	private String module = "WebAbstractDataSource";
	
	// aggregate list of IDs from all network lists.
	private ArrayList<String> aggIds = new ArrayList<String>();
	
	// retrieved from property
	private String abstractUrl = null;

	private Properties myProps = new Properties();
	
	// Time range from the routing spec:
	String rsSince = null;
	String rsUntil = null;

	private WebDataSource currentWebDs = null;
	private int xportIdx = 0;
	private int urlsGenerated = 0;
	private static String dfltSinceFmt = "yyyyMMdd-HHmm";
	private SimpleDateFormat sinceFormat = new SimpleDateFormat(dfltSinceFmt);
	private TimeZone sinceTimeZone = TimeZone.getTimeZone("UTC");
	
	private static final PropertySpec[] UTprops =
	{
		new PropertySpec("abstractUrl", PropertySpec.STRING, 
			"(default=null) Abstract URL containing $MEDIUMID, $SINCE,"
			+ " and $UNTIL."),
		new PropertySpec("sinceFormat", PropertySpec.STRING, 
			"(default=" + dfltSinceFmt + ") Specifies how to format $SINCE and $UNTIL"
			+ " if they are present in the abstract URL."),
		new PropertySpec("sinceTimeZone", PropertySpec.TIMEZONE, 
			"(default=UTC) Used to format $SINCE and $UNTIL"
			+ " if they are present in the abstract URL."),
	};

	/**
	 * @see decodes.datasource.DataSourceExec#DataSourceExec(DataSource, Database) DataSourceExec Constructor
	 *
	 * @param dataSource
	 * @param decodesDatabase
	 */
	public WebAbstractDataSource(DataSource ds, Database db)
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
		if (xportIdx >= aggIds.size() && urlsGenerated > 0)
			return null;

		Date now = new Date();
		if (xportIdx < aggIds.size())
			myProps.setProperty("MEDIUMID", aggIds.get(xportIdx++));
		
		urlsGenerated++;
		return EnvExpander.expand(abstractUrl, myProps, now);
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

		if ((abstractUrl = PropertiesUtil.getIgnoreCase(myProps, "AbstractUrl")) == null)
			throw new DataSourceException(module 
				+ " Missing required property 'AbstractUrl'!");

		String s = myProps.getProperty("sinceFormat");
		if (s != null && s.trim().length() > 0)
			sinceFormat = new SimpleDateFormat(s);
		s = myProps.getProperty("sinceTimeZone");
		if (s != null && s.trim().length() > 0)
			sinceTimeZone = TimeZone.getTimeZone(s);
		sinceFormat.setTimeZone(sinceTimeZone);
			
		rsSince = since;
		rsUntil = until;

		// The URL is allowed to contain $SINCE and/or $UNTIL.
		// Evaluate these strings according to the routing spec and format
		// them in the specified way.
		if (rsSince != null && rsSince.trim().length() > 0
		 && abstractUrl.toUpperCase().contains("$SINCE"))
		{
			try
			{
				Date sinceDate = IDateFormat.parse(rsSince);
				myProps.setProperty("SINCE", sinceFormat.format(sinceDate));
			}
			catch(Exception ex)
			{
				throw new DataSourceException("Bad Since Time: " + ex.getMessage());
			}
		}
		if (rsUntil != null && rsUntil.trim().length() > 0
		 && abstractUrl.toUpperCase().contains("$UNTIL"))
		{
			try
			{
				Date untilDate = IDateFormat.parse(rsUntil);
				myProps.setProperty("UNTIL", sinceFormat.format(untilDate));
			}
			catch(Exception ex)
			{
				throw new DataSourceException("Bad Until Time: " + ex.getMessage());
			}
		}
		
		if (netlists != null)
			for(NetworkList nl : netlists)
			{
				for (NetworkListEntry nle : nl.values())
					if (!aggIds.contains(nle.getTransportId()))
						aggIds.add(nle.getTransportId());
			}
		
		if (aggIds.size() == 0)
		{
			String msg = module + " init() No medium ids. Will only execute once.";
			log(Logger.E_INFORMATION, msg);
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
			myProps.setProperty("url", url);
			try
			{
				currentWebDs.init(myProps, rsSince, rsUntil, null);
				return currentWebDs.getRawMessage();
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
