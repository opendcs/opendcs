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

/**
 * Stores an entry from the DACQ_EVENT table.
 */
public class ApiDacqEvent
{
	private Long eventId = null;
	
	// a.k.a. scheduleEntryStatusID in database
	private Long routingExecId = null;

	private Long platformId = null;
	
	private Date eventTime = null;
	
	private String priority = null;
	
	private Long appId = null;
	private String appName = null;
	private String subsystem = null;
	private Date msgRecvTime = null;
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
