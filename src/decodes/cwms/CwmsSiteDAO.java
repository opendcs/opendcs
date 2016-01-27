/**
 * $Id$
 * 
 * $Log$
 * Revision 1.7  2015/05/14 13:52:19  mmaloney
 * RC08 prep
 *
 * Revision 1.6  2015/01/30 20:08:12  mmaloney
 * Improve debug.
 *
 * Revision 1.5  2015/01/22 19:50:59  mmaloney
 * log message improvements
 *
 * Revision 1.4  2014/12/11 20:19:20  mmaloney
 * Debug msg improvement
 *
 * Revision 1.3  2014/07/03 12:41:49  mmaloney
 * debug improvements.
 *
 * Revision 1.2  2014/06/27 20:02:23  mmaloney
 * Fixes for deleting a site. It wasn't being removed from cache.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package decodes.cwms;

import ilex.util.Location;
import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;
import opendcs.dao.DatabaseConnectionOwner;
import opendcs.dao.SiteDAO;

/**
 * Data Access Object for CWMS Sites
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class CwmsSiteDAO extends SiteDAO
{
	private String officeId = null;

	public CwmsSiteDAO(DatabaseConnectionOwner tsdb, String officeId)
	{
		super(tsdb);
		this.officeId = officeId;
		siteTableName = "CWMS_V_LOC";
		siteAttributes = 
			"location_code, latitude, longitude, nearest_city, state_initial, "
			+ "'', time_zone_name, nation_id, elevation, 'm', description, location_id, public_name"
			+ ", location_type, active_flag, vertical_datum, horizontal_datum, location_type";
		siteTableKeyColumn = "location_code";
		this.module = "CwmsSiteDAO";
	}

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
//Logger.instance().debug3("CwmsSiteDAO.resultSet2Site read elevation: " + d);
		if (!rsSite.wasNull())
			site.setElevation(d);
		site.setElevationUnits(rsSite.getString(10));
		site.setDescription(rsSite.getString(11));
		SiteName cwmsName = new SiteName(site, Constants.snt_CWMS, rsSite.getString(12));
		site.addName(cwmsName);
		site.setPublicName(rsSite.getString(13));
		site.setLocationType(rsSite.getString(14));
		site.setActive(TextUtil.str2boolean(rsSite.getString(15)));
		String s = rsSite.getString(16);
		if (s != null && s.trim().length() > 0)
			site.setProperty("vertical_datum", s);
		s = rsSite.getString(17);
		if (s != null && s.trim().length() > 0)
			site.setProperty("horizontal_datum", s);
		s = rsSite.getString(18);
		if (s != null && s.trim().length() > 0)
			site.setProperty("location_type", s);
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

		String q = "";
		if (siteName.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
		{
			q = "select location_code from cwms_v_loc where lower(location_id) = " 
				+ sqlString(siteName.getNameValue().toLowerCase());
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
				q = q + " and upper(DB_OFFICE_ID) = " + sqlString(officeId);
		}
		else
		{
			q = "select a.siteid from SiteName a, CWMS_V_LOC b "
			+ " where lower(a.nameType) = " + sqlString(siteName.getNameType().toLowerCase())
			+ " and lower(a.siteName) = "  + sqlString(siteName.getNameValue().toLowerCase())
			+ " and a.siteid = b.location_code";
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
				q = q + " and upper(b.DB_OFFICE_ID) = " + sqlString(officeId);
		}
		
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

	public void writeSite(Site newSite) 
		throws DbIoException
	{
		// site may have come from XML input, and doesn't yet have an ID.
		if (newSite.getId() == Constants.undefinedId)
		{
			for(Iterator<SiteName> snit = newSite.getNames(); snit.hasNext(); )
			{
				SiteName siteName = snit.next();
				DbKey loc_code = lookupSiteID(siteName);
				if (loc_code != null && !loc_code.isNull())
				{
					newSite.forceSetId(loc_code);
					break;
				}
			}
		}
		
		SiteName cwmsName = newSite.getName(Constants.snt_CWMS);
		if (cwmsName == null)
		{
			cwmsName = newSite.getPreferredName();
			if (cwmsName == null)
			{
				warning("write site failed, cannot save site with no CWMS name.");
				return;
			}
			warning("No CWMS name for site. Using preferred name " + cwmsName);
			cwmsName = new SiteName(newSite, Constants.snt_CWMS, cwmsName.getNameValue());
			newSite.addName(cwmsName);
		}
		String state = newSite.state;
		if (state != null && state.length() > 2)
		{
			warning("Invalid state in location '" + cwmsName
				+ "' -- setting to null");
			state = null;
		}
		String tz = newSite.timeZoneAbbr;
		if (tz == null)
			tz = DecodesSettings.instance().aggregateTimeZone;
		double lat = 0.0;
		double lon = 0.0;
		double elev = newSite.getElevation();
		if (newSite.latitude != null)
			lat = Location.parseLatitude(newSite.latitude);
		if (newSite.longitude != null)
			lon = Location.parseLongitude(newSite.longitude);
		Double dlat = new Double(lat);
		Double dlon = new Double(lon);
		Double delev = (elev == Constants.undefinedDouble ? null : new Double(elev));
		try
		{
			if (DecodesSettings.instance().writeCwmsLocations)
			{
				Logger.instance().info("Writing CWMS Location '" + cwmsName.getNameValue() 
					+ "' with officeId=" + officeId);
				cwmsdb.CwmsLocJdbc cwmsLocJdbc = new cwmsdb.CwmsLocJdbc(db.getConnection());

				if (newSite.country == null || newSite.country.trim().length() == 0
				 || newSite.country.trim().toLowerCase().startsWith("us"))
					newSite.country = "US"; // picky picky picky
				
				// MJM for release 5.3 use the new improved version of store
				// This allows us to save country and nearest city.
Logger.instance().debug3("CwmsSiteDAO.writeSite calling cwmsLocJdbc.store ... delev=" + delev
+ ", elevUnits = " + newSite.getElevationUnits());
				cwmsLocJdbc.store(officeId, 
					cwmsName.getNameValue(), 
					state, 
					(String)null,               // countyName
					tz, 
					newSite.getProperty("location_type"),
					dlat,
					dlon,
					delev,
					newSite.getElevationUnits(),
					newSite.getProperty("vertical_datum"),
					newSite.getProperty("horizontal_datum"),
					newSite.getPublicName(),       // publicName
					newSite.getBriefDescription(), // longName
					newSite.getDescription(),      // description
					true,                       // active
					null,                       // locationKindId
					null,                       // mapLabel
					null,                       // publishedLatitude
					null,                       // publishedLongitude,
					null,                       // boundingOffice
					null, // PLACEHOLDER FOR NATIONID, which currently does not work!!!!
//					site.country,               // nationId
					newSite.nearestCity,
					true);                      // ignoreNulls

			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in CwmsLocJdbc.store for site '"
				+ cwmsName.getNameValue() + "': " + ex;
			warning(msg);
System.err.println("lat=" + dlat + ", lon=" + dlon + ", elev=" + delev);
System.err.println(msg);
ex.printStackTrace(System.err);
//			return;
		}
		
		// If this was a newly saved site, have to look up its new ID.
		if (newSite.getId() == Constants.undefinedId)
		{
			newSite.forceSetId(lookupSiteID(cwmsName));
			if (newSite.getId() == Constants.undefinedId)
				return;
		}

		// Drop current names, then re-insert all non-CWMS names.
		String q = "delete from siteName where siteid = " + newSite.getId();
		doModify(q);
		for(Iterator<SiteName> snit = newSite.getNames(); snit.hasNext(); )
		{
			SiteName sn = snit.next();
			if (sn.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
				continue;
			super.insertSiteName(newSite.getId(), sn);
		}
		propsDao.writeProperties("site_property", "site_id", 
			newSite.getKey(), newSite.getProperties());
	}

	@Override
	public void deleteSite(DbKey location_code)
		throws DbIoException
	{
		if (!DecodesSettings.instance().writeCwmsLocations)
			throw new DbIoException("Cannot delete location because 'writeCwmsLocations' property is false.");

		if (location_code == null || location_code.isNull())
		{
			warning("deleteSite called with null location_code");
			return;
		}
		
		Site site;
		try { site = this.getSiteById(location_code); }
		catch (NoSuchObjectException e) { site = null; }
		if (site == null)
		{
			warning("deleteSite cannot read site with location_code=" + location_code);
			return;
		}
		
		SiteName cwmsName = site.getName(Constants.snt_CWMS);
		if (cwmsName == null)
			throw new DbIoException("Cannot delete site '" + site.getDisplayName()
				+ "' because it doesn't have a CWMS name.");
		
		String q = "select count(*) from cwms_v_ts_id where upper(location_id) = "
			+ sqlString(cwmsName.getNameValue().toUpperCase());
		ResultSet rs = null;
		try
		{
			rs = doQuery(q);
			if (rs != null && rs.next())
			{
				int n = rs.getInt(1);
				if (n > 0)
					throw new DbIoException("Cannot delete site '" + site.getDisplayName()
						+ "' because time series exist at this location. Delete the time series first.");
			}

			q = "select count(*) from platform where siteid = " + location_code;
			rs = doQuery(q);
			if (rs != null && rs.next())
			{
				int n = rs.getInt(1);
				if (n > 0)
					throw new DbIoException("Cannot delete site '" + site.getDisplayName()
						+ "' because platform(s) exist at this location. Delete the platform(s) first.");
			}
			q = "select platformid from platformsensor where siteid = " + location_code;
			rs = doQuery(q);
			if (rs != null && rs.next())
			{
				DbKey platformId = DbKey.createDbKey(rs, 1);
				Platform p = Database.getDb().platformList.getById(platformId);
				throw new DbIoException("Cannot delete site '" + site.getDisplayName()
					+ "' because platform '" + p.getDisplayName() + "' has a sensor at this site."
					+ " Remove this reference before deleting site.");
			}
			
			q = "Deleting location_id '" + cwmsName.getNameValue();
			Logger.instance().info(q);
			cwmsdb.CwmsLocJdbc cwmsLocJdbc = new cwmsdb.CwmsLocJdbc(db.getConnection());
			cwmsLocJdbc.delete(officeId, cwmsName.getNameValue());
			cache.remove(location_code);

			q = "delete from sitename where siteid = " + site.getId();
			doModify(q);
			q = "DELETE FROM SITE_PROPERTY WHERE site_id = " + site.getId();
			doModify(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Execution on '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	protected String buildSiteQuery(DbKey siteId)
	{
		String q = "SELECT " + siteAttributes + " FROM " + siteTableName
			+ " where UNIT_SYSTEM = 'SI'";
		
		if (siteId != null && !siteId.isNull())
			q = q + " and location_code = " + siteId;
		else // querying all sites, must add the db_office_id
			q = q + " and upper(DB_OFFICE_ID) = " + sqlString(officeId);
			
		return q;

	}
	
	@Override
	protected void fillCache()
		throws DbIoException
	{
		Logger.instance().debug3("CwmsSiteDAO.fillCache()");

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
			}

			// Have to join with cwms_v_loc so I can filter on db_office_id
			q = "select a.siteid, a.nametype, a.sitename, a.dbnum, a.agency_cd "
				+ " from SiteName a, CWMS_V_LOC b "
				+ " where a.siteid = b.location_code ";
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
				q = q + " and upper(b.DB_OFFICE_ID) = " + sqlString(officeId);
			
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
				String nameType = rs.getString(2);
				String nameValue = rs.getString(3);
if (nameValue.toUpperCase().startsWith("FCNE"))
{
	Logger.instance().info("Read site name: " + nameType + ":" + nameValue + " key=" + key);
}
				if (site == null)
				{
					warning("SiteName for id=" + key + " (" + nameType + ":"
						+ nameValue + ") but no matching site.");
					continue;
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
		for(Site site : siteList)
			cache.put(site);
		int nProps = 0;
		if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			nProps = propsDao.readPropertiesIntoCache("site_property", cache);
		debug1("Site Cache Filled: " + cache.size() + " sites, " + nNames
			+ " names, " + nProps + " properties.");
for(Iterator<Site> sit = cache.iterator(); sit.hasNext(); )
{
Site st = sit.next();
SiteName cwmsName = st.getName(Constants.snt_CWMS);
if (cwmsName != null && cwmsName.getNameValue().toUpperCase().startsWith("FCNE"))
 Logger.instance().info("From cache: " + cwmsName.getNameValue() + ", key=" + st.getKey());
}
	}


}
