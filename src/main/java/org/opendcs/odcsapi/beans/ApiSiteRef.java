package org.opendcs.odcsapi.beans;

import java.util.HashMap;

public class ApiSiteRef
{
	private Long siteId = null;
	
	/** nametype - namevalue */
	private HashMap<String, String> sitenames = new HashMap<String, String>();
	
	private String publicName = null;
	
	private String description = null;
	
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

	public String getPublicName()
	{
		return publicName;
	}

	public void setPublicName(String publicName)
	{
		this.publicName = publicName;
	}
	
	

}
