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

@Schema(description = "Represents a site name with its associated type and unique identifier.")
public final class ApiSiteName
{

	/**
	 * The name type -- should match an enum value.
	 */
	@Schema(description = "The type of the name, which should correspond to a valid enumeration value.", example = "CWMS")
	private String nameType = null;

	/**
	 * The name value.  Case is significant.
	 */
	@Schema(description = "The value of the name. Case sensitivity is significant.", example = "BFD")
	private String nameValue = null;

	@Schema(description = "The unique numeric identifier for the site.", example = "867")
	private Long siteId = null;

	public ApiSiteName()
	{
	}

	public ApiSiteName(Long siteId, String nameType, String nameValue)
	{
		super();
		this.siteId = siteId;
		this.nameType = nameType;
		this.nameValue = nameValue;
	}

	public String getNameType()
	{
		return nameType;
	}

	public void setNameType(String nameType)
	{
		this.nameType = nameType;
	}

	public String getNameValue()
	{
		return nameValue;
	}

	public void setNameValue(String nameValue)
	{
		this.nameValue = nameValue;
	}

	public Long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}

}

