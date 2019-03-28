/*
*  $Id$
*/
package lrgs.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import lrgs.archive.XmitWindow;
import decodes.consumer.HtmlFormatter;
import decodes.db.Constants;
import decodes.sql.DbKey;
import ilex.util.ArrayUtil;
import ilex.util.ByteUtil;
import ilex.util.IDateFormat;
import ilex.util.TwoDigitYear;
import ilex.util.Logger;

/**
Data Structure that holds a single DCP Message
*/
public class DcpMsg
{
	/**
	 * Primary database key for this record. This will be undefinedId for messages
	 * not stored in a database.
	 */
	private DbKey recordId = Constants.undefinedId;

	/**
	  The DCP message data, including header, is stored as an
	  array of bytes.
	*/
	private byte[] data = null;
	
	/** The flag bits */
	public int flagbits = 0;

	/** The DROT (LRGS) time stamp */
	private Date localRecvTime = DcpMsgIndex.zeroDate;

	/** (legacy) file name constructed from DCP address & sequence num */
	private String seqFileName = null;

	/** DOMSAT (or other) sequence number for this message (-1 if unknown) */
	private int sequenceNum = -1;

	public byte mergeFilterCode = 0;

	/** Baud rate if known, 0 = unknown */
	private int baud = 0;

	/** Time stamp (msec) of carrier start (0 = unknown) */
	private Date carrierStart;

	/** Time stamp (msec) of carrier stop (0 = unknown) */
	private Date carrierStop;

	/** Time stamp (msec) of DOMSAT Receipt time (0 = unknown) */
	private Date domsatTime;

	/** The database ID of the data source from whence this message came */
	private int dataSourceId;
	
	/** GOES time stamp from header, or SBD session time. */
	private Date xmitTime;
	
	/** Iridium SBD Mobile Terminated Msg Sequence Number */
	private int mtmsm;

	/** Iridium SBD CDR Reference Number */
	private long cdrReference;
	
	/** For GOES: DCP Address, For Iridium SBD, International Mobile Equip ID */
	private DcpAddress dcpAddress = null;
	
	/** For Iridium SBD, store the session status. */
	private int sessionStatus = 0;
	
	/** reserved for future use. */
	public byte reserved[];

	/** 
	 * Max length that can be stored. The upper limit is based on:
	 * - 5 digit length field in DDS imposes limit to 99800
	 */
	public static final int MAX_DATA_LENGTH = 99800;
	
	/** transient storage for DRGS interface. Original address is NOT saved. */
	private DcpAddress origAddress = null;
	
	/** transient storage for battery voltage, used by DCP Monitor. */
	private double battVolt = 0.0;
	
	/** Failure code for non-GOES messages */
	private char failureCode = (char)0;
	
	/** Set after header is parsed */
	private int headerLength = 0;
	
	/** Max number of failure codes any messge can have */
	public static final int MAX_FAILURE_CODES = 8;

	/** Additional failure/status codes (used by DCP Monitor) */
	private char xmitFailureCodes[] = new char[MAX_FAILURE_CODES];

	private XmitWindow xmitWindow = null;
	
	private int msgLength = 0;
	
	/** When read from DCP Mon database, this indicates the table it was read from */
	private int dayNumber = 0;
	
	// New fields added for OpenDCS 6.6 LRGS HRIT and DAMS-NT
	private double goesSignalStrength = 0.;
	private double goesFreqOffset = 0.;
	private double goesGoodPhasePct = 0.;
	private double goesPhaseNoise = 0.;
	
	private static SimpleDateFormat timeSdfSec = new SimpleDateFormat("HH:mm:ss");
	private static SimpleDateFormat timeSdfMS = new SimpleDateFormat("HH:mm:ss.SSS");
	static NumberFormat bvFormat = NumberFormat.getNumberInstance();
	static
	{
		timeSdfSec.setTimeZone(TimeZone.getTimeZone("UTC")); 
		timeSdfMS.setTimeZone(TimeZone.getTimeZone("UTC")); 
		bvFormat.setGroupingUsed(false);
		bvFormat.setMaximumFractionDigits(1);
	}

	
	// Constructors ===============================================

