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

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class ApiSite
{
	private Long siteId = null;
	
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
	private boolean active = true;
	private String locationType = null;
	private String publicName = null;
	
	private Properties properties = new Properties();
	
	private Date lastModified = null;
	
	
	public ApiSite()
	{	
	}
	
	public Long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(Long siteId)
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

	public Properties getProperties()
	{
		return properties;
	}

	public void setProperties(Properties properties)
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

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public String getLocationType()
	{
		return locationType;
	}

	public void setLocationtype(String locationType)
	{
		this.locationType = locationType;
	}

}
