/*
 * Opens source software by Cove Software, LLC.
 */
package decodes.datasource;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import decodes.db.DataSource;
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
	
	private static final PropertySpec[] UTprops =
	{
		new PropertySpec("abstractUrl", PropertySpec.STRING, 
			"(default=null) Abstract URL containing $MEDIUM_ID.")
	};

	// No arg ctor required to instantiate from class name
	public WebAbstractDataSource() {}

	/**
	 * Re-evaluate the abstract URL with the next medium ID in the aggregate list.
	 */
	private String buildNextWebAddr()
		throws DataSourceException
	{
		if (xportIdx >= aggIds.size())
			return null;

		Date now = new Date();
		myProps.setProperty("MEDIUMID", aggIds.get(xportIdx++));
		return EnvExpander.expand(abstractUrl, myProps, now);
	}

	@Override
	public void processDataSource()
	{
		PropertiesUtil.copyProps(myProps, 
			getDataSource().getArguments());
	}

	@Override
	public void initDataSource(Properties rsProps, String since, 
			String until, Vector<NetworkList> netlists) 
		throws DataSourceException
	{
		Logger.instance().info(module + " initializing ...");
		PropertiesUtil.copyProps(myProps, rsProps);

		if ((abstractUrl = PropertiesUtil.getIgnoreCase(myProps, "AbstractUrl")) == null)
			throw new DataSourceException(module 
				+ " Missing required property 'AbstractUrl'!");

		rsSince = since;
		rsUntil = until;
		
		if (netlists != null)
			for(NetworkList nl : netlists)
			{
				for (NetworkListEntry nle : nl.values())
					if (!aggIds.contains(nle.getTransportId()))
						aggIds.add(nle.getTransportId());
			}
		
		if (aggIds.size() == 0)
		{
			String msg = module + " init() No medium ids. Did you forget to " +
				"supply a network list?";
			Logger.instance().failure(msg);
			throw new DataSourceException(msg);
		}
		xportIdx = 0;

		try
		{
			DataSource dsrec = new DataSource("absWebReader", "web");
			currentWebDs = (WebDataSource)dsrec.makeDelegate();
			currentWebDs.processDataSource(); 
		}
		catch(InvalidDatabaseException ex) 
		{
			Logger.instance().failure(module + " " + ex);
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
				Logger.instance().info(module
					+ " end of '" + currentWebDs.getActiveSource() + "'");
			}
		}

		String url;
		while((url = buildNextWebAddr()) != null)
		{
			myProps.setProperty("url", url);
			try
			{
				currentWebDs.initDataSource(myProps, rsSince, rsUntil, null);
				return currentWebDs.getRawMessage();
			}
			catch(Exception ex)
			{
				String msg = module + " cannot open '"
					+ url + "': " + ex;
				Logger.instance().warning(msg);
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
}
