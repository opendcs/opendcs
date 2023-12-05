package org.opendcs.odcsapi.beans;


public class ApiTimeSeriesIdentifier
{
	private String uniqueString = null;
	private Long key = null;
	private String description = null;
	private String storageUnits = null;
	private boolean active = true;
	
	public ApiTimeSeriesIdentifier() {}
	
	public ApiTimeSeriesIdentifier(String uniqueString, long key, String description, String storageUnits)
	{
		super();
		this.uniqueString = uniqueString;
		this.key = key;
		this.description = description;
		this.storageUnits = storageUnits;
	}
	
	public String getUniqueString()
	{
		return uniqueString;
	}
	public void setUniqueString(String uniqueString)
	{
		this.uniqueString = uniqueString;
	}
	public Long getKey()
	{
		return key;
	}
	public void setKey(Long key)
	{
		this.key = key;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	public String getStorageUnits()
	{
		return storageUnits;
	}
	public void setStorageUnits(String storageUnits)
	{
		this.storageUnits = storageUnits;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

}
