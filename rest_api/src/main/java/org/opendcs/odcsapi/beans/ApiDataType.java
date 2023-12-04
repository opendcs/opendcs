package org.opendcs.odcsapi.beans;

public class ApiDataType
{
	private Long id = null;
	
	/**
	* A string defining the data-type standard.
	* This must match one of the DataTypeStandard enum values.
	* Currently the allowable values are SHEF-PE, NOS-CODE, or EPA-CODE.
	*/
	private String standard = null;

	/**
	* This identifies the data type.  The form of this string depends on
	* the standard.
	*/
	private String code = null;
	
	private String displayName = null;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getStandard()
	{
		return standard;
	}

	public void setStandard(String standard)
	{
		this.standard = standard;
	}

	public String getCode()
	{
		return code;
	}

	public void setCode(String code)
	{
		this.code = code;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public void setDisplayName(String displayName)
	{
		this.displayName = displayName;
	}


}
