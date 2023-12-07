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

public class ApiAppStatus
{
	private Long appId = null;
	private String appName = null;
	private String appType = null;
	private String hostname = null;
	private Long pid = null;
	private Date heartbeat = null;
	private Integer eventPort = null;
	private String status = "Inactive";
	public Long getAppId()
	{
		return appId;
	}
	public void setAppId(Long appId)
	{
		this.appId = appId;
	}
	public Long getPid()
	{
		return pid;
	}
	public void setPid(Long pid)
	{
		this.pid = pid;
	}
	public String getHostname()
	{
		return hostname;
	}
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
	public Date getHeartbeat()
	{
		return heartbeat;
	}
	public void setHeartbeat(Date heartbeat)
	{
		this.heartbeat = heartbeat;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public String getAppName()
	{
		return appName;
	}
	public void setAppName(String appName)
	{
		this.appName = appName;
	}
	public String getAppType()
	{
		return appType;
	}
	public void setAppType(String appType)
	{
		this.appType = appType;
	}
	public Integer getEventPort()
	{
		return eventPort;
	}
	public void setEventPort(Integer eventPort)
	{
		this.eventPort = eventPort;
	}
}
