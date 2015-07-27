/*
 * $Id$
 * 
 * $Log$
 * Revision 1.9  2015/02/06 18:55:08  mmaloney
 * Bug fixes and addition of method to get 1st & last record ID in a day.
 *
 * Revision 1.8  2015/01/30 20:14:28  mmaloney
 * Fix SQL for UPDATE xmit rec statment.
 *
 * Revision 1.7  2014/12/11 20:33:11  mmaloney
 * dev
 *
 * Revision 1.6  2014/11/19 16:16:05  mmaloney
 * Additions for dcpmon
 *
 * Revision 1.5  2014/09/15 14:09:52  mmaloney
 * DCP Mon Daemon Impl
 *
 * Revision 1.4  2014/08/22 17:23:10  mmaloney
 * 6.1 Schema Mods and Initial DCP Monitor Implementation
 *
 * Revision 1.3  2014/07/03 12:53:40  mmaloney
 * debug improvements.
 *
 * Revision 1.2  2014/06/02 14:28:53  mmaloney
 * rc5 includes initial refactory for dcpmon
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

import ilex.util.Base64;
import ilex.util.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import opendcs.dai.XmitRecordDAI;

import lrgs.archive.XmitWindow;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import decodes.db.NetworkList;
import decodes.dcpmon.XmitMediumType;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Unlike most DAO's this one is designed to be more persistent. It should be instantiated
 * or re-instantiated after a database connection and re-used through the life of the
 * application. This allows efficient use of the internal prepared statements.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class XmitRecordDAO 
	extends DaoBase implements XmitRecordDAI
{
	public static final String module = "XmitRecordDao";
	
	/** Maps day numbers to xmit-rec table suffixes. */
	class XmitDayMapEntry
	{
		String suffix;
		int dayNum;
		XmitDayMapEntry(String s, int d) { suffix=s; dayNum=d; }
	};

	/** Maps day numbers to xmit-rec table suffixes. */
	private ArrayList<XmitDayMapEntry> dayNumSuffixMap = new ArrayList<XmitDayMapEntry>();
	private long dayNumSuffixMapLoadedMsec = 0L;
	
	/** Milliseconds per day */
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	
	private int numXmitsSaved = 0;
	
	/** Prepared Statements for inserting into DCP Mon my-dcps table */
	private PreparedStatement insertStatement[] = null;
	
	/** Prepared Statements for updating into DCP Mon my-dcps table */
	private PreparedStatement updateXmit[] = null;

	/** Prepared Statements used when trying to find existing DCPs in the
	 * my_dcp_tran<sufix> tables */
	private PreparedStatement selectByIdAndTime[];

	protected Calendar resultSetCalendar = null;
	
	private String dcpTransFields = 
		"record_id, medium_type, medium_id, local_recv_time, " +
		"transmit_time, failure_codes, window_start_sod, window_length, xmit_interval, " +
		"carrier_start, carrier_stop, flags, channel, battery, msg_length, msg_data";
	private SimpleDateFormat debugSdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z");

	/** Msgs with same mediumID within this number of msec are considered the same */
	private static final long TIME_FUDGE = 1000L * 10;

	/**
	 * Construct a new object after a database connection
	 * @param tsdb the database
	 * @param maxXmitDays maximum number of days to store for DCP mon.
	 */
	public XmitRecordDAO(DatabaseConnectionOwner tsdb, int maxXmitDays)
	{
		super(tsdb, module);
		debugSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		insertStatement = new PreparedStatement[maxXmitDays];
		selectByIdAndTime = new PreparedStatement[maxXmitDays];
		updateXmit = new PreparedStatement[maxXmitDays];
		insertStatement = new PreparedStatement[maxXmitDays];

		for(int i=0; i<maxXmitDays; i++)
			insertStatement[i] = updateXmit[i] = selectByIdAndTime[i] = null;
//
//		resultSetCalendar = Calendar.getInstance(tz);
	}
	
	/** 
	  Convenience method to convert msec time value to day number. 
	  @param msec the msec value
	  @return day number (0 = Jan 1, 1970)
	*/
	public static int msecToDay(long msec)
	{
		return (int)(msec / MS_PER_DAY);
	}

	/** 
	  Convenience method to conver msec time value to second of day.
	  @param msec the Java time value.
	  @return second-of-day
	*/
	public static int msecToSecondOfDay(long msec)
	{
		return (int)((msec % MS_PER_DAY)/1000L);
	}

	/**
	 * Loads the internal day-number to suffix map.
	 */
	protected void loadDayNumSuffixMap()
		throws DbIoException
	{
		Logger.instance().debug2("loadDayNumSuffixMap");
		String q = "SELECT TABLE_SUFFIX, DAY_NUMBER FROM dcp_trans_day_map ORDER BY table_suffix";
		ResultSet rs = doQuery(q);
		dayNumSuffixMap.clear();
		int latestDayNum = -1;
		try
		{
			while(rs != null && rs.next())
			{
				String suffix = rs.getString(1);
				int dayNum = rs.getInt(2);
				if (rs.wasNull())
					dayNum = -1;
				else if (dayNum > latestDayNum)
					latestDayNum = dayNum;
					
				dayNumSuffixMap.add(new XmitDayMapEntry(suffix, dayNum));
			}
			dayNumSuffixMapLoadedMsec = System.currentTimeMillis();
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Error iterating from query '"
			 	+ q + "': " + ex);
		}
		dayNumSuffixMapLoadedMsec = System.currentTimeMillis();
		info("loadDayNumSuffixMap latestDayNum=" + latestDayNum + ", "
			+ new Date(latestDayNum*MS_PER_DAY));
	}

	@Override
	public String getDcpXmitSuffix(int dayNum, boolean doAllocate)
		throws DbIoException
	{
		String method = "getDcpXmitSuffix ";
		if (dayNumSuffixMap.isEmpty())
			loadDayNumSuffixMap();

		XmitDayMapEntry firstFree = null;
		XmitDayMapEntry oldestDay = null;
		for(XmitDayMapEntry xdme : dayNumSuffixMap)
		{
			if (xdme.dayNum == dayNum)
				return xdme.suffix;
			if (xdme.dayNum <= 0)
			{
				if (firstFree == null)
					firstFree = xdme;
			}
			else if (oldestDay == null || xdme.dayNum < oldestDay.dayNum)
				oldestDay = xdme;
		}
		
		// Fell through without finding specified dayNum.
		if (!doAllocate)
			return null;

		// if there is a free slot, use it.
		if (firstFree != null)
		{
			info(method + "Using free suffix "
				+ firstFree.suffix + " for day num " + dayNum);
			firstFree.dayNum = dayNum;
			String q = "UPDATE dcp_trans_day_map SET day_number = " + dayNum
				+ " WHERE table_suffix = " + sqlString(firstFree.suffix);
			Logger.instance().debug2("getDcpXmitSuffix: " + q);
			doModify(q);
			return firstFree.suffix;
		}

		// There is no free slot.
		if (dayNum < oldestDay.dayNum)
		{
			warning(method + "Cannot allocate table "
				+ "for old day number "	+ dayNum + ", oldest day in storage is " 
				+ oldestDay.dayNum);
			return null;
		}

		// We will re-assign the oldest day to the specified day.
		// Delete all records from the oldest day.
		clearTable(oldestDay.suffix);
		info(method + 
			"Cleared tables for day num "
			+ oldestDay.dayNum + " to make room for new day number " + dayNum);
		
		oldestDay.dayNum = dayNum;

		String q = "UPDATE dcp_trans_day_map SET day_number = " + dayNum
			+ " WHERE table_suffix = " + sqlString(oldestDay.suffix);
		doModify(q);
		return oldestDay.suffix;
	}

	/**
	 * Clear the data and the transmit records for the passed suffix.
	 * @param suffix
	 * @throws DbIoException
	 */
	private void clearTable(String suffix)
		throws DbIoException
	{
		String q = "delete from DCP_TRANS_DATA_" + suffix;
		doModify(q);
		
		q = "DELETE FROM DCP_TRANS_" + suffix;
		doModify(q);
		
		q = "UPDATE dcp_trans_day_map SET day_number = null"
			+ " WHERE table_suffix = " + sqlString(suffix);
		doModify(q);
	}

	

