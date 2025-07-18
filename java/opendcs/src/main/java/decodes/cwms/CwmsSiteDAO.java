/**
 * $Id$
 * 
 * $Log$
 * Revision 1.15  2019/02/26 17:16:44  mmaloney
 * HDB 660
 *
 * Revision 1.14  2018/11/28 21:18:48  mmaloney
 * CWMS JOOQ Migration Mods
 *
 * Revision 1.13  2017/01/24 15:36:27  mmaloney
 * CWMS-10060 added support for DecodesSettings.tsidFetchSize
 *
 * Revision 1.12  2016/11/29 00:53:23  mmaloney
 * Overload lookupSiteID
 *
 * Revision 1.11  2016/11/03 18:59:41  mmaloney
 * Implement wildcard evaluation for groups.
 *
 * Revision 1.10  2016/09/29 18:54:36  mmaloney
 * CWMS-8979 Allow Database Process Record to override decodes.properties and
 * user.properties setting. Command line arg -Dsettings=appName, where appName is the
 * name of a process record. Properties assigned to the app will override the file(s).
 *
 * Revision 1.9  2016/06/07 21:27:29  mmaloney
 * fillCache made public to allow it to be called from ts DAO.
 *
 * Revision 1.8  2016/01/27 21:38:52  mmaloney
 * Removed unneeded debugs.
 *
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
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package decodes.cwms;

import ilex.util.Location;
import ilex.util.Logger;
import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import usace.cwms.db.dao.ifc.loc.CwmsDbLoc;
import usace.cwms.db.dao.util.services.CwmsDbServiceLookup;

/**
 * Data Access Object for CWMS Sites
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class CwmsSiteDAO extends SiteDAO
{
	private final String officeId;

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

	@Override
	protected Site resultSet2Site(Site site, ResultSet rsSite)
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

		return site;
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

		StringBuilder q = new StringBuilder();
		ArrayList<Object> parameters = new ArrayList<>();
		if (siteName.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
		{
			q.append("select location_code from cwms_v_loc where UPPER(location_id) = upper(?) and UNIT_SYSTEM='SI'");
			parameters.add(siteName.getNameValue());
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			{
				q.append(" and upper(DB_OFFICE_ID) = upper(?)");
				parameters.add(officeId);
			}
		}
		else
		{
			q.append("select a.siteid from SiteName a, CWMS_V_LOC b "
			+ " where upper(a.nameType) = upper(?)"
			+ " and upper(a.siteName) = upper(?)"
			+ " and a.siteid = b.location_code");
			parameters.add(siteName.getNameType());
			parameters.add(siteName.getNameValue());
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			{
				q.append(" and upper(b.DB_OFFICE_ID) = upper(?)");
				parameters.add(officeId);
			}
		}
		
		try
		{
			return getSingleResultOr(q.toString(),
								     rs->DbKey.createDbKey(rs, 1),
									 Constants.undefinedId,
									 parameters.toArray(new Object[0]));
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
		Double dlat = lat;
		Double dlon = lon;
		Double delev = (elev == Constants.undefinedDouble ? null : elev);
		try(Connection conn = getConnection())
		{
			if (DecodesSettings.instance().writeCwmsLocations)
			{
				Logger.instance().info("Writing CWMS Location '" + cwmsName.getNameValue() 
					+ "' with officeId=" + officeId);
				CwmsDbLoc cwmsDbLoc = CwmsDbServiceLookup.buildCwmsDb(CwmsDbLoc.class, conn);
				
				if (newSite.country == null || newSite.country.trim().length() == 0
				 || newSite.country.trim().toLowerCase().startsWith("us"))
					newSite.country = "US"; // required
				
				// MJM for release 5.3 use the new improved version of store
				// This allows us to save country and nearest city.
				cwmsDbLoc.store(
					conn,
					officeId, 
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
					newSite.country, 			// NATIONID example: 'US'
					newSite.nearestCity,
					true);                      // ignoreNulls

			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in CwmsLocJdbc.store for site '"
				+ cwmsName.getNameValue() + "': " + ex;
			failure(msg);
			msg = 
				"cwmsDbLoc.store failed for officeId=" + officeId
				+ ", cwmsName=" + cwmsName.getNameValue()
				+ ", state=" + state
				+ ", tz=" + tz
				+ ", locType=" + newSite.getProperty("location_type")
				+ ", lat=" + dlat 
				+ ", lon=" + dlon 
				+ ", elev=" + delev
				+ ", vertDatum=" + newSite.getProperty("vertical_datum")
				+ ", horzDatum=" + newSite.getProperty("horizontal_datum")
				+ ", pubName=" + newSite.getPublicName()
				+ ", longName=" + newSite.getBriefDescription()
				+ ", desc=" + newSite.getDescription()
				+ ", nearestCity=" + newSite.nearestCity;
			failure(msg);
			if (Logger.instance().getLogOutput() != null)
			{
				failure("STACK TRACE FOLLOWS:");
				ex.printStackTrace(Logger.instance().getLogOutput());
			}
		}
		
		// If this was a newly saved site, have to look up its new ID.
		if (newSite.getId() == Constants.undefinedId)
		{
			newSite.forceSetId(lookupSiteID(cwmsName));
			if (newSite.getId() == Constants.undefinedId)
				return;
		}

		// Drop current names, then re-insert all non-CWMS names.
		String q = "delete from siteName where siteid = ?";
		try
		{
			doModify(q,newSite.getId());
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to delete location with ID " + newSite.getId(), ex);
		}

		for(Iterator<SiteName> snit = newSite.getNames(); snit.hasNext(); )
		{
			SiteName sn = snit.next();
			if (sn.getNameType().equalsIgnoreCase(Constants.snt_CWMS))
				continue;
			super.insertSiteName(newSite.getId(), sn);
		}
		propsDao.writeProperties("site_property", "site_id", 
			newSite.getKey(), newSite.getProperties());

		cache.remove(newSite.getId());
		cache.put(newSite);
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
		try
		{
			site = this.getSiteById(location_code);
		}
		catch (NoSuchObjectException e) { site = null; }
		if (site == null)
		{
			warning("deleteSite cannot read site with location_code=" + location_code);
			return;
		}
		
		SiteName cwmsName = site.getName(Constants.snt_CWMS);
		if (cwmsName == null)
		{
			throw new DbIoException("Cannot delete site '" + site.getDisplayName()
				+ "' because it doesn't have a CWMS name.");
		}
		
		String q = "select count(*) from cwms_v_ts_id where upper(location_id) = upper(?)";
		try (Connection conn = getConnection())
		{
			int n = getSingleResult(q,rs->rs.getInt(1),cwmsName.getNameValue());
			if (n > 0)
			{
					throw new DbIoException("Cannot delete site '" + site.getDisplayName()
						+ "' because time series exist at this location. Delete the time series first.");
			}

			q = "select count(*) from platform where siteid = ?";
			n = getSingleResult(q,rs->rs.getInt(1),location_code);
			if (n > 0)
			{
					throw new DbIoException("Cannot delete site '" + site.getDisplayName()
						+ "' because platform(s) exist at this location. Delete the platform(s) first.");
			}
			q = "select platformid from platformsensor where siteid = ?";
			DbKey platformId = getSingleResult(q, rs -> DbKey.createDbKey(rs, 1), location_code);
			if (platformId != null)
			{
				Platform p = Database.getDb().platformList.getById(platformId);
				throw new DbIoException("Cannot delete site '" + site.getDisplayName()
					+ "' because platform '" + p.getDisplayName() + "' has a sensor at this site."
					+ " Remove this reference before deleting site.");
			}
			
			q = "Deleting location_id '" + cwmsName.getNameValue();
			Logger.instance().info(q);

			q = "delete from sitename where siteid = ?";
			doModify(q,site.getId());
			q = "DELETE FROM SITE_PROPERTY WHERE site_id = ?";
			doModify(q,site.getId());

			CwmsDbLoc cwmsDbLoc = CwmsDbServiceLookup.buildCwmsDb(CwmsDbLoc.class, conn);
			cwmsDbLoc.delete(conn, officeId, cwmsName.getNameValue());
			cache.remove(location_code);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Execution on '" + q + "': " + ex;
			Logger.instance().warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public void fillCache()
		throws DbIoException
	{
		debug3("CwmsSiteDAO.fillCache()");

		
		int nNames[] = new int[1];
		nNames[0] = 0;

		StringBuffer q = new StringBuffer(255);
		q.append("SELECT " + siteAttributes + " FROM " + siteTableName + " where UNIT_SYSTEM = 'SI'");
		q.append(" and upper(DB_OFFICE_ID) = upper(?)");
		int origFetchSize = getFetchSize();
		try
		{
			int tsidFetchSize = DecodesSettings.instance().tsidFetchSize;
			if (tsidFetchSize > 0)
				setFetchSize(tsidFetchSize); 

			final List<Site> siteList = getResults(q.toString(), rs ->
			{
				Site site = new Site();
				resultSet2Site(site, rs);
				return site;
			}, officeId);

			q.setLength(0);
			ArrayList<Object> parameters = new ArrayList<>();
			// Have to join with cwms_v_loc so I can filter on db_office_id
			q.append("select a.siteid, a.nametype, a.sitename, a.dbnum, a.agency_cd "
				+ " from SiteName a, CWMS_V_LOC b "
				+ " where a.siteid = b.location_code");
			if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			{
				q.append(" and upper(b.DB_OFFICE_ID) = upper(?)");
				parameters.add(officeId);
			}
			
			doQuery(q.toString(), rs ->
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
				if (site == null)
				{
					warning("SiteName for id=" + key + " (" + nameType + ":"
						+ nameValue + ") but no matching site.");
					return;
				}
				SiteName sn = new SiteName(site, nameType, nameValue);
				sn.setUsgsDbno(rs.getString(4));
				sn.setAgencyCode(rs.getString(5));
				site.addName(sn);
				nNames[0]++;
			}, parameters.toArray(new Object[0]));

			for(Site site : siteList)
			{
				cache.put(site);
			}

			int nProps = 0;
			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			{
				nProps = propsDao.readPropertiesIntoCache("site_property", cache);
			}

			debug1("Site Cache Filled: " + cache.size() + " sites, " + nNames[0]
				+ " names, " + nProps + " properties.");
			lastCacheFillMsec = System.currentTimeMillis();
		}
		catch(SQLException ex)
		{
			String msg = "fillCache - Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
		finally
		{
			// reset fetch size back to original in case it was changed.
			setFetchSize(origFetchSize);
		}
	}

	@Override
	public synchronized DbKey lookupSiteID(final String nameValue)
		throws DbIoException
	{
		// This will search cache and db for CWMS location name.
		SiteName sn = new SiteName(null, Constants.snt_CWMS, nameValue);
		DbKey siteId = lookupSiteID(sn);
		if (siteId != null)
		{
			return siteId;
		}
		
		// If that fails, the super class will search for any matching value
		// in the SITENAME table.
		return super.lookupSiteID(nameValue);
	}

	@Override
	public void readSite(Site site) throws DbIoException, NoSuchObjectException
	{
		DbKey id = site.getId();
		String q = "select " + siteAttributes + " from " + siteTableName
		         + " where " + siteTableKeyColumn + " = ?"
				 + " and UNIT_SYSTEM = 'SI'"
				 + " and upper(DB_OFFICE_ID) =upper(?)";
		try
		{
			Site ret = getSingleResult(q, rs->
			{
				resultSet2Site(site, rs);
				return site;
			}, site.getId(),officeId);

			if (ret == null)
			{
				throw new NoSuchObjectException("No site for location code =" + site.getId());
			}
			readNames(site);
			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_8)
			{
				propsDao.readProperties("site_property", "site_id", id, site.getProperties());
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to retrieve site.",ex);
		}
	}
}
