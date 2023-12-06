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

public class ApiSeason
{
	/** one-word abbreviation for season */
	private String abbr = null;
	/** one-word abbreviation for season.  If this is set, it updates a previously saved season.  If this is not set, it creates a new one.*/
	private String fromabbr = null;
	/** multi-word more descriptive name */
	private String name = null;
	/** String representation of season start */
	private String start = null;
	/** String representation of season end */
	private String end = null;
	/** Time Zone ID */
	private String tz = null;
	/** Allows user to specify sort order of seasons for GUI display */
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
