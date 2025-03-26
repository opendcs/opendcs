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

@Schema(description = "Represents a time-series value with its timestamp and raw data position.")
public final class ApiDecodesTSValue
{
	@Schema(description = "The timestamp associated with the time-series value.",
			example = "2025-01-01T00:00:00.000[UTC]")
	private Date time = null;

	@Schema(description = "The string representation of the time-series value.", example = "123.45")
	private String value = null;

	@Schema(description = "The raw data position in the source data for this value.")
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
