package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import opendcs.opentsdb.hydrojson.ErrorCodes;
import opendcs.opentsdb.hydrojson.errorhandling.WebAppException;

/**
 * A full DECODES Site Record in bean format.
 */
public class SiteFull
{
	private long siteId = DbKey.NullKey.getValue();
	
	/** nametype - namevalue */
	private HashMap<String, String> sitenames = new HashMap<String, String>();
	
	private String description = null;
	
	private String latitude = null;
	private String longitude = null;
	private double elevation = 0.0;
	private String elevUnits = null;
	private String nearestCity = null;
	private String timezone = null;
	private String state = null;
	private String country = null;
	private String region = null;
	private String publicName = null;
	
	private HashMap<String, String> properties = new HashMap<String, String>();
	
	private Date lastModified = null;
	
	
	public SiteFull()
	{	
	}
	
	/**
	 * Accept a DECODES Site object and return a SiteFull.
	 * @param site
	 * @return
	 */
	public static SiteFull fromSite(Site site)
	{
		SiteFull ret = new SiteFull();
		ret.siteId = site.getId().getValue();
		for(SiteName sn : site.getNameArray())
			ret.sitenames.put(sn.getNameType(), sn.getNameValue());
		ret.description = site.getDescription();
		
		ret.latitude = site.latitude;
		ret.longitude = site.longitude;
		ret.elevation = site.getElevation();
		ret.elevUnits = site.getElevationUnits();
		ret.nearestCity = site.nearestCity;
		ret.timezone = site.timeZoneAbbr;
		ret.state = site.state;
		ret.country = site.country;
		ret.region = site.region;
		ret.publicName = site.getPublicName();
		
		Properties props = site.getProperties();
		for (String pn : props.stringPropertyNames())
			ret.properties.put(pn, props.getProperty(pn));
		
		ret.lastModified = site.getLastModifyTime();
		
		return ret;
	}
	
	/**
	 * Convert the data in this object to a DECODES Site.
	 */
	public Site toSite()
		throws WebAppException
	{
		Site site = new Site();
		
		if (sitenames.size() == 0)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"A site must contain at least one name.");
		
		site.forceSetId(
			siteId == DbKey.NullKey.getValue() ? DbKey.NullKey :
			DbKey.createDbKey(siteId));
		for(String nametype : sitenames.keySet())
			site.addName(new SiteName(site, nametype, sitenames.get(nametype)));
		site.setDescription(description);
		site.latitude = latitude;
		site.longitude = longitude;
		site.setElevation(elevation);
		site.setElevationUnits(elevUnits);
		site.nearestCity = nearestCity;
		site.timeZoneAbbr = timezone;
		site.state = state;
		site.country = country;
		site.region = region;
		site.setPublicName(publicName);
		
		for(String pname : properties.keySet())
			site.setProperty(pname, properties.get(pname));
		
		site.setLastModifyTime(lastModified);

		return site;
	}

	public long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(long siteId)
	{
		this.siteId = siteId;
	}

	public HashMap<String, String> getSitenames()
	{
		return sitenames;
	}

	public void setSitenames(HashMap<String, String> sitenames)
	{
		this.sitenames = sitenames;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getLatitude()
	{
		return latitude;
	}

	public void setLatitude(String latitude)
	{
		this.latitude = latitude;
	}

	public String getLongitude()
	{
		return longitude;
	}

	public void setLongitude(String longitude)
	{
		this.longitude = longitude;
	}

	public double getElevation()
	{
		return elevation;
	}

	public void setElevation(double elevation)
	{
		this.elevation = elevation;
	}

	public String getElevUnits()
	{
		return elevUnits;
	}

	public void setElevUnits(String elevUnits)
	{
		this.elevUnits = elevUnits;
	}

	public String getNearestCity()
	{
		return nearestCity;
	}

	public void setNearestCity(String nearestCity)
	{
		this.nearestCity = nearestCity;
	}

	public String getTimezone()
	{
		return timezone;
	}

	public void setTimezone(String timezone)
	{
		this.timezone = timezone;
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public String getCountry()
	{
		return country;
	}

	public void setCountry(String country)
	{
		this.country = country;
	}

	public String getRegion()
	{
		return region;
	}

	public void setRegion(String region)
	{
		this.region = region;
	}

	public String getPublicName()
	{
		return publicName;
	}

	public void setPublicName(String publicName)
	{
		this.publicName = publicName;
	}

	public HashMap<String, String> getProperties()
	{
		return properties;
	}

	public void setProperties(HashMap<String, String> properties)
	{
		this.properties = properties;
	}

	public Date getLastModified()
	{
		return lastModified;
	}

	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}

}
