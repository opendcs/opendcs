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

@Schema(description = "Represents a schedule entry reference, including metadata such as name, application, routing specification, and enabled status.")
public final class ApiScheduleEntryRef
{
	@Schema(description = "The unique numeric identifier for the schedule entry.", example = "86")
	private Long schedEntryId = null;

	@Schema(description = "The name of the schedule entry.", example = "goes2")
	private String name = null;

	@Schema(description = "The name of the application associated with this schedule entry.", example = "RoutingScheduler")
	private String appName = null;

	@Schema(description = "The routing specification name for this schedule entry.", example = "goes2")
	private String routingSpecName = null;

	@Schema(description = "Indicates whether the schedule entry is enabled.", example = "true")
	private boolean enabled = false;

	@Schema(description = "The last modification date of the schedule entry.", example = "2023-10-05T12:00:00.000[UTC]")
	private Date lastModified = null;

	public Long getSchedEntryId()
	{
		return schedEntryId;
	}

	public void setSchedEntryId(Long schedEntryId)
	{
		this.schedEntryId = schedEntryId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public String getRoutingSpecName()
	{
		return routingSpecName;
	}

	public void setRoutingSpecName(String routingSpecName)
	{
		this.routingSpecName = routingSpecName;
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

}
