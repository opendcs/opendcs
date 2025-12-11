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

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a log message with a timestamp, priority level, and text content.")
public final class ApiLogMessage
{
	@Schema(description = "The timestamp when the log message was created.", example = "2025-01-01T12:00:00.605[UTC]")
	private Date timeStamp = new Date();

	@Schema(description = "The priority level of the log message, e.g., INFO, WARN, or ERROR. Corresponds to Logger levels.",
			example = "INFO")
	private String priority = null;

	@Schema(description = "The text content of the log message.", example = "System started successfully.")
	private String text = null;

	/**
	 * No-args ctor for JSON
	 */
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
