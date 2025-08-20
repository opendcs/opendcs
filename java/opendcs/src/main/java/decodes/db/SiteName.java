/*
*  $Id$
*/
package decodes.db;

import ilex.util.Logger;

/**
 * This encapsulates information about one of the names of an individual
 * Site.  Each Site can have multiple names, up to one for each known
 * site-name-type.
 * <p>
 *   Each SiteName object has a reference to the Site
 *   which it describes, a String which stores the actual name, and
 *   an EnumValue belonging to the 'SiteNameType' Enum.  The EnumValue
 *   indicates of which of the predefined name types this name is a member.
 * </p>
 * <p>
 *   The case of a SiteName's name is significant, and is preserved by this
 *   object.
 * </p>
 * <p>
 *   Site name values are limited to a maximum length and may not
 *   contain embedded spaces.
 * </p>
 */
public class SiteName extends DatabaseObject
{
	/** The name component cannot be longer than this */
	public static int MAX_NAME_LENGTH = 64;

	/** The name type -- should match an enum value. */
	private String nameType;

	/** The name value.  Case is significant.  */
	private String nameValue;

  	/**
   	* Stores a reference to the Site which this describes.
   	* This can never be null; i.e. you can never have a "dangling" SiteName,
   	* one that doesn't apply to a Site.
   	*/
	public Site site;

  	/**
   	* This is one of the 'SiteNameType' EnumValues.  This cannot be null.
   	* Every SiteName must be associated with a valid name type.
   	*/
	public EnumValue nameTypeEnumValue;

	/**
	 * For USGS site names, this holds the agency code.
	 */
	private String agencyCode;

	/**
	 * For USGS site names, this holds the database number.
	 */
	private String usgsDbno;


  	/**
   	  Construct with a reference to the Site and a name-type.  The name
   	  should be filled in later.  The case of the name-type is not
   	  significant.
   	  Note:  this will validate the name-type with the
   	  'SiteNameType' Enum.  This requires that the EnumList must have been
   	  read from the database prior to constructing any SiteNames.
	  @param s the Site that owns this name
	  @param type the type of this name
   	*/
	public SiteName(Site s, String type)
	{
		site = s;
		nameType = type;
		nameTypeEnumValue = getNameTypeEnumValue(type);
		nameValue = null;
		agencyCode = null;
		usgsDbno = null;
	}

  	/**
   	* Construct with a Site, a name-type and a name.
   	* The case of the name-type is not significant
   	* Note:  this will validate the name-type with the
   	* 'SiteNameType' Enum.  This requires that the EnumList must have been
   	* read from the database prior to constructing any SiteNames.
   	* The case of the name argument is significant, and is preserved by
   	* this object.
	  @param site the owning site
	  @param type the type of the name
	  @param name the value of the name
   	*/
	public SiteName(Site site, String type, String name)
	{
		this(site, type);
		this.nameValue = name;
	}

  	/**
   	  This gets the 'SiteNameType' EnumValue for a given String representation
   	  of a name type.  The case of the argument is not significant.
   	  This simply delegates this request to the SiteList of this SiteName's
   	  database.
	  @return EnumValue for specified SiteName type
   	*/
    public EnumValue getNameTypeEnumValue(String type)
    {
		if (myDatabase == null || myDatabase.siteList == null)
			return null;
        return myDatabase.siteList.getNameTypeEnumValue(type);
    }

  	/**
   	* Return the site-name-type as a String.  Note that this will always
   	* be in lowercase.
	  @return name type
   	*/
    public String getNameType() 
	{
        return nameType;
    }

	/**
	  Sets the name type.
	  @param typ the name type
	*/
	public void setNameType(String typ)
	{
		nameType = typ;
		nameTypeEnumValue = getNameTypeEnumValue(typ);
	}

  	/**
   	* Determines if two SiteName objects are equal.
	  @param ob the other object
   	*/
	public boolean equals(Object ob)
	{
		if (!(ob instanceof SiteName))
			return false;
		SiteName sn = (SiteName)ob;
		if (this == sn)
			return true;

		if (nameType.equalsIgnoreCase(sn.nameType)
		 && getDisplayName().equals(sn.getDisplayName()))
			return true;
		return false;
	}

