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
import java.util.HashMap;

/**
 * @author mmaloney
 *
 */
public class ApiNetList
{
	/** Unique surrogate key ID of this network list */
	private Long netlistId = null;

	/** Unique name of this network list. */
	private String name;

	/** Type of transport medium stored in this network list. */
	private String transportMediumType;

	/** Preferred name type for this network list. */
	private String siteNameTypePref;

	/** Time that this network list was last modified in the database. */
	private Date lastModifyTime;

	/**
	 * This HashMap stores the NetworkListEntry objects, indexed by their
	 * transportId's.  The transportId's are converted to uppercase before
	 * being used as a key in this HashMap.
	 * This data member is never null.
	 */
	private HashMap<String, ApiNetListItem> items = new HashMap<String, ApiNetListItem>();

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

	public HashMap<String, ApiNetListItem> getItems()
	{
		return items;
	}

	public void setItems(HashMap<String, ApiNetListItem> items)
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
