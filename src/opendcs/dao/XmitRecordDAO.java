/*
 * $Id$
 * 
 * $Log$
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 */
package opendcs.dao;

import ilex.util.Base64;
import ilex.util.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;

import opendcs.dai.XmitRecordDAI;

import lrgs.common.DcpAddress;
import decodes.dcpmon.DcpGroup;
import decodes.dcpmon.XmitRecord;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;


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
	
	/** Milliseconds per day */
	public static final long MS_PER_DAY = 1000L*3600L*24L;
	
	private String tableRootName = "";
	private int numXmitsSaved = 0;
	
	/** Prepared Statements for inserting into DCP Mon my-dcps table */
	private PreparedStatement insertMy[] = null;
	
	/** Prepared Statements for updating into DCP Mon my-dcps table */
	private PreparedStatement updateXmit[] = null;

	/** Prepared Statements used when trying to find existing DCPs in the
	 * my_dcp_tran<sufix> tables */
	private PreparedStatement findDcp[];

	protected Calendar resultSetCalendar = null;
	
	private String dcpTransFields = 
		"record_id, dcp_address, goes_time, failure_codes, "+
		"signal_strength, msg_length, goes_channel, freq_offset, " +
		"mod_index, window_start_sod, window_length, xmit_interval, " +
		"carrier_start, carrier_end, flags, source_id, domsattime, " +
		"battery, uplinkcarrier";


	/**
	 * Construct a new object after a database connection
	 * @param tsdb the database
	 * @param maxXmitDays maximum number of days to store for DCP mon.
	 */
	public XmitRecordDAO(DatabaseConnectionOwner tsdb, int maxXmitDays)
	{
		super(tsdb, module);
		tableRootName = 
			db.getTsdbVersion() < TsdbDatabaseVersion.VERSION_9 ? "my_dcp_trans_" : "dcp_trans_";
		insertMy = new PreparedStatement[maxXmitDays];
		findDcp = new PreparedStatement[maxXmitDays];
		updateXmit = new PreparedStatement[maxXmitDays];
		for(int i=0; i<maxXmitDays; i++)
			insertMy[i] = findDcp[i] = updateXmit[i] = null;
		TimeZone tz = TimeZone.getTimeZone(DecodesSettings.instance().sqlTimeZone);

		resultSetCalendar = Calendar.getInstance(tz);
	}

	/**
	 * This method inserts a timestamp into a prepared statement for writing/selecting the db.
	 * @param ps the prepared statement
	 * @param column the column number
	 * @param msecTime long value representing msec since the epoch
	 * @throws SQLException 
	 */
	protected void setPrepStatementTimestamp(PreparedStatement ps, int column, long msecTime) 
		throws SQLException
	{
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
			ps.setLong(column, msecTime);
		else
		{
			if (msecTime == 0L)
				ps.setNull(column, Types.TIMESTAMP);
			else
				ps.setTimestamp(column, new Timestamp(msecTime), resultSetCalendar);
		}
	}
	
	/**
	 * @return msec time from the XmitRecord prepared statement.
	 */
	protected long getResultSetTimestamp(ResultSet rs, int column)
		throws SQLException
	{
		if (db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_9)
		{
			Date d = db.getFullDate(rs, column);
			return d == null ? 0L : d.getTime();
		}
		
		java.sql.Timestamp ts = rs.getTimestamp(column, resultSetCalendar);
		if (rs.wasNull())
			return 0L;
		else
			return ts.getTime();
	}

	
	@Override
	public Date getLatestTimeStamp(int dayNum) 
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		
		String tab = tableRootName + suffix;
		String q = "SELECT MAX(goes_time) as maxDate FROM " + tab;
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
	public void saveDcpTranmission(XmitRecord xr)
		throws DbIoException
	{
		int dayNum = xr.getDayNum();
		int sod = xr.getSecOfDay();
		String suffix = getDcpXmitSuffix(dayNum, true);
		if (suffix == null)
			return;
		String tab = tableRootName + suffix;

		if (xr.getRecordId().isNull())
		{
			xr.setRecordId(getKey(tab));
			
			if ((++numXmitsSaved % 100) == 0)
				debug1("Saving day=" + dayNum + ", sod=" + sod + " to " + tab);
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
				fillDcpInsertStatement(ps, xr);
				ps.executeUpdate();
			}
			catch(SQLException ex)
			{
				throw new DbIoException(module + ":saveDcpTranmission failed: "
					+ ", sod=" + sod + " to '" + tab + "': "
					+ ex);
			}
		}
		else
		{
			Logger.instance().debug2(module + 
					":saveDcpTranmission Updating day=" + dayNum 
				+ ", sod=" + sod + " to '" + tab + "'");
			
			PreparedStatement ps = getUpdateStatement(suffix);
			if (ps == null)
			{
				String msg = module + ":saveDcpTranmission() " +
							" Invalid Update PreparedStatement, ps = " + ps;
				warning(msg);
				throw new DbIoException(msg);
			}
			fillDcpUpdateStatement(ps, xr);
			try 
			{ 
				ps.executeUpdate();
			}
			catch(SQLException ex)
			{
				throw new DbIoException(module + ":saveDcpTranmission: " +
						"updating XmitRecord " + ex);
			}
			//OLD Code
			//String q = makeUpdateDcpXmitQuery(tab, xr);
			//doModify(q);
			//commit();
		}
	}

	@Override
	public XmitRecord findDcpTranmission(DcpAddress dcpAddress, Date timestamp, int dayNum)
		throws DbIoException
	{
		XmitRecord ret = doFindDcpTranmission(dcpAddress, timestamp.getTime(), dayNum);
		
		// Since I'm using a 2-min fudge factor, the matching record may
		// have been saved in a different day if I'm on the hairy edge.
		if (ret == null)
		{
			long msecOfDay = timestamp.getTime()%MS_PER_DAY;
			if (msecOfDay < 120000L)
				ret = doFindDcpTranmission(dcpAddress, timestamp.getTime(), dayNum-1);
			else if (msecOfDay > (MS_PER_DAY - 12000L))
				ret = doFindDcpTranmission(dcpAddress, timestamp.getTime(), dayNum+1);
		}
		return ret;
	}
	
	private XmitRecord doFindDcpTranmission(
		DcpAddress dcpAddress, long msecTime, int dayNum)
		throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		PreparedStatement ps = getSelectXmitStatement(suffix);
		if (ps == null)
		{
			String msg = "findDcpTranmission() " +
						" Invalid PreparedStatement, ps = " + ps;
			warning(msg);
			throw new DbIoException(msg);
		}
		
		//Msg time stamps may vary. Search for msg within 2 minutes.
		long dBefore = msecTime - 120000;
		long dAfter = msecTime + 120000;
		
		String  mediumId = dcpAddress.toString();
		try
		{
			ps.setString(1, mediumId);
			setPrepStatementTimestamp(ps, 2, dBefore);
			setPrepStatementTimestamp(ps, 3, dAfter);
			ResultSet rs = ps.executeQuery();
			if (rs != null && rs.next())
				return rs2XmitRecord(rs);
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
	protected PreparedStatement getSelectXmitStatement(String suffix)
	{
		try
		{
			int idx = Integer.parseInt(suffix.trim()) - 1;
			if (findDcp[idx] == null)
			{
				String tab = tableRootName + suffix;
				String q = "SELECT " + dcpTransFields + " FROM " + tab +
				" WHERE dcp_address = ?" + 
				" AND goes_time >= ?" +
				" AND goes_time <= ?";
				findDcp[idx] = db.getConnection().prepareStatement(q);
				info("Created select prepared " +
						"statement for '" + q + "'");
			}
			return findDcp[idx];
		}
		catch(Exception ex)
		{
			warning("getSelectXmitStatement: " + ex);
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
	private decodes.dcpmon.XmitRecord rs2XmitRecord(ResultSet rs) 
	throws DbIoException
	{
		try
		{
			DbKey recId = DbKey.createDbKey(rs, 1);
			String addr = rs.getString(2);
			DcpAddress dcpAddress = new DcpAddress(addr);
			Date ts = db.getFullDate(rs, 3);
			long ts_ms = ts.getTime();
			int sod = (int)((ts_ms % MS_PER_DAY) / 1000L);
			int dayNum = (int)(ts_ms / MS_PER_DAY);
			XmitRecord xr = new decodes.dcpmon.XmitRecord(dcpAddress, sod, dayNum);

			xr.setRecordId(recId);
			
			String s = rs.getString(4);
			for(int i=0; i<s.length(); i++)
				xr.addCode(s.charAt(i));

			xr.setSignalStrength(rs.getInt(5));
			xr.setMsgLength(rs.getInt(6));
			xr.setGoesChannel(rs.getInt(7));
			xr.setFreqOffset(rs.getInt(8));
			s = rs.getString(9);
			if (s.length() > 0)
				xr.setModIndex(s.charAt(0));

			xr.setWindowStartSec(rs.getInt(10));
			xr.setWindowLength(rs.getInt(11));
			xr.setXmitInterval(rs.getInt(12));
			xr.setCarrierStart(getResultSetTimestamp(rs, 13));
			xr.setCarrierEnd(getResultSetTimestamp(rs, 14));
			xr.addFlags(rs.getInt(15));
			xr.setSourceId(DbKey.createDbKey(rs, 16));
			xr.setDomsatTimeMsec(getResultSetTimestamp(rs, 17));

			xr.setBattVolt(rs.getFloat(18));
			if (rs.wasNull())
				xr.setBattVolt((float)0.0);
		
			xr.setDrgsCode(rs.getString(19));
			xr.clearDbChangedFlags();
			
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
	public int readXmitsByGroup(Collection<XmitRecord> results, int dayNum,
		DcpGroup grp) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		
//		 By Group this is the MS command 
//			Do the following select:  
//				Select <fields> from my_dcp_trans_<day>, networklistentry 
//				where my_dcp_trans<day>.dcp_address = 
//				networklistentry.transportid and 
//				networklistentry.networklistid = groupId
//				The day number will be passed to the method 
//				(the above statement will be repeated for how 
//				many days the user has selected to go back)
		String tab = tableRootName + suffix;
		String transFields = 
			"a.record_id," + 
			"a.dcp_address, " +
			"a.goes_time, " + 
			"a.failure_codes, " +
			"a.signal_strength, " +
			"a.msg_length, " +  
			"a.goes_channel, " + 
			"a.freq_offset, " +  
			"a.mod_index, " +
			"a.window_start_sod, " +  
			"a.window_length, " +
			"a.xmit_interval, " + 
			"a.carrier_start, " +
			"a.carrier_end, " + 
			"a.flags, " +
			"a.source_id, " + 
			"a.domsattime, " +
			"a.battery, " + 
			"a.uplinkcarrier";
		//No need to read the raw message field in this case
		String q = "SELECT " + transFields + " FROM " + tab + " a" +
				", networklistentry b" + 
				" WHERE a.dcp_address = " +
				"b.transportid AND " +
				"b.networklistid = " + grp.getGroupId();
		ResultSet rs = doQuery(q);
		return parseDcpXmitResults(rs, results, false);
	}
	
	/**
	 * Parses the results from a query to one of the XMIT Record tables.
	 * @param rs the result set from the SQL query
	 * @param results array in which to store xmit records
	 * @return the number of results added
	 */
	protected int parseDcpXmitResults(ResultSet rs, 
		Collection<XmitRecord> results, boolean hasRawMsg)
		throws DbIoException
	{
		try
		{
			int numAdded = 0;
			while(rs.next())
			{
				DbKey recId = DbKey.createDbKey(rs, 1);
				String addr = rs.getString(2);
				DcpAddress dcpAddress = new DcpAddress(addr);

				Date ts = db.getFullDate(rs, 3);
				long ts_ms = ts.getTime();
				int sod = (int)((ts_ms % MS_PER_DAY) / 1000L);
				int dayNum = (int)(ts_ms / MS_PER_DAY);
				XmitRecord xr = new XmitRecord(dcpAddress, sod, dayNum);

				xr.setRecordId(recId);
				String s = rs.getString(4);
				for(int i=0; i<s.length(); i++)
					xr.addCode(s.charAt(i));

				xr.setSignalStrength(rs.getInt(5));
				xr.setMsgLength(rs.getInt(6));
				xr.setGoesChannel(rs.getInt(7));
				xr.setFreqOffset(rs.getInt(8));
				s = rs.getString(9);
				if (s.length() > 0)
					xr.setModIndex(s.charAt(0));

				xr.setWindowStartSec(rs.getInt(10));
				xr.setWindowLength(rs.getInt(11));
				xr.setXmitInterval(rs.getInt(12));
				xr.setCarrierStart(getResultSetTimestamp(rs, 13));
				xr.setCarrierEnd(getResultSetTimestamp(rs, 14));
				xr.addFlags(rs.getInt(15));
				xr.setSourceId(DbKey.createDbKey(rs, 16));
				xr.setDomsatTimeMsec(getResultSetTimestamp(rs, 17));

				xr.setBattVolt(rs.getFloat(18));
				if (rs.wasNull())
					xr.setBattVolt((float)0.0);
			
				xr.setDrgsCode(rs.getString(19));
				
				if (hasRawMsg)
					xr.setRawMsg(rs.getBytes(20));
				
				results.add(xr);
				numAdded++;
			}
			Logger.instance().debug1("Parsed " + numAdded + " results.");
			return numAdded;
		}
		catch(SQLException ex)
		{
			String msg = module + ":parseDcpXmitResults Cannot parse xmit " +
					"results: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}


	@Override
	public int readXmitsByChannel(Collection<XmitRecord> results, int dayNum,
		int chan) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		String tab = tableRootName + suffix;
		String q = "SELECT " + dcpTransFields + " FROM " + tab 
			+ " WHERE goes_channel = " + chan;
		ResultSet rs = doQuery(q);
		return parseDcpXmitResults(rs, results, false);
	}

	@Override
	public int readXmitsByDcpAddress(Collection<XmitRecord> results,
		int dayNum, DcpAddress dcpAddress) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return 0;
		String tab = tableRootName + suffix;
		String  mediumId = dcpAddress.toString();
		String q = "SELECT " + dcpTransFields + " FROM " + tab + 
			" WHERE dcp_address = " + sqlString(mediumId);
		ResultSet rs = doQuery(q);
		return parseDcpXmitResults(rs, results, false);
	}

	@Override
	public XmitRecord readXmitRawMsg(int dayNum, DcpAddress dcpAddress,
		Date timestamp) throws DbIoException
	{
		String suffix = getDcpXmitSuffix(dayNum, false);
		if (suffix == null)
			return null;
		String  mediumId = dcpAddress.toString();
		String tab = tableRootName + suffix;
		try
		{
			String q = "SELECT " + dcpTransFields + ", raw_msg FROM " + tab +
				" WHERE dcp_address = ?" + " AND goes_time = ?";
			PreparedStatement ps = db.getConnection().prepareStatement(q);
			if (ps == null)
			{
				String msg = "readXmitsRawMsg() " +
							" Invalid PreparedStatement, ps = " + ps;
				warning(msg);
				throw new DbIoException(msg);
			}
			ps.setString(1, mediumId);
			setPrepStatementTimestamp(ps, 2, timestamp.getTime());
			ResultSet rs = ps.executeQuery();
			ArrayList<XmitRecord> xrs = new ArrayList<XmitRecord>();
			int n = this.parseDcpXmitResults(rs, xrs, true);
			if (n > 0)
			{
				XmitRecord xr = xrs.get(0);
				byte[] temp = xr.getRawMsg();
				if (temp != null)
					xr.setRawMsg(Base64.decodeBase64(temp));
				return xr;
			}
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = module + ":readXmitsRawMsg Cannot parse xmit " +
					"result: " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}

	@Override
	public void deleteDcpXmitsBefore(int dayNum) throws DbIoException
	{
		for(XmitDayMapEntry xdme : dayNumSuffixMap)
			if (xdme.dayNum < dayNum)
			{
				String tab = tableRootName + xdme.suffix;
				String q = "DELETE from " + tab;
				doModify(q);
				info(module + ": deleteDcpXmitsBefore deleting " +
						"records = " + q);
				q = "UPDATE dcp_trans_day_map SET day_number = null "
					+ "WHERE day_number = " + xdme.dayNum;
			}
		loadDayNumSuffixMap();
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
			doModify(q);
			return firstFree.suffix;
		}

		if (dayNum < oldestDay.dayNum)
		{
			warning(method + "Cannot allocate table "
				+ "for old day number "	+ dayNum + ", oldest day in storage is " 
				+ oldestDay.dayNum);
			return null;
		}

		// Delete all records from the oldest day.
		String tab = tableRootName + oldestDay.suffix;

		String q = "DELETE FROM " + tab;
		doModify(q);
		info(method + 
			"Cleared table " + tab + " for day num "
			+ oldestDay.dayNum + " to make room for new day number " + dayNum
			+ ", executed '" + q + "'");
		q = "commit";
		
		oldestDay.dayNum = dayNum;

		q = "UPDATE dcp_trans_day_map SET day_number = " + dayNum
			+ " WHERE table_suffix = " + sqlString(oldestDay.suffix);
		doModify(q);
		return oldestDay.suffix;
	}
	
	/**
	 * Loads the internal day-number to suffix map.
	 */
	protected void loadDayNumSuffixMap()
		throws DbIoException
	{
		String q = "SELECT * FROM dcp_trans_day_map ORDER BY table_suffix";
		ResultSet rs = doQuery(q);
		dayNumSuffixMap.clear();
		if (rs == null)
			info("rs from '" +q + "' is null!!");
		int latestDayNum = -1;
		try
		{
			while(rs.next())
			{
				String suffix = rs.getString(1);
				int dayNum = rs.getInt(2);
				if (rs.wasNull())
					dayNum = -1;
				else if (dayNum > latestDayNum)
					latestDayNum = dayNum;
					
				dayNumSuffixMap.add(new XmitDayMapEntry(suffix, dayNum));
			}
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Error iterating from query '"
			 	+ q + "': " + ex);
		}
		info("loadDayNumSuffixMap latestDayNum=" + latestDayNum + ", "
			+ new Date(latestDayNum*MS_PER_DAY));
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
			if (insertMy[idx] == null)
			{
				String tab = tableRootName + suffix;
				String q = "INSERT INTO " + tab;
				q = q + 
					" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, " +
							"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				insertMy[idx] = db.getConnection().prepareStatement(q);
				info("Created new prepared statement for '" + q + "'");
			}
			return insertMy[idx];
		}
		catch(Exception ex)
		{
			warning("getInsertStatement: " + ex);
			return null;
		}
	}
	
	/**
	 * Helper method to fill a prepared statement with the values from a
	 * XmitRecord.
	 * @param ps the prepared statement
	 * @param xr the Xmit Record
	 */
	protected void fillDcpInsertStatement(PreparedStatement ps, 
			decodes.dcpmon.XmitRecord xr)
		throws SQLException
	{
		try
		{
			ps.setLong(1, xr.getRecordId().getValue());
			String mediumId = xr.getDcpAddress().toString();
			ps.setString(2, mediumId);
			setPrepStatementTimestamp(ps, 3, xr.getGoesTimeMsec());
			ps.setString(4, xr.failureCodes());
			ps.setInt(5, xr.getSignalStrength());
			ps.setInt(6, xr.getMsgLength());
			ps.setInt(7, xr.getGoesChannel());
			ps.setInt(8, xr.getFreqOffset());
			String s = "" + xr.getModIndex();
			if (s.length() < 1) s = "N";
			else if (s.length() > 1) s = s.substring(0, 1);
			ps.setString(9, s);
			ps.setInt(10, xr.getWindowStartSec());
			ps.setInt(11, xr.getWindowLength());
			ps.setInt(12, xr.getXmitInterval());
			setPrepStatementTimestamp(ps, 13, xr.getCarrierStart());
			setPrepStatementTimestamp(ps, 14, xr.getCarrierEnd());
			ps.setInt(15, xr.getFlags());
			ps.setLong(16, xr.getSourceId().getValue());

			setPrepStatementTimestamp(ps, 17, xr.getDomsatTimeMsec());

			ps.setDouble(18, xr.getBattVolt());
			
			ps.setString(19, xr.getUpLinkCarrier());
			
			//use the base64 to store the raw msg.
			if (xr.getRawMsg() != null)
			{
				//Get the rawMsg in bytes - convert to String
				ps.setString(20, encodeBase64(xr.getRawMsg()));
			}
			else
				ps.setString(20, null);
		}
		catch(SQLException ex)
		{
			warning("fillDcpInsertStatement: " + ex);
			throw ex;
		}
	}
	
	/**
	 * Helper method to return a prepared update statement with the given
	 * suffix.
	 * @return the prepared statement.
	 */
	protected PreparedStatement getUpdateStatement(String suffix)
	{
		try
		{
			int idx = Integer.parseInt(suffix.trim()) - 1;
			if (updateXmit[idx] == null)
			{
				String tab = tableRootName + suffix;

				StringBuilder q = new StringBuilder();
				q.append("UPDATE "); 
				q.append(tab);//table 
				q.append(" SET ");
				q.append("dcp_address = ?" );
				q.append(", goes_time = ?");
				q.append(", failure_codes = ?");
				q.append(", signal_strength = ?");
				q.append(", msg_length = ?");
				q.append(", goes_channel = ?");
				q.append(", freq_offset = ?");		
				q.append(", mod_index = ?");
				q.append(", window_start_sod = ?");
				q.append(", window_length = ?");
				q.append(", xmit_interval = ?");
				q.append(", carrier_start = ?");
				q.append(", carrier_end = ?");
				q.append(", flags = ?");
				q.append(", source_id = ?");
				q.append(", domsatTime = ?");
				q.append(", battery = ?");
				q.append(", uplinkcarrier = ?");
				q.append(", raw_msg = ?");			
				q.append(" where record_id = ? ");
				
				updateXmit[idx] = db.getConnection().prepareStatement(q.toString());
				info("Created new prepared statement for '"
					+ q + "'");
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
	 * Helper method to fill a prepared statement with the values from an
	 * existing XmitRecord.
	 * @param ps the prepared statement
	 * @param xr the Xmit Record
	 */
	protected void fillDcpUpdateStatement(PreparedStatement ps, XmitRecord xr)
	{
		try
		{
			//Logger.instance().debug2("Updating record " + xr.getRecordId()
			//	+ ": " + xr.getChanges());
			String mediumId = xr.getDcpAddress().toString();
			ps.setString(1, mediumId);
			setPrepStatementTimestamp(ps, 2, xr.getGoesTimeMsec());
			ps.setString(3, xr.failureCodes());
			ps.setInt(4, xr.getSignalStrength());
			ps.setInt(5, xr.getMsgLength());
			ps.setInt(6, xr.getGoesChannel());
			ps.setInt(7, xr.getFreqOffset());
			ps.setString(8, "" + xr.getModIndex());
			ps.setInt(9, xr.getWindowStartSec());
			ps.setInt(10, xr.getWindowLength());
			ps.setInt(11, xr.getXmitInterval());
			setPrepStatementTimestamp(ps, 12, xr.getCarrierStart());
			setPrepStatementTimestamp(ps, 13, xr.getCarrierEnd());
			ps.setInt(14, xr.getFlags());
			ps.setLong(15, xr.getSourceId().getValue());

			setPrepStatementTimestamp(ps, 16, xr.getDomsatTimeMsec());

			ps.setDouble(17, xr.getBattVolt());
			
			ps.setString(18, xr.getUpLinkCarrier());
			
			//use the base64 to store the raw msg.
			if (xr.getRawMsg() != null)
			{
				//Get the rawMsg in bytes - convert to String
				ps.setString(19, encodeBase64(xr.getRawMsg()));
			}
			else // this is for missing message where we do not store raw data
				ps.setString(19, null);
			
			ps.setLong(20, xr.getRecordId().getValue());
		}
		catch(SQLException ex)
		{
			warning("fillDcpUpdateStatement: " + ex);
		}
	}


	/**
	 * 
	 * @param data
	 * @return string representation of the encoded byte array
	 */
	private String encodeBase64(byte[] data)
	{
		byte[] tempRaw = Base64.encodeBase64(data);
		//Get the rawMsg in bytes - convert to String
		return new String(tempRaw);
	}

}
