/**
 * $Id$
 *
 * $Log$
 * Revision 1.13  2017/06/16 15:33:18  mmaloney
 * To handle import from XML, when writing, if loading app ID is null but the name is not,
 * lookup the ID from the name.
 *
 * Revision 1.12  2016/12/21 23:42:15  mmaloney
 * bugswat
 *
 * Revision 1.11  2016/12/21 20:28:21  mmaloney
 * bugswat
 *
 * Revision 1.10  2016/12/21 19:42:23  mmaloney
 * Last Src can only hold 32 chars. If string is longer, truncate it.
 *
 * Revision 1.9  2016/08/05 14:48:16  mmaloney
 * Updates for Session Status GUI.
 *
 * Revision 1.8  2016/07/20 15:48:44  mmaloney
 * update last source and consumer. It wasn't doing that before.
 *
 * Revision 1.7  2016/02/04 19:00:15  mmaloney
 * SQL bug fix where it wasn't calling rs.next().
 *
 * Revision 1.6  2015/04/14 18:21:39  mmaloney
 * Prevent schedule entry statuses of more than 24 chars.
 *
 * Revision 1.5  2015/02/06 18:51:35  mmaloney
 * When deleting schedule entry status, also delete dependent DACQ_EVENTs
 *
 * Revision 1.4  2014/12/11 20:33:11  mmaloney
 * dev
 *
 * Revision 1.3  2014/08/29 18:18:46  mmaloney
 * For XML import, handle case where existing entry doesn't have a DbKey.
 *
 * Revision 1.2  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import ilex.util.TextUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

public class ScheduleEntryDAO extends DaoBase implements ScheduleEntryDAI
{
	private static final Logger log = LoggerFactory.getLogger(ScheduleEntryDAO.class);

    private static String seColumns = "a.schedule_entry_id, a.name, "
        + "a.loading_application_id, a.routingspec_id, a.start_time, a.timezone, "
        + "a.run_interval, a.enabled, a.last_modified, c.name";
    private static String seTables = "schedule_entry a, routingspec c";
    private static String seJoinClause = "a.routingspec_id = c.id";
    private static String sesColumns = "a.schedule_entry_status_id, "
        + "a.schedule_entry_id, a.run_start_time, a.last_message_time, "
        + "a.run_complete_time, a.hostname, a.run_status, a.num_messages, "
        + "a.num_decode_errors, a.num_platforms, a.last_source, a.last_consumer, "
        + "a.last_modified, b.name";
    private static String sesTables = "schedule_entry_status a, schedule_entry b";
    private static String sesJoinClause = "a.schedule_entry_id = b.schedule_entry_id";

    public ScheduleEntryDAO(DatabaseConnectionOwner tsdb)
    {
        super(tsdb, "ScheduleEntryDAO");
    }

    @Override
    public ScheduleEntry readScheduleEntry(String name) throws DbIoException
    {
        String q = "select " + seColumns + " from " + seTables
            + " where " + seJoinClause
            + " and a.name = ?";
        try
        {
			final ScheduleEntry ret = getSingleResultOr(q, rs ->
			{
				ScheduleEntry tmp = new ScheduleEntry(DbKey.createDbKey(rs,1));
				rs2scheduleEntry(rs, tmp);
				return tmp;
			},
			null,
			name);
            if (ret != null && !ret.getLoadingAppId().isNull())
            {
				try (LoadingAppDAI loadingAppDao = db.makeLoadingAppDAO())
				{
					loadingAppDao.inTransactionOf(this);
                	CompAppInfo appInfo = loadingAppDao.getComputationApp(ret.getLoadingAppId());
                	if (appInfo != null)
					{
                    	ret.setLoadingAppName(appInfo.getAppName());
					}
				}
            }

            return ret;
        }
        catch(Exception ex)
        {
            String msg = "Error in query '" + q + "'";
            log.atWarn()
			   .setCause(ex)
			   .log(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public ArrayList<ScheduleEntry> listScheduleEntries(CompAppInfo app)throws DbIoException
    {
        final ArrayList<ScheduleEntry> ret = new ArrayList<ScheduleEntry>();

        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return ret;
		}

		ArrayList<Object> parameters = new ArrayList<>();
		final StringBuilder q = new StringBuilder("select " + seColumns + " from " + seTables
            									+ " where " + seJoinClause);
        if (app != null)
		{
            q.append(" and a.loading_application_id = ?");
			parameters.add(app.getKey());
		}
        try
        {
			this.inTransaction(dao ->
			{
				try (LoadingAppDAI loadingAppDAO = db.makeLoadingAppDAO())
				{
					final ArrayList<CompAppInfo> appInfos = loadingAppDAO.listComputationApps(false);
					loadingAppDAO.inTransactionOf(dao);
					dao.doQuery(q.toString(), rs ->
					{
						ScheduleEntry se = new ScheduleEntry(DbKey.createDbKey(rs, 1));
							rs2scheduleEntry(rs, se);
							if (!se.getLoadingAppId().isNull())
							{
								for(CompAppInfo appInfo : appInfos)
								{
									if (se.getLoadingAppId().equals(appInfo.getAppId()))
									{
										se.setLoadingAppName(appInfo.getAppName());
										break;
									}
								}
							}
							ret.add(se);
					},
					parameters.toArray(new Object[0]));
				}
			});
        }
        catch (Exception ex)
        {
            throw new DbIoException("Error in query '" + q + "'", ex);
        }
        return ret;
    }

    /**
     * Fill a schedule entry from a result set. SE should already contain the key
     * @param rs the result set
     * @param se the schedule entry
     * @throws SQLException on error
     */
    private void  rs2scheduleEntry(ResultSet rs, ScheduleEntry se)
        throws SQLException
    {
        se.setName(rs.getString(2));
        se.setLoadingAppId(DbKey.createDbKey(rs, 3));
        se.setRoutingSpecId(DbKey.createDbKey(rs, 4));
        se.setStartTime(db.getFullDate(rs, 5));
        se.setTimezone(rs.getString(6));
        se.setRunInterval(rs.getString(7));
        se.setEnabled(TextUtil.str2boolean(rs.getString(8)));
        se.setLastModified(db.getFullDate(rs, 9));
        se.setRoutingSpecName(rs.getString(10));
    }

    @Override
    public boolean checkScheduleEntry(ScheduleEntry scheduleEntry)
        throws DbIoException, NoSuchObjectException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return false;
		}

        String q = "select last_modified from schedule_entry "
            + " where schedule_entry_id = ?";
        try
        {
			Date lmt = getSingleResultOr(q,
										 rs -> db.getFullDate(rs, 1),
										 null,
										 scheduleEntry.getKey());
            if (lmt != null)
            {
                if (lmt.after(scheduleEntry.getLastModified()))
                {
                    q = "select " + seColumns + " from " + seTables
                        + " where " + seJoinClause
                        + " and a.schedule_entry_id = ?";;
                    doQuery(q, rs -> rs2scheduleEntry(rs, scheduleEntry), scheduleEntry.getKey());
                    return true;
                }
                else
                    return false;
            }
            else
			{
                throw new NoSuchObjectException("ScheduleEntry id="
                    + scheduleEntry.getKey() + " '"
                    + scheduleEntry.getName() + "' does not exist in database.");
			}
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error checking ScheduleEntry id="
                + scheduleEntry.getKey() + " '"
                + scheduleEntry.getName() + "': ", ex);
        }
    }

    @Override
    public void writeScheduleEntry(ScheduleEntry scheduleEntry) throws DbIoException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return;
		}

		debug3("writeScheduleEntry(" + scheduleEntry.getName() + ") rsID=" + scheduleEntry.getRoutingSpecId()
				+ ", rsname='" + scheduleEntry.getRoutingSpecName() + "', appID=" + scheduleEntry.getLoadingAppId()
				+ ", appName='" + scheduleEntry.getLoadingAppName() + "'");
        if (scheduleEntry.getRoutingSpecId().isNull()
         && scheduleEntry.getRoutingSpecName() != null
         && scheduleEntry.getRoutingSpecName().length() > 0)
        {
            String q = "select id from RoutingSpec where upper(name) = upper(?)";
            try
            {
				doQuery(q, rs -> scheduleEntry.setRoutingSpecId(DbKey.createDbKey(rs, 1)), scheduleEntry.getRoutingSpecName());
            }
            catch (SQLException ex)
            {
                throw new DbIoException("writeScheduleEntry Error in query '" + q + "'", ex);
            }
        }

        if (scheduleEntry.getLoadingAppId().isNull()
         && scheduleEntry.getLoadingAppName() != null
         && scheduleEntry.getLoadingAppName().length() > 0)
        {
            String q = "select loading_application_id from "
                + "hdb_loading_application where upper(loading_application_name) = upper(?)";
            try
            {
				doQuery(q, rs -> scheduleEntry.setLoadingAppId(DbKey.createDbKey(rs, 1)), scheduleEntry.getLoadingAppName());
            }
            catch (SQLException ex)
            {
                throw new DbIoException("writeScheduleEntry Error in query '" + q + "'", ex);
            }
        }


        scheduleEntry.setLastModified(new Date());
        // It might be an import from an xml file. If no key, try to lookup from name.
        if (scheduleEntry.getKey().isNull())
        {
            String q = "select schedule_entry_id from schedule_entry where "
                + " upper(name) = upper(?)";
            try
            {
				doQuery(q, rs -> scheduleEntry.forceSetId(DbKey.createDbKey(rs, 1)), scheduleEntry.getName());
            }
            catch (SQLException ex)
            {
                log.atWarn()
				   .setCause(ex)
				   .log("Error in query '{}'", q);
            }
        }
		ArrayList<Object> parameters = new ArrayList<>();
		final StringBuilder q = new StringBuilder();
        if (scheduleEntry.getKey().isNull())
        {
            scheduleEntry.forceSetId(getKey("schedule_entry"));
            q.append( "insert into schedule_entry("
                + "schedule_entry_id, name, loading_application_id, routingspec_id, start_time, "
                + "timezone, run_interval, enabled, last_modified)"
                + " values(?,?,?,?, ?,?,?,?, ?)");
			parameters.add(scheduleEntry.getKey());
			parameters.add(scheduleEntry.getName());
			parameters.add(scheduleEntry.getLoadingAppId());
			parameters.add(scheduleEntry.getRoutingSpecId());
			parameters.add(scheduleEntry.getStartTime());
			parameters.add(scheduleEntry.getTimezone());
			parameters.add(scheduleEntry.getRunInterval());
			parameters.add(scheduleEntry.isEnabled());
			parameters.add(scheduleEntry.getLastModified());
        }
        else // do an update
        {
            q.append("update schedule_entry set ")
			 .append("name = ?,")
             .append("loading_application_id = ?,")
             .append("routingspec_id = ?,")
             .append("start_time = ?,")
             .append("timezone = ?,")
             .append("run_interval = ?,")
             .append("enabled = ?,")
             .append("last_modified = ?")
             .append(" where schedule_entry_id = ?");
			parameters.add(scheduleEntry.getName());
			parameters.add(scheduleEntry.getLoadingAppId());
			parameters.add(scheduleEntry.getRoutingSpecId());
			parameters.add(scheduleEntry.getStartTime());
			parameters.add(scheduleEntry.getTimezone());
			parameters.add(scheduleEntry.getRunInterval());
			parameters.add(scheduleEntry.isEnabled());
			parameters.add(scheduleEntry.getLastModified());
			parameters.add(scheduleEntry.getKey());
        }
		try
		{
			doModify(q.toString(), parameters.toArray(new Object[0]));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error inserting or updating scheduled entry.", ex);
		}

    }

    @Override
    public void deleteScheduleEntry(ScheduleEntry scheduleEntry)
        throws DbIoException
    {
		try
		{
			inTransaction(dao ->
			{
				try (ScheduleEntryDAI schedDao = db.makeScheduleEntryDAO())
				{
					schedDao.inTransactionOf(dao);
					schedDao.deleteScheduleStatusFor(scheduleEntry);
					String q = "delete from schedule_entry where schedule_entry_id = ?";
					dao.doModify(q, scheduleEntry.getKey());
				}
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to delete scheduled entry", ex);
		}
    }

    @Override
    public ArrayList<ScheduleEntryStatus> readScheduleStatus(
        ScheduleEntry scheduleEntry) throws DbIoException
    {
        ArrayList<ScheduleEntryStatus> ret = new ArrayList<ScheduleEntryStatus>();
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return ret;
		}

        String q = "select " + sesColumns + " from " + sesTables + " where " + sesJoinClause;
		ArrayList<Object> parameters = new ArrayList<>();
        if (scheduleEntry != null)
		{
            q = q + " and a.schedule_entry_id = ?";
			parameters.add(scheduleEntry.getKey());
		}
        q = q + " order by a.run_start_time desc";

        try
        {
            doQuery(q, rs ->
            {
                ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.createDbKey(rs, 1));
                rs2scheduleEntryStatus(rs, ses);
                ret.add(ses);
            },
			parameters.toArray(new Object[0]));
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Error in query '" + q + "'", ex);
        }
        return ret;
    }

    /**
     * Fill a schedule entry status from a result set. SSE should already contain the key
     * @param rs the result set
     * @param ses the schedule entry status
     * @throws SQLException on error
     */
    private void rs2scheduleEntryStatus(ResultSet rs, ScheduleEntryStatus ses)
        throws SQLException
    {
        ses.setScheduleEntryId(DbKey.createDbKey(rs, 2));
        ses.setRunStart(db.getFullDate(rs, 3));
        ses.setLastMessageTime(db.getFullDate(rs, 4));
        ses.setRunStop(db.getFullDate(rs, 5));
        ses.setHostname(rs.getString(6));
        ses.setRunStatus(rs.getString(7));
        ses.setNumMessages(rs.getInt(8));
        ses.setNumDecodesErrors(rs.getInt(9));
        ses.setNumPlatforms(rs.getInt(10));
        ses.setLastSource(rs.getString(11));
        ses.setLastConsumer(rs.getString(12));
        ses.setLastModified(db.getFullDate(rs, 13));
        ses.setScheduleEntryName(rs.getString(14));
    }


    @Override
    public void writeScheduleStatus(ScheduleEntryStatus seStatus)
        throws DbIoException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return;
		}

        if (seStatus.getRunStatus() != null
         && seStatus.getRunStatus().length() > 24)
		{
            seStatus.setRunStatus(seStatus.getRunStatus().substring(0,24));
		}

        seStatus.setLastModified(new Date());
        String lastSrc = seStatus.getLastSource();
        if (lastSrc != null && lastSrc.length() > 31)
		{
            lastSrc = lastSrc.substring(lastSrc.length()-31);
		}
        String lastCon = seStatus.getLastConsumer();
        if (lastCon != null && lastCon.length() > 31)
		{
            lastCon = lastCon.substring(lastCon.length()-31);
		}
		ArrayList<Object> parameters = new ArrayList<>();
		StringBuilder q = new StringBuilder();
        if (seStatus.getKey().isNull())
        {
            seStatus.forceSetId(getKey("schedule_entry_status"));
            q.append("insert into schedule_entry_status values(?,?,?,?,? ,?,?,?,?,?, ?,?,?)");
			parameters.add(seStatus.getKey());
			parameters.add(seStatus.getScheduleEntryId());
			parameters.add(seStatus.getRunStart());
            parameters.add(seStatus.getLastMessageTime());
            parameters.add(seStatus.getRunStop());
            parameters.add(seStatus.getHostname());
            parameters.add(seStatus.getRunStatus());
            parameters.add(seStatus.getNumMessages());
            parameters.add(seStatus.getNumDecodesErrors());
            parameters.add(seStatus.getNumPlatforms());
            parameters.add(lastSrc);
            parameters.add(lastCon);
            parameters.add(seStatus.getLastModified());
        }
        else // do an update
        {
            q.append("update schedule_entry_status set ")
			 .append("schedule_entry_id = ?,")
         	 .append("run_start_time = ?,")
             .append("last_message_time = ?,")
             .append("run_complete_time = ?,")
             .append("hostname = ?,")
             .append("run_status = ?,")
			 .append("num_messages = ?,")
             .append("num_decode_errors = ?,")
             .append("num_platforms = ?,")
             .append("last_source = ?,")
             .append("last_consumer = ?,")
             .append("last_modified = ?")
             .append(" where schedule_entry_status_id = ?");
			parameters.add(seStatus.getScheduleEntryId());
			parameters.add(seStatus.getRunStart());
			parameters.add(seStatus.getLastMessageTime());
			parameters.add(seStatus.getRunStop());
			parameters.add(seStatus.getHostname());
			parameters.add(seStatus.getRunStatus());
			parameters.add(seStatus.getNumMessages());
			parameters.add(seStatus.getNumDecodesErrors());
			parameters.add(seStatus.getNumPlatforms());
			parameters.add(lastSrc);
			parameters.add(lastCon);
			parameters.add(seStatus.getLastModified());
			parameters.add(seStatus.getKey());
        }
		try
		{
			doModify(q.toString(), parameters.toArray(new Object[0]));
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to insert or update scheduled entry status. Query: " + q, ex);
		}
    }

    @Override
    public void deleteScheduleStatusBefore(CompAppInfo appInfo, Date cutoff)
        throws DbIoException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return;
		}

        StringBuilder q = new StringBuilder(
			  "delete from platform_status where last_schedule_entry_status_id in "
            + "(select schedule_entry_status_id from schedule_entry_status "
            + "where run_start_time < ?"
            + " and schedule_entry_id in "
            + "(select schedule_entry_id from schedule_entry where loading_application_id = ?))");
		try
		{
			inTransaction(dao ->
			{
				dao.doModify(q.toString(), cutoff, appInfo.getAppId());
				q.setLength(0);
				q.append(
					db.isOracle() ? // Oracle uses inner join syntax
						"DELETE from schedule_entry_status "
						+ "where run_start_time < ?"
						+ " and schedule_entry_id in "
						+ "(select schedule_entry_id from schedule_entry where loading_application_id = ?)"
					: // else postgres 'using' syntax:
						"delete from schedule_entry_status a "
						+ "using schedule_entry b "
						+ "where b.schedule_entry_id = a.schedule_entry_id "
						+ "and a.run_start_time < ?"
						+ " and b.loading_application_id = ?"
				);
				dao.doModify(q.toString(), cutoff, appInfo.getAppId());
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Error deleting schedule status.", ex);
		}
    }

    @Override
    public void deleteScheduleStatusFor(ScheduleEntry scheduleEntry)
        throws DbIoException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return;
		}

		try
		{
			final StringBuilder q = new StringBuilder();
			inTransaction(dao ->
			{
				q.append("delete from dacq_event where schedule_entry_status_id in "
					+ "(select schedule_entry_status_id from schedule_entry_status where schedule_entry_id = ?)");
				doModify(q.toString(), scheduleEntry.getId());

				q.setLength(0);
				q.append("delete from platform_status where last_schedule_entry_status_id in "
					+ "(select schedule_entry_status_id from schedule_entry_status "
					+ "where schedule_entry_id = ?)");
				doModify(q.toString(), scheduleEntry.getKey());

				q.setLength(0);
				q.append("delete from schedule_entry_status where schedule_entry_id = ?");
				doModify(q.toString(), scheduleEntry.getKey());
			});
		}
		catch (Exception ex)
		{
			throw new DbIoException("Unable to delete schedule status", ex);
		}
    }

    @Override
    public ScheduleEntryStatus getLastScheduleStatusFor(
        ScheduleEntry scheduleEntry) throws DbIoException
    {
        if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
		{
            return null;
		}

        String q = "select " + sesColumns + " from " + sesTables + " where " + sesJoinClause;
        q = q + " and a.schedule_entry_id = ?"
            + " and a.last_modified = "
            + "(select max(last_modified) from schedule_entry_status "
            + " where schedule_entry_id = ?)";
        try
        {
            return  getSingleResultOr(q, rs ->
			{
                ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.createDbKey(rs, 1));
                rs2scheduleEntryStatus(rs, ses);
                return ses;
            },
			null,
			scheduleEntry.getKey(), scheduleEntry.getKey());
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Error in query '" + q + "'", ex);
        }
    }

    @Override
    public void close()
    {
        super.close();
    }

}
