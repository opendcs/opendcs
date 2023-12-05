package org.opendcs.odcsapi.beans;

public class ApiSiteName
{
	/** The name component cannot be longer than this */
	public static int MAX_NAME_LENGTH = 64;

	/** The name type -- should match an enum value. */
	private String nameType = null;

	/** The name value.  Case is significant.  */
	private String nameValue = null;
	
	private Long siteId = null;

	public ApiSiteName()
	{
	}

	public ApiSiteName(Long siteId, String nameType, String nameValue)
	{
		super();
		this.siteId = siteId;
		this.nameType = nameType;
		this.nameValue = nameValue;
	}

	public String getNameType()
	{
		return nameType;
	}

	public void setNameType(String nameType)
	{
		this.nameType = nameType;
	}

	public String getNameValue()
	{
		return nameValue;
	}

	public void setNameValue(String nameValue)
	{
		this.nameValue = nameValue;
	}

	public Long getSiteId()
	{
		return siteId;
	}

	public void setSiteId(Long siteId)
	{
		this.siteId = siteId;
	}

 }

