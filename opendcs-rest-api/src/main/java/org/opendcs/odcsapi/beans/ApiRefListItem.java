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

/**
 * Adapter for EnumValue
 *
 * @author mmaloney
 */
@Schema(description = "Represents a single item in an enumeration with associated metadata such as value, description, class names, and a sort order.")
public final class ApiRefListItem
{
	@Schema(description = "The value of the enumeration item.", example = "standard")
	private String value;

	@Schema(description = "A brief description of the enumeration item.",
			example = "DECODES Format Statements and Unit Conversions")
	private String description;

	@Schema(description = "The name of the executable class associated with this enumeration item.",
			example = "DecodesScript")
	private String execClassName;

	@Schema(description = "The name of the editable class associated with this enumeration item.")
	private String editClassName;

	@Schema(description = "The sort order for this enumeration item.", example = "1")
	private Integer sortNumber;

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getExecClassName()
	{
		return execClassName;
	}

	public void setExecClassName(String execClassName)
	{
		this.execClassName = execClassName;
	}

	public String getEditClassName()
	{
		return editClassName;
	}

	public void setEditClassName(String editClassName)
	{
		this.editClassName = editClassName;
	}

	public Integer getSortNumber()
	{
		return sortNumber;
	}

	public void setSortNumber(Integer sortNumber)
	{
		this.sortNumber = sortNumber;
	}

}
