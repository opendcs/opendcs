/*
 * Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 * 
 * Copyright 2013 The OpenDCS Consortium
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package opendcs.dao;

import ilex.util.Base64;
import ilex.util.Logger;

import java.sql.Connection;
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
import decodes.dcpmon.XmitRecSpec;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * As of 2023-12-24 This class no longer caches prepared statements
 * and should be used like any other DAO. Statement Cache is performed at the
 * Database level.
 *
 * @author mmaloney Mike Maloney, Cove Software, LLC
 * @author Mike Neilson
 */
public class XmitRecordDAO
    extends DaoBase implements XmitRecordDAI
{
    private final Logger logger = Logger.instance();
    public static final String module = "XmitRecordDao";
    protected static int numDaysStorage=5;

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
    }

    /**
      * Convenience method to convert msec time value to day number.
      * @param msec the msec value
      * @return day number (0 = Jan 1, 1970)
      */
    public static int msecToDay(long msec)
    {
        return (int)(msec / MS_PER_DAY);
    }

    /**
      * Convenience method to conver msec time value to second of day.
      * @param msec the Java time value.
      * @return second-of-day
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
        dayNumSuffixMap.clear();
        final int[] latestDayNum = new int[1];
        latestDayNum[0]=-1;
        try
        {
            doQuery(q, rs ->
            {
                String suffix = rs.getString(1);
                int dayNum = rs.getInt(2);
                if (rs.wasNull())
                {
                    dayNum = -1;
                }
                else if (dayNum > latestDayNum[0])
                {
                    latestDayNum[0] = dayNum;
                }
                dayNumSuffixMap.add(new XmitDayMapEntry(suffix, dayNum));
            });
            dayNumSuffixMapLoadedMsec = System.currentTimeMillis();
        }
        catch(SQLException ex)
        {
            throw new DbIoException("Error iterating from query '" + q + "': " + ex,ex);
        }
        dayNumSuffixMapLoadedMsec = System.currentTimeMillis();
        info("loadDayNumSuffixMap latestDayNum=" + latestDayNum[0] + ", "
            + new Date(latestDayNum[0]*MS_PER_DAY));
    }

    @Override
    public String getDcpXmitSuffix(int dayNum, boolean doAllocate)
        throws DbIoException
    {
        String method = "getDcpXmitSuffix ";
        if (dayNumSuffixMap.isEmpty())
        {
            loadDayNumSuffixMap();
        }

        XmitDayMapEntry firstFree = null;
        XmitDayMapEntry oldestDay = null;
        for(XmitDayMapEntry xdme : dayNumSuffixMap)
        {
            if (xdme.dayNum == dayNum)
            {
                return xdme.suffix;
            }
            if (xdme.dayNum <= 0)
            {
                if (firstFree == null)
                {
                    firstFree = xdme;
                }
            }
            else if (oldestDay == null || xdme.dayNum < oldestDay.dayNum)
            {
                oldestDay = xdme;
            }
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
            String q = "UPDATE dcp_trans_day_map SET day_number = ?"
                       + " WHERE table_suffix = ?";
            Logger.instance().debug2("getDcpXmitSuffix: " + q);
            try
            {
                doModify(q,dayNum,firstFree.suffix);
            }
            catch (SQLException ex)
            {
                throw new DbIoException("Unable to update dcp_trans_day_map", ex);
            }
            return firstFree.suffix;
        }

        // There is no free slot.
        if (dayNum < oldestDay.dayNum)
        {
            warning(method + "Cannot allocate table "
                + "for old day number "    + dayNum + ", oldest day in storage is "
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

        String q = "UPDATE dcp_trans_day_map SET day_number = ?"
                 + " WHERE table_suffix = ?";
        try
        {
            doModify(q,dayNum,oldestDay.suffix);
        }
        catch (SQLException ex)
        {
            throw new DbIoException("Unable to update dcp_trans_day_map", ex);
        }

        return oldestDay.suffix;
    }

    public void deleteOldTableData()
        throws DbIoException
    {
        info("deleteOldTableData ...");

        int today = msecToDay(System.currentTimeMillis());
        for(XmitDayMapEntry xdme : dayNumSuffixMap)
        {
            if (xdme.dayNum != -1 && today - xdme.dayNum > numDaysStorage)
            {
                // MJM 20210213 fix concurrent modification exception.
                // No need to search the map for the suffix, I have it in my hand.
                String suffix = xdme.suffix;
                info("Clearing data for day number " + xdme.dayNum + ", suffix=" + suffix);
                clearTable(suffix);
            }
        }
    }

    /**
     * Clear the data and the transmit records for the passed suffix.
     * @param suffix
     * @throws DbIoException
     */
    private void clearTable(String suffix)
        throws DbIoException
    {
        final StringBuffer q = new StringBuffer(60);
        q.append("delete from DCP_TRANS_DATA_" + suffix);
        try(Connection c = getConnection();)
        {
            inTransaction(dao ->
            {
                dao.doModify(q.toString(), new Object[0]);

                q.setLength(0);
                q.append("DELETE FROM DCP_TRANS_" + suffix);
                dao.doModify(q.toString(), new Object[0]);

                q.setLength(0);
                q.append("UPDATE dcp_trans_day_map SET day_number = null"
                       + " WHERE table_suffix = ?");
                dao.doModify(q.toString(),suffix);

                q.setLength(0);
                q.append("Reset Sequence");
                // Reset sequence so record_id starts at 1. We don't want it wrapping.
                db.getKeyGenerator()
                  .reset("DCP_TRANS_"+suffix, dao.getConnection());
            });
        }
        catch(Exception ex)
        {
            String msg = String.format("Unable to clear table query =%s",q);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public Date getLatestTimeStamp(int dayNum)
        throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return null;
        }

        String tab = "DCP_TRANS_" + suffix;
        String q = "SELECT MAX(transmit_time) as maxDate FROM " + tab;
        try
        {
            return getSingleResult(q, rs -> getFullDate(rs,1));
        }
        catch (SQLException ex)
        {
            String msg = "getLatestTimeStamp Cannot parse xmit result: " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
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
        {
            return;
        }
        String tab = "DCP_TRANS_" + suffix;
        String base64data = new String(Base64.encodeBase64(xr.getData()));

        if (xr.getRecordId().isNull())
        {
            xr.setRecordId(getKey(tab));

            if ((++numXmitsSaved % 100) == 0)
            {
                debug2("Saving new msg with dcp addr=" + xr.getDcpAddress()
                    + " at time " + debugSdf.format(xmitTime)
                    + ", day=" + dayNum + " to " + tab);
            }

            try(Connection c = getConnection();
                PreparedStatement ps = getInsertStatement(suffix,c);)
            {
                fillWriteStatement(ps, xr, base64data);
                ps.executeUpdate();
            }
            catch(SQLException ex)
            {
                throw new DbIoException(module + ":saveDcpTranmission failed: "
                    + " xmitTime=" + debugSdf.format(xmitTime)
                    + ", tab='" + tab + "': "
                    + ex, ex);
            }
        }
        else
        {
            debug1("Updating msg at time " + debugSdf.format(xmitTime)
                + ", day=" + dayNum + " to " + tab);

            try(Connection c = getConnection();
                PreparedStatement ps = getUpdateStatement(suffix,c);)
            {
                fillWriteStatement(ps, xr, base64data);
                // 17th param is the record_id in the where clause
                ps.setLong(17, xr.getRecordId().getValue());
                ps.executeUpdate();
                String q = "delete from DCP_TRANS_DATA_" + suffix
                    + " where RECORD_ID = ?";
                doModify(q,xr.getRecordId());
            }
            catch(SQLException ex)
            {
                throw new DbIoException(module + ":saveDcpTranmission: " +
                        "updating XmitRecord " + ex,ex);
            }
        }

        // Very long message have extended blocks stored in the
        // DATA_TRANS_DATA_SUFFIX table. Write blocks in 4000 byte chunks.
        tab = "DCP_TRANS_DATA_" + suffix;
        String q = "insert into " + tab + " values(?,?,?)";
        for (int blockNum = 0; base64data.length() > 4000; blockNum++)
        {
            base64data = base64data.substring(4000);
            String toWrite = base64data.length() <= 4000 ? base64data :
                base64data.substring(0, 4000);
            try
            {
                doModify(q,xr.getRecordId(),blockNum,new String(toWrite));
            }
            catch (SQLException ex)
            {
                logger.warning("Unable to large message chunk " + xr.toString()
                             + " because " + ex.getLocalizedMessage());
            }

        }
    }

    /**
     * Helper method to return a prepared insert statement with the given
     * suffix.
     * @return the prepared statement.
     */
    private PreparedStatement getInsertStatement(String suffix, Connection c) throws SQLException
    {
        String tab = "DCP_TRANS_" + suffix;
        String q = "INSERT INTO " + tab;
        q = q +
            " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                    "?, ?, ?, ?, ?, ?)";
        return c.prepareStatement(q);
    }


    /**
     * The update statement must have the same params in the same order as the
     * insert statement so that fillWriteStatement() will work for both.
     * @return the prepared statement.
     */
    private PreparedStatement getUpdateStatement(String suffix, Connection c) throws SQLException
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
            {
                q.append(", ");
            }
            first = false;
            q.append(st.nextToken() + " = ?");
        }
        // The final token in the update statement is for the where clause.
        q.append(" where record_id = ? ");

        return c.prepareStatement(q.toString());
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
            {
                ps.setLong(10, xr.getCarrierStart().getTime());
            }
            else
            {
                ps.setNull(10, Types.BIGINT);
            }

            if (xr.getCarrierStop() != null)
            {
                ps.setLong(11, xr.getCarrierStop().getTime());
            }
            else
            {
                ps.setNull(11, Types.BIGINT);
            }

            ps.setInt(12, xr.getFlagbits());
            ps.setInt(13, xr.getGoesChannel());
            ps.setFloat(14, (float)xr.getBattVolt());
            ps.setInt(15, xr.getMessageLength());
            if (base64data.length() > 4000)
            {
                base64data = base64data.substring(0, 4000);
            }
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
        {
            return null;
        }
        String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
            + " where RECORD_ID = ?";// + recordId;
        try
        {
            return getSingleResult(q, rs ->
            {
                try
                {
                    DcpMsg ret = rs2XmitRecord(rs);
                    ret.setDayNumber(dayNum);
                    fillCompleteMsg(ret);
                    return ret;
                }
                catch (DbIoException ex)
                {
                    throw new SQLException("Unable to fill message",ex);
                }
            },
            recordId);
        }
        catch (SQLException ex)
        {
            String msg = "readDcpMsg: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public long getFirstRecordID(int dayNum)
        throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return -1;
        }
        String q = "select min(record_id) from DCP_TRANS_" + suffix;
        try
        {
            return getSingleResultOr(q,rs -> rs.getLong(1), -1L);
        }
        catch (SQLException ex)
        {
            String msg = "getFirstRecordId: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }
    }

    @Override
    public long getLastRecordID(int dayNum)
        throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return -1;
        }
        String q = "select max(record_id) from DCP_TRANS_" + suffix;
        try
        {
            return getSingleResultOr(q, rs -> rs.getLong(1), -1L);
        }
        catch (SQLException ex)
        {
            String msg = "getFirstRecordId: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
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
            {
                ret = doFindDcpTranmission(mediumType, mediumId, timestamp.getTime(), dayNum-1);
            }
            else if (msecOfDay > (MS_PER_DAY - TIME_FUDGE))
            {
                ret = doFindDcpTranmission(mediumType, mediumId, timestamp.getTime(), dayNum+1);
            }
        }
        return ret;
    }

    private DcpMsg doFindDcpTranmission(XmitMediumType mediumType, String mediumId,
        long msecTime, int dayNum)
        throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return null;
        }

        //Msg time stamps may vary. Search for msg within fudge time.
        long dBefore = msecTime - TIME_FUDGE;
        long dAfter  = msecTime + TIME_FUDGE;

        try(Connection c = getConnection();
            PreparedStatement ps = getSelectByIdAndTime(suffix,c);
        )
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
            {
                return null;
            }
        }
        catch (SQLException ex)
        {
            String msg = module + ":findDcpTranmission Cannot parse xmit " +
                    "result: " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }
    }

    /**
     * Helper method to return a prepared select statement with the given
     * suffix.
     * @return the prepared statement.
     */
    protected PreparedStatement getSelectByIdAndTime(String suffix, Connection c) throws SQLException
    {
        String q = null;

        String tab = "DCP_TRANS_" + suffix;
        q = "SELECT " + dcpTransFields + " FROM " + tab +
            " WHERE medium_type = ?" +
            " AND medium_id = ?" +
            " AND transmit_time >= ?" +
            " AND transmit_time <= ?";
        return c.prepareStatement(q);
    }

    /**
     * Creates a XmitRecord from the SQL record
     *
     * @param rs
     * @return XmitRecord
     * @throws DbIoException
     */
    private DcpMsg rs2XmitRecord(ResultSet rs)
        throws SQLException
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
        if (rs.wasNull())
        {
            windowStart = -1;
        }
        int windowLength = rs.getInt(8);
        if (rs.wasNull())
        {
            windowLength = -1;
        }
        int xmitInterval = rs.getInt(9);
        if (rs.wasNull())
        {
            xmitInterval = -1;
        }
        ms = rs.getLong(10);
        Date carrierStart = rs.wasNull() ? null : new Date(ms);
        ms = rs.getLong(11);
        Date carrierStop = rs.wasNull() ? null : new Date(ms);
        int flags = rs.getInt(12);
        //int channel = rs.getInt(13);
        //if (rs.wasNull()) channel = -1;
        double battery = rs.getDouble(14);
        if (rs.wasNull())
        {
            battery = 0.0;
        }
        int msgLength = rs.getInt(15);
        String base64data = rs.getString(16);
        byte data[] = Base64.decodeBase64(base64data.getBytes());

        XmitWindow xmitWindow = null;
        if (windowStart != -1 && xmitInterval > 0)
        {
            // If a xmit window is present, parse it.
            int firstWindowStart = windowStart;
            while(firstWindowStart - xmitInterval >= 0)
            {
                firstWindowStart -= xmitInterval;
            }
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
        {
            xr.addXmitFailureCode(failCodes.charAt(i));
        }
        xr.setData(data);
        xr.setDcpAddress(new DcpAddress(mediumId));
        if (msgLength > data.length)
        {
            xr.setMsgLength(msgLength);
            logger.debug1("XmitRecordDAO.rs2XmitRecord read a partial message: data.len="
                        + data.length + ", msgLength=" + msgLength);
        }
        xr.setBattVolt(battery);
        xr.setXmitWindow(xmitWindow);
        // Don't need to save channel -- it is read from GOES Header.
        // Don't need to save mediumType -- it is encapsulated in flag bits.

        return xr;
    }

    @Override
    public void fillCompleteMsg(DcpMsg msg)
        throws DbIoException
    {
        // Note msg.msgLength the decoded length -- not the base64 length. So this is Okay.
        if (msg.getData().length >= msg.getMsgLength())
        {
            return;
        }
        String suffix = getDcpXmitSuffix(msg.getDayNumber(), false);
        if (suffix == null)
        {
            return;
        }
        String q = "select msg_data from dcp_trans_data_" + suffix
                 + " where record_id = ?"
                  + " order by block_num";

        final byte completeMsgData[] = new byte[msg.getMessageLength()];
        final byte firstBlock[] = msg.getData();
        final int[] cmdi = new int[1];
        cmdi[0] = 0;
        for(; cmdi[0]<firstBlock.length && cmdi[0] < completeMsgData.length; cmdi[0]++)
        {
            completeMsgData[cmdi[0]] = firstBlock[cmdi[0]];
        }
        try
        {
            doQuery(q,rs ->
            {
                String base64data = rs.getString(1);
                byte data[] = Base64.decodeBase64(base64data.getBytes());
                for(int idx = 0; idx<data.length && cmdi[0] < completeMsgData.length; idx++, cmdi[0]++)
                {
                    completeMsgData[cmdi[0]] = data[idx];
                }
            },
            msg.getRecordId());
            msg.setData(completeMsgData);
        }
        catch (SQLException ex)
        {
            warning("Error in fillCompleteMsg(" + msg.getDcpAddress() + ") "
                + "dataLen=" + cmdi[0] + ", totlen=" + completeMsgData.length + ": " + ex);
        }
    }

    @Override
    public int readXmitsByGroup(Collection<DcpMsg> results, int dayNum,
        NetworkList grp) throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return 0;
        }
        String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix + " a, "
            + "NetworkListEntry b "
            + "where a.medium_type = ?"
            + " and b.networklistid = ?"
            + " and upper(a.medium_id) = upper(b.transportid)"
            + " order by transmit_time";

        int[] n = new int[1];
        n[0] = 0;
        try
        {
            results.addAll(
                getResults(q, rs ->
                {
                    DcpMsg ret = rs2XmitRecord(rs);
                    ret.setDayNumber(dayNum);
                    n[0]++;
                    return ret;

                },
                XmitMediumType.transportMediumType2type(grp.transportMediumType)
                              .getCode(),
                grp.getId())
            );
        }
        catch (SQLException ex)
        {
            String msg = "readXmitsByGroup: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }

        return n[0];
    }

    @Override
    public int readXmitsByChannel(Collection<DcpMsg> results, int dayNum,
        int chan) throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return 0;
        }
        String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
            + " where channel = ?" 
            + " order by transmit_time";

        final int[] n = new int[1];
        n[0] = 0;
        try
        {
            results.addAll(
                getResults(q, rs ->
                {
                    DcpMsg ret = rs2XmitRecord(rs);
                    ret.setDayNumber(dayNum);
                    n[0]++;
                    return ret;
                },
                chan)
            );
        }
        catch (SQLException ex)
        {
            String msg = "readXmitsByChannel: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }

        return n[0];
    }

    @Override
    public int readXmitsByMediumId(Collection<DcpMsg> results, int dayNum,
        XmitMediumType mediumType, String mediumId)
        throws DbIoException
    {
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            return 0;
        }
        String q = "select " + dcpTransFields + " from DCP_TRANS_" + suffix
            + " where medium_type = ?"
            + " and medium_id = ?"
            + " order by transmit_time";
        final int[] n = new int[1];
        n[0] = 0;
        try
        {
            results.addAll(
                getResults(q, rs ->
                {
                    DcpMsg ret = rs2XmitRecord(rs);
                    ret.setDayNumber(dayNum);
                    n[0]++;
                    return ret;
                },
                mediumType.getCode(),
                mediumId));
        }
        catch (SQLException ex)
        {
            String msg = "readXmitsByGroup: Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }

        return n[0];
    }

    @Override
    public Date getLastLocalRecvTime() throws DbIoException
    {
        loadDayNumSuffixMap();
        XmitDayMapEntry latestDay = null;
        for(XmitDayMapEntry xdme : dayNumSuffixMap)
        {
            if (xdme.dayNum != -1 && (latestDay == null || xdme.dayNum > latestDay.dayNum))
            {
                latestDay = xdme;
            }
            logger.debug2("XmitRecordDAO.getLastLocalRecvTime: latestDay="
                        + (latestDay == null ? "null" : "" + latestDay.dayNum));
        }
        if (latestDay == null)
        {
            return null;
        }

        String q = "select max(local_recv_time) from " + "DCP_TRANS_" + latestDay.suffix;
        logger.debug2("XmitRecordDAO.getLastLocalRecvTime: " + q);
        try
        {
            return getSingleResultOr(q, rs -> new Date(rs.getLong(1)),null);
        }
        catch(SQLException ex)
        {
            warning("Error in query '" + q + "': " + ex);
        }
        return null;
    }

    @Override
    public ArrayList<XmitRecSpec> readSince(int dayNum, long lastRecId)
        throws DbIoException
    {
        ArrayList<XmitRecSpec> ret = new ArrayList<XmitRecSpec>();
        String suffix = getDcpXmitSuffix(dayNum, false);
        if (suffix == null)
        {
            warning("No suffix for dayNum=" + dayNum);
            return ret;
        }

        ArrayList<Object> parameters = new ArrayList<>();
        String tab = "DCP_TRANS_" + suffix;
        String q = "SELECT RECORD_ID, MEDIUM_TYPE, MEDIUM_ID, TRANSMIT_TIME, FAILURE_CODES, CHANNEL"
            + " FROM " + tab;
        if (lastRecId != -1)
        {
            q = q + " WHERE RECORD_ID > ?";
            parameters.add(lastRecId);
        }
        q = q + " ORDER BY RECORD_ID";

        final int[] n = new int[1];
        n[0] = 0;
        try
        {

            return new ArrayList<>(
                getResults(q,rs ->
                {
                    XmitRecSpec xrs = new XmitRecSpec(rs.getLong(1));
                    String s = rs.getString(2);
                    if (s == null || s.length() == 0)
                        xrs.setMediumType('G');
                    else
                        xrs.setMediumType(s.charAt(0));
                    xrs.setMediumId(rs.getString(3));
                    xrs.setXmitTime(new Date(rs.getLong(4)));
                    xrs.setFailureCodes(rs.getString(5));
                    xrs.setGoesChannel(rs.getInt(6));
                    if (++n[0] % 1000 == 0)
                    {
                        debug2("" + n + " records so far");
                    }
                    return xrs;
                },
                parameters.toArray(new Object[0])
            ));

        }
        catch (SQLException ex)
        {
            String msg = "getLatestTimeStamp Cannot parse xmit result: " + ex;
            warning(msg);
            throw new DbIoException(msg, ex);
        }
        finally
        {
            debug2("" + n[0] + " records received.");
        }
    }


    /**
    * In xmit record tables, date/times are represented as long integer.
     * @param rs
     * @param column
     * @return
     */
    private Date getFullDate(ResultSet rs, int column)
    {
        // In OpenTSDB, date/times are stored as long integer
        try
        {
            long t = rs.getLong(column);
            if (rs.wasNull())
            {
                return null;
            }
            return new Date(t);
        }
        catch (SQLException ex)
        {
            warning("Cannot convert date!");
            return null;
        }
    }

    @Override
    public void setNumDaysStorage(int numDaysStorage)
    {
        XmitRecordDAO.numDaysStorage = numDaysStorage;
        try
        {
            if (dayNumSuffixMapLoadedMsec == 0L)
            {
                this.loadDayNumSuffixMap();
            }
            deleteOldTableData();
        }
        catch(DbIoException ex)
        {
            warning("Error deleting old data: " + ex);
            System.err.println("Error deleting old data: " + ex);
            ex.printStackTrace();
        }
    }
}
