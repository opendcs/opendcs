/*
*  $Id$
*  
*  Open Source Software
*  
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.9  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*/
package decodes.dcpmon;

import ilex.util.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import lrgs.common.DcpAddress;
import decodes.db.Constants;
import decodes.sql.DbKey;

/**
The DcpMonitor application stores one of these records in Database for every
DCP message it encounters. Each one represents a single DCP transmission
plus any DAPS-generated error messages that refer to it.
*/
public class XmitRecord 
{
	/** DCP Address as a long integer */
	private DcpAddress dcpAddress;

	/** The second of the day. Have to know day # to get full time stamp. */
	private int secOfDay;

	/** Goes header fields: */
	private char failureCodes[];

	/** Max number of failure codes any messge can have */
	public static final int MAX_FAILURE_CODES = 8;

	/** Signal strength */
	private int signalStrength;

	/** message length */
	private int msgLength;

	/** GOES channel number (1...266) */
	private int goesChannel;

	/** Frequency offset */
	private int freqOffset;

	/** Modulation index */
	private char modIndex;

	/** Decoded from the message. Will be zero if no decoding was done. */
	private float battVolt;

	/** Start of window for this transmission */
	private int windowStartSec;

	/** Length of window in seconds. */
	private int windowLength;

	/** Interval between transmissions in seconds */
	private int xmitInterval;

	/** Start of carrier (msec since epoch) */
	private long carrierStart;
	
	/** End of carrier (msec since epoch) */
	private long carrierEnd;
	
	/** Various binary flags -- see XmitRecordFlags.java */
	private int flags;

	/** ID representing source of this msg. */
	private DbKey sourceId;

	/** Time transmitted over DOMSAT (int time_t format), if known. */
	private int domsatTime;

	/** Set when preparing output. */
	String basin;

//	/** Set when preparing output. */
//	String dcpName;

	int firstXmitSecOfDay;

	/** The day number. */
	private int dayNum;

	private DbKey recordId;

	/** The Up link carrier code This indicates the DRGS code
	 * that this message came from */
	private String drgsCode;
	
	/** the raw message, this is the entire data message */
	private byte[] rawMsg;
	
	/** Used by DB/IO to detect what needs to be updated. */
	public int dbChangedFlags;
	public static final int ADDR_CHANGED      = 0x00001;
	public static final int  SOD_CHANGED      = 0x00002;
	public static final int   FC_CHANGED      = 0x00004;
	public static final int   SS_CHANGED      = 0x00008;
	public static final int  LEN_CHANGED      = 0x00010;
	public static final int CHAN_CHANGED      = 0x00020;
	public static final int   FO_CHANGED      = 0x00040;
	public static final int   MI_CHANGED      = 0x00080;
	public static final int   BV_CHANGED      = 0x00100;
	public static final int  WST_CHANGED      = 0x00200;
	public static final int WLEN_CHANGED      = 0x00400;
	public static final int XINT_CHANGED      = 0x00800;
	public static final int CARS_CHANGED      = 0x01000;
	public static final int CARE_CHANGED      = 0x02000;
	public static final int  SRC_CHANGED      = 0x04000;
	public static final int DOMS_CHANGED      = 0x08000;
	public static final int FLAG_CHANGED      = 0x10000;
	public static final int DRGS_CODE_CHANGED    = 0x20000;
	public static final int RAWMSG_CHANGED    = 0x40000;

	/** Default Constructor */
	public XmitRecord()
	{
		dcpAddress = null;
		secOfDay = 0;
		failureCodes = new char[MAX_FAILURE_CODES];
		for(int i=0; i<MAX_FAILURE_CODES; i++)
			failureCodes[i] = '\0';
		signalStrength = 0;
		msgLength = 0;
		goesChannel = 0;
		freqOffset = 0;
		modIndex = '?';
		battVolt = (float)0.0;
		basin = "-";
//		dcpName = "-";
		firstXmitSecOfDay = -1;
		windowStartSec = -1;
		windowLength = 60;
		xmitInterval = 0;
		carrierStart = carrierEnd = 0L;
		flags = 0;
		sourceId = Constants.undefinedId;
		domsatTime = 0;
		firstXmitSecOfDay = 0;
		dbChangedFlags = 0;
		dayNum = 0;
		recordId = Constants.undefinedId;
		drgsCode = "";
		rawMsg = null;
	}

	/**
	  Construct for particular DCP message
	  @param addr the DCP address
	  @param sod the second-of-day for this messge time
	*/
	public XmitRecord(DcpAddress addr, int sod, int dayNum)
	{
		this();
		dcpAddress = new DcpAddress(addr);
		secOfDay = sod;
		this.dayNum = dayNum;
	}