	/** Allocate an empty DCP message */
	public DcpMsg()
	{
		setCarrierStart(null);
		setCarrierStop(null);
		setDomsatTime(null);
		setDataSourceId(-1);
		reserved = new byte[32];
		setXmitTime(null);
		setMtmsm(0);
		setCdrReference(0L);
		for(int i=0; i<MAX_FAILURE_CODES; i++)
			xmitFailureCodes[i] = '\0';
	}

	/** 
	  Use the passed byte array to allocate a new DCP Message 
	  @param data the data bytes
	  @param offset in data where this message starts
	  @param size number of data bytes in this message
	*/
	public DcpMsg(byte data[], int size, int offset)
	{
		this();
		set(data, size, offset);
	}
	
	/** 
	  The above constructor assumes message type is GOES (a 0 in the
	  type-bits of the flag). This causes problems in the set() method
	  because it tries to parse the DCP address.
	  This constructor should be used by non-GOES data sources.
	  @param flagbits the flag bits indicating message type other than GOES.
	  @param data the data bytes
	  @param offset in data where this message starts
	  @param size number of data bytes in this message
	*/
	public DcpMsg(int flagbits, byte data[], int size, int offset)
	{
		this();
		this.setFlagbits(flagbits);
		set(data, size, offset);
	}

	/**
	  @return the entire length of the data, including header.
	*/
	public int getMsgLength() 
	{
		return msgLength;
		// In new DCP Mon, a very long message may be only partially read
		// from the database. Therefore we track length separately from
		// the length of the data bytes stored here.
		//OLD:		return data != null ? data.length : 0;
	}

	/**
	 * Sets the local receive time.
	 * @param t the time value as a Unix time_t.
	 */
	public void setLocalReceiveTime(Date t)
	{
		localRecvTime = t;
	}

	/** @return the local receive time as a unix time_t. */
	public Date getLocalReceiveTime() { return localRecvTime; }

	/** 
	 * @return the sequence number associated with this message, or -1 if none.
	 */
	public int getSequenceNum()
	{
		return sequenceNum;
	}

	/**
	 * Sets the seqnuence number to be associated with this msg.
	 * @param sn the sequence num.
	 */
	public void setSequenceNum(int sn)
	{
		sequenceNum = sn;
	}

	/**
	  Allocate a new data field and populate it with a copy of the
	  passed byte array.
	  @param data the data bytes
	  @param offset in data where this message starts
	  @param size number of data bytes in this message
	*/
	private void set(byte data[], int size, int offset)
	{
//MJM 20160831 Not sure what this code was for. Should never be called with < 0
//		if (isGoesMessage() 
//		 && (size > MAX_DATA_LENGTH || size < 0))
//		{
//			Logger.instance().warning("Cannot set DcpMsg data, invalid size="
//				+ size + ", attempting to parse length from header.");
//
//			byte ml[] = ArrayUtil.getField(data, offset + 32, 5);
//			if (ml == null)
//			{
//				Logger.instance().warning("Parse failed setting empty msg.");
//				size = 0;
//			}
//			else
//			{
//				try 
//				{
//					size = 37 + Integer.parseInt(new String(ml));
//					Logger.instance().warning("Parsed msg length = " + size);
//				}
//				catch(NumberFormatException ex)
//				{
//					Logger.instance().warning("Parse failed setting empty msg.");
//					size = 0;
//				}
//			}
//		}
//MJM 20160831 replaced with the following, simply truncate to max data len if too big.
//Do this regardless of type.
		if (size > MAX_DATA_LENGTH)
		{
			Logger.instance().warning("DcpMsg too big (" + size + "). Truncated to max len="  
				+ MAX_DATA_LENGTH); 
			ArrayUtil.getField(data, 0, size = MAX_DATA_LENGTH);
		}
		byte[] buf  = new byte[size];
		for(int i = 0; i<size; i++)
			buf[i] = data[offset+i];
		setData(buf);
	}

	/**
	 * Sets the internally stored message bytes. Assume that a complete
	 * message is being set and also set this.msgLength accordingly.
	 * @param buf the message bytes.
	 */
	public void setData(byte[] buf)
	{
		this.data = buf;
		msgLength = buf.length;
		if (DcpMsgFlag.isGOES(flagbits))
		{
			setXmitTime(getDapsTime());
			setDcpAddress(this.getGoesDcpAddress());
			char c = this.getFailureCode();
			if (c != '-')
				addXmitFailureCode(c);
		}
	}

