package opendcs.opentsdb.hydrojson.beans;

import java.util.Properties;

import javax.xml.bind.annotation.XmlRootElement;

import decodes.db.Platform;
import decodes.db.TransportMedium;
import decodes.sql.DbKey;

@XmlRootElement
public class PlatformRef
{
	private long platformId = DbKey.NullKey.getValue();
	private String name = null;
	private long siteId = DbKey.NullKey.getValue();
	private String agency = null;
	private Properties transportMedia = new Properties();
	private String config = null;
	private long configId = DbKey.NullKey.getValue();
	private String description = null;
	private String designator = null;
	
	/**
	 * No args ctor for JSON
	 */
	public PlatformRef() {}
	
	/**
	 * Construct a PlatformSpec bean from the passed DECODES Platform Object.
	 * @param p
	 */
	public static PlatformRef fromDecodes(Platform p, TransportMedium tm)
	{
		PlatformRef ret = new PlatformRef();
		ret.platformId = p.getId().getValue();
		ret.name = p.getDisplayName();
		ret.agency = p.getAgency();
		ret.config = p.getConfigName();
		ret.description = p.getDescription();
		
		return ret;
	}

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

	public long getPlatformId()
	{
		return platformId;
	}

	public void setPlatformId(long platformId)
	{
		this.platformId = platformId;
	}

	public long getConfigId()
	{
		return configId;
	}

	public void setConfigId(long configId)
	{
		this.configId = configId;
	}

	public long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(long siteId)
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
