package opendcs.opentsdb.hydrojson.beans;

import decodes.db.Constants;

public class DecodesTransportMedium
{
	private String mediumType = Constants.medium_Goes;
	private String mediumId = null;
	private String scriptName = null;
	private Integer channelNum = null;
	private Integer assignedTime = null;
	private Integer transportWindow = null;
	private Integer transportInterval = null;
	private int timeAdjustment = 0;
	private String timezone = null;
	private String loggerType = null;
	private Integer baud = null;
	private Integer stopBits = null;
	private String parity = null;
	private Integer dataBits = null;
	private Boolean doLogin = null;
	private String username = null;
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
