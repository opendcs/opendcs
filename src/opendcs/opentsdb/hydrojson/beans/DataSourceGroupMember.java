package opendcs.opentsdb.hydrojson.beans;

import decodes.sql.DbKey;

/**
 * represents a member of a group-type data source
 */
public class DataSourceGroupMember
{
	private long dataSourceId = DbKey.NullKey.getValue();
	
	private String dataSourceName = null;

	public long getDataSourceId()
	{
		return dataSourceId;
	}

	public void setDataSourceId(long dataSourceId)
	{
		this.dataSourceId = dataSourceId;
	}

	public String getDataSourceName()
	{
		return dataSourceName;
	}

	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}
	
}
