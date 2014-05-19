/*
* $Id$
* 
* $Log$
*/
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import opendcs.dai.SiteDAI;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.util.DecodesSettings;

/**
This class is a helper to the TempestTsdb for reading & writing sites & names.
*/
public class SiteDAO
	extends DaoBase 
	implements SiteDAI
{
	protected static DbObjectCache<Site> cache = new DbObjectCache<Site>(15 * 60 * 1000L, false);

	public String siteAttributes = 
		"id, latitude, longitude, nearestCity, state, "
		+ "region, timezone, country, elevation, elevUnitAbbr, description";
	public String siteNameAttributes =
		"siteid, nameType, siteName, dbNum, agency_cd";
	public String siteTableName = "Site";
	protected String siteTableKeyColumn = "id";
	
	protected PropertiesSqlDao propsDao = null;
	
	public SiteDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "SiteDao");
		propsDao = new PropertiesSqlDao(tsdb);
		siteNameAttributes = 
			(tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7) ?
				"siteid, nameType, SiteName, dbNum, agency_cd" :
				"siteid, nameType, SiteName";
		if (tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
			siteAttributes = siteAttributes + 
				", active_flag, location_type, modify_time, public_name";
	}

	@Override
	public synchronized Site getSiteById(DbKey id)
		throws DbIoException, NoSuchObjectException
	{
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
	public synchronized DbKey lookupSiteID(String nameValue)
		throws DbIoException
	{
		Site site = cache.getByUniqueName(nameValue);
		if (site != null)
			return site.getKey();
		
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
	
	protected void fillCache()
		throws DbIoException
	{
		ArrayList<Site> siteList = new ArrayList<Site>();
		int nNames = 0;
		String q = buildSiteQuery(Constants.undefinedId);
		try
		{
			ResultSet rs = doQuery(q);
			while (rs != null && rs.next())
			{
				Site site = new Site();
				resultSet2Site(site, rs);
				siteList.add(site);
				// Can't put in cache because names are not yet known
			}

			q = "SELECT " + siteNameAttributes + " FROM SiteName";
			rs = doQuery(q);
			while (rs != null && rs.next())
			{
				DbKey key = DbKey.createDbKey(rs, 1);
				Site site = null;
				for(Site s : siteList)
					if (key.equals(s.getKey()))
					{
						site = s;
						break;
					}
				if (site == null)
				{
					warning("SiteName for id=" + key + ", but no matching site.");
					continue;
				}
				SiteName sn = new SiteName(site, rs.getString(2), rs.getString(3));
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
		for(Site site : siteList)
			cache.put(site);
		int nProps = 0;
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			nProps = propsDao.readPropertiesIntoCache("site_property", cache);
		info("Site Cache Filled: " + cache.size() + " sites, " + nNames
			+ " names, " + nProps + " properties.");
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
			q = q + " where id = " + siteId;
			
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
		if (DecodesSettings.instance().hdbSiteDescriptions && desc.indexOf("\n") == -1)
		{
			String sn = newSite.getDisplayName();
			if (desc == null)
				desc = sn + "\n";
			else
				desc = sn + "\n" + desc;
		}
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
	 * Update Sitename records for the given site
	 * @param newSite
	 * @param siteId
	 * @throws DatabaseException
	 * @throws SQLException
	 */
	private void updateAllSiteNames(Site newSite, Site dbSite)
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
	private void updateSiteName(DbKey siteId, SiteName sn)
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
		if (DecodesSettings.instance().hdbSiteDescriptions
		 && (desc == null || desc.indexOf("\n") == -1))
		{
			String sn = s.getDisplayName();
			if (desc == null)
				desc = sn + "\n";
			else
				desc = sn + "\n" + desc;
		}
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
	}
	
	public void close()
	{
		super.close();
		propsDao.close();
	}
}

