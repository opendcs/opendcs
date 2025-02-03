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
package decodes.db;

import java.util.Date;

/**
 * This bean holds the status info for a single execution of a routing spec.
 *
 * This is used by the REST API.
 *
 * @author zack-rma, Zack Olson, GEI Consultants Inc.
 */
public class RoutingExecStatus {
	/** Corresponds to SCHEDULE_ENTRY_STATUS.SCHEDULE_ENTRY_STATUS_ID */
	private Long routingExecId = null;

	private Long scheduleEntryId = null;

	private Long routingSpecId = null;

	private Date runStart = null;

	// will be null if this execution is still running or was terminated via interrupt.
	private Date runStop = null;

	private int numMessages = 0;

	private int numErrors = 0;

	private int numPlatforms = 0;

	private Date lastMsgTime = null;

	private Date lastActivity = null;

	private String runStatus = null;

	private String hostname = null;

	private String lastInput = null;

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
