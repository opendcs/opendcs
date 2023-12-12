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
