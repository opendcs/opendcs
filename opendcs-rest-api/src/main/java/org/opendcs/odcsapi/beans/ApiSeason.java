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

@Schema(description = "Represents a season with details like abbreviation, name, start/end dates, and time zone.")
public final class ApiSeason
{
	/**
	 * one-word abbreviation for season
	 */
	@Schema(description = "One-word abbreviation for the season.", example = "autumn")
	private String abbr = null;
	/**
	 * one-word abbreviation for season.  If this is set, it updates a previously saved season.  If this is not set, it creates a new one.
	 */
	@Schema(description = "One-word abbreviation from an existing season. If set, it updates the existing season. Otherwise, it creates a new one.")
	private String fromabbr = null;
	/**
	 * multi-word more descriptive name
	 */
	@Schema(description = "Multi-word descriptive name of the season.", example = "Autumn")
	private String name = null;
	/**
	 * String representation of season start
	 */
	@Schema(description = "String representation of the season's start date.", example = "09/22-12:00")
	private String start = null;
	/**
	 * String representation of season end
	 */
	@Schema(description = "String representation of the season's end date.", example = "12/21-12:00")
	private String end = null;
	/**
	 * Time Zone ID
	 */
	@Schema(description = "ID of the time zone associated with the season.", example = "EST5EDT")
	private String tz = null;
	/**
	 * Allows user to specify sort order of seasons for GUI display
	 */
	@Schema(description = "Specifies the sort order of seasons for GUI display.", example = "1")
	private Integer sortNumber = null;

	public ApiSeason()
	{	
	}

	public String getAbbr()
	{
		return abbr;
	}
	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
	}
	public String getFromabbr()
	{
		return fromabbr;
	}
	public void setFromabbr(String fromabbr)
	{
		this.fromabbr = fromabbr;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getStart()
	{
		return start;
	}
	public void setStart(String start)
	{
		this.start = start;
	}
	public String getEnd()
	{
		return end;
	}
	public void setEnd(String end)
	{
		this.end = end;
	}
	public String getTz()
	{
		return tz;
	}
	public void setTz(String tz)
	{
		this.tz = tz;
	}

	public Integer getSortNumber()
	{
		return sortNumber;
	}

	public void setSortNumber(Integer sortNumber)
	{
		this.sortNumber = sortNumber;
	}

}
