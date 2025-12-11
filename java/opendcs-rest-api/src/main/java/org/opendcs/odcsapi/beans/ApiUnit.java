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

@Schema(description = "Represents a unit with its abbreviation, name, family, and associated measures.")
public final class ApiUnit
{
	@Schema(description = "Abbreviation for the unit.", example = "$")
	private String abbr = null;

	@Schema(description = "Name of the unit.", example = "Dollars")
	private String name = null;

	@Schema(description = "Family or category of the unit.", example = "univ")
	private String family = null;

	@Schema(description = "Type of measures associated with the unit.", example = "Currency")
	private String measures = null;

	public String getAbbr()
	{
		return abbr;
	}
	public void setAbbr(String abbr)
	{
		this.abbr = abbr;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public String getFamily()
	{
		return family;
	}
	public void setFamily(String family)
	{
		this.family = family;
	}
	public String getMeasures()
	{
		return measures;
	}
	public void setMeasures(String measures)
	{
		this.measures = measures;
	}
}
