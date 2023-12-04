package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiDecodesTSValue
{
	private Date time = null;
	private String value = null;
	private ApiTokenPosition rawDataPosition = null;

	
	public Date getTime()
	{
		return time;
	}
	public void setTime(Date time)
	{
		this.time = time;
	}
	public String getValue()
	{
		return value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	public ApiTokenPosition getRawDataPosition()
	{
		return rawDataPosition;
	}
	public void setRawDataPosition(ApiTokenPosition rawDataPosition)
	{
		this.rawDataPosition = rawDataPosition;
	}
}
