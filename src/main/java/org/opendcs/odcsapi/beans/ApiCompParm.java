package org.opendcs.odcsapi.beans;

public class ApiCompParm
{
	private String algoParmType;

	/** Algorithm role name. */
	private String algoRoleName = null;
	
	// Non-group comp parms will be completely defined with a time series key.
	private Long tsKey = null;
	 
	private Long dataTypeId = null;
	private String dataType = null;

	/**
	 * String interval code.
	 * @see IntervalCodes
	 */
	private String interval = null;
	
	/**
	 * Signed number of time intervals. Offset for retrieving this parameter
	 * relative to zero-based time.
	 */
	private int deltaT = 0;
	private String deltaTUnits = "Seconds";

	private String unitsAbbr = null;

	private Long siteId = null;
	private String siteName = null;
	
	// HDB-specific fields:
	private String tableSelector = null;
	private Integer modelId = null;
	
	// CWMS-specific fields:
	private String paramType = null;
	private String duration = null;
	private String version = null;
	private String ifMissing = null;
	
	public String getAlgoParmType()
	{
		return algoParmType;
	}
	public void setAlgoParmType(String algoParmType)
	{
		this.algoParmType = algoParmType;
	}
	public String getAlgoRoleName()
	{
		return algoRoleName;
	}
	public void setAlgoRoleName(String algoRoleName)
	{
		this.algoRoleName = algoRoleName;
	}
	public String getInterval()
	{
		return interval;
	}
	public void setInterval(String interval)
	{
		this.interval = interval;
	}
	public int getDeltaT()
	{
		return deltaT;
	}
	public void setDeltaT(int deltaT)
	{
		this.deltaT = deltaT;
	}
	public String getDeltaTUnits()
	{
		return deltaTUnits;
	}
	public void setDeltaTUnits(String deltaTUnits)
	{
		this.deltaTUnits = deltaTUnits;
	}
	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}
	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}
	public Long getSiteId()
	{
		return siteId;
	}
	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}
	public String getSiteName()
	{
		return siteName;
	}
	public void setSiteName(String siteName)
	{
		this.siteName = siteName;
	}
	public Long getDataTypeId()
	{
		return dataTypeId;
	}
	public void setDataTypeId(Long dataTypeId)
	{
		this.dataTypeId = dataTypeId;
	}
	public String getDataType()
	{
		return dataType;
	}
	public void setDataType(String dataType)
	{
		this.dataType = dataType;
	}

	
	public String getTableSelector()
	{
		return tableSelector;
	}
	public void setTableSelector(String tableSelector)
	{
		this.tableSelector = tableSelector;
	}
	public Integer getModelId()
	{
		return modelId;
	}
	public void setModelId(Integer modelId)
	{
		this.modelId = modelId;
	}

	public String getParamType()
	{
		return paramType;
	}
	public void setParamType(String paramType)
	{
		this.paramType = paramType;
	}
	public String getDuration()
	{
		return duration;
	}
	public void setDuration(String duration)
	{
		this.duration = duration;
	}
	public String getVersion()
	{
		return version;
	}
	public void setVersion(String version)
	{
		this.version = version;
	}
	public String getIfMissing()
	{
		return ifMissing;
	}
	public void setIfMissing(String ifMissing)
	{
		this.ifMissing = ifMissing;
	}

	public Long getTsKey()
	{
		return tsKey;
	}

	public void setTsKey(Long tsKey)
	{
		this.tsKey = tsKey;
	}


}
