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

@Schema(description = "Represents the status of an application instance including details such as name, "
		+ "type, hostname, and current state.")
public final class ApiAppStatus
{
	@Schema(description = "Unique numeric identifier for the application instance.", example = "9")
	private Long appId = null;
	@Schema(description = "Name of the application.", example = "compproc")
	private String appName = null;
	@Schema(description = "Type of the application.", example = "computationprocess")
	private String appType = null;
	@Schema(description = "Hostname of the machine running the application.", example = "localhost")
	private String hostname = null;
	@Schema(description = "Process ID (PID) of the application's instance.", example = "17116")
	private Long pid = null;
	@Schema(description = "Last recorded heartbeat timestamp for the application.",
			example = "2023-05-25T16:34:18.073Z[UTC]")
	private Date heartbeat = null;
	@Schema(description = "Port number used for event communication.", example = "8086")
	private Integer eventPort = null;
	@Schema(description = "Current status of the application. Default is 'Inactive'.", example = "Cmps: 0/0")
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
