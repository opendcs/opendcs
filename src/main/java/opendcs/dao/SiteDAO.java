/*
* $Id$
* 
* $Log$
* Revision 1.14  2019/02/25 20:02:55  mmaloney
* HDB 660 Allow Computation Parameter Site and Datatype to be set independently in group comps.
*
* Revision 1.13  2018/02/19 16:25:03  mmaloney
* Do periodic cache maintenance every 2 hours.
* Only pause for 1 sec in the main loop if the data collection was empty.
* (otherwise read the next batch of data immediately).
*
* Revision 1.12  2017/08/22 19:58:40  mmaloney
* Refactor
*
* Revision 1.11  2017/03/31 16:21:20  mmaloney
* Fix for duplicate site names.
*
* Revision 1.10  2017/03/23 16:08:04  mmaloney
* HDB has many orphan site names - so no warning on this.
*
* Revision 1.9  2016/11/29 01:17:42  mmaloney
* Increase cache time to 1 hour. Add debugs.
*
* Revision 1.8  2016/08/05 14:49:26  mmaloney
* No longer put HDB site name in the description field.
*
* Revision 1.7  2016/06/07 21:28:25  mmaloney
* fillCache made public to allow it to be called from ts DAO.
*
* Revision 1.6  2015/07/17 13:19:23  mmaloney
* Guard against null/blank site name.
*
* Revision 1.5  2015/04/14 18:22:20  mmaloney
* Search the entire cache before going to the database in lookupSiteID(String)
*
* Revision 1.4  2015/01/22 19:51:49  mmaloney
* log message improvements
*
* Revision 1.3  2014/07/03 12:47:57  mmaloney
* debug improvements.
*
* Revision 1.2  2014/06/27 20:36:07  mmaloney
* After deleting a site, remove it from the local cache.
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* This software was written by Cove Software, LLC ("COVE") under contract 
* to the United States Government. 
* 
* No warranty is provided or implied other than specific contractual terms
* between COVE and the U.S. Government
* 
* Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
* All rights reserved.
*/
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import opendcs.dai.PropertiesDAI;
import opendcs.dai.SiteDAI;
import opendcs.util.sql.WrappedConnection;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;

