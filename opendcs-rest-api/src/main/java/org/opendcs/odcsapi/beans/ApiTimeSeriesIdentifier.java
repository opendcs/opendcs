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


public class ApiTimeSeriesIdentifier
{
	private String uniqueString = null;
	private Long key = null;
	private String description = null;
	private String storageUnits = null;
	private boolean active = true;
	
	public ApiTimeSeriesIdentifier() {}
	
	public ApiTimeSeriesIdentifier(String uniqueString, long key, String description, String storageUnits)
	{
		super();
		this.uniqueString = uniqueString;
		this.key = key;
		this.description = description;
		this.storageUnits = storageUnits;
	}
	
	public String getUniqueString()
	{
		return uniqueString;
	}
	public void setUniqueString(String uniqueString)
	{
		this.uniqueString = uniqueString;
	}
	public Long getKey()
	{
		return key;
	}
	public void setKey(Long key)
	{
		this.key = key;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getStorageUnits()
	{
		return storageUnits;
	}
	public void setStorageUnits(String storageUnits)
	{
		this.storageUnits = storageUnits;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

}
