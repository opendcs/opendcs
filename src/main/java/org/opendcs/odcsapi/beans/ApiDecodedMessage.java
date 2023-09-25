package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Date;

public class ApiDecodedMessage
{
	private Date messageTime = null;
	private ArrayList<ApiLogMessage> logMessages = new ArrayList<ApiLogMessage>();
	private ArrayList<ApiDecodesTimeSeries> timeSeries = new ArrayList<ApiDecodesTimeSeries>();

	public ArrayList<ApiLogMessage> getLogMessages()
	{
		return logMessages;
	}

	public void setLogMessages(ArrayList<ApiLogMessage> logMessages)
	{
		this.logMessages = logMessages;
	}

	public Date getMessageTime()
	{
		return messageTime;
	}

	public void setMessageTime(Date messageTime)
	{
		this.messageTime = messageTime;
	}

	public ArrayList<ApiDecodesTimeSeries> getTimeSeries()
	{
		return timeSeries;
	}

	public void setTimeSeries(ArrayList<ApiDecodesTimeSeries> timeSeries)
	{
		this.timeSeries = timeSeries;
	}
}
