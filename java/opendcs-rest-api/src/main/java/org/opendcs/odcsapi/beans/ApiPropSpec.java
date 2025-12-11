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

@Schema(description = "Represents the specifications of a property, including its name, type, and description.")
public final class ApiPropSpec
{
	/**
	 * The name of the property in a Properties object
	 */
	@Schema(description = "The name of the property in a Properties object.", example = "debugLevel")
	private String name = null;

	/**
	 * A coded string describing the type of the property (see constant prop types in the Toolkit PropertySpec class).
	 */
	@Schema(description = "A coded string describing the type of the property. Valid values are i (int), n (number)," +
			" b (boolean), f (filename), d (directory), s (string), t (timezone), e:<enumName> (decodes enum)," +
			" h (hostname), E:<fullEnumClassPath> (java enum), l (longstring), or c (color).", example = "i")
	private String type = null;

	/**
	 * A description of this property
	 */
	@Schema(description = "A detailed description of the property.",
			example = "(default=0) Set to 1, 2, 3 for increasing levels of debug information when this platform is decoded.")
	private String description = null;

	public ApiPropSpec(String name, String type, String description)
	{
		super();
		this.name = name;
		this.type = type;
		this.description = description;
	}

	public String getName()
	{
		return name;
	}

	public String getType()
	{
		return type;
	}

	public String getDescription()
	{
		return description;
	}

}
