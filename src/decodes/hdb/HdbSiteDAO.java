package decodes.hdb;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.SiteDAO;

public class HdbSiteDAO extends SiteDAO
{
	private String joinClause = null, filterClause = null;
	private String siteNameTable = "HDB_EXT_SITE_CODE a, HDB_EXT_SITE_CODE_SYS b, enum e, enumvalue ev";
	private String siteNameJoin = "a.EXT_SITE_CODE_SYS_ID = b.EXT_SITE_CODE_SYS_ID "
		+ "AND b.EXT_SITE_CODE_SYS_NAME = ev.ENUMVALUE and e.ID = ev.ENUMID and lower(e.NAME) = 'sitenametype'";
	private String siteNameKey = "a.HDB_SITE_ID";
	
	private static HashMap<String, DbKey> siteName2ExtSysId = new HashMap<String, DbKey>();
	private static HashMap<String, DbKey> stateName2Id = new HashMap<String, DbKey>();
	private static HashMap<String, DbKey> stateAbbr2Id = new HashMap<String, DbKey>();
	private static String myDbSiteCode = null;
	
	public HdbSiteDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb);
		siteTableName = "HDB_SITE a, DECODES_SITE_EXT b, HDB_OBJECTTYPE c";
		
		joinClause = "a.site_id = b.site_id "
			+ "and a.objecttype_id = c.objecttype_id";
		
		// Order the attributes so they match the list in SiteDAO.java:
		siteAttributes = 
			"a.site_id, a.lat, a.longi, b.nearestcity, b.state, b.region, b.timezone, b.country, a.elevation,"
			+ " b.elevunitabbr, a.description, 'Y', c.objecttype_name, null, a.site_common_name";
		
		siteTableKeyColumn = "a.site_id";
		
		siteNameAttributes = "a.HDB_SITE_ID, b.EXT_SITE_CODE_SYS_NAME, a.PRIMARY_SITE_CODE, null, null";
		
		this.module = "HdbSiteDAO";
		
		if (siteName2ExtSysId.size() == 0)
		{
			synchronized(siteName2ExtSysId)
			{
				String q = "select b.ext_site_code_sys_name, b.ext_site_code_sys_id "
					+ "from hdb_ext_site_code_sys b, enum e, enumvalue ev "
					+ "where b.EXT_SITE_CODE_SYS_NAME = ev.ENUMVALUE "
					+ "and e.ID = ev.ENUMID "
					+ "and lower(e.NAME) = 'sitenametype'";
				
				try
				{
					ResultSet rs = doQuery(q);
					while(rs != null && rs.next())
					{
						siteName2ExtSysId.put(rs.getString(1).toLowerCase(), DbKey.createDbKey(rs, 2));
					}
					Logger.instance().info(module + " read " + siteName2ExtSysId.size() 
						+ " possible site name types.");
				}
				catch (Exception ex)
				{
					Logger.instance().failure(module + " error in '" + q + "': " + ex);
				}
			}
		}
		
		if (stateName2Id.size() == 0)
		{
			synchronized(stateName2Id)
			{
				String q = "select STATE_ID, STATE_CODE, STATE_NAME from HDB_STATE";
				try
				{
					ResultSet rs = doQuery(q);
					while(rs != null && rs.next())
					{
						DbKey id = DbKey.createDbKey(rs, 1);
						stateAbbr2Id.put(rs.getString(2).toLowerCase(), id);
						stateName2Id.put(rs.getString(3).toLowerCase(), id);
					}
					Logger.instance().info(module + " read " + stateAbbr2Id.size() 
						+ " states.");
				}
				catch (Exception ex)
				{
					Logger.instance().failure(module + " error in '" + q + "': " + ex);
				}
			}
		}
		
		if (myDbSiteCode == null)
		{
			synchronized(stateName2Id)
			{
				String q = "select db_site_code from ref_db_list where session_no = 1";
				try
				{
					ResultSet rs = doQuery(q);
					if (rs != null && rs.next())
					{
						myDbSiteCode = rs.getString(1);
						Logger.instance().info(module + " myDbSiteCode='" + myDbSiteCode + "'");
					}
					else
					{
						Logger.instance().warning(module + " No results for '" + q + "' -- defaulting myDbSiteCode to 'ECO'");
						myDbSiteCode = "ECO";
					}
				}
				catch (Exception ex)
				{
					Logger.instance().failure(module + " error in '" + q + "': " + ex);
				}
			}
		}
		filterClause = "a.db_site_code = " + sqlString(myDbSiteCode);

	}
	
	@Override
	protected void resultSet2Site(Site site, ResultSet rsSite)
		throws SQLException
	{
		// Base class handles all the normal attributes, which we've put in the proper order above.
		super.resultSet2Site(site, rsSite);
		// Now add the "hdb" site name as the surrogate key
		site.addName(new SiteName(site, "hdb", "" + site.getKey()));
	}
	
	@Override
	protected String buildSiteQuery(DbKey siteId)
	{
		String q = "SELECT " + siteAttributes + " FROM " + siteTableName
			+ " where " + joinClause;
		
		if (!DbKey.isNull(siteId))
			q = q + " and a.site_id = " + siteId;
		else // querying all sites, must add filter to only get session_no = 1.
			q = q + " and " + filterClause;
			
		return q;
	}
	
	@Override
	protected void readNames(Site site)
		throws DbIoException, SQLException
	{
		// Next read all Names and then assign them to site.
		String q = buildSiteNameQuery(site);

		ResultSet rs = doQuery(q);
		while (rs != null && rs.next())
		{
			SiteName sn = new SiteName(site, rs.getString(2),
				rs.getString(3));
			site.addName(sn);
		}
	}
	
	@Override
	public synchronized DbKey lookupSiteID( final SiteName siteName )
		throws DbIoException
	{
		String q = basicSiteNameQuery(null) 
			+ " AND lower(b.EXT_SITE_CODE_SYS_NAME) = " + sqlString(siteName.getNameType().toLowerCase())
			+ " AND a.PRIMARY_SITE_CODE = " + sqlString(siteName.getNameValue());
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
	public synchronized DbKey lookupSiteID(final String nameValue)
		throws DbIoException
	{
debug3("HdbSiteDAO.lookupSiteID(" + nameValue + ") cache has " + cache.size() + " sites.");
		// The 'uniqueName' in the cache will be the preferred site name type
		Site site = cache.getByUniqueName(nameValue);
		if (site != null)
			return site.getKey();
debug3("HdbSiteDAO.lookupSiteID -- no match to any unique primary site name in cache.");

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
debug3("HdbSiteDAO.lookupSiteID -- no match to any site name in cache.");

		// HDB Users often use surrogate key as a site name.
		try
		{
			long key = Long.parseLong(nameValue);
			DbKey dbKey = DbKey.createDbKey(key);
			site = getSiteById(dbKey);
			if (site != null)
				return dbKey;
		}
		catch(NumberFormatException ex)
		{
			debug3("lookupSiteID name value '" + nameValue + "' is not a site ID.");
		}
		catch (NoSuchObjectException e)
		{
		}
		debug3("lookupSiteID name value '" + nameValue + "' is does not match any site ID.");
		
		// Finally search the database for a SiteName with matching value.
		String q = basicSiteNameQuery(null);
		q = q + " and lower(a.PRIMARY_SITE_CODE) = " + sqlString(nameValue.toLowerCase());
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

	private String basicSiteNameQuery(Site site)
	{
		String r = "SELECT " + siteNameAttributes 
			+ " FROM " + siteNameTable
			+ " WHERE " + siteNameJoin;
		if (site != null)
			r = r + " AND " + siteNameKey + " = " + site.getKey();
		return r;

	}
	
	@Override
	protected String buildSiteNameQuery(Site site)
	{
		String r = basicSiteNameQuery(site);
		r = r + " order by b.EXT_SITE_CODE_SYS_NAME, a.PRIMARY_SITE_CODE";
		return r;
	}

	@Override
	protected void update(Site newSite, Site dbSite)
		throws DbIoException, NoSuchObjectException
	{
//		String desc = newSite.getDescription();
//		if (desc == null)
//			desc = "";
//		if (DecodesSettings.instance().hdbSiteDescriptions && desc.indexOf("\n") == -1)
//		{
//			String sn = newSite.getDisplayName();
//			if (desc == null)
//				desc = sn + "\n";
//			else
//				desc = sn + "\n" + desc;
//		}
//		if (desc.length() > 800)
//			desc = desc.substring(0,799);
		
		ArrayList<String> sets = new ArrayList<String>();
		if (!strEqual(newSite.latitude, dbSite.latitude))
			sets.add("LAT = " + sqlString(newSite.latitude));
		if (!strEqual(newSite.longitude, dbSite.longitude))
			sets.add("LONGI = " + sqlString(newSite.longitude));
		if (newSite.getElevation() != dbSite.getElevation())
			sets.add("elevation = " + sqlDouble(newSite.getElevation()));
		if (!strEqual(newSite.getDescription(), dbSite.getDescription()))
			sets.add("description = " + sqlString(newSite.getDescription()));
		if (!strEqual(newSite.getPublicName(), dbSite.getPublicName()))
			sets.add("site_common_name = " + sqlString(newSite.getPublicName()));

		if (sets.size() > 0)
		{
			String q = "UPDATE HDB_SITE SET ";
			for(int idx = 0; idx < sets.size(); idx++)
			{
				q = q + sets.get(idx);
				if (idx < sets.size() - 1)
					q = q + ", ";
			}
			q = q + " where site_id = " + newSite.getKey();
				
			doModify(q);
		}
		sets.clear();
		
		if (!strEqual(newSite.nearestCity, dbSite.nearestCity))
			sets.add("nearestCity = " + sqlString(newSite.nearestCity));
		if (!strEqual(newSite.state, dbSite.state))
			sets.add("state = " + sqlString(newSite.state));
		if (!strEqual(newSite.region, dbSite.region))
			sets.add("region = " + sqlString(newSite.region));
		if (!strEqual(newSite.timeZoneAbbr, dbSite.timeZoneAbbr))
			sets.add("timezone = " + sqlString(newSite.timeZoneAbbr));
		if (!strEqual(newSite.country, dbSite.country))
			sets.add("country = " + sqlString(newSite.country));
		if (!strEqual(newSite.getElevationUnits(), dbSite.getElevationUnits()))
			sets.add("elevunitabbr = " + sqlString(newSite.getElevationUnits()));
	
		if (sets.size() > 0)
		{
			String q = "UPDATE DECODES_SITE_EXT SET ";
			for(int idx = 0; idx < sets.size(); idx++)
			{
				q = q + sets.get(idx);
				if (idx < sets.size() - 1)
					q = q + ", ";
			}
			q = q + " where site_id = " + newSite.getKey();
				
			doModify(q);
		}

		updateAllSiteNames(newSite, dbSite);
	}

	@Override
	protected void updateAllSiteNames(Site newSite, Site dbSite)
		throws DbIoException, NoSuchObjectException
	{
		//Go through the site names to determine records to insert or update
		for (Iterator<SiteName> newIt = newSite.getNames(); newIt.hasNext(); )
		{
			SiteName newSn = newIt.next();
			
			// Ignore HDB site names, which are really surrogate keys
			if (newSn.getNameType().equalsIgnoreCase("hdb"))
				continue;
			
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
		{
			SiteName sn = dbSnIt.next();
			
			// hdb names are the surrogate keys, not actually saved as names.
			if (sn.getNameType().equalsIgnoreCase("hdb"))
				continue;
			
			DbKey sysId = siteName2ExtSysId.get(sn.getNameType().toLowerCase());
			if (sysId == null)
			{
				Logger.instance().warning(module + " site name '" + sn + "' has invalid name type");
				continue;
			}
			doModify("delete from hdb_ext_site_code "
				+ "where hdb_site_id = " + newSite.getKey()
				+ " and ext_site_code_sys_id = " + sysId);
		}
	}


	@Override
	protected void updateSiteName(DbKey siteId, SiteName sn)
		throws DbIoException
	{
		DbKey sysId = siteName2ExtSysId.get(sn.getNameType().toLowerCase());
		if (sysId == null)
		{
			Logger.instance().warning(module + " cannot save invalid name type '" + sn.getNameType() + "'");
			return;
		}
		
		String q =
			"UPDATE HDB_EXT_SITE_CODE SET " +
				"primary_site_code = " + sqlString(sn.getNameValue())
				+ " where hdb_site_id = " + siteId
				+ " and ext_site_code_sys_id = " + sysId;
		doModify(q);
	}

	@Override
	protected void insert(Site s)
		throws DbIoException
	{
		// Assign new ID & re-add with ID to the database's collection of sites.
		DbKey id = getKey("Site");
		s.forceSetId(id);
		s.getDatabase().siteList.addSite(s);

		String desc = s.getDescription();
		String dispName = null;
		if (desc != null && desc.trim().length() > 0)
			dispName = TextUtil.getFirstLine(desc.trim());
		else if (s.getPublicName() != null)
			dispName = s.getPublicName();
		else 
			dispName = s.getPreferredName().getNameValue();
		
		String commonName = s.getPublicName();
		if (commonName == null)
			commonName = dispName;
		DbKey stateId = s.state != null ? stateName2Id.get(s.state) : null;
		String sid = stateId == null ? "null" : stateId.toString();
		
		String q =
			"INSERT INTO HDB_SITE(SITE_ID, SITE_NAME, SITE_COMMON_NAME, OBJECTTYPE_ID, STATE_ID, "
			+ "LAT, LONGI, ELEVATION, DESCRIPTION, DB_SITE_CODE) VALUES ("
		  	+ id + ", " 
			+ sqlString(dispName) + ", "
			+ sqlString(commonName) + ", "
			+ "9, "
			+ sid + ", "
			+ sqlString(s.latitude) + ", "
		  	+ sqlString(s.longitude) + ", "
			+ sqlDouble(s.getElevation()) + ", "
			+ sqlString(desc) + ", "
			+ sqlString(myDbSiteCode)
			+ ")";
		doModify(q);
		
		// NOTE: There is a trigger on HDB_SITE that will create the DECODES_SITE_EXT
		// record. So do an update even though this is a new site.
		q = "UPDATE DECODES_SITE_EXT "
			+ "SET NEARESTCITY = " + sqlString(s.nearestCity) + ", "
			+ "STATE = " + sqlString(s.state) + ", "
			+ "REGION = " + sqlString(s.region) + ", "
			+ "TIMEZONE = " + sqlString(s.timeZoneAbbr) + ", "
			+ "COUNTRY = " + sqlString(s.country) + ", "
			+ "ELEVUNITABBR = " + sqlString(s.getElevationUnits())
			+ " WHERE SITE_ID = " + id;
		doModify(q);
 
		for(Iterator<SiteName> snit = s.getNames(); snit.hasNext(); )
			insertSiteName(s.getKey(), snit.next());
	}

	@Override
	protected void insertSiteName(DbKey siteId, SiteName sn)
		throws DbIoException
	{
		// Ignore null or missing name values. Oracle can't store them.
		if (sn.getNameValue() == null || sn.getNameValue().trim().length() == 0
		 || sn.getNameType() == null)
			return;
		
		DbKey sysId = siteName2ExtSysId.get(sn.getNameType().toLowerCase());
		if (sysId == null)
		{
			Logger.instance().warning(module + " cannot save name type '" + sn + "' invalid name type '"
				+ sn.getNameType() + "'");
			return;
		}
		
		String q = "INSERT INTO HDB_EXT_SITE_CODE(EXT_SITE_CODE_SYS_ID, PRIMARY_SITE_CODE, HDB_SITE_ID)"
			+ " VALUES ("
			+ sysId + ", " + sqlString(sn.getNameValue()) + ", " + siteId + ")";
		doModify(q);
	}

	@Override
	public void deleteSite(DbKey key) throws DbIoException
	{
		doModify("delete from HDB_EXT_SITE_CODE where HDB_SITE_ID = " + key);
		doModify("delete from site_property where site_id = " + key);
		doModify("delete from DECODES_SITE_EXT where SITE_ID = " + key);
		doModify("delete from HDB_EXT_SITE_CODE where HDB_SITE_ID = " + key);
		cache.remove(key);
	}
	
	// String compare, but consider blank strings the same as null.
	private boolean strEqual(String s1, String s2)
	{
		if (s1 == null || s1.length() == 0)
		{
			if (s2 == null || s2.length() == 0)
				return true;
			return false;
		}
		else if (s2 == null || s2.length() == 0)
			return false;
		return s1.equals(s2);
	}
}
