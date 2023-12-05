package org.opendcs.odcsapi.beans;

/**
 * represents a member of a group-type data source
 */
public class ApiDataSourceGroupMember
{
	private Long dataSourceId = null;
	
	private String dataSourceName = null;

	public Long getDataSourceId()
	{
		return dataSourceId;
	}

	public void setDataSourceId(Long dataSourceId)
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