	/**
	  Return a subset of the data as a new byte array. Returns
	  null if either of the indices are outside the array bounds.
	  @return a subset of the data as a new byte array, or
	  null if either of the indices are outside the array bounds.
	*/
	public byte[] getField(int start, int length) 
	{
		byte ret[] = ArrayUtil.getField(data, start, length);
		if (ret == null)
		{
			Logger.instance().warning("Invalid msg length=" + data.length
				+ ", field=" + start + "..." + (start+length));
		}
		return ret;
	}

	/**
	  @return address of DCP that sent this message.
	*/
	public DcpAddress getGoesDcpAddress() 
	{
		byte addrfield[] = getField(IDX_DCP_ADDR, 8);
		if (addrfield == null)
			throw new NumberFormatException("No data");
		return new DcpAddress(new String(addrfield));
	}

	/**
	  @return date/time that message was received by DAPS.
	*/
	public Date getDapsTime()
	{
		if (!DcpMsgFlag.isGOES(this.flagbits))
			return this.xmitTime;

		try
		{
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.clear();

			byte field[] = getField(IDX_YEAR, 2);
			if (field == null)
				return new Date(0);

			cal.set(Calendar.YEAR, TwoDigitYear.getYear(new String(field)));

			field = getField(IDX_DAY, 3);
			if (field == null)
				return new Date(0);
			int i = Integer.parseInt(new String(field));
			cal.set(Calendar.DAY_OF_YEAR, i);

			field = getField(IDX_HOUR, 2);
			if (field == null)
				return new Date(0);
			i = Integer.parseInt(new String(field));
			cal.set(Calendar.HOUR_OF_DAY, i);

			field = getField(IDX_MIN, 2);
			if (field == null)
				return new Date(0);
			i = Integer.parseInt(new String(field));
			cal.set(Calendar.MINUTE, i);

			field = getField(IDX_SEC, 2);
			if (field == null)
				return new Date(0);
			i = Integer.parseInt(new String(field));
			cal.set(Calendar.SECOND, i);

			return cal.getTime();
		}
		catch(Exception ex)
		{
			// Note NumberFormatException can happen if garbage in the
			// date fields.
			// If this happens, just return the current time.
			return new Date();
		}
	}

	/**
	  @return failure code contained in this message.
	*/
	public char getFailureCode()
	{
		if (!isGoesMessage())
			return failureCode;
				
		byte field[] = getField(IDX_FAILCODE, 1);
		if (field == null)
			return '-';
		return (char)field[0];
	}

	/**
	  @return true if this is a DAPS-generated status message.
	*/
	public boolean isDapsStatusMsg()
	{
		char c = getFailureCode();
		return isGoesMessage() && !(c == 'G' || c == '?');
	}
	
	/** @return true if this is a GOES message from any source */
	public boolean isGoesMessage()
	{
		return DcpMsgFlag.isGOES(flagbits);
	}

	/**
	  @return signal strength as an integer.
	*/
	public int getSignalStrength()
	{
		byte field[] = getField(IDX_SIGSTRENGTH, 2);
		if (field == null)
			return 0;
		try { return Integer.parseInt(new String(field)); }
		catch(NumberFormatException ex)
		{
			return 0;
		}
	}

	/**
	  @return the frequence offset in increments of 50 Hz.
	*/
	public int getFrequencyOffset()
	{
		byte field[] = getField(IDX_FREQOFFSET, 2);
		if (field == null)
			return 0;
		char c = (char)field[1];
		int i = ByteUtil.fromHexChar(c);
		return (char)field[0] == '-' ? -i : i;
	}

	/**
	  @return 'N' for Normal, 'L' for Low, 'H' for High, or 'U' for unknown.
	*/
	public char getModulationIndex()
	{
		byte field[] = getField(IDX_MODINDEX, 1);
		if (field == null)
			return 'U';
		return (char)field[0];
	}

