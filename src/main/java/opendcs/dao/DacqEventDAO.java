package opendcs.dao;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import opendcs.dai.DacqEventDAI;

public class DacqEventDAO 
	extends DaoBase implements DacqEventDAI
{
	public static final String module = "DacqEventDAO";
	public static final String tableName = "DACQ_EVENT";
	public static final String columnsBase = "DACQ_EVENT_ID, SCHEDULE_ENTRY_STATUS_ID, PLATFORM_ID, EVENT_TIME, "
		+ "EVENT_PRIORITY, SUBSYSTEM, MSG_RECV_TIME, EVENT_TEXT";
	public static String columns = columnsBase;
	private boolean hasAppId = false;
	

	public DacqEventDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
		String q = "select max(LOADING_APPLICATION_ID) from DACQ_EVENT";
		hasAppId = tsdb.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_15;
		if (hasAppId)
		{
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
		if (hasAppId)
			columns = columnsBase + ", LOADING_APPLICATION_ID";
	}

	@Override
	public void writeEvent(DacqEvent evt) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;
		
		if (evt.getDacqEventId().isNull())
			evt.setDacqEventId(getKey(tableName));
		evt.setEventTime(new Date());
		String txt = evt.getEventText();
		if (txt.length() >= 256)
			txt = txt.substring(0,255);

		StringBuilder q = new StringBuilder();
		q.append("INSERT INTO " + tableName + "(" + columns + ") VALUES(?,?,?,?,?,?,?,?");

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
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			throw new DbIoException(msg,ex);
		}		
	}

	@Override
	public void deleteBefore(Date cutoff) throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return;

		String q = "DELETE FROM " + tableName + " WHERE EVENT_TIME < ?";
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
	public int readEventsContaining(String text, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return 0;

		String q = "SELECT " + columns + " FROM " + tableName
			+ " WHERE EVENT_TEXT LIKE '%" + text + "%'";
		if (evtList.size() > 0)
			q = q + " AND DACQ_EVENT_ID > " + evtList.get(evtList.size()-1).getDacqEventId();
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}
	
	private int queryForEvents(String q, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		ResultSet rs = this.doQuery(q);
		try
		{
			int newEvts = 0;
			while(rs != null && rs.next())
			{
				evtList.add(rs2evt(rs));
				newEvts++;
			}
			return newEvts;
		}
		catch (SQLException ex)
		{
			String msg = module + " Error in query '" + q + "': " + ex;
			Logger.instance().warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			throw new DbIoException(msg);
		}
	}

	private DacqEvent rs2evt(ResultSet rs)
		throws SQLException
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
			evt.setAppId(DbKey.createDbKey(rs, 9));

		return evt;
	}

	@Override
	public int readEventsForScheduleStatus(DbKey scheduleEntryStatusId, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return 0;

		String q = "SELECT " + columns + " FROM " + tableName
			+ " WHERE SCHEDULE_ENTRY_STATUS_ID =" + scheduleEntryStatusId;
		if (evtList.size() > 0)
			q = q + " AND DACQ_EVENT_ID > " + evtList.get(evtList.size()-1).getDacqEventId();
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}

	@Override
	public int readEventsForPlatform(DbKey platformId, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
		if (db.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_11)
			return 0;

		String q = "SELECT " + columns + " FROM " + tableName
			+ " WHERE PLATFORM_ID =" + platformId;
		if (evtList.size() > 0)
			q = q + " AND DACQ_EVENT_ID > " + evtList.get(evtList.size()-1).getDacqEventId();
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}

	@Override
	public void deleteEventsForPlatform(DbKey platformId) throws DbIoException
	{
		String q = "DELETE FROM " + tableName + " WHERE PLATFORM_ID =" + platformId;
		doModify(q);
	}
	
	@Override
	public DbKey getFirstIdAfter(Date since)
			throws DbIoException
	{
		String q = "SELECT min(DACQ_EVENT_ID) from " + tableName
			+ " WHERE EVENT_TIME > " + db.sqlDate(since);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				return DbKey.createDbKey(rs, 1);
			else
				return DbKey.NullKey;
		}
		catch (SQLException ex)
		{
			throw new DbIoException(module + " getFirstIdAfter() Cannot execute query '"
				+ q + "': " + ex);
		}
	}

	@Override
	public int readEventsAfter(Date since, ArrayList<DacqEvent> evtList) throws DbIoException
	{
		String q = "SELECT " + columns + " FROM " + tableName;
		if (since != null)
			q = q + " WHERE EVENT_TIME >= " + db.sqlDate(since);
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}

	@Override
	public int readEventsAfter(DbKey eventId, ArrayList<DacqEvent> evtList) throws DbIoException
	{
		String q = "SELECT " + columns + " FROM " + tableName
			+ " WHERE DACQ_EVENT_ID > " + eventId + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}
}
