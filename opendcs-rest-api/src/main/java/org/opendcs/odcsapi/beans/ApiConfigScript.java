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

import java.util.ArrayList;
import java.util.List;

public class ApiConfigScript
{
	private String name = null;
	
	/** U=undefined, A=ascending, D=descending */
	private char dataOrder = 'U';
	
	private String headerType = null;
	
	private List<ApiConfigScriptSensor> scriptSensors =
		new ArrayList<>();
	
	private List<ApiScriptFormatStatement> formatStatements =
		new ArrayList<>();

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public char getDataOrder()
	{
		return dataOrder;
	}

	public void setDataOrder(char dataOrder)
	{
		this.dataOrder = dataOrder;
	}

	public String getHeaderType()
	{
		return headerType;
	}

	public void setHeaderType(String headerType)
	{
		this.headerType = headerType;
	}

	public List<ApiConfigScriptSensor> getScriptSensors()
	{
		return scriptSensors;
	}

	public void setScriptSensors(List<ApiConfigScriptSensor> scriptSensors)
	{
		this.scriptSensors = scriptSensors;
	}

	public List<ApiScriptFormatStatement> getFormatStatements()
	{
		return formatStatements;
	}

	public void setFormatStatements(List<ApiScriptFormatStatement> formatStatements)
	{
		this.formatStatements = formatStatements;
	}
	
}
