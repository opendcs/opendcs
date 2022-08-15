package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;

import decodes.sql.DbKey;

public class ScheduleEntryRef
{
	private long schedEntryId = DbKey.NullKey.getValue();
	private String name = null;
	private String appName = null;
	private String routingSpecName = null;
	private boolean enabled = false;
	private Date lastModified = null;
	public long getSchedEntryId()
	{
		return schedEntryId;
	}
	public void setSchedEntryId(long schedEntryId)
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
