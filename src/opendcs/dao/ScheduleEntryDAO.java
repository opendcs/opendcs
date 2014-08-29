/**
 * $Id$
 * 
 * $Log$
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

import decodes.db.DatabaseException;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbDatabaseVersion;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

public class ScheduleEntryDAO 
	extends DaoBase implements ScheduleEntryDAI
{
	private static String seColumns = "a.schedule_entry_id, a.name, "
		+ "a.loading_application_id, a.routingspec_id, a.start_time, a.timezone, "
		+ "a.run_interval, a.enabled, a.last_modified, c.name";
	private static String seTables = "schedule_entry a, routingspec c";
	private static String seJoinClause = "a.routingspec_id = c.id";
	private static String sesColumns = "schedule_entry_status_id, "
		+ "schedule_entry_id, run_start_time, last_message_time, "
		+ "run_complete_time, hostname, run_status, num_messages, "
		+ "num_decode_errors, num_platforms, last_source, last_consumer, last_modified";
	private LoadingAppDAI loadingAppDAO = null;

	public ScheduleEntryDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "ScheduleEntryDAO");
		loadingAppDAO = tsdb.makeLoadingAppDAO();
	}

	@Override
	public ScheduleEntry readScheduleEntry(String name) 
		throws DbIoException
	{
		String q = "select " + seColumns + " from " + seTables
			+ " where " + seJoinClause
			+ " and a.name = " + sqlString(name);
		ResultSet rs = doQuery(q);
		try
		{
			if (!rs.next())
				return null;
			ScheduleEntry ret = new ScheduleEntry(DbKey.createDbKey(rs, 1));
			rs2scheduleEntry(rs, ret);
			
			if (!ret.getLoadingAppId().isNull())
			{
				CompAppInfo appInfo = loadingAppDAO.getComputationApp(ret.getLoadingAppId());
				if (appInfo != null)
					ret.setLoadingAppName(appInfo.getAppName());
			}

			return ret;
		}
		catch(Exception ex)
		{
			String msg = "Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public ArrayList<ScheduleEntry> listScheduleEntries(CompAppInfo app)
		throws DbIoException
	{
		ArrayList<CompAppInfo> appInfos = loadingAppDAO.listComputationApps(false);

		ArrayList<ScheduleEntry> ret = new ArrayList<ScheduleEntry>();

		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return ret;
		String q = "select " + seColumns + " from " + seTables
			+ " where " + seJoinClause;
		if (app != null)
			q = q + " and a.loading_application_id = " + app.getKey();
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				ScheduleEntry se = new ScheduleEntry(DbKey.createDbKey(rs, 1));
				rs2scheduleEntry(rs, se);
				if (!se.getLoadingAppId().isNull())
					for(CompAppInfo appInfo : appInfos)
						if (se.getLoadingAppId().equals(appInfo.getAppId()))
						{
							se.setLoadingAppName(appInfo.getAppName());
							break;
						}
				ret.add(se);
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in query '" + q + "': " + ex);
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
			return false;

		String q = "select last_modified from schedule_entry "
			+ " where schedule_entry_id = " + scheduleEntry.getKey();
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
			{
				Date lmt = db.getFullDate(rs, 1);
				if (lmt.after(scheduleEntry.getLastModified()))
				{
					q = "select " + seColumns + " from " + seTables
						+ " where " + seJoinClause
						+ " and a.schedule_entry_id = " + scheduleEntry.getKey();
					rs = doQuery(q);
					rs2scheduleEntry(rs, scheduleEntry);
					return true;
				}
				else
					return false;
			}
			else
				throw new NoSuchObjectException("ScheduleEntry id="
					+ scheduleEntry.getKey() + " '"
					+ scheduleEntry.getName() + "' does not exist in database.");
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Error checking ScheduleEntry id="
				+ scheduleEntry.getKey() + " '"
				+ scheduleEntry.getName() + "': " + ex);
		}
	}

	@Override
	public void writeScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return;
		
		if (scheduleEntry.getRoutingSpecId().isNull()
		 && scheduleEntry.getRoutingSpecName() != null
		 && scheduleEntry.getRoutingSpecName().length() > 0)
		{
			String q = "select id from RoutingSpec where upper(name) = "
				+ sqlString(scheduleEntry.getRoutingSpecName().toUpperCase());
			ResultSet rs = doQuery(q);
			try
			{
				if (rs != null && rs.next())
				{
					scheduleEntry.setRoutingSpecId(DbKey.createDbKey(rs, 1));
				}
			}
			catch (SQLException ex)
			{
				throw new DbIoException(
					"writeScheduleEntry Error in query '" + q + "': " + ex);
			}
		}

		scheduleEntry.setLastModified(new Date());
		// It might be an import from an xml file. If no key, try to lookup from name.
		if (scheduleEntry.getKey().isNull())
		{
			String q = "select schedule_entry_id from schedule_entry where "
				+ " upper(name) = " + sqlString(scheduleEntry.getName().toUpperCase());
			ResultSet rs = doQuery(q);
			try
			{
				if (rs != null && rs.next())
					scheduleEntry.forceSetId(DbKey.createDbKey(rs, 1));
			}
			catch (SQLException ex)
			{
				warning("Error in query '" + q + "': " + ex);
			}
		}
		if (scheduleEntry.getKey().isNull())
		{
			scheduleEntry.forceSetId(getKey("schedule_entry"));
			String q = "insert into schedule_entry("
				+ "schedule_entry_id, name, loading_application_id, routingspec_id, start_time, "
				+ "timezone, run_interval, enabled, last_modified)"
				+ " values("
				+ scheduleEntry.getKey() + ", "
				+ sqlString(scheduleEntry.getName()) + ", "
				+ scheduleEntry.getLoadingAppId() + ", "
				+ scheduleEntry.getRoutingSpecId() + ", "
				+ db.sqlDate(scheduleEntry.getStartTime()) + ", "
				+ sqlString(scheduleEntry.getTimezone()) + ", "
				+ sqlString(scheduleEntry.getRunInterval()) + ", "
				+ sqlBoolean(scheduleEntry.isEnabled()) + ", "
				+ db.sqlDate(scheduleEntry.getLastModified()) + ")";
			doModify(q);
		}
		else // do an update
		{
			String q = "update schedule_entry set "
				+ "name = " + sqlString(scheduleEntry.getName()) + ", "
				+ "loading_application_id = " + scheduleEntry.getLoadingAppId() + ", "
				+ "routingspec_id = " + scheduleEntry.getRoutingSpecId() + ", "
				+ "start_time = " + db.sqlDate(scheduleEntry.getStartTime()) + ", "
				+ "timezone = " + sqlString(scheduleEntry.getTimezone()) + ", "
				+ "run_interval = " + sqlString(scheduleEntry.getRunInterval()) + ", "
				+ "enabled = " + sqlBoolean(scheduleEntry.isEnabled()) + ", "
				+ "last_modified = " + db.sqlDate(scheduleEntry.getLastModified())
				+ " where schedule_entry_id = " + scheduleEntry.getKey();
			doModify(q);
		}
	}

	@Override
	public void deleteScheduleEntry(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		deleteScheduleStatusFor(scheduleEntry);
		String q = "delete from schedule_entry where schedule_entry_id = "
			+ scheduleEntry.getKey();
		doModify(q);
	}

	@Override
	public ArrayList<ScheduleEntryStatus> readScheduleStatus(
		ScheduleEntry scheduleEntry) throws DbIoException
	{
		ArrayList<ScheduleEntryStatus> ret = new ArrayList<ScheduleEntryStatus>();
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return ret;

		String q = "select " + sesColumns + " from schedule_entry_status";
		if (scheduleEntry != null)
			q = q + " where schedule_entry_id = " + scheduleEntry.getKey();
		q = q + " order by run_start_time";
		
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.createDbKey(rs, 1));
				rs2scheduleEntryStatus(rs, ses);
				ret.add(ses);
			}
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in query '" + q + "': " + ex);
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
	}


	@Override
	public void writeScheduleStatus(ScheduleEntryStatus seStatus)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return;

		seStatus.setLastModified(new Date());
		if (seStatus.getKey().isNull())
		{
			seStatus.forceSetId(getKey("schedule_entry_status"));
			String q = "insert into schedule_entry_status values("
				+ seStatus.getKey() + ", "
				+ seStatus.getScheduleEntryId() + ", "
				+ db.sqlDate(seStatus.getRunStart()) + ", "
				+ db.sqlDate(seStatus.getLastMessageTime()) + ", "
				+ db.sqlDate(seStatus.getRunStop()) + ", "
				+ sqlString(seStatus.getHostname()) + ", "
				+ sqlString(seStatus.getRunStatus()) + ", "
				+ seStatus.getNumMessages() + ", "
				+ seStatus.getNumDecodesErrors() + ", "
				+ seStatus.getNumPlatforms() + ", "
				+ sqlString(seStatus.getLastSource()) + ", "
				+ sqlString(seStatus.getLastConsumer()) + ", "
				+ db.sqlDate(seStatus.getLastModified()) + ")";
			doModify(q);
		}
		else // do an update
		{
			String q = "update schedule_entry_status set "
				+ "schedule_entry_id = " + seStatus.getScheduleEntryId() + ", "
				+ "run_start_time = " + db.sqlDate(seStatus.getRunStart()) + ", "
				+ "last_message_time = " + db.sqlDate(seStatus.getLastMessageTime()) + ", "
				+ "run_complete_time = " + db.sqlDate(seStatus.getRunStop()) + ", "
				+ "hostname = " + sqlString(seStatus.getHostname()) + ", "
				+ "run_status = " + sqlString(seStatus.getRunStatus()) + ", "
				+ "num_messages = " + seStatus.getNumMessages() + ", "
				+ "num_decode_errors = " + seStatus.getNumDecodesErrors() + ", "
				+ "num_platforms = " + seStatus.getNumPlatforms() + ", "
				+ "last_modified = " + db.sqlDate(seStatus.getLastModified())
				+ " where schedule_entry_status_id = " + seStatus.getKey();
			doModify(q);
		}
	}

	@Override
	public void deleteScheduleStatusBefore(CompAppInfo appInfo, Date cutoff) 
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return;

		String q = "delete from platform_status where last_schedule_entry_status_id in "
			+ "(select schedule_entry_status_id from schedule_entry_status "
			+ "where run_start_time < " + db.sqlDate(cutoff)
			+ " and schedule_entry_id in "
			+ "(select schedule_entry_id from schedule_entry where loading_application_id = "
			+ appInfo.getAppId() + "))";
		doModify(q);
			
		q = 
			db.isOracle() ? // Oracle uses inner join syntax
				"DELETE from schedule_entry_status "
				+ "where run_start_time < " + db.sqlDate(cutoff)
				+ " and schedule_entry_id in "
				+ "(select schedule_entry_id from schedule_entry where loading_application_id = "
				+ appInfo.getAppId() + ")"
			: // else postgres 'using' syntax:
				"delete from schedule_entry_status a "
				+ "using schedule_entry b "
				+ "where b.schedule_entry_id = a.schedule_entry_id "
				+ "and a.run_start_time < " + db.sqlDate(cutoff)
				+ " and b.loading_application_id = " + appInfo.getAppId();
		
		doModify(q);
	}

	@Override
	public void deleteScheduleStatusFor(ScheduleEntry scheduleEntry)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return;
		
		String q = "delete from platform_status where last_schedule_entry_status_id in "
			+ "(select schedule_entry_status_id from schedule_entry_status "
			+ "where schedule_entry_id = " + scheduleEntry.getKey() + ")";
		doModify(q);
		
		q = "delete from schedule_entry_status where schedule_entry_id = "
			+ scheduleEntry.getKey();
		doModify(q);
	}

	@Override
	public ScheduleEntryStatus getLastScheduleStatusFor(
		ScheduleEntry scheduleEntry) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10)
			return null;
		
		String q = "select " + sesColumns + " from schedule_entry_status"
			+ " where schedule_entry_id = " + scheduleEntry.getKey()
			+ " and last_modified = "
			+ "(select max(last_modified) from schedule_entry_status "
			+ " where schedule_entry_id = " + scheduleEntry.getKey() + ")";
		
		ResultSet rs = doQuery(q);
		try
		{
			while(rs != null && rs.next())
			{
				ScheduleEntryStatus ses = new ScheduleEntryStatus(DbKey.createDbKey(rs, 1));
				rs2scheduleEntryStatus(rs, ses);
				return ses;
			}
			return null;
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Error in query '" + q + "': " + ex);
		}
	}
	
	@Override
	public void close()
	{
		loadingAppDAO.close();
		super.close();
	}

}
