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

import java.util.Date;

/**
 * An array of these objects is used to populate the top panel of the Routing Monitor.
 * This is a combination of info from ROUTINGSPEC and the most recent SCHEDULE_ENTRY_STATUS
 * record.
 * Note: scheduleEntryId will be null for routing specs that do not have explicit
 * schedule entries and have never been run via the "rs" command.
 */
@Schema(description = "Represents the status of a routing specification, including schedule details, "
		+ "app information, and activity metrics.")
public final class ApiRoutingStatus
{
	@Schema(description = "The unique numeric identifier of the routing specification.", example = "44")
	private Long routingSpecId = null;
	@Schema(description = "The name of the routing specification.", example = "rs-MROI4-ROWI4")
	private String name = null;
	@Schema(description = "The unique numeric identifier of the scheduled entry associated with the routing spec. "
			+ "It can be null if it has never been explicitly scheduled.", example = "58")
	private Long scheduleEntryId = null;
	@Schema(description = "Indicates whether the routing specification is enabled.", example = "true")
	private boolean isEnabled = false;
	@Schema(description = "Indicates whether the routing specification is set to run manually.", example = "false")
	private boolean isManual = false;
	@Schema(description = "The unique numeric identifier of the application associated with this routing specification.", example = "26")
	private Long appId = null;
	@Schema(description = "The name of the application associated with this routing specification.",
			example = "RoutingScheduler")
	private String appName = null;
	@Schema(description = "The interval at which this routing spec is executed, typically expressed in cron format.",
			example = "5 minute")
	private String runInterval = null;
	@Schema(description = "The timestamp of the last activity recorded for this routing specification.",
			example = "2023-05-31T18:56:54.364Z[UTC]")
	private Date lastActivity = null;
	@Schema(description = "The timestamp of the last message processed by this routing specification.",
			example = "2023-05-31T18:56:53.099Z[UTC]")
	private Date lastMsgTime = null;
	@Schema(description = "The total number of messages processed by this routing specification.", example = "3362")
	private int numMessages = 0;
	@Schema(description = "The total number of errors encountered by this routing specification.", example = "1528")
	private int numErrors = 0;

	public Long getRoutingSpecId()
	{
		return routingSpecId;
	}
	public void setRoutingSpecId(Long routingSpecId)
	{
		this.routingSpecId = routingSpecId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getScheduleEntryId()
	{
		return scheduleEntryId;
	}
	public void setScheduleEntryId(Long scheduleEntryId)
	{
		this.scheduleEntryId = scheduleEntryId;
	}
	public boolean isManual()
	{
		return isManual;
	}
	public void setManual(boolean isManual)
	{
		this.isManual = isManual;
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
	public String getRunInterval()
	{
		return runInterval;
	}
	public void setRunInterval(String runInterval)
	{
		this.runInterval = runInterval;
	}
	public Date getLastActivity()
	{
		return lastActivity;
	}
	public void setLastActivity(Date lastActivity)
	{
		this.lastActivity = lastActivity;
	}
	public Date getLastMsgTime()
	{
		return lastMsgTime;
	}
	public void setLastMsgTime(Date lastMsgTime)
	{
		this.lastMsgTime = lastMsgTime;
	}
	public int getNumMessages()
	{
		return numMessages;
	}
	public void setNumMessages(int numMessages)
	{
		this.numMessages = numMessages;
	}
	public int getNumErrors()
	{
		return numErrors;
	}
	public void setNumErrors(int numErrors)
	{
		this.numErrors = numErrors;
	}
	public boolean isEnabled()
	{
		return isEnabled;
	}
	public void setEnabled(boolean isEnabled)
	{
		this.isEnabled = isEnabled;
	}
}
