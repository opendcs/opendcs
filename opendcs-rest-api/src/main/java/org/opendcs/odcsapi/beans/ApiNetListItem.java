/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
