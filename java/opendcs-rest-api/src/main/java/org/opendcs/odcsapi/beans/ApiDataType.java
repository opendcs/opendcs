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

@Schema(description = "Represents a data type entity with details including an identifier, standard, code, and display name.")
public final class ApiDataType
{
	@Schema(description = "The unique numeric identifier for the data type.")
	private Long id = null;

	/**
	 * A string defining the data-type standard.
	 * This must match one of the DataTypeStandard enum values.
	 * Currently the allowable values are SHEF-PE, NOS-CODE, or EPA-CODE.
	 */
	@Schema(description = "The data type standard. Allowable values are SHEF-PE, NOS-CODE, or EPA-CODE.",
			example = "SHEF-PE")
	private String standard = null;

	/**
	 * This identifies the data type.  The form of this string depends on
	 * the standard.
	 */
	@Schema(description = "The identification of the data type, formatted according to the standard.", example = "Depth-Snow")
	private String code = null;

	@Schema(description = "The display name of the data type.", example = "SHEF-PE:Depth-Snow")
	private String displayName = null;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getStandard()
	{
		return standard;
	}

	public void setStandard(String standard)
	{
		this.standard = standard;
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}
}
