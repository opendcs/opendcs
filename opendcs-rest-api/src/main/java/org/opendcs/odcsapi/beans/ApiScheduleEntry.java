/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

public class ApiScheduleEntry
{
	private Long schedEntryId = null;
	private String name = null;
	private Long appId = null;
	private String appName = null;
	private Long routingSpecId = null;
	private String routingSpecName = null;
	private boolean enabled = false;
	private Date lastModified = null;
	private Date startTime = null;
	private String timeZone = null;
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
