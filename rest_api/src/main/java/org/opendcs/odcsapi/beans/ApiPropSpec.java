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

public class ApiPropSpec
{
	/** The name of the property in a Properties object */
	private String name = null;
	
	/**
	 * A coded string describing the type of the property (see constant prop types herein).
	 */
	private String type = null;
	
	/** A description of this property */
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
