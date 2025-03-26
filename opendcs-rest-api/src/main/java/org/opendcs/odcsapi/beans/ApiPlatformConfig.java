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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the platform configuration containing details about sensors, scripts, and other platform attributes.")
public final class ApiPlatformConfig
{
	@Schema(description = "The unique numeric identifier of the platform configuration.", example = "1")
	private Long configId = null;

	@Schema(description = "The name of the platform configuration.", example = "Platform Configuration 1")
	private String name = null;

	@Schema(description = "The number of platforms associated with this configuration.", example = "122")
	private int numPlatforms = 0;

	@Schema(description = "Detailed description of the platform configuration.")
	private String description = null;

	@Schema(description = "A list of configuration sensors associated with the platform.")
	private List<ApiConfigSensor> configSensors =
			new ArrayList<>();

	@Schema(description = "A list of scripts associated with the platform configuration.")
	private List<ApiConfigScript> scripts =
			new ArrayList<>();

	public Long getConfigId()
	{
		return configId;
	}

	public void setConfigId(Long configId)
	{
		this.configId = configId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public int getNumPlatforms()
	{
		return numPlatforms;
	}

	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public List<ApiConfigSensor> getConfigSensors()
	{
		return configSensors;
	}
	
	public void setConfigSensors(List<ApiConfigSensor> configSensors)
	{
		this.configSensors = configSensors;
	}

	public List<ApiConfigScript> getScripts()
	{
		return scripts;
	}

	public void setScripts(List<ApiConfigScript> scripts)
	{
		this.scripts = scripts;
	}
}
