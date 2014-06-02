/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
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
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;

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
		if (isNew)
		{
			// Could be import from XML to overwrite existing algorithm.
			q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
			  + " where LOADING_APPLICATION_NAME = " 
			  + sqlString(app.getAppName());
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
					+ ", " + sqlString(app.getAppName())
					+ ", 'N'" 
					+ ", " + sqlString(app.getComment())
				    + ")";
				
				doModify(q);
				if (id.getValue() == 0L) // HDB does auto-sequence
				{
					q = 
					"select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
			  		+ " where LOADING_APPLICATION_NAME = " 
			  		+ sqlString(app.getAppName());
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
					setClause = "LOADING_APPLICATION_NAME = " + sqlString(app.getAppName());
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
		throws DbIoException
	{
		// TODO - check for dependencies in computations and schedule entries.
		// Don't allow deleting if any comps or se's depend on this app.
		try
		{
			String q = "delete from REF_LOADING_APPLICATION_PROP "
				+ "where LOADING_APPLICATION_ID = " + app.getKey();
			doModify(q);
			q = "delete from HDB_LOADING_APPLICATION "
				+ "where LOADING_APPLICATION_ID = " + app.getKey();
			doModify(q);
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
		String q = "SELECT * from CP_COMP_PROC_LOCK WHERE "
			+ "LOADING_APPLICATION_ID = " + appInfo.getAppId();
		ResultSet rs = doQuery(q);
		TsdbCompLock lock = null;
		try
		{
			if (rs.next() && (lock = rs2lock(rs)) != null)
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
		catch(SQLException ex)
		{
			String msg = "Error iterating result set for query '" + 
				q + "': " + ex;
			failure(msg);
		}
		if (lock != null)
			releaseCompProcLock(lock);
		lock = new TsdbCompLock(appInfo.getAppId(), pid, host, new Date(), "Starting");
		q = "INSERT INTO CP_COMP_PROC_LOCK VALUES ("
			+ appInfo.getAppId() + ", " + pid + ", " + sqlString(host) + ", " 
			+ db.sqlDate(lock.getHeartbeat()) + ", " + sqlString(lock.getStatus())
			+ ")";
		doModify(q);
		debug1("Obtained lock for application ID " + appInfo.getAppId());
		return lock;
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
		doModify("DELETE from CP_COMP_PROC_LOCK WHERE "
			+ "LOADING_APPLICATION_ID = " + lock.getAppId());
	}

	@Override
	public void checkCompProcLock(TsdbCompLock lock) 
		throws LockBusyException, DbIoException
	{
		TsdbCompLock tlock;
		Logger.instance().debug1("Checking lock for appID=" + lock.getAppId());
		try
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

				String q = "UPDATE CP_COMP_PROC_LOCK SET HEARTBEAT = "
					+ db.sqlDate(lock.getHeartbeat()) + ", CUR_STATUS = "
					+ sqlString(lock.getStatus())
					+ " WHERE LOADING_APPLICATION_ID = "
					+ lock.getAppId();
					
				doModify(q);
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
			warning("Error iterating results for query '" + q + "': " 
				+ ex);
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
			try { lockCheckStmt.close(); }
			catch(Exception ex) {}
		super.close();
	}

	@Override
	public Date getLastModified(DbKey appId)
	{
		String q = "select PROP_VALUE from REF_LOADING_APPLICATION_PROP "
			+ "where LOADING_APPLICATION_ID = " + appId
			+ " and PROP_NAME = " + sqlString("LastModified");
		try
		{
			ResultSet rs = doQuery(q);
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