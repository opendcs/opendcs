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

public class ApiNetlistRef
{
	/** Unique surrogate key ID of this network list */
	private Long netlistId = null;
	
	/** Unique name of this network list. */
	private String name = null;

	/** Type of transport medium stored in this network list. */
	private String transportMediumType = null;

	/** Preferred name type for this network list. */
	private String siteNameTypePref = null;
	
	/** Time that this network list was last modified in the database. */
	private Date lastModifyTime = null;
	
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
