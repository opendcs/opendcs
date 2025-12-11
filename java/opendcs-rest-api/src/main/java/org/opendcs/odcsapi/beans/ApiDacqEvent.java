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

/**
 * Stores an entry from the DACQ_EVENT table.
 */
@Schema(description = "Represents a data acquisition entry, which stores event details for a specific time and context.")
public final class ApiDacqEvent
{
	@Schema(description = "The unique numeric identifier of the data acquisition event.", example = "181646")
	private Long eventId = null;

	@Schema(description = "The unique numeric identifier of the routing execution instance.", example = "607")
	private Long routingExecId = null;

	@Schema(description = "The unique numeric identifier of the platform associated with the event.", example = "12")
	private Long platformId = null;

	@Schema(description = "The time when the event occurred.", example = "2023-06-08T19:21:15.281Z[UTC]")
	private Date eventTime = null;

	@Schema(description = "The priority of the event. Associated with logger levels.", example = "INFO")
	private String priority = null;

	@Schema(description = "The unique numeric identifier of the application that generated the event.", example = "26")
	private Long appId = null;

	@Schema(description = "The name of the application that generated the event.", example = "RoutingScheduler")
	private String appName = null;

	@Schema(description = "The subsystem that generated the event.")
	private String subsystem = null;

	@Schema(description = "The timestamp when the message was received.", example = "2023-06-08T19:21:15.281Z[UTC]")
	private Date msgRecvTime = null;

	@Schema(description = "The textual description of the event.",
			example = "RoutingSpec(periodic-10-minute) Connected to DDS server at www.covesw.com:-1, username='covetest'")
	private String eventText = null;


	public Long getEventId()
	{
		return eventId;
	}

	public void setEventId(Long eventId)
	{
		this.eventId = eventId;
	}

	public Long getRoutingExecId()
	{
		return routingExecId;
	}

	public void setRoutingExecId(Long routingExecId)
	{
		this.routingExecId = routingExecId;
	}

	public Long getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(Long platformId)
	{
		this.platformId = platformId;
	}

	public Date getEventTime()
	{
		return eventTime;
	}

	public void setEventTime(Date eventTime)
	{
		this.eventTime = eventTime;
	}

	public String getPriority()
	{
		return priority;
	}

	public void setPriority(String priority)
	{
		this.priority = priority;
	}

	public Long getAppId()
	{
		return appId;
	}

	public void setAppId(Long appId)
	{
		this.appId = appId;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}

	public String getSubsystem()
	{
		return subsystem;
	}

	public void setSubsystem(String subsystem)
	{
		this.subsystem = subsystem;
	}

	public Date getMsgRecvTime()
	{
		return msgRecvTime;
	}

	public void setMsgRecvTime(Date msgRecvTime)
	{
		this.msgRecvTime = msgRecvTime;
	}

	public String getEventText()
	{
		return eventText;
	}

	public void setEventText(String eventText)
	{
		this.eventText = eventText;
	}

}
