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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

@Schema(description = "Represents a platform, including its site, configuration, sensors, and transport media details.")
public final class ApiPlatform
{
	@Schema(description = "The unique numeric identifier of the platform.", example = "24")
	private Long platformId = null;

	@Schema(description = "The name of the platform.", example = "BFD")
	private String name = null;

	@Schema(description = "The unique numeric identifier of the site associated with the platform.", example = "9954")
	private Long siteId = null;

	@Schema(description = "The agency responsible for the platform.", example = "USGS")
	private String agency = null;

	@Schema(description = "The unique numeric identifier of the platform configuration.", example = "123")
	private Long configId = null;

	@Schema(description = "A description of the platform.", example = "Barre Falls Dam, Ware River, MA")
	private String description = null;

	@Schema(description = "The designator or identifier assigned to the platform.")
	private String designator = null;

	@Schema(description = "The date and time when the platform was last modified.",
			example = "2021-07-15T14:30:00.000[UTC]")
	private Date lastModified = null;

	@Schema(description = "Indicates if the platform is in production.", example = "true")
	private boolean production = false;

	@Schema(description = "Additional properties or metadata associated with the platform.")
	private Properties properties = new Properties();

	@Schema(description = "The list of sensors associated with the platform.")
	private List<ApiPlatformSensor> platformSensors = new ArrayList<>();

	@Schema(description = "The list of transport media associated with the platform.")
	private List<ApiTransportMedium> transportMedia = new ArrayList<>();

	public Long getPlatformId()
	{
		return platformId;
	}
	public void setPlatformId(Long platformId)
	{
		this.platformId = platformId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public Long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}
	public String getAgency()
	{
		return agency;
	}
	public void setAgency(String agency)
	{
		this.agency = agency;
	}
	public Long getConfigId()
	{
		return configId;
	}
	public void setConfigId(Long configId)
	{
		this.configId = configId;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getDesignator()
	{
		return designator;
	}
	public void setDesignator(String designator)
	{
		this.designator = designator;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	public boolean isProduction()
	{
		return production;
	}
	public void setProduction(boolean production)
	{
		this.production = production;
	}
	public List<ApiPlatformSensor> getPlatformSensors()
	{
		return platformSensors;
	}
	public void setPlatformSensors(List<ApiPlatformSensor> platformSensors)
	{
		this.platformSensors = platformSensors;
	}
	public List<ApiTransportMedium> getTransportMedia()
	{
		return transportMedia;
	}
	public void setTransportMedia(List<ApiTransportMedium> transportMedia)
	{
		this.transportMedia = transportMedia;
	}
	public Properties getProperties()
	{
		return properties;
	}
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
}
