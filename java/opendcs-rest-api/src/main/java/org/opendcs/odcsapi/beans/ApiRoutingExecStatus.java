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
 * Status info for a single execution of a routing spec, either manually via the "rs"
 * command, or within the Routing Scheduler.
 * An array of these obects is used to populate the middle panel in the Routing Monitor GUI.
 */
@Schema(description = "Represents the execution status of a routing specification, including its start and stop times, message counts, and runtime details.")
public final class ApiRoutingExecStatus
{
	/**
	 * Corresponds to SCHEDULE_ENTRY_STATUS.SCHEDULE_ENTRY_STATUS_ID
	 */
	@Schema(description = "Unique numeric identifier of this routing execution.", example = "123456")
	private Long routingExecId = null;

	@Schema(description = "Unique numeric identifier of the schedule entry associated with this execution.", example = "78910")
	private Long scheduleEntryId = null;

	@Schema(description = "Unique numeric identifier of the routing specification.", example = "45678")
	private Long routingSpecId = null;

	@Schema(description = "The timestamp when the execution started.", example = "2025-01-01T12:34:56.000[UTC]")
	private Date runStart = null;

	// will be null if this execution is still running or was terminated via interrupt.
	@Schema(description = "The timestamp when the execution stopped. Null if running or interrupted.",
			example = "2025-01-01T14:00:00.000[UTC]")
	private Date runStop = null;

	@Schema(description = "The number of messages processed during this execution.", example = "150")
	private int numMessages = 0;

	@Schema(description = "The number of errors encountered during this execution.", example = "5")
	private int numErrors = 0;

	@Schema(description = "The number of platforms processed during this execution.", example = "10")
	private int numPlatforms = 0;

	@Schema(description = "The timestamp of the last processed message.", example = "2025-01-01T13:45:00.000[UTC]")
	private Date lastMsgTime = null;

	@Schema(description = "The timestamp of the last activity during execution.", example = "2025-01-01T13:50:00.000[UTC]")
	private Date lastActivity = null;

	@Schema(description = "The status of the execution.", example = "ERR-OutputInit")
	private String runStatus = null;

	@Schema(description = "The host where this execution took place.", example = "myhost.example.com")
	private String hostname = null;

	@Schema(description = "Details regarding the last input processed in this execution.",
			example = "Raw data from platform ID 123")
	private String lastInput = null;

	@Schema(description = "Details regarding the last output generated in this execution.",
			example = "Processed data written to file.")
	private String lastOutput = null;

	public Long getRoutingExecId()
	{
		return routingExecId;
	}
	public void setRoutingExecId(Long routingExecId)
	{
		this.routingExecId = routingExecId;
	}
	public Long getScheduleEntryId()
	{
		return scheduleEntryId;
	}
	public void setScheduleEntryId(Long scheduleEntryId)
	{
		this.scheduleEntryId = scheduleEntryId;
	}
	public Long getRoutingSpecId()
	{
		return routingSpecId;
	}
	public void setRoutingSpecId(Long routingSpecId)
	{
		this.routingSpecId = routingSpecId;
	}
	public Date getRunStart()
	{
		return runStart;
	}
	public void setRunStart(Date runStart)
	{
		this.runStart = runStart;
	}
	public Date getRunStop()
	{
		return runStop;
	}
	public void setRunStop(Date runStop)
	{
		this.runStop = runStop;
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
	public int getNumPlatforms()
	{
		return numPlatforms;
	}
	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}
	public Date getLastMsgTime()
	{
		return lastMsgTime;
	}
	public void setLastMsgTime(Date lastMsgTime)
	{
		this.lastMsgTime = lastMsgTime;
	}
	public Date getLastActivity()
	{
		return lastActivity;
	}
	public void setLastActivity(Date lastActivity)
	{
		this.lastActivity = lastActivity;
	}
	public String getRunStatus()
	{
		return runStatus;
	}
	public void setRunStatus(String runStatus)
	{
		this.runStatus = runStatus;
	}
	public String getHostname()
	{
		return hostname;
	}
	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}
	public String getLastInput()
	{
		return lastInput;
	}
	public void setLastInput(String lastInput)
	{
		this.lastInput = lastInput;
	}
	public String getLastOutput()
	{
		return lastOutput;
	}
	public void setLastOutput(String lastOutput)
	{
		this.lastOutput = lastOutput;
	}
}
