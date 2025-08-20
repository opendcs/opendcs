/*
*  $Id$
*
*  Open source software
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.10  2004/08/26 13:29:24  mjmaloney
*  Added javadocs
*
*  Revision 1.9  2002/11/02 14:10:09  mjmaloney
*  Network List Entry description must be only a single line when converted
*  to an LRGS-style network list.
*
*  Revision 1.8  2002/08/26 04:53:45  chris
*  Major SQL Database I/O development.
*
*  Revision 1.7  2002/03/14 21:07:44  mike
*  Bug fixes.
*
*  Revision 1.6  2001/11/10 21:17:17  mike
*  *** empty log message ***
*
*  Revision 1.5  2001/11/10 14:55:16  mike
*  Implementing sources & network list editors.
*
*  Revision 1.4  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.3  2001/04/12 12:30:29  mike
*  dev
*
*  Revision 1.2  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.1  2001/03/16 19:53:10  mike
*  Implemented XML parsers for routing specs
*
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
		//this();
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
