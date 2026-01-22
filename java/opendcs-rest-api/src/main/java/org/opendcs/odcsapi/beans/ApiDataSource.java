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
import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a data source including its ID, name, type, properties, usage information, and group membership details.")
public final class ApiDataSource
{
	@Schema(description = "The unique numeric identifier of the data source.", example = "10")
	private Long dataSourceId = null;
	@Schema(description = "The name of the data source.", example = "USGS-Web")
	private String name = null;
	@Schema(description = "The type of the data source.", example = "USGS-backupgroup")
	private String type = null;
	@Schema(description = "The count of entities using this data source.", example = "3")
	private int usedBy = 0;
	@Schema(description = "Configuration properties associated with the data source.")
	private Properties props = null;
	@Schema(description = "A list of group memberships associated with the data source.")
	private List<ApiDataSourceGroupMember> groupMembers = new ArrayList<>();

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
	public List<ApiDataSourceGroupMember> getGroupMembers()
	{
		return groupMembers;
	}
	public void setGroupMembers(List<ApiDataSourceGroupMember> groupMembers)
	{
		this.groupMembers = groupMembers;
	}

}
