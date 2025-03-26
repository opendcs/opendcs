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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents an interval entity with its unique ID, name, calendar constant, and multiplier used in calculations.")
public final class ApiInterval
{
	@Schema(description = "The unique numeric identifier for the interval.", example = "1")
	private Long intervalId = null;

	@Schema(description = "The name of the interval.", example = "irregular")
	private String name = null;

	@Schema(description = "The calendar constant associated with the interval.", example = "Minute")
	private String calConstant = null;

	@Schema(description = "The multiplier applied to the calendar constant.", example = "4")
	private int calMultilier = 1;

	public ApiInterval()
	{
	}
	
	public ApiInterval(Long intervalId, String name, String calConstant, int calMultilier)
	{
		super();
		this.intervalId = intervalId;
		this.name = name;
		this.calConstant = calConstant;
		this.calMultilier = calMultilier;
	}

	public Long getIntervalId()
	{
		return intervalId;
	}
	public void setIntervalId(Long intervalId)
	{
		this.intervalId = intervalId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getCalConstant()
	{
		return calConstant;
	}
	public void setCalConstant(String calConstant)
	{
		this.calConstant = calConstant;
	}
	public int getCalMultilier()
	{
		return calMultilier;
	}
	public void setCalMultilier(int calMultilier)
	{
		this.calMultilier = calMultilier;
	}
	
}
