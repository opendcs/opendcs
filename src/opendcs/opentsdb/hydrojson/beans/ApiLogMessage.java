package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;

public class ApiLogMessage
{
	private Date timeStamp = new Date();
	private String priority = null;
	private String text = null;
	
	/** No-args ctor for JSON */
	public ApiLogMessage()
	{
	}
	
	public ApiLogMessage(Date timeStamp, String priority, String text)
	{
		super();
		this.timeStamp = timeStamp;
		this.priority = priority;
		this.text = text;
	}

	public Date getTimeStamp()
	{
		return timeStamp;
	}
	public void setTimeStamp(Date timeStamp)
	{
		this.timeStamp = timeStamp;
	}
	public String getPriority()
	{
		return priority;
	}
	public void setPriority(String priority)
	{
		this.priority = priority;
	}
	public String getText()
	{
		return text;
	}
	public void setText(String text)
	{
		this.text = text;
	}

}
