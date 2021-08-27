/*
 * $Id: LoadingAppDao.java,v 1.12 2020/02/14 22:27:05 mmaloney Exp $
 *
 * $Log: LoadingAppDao.java,v $
 * Revision 1.12  2020/02/14 22:27:05  mmaloney
 * Updates
 *
 * Revision 1.11  2019/10/21 14:16:12  mmaloney
 * Bug Fix. For DB Version 17, it was still attempting to delete from ALARM_DEF.
 *
 * Revision 1.10  2019/08/26 20:52:19  mmaloney
 * Removed unneeded debugs.
 *
 * Revision 1.9  2018/06/07 13:10:51  mmaloney
 * Bug fix: Check db version before deleting from DACQ_EVENT.
 *
 * Revision 1.8  2017/12/14 16:50:45  mmaloney
 * In close() guard against multiple statement close calls.
 *
 * Revision 1.7  2017/10/03 12:34:13  mmaloney
 * Handle constraint exceptions
 *
 * Revision 1.6  2015/05/14 13:52:18  mmaloney
 * RC08 prep
 *
 * Revision 1.5  2015/03/19 18:08:00  mmaloney
 * null ptr protection.
 *
 * Revision 1.4  2015/02/06 18:52:45  mmaloney
 * Downgrade lock check debug message.
 *
 * Revision 1.3  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 * Revision 1.2  2014/06/02 00:22:18  mmaloney
 * Add getLastModified method.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.cobraparser.html.domimpl.HTMLElementBuilder.P;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PropertiesDAI;
import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;

/**
 * Data Access Object for writing/reading DbEnum objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class LoadingAppDao
	extends DaoBase
	implements LoadingAppDAI
{
	private PreparedStatement lockCheckStmt = null;
	private SimpleDateFormat lastModifiedSdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public LoadingAppDao(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "LoadingAppDao");
		lastModifiedSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public List<String> listComputationsByApplicationId( DbKey appId, boolean enabledOnly )
		throws DbIoException
	{
		String q = "select COMPUTATION_NAME from CP_COMPUTATION";

		if (appId != Constants.undefinedId)
			q = q + " where LOADING_APPLICATION_ID = " + appId;
		if (enabledOnly)
			q = q + (appId != Constants.undefinedId ? " and " : " where ")
				+ "enabled = 'Y'";

		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<String> ret = new ArrayList<String>();
			while(rs.next())
				ret.add(rs.getString(1));
			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing computations: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public ArrayList<CompAppInfo> listComputationApps(boolean usedOnly)
		throws DbIoException
	{
		String q;
		if (usedOnly)
			q = "select LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME, "
			+ "MANUAL_EDIT_APP, CMMNT from HDB_LOADING_APPLICATION "
			+ "where LOADING_APPLICATION_ID in "
			+ "(select distinct LOADING_APPLICATION_ID from CP_COMPUTATION)";
		else
			q = "select * from HDB_LOADING_APPLICATION";
		PropertiesDAI propertiesDao = db.makePropertiesDAO();
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<CompAppInfo> ret = new ArrayList<CompAppInfo>();
			while(rs.next())
			{
				CompAppInfo cai = new CompAppInfo(DbKey.createDbKey(rs, 1));
				cai.setAppName(rs.getString(2));
				cai.setManualEditApp(TextUtil.str2boolean(rs.getString(3)));
				cai.setComment(rs.getString(4));
				ret.add(cai);
			}

			fillInProperties(ret, "");

			q = "select a.loading_application_id, count(1) as CompsUsingProc "
				+ "from hdb_loading_application a, cp_computation b "
				+ "where a.loading_application_id = b.loading_application_id "
				+ "group by a.loading_application_id";
			rs = doQuery(q);
			while(rs != null && rs.next())
			{
				DbKey appId = DbKey.createDbKey(rs, 1);
				int numComps = rs.getInt(2);
				for(CompAppInfo cai : ret)
					if (cai.getAppId().equals(appId))
						cai.setNumComputations(numComps);
			}

			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing applications: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
		finally
		{
			propertiesDao.close();
		}
	}

	private void fillInProperties(ArrayList<CompAppInfo> list, String whereClause)
		throws SQLException, DbIoException
	{
		String q = "select LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE "
			+ "from REF_LOADING_APPLICATION_PROP " + whereClause;
		ResultSet rs = doQuery(q);
		while(rs != null && rs.next())
		{
			DbKey id = DbKey.createDbKey(rs, 1);
			String nm = rs.getString(2);
			String vl = rs.getString(3);
			for(CompAppInfo cai : list)
				if (cai.getAppId() == id)
				{
					cai.setProperty(nm, vl);
					break;
				}
		}

	}

	@Override
	public ArrayList<CompAppInfo> ComputationAppsIn(String inList)
		throws DbIoException
	{
		String q;
		q = "select LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME, "
		+ "MANUAL_EDIT_APP, CMMNT from HDB_LOADING_APPLICATION "
		+ "where LOADING_APPLICATION_ID in ("
		+ inList + ")";
		try
		{
			ResultSet rs = doQuery(q);
			ArrayList<CompAppInfo> ret = new ArrayList<CompAppInfo>();
			while(rs.next())
			{
				CompAppInfo cai = new CompAppInfo(DbKey.createDbKey(rs, 1));
				cai.setAppName(rs.getString(2));
				cai.setManualEditApp(TextUtil.str2boolean(rs.getString(3)));
				cai.setComment(rs.getString(4));
				ret.add(cai);
			}

			fillInProperties(ret, "where LOADING_APPLICATION_ID in (" + inList + ")");

			return ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing applications: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public CompAppInfo getComputationApp(DbKey id)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select "
			+ "LOADING_APPLICATION_NAME, MANUAL_EDIT_APP, CMMNT "
			+ "from HDB_LOADING_APPLICATION "
			+ "where LOADING_APPLICATION_ID = " + id;
		PropertiesDAI propsDao = db.makePropertiesDAO();
		try
		{
			ResultSet rs = doQuery(q);
			if (!rs.next())
				throw new NoSuchObjectException("No application with id=" + id);

			CompAppInfo cai = new CompAppInfo(id);
			cai.setAppName(rs.getString(1));
			cai.setManualEditApp(TextUtil.str2boolean(rs.getString(2)));
			cai.setComment(rs.getString(3));

			propsDao.readProperties("REF_LOADING_APPLICATION_PROP", "LOADING_APPLICATION_ID", id,
				cai.getProperties());
			String lmp = PropertiesUtil.getIgnoreCase(cai.getProperties(), "LastModified");
			if (lmp != null)
				try { cai.setLastModified(lastModifiedSdf.parse(lmp)); }
				catch(ParseException ex)
				{
					warning("Cannot parse LastModified '" + lmp + "': " + ex);
				}

			return cai;
		}
		catch(SQLException ex)
		{
			String msg = "Error in getComputationApp(" + id + "): " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
		finally
		{
			propsDao.close();
		}
	}

	@Override
	public void writeComputationApp(CompAppInfo app)
		throws DbIoException
	{
		DbKey id = app.getAppId();
		boolean isNew = id.isNull();
		String q;
		String appName = app.getAppName();
		if (appName.length() > 24)
			appName = appName.substring(0, 24);

		if (isNew)
		{
			// Could be import from XML to overwrite existing algorithm.
			q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
			  + " where LOADING_APPLICATION_NAME = "
			  + sqlString(appName);
			try
			{
				ResultSet rs = doQuery(q);
				if (rs.next())
				{
					id = DbKey.createDbKey(rs, 1);
					app.setAppId(id);
					isNew = false;
				}
			}
			catch(Exception ex) { /* ignore */ }
		}

		CompAppInfo oldcai = null;
		if (!isNew)
		{
			try { oldcai = getComputationApp(id); }
			catch(NoSuchObjectException ex)
			{
				isNew = true;
			}
		}

		PropertiesDAI propertiesDao = db.makePropertiesDAO();

		try
		{
			if (isNew)
			{
				id = getKey("HDB_LOADING_APPLICATION");
				q = "INSERT INTO HDB_LOADING_APPLICATION(loading_application_id, "
					+ "loading_application_name, manual_edit_app, cmmnt"
					+ ") VALUES("
					+ id
					+ ", " + sqlString(appName)
					+ ", 'N'"
					+ ", " + sqlString(app.getComment())
				    + ")";

				doModify(q);
				if (id.getValue() == 0L) // HDB does auto-sequence
				{
					q =
					"select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
			  		+ " where LOADING_APPLICATION_NAME = "
			  		+ sqlString(appName);
					try
					{
						ResultSet rs = doQuery(q);
						if (rs.next())
							id = DbKey.createDbKey(rs, 1);
					}
					catch(SQLException ex)
					{
						warning("Error getting app ID: " + ex);
					}
				}
				app.setAppId(id);
			}
			else // update
			{
				String setClause = "";
				if (!TextUtil.strEqual(app.getAppName(), oldcai.getAppName()))
					setClause = "LOADING_APPLICATION_NAME = " + sqlString(appName);
				if (!TextUtil.strEqual(app.getComment(), oldcai.getComment()))
				{
					if (setClause != "")
						setClause += ", ";
					setClause = setClause + "CMMNT = " + sqlString(app.getComment());
				}
				if (app.getManualEditApp() != oldcai.getManualEditApp())
				{
					if (setClause != "")
						setClause += ", ";
					setClause = setClause +
						"MANUAL_EDIT_APP = " + (app.getManualEditApp() ? "'Y'" : "'N'");
				}

				if (setClause != "")
				{
					q = "UPDATE HDB_LOADING_APPLICATION SET "
						+ setClause
						+ " WHERE LOADING_APPLICATION_ID = " + id;
					doModify(q);
				}
			}

			app.getProperties().setProperty("LastModified", lastModifiedSdf.format(new Date()));
			propertiesDao.writeProperties("REF_LOADING_APPLICATION_PROP", "LOADING_APPLICATION_ID",
				app.getKey(), app.getProperties());
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			throw ex;
		}
		finally
		{
			propertiesDao.close();
		}

	}

	@Override
	public void deleteComputationApp(CompAppInfo app)
		throws DbIoException, ConstraintException
	{
		// TODO - check for dependencies in computations and schedule entries.
		// Don't allow deleting if any comps or se's depend on this app.
		String q = "";
		try
		{
			q = "select count(*) from CP_COMPUTATION where LOADING_APPLICATION_ID = "
				+ app.getKey();
			ResultSet rs = doQuery(q);
			if (rs.next())
			{
				int num = rs.getInt(1);
				if (num > 0)
					throw new ConstraintException("Cannot delete application '" + app.getAppName()
						+ "' with id=" + app.getKey() + " because " + num + " computations are "
							+ "assigned to it.");
			}
			q = "select count(*) from SCHEDULE_ENTRY where LOADING_APPLICATION_ID = "
				+ app.getKey();
			rs = doQuery(q);
			if (rs.next())
			{
				int num = rs.getInt(1);
				if (num > 0)
					throw new ConstraintException("Cannot delete application '" + app.getAppName()
						+ "' with id=" + app.getKey() + " because " + num + " schedule entries are "
							+ "assigned to it.");
			}

			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_68)
			{
				q = "select count(*) from ALARM_SCREENING where LOADING_APPLICATION_ID = " + app.getKey();
				rs = doQuery(q);
				if (rs.next())
				{
					int num = rs.getInt(1);
					if (num > 0)
						throw new ConstraintException("Cannot delete application '" + app.getAppName()
							+ "' with id=" + app.getKey() + " because " + num + " alarm screenings are "
							+ "assigned to it.");
				}
			}



			q = "delete from REF_LOADING_APPLICATION_PROP "
				+ "where LOADING_APPLICATION_ID = " + app.getKey();
			doModify(q);



			// LOADING_APPLICATION_ID column doesn't exist in old versions of DACQ_EVENT.
			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15)
			{
				q = "delete from DACQ_EVENT where LOADING_APPLICATION_ID = " + app.getKey();
				doModify(q);
			}
			q = "delete from cp_comp_proc_lock where LOADING_APPLICATION_ID = " + app.getKey();
			doModify(q);
			if (DecodesSettings.instance().editDatabaseTypeCode == DecodesSettings.DB_OPENTSDB
			 && db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15)
			{
				q = "delete from "
					+ (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_17 ? "ALARM_EVENT"
						: "ALARM_DEF")
					+ " where LOADING_APPLICATION_ID = " + app.getKey();
				doModify(q);
				q = "delete from PROCESS_MONITOR where LOADING_APPLICATION_ID = " + app.getKey();
				doModify(q);
			}
			q = "delete from HDB_LOADING_APPLICATION "
				+ "where LOADING_APPLICATION_ID = " + app.getKey();
			doModify(q);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			throw ex;
		}
	}

	@Override
	public DbKey lookupAppId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select loading_application_id from HDB_LOADING_APPLICATION" +
			" where upper(loading_application_name) = " + sqlString(name.toUpperCase());
		try
		{
			ResultSet rs = doQuery(q);
			if (rs == null || !rs.next())
				throw new NoSuchObjectException("No app named '" + name + "'");
			return DbKey.createDbKey(rs, 1);
		}
		catch(SQLException ex)
		{
			String msg = "Error in lookupAppId(" + name + "): " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public CompAppInfo getComputationApp(String name) throws DbIoException,
		NoSuchObjectException
	{
		return getComputationApp(lookupAppId(name));
	}

	@Override
	public TsdbCompLock obtainCompProcLock(CompAppInfo appInfo, int pid, String host)
		throws LockBusyException, DbIoException
	{
		//ResultSet rs = null; //doQuery(q);
		TsdbCompLock lock = null;
		try( PreparedStatement getLock =
				db.getConnection().prepareStatement(
					"SELECT * from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?"
				);
		)
		{
			getLock.setLong(1,appInfo.getAppId().getValue());
			try(ResultSet rs = getLock.executeQuery();){
				if ( rs.next() && (lock = rs2lock(rs)) != null)
				{
					// Same application is re-connecting? (pid will be same)
					if (lock.getPID() == pid)
					{
						// No need to re-create. Update the existing lock.
						checkCompProcLock(lock);
						return lock;
					}
					if (!lock.isStale())
					{
						String msg =
							"Cannot obtain lock for app ID " + appInfo.getAppId()
							+ ". Currently owned by PID " + lock.getPID()
							+ " on host '" + lock.getHost() + "'";
						fatal(msg);
						throw new LockBusyException(msg);
					}
				}
			}

		}
		catch(SQLException ex)
		{
			String msg = "Obtaining existing lock information: "+ ex;
			failure(msg);
		}
		if (lock != null)
			releaseCompProcLock(lock);
		lock = new TsdbCompLock(appInfo.getAppId(), pid, host, new Date(), "Starting");
		try( PreparedStatement insertLockInfo =
			db.getConnection().prepareStatement(
				"INSERT INTO CP_COMP_PROC_LOCK VALUES ("
			+ "?,?,?,?,?)"
			);
		){
			insertLockInfo.setLong(1,appInfo.getAppId().getValue());
			insertLockInfo.setInt(2,pid);
			insertLockInfo.setString(3,host);
			insertLockInfo.setDate(4, new java.sql.Date(lock.getHeartbeat().getTime()));
			insertLockInfo.setString(5,lock.getStatus());
			insertLockInfo.execute();

			debug1("Obtained lock for application ID " + appInfo.getAppId());
			lock.setAppName(appInfo.getAppName());
			return lock;
		}
		catch(SQLException ex)
		{
			String msg = "Error inserting new lock: " + ex;
			failure(msg);
			throw new DbIoException(msg);
		}

	}

	/**
	 * Helper method Parses result set & returns lock object.
	 * @return lock or null if unsuccessful.
	 */
	private TsdbCompLock rs2lock(ResultSet rs)
	{
		try
		{
			DbKey appId = DbKey.createDbKey(rs, 1);
			int pid = rs.getInt(2);
			String host = rs.getString(3);
			Date heartbeat = db.getFullDate(rs, 4);
			String status = rs.getString(5);

			return new TsdbCompLock(appId, pid, host, heartbeat, status);
		}
		catch(SQLException ex)
		{
			warning("Cannot convert result set to lock: " + ex);
			return null;
		}
	}

	@Override
	public void releaseCompProcLock(TsdbCompLock lock) throws DbIoException
	{
		if (lock != null){
			try( PreparedStatement deleteLock =
					db.getConnection().prepareStatement("DELETE from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?");
			) {
				deleteLock.setLong(1, lock.getAppId().getValue());
				deleteLock.execute();
			} catch(SQLException err ){
				warning(err.getLocalizedMessage());
				throw new DbIoException("Failed to delete lock");
			}
		}
	}

	@Override
	public void checkCompProcLock(TsdbCompLock lock)
		throws LockBusyException, DbIoException
	{
		TsdbCompLock tlock;
//		Logger.instance().debug3("Checking lock for appID=" + lock.getAppId());
		try( PreparedStatement updateHeartbeat =
			db.getConnection().prepareStatement(
				"UPDATE CP_COMP_PROC_LOCK SET HEARTBEAT = ?, CUR_STATUS = ? WHERE LOADING_APPLICATION_ID = ?"
			))
		{
			// Retrieve the lock for this process.
			if (lockCheckStmt == null)
			{
				String q = "SELECT * from CP_COMP_PROC_LOCK WHERE "
					+ "LOADING_APPLICATION_ID = ?";
				lockCheckStmt = db.getConnection().prepareStatement(q);
			}
			lockCheckStmt.setLong(1, lock.getAppId().getValue());
			ResultSet rs = lockCheckStmt.executeQuery();

			if (rs.next() && (tlock = rs2lock(rs)) != null)
			{
				if (lock.getPID() != tlock.getPID()
				 || !lock.getHost().equalsIgnoreCase(tlock.getHost()))
				{
					throw new LockBusyException(
						"Lock for app ID " + lock.getAppId()
						+ " has been stolen by PID " + tlock.getPID()
						+ " on host '" + tlock.getHost() + "'"
						+ ", my PID=" + lock.getPID()
						+ ", my host='" + lock.getHost() + "'");
				}
				lock.setHeartbeat(new Date());

				updateHeartbeat.setDate(1,new java.sql.Date(lock.getHeartbeat().getTime()));
				updateHeartbeat.setString(2,lock.getStatus());
				updateHeartbeat.setLong(3,lock.getAppId().getValue());
				debug3("updating heartbeat");
				updateHeartbeat.execute();
/*				String q = "UPDATE CP_COMP_PROC_LOCK SET HEARTBEAT = "
					+ db.sqlDate(lock.getHeartbeat()) + ", CUR_STATUS = "
					+ sqlString(lock.getStatus())
					+ " WHERE LOADING_APPLICATION_ID = "
					+ lock.getAppId();

				doModify(q);*/
			}
			else
				throw new LockBusyException("Lock for app ID " + lock.getAppId()
					+ " has been deleted.");
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Cannot read locks: " + ex);
		}
	}

	@Override
	public List<TsdbCompLock> getAllCompProcLocks()
		throws DbIoException
	{
		ArrayList<TsdbCompLock> ret = new ArrayList<TsdbCompLock>();
		String q = "SELECT * from CP_COMP_PROC_LOCK";
		ResultSet rs = doQuery(q);
		try
		{
			while(rs.next())
				ret.add(rs2lock(rs));
		}
		catch(SQLException ex)
		{
			warning("Error iterating results for query '" + q + "': " + ex);
		}

		q = "SELECT LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME FROM HDB_LOADING_APPLICATION";
		rs = doQuery(q);
		try
		{
			while(rs.next())
			{
				DbKey appId = DbKey.createDbKey(rs, 1);
				String appName = rs.getString(2);
				for(TsdbCompLock lock : ret)
					if (lock.getAppId().equals(appId))
					{
						lock.setAppName(appName);
						break;
					}
			}
		}
		catch(SQLException ex)
		{
			warning("Error iterating results for query '" + q + "': " + ex);
		}

		return ret;
	}

	@Override
	public boolean supportsLocks()
	{
		return true;
	}

	@Override
	public void close()
	{
		if (lockCheckStmt != null)
			try { lockCheckStmt.close(); lockCheckStmt = null; }
			catch(Exception ex) {}
		super.close();
	}

	@Override
	public Date getLastModified(DbKey appId)
	{
		try(PreparedStatement getProperties =
				db.getConnection().prepareStatement("select PROP_VALUE from REF_LOADING_APPLICATION_PROP "
				+ "where LOADING_APPLICATION_ID = ?"
				+ " and PROP_NAME = ?" );)
		{
			getProperties.setLong(1,appId.getValue());
			getProperties.setString(2,"LastModified");

			ResultSet rs = getProperties.executeQuery();
			if(rs.next())
				return lastModifiedSdf.parse(rs.getString(1));
		}
		catch(Exception ex)
		{
			warning("Cannot retrieve or parse last modify time for appId="
				+ appId + ": " + ex);
		}
		return null;
	}
}