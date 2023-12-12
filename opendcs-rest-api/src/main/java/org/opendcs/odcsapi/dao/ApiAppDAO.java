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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ApiTextUtil;

public class ApiAppDAO
	extends ApiDaoBase
{
	public static String module = "AppDAO";
	private SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public ApiAppDAO(DbInterface dbi)
	{
		super(dbi, module);
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public ArrayList<ApiAppRef> getAppRefs()
		throws DbException
	{
		ArrayList<ApiAppRef> ret = new ArrayList<ApiAppRef>();
		
		String q = "select ap.LOADING_APPLICATION_ID, ap.LOADING_APPLICATION_NAME, ap.CMMNT"
				+ " from HDB_LOADING_APPLICATION ap"
				+ " order by ap.LOADING_APPLICATION_ID";
		
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				ApiAppRef ref = new ApiAppRef();
				ref.setAppId(rs.getLong(1));
				ref.setAppName(rs.getString(2));
				ref.setComment(rs.getString(3));
				ret.add(ref);
			}
			
			q = "select LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE"
				+ " from REF_LOADING_APPLICATION_PROP";
			rs = doQuery(q);
			while (rs.next())
			{
				long id = rs.getLong(1);
				String name = rs.getString(2);
				String value = rs.getString(3);
				for(ApiAppRef ref : ret)
					if (ref.getAppId() == id)
					{
						if (name.equalsIgnoreCase("apptype"))
							ref.setAppType(value);
						else if (name.equalsIgnoreCase("lastmodified"))
						{
							try 
							{
								ref.setLastModified(lastModifiedSdf.parse(value));
							}
							catch(ParseException ex) {}
						}
						break;
					}
			}
			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
	
	public ApiLoadingApp getApp(long id)
		throws DbException, WebAppException, SQLException
	{
		SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		String q = "select LOADING_APPLICATION_NAME, CMMNT, MANUAL_EDIT_APP"
				+ " from HDB_LOADING_APPLICATION"
				+ " where LOADING_APPLICATION_ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, id);
		
		try
		{
			if (!rs.next())
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No app with appid=" + id);
			
			ApiLoadingApp ret = new ApiLoadingApp();
			ret.setAppId(id);
			ret.setAppName(rs.getString(1));
			ret.setComment(rs.getString(2));
			ret.setManualEditingApp(ApiTextUtil.str2boolean(rs.getString(3)));

			q = "select PROP_NAME, PROP_VALUE"
				+ " from REF_LOADING_APPLICATION_PROP"
				+ " where LOADING_APPLICATION_ID = ?";
			rs = doQueryPs(conn, q, id);
			
			while (rs.next())
			{
				String name = rs.getString(1);
				String value = rs.getString(2);
				if (name.equalsIgnoreCase("apptype"))
					ret.setAppType(value);
				else if (name.equalsIgnoreCase("lastmodified"))
				{
					try 
					{
						ret.setLastModified(lastModifiedSdf.parse(value));
					}
					catch(ParseException ex) {}
				}
				else
					ret.getProperties().setProperty(name, value);
			}

			return ret;
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}

	public void writeApp(ApiLoadingApp app)
		throws DbException, WebAppException, SQLException
	{
		// Check to make sure name is unique throw WebAppException if conflict
		// I.e. check for list with same name but different key.
		String q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION "
			+ "where lower(LOADING_APPLICATION_NAME) = ?"; 
		ArrayList<Object> args = new ArrayList<Object>();
		args.add(app.getAppName().toLowerCase());
		if (app.getAppId() != null)
		{
			q = q + " and LOADING_APPLICATION_ID != ?";
			args.add(app.getAppId());
		}	
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, args.toArray());
		
		try
		{
			if (rs.next())
				throw new WebAppException(ErrorCodes.NOT_ALLOWED, 
					"Cannot write App with name '" + app.getAppName() 
					+ "' because another App with id=" + rs.getLong(1) 
					+ " also has that name.");

			app.setLastModified(new Date());
			if (app.getAppId() == null)
			{
				app.setAppId(getKey("HDB_LOADING_APPLICATION"));
	
				q = "insert into HDB_LOADING_APPLICATION(LOADING_APPLICATION_ID,"
					+ " LOADING_APPLICATION_NAME, MANUAL_EDIT_APP, CMMNT) values (?, ?, ?, ?)";
				doModifyV(q, app.getAppId(), app.getAppName(), app.isManualEditingApp() ? "Y": "N", app.getComment());
				addProps(app);
			}
			else
			{
				q = "update HDB_LOADING_APPLICATION set"
					+ " LOADING_APPLICATION_NAME = ?, MANUAL_EDIT_APP = ?, CMMNT = ? where LOADING_APPLICATION_ID = ?";
				doModifyV(q, app.getAppName(), app.isManualEditingApp() ? "Y": "N", app.getComment(), app.getAppId());
				
				q = "delete from REF_LOADING_APPLICATION_PROP where LOADING_APPLICATION_ID = ?";
				doModifyV(q, app.getAppId());
				addProps(app);
			}
		}
		catch (SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}

	}			

	private void addProps(ApiLoadingApp app)
		throws DbException
	{
		Properties props = new Properties();
		ApiPropertiesUtil.copyProps(props, app.getProperties());
		
		if (app.getAppType() != null)
			props.setProperty("appType", app.getAppType());

		SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		props.setProperty("LastModified", lastModifiedSdf.format(app.getLastModified()));
		
		for(Object k : props.keySet())
		{
			String pn = (String)k;
			String pv = props.getProperty(pn);
			String q = "insert into REF_LOADING_APPLICATION_PROP(LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE)"
				+ " values (?, ?, ?)";
			doModifyV(q, app.getAppId(), pn, pv);
		}
	}

	public void deleteApp(long appId)
		throws DbException, WebAppException, SQLException
	{
		String q = "select count(*) from CP_COMPUTATION where LOADING_APPLICATION_ID = ?";
		Connection conn = null;
		ResultSet rs = doQueryPs(conn, q, appId);
		
		try
		{
			if (rs.next())
			{
				long n = rs.getLong(1);
				if (!rs.wasNull() && n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because there are " + n + " computations assigned to this app.");
			}
			q = "select PID, HOSTNAME, HEARTBEAT from CP_COMP_PROC_LOCK where LOADING_APPLICATION_ID = ?"; 
			
			rs = doQueryPs(conn, q, appId);
			
			if (rs.next())
			{
				long heartbeat = rs.getLong(3);
				if (System.currentTimeMillis() - heartbeat < 20000L)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because it is currently running on host '" + rs.getString(2)
						+ "' with PID=" + rs.getInt(1));

			}
			q = "select count(*) from SCHEDULE_ENTRY where LOADING_APPLICATION_ID = ?";
			rs = doQueryPs(conn, q, appId);
			
			if (rs.next())
			{
				long n = rs.getLong(1);
				if (!rs.wasNull() && n > 0)
					throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
						+ " because there are " + n + " schedule entries assigned to this app.");
			}

			if (DbInterface.isOpenTsdb)
			{			
				q = "select count(*) from TSDB_DATA_SOURCE where LOADING_APPLICATION_ID = ?";
				rs = doQueryPs(conn, q, appId);
				
				if (rs.next())
				{
					int n = rs.getInt(1);
					if (n > 0)
						throw new WebAppException(ErrorCodes.NOT_ALLOWED, "Cannot delete loading appId=" + appId
							+ " because there are " + n + " TSDB_DATA_SOURCE records assigned to this app.");
				}
			}
			
			q = "delete from CP_COMP_TASKLIST where LOADING_APPLICATION_ID = ?";
			doModifyV(q, appId);
			
			q = "delete from DACQ_EVENT where LOADING_APPLICATION_ID = ?";
			doModifyV(q, appId);

			q = "delete from REF_LOADING_APPLICATION_PROP where LOADING_APPLICATION_ID = ?";
			doModifyV(q, appId);

			q = "delete from HDB_LOADING_APPLICATION where LOADING_APPLICATION_ID = ?";
			doModifyV(q, appId);
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
	
	public ArrayList<ApiAppStatus> getAppStatus()
		throws DbException
	{
		ArrayList<ApiAppStatus> ret = new ArrayList<ApiAppStatus>();
		
		// Make an entry for all apps.
		ArrayList<ApiAppRef> appRefs = getAppRefs();
		for(ApiAppRef ar : appRefs)
		{
			ApiAppStatus stat = new ApiAppStatus();
			stat.setAppId(ar.getAppId());
			stat.setAppName(ar.getAppName());
			stat.setAppType(ar.getAppType());
			ret.add(stat);
		}
		
		// Now fill in the status for the ones that are actually running.
		String q = "select LOADING_APPLICATION_ID, PID, HOSTNAME, HEARTBEAT, CUR_STATUS "
				+ "from CP_COMP_PROC_LOCK";
		try
		{
			ResultSet rs = doQuery(q);
			while(rs.next())
			{
				long appId = rs.getLong(1);
				for(ApiAppStatus stat : ret)
					if (stat.getAppId() == appId)
					{
						stat.setPid(rs.getLong(2));
						stat.setHostname(rs.getString(3));
						Long x = rs.getLong(4);
						if (!rs.wasNull())
							stat.setHeartbeat(new Date(x));
						stat.setStatus(rs.getString(5));
						break;
					}
					// No ELSE, there can't be an app status without an app.
			}
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
		
		return ret;
	}

	/**
	 * Get status for a single app.
	 * @param appId
	 * @return
	 * @throws DbException
	 * @throws WebAppException
	 * @throws SQLException 
	 */
	public ApiAppStatus getAppStatus(Long appId)
		throws DbException, WebAppException, SQLException
	{
		ApiLoadingApp app = getApp(appId); // Will throw NO_SUCH_OBJECT if not found.
		
		ApiAppStatus ret = new ApiAppStatus();
		ret.setAppId(appId);
		ret.setAppName(app.getAppName());
		ret.setAppType(app.getAppType());
		
		String q = "select PID, HOSTNAME, HEARTBEAT, CUR_STATUS "
			+ "from CP_COMP_PROC_LOCK "
			+ "where LOADING_APPLICATION_ID = ?";
		try
		{
			Connection conn = null;
			ResultSet rs = doQueryPs(conn, q, appId);
			
			if (!rs.next())
				return ret; // PID and hostname will be null, status will be "inactive"
			
			ret.setPid(rs.getLong(1));
			ret.setHostname(rs.getString(2));
			Long x = rs.getLong(3);
			if (!rs.wasNull())
				ret.setHeartbeat(new Date(x));
			ret.setStatus(rs.getString(4));
			ret.setEventPort(determineEventPort(app, ret.getPid()));
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
		
		return ret;
	}
	
	/**
	 * return the event port this process is using or null if it's not running.
	 * @param app
	 * @param pid
	 * @return
	 */
	private Integer determineEventPort(ApiLoadingApp app, Long pid)
	{
		Integer port = null;
		// If EventPort is specified, use it.
		String evtPorts = ApiPropertiesUtil.getIgnoreCase(app.getProperties(), "eventport");
		
		if (evtPorts != null)
		{
			try { port = Integer.parseInt(evtPorts.trim()); }
			catch(NumberFormatException ex)
			{
				warning("Bad EventPort property '" + evtPorts
					+ "' must be integer -- will derive from PID");
			}
		}
		if (port == null && pid != null)
			port = 20000 + (pid.intValue() % 10000);
		return port;
	}

	public void terminateApp(Long appId)
		throws DbException
	{
		String q = "delete from cp_comp_proc_lock where loading_application_id = ?";
		doModifyV(q, appId);
	}

}
