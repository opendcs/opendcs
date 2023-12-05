package org.opendcs.odcsapi.beans;

import java.util.Date;

/**
 * An array of these objects is used to populate the top panel of the Routing Monitor.
 * This is a combination of info from ROUTINGSPEC and the most recent SCHEDULE_ENTRY_STATUS
 * record.
 * Note: scheduleEntryId will be null for routing specs that do not have explicit
 * schedule entries and have never been run via the "rs" command.
 */
public class ApiRoutingStatus
{
	private Long routingSpecId = null;
	private String name = null;
	private Long scheduleEntryId = null;
	private boolean isEnabled = false;
	private boolean isManual = false;
	private Long appId = null;
	private String appName = null;
	private String runInterval = null;
	private Date lastActivity = null;
	private Date lastMsgTime = null;
	private int numMessages = 0;
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