public class SiteDAO
	extends DaoBase 
	implements SiteDAI
{
	// MJM Increased from 30 min to 3 hours for 6.4 RC08
	public static final long CACHE_MAX_AGE = 3 * 60 * 60 * 1000L;
	protected static DbObjectCache<Site> cache = new DbObjectCache<Site>(CACHE_MAX_AGE, false);

	public String siteAttributes = 
		"id, latitude, longitude, nearestCity, state, "
		+ "region, timezone, country, elevation, elevUnitAbbr, description";
	public String siteNameAttributes =
		"siteid, nameType, siteName, dbNum, agency_cd";
	public String siteTableName = "Site";
	protected String siteTableKeyColumn = "id";
	
	protected PropertiesDAI propsDao = null;
	protected long lastCacheFillMsec = 0L;
	
	public SiteDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "SiteDao");
		propsDao = new PropertiesSqlDao(tsdb);
		siteNameAttributes = 
			(tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7) ?
				"siteid, nameType, SiteName, dbNum, agency_cd" :
				"siteid, nameType, SiteName";
		// MJM 2016/05/23 Do not add the new fields for HDB.
		if (tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			siteAttributes = siteAttributes + 
				", active_flag, location_type, modify_time, public_name";
	}
	
	@Override
	public void setManualConnection(Connection conn)
	{
		super.setManualConnection(conn);
		propsDao.setManualConnection(conn);
	}
	
	@Override
	public Connection getConnection()
	{
		Connection conn = super.getConnection();
		propsDao.setManualConnection(conn);
		return new WrappedConnection(conn, c -> {});
	}


	@Override
	public synchronized Site getSiteById(DbKey id)
		throws DbIoException, NoSuchObjectException
	{
		if (DbKey.isNull(id))
			return null;
		
		if (cache.size() == 0)
			fillCache();

		Site ret = cache.getByKey(id);
		if (ret != null)
			return ret;

		Site site = new Site();
		site.forceSetId(id);
		readSite(site);
		cache.put(site);
		return site;
	}

	protected void readNames(Site site)
		throws DbIoException, SQLException
	{
		// Next read all Names and then assign them to sites.
		String q = "SELECT " + siteNameAttributes 
			+ " FROM SiteName WHERE siteId = " + site.getId();

		ResultSet rs = doQuery(q);
		while (rs != null && rs.next())
		{
			SiteName sn = new SiteName(site, rs.getString(2),
				rs.getString(3));
			sn.setUsgsDbno(rs.getString(4));
			sn.setAgencyCode(rs.getString(5));
			site.addName(sn);
		}
	}
	
	/**
	  Extract values from result set and put them in the passed Site.
	  @param site the Site object
	  @param rsSite the JDBC Result Set
	*/
	protected void resultSet2Site(Site site, ResultSet rsSite)
		throws SQLException
	{
		site.forceSetId(DbKey.createDbKey(rsSite, 1));
		site.latitude = rsSite.getString(2);
		site.longitude = rsSite.getString(3);
		site.nearestCity = rsSite.getString(4);
		site.state = rsSite.getString(5);
		site.region = rsSite.getString(6);
		site.timeZoneAbbr = rsSite.getString(7);
		site.country = rsSite.getString(8);

		double d = rsSite.getDouble(9);
		if (!rsSite.wasNull())
			site.setElevation(d);
		site.setElevationUnits(rsSite.getString(10));
		site.setDescription(rsSite.getString(11));
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
		{
			site.setActive(TextUtil.str2boolean(rsSite.getString(12)));
			site.setLocationType(rsSite.getString(13));
			site.setLastModifyTime(db.getFullDate(rsSite, 14));
			site.setPublicName(rsSite.getString(15));
		}
	}
	
	@Override
	public synchronized DbKey lookupSiteID(final String nameValue)
		throws DbIoException
	{
		// The 'uniqueName' in the cache will be the preferred site name type
		Site site = cache.getByUniqueName(nameValue);
		if (site != null)
			return site.getKey();

		// If not found, search the cache for any name match
		site = cache.search(
			new Comparable<Site>()
			{
				@Override
				public int compareTo(Site ob)
				{
					for(SiteName sn : ob.getNameArray())
						if (sn.getNameValue().equalsIgnoreCase(nameValue))
							return 0;
					// Note: DbObjectCache.search does a linear search, not a binary search.
					// So always returning -1 meaning 'no match' is okay.
					return -1;
				}
			});
		if (site != null)
			return site.getKey();

		
		// Finally search the database for a SiteName with matching value.
		String q = "select siteid from SiteName "
			+ " where lower(siteName) = " + sqlString(nameValue.toLowerCase());
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			return Constants.undefinedId;
		}
		catch(SQLException ex)
		{
			String msg = "lookupSiteId(str) - Error in query '" 
				+ q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public synchronized DbKey lookupSiteID( final SiteName siteName )
		throws DbIoException
	{
		Site site = cache.search(
			new Comparable<Site>()
			{
				@Override
				public int compareTo(Site ob)
				{
					SiteName obsn = ob.getName(siteName.getNameType());
					if (obsn == null)
						return -1;
					
					return siteName.getNameValue().toLowerCase().compareTo(
						obsn.getNameValue().toLowerCase());
				}
			});
		if (site != null)
			return site.getKey();

		String q = "select siteid from SiteName "
			+ " where lower(nameType) = " + sqlString(siteName.getNameType().toLowerCase())
			+ " and lower(siteName) = "  + sqlString(siteName.getNameValue().toLowerCase());
		try
		{
			ResultSet rs = doQuery(q);
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			return Constants.undefinedId;
		}
		catch(SQLException ex)
		{
			String msg = "lookupSiteId - Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public void fillCache()
		throws DbIoException
	{
		debug3("(Generic)SiteDAO.fillCache()");

		HashMap<DbKey, Site> siteHash = new HashMap<DbKey, Site>();
//		ArrayList<Site> siteList = new ArrayList<Site>();
		int nNames = 0;
		String q = buildSiteQuery(Constants.undefinedId);
		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				Site site = new Site();
				resultSet2Site(site, rs);
				siteHash.put(site.getKey(), site);
				// Can't put in cache because names are not yet known
			}

			q = buildSiteNameQuery(null);
			rs = doQuery(q);
			String prevNameType="", prevNameValue="";
			DbKey prevId = DbKey.NullKey;
			while (rs != null && rs.next())
			{
				DbKey key = DbKey.createDbKey(rs, 1);
				Site site = siteHash.get(key);
				
				if (site == null)
				{
					if (!db.isHdb()) // For some crazy reason, HDB has lots of orphan site names.
						warning("SiteName for id=" + key + ", but no matching site.");
					continue;
				}
				
				// There is an issue in HDB with multiple identical site names pointing to different sites.
				// The HDB site name query orders results by type,value.
				String nameType = rs.getString(2);
				String nameValue = rs.getString(3);
				if (prevNameType.equalsIgnoreCase(nameType) && prevNameValue.equalsIgnoreCase(nameValue))
				{
					warning("SiteName for id=" + key + " with nametype=" + nameType + " and nameValue="
						+ nameValue + " is a duplicate to a name to a different site with id="
							+ prevId + ". Discarding the name for " + key);
				}
				else
				{
					prevNameType = nameType;
					prevNameValue = nameValue;
					prevId = key;
				}
				
				SiteName sn = new SiteName(site, nameType, nameValue);
				sn.setUsgsDbno(rs.getString(4));
				sn.setAgencyCode(rs.getString(5));
				site.addName(sn);
				nNames++;
			}			

		}
		catch(SQLException ex)
		{
			String msg = "fillCache - Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
//		for(Site site : siteList)
		for(Site site : siteHash.values())
			cache.put(site);
		int nProps = 0;
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			nProps = propsDao.readPropertiesIntoCache("site_property", cache);
		debug1("Site Cache Filled: " + cache.size() + " sites, " + nNames
			+ " names, " + nProps + " properties.");
		lastCacheFillMsec = System.currentTimeMillis();
	}
	
	protected String buildSiteNameQuery(Site site)
	{
		String r = "SELECT " + siteNameAttributes 
			+ " FROM SiteName";
		if (site != null)
			r = r + " WHERE siteid = " + site.getKey();
		return r;
	}

	@Override
	public void read(SiteList siteList) throws DbIoException
	{
		if (cache.size() == 0)
			fillCache();
		for(DbObjectCache<Site>.CacheIterator cit = cache.iterator(); cit.hasNext(); )
			siteList.addSite(cit.next());
	}
	
	@Override
	public void readSite(Site site)
		throws DbIoException, NoSuchObjectException
	{
		DbKey id = site.getId();
		String q = buildSiteQuery(id);
		try
		{
			ResultSet rs = doQuery(q);
		
			// Should be either 0 or 1 site returned.
			if (rs == null || !rs.next())
				throw new NoSuchObjectException("No such site with ID=" + id);

			resultSet2Site(site, rs);

			readNames(site);
			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
				propsDao.readProperties("site_property", "site_id", id, site.getProperties());
		}
		catch(SQLException ex)
		{
			String err = "Error in readSite(" + siteTableKeyColumn + "=" + id + "): " + ex;
			failure(err);
			throw new DbIoException(err);
		}
	}
	
	/**
	 * Build a query of the site table.
	 * @param siteId if !null, include in the query.
	 * @return a query string.
	 */
	protected String buildSiteQuery(DbKey siteId)
	{
		String q = "SELECT " + siteAttributes + " FROM " + siteTableName;
		if (siteId != null && !siteId.isNull())
			q = q + " where " + siteTableKeyColumn + " = " + siteId;
			
		return q;
	}
	
	@Override
	public Site getSiteBySiteName(SiteName sn)
		throws DbIoException
	{
		DbKey id = lookupSiteID(sn);
		if (id == null || id.isNull())
			return null;
		
		try { return getSiteById(id); }
		catch (NoSuchObjectException e) { return null; }
	}

	@Override
	public void writeSite(Site newSite) 
		throws DbIoException
	{
		if (!newSite.idIsSet())
		{
			for (Iterator<SiteName> snit = newSite.getNames(); snit.hasNext(); )
			{
				DbKey id = lookupSiteID(snit.next());
				if (id != null && !id.isNull())
				{
					newSite.forceSetId(id);
					break;
				}
			}
		}
		newSite.setLastModifyTime(new Date());
		if (newSite.idIsSet())
		{
			try
			{
				Site dbSite = new Site();
				dbSite.forceSetId(newSite.getId());
				readSite(dbSite);
				cache.remove(dbSite.getId());
				update(newSite, dbSite);
				cache.put(newSite);
			}
			catch(NoSuchObjectException ex)
			{
				newSite.clearId();
			}
		}
		if (!newSite.idIsSet())
		{
			insert(newSite);
			cache.put(newSite);
		}
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			propsDao.writeProperties("site_property", "site_id", 
				newSite.getKey(), newSite.getProperties());
	}

	
	/**
	* Updates an already-existing Site in the database.
	  @param newSite the new Site object to write to the database
	  @param dbSite the site as it currently exists in the database
	*/
	protected void update(Site newSite, Site dbSite)
		throws DbIoException, NoSuchObjectException
	{
		DbKey id = newSite.getId();

		String desc = newSite.getDescription();
		if (desc == null)
			desc = "";
		if (desc.length() > 800)
			desc = desc.substring(0,799);

		String q = "UPDATE site SET " +
		  	"Latitude = " + sqlString(newSite.latitude) + ", " +
		  	"Longitude = " + sqlString(newSite.longitude) + ", " +
		  	"NearestCity = " + sqlString(newSite.nearestCity) + ", " +
		  	"State = " + sqlString(newSite.state) + ", " +
		  	"Region = " + sqlString(newSite.region) + ", " +
		  	"TimeZone = " + sqlString(newSite.timeZoneAbbr) + ", " +
		  	"Country = " + sqlString(newSite.country);
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_6)
			q = q + ", " +
			  	"elevation = " + sqlDouble(newSite.getElevation()) + ", " +
			  	"elevunitabbr = " + sqlString(newSite.getElevationUnits()) + ", " +
				"description = " + sqlString(desc);
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			q = q + ", " +
				"active_flag = " + sqlBoolean(newSite.isActive()) + ", " +
			  	"location_type = " + sqlString(newSite.getLocationType()) + ", " +
			  	"modify_time = " + db.sqlDate(newSite.getLastModifyTime()) + ", " +
			  	"public_name = " + sqlString(newSite.getPublicName());
		
		q = q + " WHERE " + siteTableKeyColumn + " = " + id;

		doModify(q);

		updateAllSiteNames(newSite, dbSite);
		// Now delete & re-insert the SiteNames.
		//deleteSiteNames(s);
		//insertAllSiteNames(s, id);
	}

	/**
	 * Update SiteName records for the given site.
	 *
	 * @param newSite Site object with new information.
	 * @param dbSite Site object as it was from the database.
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	protected void updateAllSiteNames(Site newSite, Site dbSite)
		throws DbIoException, NoSuchObjectException
	{
		//Go through the site names to determine records to insert or update
		for (Iterator<SiteName> newIt = newSite.getNames(); newIt.hasNext(); )
		{
			SiteName newSn = newIt.next();
			SiteName dbSn = dbSite.getName(newSn.getNameType());
			if (dbSn == null)
				insertSiteName(newSite.getKey(), newSn);
			else
			{
				if (!dbSn.equals(newSn))
					updateSiteName(newSite.getKey(), newSn);
				dbSite.removeName(newSn.getNameType());
			}
		}
		
		// Any names left in dbSite don't exist in newSite and should be removed
		for (Iterator<SiteName> dbSnIt = dbSite.getNames(); dbSnIt.hasNext(); )
			doModify("delete from sitename where siteid = " + newSite.getKey()
				+ " and nametype = " + sqlString(dbSnIt.next().getNameType()));
	}
	
	/**
	 * Update a SiteName record
	 * 
	 * @param siteId
	 * @param sn
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	protected void updateSiteName(DbKey siteId, SiteName sn)
		throws DbIoException
	{
		String q =
			"UPDATE SiteName SET " +
				"sitename = " + sqlString(sn.getNameValue());		
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
		{
			q = q + ", dbNum = " + sqlString(sn.getUsgsDbno())
			      + ", agency_cd = " + sqlString(sn.getAgencyCode());
		}

		q = q + " WHERE siteid = " + siteId
			+ " AND lower(nametype) = lower(" + sqlString(sn.getNameType()) + ")";
		doModify(q);
	}

	/**
	* Inserts a new Site into the database.  The Site must not have
	* an SQL Database ID value set already.
	* @param s the Site object
	*/
	protected void insert(Site s)
		throws DbIoException
	{
		// Assign new ID & re-add with ID to the database's collection of sites.
		DbKey id = getKey("Site");
		s.forceSetId(id);
		s.getDatabase().siteList.addSite(s);

		String desc = s.getDescription();
		if (desc != null && desc.length() > 800)
			desc = desc.substring(0,799);

		String q;
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_6)
			q =
				"INSERT INTO Site VALUES (" +
			  	id + ", " +
			  	sqlString(s.latitude) + ", " +
			  	sqlString(s.longitude) + ", " +
			  	sqlString(s.nearestCity) + ", " +
			  	sqlString(s.state) + ", " +
			  	sqlString(s.region) + ", " +
			  	sqlString(s.timeZoneAbbr) + ", " +
			  	sqlString(s.country) +
				")";
		else if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			q =
				"INSERT INTO Site VALUES (" +
			  	id + ", " +
			  	sqlString(s.latitude) + ", " +
			  	sqlString(s.longitude) + ", " +
			  	sqlString(s.nearestCity) + ", " +
			  	sqlString(s.state) + ", " +
			  	sqlString(s.region) + ", " +
			  	sqlString(s.timeZoneAbbr) + ", " +
			  	sqlString(s.country) + ", " +
				sqlDouble(s.getElevation()) + ", " +
				sqlString(s.getElevationUnits()) + ", " +
				sqlString(desc) + " " +
				")";
		else // version 10 or higher
		{
			q = "INSERT INTO Site VALUES (" +
			  	id + ", " +
			  	sqlString(s.latitude) + ", " +
			  	sqlString(s.longitude) + ", " +
			  	sqlString(s.nearestCity) + ", " +
			  	sqlString(s.state) + ", " +
			  	sqlString(s.region) + ", " +
			  	sqlString(s.timeZoneAbbr) + ", " +
			  	sqlString(s.country) + ", " +
				sqlDouble(s.getElevation()) + ", " +
				sqlString(s.getElevationUnits()) + ", " +
				sqlString(desc) + ", " +
				sqlBoolean(s.isActive()) + ", " +
				sqlString(s.getLocationType()) + ", " + 
				db.sqlDate(s.getLastModifyTime()) + ", " + 
				sqlString(s.getPublicName()) + 
				")";
		}


		doModify(q);

		for(Iterator<SiteName> snit = s.getNames(); snit.hasNext(); )
			insertSiteName(s.getKey(), snit.next());
	}

	/**
	* Insert a new SiteName into the database.
	* @param siteId the Site SQL ID
	* @param sn the SiteName object
	*/
	protected void insertSiteName(DbKey siteId, SiteName sn)
		throws DbIoException
	{
		// Ignore null or missing name values. Oracle can't store them.
		if (sn.getNameValue() == null || sn.getNameValue().trim().length() == 0)
			return;
		
		String q =
			"INSERT INTO SiteName VALUES (" +
				siteId + ", " +
				sqlString(sn.getNameType()) + ", " +
				sqlString(sn.getNameValue());
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
		{
			q = q + ", " + sqlString(sn.getUsgsDbno())
			      + ", " + sqlString(sn.getAgencyCode());
		}

		q = q + ")";
		doModify(q);
	}

	@Override
	public void deleteSite(DbKey key) throws DbIoException
	{
		doModify("delete from sitename where siteid = " + key);
		doModify("delete from site_property where site_id = " + key);
		doModify("delete from site where " + siteTableKeyColumn + " = " + key);
		cache.remove(key);
	}
	
	public void close()
	{
		super.close();
		propsDao.close();
	}

	public long getLastCacheFillMsec()
	{
		return lastCacheFillMsec;
	}
}

