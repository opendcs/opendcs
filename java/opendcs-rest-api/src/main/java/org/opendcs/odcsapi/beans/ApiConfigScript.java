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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a configuration script, including name, data order, header type, sensors, and format statements.")
public final class ApiConfigScript
{
	@Schema(description = "The name of the configuration script.", example = "ST")
	private String name = null;

	/**
	 * U=undefined, A=ascending, D=descending
	 */
	@JsonProperty("dataOrder")
	@Schema(description = "Defines the data order. Valid values from the DataOrder enum are: U (undefined), A (ascending), D (descending).",
			example = "D")
	private DataOrder dataOrder = DataOrder.UNDEFINED;

	@Schema(description = "Specifies the type of header used in the configuration script.", example = "decodes:goes")
	private String headerType = null;

	@Schema(description = "A list of sensors associated with the configuration script.",
			implementation = ApiConfigScriptSensor.class)
	private List<ApiConfigScriptSensor> scriptSensors =
			new ArrayList<>();

	@Schema(description = "A list of format statements used by the configuration script.",
			implementation = ApiScriptFormatStatement.class)
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

	public DataOrder getDataOrder()
	{
		return dataOrder;
	}

	public void setDataOrder(DataOrder dataOrder)
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

	public enum DataOrder
	{
		@JsonProperty("U")
		UNDEFINED('U'),
		@JsonProperty("A")
		ASCENDING('A'),
		@JsonProperty("D")
		DESCENDING('D');

		private final char code;

		DataOrder(char code)
		{
			this.code = code;
		}

		@JsonValue
		public char getCode()
		{
			return code;
		}
	}
}
