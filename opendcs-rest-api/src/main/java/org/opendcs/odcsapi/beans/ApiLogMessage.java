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
