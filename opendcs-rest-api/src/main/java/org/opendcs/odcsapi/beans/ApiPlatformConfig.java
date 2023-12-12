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

public class ApiPlatformConfig
{
	private Long configId = null;
	
	private String name = null;
	
	private int numPlatforms = 0;
	
	private String description = null;
	
	private ArrayList<ApiConfigSensor> configSensors = 
		new ArrayList<ApiConfigSensor>();
	
	private ArrayList<ApiConfigScript> scripts = 
		new ArrayList<ApiConfigScript>();

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

	public ArrayList<ApiConfigSensor> getConfigSensors()
	{
		return configSensors;
	}
	
	public void setConfigSensors(ArrayList<ApiConfigSensor> configSensors)
	{
		this.configSensors = configSensors;
	}

	public ArrayList<ApiConfigScript> getScripts()
	{
		return scripts;
	}

	public void setScripts(ArrayList<ApiConfigScript> scripts)
	{
		this.scripts = scripts;
	}
}
