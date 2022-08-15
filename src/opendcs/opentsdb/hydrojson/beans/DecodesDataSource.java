package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;
import java.util.Properties;

import decodes.sql.DbKey;

public class DecodesDataSource
{
	private long dataSourceId = DbKey.NullKey.getValue();
	private String name = null;
	private String type = null;
	private int usedBy = 0;
	private Properties props = null;
	private ArrayList<DataSourceGroupMember> groupMembers = new ArrayList<DataSourceGroupMember>();
	
	public long getDataSourceId()
	{
		return dataSourceId;
	}
	public void setDataSourceId(long dataSourceId)
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
	public ArrayList<DataSourceGroupMember> getGroupMembers()
	{
		return groupMembers;
	}
	public void setGroupMembers(ArrayList<DataSourceGroupMember> groupMembers)
	{
		this.groupMembers = groupMembers;
	}

}
