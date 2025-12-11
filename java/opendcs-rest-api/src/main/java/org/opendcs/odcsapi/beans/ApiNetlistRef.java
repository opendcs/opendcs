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

@Schema(description = "Represents a reference to a network list, including metadata such as Transport Medium Type, Site Name Type, and modification time.")
public final class ApiNetlistRef
{
	/** Unique surrogate key ID of this network list */
	@Schema(description = "Unique numeric identifier of this network list.", example = "881")
	private Long netlistId = null;

	/** Unique name of this network list. */
	@Schema(description = "Unique name of this network list.", example = "USGS-Sites")
	private String name = null;

	/** Type of transport medium stored in this network list. */
	@Schema(description = "Type of transport medium stored in this network list.", example = "other")
	private String transportMediumType = null;

	/** Preferred name type for this network list. */
	@Schema(description = "Preferred name type for this network list.", example = "nwshb5")
	private String siteNameTypePref = null;

	/** Time that this network list was last modified in the database. */
	@Schema(description = "Timestamp when this network list was last modified in the database.",
			example = "2021-07-01T12:34:56.493[UTC]")
	private Date lastModifyTime = null;

	@Schema(description = "Number of platforms associated with this network list.", example = "3")
	private int numPlatforms = 0;

	public Long getNetlistId()
	{
		return netlistId;
	}

	public void setNetlistId(Long netlistId)
	{
		this.netlistId = netlistId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTransportMediumType()
	{
		return transportMediumType;
	}

	public void setTransportMediumType(String transportMediumType)
	{
		this.transportMediumType = transportMediumType;
	}

	public String getSiteNameTypePref()
	{
		return siteNameTypePref;
	}

	public void setSiteNameTypePref(String siteNameTypePref)
	{
		this.siteNameTypePref = siteNameTypePref;
	}

	public Date getLastModifyTime()
	{
		return lastModifyTime;
	}

	public void setLastModifyTime(Date lastModifyTime)
	{
		this.lastModifyTime = lastModifyTime;
	}

	public int getNumPlatforms()
	{
		return numPlatforms;
	}

	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}

}
