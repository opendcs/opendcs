package org.opendcs.odcsapi.beans;

import java.util.ArrayList;

public class ApiTsGroup
{
	private Long groupId = null;
	private String groupName = null;
	private String groupType = null;
	private String description = null;
	
	private ArrayList<ApiTimeSeriesIdentifier> tsIds = new ArrayList<ApiTimeSeriesIdentifier>();
	private ArrayList<ApiTsGroupRef> includeGroups = new ArrayList<ApiTsGroupRef>();
	private ArrayList<ApiTsGroupRef> excludeGroups = new ArrayList<ApiTsGroupRef>();
	private ArrayList<ApiTsGroupRef> intersectGroups = new ArrayList<ApiTsGroupRef>();
	
	// list of name=value pairs, where name is one of BaseLocation, SubLocation,
	// BaseParam, SubParam, ParamType, Interval, Duration, Version, BaseVersion, SubVersion
	// Interval, Duration, Version
	// NOTE: Location and Param are handled by groupSites, and groupDataTypes below
	private ArrayList<String> groupAttrs = new ArrayList<String>();
	
	// Explicit Location (aka Site) specs:
	private ArrayList<ApiSiteRef> groupSites = new ArrayList<ApiSiteRef>();
	
	// Explicit DataType (aka Param) specs:
	private ArrayList<ApiDataType> groupDataTypes = new ArrayList<ApiDataType>();
	
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
	public ArrayList<ApiTimeSeriesIdentifier> getTsIds()
	{
		return tsIds;
	}
	public ArrayList<ApiTsGroupRef> getIncludeGroups()
	{
		return includeGroups;
	}
	public ArrayList<ApiTsGroupRef> getExcludeGroups()
	{
		return excludeGroups;
	}
	public ArrayList<ApiTsGroupRef> getIntersectGroups()
	{
		return intersectGroups;
	}
	public ArrayList<String> getGroupAttrs()
	{
		return groupAttrs;
	}
	public ArrayList<ApiSiteRef> getGroupSites()
	{
		return groupSites;
	}
	public ArrayList<ApiDataType> getGroupDataTypes()
	{
		return groupDataTypes;
	}

}