	/**
	  Adds a failure code to this record.
	  @param code the failure code
	*/
	public void addCode(char code)
	{
		int i;
		for(i=0; i < MAX_FAILURE_CODES && failureCodes[i] != '\0'; i++)
			if (failureCodes[i] == code)
				return;
		if (i < MAX_FAILURE_CODES)
		{
			failureCodes[i] = code;
			dbChangedFlags |= FC_CHANGED;
		}
	}

	/** @return a list of failure codes as a string */
	public String failureCodes()
	{
		int i=0;
		while(i < MAX_FAILURE_CODES && failureCodes[i] != '\0')
			i++;
		if (i == 0) return "-";
		return new String(failureCodes, 0, i);
	}

	public boolean hasFailureCode(char code)
	{
		for(int i=0; i < MAX_FAILURE_CODES && failureCodes[i] != '\0'; i++)
			if (failureCodes[i] == code)
				return true;
		return false;
	}

	public void rmFailureCode(char code)
	{
		for(int i=0; i < MAX_FAILURE_CODES && failureCodes[i] != '\0'; i++)
			if (failureCodes[i] == code)
			{
				for(; i<MAX_FAILURE_CODES-1; i++)
					failureCodes[i] = failureCodes[i+1];
				failureCodes[MAX_FAILURE_CODES-1] = '\0';
				dbChangedFlags |= FC_CHANGED;
				return;
			}
	}

	/** @return dcpAddress */
	public DcpAddress getDcpAddress() 
	{
		return dcpAddress;
	}

	/** Sets dcpAddress */
	public void setDcpAddress(DcpAddress dcpAddress) 
	{
		if (this.dcpAddress != null
		 && this.dcpAddress.equals(dcpAddress))
			return;
		this.dcpAddress = dcpAddress;
		dbChangedFlags |= ADDR_CHANGED;
	}

	/** @return secOfDay */
	public int getSecOfDay() { return secOfDay; }

	/** Sets secOfDay */
	public void setSecOfDay(int secOfDay) 
	{
		if (this.secOfDay != secOfDay)
		{
			this.secOfDay = secOfDay;
			dbChangedFlags |= SOD_CHANGED;
		}
	}

	/** @return signalStrength */
	public int getSignalStrength() { return signalStrength; }

	/** Sets signalStrength */
	public void setSignalStrength(int signalStrength) 
	{
		if (this.signalStrength != signalStrength)
		{
			this.signalStrength = signalStrength;
			dbChangedFlags |= SS_CHANGED;
		}
	}

	/** @return msgLength */
	public int getMsgLength() { return msgLength; }

	/** Sets msgLength */
	public void setMsgLength(int msgLength) 
	{
		if (this.msgLength != msgLength)
		{
			this.msgLength = msgLength; 
			dbChangedFlags |= LEN_CHANGED;
		}
	}

	/** @return goesChannel */
	public int getGoesChannel() { return goesChannel; }

	/** Sets goesChannel */
	public void setGoesChannel(int goesChannel)
	{
		if (this.goesChannel != goesChannel)
		{
			this.goesChannel=goesChannel;
			dbChangedFlags |= CHAN_CHANGED;
		}
	}

	/** @return freqOffset */
	public int getFreqOffset() { return freqOffset; }

	/** Sets freqOffset */
	public void setFreqOffset(int freqOffset) 
	{
		if (this.freqOffset != freqOffset)
		{
			this.freqOffset = freqOffset;
			dbChangedFlags |= FO_CHANGED;
		}
	}

	/** @return modIndex */
	public char getModIndex() { return modIndex; }

	/** Sets modIndex */
	public void setModIndex(char modIndex)
	{
		if (this.modIndex != modIndex)
		{
			this.modIndex = modIndex;
			dbChangedFlags |= MI_CHANGED;
		}
	}

	/** @return battVolt */
	public float getBattVolt() { return battVolt; }

	/** Sets battVolt */
	public void setBattVolt(float battVolt) 
	{
		if (this.battVolt != battVolt)
		{
			this.battVolt = battVolt;
			dbChangedFlags |= BV_CHANGED;
		}
	}

	/** @return basin */
	public String getBasin() { return basin; }

	/** Sets basin */
	public void setBasin(String basin) { this.basin = basin; }

//	/** @return dcpName */
//	public String getDcpName() { return dcpName; }
//
//	/** Sets dcpName */
//	public void setDcpName(String dcpName) { this.dcpName = dcpName; }

	/** @return windowStartSec */
	public int getWindowStartSec() { return windowStartSec; }

	/** Sets windowStartSec */
	public void setWindowStartSec(int windowStartSec) 
	{
		if (this.windowStartSec != windowStartSec)
		{
			this.windowStartSec = windowStartSec;
			dbChangedFlags |= WST_CHANGED;
		}
	}

