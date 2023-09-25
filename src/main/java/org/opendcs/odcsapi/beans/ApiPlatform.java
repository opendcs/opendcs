package org.opendcs.odcsapi.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class ApiPlatform
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
	
	private ArrayList<ApiPlatformSensor> platformSensors = new ArrayList<ApiPlatformSensor>();
	private ArrayList<ApiTransportMedium> transportMedia = new ArrayList<ApiTransportMedium>();
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
	public ArrayList<ApiPlatformSensor> getPlatformSensors()
	{
		return platformSensors;
	}
	public void setPlatformSensors(ArrayList<ApiPlatformSensor> platformSensors)
	{
		this.platformSensors = platformSensors;
	}
	public ArrayList<ApiTransportMedium> getTransportMedia()
	{
		return transportMedia;
	}
	public void setTransportMedia(ArrayList<ApiTransportMedium> transportMedia)
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