//	/**
//	 * This method inserts a timestamp into a prepared statement for writing/selecting the db.
//	 * @param ps the prepared statement
//	 * @param column the column number
//	 * @param msecTime long value representing msec since the epoch
//	 * @throws SQLException 
//	 */
//	protected void setPrepStatementTimestamp(PreparedStatement ps, int column, long msecTime) 
//		throws SQLException
//	{
//		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
//			ps.setLong(column, msecTime);
//		else
//		{
//			if (msecTime == 0L)
//				ps.setNull(column, Types.TIMESTAMP);
//			else
//				ps.setTimestamp(column, new Timestamp(msecTime), resultSetCalendar);
//		}
//	}
	
//	/**
//	 * @return msec time from the XmitRecord prepared statement.
//	 */
//	protected long getResultSetTimestamp(ResultSet rs, int column)
//		throws SQLException
//	{
//		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
//		{
//			Date d = db.getFullDate(rs, column);
//			return d == null ? 0L : d.getTime();
//		}
//		
//		java.sql.Timestamp ts = rs.getTimestamp(column, resultSetCalendar);
//		if (rs.wasNull())
//			return 0L;
//		else
//			return ts.getTime();
//	}

	
	@Override
	public Date getLatestTimeStamp(int dayNum) 
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		
		String tab = "DCP_TRANS_" + suffix;
		String q = "SELECT MAX(transmit_time) as maxDate FROM " + tab;
		ResultSet rs = doQuery(q);

		try
		{
			if (rs != null && rs.next())
				return db.getFullDate(rs, 1);
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "getLatestTimeStamp Cannot parse xmit result: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	
	@Override
	public void saveDcpTranmission(DcpMsg xr)
		throws DbIoException
	{
		// Messages are assigned to a table by transmit time.
		Date xmitTime = xr.getXmitTime();
		if (xmitTime == null)
		{
			warning("Cannot save message without xmitTime: " + xr.getHeader());
			return;
		}
		
		// MJM LRD DCPMon was seeing garbage messages with times way in the future.
		// Don't allow messages more than half hour in the future.
		long msgAgeMsec = System.currentTimeMillis() - xmitTime.getTime();
		if (msgAgeMsec < -1800000L)
		{
			warning("Ignoring future message: " + xr.getHeader() + ", xmitTime=" + xmitTime);
			return;
		}
		
		int dayNum = msecToDay(xmitTime.getTime());
		String suffix = getDcpXmitSuffix(dayNum, true);
		if (suffix == null)
			return;
		String tab = "DCP_TRANS_" + suffix;
		String base64data = new String(Base64.encodeBase64(xr.getData()));
		
		if (xr.getRecordId().isNull())
		{
			xr.setRecordId(getKey(tab));
			
//			if ((++numXmitsSaved % 100) == 0)
				debug1("Saving new msg with dcp addr=" + xr.getDcpAddress() 
					+ " at time " + debugSdf.format(xmitTime)
					+ ", day=" + dayNum + " to " + tab);
			
			PreparedStatement ps = getInsertStatement(suffix);
			if (ps == null)
			{
				String msg = module + ":saveDcpTranmission() " +
							" Invalid PreparedStatement, ps = " + ps;
				warning(msg);
				throw new DbIoException(msg);
			}
			try
			{
				fillWriteStatement(ps, xr, base64data);
				ps.executeUpdate();
			}
			catch(SQLException ex)
			{
				throw new DbIoException(module + ":saveDcpTranmission failed: "
					+ " xmitTime=" + debugSdf.format(xmitTime)
					+ ", tab='" + tab + "': "
					+ ex);
			}
		}
		else
		{
			debug1("Updating msg at time " + debugSdf.format(xmitTime)
				+ ", day=" + dayNum + " to " + tab);
			
			PreparedStatement ps = getUpdateStatement(suffix);
			if (ps == null)
			{
				String msg = module + ":saveDcpTranmission() " +
							" Invalid Update PreparedStatement, ps = " + ps;
				warning(msg);
				throw new DbIoException(msg);
			}
			try 
			{ 
				fillWriteStatement(ps, xr, base64data);
				// 17th param is the record_id in the where clause
				ps.setLong(17, xr.getRecordId().getValue());
				ps.executeUpdate();
				String q = "delete from DCP_TRANS_DATA_" + suffix
					+ " where RECORD_ID = " + xr.getRecordId();
				doModify(q);
			}
			catch(SQLException ex)
			{
				throw new DbIoException(module + ":saveDcpTranmission: " +
						"updating XmitRecord " + ex);
			}
		}
		
		// Very long message have extended blocks stored in the
		// DATA_TRANS_DATA_SUFFIX table. Write blocks in 4000 byte chunks.
		tab = "DCP_TRANS_DATA_" + suffix;
		for (int blockNum = 0; base64data.length() > 4000; blockNum++)
		{
			base64data = base64data.substring(4000);
			String toWrite = base64data.length() <= 4000 ? base64data : 
				base64data.substring(0, 4000);
			String q = "insert into " + tab + " values("
				+ xr.getRecordId() + ", " + blockNum + ", "
				+ sqlString(new String(toWrite)) + ")";
			doModify(q);
		}
	}

	/**
	 * Helper method to return a prepared insert statement with the given
	 * suffix.
	 * @return the prepared statement.
	 */
	protected PreparedStatement getInsertStatement(String suffix)
	{
		try
		{
			int idx = Integer.parseInt(suffix.trim()) - 1;
			if (insertStatement[idx] == null)
			{
				String tab = "DCP_TRANS_" + suffix;
				String q = "INSERT INTO " + tab;
				q = q + 
					" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
							"?, ?, ?, ?, ?, ?)";
				insertStatement[idx] = db.getConnection().prepareStatement(q);
				info("Created new prepared statement for '" + q + "'");
			}
			return insertStatement[idx];
		}
		catch(Exception ex)
		{
			warning("getInsertStatement: " + ex);
			return null;
		}
	}
	
	
	/**
	 * The update statement must have the same params in the same order as the
	 * insert statement so that fillWriteStatement() will work for both.
	 * @return the prepared statement.
	 */
	protected PreparedStatement getUpdateStatement(String suffix)
	{
		try
		{
			int idx = Integer.parseInt(suffix.trim()) - 1;
			if (updateXmit[idx] == null)
			{
				String tab = "DCP_TRANS_" + suffix;

				StringBuilder q = new StringBuilder();
				q.append("UPDATE "); 
				q.append(tab);//table 
				q.append(" SET ");
				StringTokenizer st = new StringTokenizer(this.dcpTransFields, " ,");
				boolean first = true;
				while(st.hasMoreTokens())
				{
					if (!first)
						q.append(", ");
					first = false;
					q.append(st.nextToken() + " = ?");
				}
				// The final token in the update statement is for the where clause.
				q.append(" where record_id = ? ");
				
				updateXmit[idx] = db.getConnection().prepareStatement(q.toString());
				info("Created new prepared statement for '" + q + "'");
			}
			return updateXmit[idx];
		}
		catch(Exception ex)
		{
			warning("getUpdateStatement: " + ex);
			return null;
		}
	}

	
	/**
	 * Helper method to fill a prepared statement with the values from a
	 * XmitRecord.
	 * @param ps the prepared statement
	 * @param xr the Xmit Record
	 * @param base64data the message data encoded into base64
	 */
	protected void fillWriteStatement(PreparedStatement ps, DcpMsg xr, String base64data)
		throws SQLException
	{
		try
		{
			ps.setLong(1, xr.getRecordId().getValue());
			ps.setString(2, ""+XmitMediumType.flags2type(xr.getFlagbits()).getCode());
			ps.setString(3, xr.getDcpAddress().toString());
			ps.setLong(4, xr.getLocalReceiveTime().getTime());
			ps.setLong(5, xr.getXmitTime().getTime());
			ps.setString(6, xr.getXmitFailureCodes());
			
			XmitWindow xw = xr.getXmitTimeWindow();
			if (xw == null)
			{
				ps.setNull(7, Types.INTEGER);
				ps.setNull(8, Types.INTEGER);
				ps.setNull(9, Types.INTEGER);
			}
			else
			{
				ps.setInt(7, xw.thisWindowStart);
				ps.setInt(8, xw.windowLengthSec);
				ps.setInt(9, xw.xmitInterval);
			}
			
			if (xr.getCarrierStart() != null)
				ps.setLong(10, xr.getCarrierStart().getTime());
			else
				ps.setNull(10, Types.BIGINT);
			
			if (xr.getCarrierStop() != null)
				ps.setLong(11, xr.getCarrierStop().getTime());
			else
				ps.setNull(11, Types.BIGINT);
			
			ps.setInt(12, xr.getFlagbits());
			ps.setInt(13, xr.getGoesChannel());
			ps.setFloat(14, (float)xr.getBattVolt());
			ps.setInt(15, xr.getMessageLength());
			if (base64data.length() > 4000)
				base64data = base64data.substring(0, 4000);
			ps.setString(16, base64data);
			
		}
		catch(SQLException ex)
		{
			warning("fillDcpInsertStatement: " + ex);
			throw ex;
		}
	}

	@Override
	public DcpMsg readDcpMsg(int dayNum, long recordId)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
			+ " where RECORD_ID = " + recordId;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
			{
				DcpMsg ret = rs2XmitRecord(rs);
				ret.setDayNumber(dayNum);
				fillCompleteMsg(ret);
				return ret;
			}
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "readDcpMsg: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public long getFirstRecordID(int dayNum)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return -1;
		String q = "select min(record_id) from DCP_TRANS_" + suffix;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
				return rs.getLong(1);
			else
				return -1;
		}
		catch (SQLException ex)
		{
			String msg = "getFirstRecordId: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	@Override
	public long getLastRecordID(int dayNum)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return -1;
		String q = "select max(record_id) from DCP_TRANS_" + suffix;
		ResultSet rs = doQuery(q);
		try
		{
			if (rs != null && rs.next())
				return rs.getLong(1);
			else
				return -1;
		}
		catch (SQLException ex)
		{
			String msg = "getFirstRecordId: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	
	
	@Override
	public DcpMsg findDcpTranmission(XmitMediumType mediumType, String mediumId, Date timestamp)
		throws DbIoException
	{
		int dayNum = msecToDay(timestamp.getTime());
		DcpMsg ret = doFindDcpTranmission(mediumType, mediumId, timestamp.getTime(), dayNum);
		
		// Use a 2-min fudge factor. Thus the matching record may
		// have been saved in a different day if I'm on the hairy edge.
		if (ret == null)
		{
			long msecOfDay = timestamp.getTime()%MS_PER_DAY;
			if (msecOfDay < TIME_FUDGE)
				ret = doFindDcpTranmission(mediumType, mediumId, timestamp.getTime(), dayNum-1);
			else if (msecOfDay > (MS_PER_DAY - TIME_FUDGE))
				ret = doFindDcpTranmission(mediumType, mediumId, timestamp.getTime(), dayNum+1);
		}
		return ret;
	}
	
	private DcpMsg doFindDcpTranmission(XmitMediumType mediumType, String mediumId,
		long msecTime, int dayNum)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		PreparedStatement ps = getSelectByIdAndTime(suffix);
		if (ps == null)
		{
			String msg = "findDcpTranmission() " +
						" Invalid PreparedStatement, ps = " + ps;
			warning(msg);
			throw new DbIoException(msg);
		}
		
		//Msg time stamps may vary. Search for msg within fudge time.
		long dBefore = msecTime - TIME_FUDGE;
		long dAfter  = msecTime + TIME_FUDGE;
		
		try
		{
			ps.setString(1, "" + mediumType.getCode());
			ps.setString(2, mediumId);
			ps.setLong(3, dBefore);
			ps.setLong(4, dAfter);
			ResultSet rs = ps.executeQuery();
			if (rs != null && rs.next())
			{
				DcpMsg ret = rs2XmitRecord(rs);
				ret.setDayNumber(dayNum);
				fillCompleteMsg(ret);
				return ret;
			}
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = module + ":findDcpTranmission Cannot parse xmit " +
					"result: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	/**
	 * Helper method to return a prepared select statement with the given
	 * suffix.
	 * @return the prepared statement.
	 */
	protected PreparedStatement getSelectByIdAndTime(String suffix)
	{
		String q = null;
		try
		{
			int idx = Integer.parseInt(suffix.trim()) - 1;
			if (selectByIdAndTime[idx] == null)
			{
				String tab = "DCP_TRANS_" + suffix;
				q = "SELECT " + dcpTransFields + " FROM " + tab +
					" WHERE medium_type = ?" + 
					" AND medium_id = ?" +
					" AND transmit_time >= ?" +
					" AND transmit_time <= ?";
				selectByIdAndTime[idx] = db.getConnection().prepareStatement(q);
				info("Created select prepared " +
						"statement for '" + q + "'");
			}
			return selectByIdAndTime[idx];
		}
		catch(Exception ex)
		{
			String msg = "getSelectByIdAndTime failed for '" + q + "': " + ex;
			warning(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
			return null;
		}
	}
	
	/**
	 * Creates a XmitRecord from the SQL record
	 * 
	 * @param rs
	 * @return XmitRecord
	 * @throws DbIoException
	 */
	private DcpMsg rs2XmitRecord(ResultSet rs) 
		throws DbIoException
	{
		try
		{
			// Parse columns from the result set:
			//"record_id, medium_type, medium_id, local_recv_time, " +
			//"transmit_time, failure_codes, window_start_sod, window_length, xmit_interval, " +
			//"carrier_start, carrier_stop, flags, channel, battery, msg_length, msg_data";
			DbKey recId = DbKey.createDbKey(rs, 1);
			//String mediumType = rs.getString(2);
			String mediumId = rs.getString(3);
			long ms = rs.getLong(4);
			Date localRecvTime = new Date(ms);
			ms = rs.getLong(5);
			Date xmitTime = rs.wasNull() ? null : new Date(ms);
			String failCodes = rs.getString(6);
			int windowStart = rs.getInt(7);
			if (rs.wasNull()) windowStart = -1;
			int windowLength = rs.getInt(8);
			if (rs.wasNull()) windowLength = -1;
			int xmitInterval = rs.getInt(9);
			if (rs.wasNull()) xmitInterval = -1;
			ms = rs.getLong(10);
			Date carrierStart = rs.wasNull() ? null : new Date(ms);
			ms = rs.getLong(11);
			Date carrierStop = rs.wasNull() ? null : new Date(ms);
			int flags = rs.getInt(12);
			//int channel = rs.getInt(13);
			//if (rs.wasNull()) channel = -1;
			double battery = rs.getDouble(14);
			if (rs.wasNull()) battery = 0.0;
			int msgLength = rs.getInt(15);
			String base64data = rs.getString(16);
			byte data[] = Base64.decodeBase64(base64data.getBytes());
			
			XmitWindow xmitWindow = null;
			if (windowStart != -1 && xmitInterval > 0)
			{
				// If a xmit window is present, parse it.
				int firstWindowStart = windowStart;
				while(firstWindowStart - xmitInterval >= 0)
					firstWindowStart -= xmitInterval;
				xmitWindow = new XmitWindow(firstWindowStart, windowLength, xmitInterval, windowStart);
			}

			// Create DcpMsg and fill in the fields.
			DcpMsg xr = new DcpMsg();
			xr.setRecordId(recId);
			xr.setFlagbits(flags);
			xr.setLocalReceiveTime(localRecvTime);
			xr.setXmitTime(xmitTime);
			xr.setCarrierStart(carrierStart);
			xr.setCarrierStop(carrierStop);
			xr.setFailureCode(failCodes.charAt(0));
			for(int i=0; i<failCodes.length(); i++)
				xr.addXmitFailureCode(failCodes.charAt(i));
			xr.setData(data);
			xr.setDcpAddress(new DcpAddress(mediumId));
			if (msgLength > data.length)
			{
				xr.setMsgLength(msgLength);
Logger.instance().debug1("XmitRecordDAO.rs2XmitRecord read a partial message: data.len="
+ data.length + ", msgLength=" + msgLength);
			}
			xr.setBattVolt(battery);
			xr.setXmitWindow(xmitWindow);
			// Don't need to save channel -- it is read from GOES Header.
			// Don't need to save mediumType -- it is encapsulated in flag bits.
			
			return xr;
		}
		catch(SQLException ex)
		{
			String msg = module + ":rs2XmitRecord Cannot parse xmit " +
					"results: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void fillCompleteMsg(DcpMsg msg)
		throws DbIoException
	{
		// Note msg.msgLength the decoded length -- not the base64 length. So this is Okay.
		if (msg.getData().length >= msg.getMsgLength())
			return;
		String suffix = getDcpXmitSuffix(msg.getDayNumber(), false);
		if (suffix == null)
			return;
		String q = "select msg_data from dcp_trans_data_" + suffix
			+ " where record_id = " + msg.getRecordId()
			+ " order by block_num";
		ResultSet rs = doQuery(q);
		byte completeMsgData[] = new byte[msg.getMessageLength()];
		byte firstBlock[] = msg.getData();
		int cmdi = 0;
		for(; cmdi<firstBlock.length && cmdi < completeMsgData.length; cmdi++)
			completeMsgData[cmdi] = firstBlock[cmdi];
		try
		{
			while(rs != null && rs.next())
			{
				String base64data = rs.getString(1);
				byte data[] = Base64.decodeBase64(base64data.getBytes());
				for(int idx = 0; idx<data.length && cmdi < completeMsgData.length; idx++, cmdi++)
					completeMsgData[cmdi] = data[idx];
			}
			msg.setData(completeMsgData);
		}
		catch (SQLException ex)
		{
			warning("Error in fillCompleteMsg(" + msg.getDcpAddress() + ") "
				+ "dataLen=" + cmdi + ", totlen=" + completeMsgData.length + ": " + ex);
		}
	}

	@Override
	public int readXmitsByGroup(Collection<DcpMsg> results, int dayNum,
		NetworkList grp) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix + " a, "
			+ "NetworkListEntry b "
			+ "where a.medium_type = '" + 
				XmitMediumType.transportMediumType2type(grp.transportMediumType).getCode() + "' "
			+ " and b.networklistid = " + grp.getId()
			+ " and upper(a.medium_id) = upper(b.transportid)"
			+ " order by transmit_time";
			
		ResultSet rs = doQuery(q);
		int n = 0;
		try
		{
			while (rs != null && rs.next())
			{
				DcpMsg ret = rs2XmitRecord(rs);
				ret.setDayNumber(dayNum);
				results.add(ret);
				n++;
			}
		}
		catch (SQLException ex)
		{
			String msg = "readXmitsByGroup: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}

		return n;
	}
	
	@Override
	public int readXmitsByChannel(Collection<DcpMsg> results, int dayNum,
		int chan) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
			+ " where channel = " + chan
			+ " order by transmit_time";
			
		ResultSet rs = doQuery(q);
		int n = 0;
		try
		{
			while (rs != null && rs.next())
			{
				DcpMsg ret = rs2XmitRecord(rs);
				ret.setDayNumber(dayNum);
				results.add(ret);
			}
		}
		catch (SQLException ex)
		{
			String msg = "readXmitsByChannel: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}

		return n;
	}

	@Override
	public int readXmitsByMediumId(Collection<DcpMsg> results, int dayNum, 
		XmitMediumType mediumType, String mediumId)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
			+ " where medium_type = '" + mediumType.getCode() + "' "
			+ " and medium_id = " + sqlString(mediumId)
			+ " order by transmit_time";
			
		ResultSet rs = doQuery(q);
		int n = 0;
		try
		{
			while (rs != null && rs.next())
			{
				DcpMsg ret = rs2XmitRecord(rs);
				ret.setDayNumber(dayNum);
				results.add(ret);
			}
		}
		catch (SQLException ex)
		{
			String msg = "readXmitsByGroup: Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}

		return n;
	}

	@Override
	public void deleteDcpXmitsBefore(int dayNum) throws DbIoException
	{
		for(XmitDayMapEntry xdme : dayNumSuffixMap)
			if (xdme.dayNum < dayNum)
				clearTable(xdme.suffix);
		loadDayNumSuffixMap();
	}
	
	@Override
	public Date getLastLocalRecvTime() throws DbIoException
	{
		loadDayNumSuffixMap();
		XmitDayMapEntry latestDay = null;
		for(XmitDayMapEntry xdme : dayNumSuffixMap)
			if (xdme.dayNum != -1
			 && (latestDay == null || xdme.dayNum > latestDay.dayNum))
				latestDay = xdme;
Logger.instance().debug2("XmitRecordDAO.getLastLocalRecvTime: latestDay=" + 
(latestDay == null ? "null" : ""+latestDay.dayNum));
		if (latestDay == null)
			return null;
		
		String q = "select max(local_recv_time) from " + "DCP_TRANS_" + latestDay.suffix;
Logger.instance().debug2("XmitRecordDAO.getLastLocalRecvTime: " + q);
		ResultSet rs = doQuery(q);
		try
		{
			if (rs.next())
				return new Date(rs.getLong(1));
		}
		catch(SQLException ex)
		{
			warning("Error in query '" + q + "': " + ex);
		}
		return null;
	}
	
}
