package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiPlatformStatus
{
	private Long platformId = null;
	private String platformName = null;
	private Long siteId = null;
	private Date lastContact = null;
	private Date lastMessage = null;
	private Date lastError = null;
	private String lastMsgQuality = null;
	private String annotation = null;
	private Long lastRoutingExecId = null;
	private String routingSpecName = null;
	public Long getPlatformId()
	{
		return platformId;
	}
	public void setPlatformId(Long platformId)
	{
		this.platformId = platformId;
	}
	public String getPlatformName()
	{
		return platformName;
	}
	public void setPlatformName(String platformName)
	{
		this.platformName = platformName;
	}
	public Long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}
	public Date getLastContact()
	{
		return lastContact;
	}
	public void setLastContact(Date lastContact)
	{
		this.lastContact = lastContact;
	}
	public Date getLastMessage()
	{
		return lastMessage;
	}
	public void setLastMessage(Date lastMessage)
	{
		this.lastMessage = lastMessage;
	}
	public Date getLastError()
	{
		return lastError;
	}
	public void setLastError(Date lastError)
	{
		this.lastError = lastError;
	}
	public String getLastMsgQuality()
	{
		return lastMsgQuality;
	}
	public void setLastMsgQuality(String lastMsgQuality)
	{
		this.lastMsgQuality = lastMsgQuality;
	}
	public String getAnnotation()
	{
		return annotation;
	}
	public void setAnnotation(String annotation)
	{
		this.annotation = annotation;
	}
	public Long getLastRoutingExecId()
	{
		return lastRoutingExecId;
	}
	public void setLastRoutingExecId(Long lastRoutingExecId)
	{
		this.lastRoutingExecId = lastRoutingExecId;
	}
	public String getRoutingSpecName()
	{
		return routingSpecName;
	}
	public void setRoutingSpecName(String routingSpecName)
	{
		this.routingSpecName = routingSpecName;
	}
	
}
