package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiRoutingRef
{
	private Long routingId = null;
	private String name = null;
	private String dataSourceName = null;
	private String destination = null;
	private Date lastModified = null;
	public Long getRoutingId()
	{
		return routingId;
	}
	public void setRoutingId(Long routingId)
	{
		this.routingId = routingId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getDataSourceName()
	{
		return dataSourceName;
	}
	public void setDataSourceName(String dataSourceName)
	{
		this.dataSourceName = dataSourceName;
	}
	public String getDestination()
	{
		return destination;
	}
	public void setDestination(String consumer)
	{
		this.destination = consumer;
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