	/**
	  @return 'N' for normal, 'F' for fair, 'P' for poor, or 'U' for unknown.
	*/
	public char getDataQuality()
	{
		byte field[] = getField(IDX_DATAQUALITY, 1);
		if (field == null)
			return 'U';
		return (char)field[0];
	}

	/**
	  @return GOES Channel number in range 1...266, 0 if unknown.
	*/
	public int getGoesChannel()
	{
		if (!DcpMsgFlag.isGOES(flagbits))
			return 0;
		
		byte field[] = getField(IDX_GOESCHANNEL, 3);
		if (field == null)
			return 0;
		for(int i=0; i<field.length; i++)
			if (field[i] == (byte)' ')
				field[i] = (byte)'0';

		try { return Integer.parseInt(new String(field)); }
		catch(NumberFormatException ex)
		{
			return 0;
		}
	}

	/**
	  @return 'E' for GOES East, 'W' for GOES West, 'U' for unknown.
	*/
	public char getGoesSpacecraft()
	{
		byte field[] = getField(IDX_GOES_SC, 1);
		if (field == null)
			return 'U';
		return (char)field[0];
	}

	/**
	  Uplink status is represented in the message by 2 hex digits.
	  This is also used to store a 2-char DRGS source code.
	  @return uplink status code as an integer.
	*/
	public String getDrgsCode()
	{
		byte field[] = getField(DRGS_CODE, 2);
		if (field == null)
			return "xx";
		return new String(field);
	}

	/**
	  @return the length of the DCP data, as reported in the message, or 0
	   if length cannot be parsed.
	*/
	public int getDcpDataLength()
	{
		if (!isGoesMessage())
			return this.getMsgLength();
		
		byte field[] = getField(IDX_DATALENGTH, 5);
		if (field == null)
			return 0;
		try { return Integer.parseInt(new String(field)); }
		catch(NumberFormatException ex)
		{
			return 0;
		}
	}

	/**
	  @return the message-proper. That is, the data actually sent by 
	  the DCP.  Note that due to possible transmission errors, the 
	  length may not be equal to the value returned by getDcpDataLength.
	*/
	public byte[] getDcpData()
	{
		if (data.length <= IDX_DATA)
			return new byte[0];

		return getField(IDX_DATA, data.length-IDX_DATA);
	}

	// Constants for accessing fields. ===============
	public static final int IDX_DCP_ADDR      = 0;
	public static final int IDX_YEAR          = 8;
	public static final int IDX_DAY           = 10;
	public static final int IDX_HOUR          = 13;
	public static final int IDX_MIN           = 15;
	public static final int IDX_SEC           = 17;
	public static final int IDX_FAILCODE      = 19;
	public static final int IDX_SIGSTRENGTH   = 20;
	public static final int IDX_FREQOFFSET    = 22;
	public static final int IDX_MODINDEX      = 24;
	public static final int IDX_DATAQUALITY   = 25;
	public static final int IDX_GOESCHANNEL   = 26;
	public static final int IDX_GOES_SC       = 29;
	public static final int DRGS_CODE    = 30;
	public static final int IDX_DATALENGTH    = 32;
	public static final int IDX_DATA          = 37;

	public static final int DCP_MSG_MIN_LENGTH = 37;

	/**
	  @return entire DCP message as a string.
	*/
	public String toString()
	{
		return new String(data);
	}

	/**
	  @return the 37-char DOMSAT header portion of the message.
	*/
	public String getHeader()
	{
		if (data.length < 37)
			return new String(data);
		else
			return new String(data, 0, 37);
	}

	/**
	  Make a quasi-unique temporary file suitable for storing this message.
	  @param sequence sequence number to make the filename unique
	  @return filename
	*/
	public String makeFileName(int sequence)
	{
		
		return getDcpAddress().toString() + "-" + sequence;
/*
		try
		{
			StringBuffer sb = new StringBuffer(getDcpAddress().toString());
			sb.append('.');
			int x = sequence % 65536;
			sb.append((char)((int)'a' + (x/676)));
			sb.append((char)((int)'a' + ((x%676)/26)));
			sb.append((char)((int)'a' + (x%26)));
			return new String(sb);
		}
		catch (Exception e)
		{
			return "tmpdcpfile";
		}
*/
	}

