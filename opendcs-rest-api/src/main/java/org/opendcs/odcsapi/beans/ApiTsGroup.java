/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a time series group with metadata like group type, description, and associated data.")
public final class ApiTsGroup
{
	@Schema(description = "The unique numeric identifier of the time series group.", example = "1001")
	private Long groupId = null;

	@Schema(description = "The name of the time series group.", example = "Flow Data Group")
	private String groupName = null;

	@Schema(description = "The type of the group", example = "basin")
	private String groupType = null;

	@Schema(description = "A brief description of the group.", example = "This group contains flow data for river sites.")
	private String description = null;

	@Schema(description = "List of time series identifiers associated with this group.")
	private List<ApiTimeSeriesIdentifier> tsIds = new ArrayList<>();

	@Schema(description = "List of groups that are included in this group.")
	private List<ApiTsGroupRef> includeGroups = new ArrayList<>();

	@Schema(description = "List of groups excluded from this group.")
	private List<ApiTsGroupRef> excludeGroups = new ArrayList<>();

	@Schema(description = "List of groups that intersect with this group.")
	private List<ApiTsGroupRef> intersectGroups = new ArrayList<>();

	// list of name=value pairs, where name is one of BaseLocation, SubLocation,
	// BaseParam, SubParam, ParamType, Interval, Duration, Version, BaseVersion, SubVersion
	// Interval, Duration, Version
	// NOTE: Location and Param are handled by groupSites, and groupDataTypes below
	@Schema(description = "List of name=value pairs representing group attributes: BaseLocation, SubLocation," +
			" BaseParam, SubParam, ParamType, Interval, Duration, Version, BaseVersion, SubVersion," +
			" Interval, Duration, or Version.")
	private List<String> groupAttrs = new ArrayList<>();

	@Schema(description = "Explicit location (site) specifications for the group.")
	private List<ApiSiteRef> groupSites = new ArrayList<>();

	@Schema(description = "Explicit data type (parameter) specifications for the group.")
	private List<ApiDataType> groupDataTypes = new ArrayList<>();

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
	public List<ApiTimeSeriesIdentifier> getTsIds()
	{
		return tsIds;
	}
	public List<ApiTsGroupRef> getIncludeGroups()
	{
		return includeGroups;
	}
	public List<ApiTsGroupRef> getExcludeGroups()
	{
		return excludeGroups;
	}
	public List<ApiTsGroupRef> getIntersectGroups()
	{
		return intersectGroups;
	}
	public List<String> getGroupAttrs()
	{
		return groupAttrs;
	}
	public List<ApiSiteRef> getGroupSites()
	{
		return groupSites;
	}
	public List<ApiDataType> getGroupDataTypes()
	{
		return groupDataTypes;
	}
}
