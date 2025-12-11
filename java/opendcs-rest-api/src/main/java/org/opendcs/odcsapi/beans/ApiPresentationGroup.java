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
import java.util.Date;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a presentation group that can inherit properties from a parent group.")
public final class ApiPresentationGroup
{
	@Schema(description = "Unique numeric identifier of the presentation group.", example = "8153")
	private Long groupId = null;

	@Schema(description = "Name of the presentation group.", example = "SHEF-English")
	private String name = null;

	@Schema(description = "Name of the group this presentation group inherits from.", example = "CWMS-English")
	private String inheritsFrom = null;

	@Schema(description = "Unique numeric identifier of the parent group this presentation group inherits from.", example = "152")
	private Long inheritsFromId = null;

	@Schema(description = "Date when the presentation group was last modified.",
			example = "2025-01-01T00:00:00.000[UTC]")
	private Date lastModified = null;

	@Schema(description = "Flag to indicate if this group is a production group.", example = "false")
	private boolean isProduction = false;

	@Schema(description = "List of presentation elements that belong to this group.")
	private List<ApiPresentationElement> elements = new ArrayList<>();

	public Long getGroupId()
	{
		return groupId;
	}

	public void setGroupId(Long groupId)
	{
		this.groupId = groupId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getInheritsFrom()
	{
		return inheritsFrom;
	}

	public void setInheritsFrom(String inheritsFrom)
	{
		this.inheritsFrom = inheritsFrom;
	}

	public Long getInheritsFromId()
	{
		return inheritsFromId;
	}

	public void setInheritsFromId(Long inheritsFromId)
	{
		this.inheritsFromId = inheritsFromId;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

	public boolean isProduction()
	{
		return isProduction;
	}

	public void setProduction(boolean isProduction)
	{
		this.isProduction = isProduction;
	}

	public List<ApiPresentationElement> getElements()
	{
		return elements;
	}

	public void setElements(List<ApiPresentationElement> elements)
	{
		this.elements = elements;
	}
}
