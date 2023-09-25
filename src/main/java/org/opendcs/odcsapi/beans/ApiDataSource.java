package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Properties;

public class ApiDataSource
{
	private Long dataSourceId = null;
	private String name = null;
	private String type = null;
	private int usedBy = 0;
	private Properties props = null;
	private ArrayList<ApiDataSourceGroupMember> groupMembers = new ArrayList<ApiDataSourceGroupMember>();
	
	public Long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(Long dataSourceId)
	{
		this.dataSourceId = dataSourceId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getType()
	{
		return type;
	}
	public void setType(String type)
	{
		this.type = type;
	}
	public int getUsedBy()
	{
		return usedBy;
	}
	public void setUsedBy(int usedBy)
	{
		this.usedBy = usedBy;
	}
	public Properties getProps()
	{
		return props;
	}
	public void setProps(Properties props)
	{
		this.props = props;
	}
	public ArrayList<ApiDataSourceGroupMember> getGroupMembers()
	{
		return groupMembers;
	}
	public void setGroupMembers(ArrayList<ApiDataSourceGroupMember> groupMembers)
	{
		this.groupMembers = groupMembers;
	}

}
