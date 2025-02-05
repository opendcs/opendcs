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
import java.util.Date;
import java.util.List;
import java.util.Properties;

public final class ApiPlatform
{
	private Long platformId = null;
	private String name = null;
	private Long siteId = null;
	private String agency = null;
	private Long configId = null;
	private String description = null;
	private String designator = null;
	private Date lastModified = null;
	private boolean production = false;
	private Properties properties = new Properties();
	
	private List<ApiPlatformSensor> platformSensors = new ArrayList<>();
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
