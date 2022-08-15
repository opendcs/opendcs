package opendcs.opentsdb.hydrojson.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import decodes.sql.DbKey;

public class DecodesPlatform
{
	private long platformId = DbKey.NullKey.getValue();
	private String name = null;
	private long siteId = DbKey.NullKey.getValue();
	private String agency = null;
	private long configId = DbKey.NullKey.getValue();
	private String description = null;
	private String designator = null;
	private Date lastModified = null;
	private boolean production = false;
	private Properties properties = new Properties();
	
	private ArrayList<DecodesPlatformSensor> platformSensors = new ArrayList<DecodesPlatformSensor>();
	private ArrayList<DecodesTransportMedium> transportMedia = new ArrayList<DecodesTransportMedium>();
	public long getPlatformId()
	{
		return platformId;
	}
	public void setPlatformId(long platformId)
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
	public long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(long siteId)
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
	public long getConfigId()
	{
		return configId;
	}
	public void setConfigId(long configId)
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
	public ArrayList<DecodesPlatformSensor> getPlatformSensors()
	{
		return platformSensors;
	}
	public void setPlatformSensors(ArrayList<DecodesPlatformSensor> platformSensors)
	{
		this.platformSensors = platformSensors;
	}
	public ArrayList<DecodesTransportMedium> getTransportMedia()
	{
		return transportMedia;
	}
	public void setTransportMedia(ArrayList<DecodesTransportMedium> transportMedia)
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
