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

import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a platform reference, including its ID, name, agency, and other metadata.")
public final class ApiPlatformRef
{
	@Schema(description = "Unique identifier of the platform.", example = "15")
	private Long platformId = null;

	@Schema(description = "Name of the platform.", example = "BMD-tailwater")
	private String name = null;

	@Schema(description = "Unique numeric Site identifier associated with the platform.", example = "61")
	private Long siteId = null;

	@Schema(description = "Agency that owns or manages the platform.", example = "CWMS")
	private String agency = null;

	@Schema(description = "Properties object containing metadata associated with the platform.")
	private Properties transportMedia = new Properties();

	@Schema(description = "Configuration name associated with the platform.", example = "PrimaryPlatformConfig")
	private String config = null;

	@Schema(description = "Unique numeric identifier for the platform configuration.", example = "6")
	private Long configId = null;

	@Schema(description = "Description of the platform.", example = "Ball Mountain TW")
	private String description = null;

	@Schema(description = "Designator for the platform.", example = "tailwater")
	private String designator = null;
	
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
