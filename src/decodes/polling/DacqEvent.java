/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package decodes.polling;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ilex.util.Logger;
import decodes.sql.DbKey;

/**
 * Bean class storing the attributes of a data acquisition event.
 */
public class DacqEvent
{
	private DbKey dacqEventId = DbKey.NullKey;
	private DbKey scheduleEntryStatusId = DbKey.NullKey;
	private DbKey platformId = DbKey.NullKey;
	private Date eventTime = null;
	private int eventPriority = Logger.E_INFORMATION;
	private String subsystem = null;
	private Date msgRecvTime = null;
	private String eventText = null;
	private static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	public DacqEvent()
	{
	}
	
	public String toString()
	{
		return "Evt: id=" + dacqEventId
			+ ", schedEntryStatId=" + scheduleEntryStatusId
			+ ", platformId=" + platformId
			+ ", evtTime=" + eventTime
			+ ", priority=" + Logger.priorityName[eventPriority].trim() + "(" + eventPriority + ")"
			+ ", susbsys='" + subsystem + "'"
			+ ", msgTime=" + msgRecvTime
			+ ", text='" + eventText + "'";
	}

	public DbKey getDacqEventId()
	{
		return dacqEventId;
	}

	public void setDacqEventId(DbKey dacqEventId)
	{
		this.dacqEventId = dacqEventId;
	}

	public DbKey getScheduleEntryStatusId()
	{
		return scheduleEntryStatusId;
	}

	public void setScheduleEntryStatusId(DbKey scheduleEntryStatusId)
	{
		this.scheduleEntryStatusId = scheduleEntryStatusId;
	}

	public DbKey getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(DbKey platformId)
	{
		this.platformId = platformId;
	}

	public Date getEventTime()
	{
		return eventTime;
	}

	public void setEventTime(Date eventTime)
	{
		this.eventTime = eventTime;
	}

	public int getEventPriority()
	{
		return eventPriority;
	}

	public void setEventPriority(int eventPriority)
	{
		this.eventPriority = eventPriority;
	}

	public String getSubsystem()
	{
		return subsystem;
	}

	public void setSubsystem(String subsystem)
	{
		this.subsystem = subsystem;
	}

	public Date getMsgRecvTime()
	{
		return msgRecvTime;
	}

	public void setMsgRecvTime(Date msgRecvTime)
	{
		this.msgRecvTime = msgRecvTime;
	}

	public String getEventText()
	{
		return eventText;
	}

	public void setEventText(String eventText)
	{
		this.eventText = eventText;
	}
	
	public String getPriorityStr()
	{
		return eventPriority >= 0 && eventPriority <= Logger.E_FATAL ?
			Logger.priorityName[eventPriority].trim() : "INFO";
	}
	
	public String getTimeStr()
	{
		synchronized(sdf)
		{
			return sdf.format(eventTime);
		}
	}

}
