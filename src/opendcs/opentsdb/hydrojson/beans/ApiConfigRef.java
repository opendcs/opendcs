package opendcs.opentsdb.hydrojson.beans;

import decodes.sql.DbKey;

public class ApiConfigRef
{
	private long configId = DbKey.NullKey.getValue();
	
	private String name = null;
	private int numPlatforms = 0;
	private String description = null;
	public long getConfigId()
	{
		return configId;
	}
	public void setConfigId(long configId)
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
