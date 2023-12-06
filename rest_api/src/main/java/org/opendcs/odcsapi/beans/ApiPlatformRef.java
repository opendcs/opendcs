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

import java.util.Properties;

public class ApiPlatformRef
{
	private Long platformId = null;
	private String name = null;
	private Long siteId =null;
	private String agency = null;
	private Properties transportMedia = new Properties();
	private String config = null;
	private Long configId = null;
	private String description = null;
	private String designator = null;
	
	/**
	 * No args ctor for JSON
	 */
	public ApiPlatformRef() {}
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getAgency()
	{
		return agency;
	}

	public void setAgency(String agency)
	{
		this.agency = agency;
	}

	public String getConfig()
	{
		return config;
	}

	public void setConfig(String config)
	{
		this.config = config;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public Long getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(Long platformId)
	{
		this.platformId = platformId;
	}

	public Long getConfigId()
	{
		return configId;
	}

	public void setConfigId(Long configId)
	{
		this.configId = configId;
	}

	public Long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}

	public String getDesignator()
	{
		return designator;
	}

	public void setDesignator(String designator)
	{
		this.designator = designator;
	}

	public Properties getTransportMedia()
	{
		return transportMedia;
	}

	public void setTransportMedia(Properties transportMedia)
	{
		this.transportMedia = transportMedia;
	}


}
