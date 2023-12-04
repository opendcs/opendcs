package org.opendcs.odcsapi.beans;

import java.util.ArrayList;

public class ApiCompTestResults
{
	private ArrayList<ApiCompParmData> compParmData = new ArrayList<ApiCompParmData>();
	private ArrayList<ApiLogMessage> logMessages = new ArrayList<ApiLogMessage>();
	
	public ArrayList<ApiCompParmData> getCompParmData()
	{
		return compParmData;
	}
	public void setCompParmData(ArrayList<ApiCompParmData> compParmData)
	{
		this.compParmData = compParmData;
	}
	public ArrayList<ApiLogMessage> getLogMessages()
	{
		return logMessages;
	}
	public void setLogMessages(ArrayList<ApiLogMessage> logMessages)
	{
		this.logMessages = logMessages;
	}

}