	public void setBaud(int b)
	{
		baud = b;
		switch(b)
		{
		case 100: flagbits |= DcpMsgFlag.BAUD_100; break;
		case 300: flagbits |= DcpMsgFlag.BAUD_300; break;
		case 1200: flagbits |= DcpMsgFlag.BAUD_1200; break;
		}
	}

	public int getBaud() { return baud; }

	/**
     * @param seqFileName the seqFileName to set
     */
    public void setSeqFileName(String seqFileName)
    {
	    this.seqFileName = seqFileName;
    }

	/**
     * @return the seqFileName
     */
    public String getSeqFileName()
    {
	    return seqFileName;
    }

	/**
     * @param carrierStart the carrierStart to set
     */
    public void setCarrierStart(Date carrierStart)
    {
	    this.carrierStart = carrierStart;
    }

	/**
     * @return the carrierStart
     */
    public Date getCarrierStart()
    {
	    return carrierStart;
    }

	/**
     * @param carrierStop the carrierStop to set
     */
    public void setCarrierStop(Date carrierStop)
    {
	    this.carrierStop = carrierStop;
    }

	/**
     * @return the carrierStop
     */
    public Date getCarrierStop()
    {
	    return carrierStop;
    }

	/**
     * @param domsatTime the domsatTime to set
     */
    public void setDomsatTime(Date domsatTime)
    {
	    this.domsatTime = domsatTime;
    }

