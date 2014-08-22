package opendcs.dai;

import java.util.Collection;
import java.util.Date;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;

import decodes.dcpmon.DcpGroup;
import decodes.dcpmon.XmitMediumType;
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
	 * @param dcpAddress
	 * @param timestamp
	 * @return XmitRecord
	 */
	public DcpMsg findDcpTranmission(DcpAddress dcpAddress, Date timestamp)
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
	public int readXmitsByGroup(Collection<DcpMsg> results, int dayNum, DcpGroup grp)
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
	public int readXmitsByDcpAddress(Collection<DcpMsg> results, int dayNum, 
		XmitMediumType mediumType, DcpAddress dcpAddress)
		throws DbIoException;
	
	/**
	 * Reads an XmitRecord with the raw msg from the database for the 
	 * given day, dcp addr and timestamp.
	 * This method is used when the user request to see a message.
	 * 
	 * @param dayNum The day number used as a table suffix.
	 * @param addr The DCP Address
	 * @param timestamp The time stamp
	 * @return XmitRecord containing raw message text including DOMSAT header
	 * @throws DbIoException on any database error
	 */
	public DcpMsg readXmitRawMsg(int dayNum, DcpAddress dcpAddress, Date timestamp)
		throws DbIoException;
	
	/**
	 * Delete all transmissions before the specified day number.
	 * Requires the DCP Monitor Extensions to the Time Series Database.
	 * @param dayNum the day number (Jan 1, 1970 was day 0).
	 */
	public void deleteDcpXmitsBefore(int dayNum)
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
}