	/** @return windowLength */
	public int getWindowLength() { return windowLength; }

	/** Sets windowLength */
	public void setWindowLength(int windowLength) 
	{
		if (this.windowLength != windowLength)
		{
			this.windowLength = windowLength;
			dbChangedFlags |= WLEN_CHANGED;
		}
	}

	/** @return xmitInterval */
	public int getXmitInterval() { return xmitInterval; }

	/** Sets xmitInterval */
	public void setXmitInterval(int xmitInterval) 
	{
		if (this.xmitInterval != xmitInterval)
		{
			this.xmitInterval = xmitInterval;
			dbChangedFlags |= XINT_CHANGED;
		}
	}

	/** @return carrierStart */
	public long getCarrierStart() { return carrierStart; }

	/** Sets carrierStart */
	public void setCarrierStart(long carrierStart) 
	{
		if (this.carrierStart != carrierStart)
		{
			this.carrierStart = carrierStart;
			dbChangedFlags |= CARS_CHANGED;
		}
	}

	/** @return carrierEnd */
	public long getCarrierEnd() { return carrierEnd; }

	/** Sets carrierEnd */
	public void setCarrierEnd(long carrierEnd)
	{
		if (this.carrierEnd != carrierEnd)
		{
			this.carrierEnd = carrierEnd;
			dbChangedFlags |= CARE_CHANGED;
		}
	}

	public void setHasCarrierTimeMsec(boolean tf)
	{
		boolean currentSetting = 
			(this.flags & XmitRecordFlags.CARRIER_TIME_MSEC) != 0;
		if (tf == currentSetting)
			return;
		if (tf)
			this.flags |= XmitRecordFlags.CARRIER_TIME_MSEC;
		else
			this.flags &= (~XmitRecordFlags.CARRIER_TIME_MSEC);
		dbChangedFlags |= FLAG_CHANGED;
	}

	/** @return flags */
	public int getFlags() { return flags; }

	/** Add flag bits */
	public void addFlags(int flags) 
	{
		if (this.flags != flags)
		{
			this.flags |= flags; 
			dbChangedFlags |= FLAG_CHANGED;
		}
	}

	/** Sets flags */
	public void clearFlags() { this.flags = 0; }

	/** @return sourceId */
	public DbKey getSourceId() { return sourceId; }


	/** Sets sourceId */
	public void setSourceId(DbKey sourceId)
	{
		if (!this.sourceId.equals(sourceId))
		{
			this.sourceId = sourceId;
			dbChangedFlags |= SRC_CHANGED;
		}
	}

	/** @return domsatTime as msec since epoch*/
	public long getDomsatTimeMsec() { return domsatTime * 1000L; }

	/** Sets domsatTime as msec since epoch*/
	public void setDomsatTimeMsec(long domsatTime)
	{
		if (this.domsatTime != (int)(domsatTime / 1000L))
		{
			this.domsatTime = (int)(domsatTime / 1000L);
			dbChangedFlags |= DOMS_CHANGED;
		}
	}

	public long getGoesTimeMsec()
	{
		return (dayNum * RecentDataStore.MSEC_PER_DAY) + (secOfDay * 1000L);
	}

	public boolean isRandom()
	{
		return (flags & XmitRecordFlags.IS_RANDOM) != 0;
	}

	public DbKey getRecordId() { return recordId; }

	public void setRecordId(DbKey id) { recordId = id; }

	public int getDayNum() { return dayNum; }

	public void setDayNum(int dayNum) { this.dayNum = dayNum; }

	public static final String badCodes = "?MTUBIQW";
	public boolean hasAnyErrors()
	{
		for(int i=0; i < failureCodes.length; i++)
			if (badCodes.indexOf(failureCodes[i]) >= 0)
				return true;
		return false;
	}

	/** Get the up link carrier 2 char code */
	public String getUpLinkCarrier()
	{
		return drgsCode;
	}
	
	/** set the up link carrier 2 char code */
	public void setDrgsCode(String drgsCode)
	{
		if (this.drgsCode != null && this.drgsCode.equals(drgsCode))
			return;
		this.drgsCode = drgsCode;
		dbChangedFlags |= DRGS_CODE_CHANGED;
	}

	public byte[] getRawMsg()
	{
		return rawMsg;
	}

	public void setRawMsg(byte[] rawMsg)
	{
		if (this.rawMsg != null)
		{
			int i=0;
			if (this.rawMsg.length == rawMsg.length)
				for(i=0; i<rawMsg.length; i++)
					if (this.rawMsg[i] != rawMsg[i])
						break;
			if (i == rawMsg.length)
				return;
		}
		this.rawMsg = rawMsg;
		dbChangedFlags |= RAWMSG_CHANGED;
	}
	