	/**
	  Sets the name value.
	  @param nm the name value
	*/
	public void setNameValue(String nm)
	{
		if (nm.length() > MAX_NAME_LENGTH)
		{
			String nmt = nm.substring(0, MAX_NAME_LENGTH);
			Logger.instance().log(Logger.E_WARNING, "Site name '"
				+ nm + "' too long, truncating to '" + nmt + "'");
			nameValue = nmt;
		}
		else 
			nameValue = nm;
	}

	/**
	 * @return the name value, or the empty string if none is defined.
	 */
	public String getNameValue()
	{
		return nameValue == null ? "" : nameValue;
	}

	/**
	 * Used for USGS names only, this returns the agency code.
	 */
	public String getAgencyCode()
	{
		return agencyCode;
	}

	/**
	 * Used for USGS names only, this sets the agency code.
	 * @param agencyCode the agency code
	 */
	public void setAgencyCode(String agencyCode)
	{
		this.agencyCode = agencyCode;
	}

	/**
	 * Used for USGS names only, this returns the database number.
	 */
	public String getUsgsDbno()
	{
		return usgsDbno;
	}

	/**
	 * Used for USGS names only, this sets the agency code.
	 * @param usgsDbno the usgs database number
	 */
	public void setUsgsDbno(String usgsDbno)
	{
		this.usgsDbno = usgsDbno;
	}

	/**
	 * @return the Site object associated with this name.
	 */
	public Site getSite() { return site; }

	/**
	 * Sets the Site object associated with this name.
	 * @param site the Site
	 */
	public void setSite(Site site)
	{
		 this.site = site;
	}

  	/**
   	* This overrides the DatabaseObject's getObjectType() method, and
   	* @return 'SiteName'.
   	*/
	public String getObjectType() { return "SiteName"; }

  	/**
   	* @return this as a String.
   	*/
	public String toString()
	{
		return nameType + "=" + nameValue;
	}

	/**
	 * @return displayable name for use in a GUI list of sites.
	 */
	public String getDisplayName()
	{
		if (!nameType.equalsIgnoreCase("usgs"))
			return getNameValue();

		StringBuilder sb = new StringBuilder();
		if (agencyCode != null 
		 && agencyCode.trim().length() > 0
		 && !agencyCode.trim().equalsIgnoreCase("USGS"))
		{
			sb.append(agencyCode);
			sb.append("_");
		}
		sb.append(getNameValue());
		if (usgsDbno != null 
		 && usgsDbno.trim().length() > 0
		 && !usgsDbno.equalsIgnoreCase("01"))
		{
			sb.append("-DB");
			sb.append(usgsDbno);
		}
		return sb.toString();
	}

  	/**
   	* @return this site name in a suitable format for use as a file name.
   	* The format is <type>-<name>, where <type> is the name type and <name>
   	* is the name. Any white space in the type or name will be replaced by
   	* hyphens.
   	*/
    public String makeFileName()
	{
		StringBuilder ret = 
			new StringBuilder(nameType + '-' + getDisplayName());
		int n = ret.length();
		for(int i = 0; i < n; i++)
			if (Character.isWhitespace(ret.charAt(i)))
				ret.setCharAt(i, '-');
		return ret.toString();
	}

  	/**
   	* From DatabaseObject interface,
   	*/
    public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

  	/**
   	* From DatabaseObject interface,
   	*/
    public boolean isPrepared()
	{
		return true;
	}

  	/**
   	* From DatabaseObject interface,
   	*/
    public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

  	/**
   	* This overrides the DatabaseObject's read() method, but this
   	* implementation does nothing.  I/O for this is done by the
   	* SiteList.
   	*/
    public void read()
		throws DatabaseException
	{
	}

  	/**
   	* This overrides the DatabaseObject's write() method, but this
   	* implementation does nothing.  I/O for this is done by the
   	* SiteList.
   	*/
    public void write()
		throws DatabaseException
	{
	}
}

