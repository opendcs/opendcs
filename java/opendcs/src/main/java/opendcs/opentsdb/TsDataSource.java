package opendcs.opentsdb;

import decodes.sql.DbKey;

/**
 * Holds info about a time series data source.
 * Hashable by unique tupple (appId, module)
 */
public class TsDataSource
{
	private DbKey sourceId = DbKey.NullKey;
	private DbKey appId = DbKey.NullKey;
	private String appModule = null;
	private String appName = null;
	

	public TsDataSource(DbKey sourceId, DbKey appId, String appModule)
	{
		super();
		this.sourceId = sourceId;
		this.appId = appId;
		this.appModule = appModule;
	}

	public DbKey getSourceId()
	{
		return sourceId;
	}


	public void setSourceId(DbKey sourceId)
	{
		this.sourceId = sourceId;
	}


	public DbKey getAppId()
	{
		return appId;
	}


	public void setAppId(DbKey appId)
	{
		this.appId = appId;
	}


	public String getAppModule()
	{
		return appModule;
	}


	public void setAppModule(String appModule)
	{
		this.appModule = appModule;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}
}
