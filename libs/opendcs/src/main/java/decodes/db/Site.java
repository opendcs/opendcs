/*
*  $Id$
*/

package decodes.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import opendcs.dao.CachableDbObject;

import ilex.util.HasProperties;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;
import decodes.sql.DbKey;
import decodes.util.DecodesSettings;

/**
 * This class encapsulates a Site, which is an actual, physical location.
 */
public class Site extends IdDatabaseObject
	implements HasProperties, CachableDbObject
{
	/**
	* Static flag -- set in DB editor to prevent platform sites from being
	* added to the list.
	*/
	public static boolean explicitList = false;

	/**
	* The latitude.  This will be the empty string if the latitude is
	* not known.
	*/
	public String latitude = null;

	/**
	* The longitude.  This will be the empty string if the longitude is
	* not known.
	*/
	public String longitude = null;

	/** The Elevation (above mean sea level) of this site. */
	private double elevation = Constants.undefinedDouble;

	/** The units (ft or m) for the elevation (default = ft). */
	private String elevationUnits = "ft";

	/**
	* The string abbreviation of the TimeZone.  This will be the empty
	* string if the time zone is not known.
	*/
	public String timeZoneAbbr = null;

	/**
	* The country.  This will be the empty string if the country is not
	* known.
	*/
	public String country = null;

	/**
	* The state where this Site is located.
	* This will be the empty string if the state is not known.
	*/
	public String state = null;

	/**
	* The name of the nearest city to this Site.
	* This will be the empty string if it is not known.
	*/
	public String nearestCity = null;

	/**
	* The region in which this Site is located.
	* This will be the empty string if the region is not known.
	*/
	public String region = null;

	/// Constant maximum length of free-form description.
	public static final int MAX_DESCRIPTION_LENGTH = 800;

	/**
 	  Free-form description of this site, limited to MAX_DESCRIPTION_LENGTH
	  characters. May contain newlines.
	*/
	private String description = null;
	
	/**
	 * Public display name
	 */
	private String publicName = null;
	  
	/**
	  Used by XML database only. This is the filename from/to which the
	  site was last read/written. It is used by the editor so it can remove
	  old files when a preferred site name has been changed.
	*/
	public String filename;

	/** As of DB version 8 Sites can have properties: */
	private Properties siteProps = new Properties();
	
	// Links
	private ArrayList<SiteName> siteNames;

	/** This flag used by the editor to delete abandoned new platforms. */
	public boolean isNew;
	
	/** DB Version 10 supports making sites inactive */
	private boolean active = true;
	
	/** DB Version 10 supports location types */
	private String locationType = null;

	/** DB Version 10 tracks last modify time for sites */
	private Date lastModifyTime = null;

	/** Construct new empty Site */
	public Site()
	{
		super();  // Sets _id to Constants.undefinedId;

		siteNames = new ArrayList<SiteName>();
		Database db = Database.getDb();
		if (!explicitList && db != null)
			Database.getDb().siteList.addSite(this);
		isNew = false; // Assume this is NOT a new platform.
	}

	/**
	 * Copies info into this site record from the passed site record.
	 * This is called from the import function. It creates a site in the
	 * edit db using the factory. Then this method copies in information
	 * from the imported site.
	 */
	public synchronized void copyFrom(Site rhs)
	{
		latitude = rhs.latitude;
		longitude = rhs.longitude;
		nearestCity = rhs.nearestCity;
		state = rhs.state;
		region = rhs.region;
		timeZoneAbbr = rhs.timeZoneAbbr;
		country = rhs.country;
		elevation = rhs.elevation;
		elevationUnits = rhs.elevationUnits;
		description = rhs.description;
		clearNames();
		for(Iterator<SiteName> it = rhs.getNames(); it.hasNext(); )
		{
			SiteName sn = it.next();
			if (getName(sn.getNameType()) == null)
				addName(sn);
		}
		for(Enumeration<?> pe = rhs.getPropertyNames(); pe.hasMoreElements(); )
		{
			String nm = (String)pe.nextElement();
			setProperty(nm, rhs.getProperty(nm));
		}
	}

	/** 
	  Construct new Site object associated with a particular Platform 
	  @param platform A platform that resides at this site.
	*/
	public Site(Platform platform)
	{
		this();
	}

	/** @return "Site" */
	public String getObjectType() { return "Site"; }

	/** @return description of this site. */
	public synchronized String getDescription() { return description; }


	/** 
	  Sets the description for this site. 
	  @param d the description
	*/
	public synchronized void setDescription(String d) { description = d; } 

	/**
	  From DatabaseObject interface,
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  From DatabaseObject interface,
	  @return true
	*/
	public boolean isPrepared()
	{
		return true;
	}

	/**
	  From DatabaseObject interface,
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	  Adds a name to this Site.
	  Enforces rule that only one name of a given type can exist. If a name
	  of the passed type already exists, it is overwritten.
	  @param sn the site name
	*/
	public synchronized void addName(SiteName sn)
	{		
		SiteName oldsn = getName(sn.getNameType());
		if (oldsn != null)
		{
			siteNames.remove(oldsn);
		}

		sn.site = this;
		siteNames.add(sn);	
	}

	/**
	 * Removes all names defined for this site (for use by editor).
	 */
	public synchronized void clearNames()
	{
		siteNames.clear();
	}

	/**
	  @param type the requested name type
	  @return the name of this site corresponding to a given name-type,
	  or null if no name of the given type exists.
	*/
	public synchronized SiteName getName(String type)
	{
		if (type == null)
			return getPreferredName();
		for(SiteName sn : siteNames)
			if (sn.getNameType().equalsIgnoreCase(type))
				return sn;
		return null;
	}

	/**
	  Returns the site name of the preferred type, as defined in the
	  decodes settings.
	  @return the SiteName of the preferred type, or null if no name defined.
 	*/
	public synchronized SiteName getPreferredName()
	{
		SiteName ret = null;
		String type = DecodesSettings.instance().siteNameTypePreference;
		if (type != null)
			ret = getName(type);
		if (ret == null && siteNames.size() > 0)
			ret = siteNames.get(0);
		return ret;
	}

	/**
	 * Return a name used for display of this site.
	 * Guaranteed not to return null
	 * @return site name suitable for display
	 */
	public synchronized String getDisplayName()
	{
		if (publicName != null)
			return publicName;
		
		SiteName sn = getPreferredName();
		if (sn == null)
			return "unknown";
		return sn.getNameValue();
	}
	
	/**
	 * @param nameValue the name value
	 * @return true if this site has a name with the passed value (case INsensitive)
	 */
	public synchronized boolean hasNameValue(String nameValue)
	{
		for(SiteName sn : siteNames)
			if (sn.getNameValue().equalsIgnoreCase(nameValue))
				return true;
		return false;
	}
	
	public synchronized String getUniqueName()
	{
		SiteName sn = getPreferredName();
		return sn != null ? sn.getNameValue() : null;
	}

	/** @return an iterator into vector of SiteName objects */
	public synchronized Iterator<SiteName> getNames()
	{
		return siteNames.iterator();
	}
	
	/** @return number of names assigned to this site. */
	public synchronized int getNameCount()
	{
		return siteNames.size();
	}

	public synchronized SiteName getNameAt(int idx)
	{
		return idx >= siteNames.size() ? null :
			siteNames.get(idx);
	}

	public synchronized void removeNameAt(int idx)
	{
		SiteName sn = getNameAt(idx);
		if (sn != null)
		{
			siteNames.remove(idx);
//			myDatabase.siteList.rmSiteName(sn);
		}
	}

	/** @return the elevation (above mean sea level) for this site. */
	public synchronized double getElevation() { return elevation; }

	/** 
	  Sets the elevation (above mean sea level) for this site. 
	  @param elev the elevation
	*/
	public synchronized void setElevation(double elev) { elevation = elev; }

	/** @return the elevatation units for this site. */
	public synchronized String getElevationUnits() { return elevationUnits; }

	/** 
	  Set elevation units for this site. 
	  @param eu the units
	*/
	public synchronized void setElevationUnits(String eu) { elevationUnits = eu; }

	/** 
	 * Return the USGS Database Number or null if undefined. 
	 * The USGS DBNO is stored at the end of the USGS Name in
	 * the format ssssssss-DBnn, where ssssssss is the site number and
	 * nn is the DBNO. If it contains no -DB suffix, assume database 01.
	 * @return the USGS Database Number (defaults to "01" if undefined). 
	 */
	public synchronized String getUsgsDbno() 
	{
		SiteName usgsName = getName(Constants.snt_USGS);
		if (usgsName == null)
			return null;
		String dbno = usgsName.getUsgsDbno();
		return dbno == null ? "01" : dbno;
	}

	/** Reads this site from the database. */
	public synchronized void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readSite(this);
	}

	/** Writes this site to the database. */
	public synchronized void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeSite(this);
	}

	/** 
	 * Adds a property to this object's meta-data.
	 * @param name the property name.
	 * @param value the property value.
	 */
	public synchronized void setProperty(String name, String value)
	{
		if (publicName == null && value.equalsIgnoreCase("PUBLIC_NAME"))
			setPublicName(name);
		else
			siteProps.setProperty(name, value);
	}

	/**
	 * Retrieve a property by name.
	 * @param name the property name.
	 * @return value of name property, or null if not defined.
	 */
	public synchronized String getProperty(String name)
	{
		if (name.equalsIgnoreCase("PUBLIC_NAME") && publicName != null)
			return publicName;
		return siteProps.getProperty(name);
	}

	/**
	 * @return enumeration of all names in the property set.
	 */
	public synchronized Enumeration getPropertyNames()
	{
		return siteProps.propertyNames();
	}

	/**
	 * Removes a property assignment.
	 * @param name the property name.
	 */
	public synchronized void rmProperty(String name)
	{
		siteProps.remove(name);
	}

	public synchronized Properties getProperties() { return siteProps; }

	public synchronized String getBriefDescription()
	{
		if (description == null)
			return "";
		int i=0;
		for(; i<description.length() && i < 60; i++)
		{
			char c = description.charAt(i);
			if (c == '.' || c == '\n' || c == '\r')
				break;
		}
		return description.substring(0, i);
	}
	
	public synchronized boolean importEquals(Site os)
	{
		if (os == null)
		{
			Logger.instance().debug3("Imported site is null");
			return false;
		}
		Logger.instance().debug3("Comparing site '" + this.getDisplayName()
			+ "' to imported '" + os.getDisplayName() + "'");
		if (!TextUtil.strEqualIgnoreCase(latitude, os.latitude))
		{
			Logger.instance().debug3("Latitude differs, this='" + latitude
				+ "', that='" + os.latitude + "'");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(longitude, os.longitude))
		{
			Logger.instance().debug3("longitude differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(timeZoneAbbr, os.timeZoneAbbr))
		{
			Logger.instance().debug3("timeZoneAbbr differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(country, os.country))
		{
			Logger.instance().debug3("country differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(state, os.state))
		{
			Logger.instance().debug3("state differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(nearestCity, os.nearestCity))
		{
			Logger.instance().debug3("nearestCity differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(region, os.region))
		{
			Logger.instance().debug3("region differs");
			return false;
		}
		if (!TextUtil.strEqualIgnoreCase(description, os.description))
		{
			Logger.instance().debug3("description differs");
			return false;
		}
		if (!PropertiesUtil.propertiesEqual(siteProps, os.siteProps))
		{
			Logger.instance().debug3("site properties differ");
			return false;
		}
		if (siteNames.size() != os.siteNames.size())
		{
			Logger.instance().debug3("# of site names differs");
			return false;
		}
		for(SiteName sn : siteNames)
		{
			SiteName ossn = os.getName(sn.getNameType());
			if (!TextUtil.strEqualIgnoreCase(sn.getNameValue(), ossn.getNameValue()))
			{
				Logger.instance().debug3("Name type '" +sn.getNameType() + "' differs");
				return false;
			}
				
		}
		return true;
	}

	/** @return the public display name for this site */
	public synchronized String getPublicName()
	{
		return publicName;
	}

	/** Set the public display name for this site */
	public synchronized void setPublicName(String publicName)
	{
		this.publicName = publicName;
	}
	
	/**
	 * Remove the name of the given name type
	 * @param type the name type
	 */
	public synchronized void removeName(String type)
	{
		for(int idx = 0; idx < siteNames.size(); idx++)
			if (siteNames.get(idx).getNameType().equalsIgnoreCase(type))
			{
				siteNames.remove(idx);
				return;
			}
	}

	public synchronized boolean isActive()
	{
		return active;
	}

	public synchronized void setActive(boolean active)
	{
		this.active = active;
	}

	public synchronized String getLocationType()
	{
		return locationType;
	}

	public synchronized void setLocationType(String locationType)
	{
		this.locationType = locationType;
	}

	public synchronized Date getLastModifyTime()
	{
		return lastModifyTime;
	}

	public synchronized void setLastModifyTime(Date lastModifyTime)
	{
		this.lastModifyTime = lastModifyTime;
	}
	
	public synchronized ArrayList<SiteName> getNameArray() { return siteNames; }
}
