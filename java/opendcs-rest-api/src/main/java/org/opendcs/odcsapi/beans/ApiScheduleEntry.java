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

@Schema(description = "Represents a schedule entry, including its related application details, routing specification, and timing information.")
public final class ApiScheduleEntry
{
	@Schema(description = "Unique numeric identifier of the schedule entry.", example = "14")
	private Long schedEntryId = null;
	@Schema(description = "Name of the schedule entry.", example = "something-else")
	private String name = null;
	@Schema(description = "Unique numeric identifier of the associated application.", example = "1234")
	private Long appId = null;
	@Schema(description = "Name of the associated application.", example = "RoutingScheduler")
	private String appName = null;
	@Schema(description = "Unique numeric identifier of the associated routing specification.", example = "5678")
	private Long routingSpecId = null;
	@Schema(description = "Name of the associated routing specification.", example = "goes1")
	private String routingSpecName = null;
	@Schema(description = "Flag indicating whether the schedule entry is enabled.", example = "true")
	private boolean enabled = false;
	@Schema(description = "Timestamp of the last modification.", example = "2022-03-28T18:02:18.659Z[UTC]")
	private Date lastModified = null;
	@Schema(description = "Start time of the schedule entry.", example = "2022-03-28T18:02:13.659Z[UTC]")
	private Date startTime = null;
	@Schema(description = "Time zone associated with the schedule entry.", example = "America/New_York")
	private String timeZone = null;
	@Schema(description = "Running interval for the schedule entry.", example = "15 Minutes")
	private String runInterval = null;

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
	public Long getAppId()
	{
		return appId;
	}
	public void setAppId(Long appId)
	{
		this.appId = appId;
	}
	public Long getRoutingSpecId()
	{
		return routingSpecId;
	}
	public void setRoutingSpecId(Long routingSpecId)
	{
		this.routingSpecId = routingSpecId;
	}
	public Date getStartTime()
	{
		return startTime;
	}
	public void setStartTime(Date startTime)
	{
		this.startTime = startTime;
	}
	public String getTimeZone()
	{
		return timeZone;
	}
	public void setTimeZone(String timeZone)
	{
		this.timeZone = timeZone;
	}
	public String getRunInterval()
	{
		return runInterval;
	}
	public void setRunInterval(String runInterval)
	{
		this.runInterval = runInterval;
	}


}
