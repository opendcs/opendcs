/*
*  $Id$
*/
package lrgs.common;

import ilex.util.IDateFormat;
import ilex.util.TextUtil;
import java.util.Date;

/**
Holds all of the info present in a DCP message index entry in the LRGS
message archive.
*/
public class DcpMsgIndex implements Comparable
{
	/** Unix time_t of message receipt. */
	private Date localRecvTime;

	/** Unix time_t of message send time. */
	private Date xmitTime;

	/** Offset of this message in the file. */
	private long offset;

	/** Opaque DCP address */
	private DcpAddress dcpAddress;

	/** Primary failure code for this message (G, ?, or M) */
	private char failureCode;

	/** Sequence number */
	private int sequenceNum;

	/** GOES Channel Number */
	private int channel;

	/** bit-flags */
	private int flagbits;

	/** start time of index file containing last msg for this DCP */
	private int prevFileThisDcp;

	/** index num for last msg for this DCP */
	private int prevIdxNumThisDcp;

	/** Cached copy of message data. */
	private DcpMsg dcpMsg;

	/** Results from merge filter for this msg. */
	private byte mergeFilterCode;

	/** Database ID of the data source for this message */
	private int dataSourceId;

	/** Not persistent -- only valid in cache. */
	private int msgLength;
	
	public static final Date zeroDate = new Date(0L);

	/** Default constructor. */
	public DcpMsgIndex()
	{
		clear();
	}

	public void clear()
	{
		setLocalRecvTime(zeroDate);
		setXmitTime(zeroDate);
		setOffset(0L);
		setFailureCode('-');
		setSequenceNum(0);
		setChannel(0);
		setFlagbits(0);
		setDcpMsg(null);
		setPrevFileThisDcp(0);
		setPrevIdxNumThisDcp(-1);
		setMergeFilterCode((byte)0);
		setMsgLength(0);
		setDataSourceId(-1);
		setDcpAddress(new DcpAddress());
	}

	/** 
	 * Constructs a new index entry from a DCP message 
	 * @param msg the DcpMsg structure.
	 * @param prevFile start time of previous file containing msg from this DCP
	 * @param prevNum index number in the previous file
	 * @param loc byte-location in the message file
	 */
	public DcpMsgIndex(DcpMsg msg, int prevFile, int prevNum, long loc)
	{
		setOffset(loc);
		setMessageParams(msg);
		setPrevFileThisDcp(prevFile);
		setPrevIdxNumThisDcp(prevNum);
	}

	public void setMessageParams(DcpMsg msg)
	{
		setLocalRecvTime(msg.getLocalReceiveTime());
		setXmitTime(msg.getDapsTime());
		setDcpAddress(msg.getDcpAddress());
		setFailureCode(msg.getFailureCode());
		setSequenceNum(msg.getSequenceNum());
		setChannel(msg.getGoesChannel());
		setMsgLength(msg.getDcpDataLength());
		setFlagbits(msg.flagbits);
		setMergeFilterCode(msg.mergeFilterCode);
		setDataSourceId(msg.getDataSourceId());
		setDcpMsg(msg);
	}

	/** @return the LRGS (DROT) time stamp. */
	public Date getLocalRecvTime()
	{
		return localRecvTime;
	}

	/** @return the DAPS time stamp. */
	public Date getXmitTime()
	{
		return xmitTime;
	}

	/** @return the DCP Address. */
	public DcpAddress getDcpAddress()
	{
		return dcpAddress;
	}

	/**
	  @return a multi-line string containing all the index info.
	*/
	public String toString()
	{
		StringBuffer ret = new StringBuffer();

		ret.append("DROT Time: " + IDateFormat.toString(localRecvTime, false)
			+ '\n');
		
		ret.append("DAPS Time: " + IDateFormat.toString(xmitTime, false) + '\n');

		ret.append("DCP Address: " + dcpAddress + "        Channel: " + getChannel() + '\n');

		ret.append("File Offset: " + getOffset() + "           Sequence #: " +
			getSequenceNum() + '\n');

		ret.append("Failure Code: " + getFailureCode() + "         Flag: 0x"
			+ Integer.toHexString(getFlagbits()) + '\n');

		return new String(ret);
	}

	/** @return true if passed object is equal to this one. */
    public boolean equals(Object obj)
	{
		if (obj.getClass() != getClass())
			return false;
		return compareTo(obj) == 0;
	}

    /**
       Compare two DCP Messge Indexes. The fields of the index are
	   compared in the following order:
         DrotTime, Addr, SequenceNum, DapsTime, FailureCode, Channel,
	     Flag, Offset

	   @return 0 if objects are equal, <0 if 'this' is less than the 
		passed object. Return >0 if 'this is greater than the passed object.
     */
	public int compareTo(Object o)
	{
		if (this == o)
			return 0;
		if (o == null)
			return 1;     // A null object is always greater than a non-null.

		// The following will throw an exception if o is not the right type:
		DcpMsgIndex rhs = (DcpMsgIndex)o;

		int i = getLocalRecvTime().compareTo(rhs.getLocalRecvTime());
		if (i != 0) return i > 0 ? 1 : -1;

		i = dcpAddress.compareTo(rhs.dcpAddress);
		if (i != 0) return i > 0 ? 1 : -1;

		i = getSequenceNum() - rhs.getSequenceNum();
		if (i != 0) return i > 0 ? 1 : -1;

		i = getXmitTime().compareTo(rhs.getXmitTime());
		if (i != 0) return i > 0 ? 1 : -1;

		i = (int)getFailureCode() - (int)rhs.getFailureCode();
		if (i != 0) return i > 0 ? 1 : -1;

		i = getChannel() - rhs.getChannel();
		if (i != 0) return i > 0 ? 1 : -1;

		i = getFlagbits() - rhs.getFlagbits();
		if (i != 0) return i > 0 ? 1 : -1;

		long li = getOffset() - rhs.getOffset();
		if (li != 0L) return li > 0 ? 1 : -1;

		return 0; // All fields are equal!
	}

