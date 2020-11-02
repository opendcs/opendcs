package lrgs.db;

/**
 * 
 * This encapsulates information about LRGS Data Sources.
 * This object contains the data source id, data source 
 * type and data source name. 
 *
 */
public class DataSource 
	implements LrgsDatabaseObject
{
	private int dataSourceId;
	private String lrgsHost;
	private String dataSourceType;
	private String dataSourceName;
	
	/**
	 *  Constructor.
	 *  Initialize all private variables.
	 */
	public DataSource()
	{
		dataSourceId = 0;
		lrgsHost = null;
		dataSourceType = null;
		dataSourceName = null;
	}

	/**
	 *  Construct a Data Source Object from a data source id, data source name 
	 *  and a data source type.
	 *  
	 *  @param dataSourceId the unique ID for the data source
	 *  @param name the name of this DataSource
	 *  @param type the type of this DataSource
	 */
	public DataSource(int dataSourceId, String lrgsHost, String name, String type)
	{
		this();
		this.dataSourceId = dataSourceId;
		this.lrgsHost = lrgsHost;
		this.dataSourceName = name;
		this.dataSourceType = type;
	}
	
	/**
	 *  This method returns the Data source name.
	 *   
	 *  @return dataSourceName the name of this Data Source 
	 */
	public String getDataSourceName() 
	{
		return dataSourceName;
	}
	
	/**
	 * This method sets the Data source name.
	 * 
	 * @param dataSourceName the name of this Data Source
	 */
	public void setDataSourceName(String dataSourceName) 
	{
		this.dataSourceName = dataSourceName;
	}
	
	/**
	 *  This method returns the Data source type.
	 *   
	 *  @return dataSourceType the type of this Data Source 
	 */
	public String getDataSourceType() 
	{
		return dataSourceType;
	}
	
	/**
	 *  This method sets the Data source type.
	 * 
	 * @param dataSourceType the type of this Data Source 
	 */
	public void setDataSourceType(String dataSourceType) 
	{
		this.dataSourceType = dataSourceType;
	}
	
	/**
	 *  This method returns the Data source Id.
	 *   
	 *  @return dataSourceId of this Data Source 
	 */
	public int getDataSourceId() 
	{
		return dataSourceId;
	}
	
	/**
	 *  This method sets the Data source Id.
	 * 
	 * @param dataSourceId the Id of this Data Source 
	 */
	public void setDataSourceId(int dataSourceId) 
	{
		this.dataSourceId = dataSourceId;
	}

	public String getLrgsHost()
	{
		return lrgsHost;
	}

	public void setLrgsHost(String lrgsHost)
	{
		this.lrgsHost = lrgsHost;
	}
}
