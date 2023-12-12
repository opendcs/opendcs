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

import java.util.HashMap;

/**
 * @author mmaloney
 *
 */
public class ApiRefList
{
	private Long reflistId = null;
	private String enumName = null;
	private HashMap<String, ApiRefListItem> items = new HashMap<String, ApiRefListItem>();
	private String defaultValue = null;
	private String description = null;
	
	public String getEnumName()
	{
		return enumName;
	}

	public void setEnumName(String enumName)
	{
		this.enumName = enumName;
	}

	public HashMap<String, ApiRefListItem> getItems()
	{
		return items;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getReflistId()
	{
		return reflistId;
	}

	public void setReflistId(Long reflistId)
	{
		this.reflistId = reflistId;
	}

	public void setItems(HashMap<String, ApiRefListItem> items)
	{
		this.items = items;
	}
}
