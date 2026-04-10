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

@Schema(description = "Represents an algorithm parameter with a role name and parameter type.")
public final class ApiAlgoParm
{
	/**
	 * The role name -- must be unique within an algorithm.
	 */
	@Schema(description = "The role name of the parameter, must be unique within an algorithm.", example = "input1")
	private String roleName;

	/**
	 * The parameter type.
	 */
	@Schema(description = "The type of the parameter. Can be input (i), output (o), or can embed extra information.",
			example = "i")
	private String parmType;

	public String getRoleName()
	{
		return roleName;
	}

	public void setRoleName(String roleName)
	{
		this.roleName = roleName;
	}

	public String getParmType()
	{
		return parmType;
	}

	public void setParmType(String parmType)
	{
		this.parmType = parmType;
	}


}
