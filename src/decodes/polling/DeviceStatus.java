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

/**
 * This bean corresponds to the SERIAL_PORT_STATUS database table
 */
public class DeviceStatus
{
	/** unique port name used as database key */
	private String portName = null;
	
	/** Indicates whether this port is being used or available */
	private boolean inUse = false;
	
	/** Name of process that last used this device */
	private String lastUsedByProc = null;
	
	/** host name from which this device was last used */
	private String lastUsedByHost = null;
	
	/** Time that this device was last used */
	private Date lastActivityTime = null;
	
	/** Time that this device last successfully received a message */
	private Date lastReceiveTime = null;
	
	/** Medium ID from the last message received on this device */
	private String lastMediumId = null;

	/** Time that an error last occurred on this device */
	private Date lastErrorTime = null;
	
	/** Current port status */
	private String portStatus = "";
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }
	
	public DeviceStatus(String portName)
	{
		this.portName = portName;
	}

	public String getPortName()
	{
		return portName;
	}

	public boolean isInUse()
	{
		return inUse;
	}

	public void setInUse(boolean inUse)
	{
		this.inUse = inUse;
	}

	public String getLastUsedByProc()
	{
		return lastUsedByProc;
	}

	public void setLastUsedByProc(String lastUsedByProc)
	{
		this.lastUsedByProc = lastUsedByProc;
	}

	public String getLastUsedByHost()
	{
		return lastUsedByHost;
	}

	public void setLastUsedByHost(String lastUsedByHost)
	{
		this.lastUsedByHost = lastUsedByHost;
	}

	public Date getLastActivityTime()
	{
		return lastActivityTime;
	}
	
	public String getLastActivityTimeStr()
	{
		synchronized(sdf) { return sdf.format(lastActivityTime); }
	}

	public void setLastActivityTime(Date lastActivityTime)
	{
		this.lastActivityTime = lastActivityTime;
	}

	public Date getLastReceiveTime()
	{
		return lastReceiveTime;
	}
	
	public String getLastReceiveTimeStr()
	{
		if (lastReceiveTime == null)
			return "";
		synchronized(sdf) { return sdf.format(lastReceiveTime); }
	}

	public void setLastReceiveTime(Date lastReceiveTime)
	{
		this.lastReceiveTime = lastReceiveTime;
	}

	public String getLastMediumId()
	{
		return lastMediumId;
	}

	public void setLastMediumId(String lastMediumId)
	{
		this.lastMediumId = lastMediumId;
	}

	public Date getLastErrorTime()
	{
		return lastErrorTime;
	}
	
	public String getLastErrorTimeStr()
	{
		synchronized(sdf) { return sdf.format(lastErrorTime); }
	}

	public void setLastErrorTime(Date lastErrorTime)
	{
		this.lastErrorTime = lastErrorTime;
	}

	public String getPortStatus()
	{
		return portStatus;
	}

	public void setPortStatus(String portStatus)
	{
		this.portStatus = portStatus;
	}
	
	public String toString()
	{
		return "DeviceStatus: " + 
			portName + ", "
			+ inUse + ", "
			+ lastUsedByProc + ", "
			+ lastUsedByHost + ", "
			+ lastActivityTime + ", "
			+ lastReceiveTime + ", "
			+ lastMediumId + ", "
			+ lastErrorTime + ", "
			+ portStatus;
	}
	
}
