package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiLrgsDownlinkStatus
{
	private int slot = 0;
	private String name = null;
	private String type = null;
	private String status = null;
	private Date lastMsgRecvTime = null;
	private String group = null;
	private long lastSeqNum = 0;
	private ApiLrgsHourlyQuality[] hourlyQuality = null;
		
	public ApiLrgsDownlinkStatus()
	{
		hourlyQuality = new ApiLrgsHourlyQuality[24];
		for(int i=0; i<24; i++)
		{
			hourlyQuality[i] = new ApiLrgsHourlyQuality();
			hourlyQuality[i].setHour(i);
		}
	}


	public int getSlot()
	{
		return slot;
	}


	public void setSlot(int slot)
	{
		this.slot = slot;
	}


	public String getName()
	{
		return name;
	}


	public void setName(String name)
	{
		this.name = name;
	}


	public String getType()
	{
		return type;
	}


	public void setType(String type)
	{
		this.type = type;
	}


	public String getStatus()
	{
		return status;
	}


	public void setStatus(String status)
	{
		this.status = status;
	}


	public Date getLastMsgRecvTime()
	{
		return lastMsgRecvTime;
	}


	public void setLastMsgRecvTime(Date lastMsgRecvTime)
	{
		this.lastMsgRecvTime = lastMsgRecvTime;
	}


	public String getGroup()
	{
		return group;
	}


	public void setGroup(String group)
	{
		this.group = group;
	}


	public ApiLrgsHourlyQuality[] getHourlyQuality()
	{
		return hourlyQuality;
	}


	public void setHourlyQuality(ApiLrgsHourlyQuality[] hourlyQuality)
	{
		this.hourlyQuality = hourlyQuality;
	}


	public long getLastSeqNum()
	{
		return lastSeqNum;
	}


	public void setLastSeqNum(long lastSeqNum)
	{
		this.lastSeqNum = lastSeqNum;
	}
}
