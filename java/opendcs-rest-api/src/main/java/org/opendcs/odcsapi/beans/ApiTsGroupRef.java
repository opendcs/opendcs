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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a time series group reference, including its ID, name, type, and description.")
public final class ApiTsGroupRef
{
	@Schema(description = "Unique numeric identifier for the time series group.", example = "101")
	private Long groupId = null;

	@Schema(description = "Name of the time series group.", example = "topgroup")
	private String groupName = null;

	@Schema(description = "Type of the time series group", example = "basin")
	private String groupType = null;

	@Schema(description = "Description of the time series group.",
			example = "Group that contains weather-related time series data.")
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
