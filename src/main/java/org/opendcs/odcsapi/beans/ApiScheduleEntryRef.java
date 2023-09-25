package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiScheduleEntryRef
{
	private Long schedEntryId = null;
	private String name = null;
	private String appName = null;
	private String routingSpecName = null;
	private boolean enabled = false;
	private Date lastModified = null;
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

}
