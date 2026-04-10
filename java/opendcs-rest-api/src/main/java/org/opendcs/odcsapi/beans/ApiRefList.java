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

import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author mmaloney
 */
@Schema(description = "Represents an enumeration object containing values. Referred to as a reference list.")
public final class ApiRefList
{
	@Schema(description = "The unique numeric identifier for the enumeration.", example = "3")
	private Long reflistId = null;

	@Schema(description = "The name of the enumeration.", example = "ScriptType")
	private String enumName = null;

	@Schema(description = "The map of items in this enumeration, where each key is an item name.")
	private Map<String, ApiRefListItem> items = new HashMap<>();

	@Schema(description = "The default value of the enumeration.")
	private String defaultValue = null;

	@Schema(description = "A human-readable explanation of this enumeration.")
	private String description = null;

	public String getEnumName()
	{
		return enumName;
	}

	public void setEnumName(String enumName)
	{
		this.enumName = enumName;
	}

	public Map<String, ApiRefListItem> getItems()
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

	public void setItems(Map<String, ApiRefListItem> items)
	{
		this.items = items;
	}
}
