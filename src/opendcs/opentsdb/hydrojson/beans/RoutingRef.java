package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;

import decodes.sql.DbKey;

public class RoutingRef
{
	private long routingId = DbKey.NullKey.getValue();
	private String name = null;
	private String dataSourceName = null;
	private String destination = null;
	private Date lastModified = null;
	public long getRoutingId()
	{
		return routingId;
	}
	public void setRoutingId(long routingId)
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
