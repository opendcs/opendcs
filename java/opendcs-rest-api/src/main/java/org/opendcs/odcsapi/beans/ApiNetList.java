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
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author mmaloney
 */
@Schema(description = "Represents a network list, including its ID, name, type of transport medium, and other related metadata.")
public final class ApiNetList
{
	@Schema(description = "Unique surrogate key identifier of this network list.", example = "34")
	private Long netlistId = null;

	@Schema(description = "Unique name of this network list.", example = "USGS-Sites")
	private String name;

	@Schema(description = "Type of transport medium stored in this network list.", example = "other")
	private String transportMediumType;

	@Schema(description = "Preferred name type for this network list.", example = "nwshb5")
	private String siteNameTypePref;

	@Schema(description = "Time that this network list was last modified in the database.",
			example = "2020-10-19T18:14:14.788Z[UTC]")
	private Date lastModifyTime;

	@Schema(description = "Stores the NetworkListItem objects, indexed by their transportId's, "
			+ "converted to uppercase before being used as a key. Never null.")
	private Map<String, ApiNetListItem> items = new HashMap<>();

	/**
	 * Default ctor required for POST method.
	 */
	public ApiNetList()
	{
		name = null;
		transportMediumType = null;
		siteNameTypePref = null;
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

	public Map<String, ApiNetListItem> getItems()
	{
		return items;
	}

	public void setItems(Map<String, ApiNetListItem> items)
	{
		this.items = items;
	}

	public Long getNetlistId()
	{
		return netlistId;
	}

	public void setNetlistId(Long netlistId)
	{
		this.netlistId = netlistId;
	}

}
