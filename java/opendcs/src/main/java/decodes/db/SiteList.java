/*
*  $Id$
*  
*  Open Source software
*  
*  $Log$
*  Revision 1.8  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.db;

import java.util.Vector;
import java.util.Iterator;

import decodes.sql.DbKey;

/**
SiteList is a collection of all known Site objects.
It provides methods for search for sites given a specific site name.
Note that each Site can have multiple SiteNames, up to one of each
known site-name-type.
*/
public class SiteList extends DatabaseObject
{
	/**
	* This stores the list of Sites.  Note that
	* Sites are stored in both a Vector and a HashMap.
	*/
	private Vector<Site> siteVec;

	/**
	* This stores a reference to this Database's 'SiteNameType' Enum.
	* This is used by the getNameTypeEnumValue() method to get the
	* EnumValue corresponding to a site name type.
	* This will be null until its needed -- the EnumList must have been
	* read from the database before that method is called.
	*/
	private DbEnum _siteNameTypeEnum;
	
	private boolean _wasRead = false;
	public boolean wasRead() { return _wasRead; }


	/** Default Constructor.  */
	public SiteList()
	{
		siteVec = new Vector<Site>();
		_siteNameTypeEnum = null;
	}

	/**
	* This overrides the DatabaseObject's getObjectType() method.
	* @return 'SiteList'.
	*/
	public String getObjectType() { return "SiteList"; }

	/**
	* Adds a site object to the collection.
	*/
	public void addSite(Site newSite)
	{
		// Add to vector if not already present.
		for(Site existingSite : siteVec)
		{
			if (newSite == existingSite)
				return;
			if (newSite.getId() != Constants.undefinedId
			 && newSite.getId() == existingSite.getId())
			{
//Logger.instance().debug3("Replacing existing site with id=" + newSite.getId()
//+ ", " + existingSite.getDisplayName());
				siteVec.remove(existingSite);
				break;
			}
		}
		siteVec.add(newSite);
//Logger.instance().info(
//"Added site id=" + newSite.getId() + ", "+ newSite.getDisplayName());
	}

	/**
	  Removes a site from the list.
	  @param site the Site to remove
	*/
	public void removeSite(Site site)
	{
		siteVec.remove(site);
	}

	/**
	* This gets the 'SiteNameType' EnumValue for a given String representation
	* of a name type.  The case of the argument is not sensitive.
	* Note that the EnumList must have been read from the Database prior to
	* calling this, or the consequences will be dire.
	  @param type the name type
	  @return The EnumValue or null if no match.
	*/
	public EnumValue getNameTypeEnumValue(String type)
	{
		if (_siteNameTypeEnum == null) 
		{
			_siteNameTypeEnum = myDatabase.getDbEnum("SiteNameType");
		}

		if (_siteNameTypeEnum != null)
		  return _siteNameTypeEnum.findEnumValue(type);
		else
			return null;
	}


	/**
	* @return a Site from its SiteName or null if no match is found.
	*/
	public Site getSite(SiteName sn)
	{
		String nt = sn.getNameType();
		String dn = sn.getDisplayName();
		for(Site testsite : siteVec)
		{
			SiteName testname = testsite.getName(nt);
			if (testname != null 
			 && dn.equalsIgnoreCase(testname.getDisplayName()))
				return testsite;
		}
		return null;
	}

	/**
		Get a Site, given its name-type and name. The case of the name-type
		is not significant.
		@param nameType the type of the SiteName object
		@param name the name-value of the SiteName object
		@return Site or null if no match
	*/
	public Site getSite(String nameType, String name)
	{
		nameType = nameType.toLowerCase();
		name = name.toLowerCase();
		for (Iterator<Site> it = iterator(); it.hasNext(); )
		{
			Site s = it.next();
			SiteName sn = s.getName(nameType);
			if ( sn.getNameValue().equals(name) )
			{
				return(s);
			}
		}
		return ((Site)null);
	}

	/**
	  Returns a site by its ID. Returns null if no match found.
	  @param id the ID
	  @return Site or null if no match
	*/
	public Site getSiteById(DbKey id)
	{
		for(Site site : siteVec)
			if (id.equals(site.getId()))
				return site;
		return null;
	}
	
	/**
	* @return number of sites in the collection
	*/
	public int size()
	{
		return siteVec.size();
	}

	/**
	* @return iterator into collection of sites.
	*/
	public Iterator<Site> iterator()
	{
		return siteVec.iterator();
	}

	/**
	  From DatabaseObject
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	  From DatabaseObject
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	  From DatabaseObject
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* Read all of the Sites from the database.
	*/

	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readSiteList(this);
		_wasRead = true;
//System.out.println("Read Site List: " + size() + " entries.");
	}

	public void write()
		throws DatabaseException
	{
		for(Site site : siteVec)
			site.write();
	}


	public void clear()
	{
		siteVec.clear();
	}
}

