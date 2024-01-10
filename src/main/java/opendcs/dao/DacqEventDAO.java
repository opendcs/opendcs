package opendcs.dao;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import opendcs.dai.DacqEventDAI;

public class DacqEventDAO extends DaoBase implements DacqEventDAI
{
	public static final String module = "DacqEventDAO";
	public static final String dacqEventTableName = "DACQ_EVENT";
	public static final String columnsBase = "DACQ_EVENT_ID, SCHEDULE_ENTRY_STATUS_ID, PLATFORM_ID, EVENT_TIME, "
		+ "EVENT_PRIORITY, SUBSYSTEM, MSG_RECV_TIME, EVENT_TEXT";
	public static String dacqEventColumns = columnsBase;
	private static Boolean hasAppId = null;
	

	public DacqEventDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
		String q = "select max(LOADING_APPLICATION_ID) from DACQ_EVENT";
		if (hasAppId == null)
		{
			// Assume we have the column based on version.
			hasAppId = tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15;
			if (hasAppId)
			{
				// But now actually verify
				try
				{
					doQuery(q,rs-> {});
				}
				catch (Exception ex)
				{
					warning(module 
						+ " DB Version is > 15 but DACQ_EVENT does not have LOADING_APPLICATION_ID: " + ex);
					hasAppId = false;
				}
			}
		}
		
