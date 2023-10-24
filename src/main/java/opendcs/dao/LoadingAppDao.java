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

import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PropertiesDAI;
import decodes.db.Constants;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;
import decodes.util.DecodesSettings;

/**
 * Data Access Object for writing/reading DbEnum objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class LoadingAppDao
    extends DaoBase
    implements LoadingAppDAI
{
    private final String lockCheckQuery =
        "SELECT loading_application_id,pid,hostname,heartbeat,cur_status from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?";
    private final String updateHeartbeatQuery =
        "UPDATE CP_COMP_PROC_LOCK SET HEARTBEAT = ?, CUR_STATUS = ? WHERE LOADING_APPLICATION_ID = ?";
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
        StringBuilder q = new StringBuilder("select COMPUTATION_NAME from CP_COMPUTATION");
        ArrayList<Object> parameters = new ArrayList<>();
        if (appId != Constants.undefinedId)
        {
            q.append(" where LOADING_APPLICATION_ID = ?");
            parameters.add(appId);
        }
        if (enabledOnly)
        {
            q.append((appId != Constants.undefinedId ? " and " : " where "));
            q.append("enabled = 'Y'");
        }

        try
        {
            return getResults(q.toString(), rs -> rs.getString(1), appId);
        }
        catch(SQLException ex)
        {
            String msg = "Error listing computations: " + ex;
            warning(msg);
            throw new DbIoException(msg);
        }
    }

    private CompAppInfo rs2CompAppInfo(ResultSet rs, String prefix) throws SQLException
    {
        CompAppInfo cai = new CompAppInfo(DbKey.createDbKey(rs, prefix+"loading_application_id"));
        cai.setAppName(rs.getString(prefix+"loading_application_name"));
        cai.setManualEditApp(TextUtil.str2boolean(rs.getString(prefix+"manual_edit_app")));
        cai.setComment(rs.getString(prefix+"cmmnt"));
        return cai;
    }

    /**
     * TODO: convert to joins at later improvement.
     */
    @Override
    public ArrayList<CompAppInfo> listComputationApps(boolean usedOnly)
        throws DbIoException
    {
        StringBuffer q = new StringBuffer(
              "select LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME, "
            + "MANUAL_EDIT_APP, CMMNT from HDB_LOADING_APPLICATION ");
        if (usedOnly)
        {
            q.append("(select distinct LOADING_APPLICATION_ID from CP_COMPUTATION)");
        }

        try(Connection conn = getConnection();
            PropertiesDAI propsDao = db.makePropertiesDAO();)
        {
            propsDao.setManualConnection(conn);
            final ArrayList<CompAppInfo> ret =
                (ArrayList<CompAppInfo>)getResults(q.toString(), rs -> rs2CompAppInfo(rs, ""));

            fillInProperties(ret, "");
            q.setLength(0);
            q.append("select a.loading_application_id, count(1) as CompsUsingProc "
                + "from hdb_loading_application a, cp_computation b "
                + "where a.loading_application_id = b.loading_application_id "
                + "group by a.loading_application_id");
            doQuery(q.toString(), rs ->
            {
                DbKey appId = DbKey.createDbKey(rs, 1);
                int numComps = rs.getInt(2);
                for(CompAppInfo cai : ret)
                {
                    if (cai.getAppId().equals(appId))
                    {
                        cai.setNumComputations(numComps);
                    }
                }
            });

            return ret;
        }
        catch(SQLException ex)
        {
            String msg = "Error listing applications: " + ex;
            warning(msg);
            throw new DbIoException(msg);
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
                ret.add(rs2CompAppInfo(rs, ""));
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
        String q = "select LOADING_APPLICATION_ID, "
            + "LOADING_APPLICATION_NAME, MANUAL_EDIT_APP, CMMNT "
            + "from HDB_LOADING_APPLICATION "
            + "where LOADING_APPLICATION_ID = ?";

        try (Connection conn = getConnection();
             PropertiesDAI propsDao = db.makePropertiesDAO();
        )
        {
            propsDao.setManualConnection(conn);
            final CompAppInfo cai = getSingleResult(q, rs -> rs2CompAppInfo(rs, ""), id);

            if (cai == null)
            {
                throw new NoSuchObjectException("No application with id=" + id);
            }
            else
            {
                propsDao.readProperties("REF_LOADING_APPLICATION_PROP",
                                        "LOADING_APPLICATION_ID", id,
                                        cai.getProperties());

                String lmp = PropertiesUtil.getIgnoreCase(cai.getProperties(), "LastModified");
                if (lmp != null)
                {
                    try
                    {
                        cai.setLastModified(lastModifiedSdf.parse(lmp));
                    }
                    catch(ParseException ex)
                    {
                        warning("Cannot parse LastModified '" + lmp + "': " + ex);
                    }
                }
                return cai;
            }
        }
        catch(SQLException ex)
        {
            String msg = "Error in getComputationApp(" + id + "): " + ex.getLocalizedMessage();
            ex.printStackTrace();
            warning(msg);
            throw new DbIoException(msg, ex);
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
        {
            appName = appName.substring(0, 24);
        }

        if (isNew)
        {
            // Could be import from XML to overwrite existing algorithm.
            q = "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
              + " where LOADING_APPLICATION_NAME = ?";
            try
            {
                DbKey existingId = getSingleResult(q, rs -> DbKey.createDbKey(rs,1), appName);
                if (existingId != null)
                {
                    id = existingId;
                    app.setAppId(id);
                    isNew = false;
                }
            }
            catch(Exception ex) {/* ignore */ }
        }

        CompAppInfo oldcai = null;
        if (!isNew)
        {
            try
            {
                oldcai = getComputationApp(id);
            }
            catch(NoSuchObjectException ex)
            {
                isNew = true;
            }
        }

        try (Connection conn = getConnection();
             PropertiesDAI propsDao = db.makePropertiesDAO();)
        {
            propsDao.setManualConnection(conn);
            if (isNew)
            {
                id = getKey("HDB_LOADING_APPLICATION");
                q = "INSERT INTO HDB_LOADING_APPLICATION(loading_application_id, "
                    + "loading_application_name, manual_edit_app, cmmnt"
                    + ") VALUES(?, ?, 'N', ?)";

                doModify(q,id,appName,app.getComment());
                if (id.getValue() == 0L) // HDB does auto-sequence
                {
                    q =
                    "select LOADING_APPLICATION_ID from HDB_LOADING_APPLICATION"
                      + " where LOADING_APPLICATION_NAME = ?";
                    try
                    {
                        id = getSingleResult(q, rs -> DbKey.createDbKey(rs,1),appName);
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
                StringBuilder updateQuery = new StringBuilder("UPDATE HDB_LOADING_APPLICATION SET ");
                final Map<String,Object> fields = new LinkedHashMap<>();

                if (!TextUtil.strEqual(app.getAppName(), oldcai.getAppName()))
                {
                    fields.put("LOADING_APPLICATION_NAME",appName);
                }
                if (!TextUtil.strEqual(app.getComment(), oldcai.getComment()))
                {
                    fields.put("CMMNT",app.getComment());
                }
                if (app.getManualEditApp() != oldcai.getManualEditApp())
                {
                    fields.put("MANUAL_EDIT_APP",(app.getManualEditApp() ? "'Y'" : "'N'"));
                }

                if (!fields.isEmpty())
                {
                    Iterator<String> columnSet = fields.keySet().iterator();
                    while(columnSet.hasNext())
                    {
                        updateQuery.append(columnSet.next()).append("=?");
                        if(columnSet.hasNext())
                        {
                            updateQuery.append(",");
                        }
                        updateQuery.append(",");
                    }
                    updateQuery.append(" WHERE LOADING_APPLICATION_ID = ?"); //+ id;

                    List<Object> parameters =
                        fields.entrySet()
                              .stream()
                              .map( e -> e.getValue())
                              .collect(Collectors.toList());
                    parameters.add(id);
                    doModify(updateQuery.toString(),parameters.toArray(new Object[0]));
                }
            }

            app.getProperties().setProperty("LastModified", lastModifiedSdf.format(new Date()));
            propsDao.writeProperties("REF_LOADING_APPLICATION_PROP", "LOADING_APPLICATION_ID",
                app.getKey(), app.getProperties());
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Database error writing computation app", ex);
        }
        catch(DbIoException ex)
        {
            warning(ex.getMessage());
            throw ex;
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
            " where upper(loading_application_name) = upper(?)";
        try
        {
            DbKey appId = getSingleResult(q, rs-> DbKey.createDbKey(rs, 1),name);
            if (appId == null)
            {
                throw new NoSuchObjectException("No app named '" + name + "'");
            }
            else
            {
                return appId;
            }
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
        TsdbCompLock lock = null;
        try
        {
            lock = getSingleResult(lockCheckQuery, rs -> rs2lock(rs), appInfo.getAppId());

            if ( lock != null)
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
            String msg = "Obtaining existing lock information: "+ ex;
            failure(msg);
        }

        if (lock != null)
        {
            releaseCompProcLock(lock);
        }
        lock = new TsdbCompLock(appInfo.getAppId(), pid, host, new Date(), "Starting");
        try (Connection conn = getConnection();
             PreparedStatement insertLockInfo =
                conn.prepareStatement(
                  "INSERT INTO CP_COMP_PROC_LOCK VALUES ("
                + "?,?,?,?,?)"
            );
        )
        {
            insertLockInfo.setLong(1,appInfo.getAppId().getValue());
            insertLockInfo.setInt(2,pid);
            insertLockInfo.setString(3,host);
            if( db.isOpenTSDB() )
                insertLockInfo.setLong(4, lock.getHeartbeat().getTime());
            else
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
     * @param rs ResultSet containing the data
     * @return lock or null if unsuccessful.
     */
    private TsdbCompLock rs2lock(ResultSet rs) throws SQLException
    {
        return rs2lock(rs, "");
    }

    /**
     * Helper method Parses result set & returns lock object.
     * @param rs ResultSet containing the data
     * @param prefix String prefixed to column names, used for joins. and be empty string.
     * @return lock or null if unsuccessful.
     */
    private TsdbCompLock rs2lock(ResultSet rs, String prefix) throws SQLException
    {
        DbKey appId = DbKey.createDbKey(rs, prefix+"loading_application_id");
        int pid = rs.getInt(prefix+"pid");
        String host = rs.getString(prefix+"hostname");
        Date heartbeat = db.getFullDate(rs, prefix+"heartbeat");
        String status = rs.getString(prefix+"cur_status");

        return new TsdbCompLock(appId, pid, host, heartbeat, status);
    }

    @Override
    public void releaseCompProcLock(TsdbCompLock lock) throws DbIoException
    {
        if (lock != null)
        {
            try
            {
                doModify("DELETE from CP_COMP_PROC_LOCK WHERE LOADING_APPLICATION_ID = ?",lock.getAppId());
            } catch (SQLException ex)
            {
                warning(ex.getLocalizedMessage());
                throw new DbIoException("Failed to delete lock", ex);
            }
        }
    }

    @Override
    public void checkCompProcLock(TsdbCompLock lock)
        throws LockBusyException, DbIoException
    {
        try
        {
            final TsdbCompLock tlock = getSingleResult(lockCheckQuery, rs->rs2lock(rs), lock.getAppId());

            if (tlock != null)
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

                debug3("updating heartbeat");
                doModify(updateHeartbeatQuery,lock.getHeartbeat(),lock.getStatus(),lock.getAppId());
            }
            else
            {
                throw new LockBusyException("Lock for app ID " + lock.getAppId()
                    + " has been deleted.");
            }
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
        String q = "SELECT loading_application_id,pid,hostname,heartbeat,cur_status from CP_COMP_PROC_LOCK";
        try
        {
            final List<TsdbCompLock> ret = getResults(q,rs->rs2lock(rs));
            q = "SELECT LOADING_APPLICATION_ID, LOADING_APPLICATION_NAME FROM HDB_LOADING_APPLICATION";
            doQuery(q, rs ->
            {
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
            warning("Error iterating results for query '" + q + "': " + ex);
            return new ArrayList<>();
        }
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
        String q = "select PROP_VALUE from REF_LOADING_APPLICATION_PROP "
                + "where LOADING_APPLICATION_ID = ?"
                + " and PROP_NAME = ?" ;
        try
        {
            return getSingleResult(q, rs ->
                    {
                        try
                        {
                            return lastModifiedSdf.parse(rs.getString(1));
                        }
                        catch (ParseException ex)
                        {
                            throw new SQLException("Unable to parse returned date/time string",ex);
                        }
                    },
                    appId.getValue(),"LastModified");
        }
        catch(Exception ex)
        {
            warning("Cannot retrieve or parse last modify time for appId="
                + appId + ": " + ex);
        }
        return null;
    }
}