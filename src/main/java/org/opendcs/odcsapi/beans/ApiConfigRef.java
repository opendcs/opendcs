package org.opendcs.odcsapi.beans;

public class ApiConfigRef
{
	private Long configId = null;
	
	private String name = null;
	private int numPlatforms = 0;
	private String description = null;
	public Long getConfigId()
	{
		return configId;
	}
	public void setConfigId(Long configId)
	{
		this.configId = configId;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public int getNumPlatforms()
	{
		return numPlatforms;
	}
	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}
	public String getDescription()
	{
		return description;
	}
	public void setDescription(String description)
	{
		this.description = description;
	}
	
}
