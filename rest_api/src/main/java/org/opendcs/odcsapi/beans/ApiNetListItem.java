package org.opendcs.odcsapi.beans;

/**
 * @author mmaloney
 */
public class ApiNetListItem
{
	/** The Transport Medium ID (eg DCP Address). */
	public String transportId;

	/** The name of the platform (site name). */
	private String platformName = null;

	/** A description */
	private String description = null;
	
	/**
	 * Default ctor required for deserializing data in POST.
	 */
	public ApiNetListItem()
	{
	}

	public ApiNetListItem(String transportId, String platformName, String description)
	{
		super();
		this.transportId = transportId;
		this.platformName = platformName;
		this.description = description;
	}

	public String getTransportId()
	{
		return transportId;
	}

	public void setTransportId(String transportId)
	{
		this.transportId = transportId;
	}

	public String getPlatformName()
	{
		return platformName;
	}

	public void setPlatformName(String platformName)
	{
		this.platformName = platformName;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public static ApiNetListItem fromString(String searchStr)
	{
			String addr = "";
	    	String name = null;
	    	String description = null;

	    	int colon = searchStr.indexOf(':');
	    	// no colon means line just has the address.
	    	addr = colon > 0 ? searchStr.substring(0, colon) : searchStr;
	    	
	    	if (colon <= 0 || searchStr.length() <= colon+1)
	    		return new ApiNetListItem(addr, name, description);

	    	searchStr = searchStr.substring(colon + 1).trim();
			int len = searchStr.length();
			if (len == 0)
	    		return new ApiNetListItem(addr, name, description);

			int ws = 0;
			while (++ws < len && !Character.isWhitespace(searchStr.charAt(ws)))
				;

			if (ws >= len)
				return new ApiNetListItem(addr, name, description);

			name = searchStr.substring(0, ws);
			searchStr = searchStr.substring(ws).trim();
		    
			len = searchStr.length();
			if (len > 0)
				description = searchStr;
			return new ApiNetListItem(addr, name, description);
	}
}
