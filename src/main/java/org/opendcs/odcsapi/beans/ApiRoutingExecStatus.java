package org.opendcs.odcsapi.beans;

import java.util.Date;

/**
 * Status info for a single execution of a routing spec, either manually via the "rs"
 * command, or within the Routing Scheduler.
 * An array of these obects is used to populate the middle panel in the Routing Monitor GUI.
 */
public class ApiRoutingExecStatus
{
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
