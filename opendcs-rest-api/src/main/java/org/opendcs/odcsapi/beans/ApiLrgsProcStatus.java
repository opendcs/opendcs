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

package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiLrgsProcStatus
{
	private int slot = 0;
	private long pid = 0;
	private String name = null;
	private String type = null;
	private String user = null;
	private String status = null;
	private Long lastSeqNum = null;
	private Date lastPollTime = null;
	private Date lastMsgTime = null;
	private int staleCount = 0;
	
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
	public String getUser()
	{
		return user;
	}
	public void setUser(String user)
	{
		this.user = user;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public Long getLastSeqNum()
	{
		return lastSeqNum;
	}
	public void setLastSeqNum(Long lastSeqNum)
	{
		this.lastSeqNum = lastSeqNum;
	}
	public Date getLastPollTime()
	{
		return lastPollTime;
	}
	public void setLastPollTime(Date lastPollTime)
	{
		this.lastPollTime = lastPollTime;
	}
	public Date getLastMsgTime()
	{
		return lastMsgTime;
	}
	public void setLastMsgTime(Date lastMsgTime)
	{
		this.lastMsgTime = lastMsgTime;
	}
	public int getStaleCount()
	{
		return staleCount;
	}
	public void setStaleCount(int staleCount)
	{
		this.staleCount = staleCount;
	}
	public int getSlot()
	{
		return slot;
	}
	public void setSlot(int slot)
	{
		this.slot = slot;
	}
	public long getPid()
	{
		return pid;
	}
	public void setPid(long pid)
	{
		this.pid = pid;
	}

}
