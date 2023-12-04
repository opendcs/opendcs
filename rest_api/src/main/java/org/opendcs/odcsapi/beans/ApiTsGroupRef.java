package org.opendcs.odcsapi.beans;

public class ApiTsGroupRef
{
	private Long groupId = null;
	private String groupName = null;
	private String groupType = null;
	private String description = null;
	public Long getGroupId()
	{
		return groupId;
	}
	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}
	public String getGroupName()
	{
		return groupName;
	}
	public void setGroupName(String groupName)
	{
		this.groupName = groupName;
	}
	public String getGroupType()
	{
		return groupType;
	}
	public void setGroupType(String groupType)
	{
		this.groupType = groupType;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
}