		if (hasAppId)
		{
			dacqEventColumns = columnsBase + ", LOADING_APPLICATION_ID";
		}
	}

	@Override
	public void writeEvent(DacqEvent evt) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;
		
		if (evt.getDacqEventId().isNull())
		{
			evt.setDacqEventId(getKey(dacqEventTableName));
		}
		evt.setEventTime(new Date());
		String txt = evt.getEventText();
		if (txt.length() >= 256)
		{
			txt = txt.substring(0,255);
		}

		StringBuilder q = new StringBuilder();
		q.append("INSERT INTO " + dacqEventTableName + "(" + dacqEventColumns + ") VALUES(?,?,?,?,?,?,?,?");

		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(evt.getDacqEventId());
		parameters.add(evt.getScheduleEntryStatusId());
		parameters.add(evt.getPlatformId());
		parameters.add(evt.getEventTime());
		parameters.add(evt.getEventPriority());
		parameters.add(evt.getSubsystem());
		parameters.add(evt.getMsgRecvTime());
		parameters.add(txt);
		
		if(hasAppId)
		{
			q.append(",?");
			parameters.add(evt.getAppId());
		}
		q.append(")");

		try
		{
			doModify(q.toString(), parameters.toArray());
		}
		catch(SQLException ex)
		{
			StringBuilder sb = new StringBuilder();
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			sb.append(msg);
			if (Logger.instance().getMinLogPriority() == Logger.E_DEBUG3)
			{
				sb.append("With event =").append(evt);
			}
			throw new DbIoException(sb.toString(),ex);
		}		
	}

	@Override
	public void deleteBefore(Date cutoff) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;

		String q = "DELETE FROM " + dacqEventTableName + " WHERE EVENT_TIME < ?";
		try
		{
			doModify(q,cutoff);
			q = "UPDATE PLATFORM_STATUS set LAST_ERROR_TIME = null where LAST_ERROR_TIME < ?";
			doModify(q,cutoff);
		}
		catch(Exception ex)
		{
			throw new DbIoException("Unable to delete platform status.", ex);
		}
	}

	@Override
	public int readEventsContaining(String text, ArrayList<DacqEvent> evtList) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return 0;

		ArrayList<Object> parameters = new ArrayList<>();
		String q = "SELECT " + dacqEventColumns + " FROM " + dacqEventTableName
			+ " WHERE EVENT_TEXT LIKE '%' || ? || '%'";
		parameters.add(text);
		if (evtList.size() > 0)
		{
			q = q + " AND DACQ_EVENT_ID > ?";
			parameters.add(evtList.get(evtList.size()-1).getDacqEventId());
		}
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList, parameters);
	}
	
	private int queryForEvents(String q, ArrayList<DacqEvent> evtList, List<Object> parameters)
		throws DbIoException
	{
		int[] newEvts = new int[1];
		newEvts[0] = 0;
		try
		{
			doQuery(q, rs ->
			{
				evtList.add(rs2evt(rs));
				newEvts[0]++;
			},
			parameters.toArray(new Object[0]));
			return newEvts[0];
		}
		catch (SQLException ex)
		{
			String msg = module + " Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg, ex);
		}
	}

	private DacqEvent rs2evt(ResultSet rs) throws SQLException
	{
		DacqEvent evt = new DacqEvent();
		evt.setDacqEventId(DbKey.createDbKey(rs, 1));
		evt.setScheduleEntryStatusId(DbKey.createDbKey(rs, 2));
		evt.setPlatformId(DbKey.createDbKey(rs, 3));
		evt.setEventTime(db.getFullDate(rs, 4));
		evt.setEventPriority(rs.getInt(5));
		evt.setSubsystem(rs.getString(6));
		evt.setMsgRecvTime(db.getFullDate(rs, 7));
		evt.setEventText(rs.getString(8));
		if (hasAppId)
		{
			evt.setAppId(DbKey.createDbKey(rs, 9));
		}

		return evt;
	}

	@Override
	public int readEventsForScheduleStatus(DbKey scheduleEntryStatusId, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
		{
			return 0;
		}
		ArrayList<Object> parameters = new ArrayList<>();
		String q = "SELECT " + dacqEventColumns + " FROM " + dacqEventTableName
			     + " WHERE SCHEDULE_ENTRY_STATUS_ID = ?";
		parameters.add(scheduleEntryStatusId);
		if (evtList.size() > 0)
		{
			q = q + " AND DACQ_EVENT_ID > ?";
			parameters.add(evtList.get(evtList.size()-1).getDacqEventId());
		}
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList, parameters);
	}

	@Override
	public int readEventsForPlatform(DbKey platformId, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
		{
			return 0;
		}
		ArrayList<Object> parameters = new ArrayList<>();
		String q = "SELECT " + dacqEventColumns + " FROM " + dacqEventTableName
			 	 + " WHERE PLATFORM_ID = ?";
		parameters.add(platformId);
		if (evtList.size() > 0)
		{
			q = q + " AND DACQ_EVENT_ID > ?";
			parameters.add(evtList.get(evtList.size()-1).getDacqEventId());
		}

		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList, parameters);
	}

	@Override
	public void deleteEventsForPlatform(DbKey platformId) throws DbIoException
	{
		String q = "DELETE FROM " + dacqEventTableName + " WHERE PLATFORM_ID = ?";
		try
		{
			doModify(q,platformId);
		}
		catch (SQLException ex)
		{
			throw new DbIoException("Unable to delete records for platform " + platformId, ex);
		}
	}
	
	@Override
	public DbKey getFirstIdAfter(Date since)
			throws DbIoException
	{
		String q = "SELECT min(DACQ_EVENT_ID) from " + dacqEventTableName
				 + " WHERE EVENT_TIME > ?";
		try
		{
			return getSingleResultOr(q, rs -> DbKey.createDbKey(rs, 1), DbKey.NullKey, since);
		}
		catch (SQLException ex)
		{
			throw new DbIoException(module + " getFirstIdAfter() Cannot execute query '"
				+ q + "': " + ex, ex);
		}
	}

	@Override
	public int readEventsAfter(Date since, ArrayList<DacqEvent> evtList) throws DbIoException
	{
		String q = "SELECT " + dacqEventColumns + " FROM " + dacqEventTableName;
		ArrayList<Object> parameters = new ArrayList<>();
		if (since != null)
		{
			q = q + " WHERE EVENT_TIME >= ?";
			parameters.add(since);
		}
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList, parameters);
	}

	@Override
	public int readEventsAfter(DbKey eventId, ArrayList<DacqEvent> evtList) throws DbIoException
	{
		String q = "SELECT " + dacqEventColumns + " FROM " + dacqEventTableName
			     + " WHERE DACQ_EVENT_ID > ? order by DACQ_EVENT_ID";
		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(eventId);
		return queryForEvents(q, evtList, parameters);
	}
}
