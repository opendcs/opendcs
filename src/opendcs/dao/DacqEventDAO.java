package opendcs.dao;

import ilex.util.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;

import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import opendcs.dai.DacqEventDAI;

public class DacqEventDAO 
	extends DaoBase implements DacqEventDAI
{
	public static final String module = "DacqEventDAO";
	public static final String tableName = "DACQ_EVENT";
	public static final String columns = "DACQ_EVENT_ID, SCHEDULE_ENTRY_STATUS_ID, PLATFORM_ID, EVENT_TIME, "
		+ "EVENT_PRIORITY, SUBSYSTEM, MSG_RECV_TIME, EVENT_TEXT";

	public DacqEventDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, module);
	}

	@Override
	public synchronized void writeEvent(DacqEvent evt) throws DbIoException
	{
		if (evt.getDacqEventId().isNull())
			evt.setDacqEventId(getKey(tableName));
		evt.setEventTime(new Date());
		String txt = evt.getEventText();
		if (txt.length() >= 256)
			txt = txt.substring(0,255);
		
		String q = "INSERT INTO " + tableName + "(" + columns + ") VALUES("
			+ evt.getDacqEventId() + ", "
			+ evt.getScheduleEntryStatusId() + ", "
			+ evt.getPlatformId() + ", "
			+ db.sqlDate(evt.getEventTime()) + ", "
			+ evt.getEventPriority() + ", "
			+ sqlString(evt.getSubsystem()) + ", "
			+ db.sqlDate(evt.getMsgRecvTime()) + ", "
			+ sqlString(txt)
			+ ")";
		
		// NOTE: Cannot use doModify(q) because it will log the statement, which will cause
		// an endless loop.
		Statement modStmt = null;
		try
		{
			modStmt = db.getConnection().createStatement();
			modStmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			throw new DbIoException(msg);
		}
		finally
		{
			if (modStmt != null)
			{
				try { modStmt.close(); }
				catch(Exception ex) {}
				modStmt = null;
			}
		}
	}

	@Override
	public synchronized void deleteBefore(Date cutoff) throws DbIoException
	{
		String q = "DELETE FROM " + tableName + " WHERE EVENT_TIME < " + db.sqlDate(cutoff);
		doModify(q);
	}

	@Override
	public int readEventsContaining(String text, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
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

		return evt;
	}

	@Override
	public int readEventsForScheduleStatus(DbKey scheduleEntryStatusId, ArrayList<DacqEvent> evtList)
		throws DbIoException
	{
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
		String q = "SELECT " + columns + " FROM " + tableName
			+ " WHERE PLATFORM_ID =" + platformId;
		if (evtList.size() > 0)
			q = q + " AND DACQ_EVENT_ID > " + evtList.get(evtList.size()-1).getDacqEventId();
			
		q = q + " order by DACQ_EVENT_ID";
		return queryForEvents(q, evtList);
	}

	
	

}
