package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;

import decodes.decoder.TokenPosition;

public class ApiDecodesTSValue
{
	private Date time = null;
	private String value = null;
	private TokenPosition rawDataPosition = null;

	
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
	public TokenPosition getRawDataPosition()
	{
		return rawDataPosition;
	}
	public void setRawDataPosition(TokenPosition rawDataPosition)
	{
		this.rawDataPosition = rawDataPosition;
	}
}