	/**
     * @return the domsatTime
     */
    public Date getDomsatTime()
    {
	    return domsatTime;
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
     * @param xmitTime the xmitTime to set
     */
    public void setXmitTime(Date xmitTime)
    {
	    this.xmitTime = xmitTime;
    }

	/**
     * @return the xmitTime
     */
    public Date getXmitTime()
    {
	    return xmitTime;
    }

	/**
     * @param mtmsm the mtmsm to set
     */
    public void setMtmsm(int mtmsm)
    {
	    this.mtmsm = mtmsm;
    }

	/**
     * @return the mtmsm
     */
    public int getMtmsm()
    {
	    return mtmsm;
    }

	/**
     * @param cdrReference the cdrReference to set
     */
    public void setCdrReference(long cdrReference)
    {
	    this.cdrReference = cdrReference;
    }

	/**
     * @return the cdrReference
     */
    public long getCdrReference()
    {
	    return cdrReference;
    }

	/**
     * @param platformId the platformId to set
     */
    public void setDcpAddress(DcpAddress dcpAddress)
    {
	    this.dcpAddress = dcpAddress;
    }

	/**
     * @return the platformId
     */
    public DcpAddress getDcpAddress()
    {
	    return dcpAddress;
    }
    
	public byte[] getData() { return data; }

	/**
     * @param origAddress the origAddress to set
     */
    public void setOrigAddress(DcpAddress origAddress)
    {
	    this.origAddress = origAddress;
    }

	/**
     * @return the origAddress
     */
    public DcpAddress getOrigAddress()
    {
	    return origAddress;
    }

	/**
     * @param battVolt the battVolt to set
     */
    public void setBattVolt(double battVolt)
    {
	    this.battVolt = battVolt;
    }

	/**
     * @return the battVolt
     */
    public double getBattVolt()
    {
	    return battVolt;
    }

	/**
     * @return the Iridium SBD sessionStatus
     */
    public int getSessionStatus()
    {
    	return sessionStatus;
    }

	/**
     * @param sessionStatus the Iridium SBD sessionStatus to set
     */
    public void setSessionStatus(int sessionStatus)
    {
    	this.sessionStatus = sessionStatus;
    	if (isIridium())
    		setFailureCode(sessionStatus <= 2 ? 'G' : '?');

    }
    
    public void setFailureCode(char fc)
    {
    	this.failureCode = fc;
    	this.addXmitFailureCode(fc);
    }

	/**
     * @return the flagbits
     */
    public int getFlagbits()
    {
    	return flagbits;
    }

	/**
     * @param flagbits the flagbits to set
     */
    public void setFlagbits(int flagbits)
    {
    	this.flagbits = flagbits;
    	if (DcpMsgFlag.isNetDcp(flagbits))
    		setFailureCode('G');
    }
    
    /**
     * @return the length of the message-proper, not including any header.
     */
    public int getMessageLength()
    {
    	return getMsgLength() - headerLength;
    }
    
    public void setHeaderLength(int headerLength)
    {
    	this.headerLength = headerLength;
    }

    /**
     * @return database key for this DCP message if it was read from a SQL
     * database, or Constants.undefinedId if not.
     */
	public DbKey getRecordId() { return recordId; }

	/**
	 * Sets the database key for this message.
	 * @param id the database key.
	 */
	public void setRecordId(DbKey id) { recordId = id; }

	/**
	  Adds a failure code to this record.
	  @param code the failure code
	*/
	public void addXmitFailureCode(char code)
	{
		int i;
		for(i=0; i < MAX_FAILURE_CODES && xmitFailureCodes[i] != '\0'; i++)
			if (xmitFailureCodes[i] == code)
				return;
		if (i < MAX_FAILURE_CODES)
			xmitFailureCodes[i] = code;
	}

	/** @return a list of failure codes as a string */
	public String getXmitFailureCodes()
	{
		int i=0;
		while(i < MAX_FAILURE_CODES && xmitFailureCodes[i] != '\0')
			i++;
		if (i == 0) return "-";
		return new String(xmitFailureCodes, 0, i);
	}

	public boolean hasXmitFailureCode(char code)
	{
		for(int i=0; i < MAX_FAILURE_CODES && xmitFailureCodes[i] != '\0'; i++)
			if (xmitFailureCodes[i] == code)
				return true;
		return false;
	}

	public void rmXmitFailureCode(char code)
	{
		for(int i=0; i < MAX_FAILURE_CODES && xmitFailureCodes[i] != '\0'; i++)
			if (xmitFailureCodes[i] == code)
			{
				for(; i<MAX_FAILURE_CODES-1; i++)
					xmitFailureCodes[i] = xmitFailureCodes[i+1];
				xmitFailureCodes[MAX_FAILURE_CODES-1] = '\0';
				return;
			}
	}
	
	/** These character codes indicate various types of errors */
	public static final String badCodes = "?MTUBIQW";
	
	public boolean hasAnyXmitErrors()
	{
		for(int i=0; i < xmitFailureCodes.length; i++)
			if (badCodes.indexOf(xmitFailureCodes[i]) >= 0)
				return true;
		return false;
	}

	public boolean isGoesRandom()
	{
		return (getFlagbits() & DcpMsgFlag.MSG_TYPE_MASK) == DcpMsgFlag.MSG_TYPE_GOES_RD;
	}

	public boolean hasCarrierTimes()
	{
		int f = getFlagbits();
		return (f & DcpMsgFlag.HAS_CARRIER_TIMES) != 0
			&& (f & DcpMsgFlag.CARRIER_TIME_EST) == 0; 
	}

	public XmitWindow getXmitTimeWindow()
	{
		return xmitWindow;
	}

	public void setXmitWindow(XmitWindow xmitWindow)
	{
		this.xmitWindow = xmitWindow;
	}

	/**
	 * For DCP mon, a very long message may be only partially read. Return true
	 * if the entire message is already present in this object. Return false if
	 * extended message blocks are required.
	 * @return true if entire message is already present here.
	 */
	public boolean isReadComplete()
	{
		return msgLength <= data.length;
	}

	public void setMsgLength(int msgLength)
	{
		this.msgLength = msgLength;
	}

	public int getDayNumber()
	{
		return dayNumber;
	}

	public void setDayNumber(int dayNumber)
	{
		this.dayNumber = dayNumber;
	}
	
	public String getStartTimeStr()
	{
		if (carrierStart != null)
		{
			String ret = "";
			synchronized(timeSdfMS) { ret = timeSdfMS.format(carrierStart); }
			// Limit to tenths of seconds.
			if (ret.length() > 10)
				ret = ret.substring(0,10);
			return ret;
		}
		else
		{
			synchronized(timeSdfSec) { return timeSdfSec.format(getXmitTime()); }
		}
	}

	public String getStopTimeStr()
	{
		String ret = "";
		if (carrierStop != null)
		{
			synchronized(timeSdfMS) { ret = timeSdfMS.format(carrierStop); }
			// Limit to tenths of seconds.
			if (ret.length() > 10)
				ret = ret.substring(0,10);
		}
		else
		{
			double dursec = getMessageLength() * 8.0 / (double)baud;
			if (baud == 300)
				dursec += .693;
            else // 1200
            	dursec += .298;
			Date stop = new Date(getXmitTime().getTime() + (long)(dursec*1000));
			synchronized(timeSdfSec) { ret = timeSdfSec.format(stop); }
			System.out.println("getStopTimeStr() computed="+stop+", fmt='" + timeSdfSec.format(stop) + ", ret='" + ret + "'");
		}
		return ret;
	}
	
	public String getWindowStartStr()
	{
		if (xmitWindow == null)
			return "";
		return IDateFormat.printSecondOfDay(xmitWindow.thisWindowStart, true);
	}

	public String getWindowStopStr()
	{
		if (xmitWindow == null)
			return "";
		return IDateFormat.printSecondOfDay(
			xmitWindow.thisWindowStart + xmitWindow.windowLengthSec, true);
	}
	
	public String getBattVoltStr()
	{
		if (battVolt < .01)
			return "N/A";
		synchronized(bvFormat) { return bvFormat.format(battVolt); }
	}
	
	/**
	 * This method is used by the Html formatter and the DCP Monitor JSF code to
	 * print a block of data for display on an HTML page.
	 * @return
	 */
	public String getDataStr()
	{
		// MJM 20170407 improvements to rendering raw message added.
		String msgStr = new String(getData());
		int newlines=0, longestSpan=0, span=0;
		for(int idx = 0; idx < msgStr.length(); idx++)
		{
			char c = msgStr.charAt(idx);
			if (Character.isWhitespace(c))
			{
				if (span > longestSpan)
					longestSpan = span;
				span = 0;
				if (c == '\n' || c == '\r') 
					newlines++;
			}
			else
				span++;
		}
		if (span > longestSpan)
			longestSpan = span;

		// Long messages with no whitespace, e.g. pseudobinary. Add space separators.
		if (longestSpan > 80)
			msgStr = HtmlFormatter.wrapString(msgStr);
		else if (newlines > 2)
			// Preserve line breaks in formatted ascii messages like RAWS data.
			msgStr = "<pre>" + msgStr + "</pre>";
Logger.instance().info("writeRaw: msglen=" + msgStr.length() + ", newlines=" + newlines + ", longestSpan=" + longestSpan);

		return msgStr;
	}
	
	public String getSource()
	{
		if (DcpMsgFlag.isGOES(flagbits))
			return "GOES";
		else if (isIridium())
			return "Iridium";
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(
			new ByteArrayInputStream(data)));
		String line;
		try
		{
			while((line = lnr.readLine()) != null && line.startsWith("//"))
			{
				line = line.substring(2).trim();
				if (line.startsWith("SOURCE"))
					return line.substring(6).trim();
			}
		}
		catch (IOException e) { /* Won't happen */ }
		finally { try { lnr.close(); } catch(Exception ex) {} }
		return "";
	}
	
	public boolean isIridium()
	{
		return DcpMsgFlag.isIridium(flagbits)
			|| (data[0] == (byte)'I' && data[1] == (byte)'D' && data[2] == (byte)'=');
	}
	
	public double getGoesSignalStrength()
	{
		return goesSignalStrength;
	}

	public void setGoesSignalStrength(double goesSignalStrength)
	{
		this.goesSignalStrength = goesSignalStrength;
	}

	public double getGoesFreqOffset()
	{
		return goesFreqOffset;
	}

	public void setGoesFreqOffset(double goesFreqOffset)
	{
		this.goesFreqOffset = goesFreqOffset;
	}

	public double getGoesGoodPhasePct()
	{
		return goesGoodPhasePct;
	}

	public void setGoesGoodPhasePct(double goesGoodPhasePct)
	{
		this.goesGoodPhasePct = goesGoodPhasePct;
	}

	public double getGoesPhaseNoise()
	{
		return goesPhaseNoise;
	}

	public void setGoesPhaseNoise(double goesPhaseNoise)
	{
		this.goesPhaseNoise = goesPhaseNoise;
	}


}
