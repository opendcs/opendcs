/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.db;

import ilex.util.TextUtil;

/*
This class holds a single entry in a NetworkList. It is modeled on the
LRGS Network List.
*/
public class NetworkListEntry extends DatabaseObject
{
	/** The Transport Medium ID (eg DCP Address). */
	public String transportId;

	/** The name of the platform (site name). */
	private String platformName = null;

	/** A description */
	private String description = null;

	/**
	 * This is a reference to the NetworkList to which this belongs.
	 * This should never be null.
	 */
	public NetworkList parent;

	/** 
	  Constructor.  
	  @param parent the owning network list
	  @param transportId ID for this entry
	*/
	public NetworkListEntry(NetworkList parent, String transportId)
	{
		setPlatformName(null);
		setDescription(null);

		this.parent = parent;
		this.transportId = transportId;
	}

	/**
	 * Makes a new copy of this object for storage in the passed netlist.
	  @param parent list that is to own the copy
	  @return copy of this entry
	 */
	public NetworkListEntry copy(NetworkList parent)
	{
		NetworkListEntry ret = new NetworkListEntry(parent, transportId);
		ret.setPlatformName(platformName);
		ret.setDescription(description);
		return ret;
	}

	public boolean equals(Object ob)
	{
		if (!(ob instanceof NetworkListEntry))
			return false;
		NetworkListEntry nle = (NetworkListEntry)ob;
		if (this == ob)
			return true;

		if (!TextUtil.strEqualIgnoreCase(transportId, nle.transportId)
		 || !TextUtil.strEqualIgnoreCase(getPlatformName(), nle.getPlatformName())
		 || !TextUtil.strEqualIgnoreCase(getDescription(), nle.getDescription()))
			return false;
		return true;
	}

	/** @return "NetworkListEntry" */
	public String getObjectType() { return "NetworkListEntry"; }

	/**
	  From DatabaseObject interface,
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  From DatabaseObject interface,
	  @return false
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  From DatabaseObject interface,
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/** Does nothing. */
	public void read()
		throws DatabaseException
	{
		// IO handled by NetworkList
	}

	/** Does nothing. */
	public void write()
		throws DatabaseException
	{
		// IO handled by NetworkList
	}

	public String getTransportId()
	{
		return transportId;
	}

	/**
	 * @return the platformName
	 */
	public String getPlatformName()
	{
		return platformName;
	}

	/**
	 * @param platformName the platformName to set
	 */
	public void setPlatformName(String platformName)
	{
		this.platformName = platformName;
	}

	/**
	 * @return the description
	 */
	public String getDescription()
	{
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description)
	{
		this.description = description;
	}
}
