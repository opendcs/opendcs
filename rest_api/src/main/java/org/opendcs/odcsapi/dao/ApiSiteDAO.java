/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import org.opendcs.odcsapi.beans.ApiSiteName;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiTextUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiSiteDAO extends ApiDaoBase
{
	public static String module = "ApiSiteDAO";

	public ApiSiteDAO(DbInterface dbi)
	{
		super(dbi, module);
	}
	
	public ArrayList<ApiSiteName> getSiteNames(Long siteId)
		throws DbException
	{
		String q = "select NAMETYPE, SITENAME from SITENAME "
			+ "where SITEID = ?";
		try 
		{
		    Connection conn = null;
		    ResultSet rs = doQueryPs(conn, q, siteId);
			ArrayList<ApiSiteName> ret = new ArrayList<ApiSiteName>();
			while(rs.next())
			{
				ApiSiteName sn = new ApiSiteName();
				sn.setSiteId(siteId);
				sn.setNameType(rs.getString(1));
				sn.setNameValue(rs.getString(2));
				ret.add(sn);
			}
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	
	public String getPreferredSiteName(Long siteId)
		throws DbException
	{
		ArrayList<ApiSiteName> siteNames = getSiteNames(siteId);
		String pref = null;
		for (ApiSiteName sn : siteNames)
			if (sn.getNameType().equalsIgnoreCase(DbInterface.siteNameTypePreference))
			{
				pref = sn.getNameValue();
				break;
			}
		if (pref == null)
		{
			if (siteNames.size() > 0)
				pref = siteNames.get(0).getNameValue();
			else
				pref = "unknownSite";
		}
		return pref;
	}
	
	public Collection<ApiSiteRef> getSiteRefs()
		throws DbException
	{
		return getSiteRefMap().values();
	}
	
	public HashMap<Long, ApiSiteRef> getSiteRefMap()
		throws DbException
	{
		HashMap<Long, ApiSiteRef> refs = new HashMap<Long, ApiSiteRef>();
		
		String q = "select ID, PUBLIC_NAME, DESCRIPTION from SITE";
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				ApiSiteRef ref = new ApiSiteRef();
				ref.setSiteId(rs.getLong(1));
				ref.setPublicName(rs.getString(2));
				ref.setDescription(rs.getString(3));
				refs.put(ref.getSiteId(), ref);
			}
			
			q = "select SITEID, NAMETYPE, SITENAME from SITENAME";
			rs = doQuery(q);
			while(rs.next())
			{
				Long siteId = rs.getLong(1);
				String type = rs.getString(2);
				String value = rs.getString(3);
				ApiSiteRef ref = refs.get(siteId);
				if (ref == null)
				{
					Logger.getLogger(ApiConstants.loggerName).log(Level.WARNING, ()->String.format("Orphan SITENAME siteID=%s %s : %s ", siteId, type, value));
					continue;
				}
				ref.getSitenames().put(type, value);
			}
			return refs;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public ApiSite getSite(Long siteId)
		throws DbException, WebAppException
	{
		
		String q = "select ID, LATITUDE, LONGITUDE, NEARESTCITY, STATE, "
				+ "REGION, TIMEZONE, COUNTRY, ELEVATION, ELEVUNITABBR, "
				+ "DESCRIPTION, ACTIVE_FLAG, LOCATION_TYPE, MODIFY_TIME, PUBLIC_NAME "
				+ " from SITE"
				+ " where ID = ?";
				
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, siteId);
			if (!rs.next())
				throw new WebAppException(ErrorCodes.MISSING_ID,
					"No such site with id=" + siteId);
			ApiSite site = new ApiSite();
			site.setSiteId(rs.getLong(1));
			site.setLatitude(rs.getString(2));
			site.setLongitude(rs.getString(3));
			site.setNearestCity(rs.getString(4));
			site.setState(rs.getString(5));
			site.setRegion(rs.getString(6));
			site.setTimezone(rs.getString(7));
			site.setCountry(rs.getString(8));

			double d = rs.getDouble(9);
			if (!rs.wasNull())
				site.setElevation(d);
			site.setElevUnits(rs.getString(10));
			site.setDescription(rs.getString(11));
			site.setActive(ApiTextUtil.str2boolean(rs.getString(12)));
			site.setLocationtype(rs.getString(13));
			site.setLastModified(dbi.getFullDate(rs, 14));
			site.setPublicName(rs.getString(15));
			
			q = "select NAMETYPE, SITENAME from SITENAME "
				+ "where SITEID = ?";
			rs = doQueryPs(conn, q, siteId);
			while(rs.next())
				site.getSitenames().put(rs.getString(1), rs.getString(2));
			
			q = "select PROP_NAME, PROP_VALUE from SITE_PROPERTY "
				+ "where SITE_ID = ?";
			rs = doQueryPs(conn, q, siteId);
			while(rs.next())
				site.getProperties().setProperty(rs.getString(1), rs.getString(2));
			return site;
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public void writeSite(ApiSite site)
		throws DbException, WebAppException
	{
		site.setLastModified(new Date());
		
		String q = "";
		try
		{
			Connection conn = null;
			String desc = site.getDescription();
			if (desc != null && desc.length() > 800)
				site.setDescription(desc.substring(0,799));

			// If this is a NEW site, check for name clashes.
			if (site.getSiteId() == null)
			{
				for(Object key : site.getSitenames().keySet())
				{
					String nameType = (String)key;
					String nameValue = site.getSitenames().get(nameType);
					
					q = "select SITEID from SITENAME where NAMETYPE = ? and SITENAME = ?";
					ResultSet rs = doQueryPs(conn, q, nameType, nameValue);
					if (rs.next())
					{
						throw new WebAppException(ErrorCodes.NOT_ALLOWED,
							"Cannot write new site because "
							+ "an existing site with ID=" + rs.getLong(1) + " has the SITENAME "
							+ nameType + ":" + nameValue);
					}
				}
				//this.closePrepConn();
				// Allocate new ID
				site.setSiteId(getKey("SITE"));
				insert(site);
			}
			else
				update(site);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}	
	}


	protected void update(ApiSite newSite)
		throws DbException, WebAppException
	{
		Long id = newSite.getSiteId();
	
		ApiSite oldSite = getSite(id);
	
		String q = "UPDATE site SET";
		ArrayList<Object> args = new ArrayList<Object>();
		q = q + " MODIFY_TIME = ?";
		args.add(dbi.sqlDateV(newSite.getLastModified()));
		
		
		if (!ApiTextUtil.strEqual(newSite.getLatitude(), oldSite.getLatitude()))
		{
		    q = q + ", Latitude = ?";
		    args.add(newSite.getLatitude());
		}
        if (!ApiTextUtil.strEqual(newSite.getLongitude(), oldSite.getLongitude()))
        {
            q = q + ", LONGITUDE = ?";
            args.add(newSite.getLongitude());
        }
        if (!ApiTextUtil.strEqual(newSite.getNearestCity(), oldSite.getNearestCity()))
        {
            q = q + ", NearestCity = ?";
            args.add(newSite.getNearestCity());
        }
        if (!ApiTextUtil.strEqual(newSite.getState(), oldSite.getState()))
        {
            q = q + ", State = ?";
            args.add(newSite.getState());
        }
        if (!ApiTextUtil.strEqual(newSite.getRegion(), oldSite.getRegion()))
        {
            q = q + ", Region = ?";
            args.add(newSite.getRegion());
        }
        if (!ApiTextUtil.strEqual(newSite.getTimezone(), oldSite.getTimezone()))
        {
            q = q + ", TimeZone = ?";
            args.add(newSite.getTimezone());
        }
        if (!ApiTextUtil.strEqual(newSite.getCountry(), oldSite.getCountry()))
        {
            q = q + ", Country = ?";
            args.add(newSite.getCountry());
        }
        if (newSite.getElevation() != oldSite.getElevation())
        {
            q = q + ", Elevation = ?";
            args.add(newSite.getElevation());
        }
        if (!ApiTextUtil.strEqual(newSite.getElevUnits(), oldSite.getElevUnits()))
        {
            q = q + ", ElevUnitAbbr = ?";
            args.add(newSite.getElevUnits());
        }
        if (!ApiTextUtil.strEqual(newSite.getDescription(), oldSite.getDescription()))
        {
            q = q + ", Description = ?";
            args.add(newSite.getDescription());
        }
        if (newSite.isActive() != oldSite.isActive())
        {
            q = q + ", ACTIVE_FLAG = ?";
            args.add(newSite.isActive());
        }
        if (!ApiTextUtil.strEqual(newSite.getLocationType(), oldSite.getLocationType()))
        {
            q = q + ", LOCATION_TYPE = ?";
            args.add(newSite.getLocationType());
        }
        if (!ApiTextUtil.strEqual(newSite.getPublicName(), oldSite.getPublicName()))
        {
            q = q + ", PUBLIC_NAME = ?";
            args.add(newSite.getPublicName());
        }
		
		q = q + " WHERE ID = ?"; // + id;
		System.out.println("Q: " + q);
		
		args.add(id);
		
		for (int x = 0; x < args.size(); x++)
		{
		    System.out.println("Arg: " + args.get(x).toString());
		}
		doModifyV(q, args.toArray());
	
		updateAllSiteNames(newSite, oldSite);
	
		updateSiteProps(newSite, oldSite);
	}

	private void updateAllSiteNames(ApiSite newSite, ApiSite oldSite)
		throws DbException
	{
		for(String nameType : newSite.getSitenames().keySet())
		{
			String newValue = newSite.getSitenames().get(nameType);
			String oldValue = oldSite.getSitenames().get(nameType);
			if (oldValue == null)
				doModifyV("insert into SITENAME(SITEID, NAMETYPE, SITENAME) "
					+ "values (?, ?, ?)", newSite.getSiteId(), nameType, newValue);
			else if (!ApiTextUtil.strEqual(newValue, oldValue))
				doModifyV("update SITENAME set SITENAME = ? where SITEID = ? and NAMETYPE = ?", newValue, newSite.getSiteId(), nameType);
		}
		for(String nameType : oldSite.getSitenames().keySet())
			if (newSite.getSitenames().get(nameType) == null)
				doModifyV("delete from SITENAME where SITEID = ? and NAMETYPE = ?", newSite.getSiteId(), nameType);
	}

	protected void insert(ApiSite site)
		throws DbException, SQLException
	{
		site.setSiteId(getKey("Site"));
		
		String q = "insert into SITE(ID, LATITUDE, LONGITUDE, NEARESTCITY, STATE, "
				+ "REGION, TIMEZONE, COUNTRY, ELEVATION, ELEVUNITABBR, "
				+ "DESCRIPTION, ACTIVE_FLAG, LOCATION_TYPE, MODIFY_TIME, PUBLIC_NAME) "
				+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				
		Connection conn = null;
		try
		{
		
		doModifyV(q, site.getSiteId(),
					site.getLatitude(),
					site.getLongitude(),
					site.getNearestCity(),
					site.getState(),
					site.getRegion(),
					site.getTimezone(),
					site.getCountry(),
					site.getElevation(),
					site.getElevUnits(),
					site.getDescription(),
					sqlBoolean(site.isActive()),
					site.getLocationType(),
					dbi.sqlDate(site.getLastModified()),
					site.getPublicName());
		}
		finally
		{
			conn.close();
		}
		updateAllSiteNames(site, new ApiSite());
		
		updateSiteProps(site, new ApiSite());
	}

	private void updateSiteProps(ApiSite newSite, ApiSite oldSite) 
		throws DbException
	{
		for(Object k : newSite.getProperties().keySet())
		{
			String propname = (String)k;
			
			String newValue = newSite.getProperties().getProperty(propname);
			String oldValue = oldSite.getProperties().getProperty(propname);

			if (oldValue == null)
			{
				doModifyV("insert into SITE_PROPERTY(SITE_ID, PROP_NAME, PROP_VALUE) "
						+ "values (?,?,?)", newSite.getSiteId(), propname, newValue);
			}
			else if (!ApiTextUtil.strEqual(newValue, oldValue))
			{
				doModifyV("update SITE_PROPERTY set PROP_VALUE = ? where SITE_ID = ? and PROP_NAME = ?", newValue, newSite.getSiteId(), propname);
			}
		}
		for(Object k : oldSite.getProperties().keySet())
		{
			String propname = (String)k;
			System.out.println("Old Site Props: " + propname);
			if (newSite.getProperties().getProperty(propname) == null)
			{
				System.out.println("Found a null");
				// An old prop exists that was removed in the new site
				doModifyV("delete from SITE_PROPERTY where SITE_ID = ? and PROP_NAME = ?", newSite.getSiteId(), propname);
			}
		}
	}

	public void deleteSite(Long siteId) 
		throws DbException, WebAppException
	{
		String q = "select count(*) from CP_COMP_TS_PARM where SITE_ID = ?";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, siteId);
			rs.next();
			int n = rs.getInt(1);
			if (n > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED,
					"Cannot delete site because it is used by " 
					+ n + " computation parameters.");
			
			q = "select count(*) from TSDB_GROUP_MEMBER_SITE where SITE_ID = ?";
			rs = doQueryPs(conn, q, siteId);
			rs.next();
			n = rs.getInt(1);
			if (n > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot delete site because it is used by " 
					+ n + " time series groups.");

			if (DbInterface.isOpenTsdb)
			{
				q = "select count(*) from TS_SPEC where SITE_ID = ?";
				rs = doQueryPs(conn, q, siteId);
				rs.next();
				n = rs.getInt(1);
				if (n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED,
						"Cannot delete site because it is used by " 
						+ n + " time series identifiers.");
			}

			q = "select count(*) from PLATFORM where SITEID = ?";
			rs = doQueryPs(conn, q, siteId);
			rs.next();
			n = rs.getInt(1);
			if (n > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED,
					"Cannot delete site because it is used by " 
					+ n + " platforms.");

			q = "select count(*) from PLATFORMSENSOR where SITEID = ?";
			rs = doQueryPs(conn, q, siteId);
			rs.next();
			n = rs.getInt(1);
			if (n > 0)
				throw new WebAppException(ErrorCodes.NOT_ALLOWED,
					"Cannot delete site because it is used by " 
					+ n + " platform sensor records.");
			
			doModifyV("delete from sitename where siteid = ?", siteId);
			doModifyV("delete from site_property where site_id = ?", siteId);
			doModifyV("delete from site where id = ?", siteId);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
		}
	}
	
	public Long name2id(String name) 
		throws DbException
	{
		String q = "select min(SITEID) from SITENAME where SITENAME = ?";
		class LH { Long ret = null; };
		final LH lh = new LH();
		Connection conn = null;
		doQueryV(conn, q, 
			new ResultSetConsumer()
			{
				@Override
				public void accept(ResultSet rs) throws SQLException
				{
					if (rs.next())
						lh.ret = rs.getLong(1);
				}
			},
			name);
		return lh.ret;
	}
}
