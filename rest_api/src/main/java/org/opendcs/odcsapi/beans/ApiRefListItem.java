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

/**
 * Adapter for EnumValue
 * @author mmaloney
 *
 */
public class ApiRefListItem
{
	private String value;
	private String description;
	private String execClassName;
	private String editClassName;
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
