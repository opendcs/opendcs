/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a transport medium with its communication and authentication details.")
public final class ApiTransportMedium
{
	@Schema(description = "The type of the medium, defaulting to 'GOES'.", example = "GOES")
	private String mediumType = "GOES";
	@Schema(description = "The identifier of the medium.", example = "CE31D030")
	private String mediumId = null;
	@Schema(description = "The name of the associated script.", example = "Script1")
	private String scriptName = null;
	@Schema(description = "The channel number used for transmission.", example = "1")
	private Integer channelNum = null;
	@Schema(description = "The assigned time value in seconds.", example = "300")
	private Integer assignedTime = null;
	@Schema(description = "The transport window duration in seconds.", example = "600")
	private Integer transportWindow = null;
	@Schema(description = "The interval between transmissions in seconds.", example = "3600")
	private Integer transportInterval = null;
	@Schema(description = "The adjustment to the time in seconds.", example = "5")
	private int timeAdjustment = 0;
	@Schema(description = "The timezone of the transport medium.", example = "UTC")
	private String timezone = null;
	@Schema(description = "The type of the logger used, if applicable.", example = "Sutron Logger")
	private String loggerType = null;
	@Schema(description = "The baud rate for transmission.", example = "9600")
	private Integer baud = null;
	@Schema(description = "The number of stop bits for transmission.", example = "1")
	private Integer stopBits = null;
	@Schema(description = "The parity setting for transmission (e.g., none, even, odd).", example = "none")
	private String parity = null;
	@Schema(description = "The number of data bits used in communication.", example = "8")
	private Integer dataBits = null;
	@Schema(description = "Indicates whether login is required for the transport medium.", example = "true")
	private Boolean doLogin = null;
	@Schema(description = "The username for authentication, if applicable.", example = "user123")
	private String username = null;
	@Schema(description = "The password for authentication, if applicable.", example = "password123")
	private String password = null;

	public String getMediumType()
	{
		return mediumType;
	}
	public void setMediumType(String mediumType)
	{
		this.mediumType = mediumType;
	}
	public String getMediumId()
	{
		return mediumId;
	}
	public void setMediumId(String mediumId)
	{
		this.mediumId = mediumId;
	}
	public String getScriptName()
	{
		return scriptName;
	}
	public void setScriptName(String scriptName)
	{
		this.scriptName = scriptName;
	}
	public Integer getChannelNum()
	{
		return channelNum;
	}
	public void setChannelNum(Integer channelNum)
	{
		this.channelNum = channelNum;
	}
	public Integer getAssignedTime()
	{
		return assignedTime;
	}
	public void setAssignedTime(Integer assignedTime)
	{
		this.assignedTime = assignedTime;
	}
	public Integer getTransportWindow()
	{
		return transportWindow;
	}
	public void setTransportWindow(Integer transportWindow)
	{
		this.transportWindow = transportWindow;
	}
	public Integer getTransportInterval()
	{
		return transportInterval;
	}
	public void setTransportInterval(Integer transportInterval)
	{
		this.transportInterval = transportInterval;
	}
	public int getTimeAdjustment()
	{
		return timeAdjustment;
	}
	public void setTimeAdjustment(int timeAdjustment)
	{
		this.timeAdjustment = timeAdjustment;
	}
	public String getTimezone()
	{
		return timezone;
	}
	public void setTimezone(String timezone)
	{
		this.timezone = timezone;
	}
	public String getLoggerType()
	{
		return loggerType;
	}
	public void setLoggerType(String loggerType)
	{
		this.loggerType = loggerType;
	}
	public Integer getBaud()
	{
		return baud;
	}
	public void setBaud(Integer baud)
	{
		this.baud = baud;
	}
	public Integer getStopBits()
	{
		return stopBits;
	}
	public void setStopBits(Integer stopBits)
	{
		this.stopBits = stopBits;
	}
	public String getParity()
	{
		return parity;
	}
	public void setParity(String parity)
	{
		this.parity = parity;
	}
	public Integer getDataBits()
	{
		return dataBits;
	}
	public void setDataBits(Integer dataBits)
	{
		this.dataBits = dataBits;
	}
	public Boolean getDoLogin()
	{
		return doLogin;
	}
	public void setDoLogin(Boolean doLogin)
	{
		this.doLogin = doLogin;
	}
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	

}