	public String getChanges()
	{
		StringBuilder sb = new StringBuilder("Changes: ");
		if ((dbChangedFlags & ADDR_CHANGED) != 0)
			sb.append("addr=" + dcpAddress + " ");
		if ((dbChangedFlags & SOD_CHANGED) != 0)
			sb.append("secOfDay=" + secOfDay + " ");
		if ((dbChangedFlags & FC_CHANGED) != 0)
			sb.append("failureCodes=" + failureCodes() + " ");
		if ((dbChangedFlags & SS_CHANGED) != 0)
			sb.append("signalStrength=" + signalStrength + " ");
		if ((dbChangedFlags & LEN_CHANGED) != 0)
			sb.append("msgLength=" + msgLength + " ");
		if ((dbChangedFlags & CHAN_CHANGED) != 0)
			sb.append("goesChannel=" + goesChannel + " ");
		if ((dbChangedFlags & FO_CHANGED) != 0)
			sb.append("freqOffset=" + freqOffset + " ");
		if ((dbChangedFlags & MI_CHANGED) != 0)
			sb.append("modIndex=" + modIndex + " ");
		if ((dbChangedFlags & BV_CHANGED) != 0)
			sb.append("battVolt=" + battVolt + " ");
		if ((dbChangedFlags & WST_CHANGED) != 0)
			sb.append("windowStartSec=" + windowStartSec + " ");
		if ((dbChangedFlags & WLEN_CHANGED) != 0)
			sb.append("windowLength=" + windowLength + " ");
		if ((dbChangedFlags & XINT_CHANGED) != 0)
			sb.append("xmitInterval=" + xmitInterval + " ");
		if ((dbChangedFlags & CARS_CHANGED) != 0)
			sb.append("carrierStart=" + carrierStart + " ");
		if ((dbChangedFlags & CARE_CHANGED) != 0)
			sb.append("carrierEnd=" + carrierEnd + " ");
		if ((dbChangedFlags & FLAG_CHANGED) != 0)
			sb.append("flags=" + flags + " ");
		if ((dbChangedFlags & SRC_CHANGED) != 0)
			sb.append("sourceId=" + sourceId + " ");
		if ((dbChangedFlags & DOMS_CHANGED) != 0)
			sb.append("domsatTime=" + domsatTime + " ");
		if ((dbChangedFlags & DRGS_CODE_CHANGED) != 0)
			sb.append("drgsCode=" + drgsCode + " ");
		if ((dbChangedFlags & RAWMSG_CHANGED) != 0)
			sb.append("Raw Msg Changed len=" + rawMsg.length);
		
		return sb.toString();
	}
	
	public void clearDbChangedFlags()
	{
		dbChangedFlags = 0;
	}

	public void debugResolution()
	{
//For testing
		if (Logger.instance().getMinLogPriority() == Logger.E_DEBUG1)
		{
//			DcpAddress daddr = new DcpAddress(dcpAddress);
			String mediumId = dcpAddress.toString();
			SimpleDateFormat dateTimeFmtSSS = 
				new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss.SSS");
			dateTimeFmtSSS.setTimeZone(TimeZone.getTimeZone("UTC"));
			if ((getFlags() & XmitRecordFlags.CARRIER_TIME_MSEC) != 0)
			{
			Logger.instance().debug1("XmitRecord: flag for CARRIER_TIME_MSEC "+
					"Msg CONTAINS millisecond resolution"
					+ "\n DCP Address = " + mediumId + " \n Channel = " + 
												goesChannel
					+ "\n carrierStart = " + dateTimeFmtSSS.format(
												new Date(carrierStart)) 
					+ "\n carrierEnd = " + dateTimeFmtSSS.format(
												new Date(carrierEnd))
					+ "\n uplink carrier = " + drgsCode);
			
					if (rawMsg != null)
						Logger.instance().debug1("\nraw data = " + 
								new String(rawMsg));
					else
						Logger.instance().debug1("\nraw data = null");
			}
			else
			{
			Logger.instance().debug1("XmitRecord: flag for CARRIER_TIME_MSEC "+
					"Msg contains DOES NOT millisecond resolution"
					+ "\n DCP Address = " + mediumId + " \n Channel = " + 
												goesChannel
					+ "\n carrierStart = " + dateTimeFmtSSS.format(
												new Date(carrierStart)) 
					+ "\n carrierEnd = " + dateTimeFmtSSS.format(
												new Date(carrierEnd))
					+ "\n uplink carrier = " + drgsCode);
				if (rawMsg != null)
					Logger.instance().debug1("\nraw data = " + 
						new String(rawMsg));
				else
					Logger.instance().debug1("\nraw data = null");
			}	
		}
//End for testing
	}
}
