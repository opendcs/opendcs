package org.opendcs.odcsapi.beans;

import java.util.Date;

public class ApiNetlistRef
{
	/** Unique surrogate key ID of this network list */
	private Long netlistId = null;
	
	/** Unique name of this network list. */
	private String name = null;

	/** Type of transport medium stored in this network list. */
	private String transportMediumType = null;

	/** Preferred name type for this network list. */
	private String siteNameTypePref = null;
	
	/** Time that this network list was last modified in the database. */
	private Date lastModifyTime = null;
	
	private int numPlatforms = 0;

	public Long getNetlistId()
	{
		return netlistId;
	}

	public void setNetlistId(Long netlistId)
	{
		this.netlistId = netlistId;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTransportMediumType()
	{
		return transportMediumType;
	}

	public void setTransportMediumType(String transportMediumType)
	{
		this.transportMediumType = transportMediumType;
	}

	public String getSiteNameTypePref()
	{
		return siteNameTypePref;
	}

	public void setSiteNameTypePref(String siteNameTypePref)
	{
		this.siteNameTypePref = siteNameTypePref;
	}

	public Date getLastModifyTime()
	{
		return lastModifyTime;
	}

	public void setLastModifyTime(Date lastModifyTime)
	{
		this.lastModifyTime = lastModifyTime;
	}

	public int getNumPlatforms()
	{
		return numPlatforms;
	}

	public void setNumPlatforms(int numPlatforms)
	{
		this.numPlatforms = numPlatforms;
	}

}
