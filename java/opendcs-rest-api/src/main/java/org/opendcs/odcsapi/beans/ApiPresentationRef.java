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

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a presentation reference consisting of metadata such as group information, inheritance details, and production status.")
public final class ApiPresentationRef
{
	@Schema(description = "The unique numeric identifier for the group.", example = "1")
	private Long groupId;

	@Schema(description = "The name of the presentation reference.", example = "CWMS-English")
	private String name = null;

	@Schema(description = "Indicates the name of the parent reference that this reference inherits from, if any.",
			example = "SHEF-English")
	private String inheritsFrom = null;

	@Schema(description = "The unique numeric identifier of the parent reference that this inherits from.", example = "17")
	private transient Long inheritsFromId = null;

	@Schema(description = "The timestamp of the last modification to this reference.", example = "2025-01-01T00:00:00.000[UTC]")
	private Date lastModified = null;

	@Schema(description = "Indicates whether this presentation reference is in production.", example = "false")
	private boolean isProduction = false;

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

	public Long getInheritsFromId()
	{
		return inheritsFromId;
	}

	public void setInheritsFromId(Long inheritsFromId)
	{
		this.inheritsFromId = inheritsFromId;
	}
	
}