	/**
	 * Makes this index a copy of the passed index.
	 */
	public void copyFrom(DcpMsgIndex rhs)
	{
		if (rhs.getLocalRecvTime() != null)
			setLocalRecvTime(rhs.getLocalRecvTime());
		setXmitTime(rhs.getXmitTime());
		setOffset(rhs.getOffset());
		dcpAddress = rhs.dcpAddress;
		setFailureCode(rhs.getFailureCode());
		setSequenceNum(rhs.getSequenceNum());
		setChannel(rhs.getChannel());
		setFlagbits(rhs.getFlagbits());
		setPrevFileThisDcp(rhs.getPrevFileThisDcp());
		setPrevIdxNumThisDcp(rhs.getPrevIdxNumThisDcp());
		setDcpMsg(rhs.getDcpMsg());
		setMergeFilterCode(rhs.getMergeFilterCode());
		setMsgLength(rhs.getMsgLength());
		setDataSourceId(rhs.getDataSourceId());
	}

	public DcpMsgIndex copy()
	{
		DcpMsgIndex ret = new DcpMsgIndex();
		ret.copyFrom(this);
		return ret;
	}

	/**
     * @param xmitTime the xmitTime to set
     */
    public void setXmitTime(Date xmitTime)
    {
	    this.xmitTime = xmitTime;
    }

	/**
     * @param localRecvTime the localRecvTime to set
     */
    public void setLocalRecvTime(Date localRecvTime)
    {
	    this.localRecvTime = localRecvTime;
    }

	/**
     * @param offset the offset to set
     */
    public void setOffset(long offset)
    {
	    this.offset = offset;
    }

	/**
     * @return the offset
     */
    public long getOffset()
    {
	    return offset;
    }

	/**
     * @param failureCode the failureCode to set
     */
    public void setFailureCode(char failureCode)
    {
	    this.failureCode = failureCode;
    }

	/**
     * @return the failureCode
     */
    public char getFailureCode()
    {
	    return failureCode;
    }

	/**
     * @param sequenceNum the sequenceNum to set
     */
    public void setSequenceNum(int sequenceNum)
    {
	    this.sequenceNum = sequenceNum;
    }

	/**
     * @return the sequenceNum
     */
    public int getSequenceNum()
    {
	    return sequenceNum;
    }

	/**
     * @param channel the channel to set
     */
    public void setChannel(int channel)
    {
	    this.channel = channel;
    }

	/**
     * @return the channel
     */
    public int getChannel()
    {
	    return channel;
    }

	/**
     * @param flagbits the flagbits to set
     */
    public void setFlagbits(int flagbits)
    {
	    this.flagbits = flagbits;
    }

	/**
     * @return the flagbits
     */
    public int getFlagbits()
    {
	    return flagbits;
    }

	/**
     * @param prevFileThisDcp the prevFileThisDcp to set
     */
    public void setPrevFileThisDcp(int prevFileThisDcp)
    {
	    this.prevFileThisDcp = prevFileThisDcp;
    }

	/**
     * @return the prevFileThisDcp
     */
    public int getPrevFileThisDcp()
    {
	    return prevFileThisDcp;
    }

	/**
     * @param prevIdxNumThisDcp the prevIdxNumThisDcp to set
     */
    public void setPrevIdxNumThisDcp(int prevIdxNumThisDcp)
    {
	    this.prevIdxNumThisDcp = prevIdxNumThisDcp;
    }

	/**
     * @return the prevIdxNumThisDcp
     */
    public int getPrevIdxNumThisDcp()
    {
	    return prevIdxNumThisDcp;
    }

	/**
     * @param dcpMsg the dcpMsg to set
     */
    public void setDcpMsg(DcpMsg dcpMsg)
    {
	    this.dcpMsg = dcpMsg;
    }

	/**
     * @return the dcpMsg
     */
    public DcpMsg getDcpMsg()
    {
	    return dcpMsg;
    }

	/**
     * @param mergeFilterCode the mergeFilterCode to set
     */
    public void setMergeFilterCode(byte mergeFilterCode)
    {
	    this.mergeFilterCode = mergeFilterCode;
    }

	/**
     * @return the mergeFilterCode
     */
    public byte getMergeFilterCode()
    {
	    return mergeFilterCode;
    }

	/**
     * @param dataSourceId the dataSourceId to set
     */
    public void setDataSourceId(int dataSourceId)
    {
	    this.dataSourceId = dataSourceId;
    }

	/**
     * @return the dataSourceId
     */
    public int getDataSourceId()
    {
	    return dataSourceId;
    }

	/**
     * @param msgLength the msgLength to set
     */
    public void setMsgLength(int msgLength)
    {
	    this.msgLength = msgLength;
    }

	/**
     * @return the msgLength
     */
    public int getMsgLength()
    {
	    return msgLength;
    }

	/**
     * @param dcpAddress the dcpAddress to set
     */
    public void setDcpAddress(DcpAddress dcpAddress)
    {
	    this.dcpAddress = dcpAddress;
    }
}

