package opendcs.dai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import decodes.db.NetworkList;
import decodes.dcpmon.XmitMediumType;
import decodes.dcpmon.XmitRecSpec;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

public interface XmitRecordDAI
{
	/**
	 * This is a new Method for the new DCP Monitor. This method will
	 * save all transmission records. The other_dcp_trans table will no
	 * longer be used.
	 * Requires the DCP Monitor Extensions to the Time Series Database.
	 * @param xr the transmission data
	 */
	public void saveDcpTranmission(DcpMsg xr)
		throws DbIoException;

	/**
	 * Find the latest timestamp for the given day.
	 * @param dayNum
	 * @return Date
	 * @throws DbIoException
	 */
	public Date getLatestTimeStamp(int dayNum) throws DbIoException;
	
	/**
	 * This method tries to find a Xmit record based on the
	 * given dcp address and time. This method will be used when
	 * creating Xmit records. When a new dcp message arrives will check
	 * the dcp monitor queue to see if the msg is in there, if it is not
	 * in the queue will check the database.
	 *  
	 * @param mediumType with the mediumId identifies a platform
	 * @param mediumId with the mediumType identifies a platform
	 * @param timestamp
	 * @return XmitRecord
	 */
	public DcpMsg findDcpTranmission(XmitMediumType mediumType, String mediumId, Date timestamp)
		throws DbIoException;
	
	/**
	 * Used by a test program to dump an entire day's data.
	 * @param dayNum the day number
	 * @param recordId the record id
	 * @return the DCP message or null if none matching recordId
	 */
	public DcpMsg readDcpMsg(int dayNum, long recordId)
		throws DbIoException;

	/**
	 * This is used from the DcpMonitorServerThread - sendMessageStatus()
	 * 
	 * @param results
	 * @param dayNum
	 * @param grp
	 * @return num read
	 * @throws DbIoException
	 */
	public int readXmitsByGroup(Collection<DcpMsg> results, int dayNum, NetworkList grp)
		throws DbIoException;
	
	/**
	 * Reads transmissions by channel number for the specified
	 * day and channel into the passed results array.
	 * Used from DcpMonitorServerThread - sendChannelExpand() method
	 * 
	 * Requires the DCP Monitor Extensions to the Time Series Database.
	 * @param results the collection to store the results
	 * @param dayNum the day number
	 * @param chan the channel number
	 * @return the number of records returned.
	 */
	public int readXmitsByChannel(Collection<DcpMsg> results, int dayNum, int chan)
		throws DbIoException;
	
	/**
	 * Reads transmissions by dcp address for the specified
	 * day and dcp address into the passed results array.
	 * Used from DcpMonitorServerThread - sendWazzupDcp() method
	 * 
	 * Requires the DCP Monitor Extensions to the Time Series Database.
	 * @param results the collection to store the results
	 * @param dayNum the day number
	 * @param addr the DCP address or -1 to get all.
	 * @return the number of records returned.
	 */
	public int readXmitsByMediumId(Collection<DcpMsg> results, int dayNum, 
		XmitMediumType mediumType, String mediumId)
		throws DbIoException;
	
	/**
	 * Given a day number, return the suffix of the table for that day.
	 * @param dayNum Day number. Day 0 = Jan 1, 1970
	 * @param doAllocate true to allocate a table for this day if one currently does not exist
	 * @return the string table suffix
	 * @throws DbIoException
	 */
	public String getDcpXmitSuffix(int dayNum, boolean doAllocate)
		throws DbIoException;

	/**
	 * Free any resources allocated.
	 */
	public void close();
	
	/**
	 * @return the local receive time of the last message stored in the archive, or null
	 * if archive is empty.
	 * @throws DbIoException
	 */
	public Date getLastLocalRecvTime()
		throws DbIoException;
	
	/**
	 * Messages longer than 4000 bytes (after base64 expansion) will be chopped.
	 * The first block will be stored in the DCP_TRANS_SUFFIX table and multiple
	 * additional blocks (as many as are needed) will be stored in the 
	 * DCP_TRANS_DATA_SUFFIX table. The DCP Monitor will read records en-mas from
	 * DCP_TRANS_SUFFIX. Only when needed (at the lowest level of detail) will the
	 * fillCompleteMsg method be called.
	 * You can tell a message is incomplete if msgLength() > data.length
	 * @param msg
	 */
	public void fillCompleteMsg(DcpMsg msg)
		throws DbIoException;
	
	public long getFirstRecordID(int dayNum)
		throws DbIoException;

	public long getLastRecordID(int dayNum)
		throws DbIoException;
	
	/**
	 * Read XmitRecSpecs from the specified day number that have been created
	 * since the lastRecId. That is, they have a recordId > lastRecId.
	 * @param dayNum
	 * @param lastRecId
	 * @return
	 * @throws DbIoException
	 */
	public ArrayList<XmitRecSpec> readSince(int dayNum, long lastRecId)
		throws DbIoException;
	
	public void setNumDaysStorage(int numDaysStorage);

}
