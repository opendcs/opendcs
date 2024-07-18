package opendcs.dai;

import org.python.bouncycastle.util.BigIntegers.Cache;

import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dao.CachableDbObject;

/**
 * Defines public interface for reading/writing site (i.e. location) objects.
 * @author mmaloney - Mike Maloney, Cove Software, LLC
 */
public interface SiteDAI
	extends DaiBase
{
	/**
	 * Return the site corresponding to the passed surrogate key. If not found,
	 * throw NoSuchObjectException
	 * @param id the surrogate key
	 * @return the Site object
	 * @throws DbIoException if SQL error
	 * @throws NoSuchObjectException if no matching site found
	 */
	public Site getSiteById(DbKey id)
		throws DbIoException, NoSuchObjectException;
	
	/**
	 * Find the best match for the passed name value. First look for a match
	 * with the preferred name type. If not found, find any matching name type.
	 * @param nameValue the value of the name
	 * @return the surrogate key of the site or Constants.undefinedId if not found.
	 * @throws DbIoException on SQL error
	 */
	public DbKey lookupSiteID(String nameValue)
		throws DbIoException;
	
	/**
	 * Given a site name, return the database's surrogate key ID.
	 * @param siteName the site name
	 * @return the ID corresponding to the passed name, or Constants.undefinedId if not found.
	 * @throws DbIoException on Database IO error.
	 */
	public DbKey lookupSiteID( final SiteName siteName )
		throws DbIoException;

	/**
	 * Read all sites into the DECODES site list.
	 * @param siteList the DECODES site list
	 * @throws DbIoException on Database IO error.
	 */
	public void read(SiteList siteList)
		throws DbIoException;
	
	/**
	 * For DECODES, read the site info into the passed object.
	 * This forces a read from the database -- never from cache.
	 * @param site The site which must contain an ID.
	 */
	public void readSite(Site site)
		throws DbIoException, NoSuchObjectException;

	/**
	 * Return site matching the passed SiteName
	 * @param sn The site name
	 * @return site matching the passed SiteName or null if not found
	 */
	public Site getSiteBySiteName(SiteName sn)
		throws DbIoException;

	/**
	 * Writes a site to the database
	 * @param s the site
	 * @throws DbIoException on SQL errors
	 */
	public void writeSite(Site s)
		throws DbIoException;
	
	/**
	 * Deletes the site and all subordinate records with the passed key
	 * @param key the key
	 */
	public void deleteSite(DbKey key)
		throws DbIoException;

	/**
	 * Closes any resources opened by the DAO
	 */
	public void close();

	/**
	 * Fills the cache of all known sites in an efficient manner.
	 * @throws DbIoException
	 */
	public void fillCache() throws DbIoException;

	default void fillCache(org.opendcs.database.DbObjectCache<Site> cache) throws DbIoException
	{
		// default do nothing.
	}
	
	/**
	 * @return the msec time that the cache was last filled, or 0 if never.
	 */
	public long getLastCacheFillMsec();


}
