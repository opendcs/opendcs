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
		ArrayList<Object> parameters = new ArrayList<>();
		if (appId != Constants.undefinedId)
		{
			q = q + " where LOADING_APPLICATION_ID = ?";
			parameters.add(appId);
		}
		if (enabledOnly)
			q = q + (appId != Constants.undefinedId ? " and " : " where ")
				+ "enabled = 'Y'";

		try
		{
			return getResults(q,(rs) -> rs.getString("COMPUTATION_NAME"),parameters);
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
		PropertiesDAI propsDao = db.makePropertiesDAO();
		propsDao.setManualConnection(getConnection());
		try
		{
			List<CompAppInfo> ret = getResults(q, (rs) -> {
				CompAppInfo cai = new CompAppInfo(DbKey.createDbKey(rs, 1));
				cai.setAppName(rs.getString(2));
				cai.setManualEditApp(TextUtil.str2boolean(rs.getString(3)));
				cai.setComment(rs.getString(4));
				return cai;
			});
			q = "in fill props";
			fillInProperties(ret, "");

			q = "select a.loading_application_id, count(1) as CompsUsingProc "
				+ "from hdb_loading_application a, cp_computation b "
				+ "where a.loading_application_id = b.loading_application_id "
				+ "group by a.loading_application_id";

			doQuery(q, (rs)-> {
				DbKey appId = DbKey.createDbKey(rs, 1);
				int numComps = rs.getInt(2);
				for(CompAppInfo cai : ret)
					if (cai.getAppId().equals(appId))
						cai.setNumComputations(numComps);
			});

			return (ArrayList<CompAppInfo>)ret;
		}
		catch(SQLException ex)
		{
			String msg = "Error listing applications: " + ex;
			msg += String.format(", query was %s",q);
			warning(msg);
			throw new DbIoException(msg,ex);
		}
		finally
		{
			propsDao.close();
		}
	}

	private void fillInProperties(List<CompAppInfo> list, String whereClause,Object... properties)
		throws SQLException, DbIoException
	{
		String q = "select LOADING_APPLICATION_ID, PROP_NAME, PROP_VALUE "
			+ "from REF_LOADING_APPLICATION_PROP where loading_application_id =? " + whereClause;

		for(CompAppInfo cai: list)
		{
			ArrayList<Object> parameters = new ArrayList<>();
			parameters.add(cai.getAppId());
			for(Object p: properties)
			{
				parameters.add(p);
			}
			doQuery(q,(rs)-> {
				String nm = rs.getString(2);
				String vl = rs.getString(3);
				cai.setProperty(nm, vl);
			},parameters.toArray());
		};
	}

	/**
	 * Unused?
	 */
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

			fillInProperties(ret, "LOADING_APPLICATION_ID in (" + inList + ")");

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
			+ "where LOADING_APPLICATION_ID = ?";
		PropertiesDAI propsDao = db.makePropertiesDAO();
		propsDao.setManualConnection(getConnection());
		try
		{
			CompAppInfo ret = getSingleResult(q, rs -> {
				CompAppInfo cai = new CompAppInfo(id);
				cai.setAppName(rs.getString(1));
				cai.setManualEditApp(TextUtil.str2boolean(rs.getString(2)));
				cai.setComment(rs.getString(3));
				return cai;
			},id);
			if( ret == null )
			{
				throw new NoSuchObjectException("No application with id=" + id);
			}
			// TODO: investigate if this actually would be better in the above block.
			propsDao.readProperties("REF_LOADING_APPLICATION_PROP", "LOADING_APPLICATION_ID", id,
					ret.getProperties());
			String lmp = PropertiesUtil.getIgnoreCase(ret.getProperties(), "LastModified");
			if (lmp != null)
				try { ret.setLastModified(lastModifiedSdf.parse(lmp)); }
				catch(ParseException ex)
				{
					warning("Cannot parse LastModified '" + lmp + "': " + ex);
				}
			return ret;
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
		String q = null;
		String appName = app.getAppName();
		if (appName.length() > 24)
			appName = appName.substring(0, 24);

		if (isNew)
		{
			// Could be import from XML to overwrite existing algorithm.
			q = "select LOADING_APPLICATION_ID as id from HDB_LOADING_APPLICATION"
			  + " where upper(LOADING_APPLICATION_NAME) = upper(?)";
			try
			{
				DbKey existing = getSingleResult(q,rs-> DbKey.createDbKey(rs, "id"), appName);
				if (existing != null)
				{
					id = existing;
					app.setAppId(id);
					isNew = false;
				}
			}
			catch(SQLException ex)
			{
				warning(
					String.format("Query '%s' failed when it should have return results or not results without failure: %s",
								  q,
								  ex.getLocalizedMessage())
				);
			}
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

		PropertiesDAI propsDao = db.makePropertiesDAO();
		propsDao.setManualConnection(getConnection());

		try
		{
			if (isNew)
			{
				id = getKey("HDB_LOADING_APPLICATION");
				q = "INSERT INTO HDB_LOADING_APPLICATION(loading_application_id, "
					+ "loading_application_name, manual_edit_app, cmmnt"
					+ ") VALUES(?,?,'N',?)";
				doModify(q,id,appName,app.getComment());
				if (id.getValue() == 0L) // HDB does auto-sequence
				{
					q =	  "select LOADING_APPLICATION_ID as ID from HDB_LOADING_APPLICATION"
                        + " where upper(LOADING_APPLICATION_NAME) = upper(?)";
					try
					{
						DbKey tmpKey = getSingleResult(q,rs->DbKey.createDbKey(rs,"id"),appName);
						if( tmpKey != null )
						{
							id = tmpKey;
						}
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
				ArrayList<Object> parameters = new ArrayList<>();

				if (!TextUtil.strEqual(app.getAppName(), oldcai.getAppName()))
				{
					setClause = "LOADING_APPLICATION_NAME = ?";
					parameters.add(appName);
				}

				if (!TextUtil.strEqual(app.getComment(), oldcai.getComment()))
				{
					if (setClause != "")
						setClause += ", ";
					setClause = setClause + "CMMNT = ?";
					parameters.add(app.getComment());
				}
				if (app.getManualEditApp() != oldcai.getManualEditApp())
				{
					if (setClause != "")
						setClause += ", ";
					setClause = setClause + "MANUAL_EDIT_APP = ?";
					parameters.add(app.getManualEditApp() ? "'Y'" : "'N'");
				}

				if (setClause != "")
				{
					q = "UPDATE HDB_LOADING_APPLICATION SET "
						+ setClause
						+ " WHERE LOADING_APPLICATION_ID = ?";
					parameters.add(id);
					doModify(q,parameters.toArray());
				}
			}

			app.getProperties().setProperty("LastModified", lastModifiedSdf.format(new Date()));
			propsDao.writeProperties("REF_LOADING_APPLICATION_PROP", "LOADING_APPLICATION_ID",
				app.getKey(), app.getProperties());
		}
		catch(SQLException ex)
		{
			String msg = String.format("Query '%s' failed",q);
			warning(msg);
			throw new DbIoException(msg,ex);
		}
		catch(DbIoException ex)
		{
			warning(ex.getMessage());
			throw ex;
		}
		finally
		{
			propsDao.close();
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
			DbKey appKey = app.getKey();
			q = "select count(*) from CP_COMPUTATION where LOADING_APPLICATION_ID = ?";
			int countDependantComps = getSingleResult(q,(rs)->rs.getInt(1),appKey);

			if (countDependantComps > 0)
			{
				throw new ConstraintException("Cannot delete application '" + app.getAppName()
					+ "' with id=" + appKey + " because " + countDependantComps + " computations are "
						+ "assigned to it.");
			}
			q = "select count(*) from SCHEDULE_ENTRY where LOADING_APPLICATION_ID = ?";
			int countScheduleEntries = getSingleResult(q,(rs)->rs.getInt(1),appKey);
			if (countScheduleEntries > 0)
			{
				throw new ConstraintException("Cannot delete application '" + app.getAppName()
					+ "' with id=" + appKey + " because " + countScheduleEntries + " schedule entries are "
					+ "assigned to it.");
			}

			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_68)
			{
				q = "select count(*) from ALARM_SCREENING where LOADING_APPLICATION_ID = ?";
				int countAlarms = getSingleResult(q,rs -> rs.getInt(1),appKey);
				if (countAlarms > 0)
				{
					throw new ConstraintException("Cannot delete application '" + app.getAppName()
						+ "' with id=" + appKey + " because " + countAlarms + " alarm screenings are "
						+ "assigned to it.");
				}
			}

			q = "delete from REF_LOADING_APPLICATION_PROP "
				+ "where LOADING_APPLICATION_ID = ?";
			doModify(q,appKey);

			// LOADING_APPLICATION_ID column doesn't exist in old versions of DACQ_EVENT.
			if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15)
			{
				q = "delete from DACQ_EVENT where LOADING_APPLICATION_ID = ?";
				doModify(q,appKey);
			}
			q = "delete from cp_comp_proc_lock where LOADING_APPLICATION_ID = ?";
			doModify(q,appKey);

			if (DecodesSettings.instance().editDatabaseTypeCode == DecodesSettings.DB_OPENTSDB
			 && db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15)
			{
				q = "delete from "
					+ (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_17 ? "ALARM_EVENT"
						: "ALARM_DEF")
					+ " where LOADING_APPLICATION_ID = ?";
				doModify(q,appKey);
				q = "delete from PROCESS_MONITOR where LOADING_APPLICATION_ID = ?";
				doModify(q,appKey);
			}
			q = "delete from HDB_LOADING_APPLICATION "
				+ "where LOADING_APPLICATION_ID = ?";
			doModify(q,appKey);
		}
		catch(SQLException ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
	}

	@Override
	public DbKey lookupAppId(String name)
		throws DbIoException, NoSuchObjectException
	{
		String q = "select loading_application_id from HDB_LOADING_APPLICATION" +
			" where upper(loading_application_name) = upper(?)";
		try
		{
			DbKey ret = getSingleResult(q,(rs)-> DbKey.createDbKey(rs,"loading_application_id"),name);
			if (ret != null )
			{
				return ret;
			} else {
				throw new NoSuchObjectException("No app named '" + name + "'");
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error in lookupAppId(" + name + "): " + ex;
			warning(msg);
			throw new DbIoException(msg,ex);
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
		String q = null;
		try{
			TsdbCompLock lock = null;
			q = "SELECT * from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?";
			lock = getSingleResult(	q, (rs) -> rs2lock(rs), appInfo.getAppId());
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
			if (lock != null)
			{
				releaseCompProcLock(lock);
			}
			lock = new TsdbCompLock(appInfo.getAppId(), pid, host, new Date(), "Starting");
			lock.setAppName(appInfo.getAppName());
			Object lockHearbeat = db.isOpenTSDB()
									? lock.getHeartbeat().getTime()
									: new java.sql.Date(lock.getHeartbeat().getTime());
			q =   "INSERT INTO CP_COMP_PROC_LOCK(loading_application_id,pid,hostname,heartbeat,cur_status)"
				+ " VALUES (?,?,?,?,?)";
			doModify(q,
					  appInfo.getKey(),pid,host,lockHearbeat,lock.getStatus()
					  );

			return lock;
		} catch (SQLException ex) {
			throw new DbIoException(String.format("Failed to obtain or set lock information using query '%s'",q),ex);
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
			int pid = rs.getInt("pid");
			String host = rs.getString("hostname");
			Date heartbeat = db.getFullDate(rs, 4);
			String status = rs.getString("cur_status");

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
		if (lock != null)
		{
			try
			{
				doModify(
					"DELETE from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?",
					lock.getAppId()
				);
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
		Logger.instance().debug3("Checking lock for appID=" + lock.getAppId());
		String q = null;
		try
		{
			q = "select * from cp_comp_proc_lock where loading_application_id=?";
			tlock = getSingleResult(q,rs -> rs2lock(rs), lock.getAppId());
			if (tlock == null)
			{
				throw new LockBusyException("Lock for app ID " + lock.getAppId()
					+ " has been deleted.");
			}

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
				Object heartBeatValue = db.isOpenTSDB()
									   ? lock.getHeartbeat().getTime()
									   : new java.sql.Date(lock.getHeartbeat().getTime());

				debug3("updating heartbeat");
				q = "UPDATE CP_COMP_PROC_LOCK SET HEARTBEAT = ?, CUR_STATUS = ? WHERE LOADING_APPLICATION_ID = ?";
				doModify(q, heartBeatValue,lock.getStatus(),lock.getAppId());


		} catch(SQLException ex ) {
			throw new DbIoException(String.format("Failed to query or update lock information using query '%s'",q),ex);
		}

	}

	@Override
	public List<TsdbCompLock> getAllCompProcLocks()
		throws DbIoException
	{
		try
		{
			List<TsdbCompLock> ret = getResults("SELECT * from CP_COMP_PROC_LOCK",(rs) -> rs2lock(rs) );
			doQuery("SELECT LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME FROM HDB_LOADING_APPLICATION",
				(rs) -> {
					DbKey appId = DbKey.createDbKey(rs, 1);
					String appName = rs.getString(2);
					for(TsdbCompLock lock : ret)
					{
						if (lock.getAppId().equals(appId))
						{
							lock.setAppName(appName);
							break;
						}
					}
			});
			return ret;
		}
		catch(SQLException ex)
		{
			warning("Error retrieving existing comp proc locks " + ex);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean supportsLocks()
	{
		return true;
	}

	@Override
	public void close()
	{
		super.close();
	}

	@Override
	public Date getLastModified(DbKey appId)
	{

		try
		{
			String modStr = getSingleResult("select PROP_VALUE from REF_LOADING_APPLICATION_PROP "
								 + "where LOADING_APPLICATION_ID = ?"
								 + " and PROP_NAME = ?",
								 (rs) -> (rs.getString("PROP_VALUE")),
								 appId,"LastModified");
			return modStr != null ? lastModifiedSdf.parse(modStr) : null;
		}
		catch(Exception ex)
		{
			warning("Cannot retrieve or parse last modify time for appId="
				+ appId + ": " + ex);
		}
		return null;
	}
}
