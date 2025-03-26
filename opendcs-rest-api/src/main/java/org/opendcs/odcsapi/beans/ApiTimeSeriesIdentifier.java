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

@Schema(description = "Represents a time series identifier including its unique string, key, description, storage units, and active status.")
public final class ApiTimeSeriesIdentifier
{
	@Schema(description = "A unique string identifying the time series.", example = "OKVI4.Stage.Inst.15Minutes.0.raw")
	private String uniqueString = null;

	@Schema(description = "The unique numeric key for the time series.", example = "1")
	private Long key = null;

	@Schema(description = "A short description of the time series.")
	private String description = null;

	@Schema(description = "The storage units associated with the time series. Used to denote the units used in the database.",
			example = "ft")
	private String storageUnits = null;

	@Schema(description = "Indicates whether the time series is active.", example = "true")
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
