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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a site with geographic, descriptive, and operational attributes.")
public final class ApiSite
{
	@Schema(description = "Unique numeric identifier of the site.", example = "45")
	private Long siteId = null;

	/**
	 * nametype - namevalue
	 */
	@Schema(description = "Map of site names where the key is the name type and the value is the name value.")
	private Map<String, String> sitenames = new HashMap<>();

	@Schema(description = "A brief description of the site.", example = "Barre Falls Dam. Ware River")
	private String description = null;

	@Schema(description = "Latitude of the site's location.", example = "42.4278")
	private String latitude = null;

	@Schema(description = "Longitude of the site's location.", example = "-72.06261")
	private String longitude = null;
	@Schema(description = "Elevation of the site in specified units.", example = "234.7")
	private double elevation = 0.0;

	@Schema(description = "Units for the site's elevation.", example = "M")
	private String elevUnits = null;
	@Schema(description = "The nearest city to the site.", example = "Barre Falls Dam")
	private String nearestCity = null;

	@Schema(description = "The timezone of the site's location.", example = "America/New_York")
	private String timezone = null;
	@Schema(description = "The state or province where the site is located.", example = "MA")
	private String state = null;

	@Schema(description = "The country where the site is located.", example = "USA")
	private String country = null;

	@Schema(description = "The region within the country where the site is located.")
	private String region = null;
	@Schema(description = "Indicates whether the site is currently active.", example = "true")
	private boolean active = true;
	@Schema(description = "Type of location represented by this site.")
	private String locationType = null;
	@Schema(description = "Public name of the site.")
	private String publicName = null;

	@Schema(description = "Custom properties associated with the site. This field is excluded from Swagger documentation.")
	private Properties properties = new Properties();

	@Schema(description = "The last modification timestamp for the site.")
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

	public Map<String, String> getSitenames()
	{
		return sitenames;
	}

	public void setSitenames(Map<String, String> sitenames)
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
