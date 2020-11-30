/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.drgsrecv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import ilex.net.BasicClient;
import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.FileLogger;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.db.LrgsConstants;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.Outage;


/**
Handles interaction with a single DRGS message thread.
The Message socket is a one way stream of DCP messages coming from the
DRGS. This class connects and continually reads and parses the message
data. It converts it into an LRGS-style DcpMsg structure and then passes
it to the archive process for saving.
*/
public class DrgsRecvMsgThread 
	extends BasicClient
	implements Runnable, LrgsInputInterface
{
	private int myConnectNum;
	private int mySlot;
	protected boolean configChanged;
	protected boolean _isShutdown;
	protected byte[] startPattern;
	static byte[] nonePattern = { (byte)'N', (byte)'O', (byte)'N', (byte)'E' };
	protected boolean _enabled;
	private int seqNum;
	protected long lastResponseTime;
	protected MsgArchive msgArchive;

	// States for reading data from the socket:
	public static final int HUNT_STATE = 0;
	public static final int HEADER_STATE = 1;
	public static final int MSGDATA_STATE = 2;
	public static final int TERMCRLF_STATE = 3;
	public static final int CARRIERTIMES_STATE = 4;
	public static final int EXTENDED_QUAL_STATE = 5;
	private int state;

	// Private scratch-pad variables:
	private int startIndex;
	private int noneIndex;
	private byte spr[];
	private byte npr[];
	private int totalReadBeforeSync;
	private byte headerBuf[];
	private DcpMsg workingMsg;
	private int msgBytesRead;
	private int dataLength;

	protected String status;
	protected String myName;
//	private boolean drgsCrLfPoll = false;

	private File cfgFile; // Configuration file containing chan assigns.
	private int chanArray[];

	/** Original address for last message received. */
	private byte[] origAddr;

	private byte sourceCode[];

	private SimpleDateFormat carrierDateFmt;
	protected SimpleDateFormat debugDateFmt;

	private boolean msgHasCarrierTimes;
	private boolean msgHasExtendedQual;
	private boolean isBinaryMsg;
	private byte[] carrierBuf = new byte[31];
	private byte[] extQualBuf = new byte[120];
	private int extQualIdx = 0;

	private Outage channelOutage;

//	private NumberFormat lenFormat;
	static byte[] crlf = new byte[2];
	static
	{
		IDateFormat.alwaysIncludeSeconds = true;
		crlf[0] = (byte)'\r';
		crlf[1] = (byte)'\n';
	}

	protected int dataSourceId = lrgs.db.LrgsConstants.undefinedId;
	protected LrgsMain lrgsMain;
	protected FileLogger activityLogger;
	protected boolean wasNone = false;
	protected DrgsConnectCfg myCfg = null;
	public int myType = DL_DRGSCON;
	public String myTypeStr = DL_DRGS_TYPESTR;
	public boolean noChannelFile = false;
	public String module = DrgsRecv.module;
	protected long connectRetryDelay = 10000L;
	protected long lastConnectTime = 0L, lastStatusTime = 0L;
	protected int numThisHour = 0, numLastHour = 0;


	/**
	  Constructor.
	  @param connection msgArchive the object that manages the archive.
	  @param pdtSched the PDT Schedule object for validation.
	  @param channelMap the CDT Map object for validation.
	*/
	public DrgsRecvMsgThread(MsgArchive msgArchive, LrgsMain lrgsMain)
	{
		super("", 17010);
		this.msgArchive = msgArchive;
		myConnectNum = -1;
		mySlot = -1;
		configChanged = true;
		_isShutdown = false;
		startPattern = new byte[] { (byte)'S', (byte)'M', (byte)'\r',
			 (byte)'\n' };
		_enabled = true;
		state = HUNT_STATE;
		startIndex = 0;
		noneIndex = 0;
		spr = new byte[4];
		npr = new byte[4];
		totalReadBeforeSync = 0;
		headerBuf = new byte[55];
		workingMsg = null;
		msgBytesRead = 0;
		dataLength = 0;
		seqNum = 0;
		lastResponseTime = 0L;
		status = "initializing";
		myName = "DRGS";
		origAddr = new byte[8];
		chanArray = new int[0];
		sourceCode = new byte[2];
		sourceCode[0] = (byte)'D';
		sourceCode[1] = (byte)'R';
		this.lrgsMain = lrgsMain;
		channelOutage = null;
		activityLogger = null;

		// MJM date format and number formats must be instance variables 
		// to avoid thread clashes.
		carrierDateFmt = new SimpleDateFormat("yyDDDHHmmssSSS");
		carrierDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		debugDateFmt = new SimpleDateFormat("yyyy/DDD-HH:mm:ss.SSS");
		debugDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Sets the connection number.
	 * @param connectNum the connection number.
	 */
	public void setConnectNum(int connectNum)
	{
		this.myConnectNum = connectNum;
	}


	/**
	  Thread run method
	*/
	public void run()
	{
		try{ Thread.sleep(2000L); }
		catch(InterruptedException ex) {}

		lastResponseTime = System.currentTimeMillis();

Logger.instance().info(module + " " + myName + " starting"
+ ", isShutdown=" + _isShutdown + ", isEnabled=" + isEnabled()
+ ", isConnected=" + isConnected());
		while(!_isShutdown)
		{
			long now = System.currentTimeMillis();

			// checkConfig is a hook used by LritDamsNtReceiver. For DRGS, the checkconfig
			// is handled by the parent.
			checkConfig();
			
			if (configChanged)
			{
				configChanged = false;
				disconnect();
				if (isEnabled())
					tryConnect();
			}

			if (isEnabled() && !isConnected()
			 && now - getLastConnectAttempt() > connectRetryDelay)
				tryConnect();

			if (!isConnected())
			{
				try{ Thread.sleep(1000L); }
				catch(InterruptedException ex) {}
			}
			else
			{
				try 
				{ 
					DcpMsg msg = getMsg();
					if (msg != null)
					{
						if (!checkMsgOk(msg))
							continue;
						msg.setSequenceNum(this.getNextSeqNum());
						msgArchive.archiveMsg(msg, this);
						if (activityLogger != null)
							activityLogger.info(msg.getHeader());
					}
					else if (now - lastResponseTime > LrgsConfig.instance().getDamsNtTimeout() * 1000L)
					{
						// More than 20 seconds since either msg or NONE.
						status = "Timeout";
						log(Logger.E_WARNING, DrgsRecv.EVT_TIMEOUT,
						  "Timeout on DAMS-NT Messge Socket -- disconnecting.");
						disconnect();
					}
					else
					{	// Brief pause waiting for more data to arrive.
						try { Thread.sleep(100L); }
						catch(InterruptedException ex) {}
					}
				}
				catch(IOException ex)
				{
					log(Logger.E_WARNING, DrgsRecv.EVT_SOCKIO,
						"Error on DAMS-NT Messge Socket: " + ex);
					disconnect();
				}
			}
		}
		log(Logger.E_INFORMATION, 0, "Disconnecting and exiting.");
		disconnect();
		status = "Shutdown";
		_isShutdown = true;
		lrgsMain.freeInput(mySlot);
	}

	protected void checkConfig()
	{
		// Base class method does nothing.
		// This is a hook to allow stand alone LritDamsNtReceiver clase to check
		// for a config change.
	}

	protected boolean checkMsgOk(DcpMsg msg)
	{
		//If Drgs receives msgs older than an hour ago 
		//- ignore then Check Msg Date 
		Date msgDate = msg.getDapsTime();
		long currentTime = System.currentTimeMillis();
		if (msgDate != null)
		{
			if ((currentTime - msgDate.getTime()) > 3600000 )
			{
				log(Logger.E_WARNING, DrgsRecv.EVT_MSG_TOO_OLD,
					"Received msg older than an hour ago. DCP"
					+ " address: " + msg.getDcpAddress()
					+ ", msgtime=" + debugDateFmt.format(msgDate)
					+ ", curtime=" + debugDateFmt.format(
						new Date(currentTime)));
				return false;
			}
		}
		return true;
	}

	/**
	  Called from parent when this connection has been reconfigured.
	  @param the connection configuration for this DRGS
	*/
	public void configure(DrgsConnectCfg cfg)
	{
		myCfg = cfg;
		for(int i=0; i<4; i++)
			startPattern[i] = cfg.startPattern[i];

		_enabled = cfg.msgEnabled;
		if (myType == LrgsInputInterface.DL_NETDCPCONT
		 || myType == LrgsInputInterface.DL_NETDCPPOLL)
		{
			if (!LrgsConfig.instance().networkDcpEnable)
				_enabled = false;
		}
		else if (!LrgsConfig.instance().enableDrgsRecv)
			_enabled = false;

		if (!_enabled)
		{
			status = "Disabled";
			disconnect();
		}

		if (!getHost().equalsIgnoreCase(cfg.host) || getPort() != cfg.msgPort)
		{
			setHost(cfg.host);
			setPort(cfg.msgPort);
			configChanged = true;
		}

		myName = myType == LrgsInputInterface.DL_NETDCPCONT ? "NetDCP:" :
			myType == LrgsInputInterface.DL_NETDCPPOLL ? "PolledDCP:" 
			: "DRGS:";
		myName = myName + 
			(cfg.name == null ? (cfg.host+":"+cfg.msgPort) : cfg.name);

		if (!noChannelFile)
			loadChannels(cfg.cfgFile != null ? cfg.cfgFile 
				: ("$LRGSHOME/" + cfg.name + ".cfg"));

		if (cfg.drgsSourceCode != null && cfg.drgsSourceCode.length >= 2)
		{
			sourceCode[0] = cfg.drgsSourceCode[0];
			sourceCode[1] = cfg.drgsSourceCode[1];
		}
		dataSourceId = 
			lrgsMain.getDbThread().getDataSourceId(DL_DRGS_TYPESTR, cfg.host);

		String activityLogName = LrgsConfig.instance().getMiscProp(
			"drgsActivityLog");
		if (activityLogName != null && activityLogName.trim().length() > 0)
		{
			String nm = "DRGS-";
			if (cfg.drgsSourceCode != null) nm += cfg.drgsSourceCode;
			try { activityLogger = new FileLogger(nm, activityLogName); }
			catch(IOException ex)
			{
				Logger.instance().warning("Cannot create DRGS activity log '"
					+ activityLogName + "': " + ex);
				activityLogger = null;
			}
		}
		else
			activityLogger = null;
	}

	public DrgsConnectCfg getConfig() { return myCfg; }
	
	public int getDataSourceId() { return dataSourceId; }

	protected void tryConnect()
	{
		log(Logger.E_DEBUG1, 0, "Attempting connection.");
		status = "Connecting";
		try 
		{
			connect(); 
//			socket.setTcpNoDelay(true);
		}
		catch(IOException ex)
		{
			log(Logger.E_WARNING, DrgsRecv.EVT_CANNOT_CONNECT,
				"Connection failed: " + ex);
			status = "Bad-Connect";
			return;
		}
		log(Logger.E_INFORMATION, DrgsRecv.EVT_CONNECTED, "Connected.");
		status = "Connected";

		// Start with clean slate:
		state = HUNT_STATE;
		startIndex = 0;
		noneIndex = 0;
		workingMsg = null;
		lastResponseTime = lastConnectTime = System.currentTimeMillis();
		numThisHour = numLastHour = 0;

		// If we had a channel outage in effect, tag its end time and drop it.
		if (channelOutage != null)
		{
			channelOutage.setEndTime(new Date());
			channelOutage = null;
		}
	}

	/**
	  Walk through the parser-states and return a DCP message when one is
	  completed. Return null if no complete message available at this time.
	  @return DcpMsg or null if no messge available.
	*/
	protected DcpMsg getMsg()
		throws IOException
	{
		boolean stateComplete = true;
		while(stateComplete && isEnabled())
		{
			/* In all states, true return means that the state 
			   was completed and a transition was made.
			   False means available input was exhausted and 
			   to try again later.
			*/
			if (state == HUNT_STATE)
				stateComplete = huntState();
			if (state == HEADER_STATE)
				stateComplete = headerState();
			if (state == MSGDATA_STATE)
			{
				// True return means we now have a complete message.
				// When this happens, state will transition to TERMCRLF.
				stateComplete = msgDataState();
			}
			if (state == TERMCRLF_STATE)
			{
				stateComplete = termCrLfState();
				if (stateComplete && !msgHasCarrierTimes)
				{
					DcpMsg ret = workingMsg;
					workingMsg = null;
					if (ret != null)
						ret.setOrigAddress(new DcpAddress(new String(origAddr)));
					numThisHour++;
					return ret;
				}
			}
			if (state == CARRIERTIMES_STATE)
			{
				stateComplete = carrierTimesState();
				if (stateComplete && state != EXTENDED_QUAL_STATE)
				{
					DcpMsg ret = workingMsg;
					workingMsg = null;
					ret.setOrigAddress(new DcpAddress(new String(origAddr)));
					numThisHour++;
					return ret;
				}
			}
			if (state == EXTENDED_QUAL_STATE)
			{
				if (workingMsg == null)
				{
					Logger.instance().warning("DrgsMsgRcvThread: "
						+ "internal error: state=EXTENDED_QUAL_STATE but workingMsg=null!");
					state = HUNT_STATE;
					return null;
				}
				stateComplete = extendedQualState();
				if (stateComplete)
				{
					DcpMsg ret = workingMsg;
					workingMsg = null;
					try { ret.setOrigAddress(new DcpAddress(new String(origAddr))); }
					catch(Exception ex)
					{
						Logger.instance().warning("DrgsMsgRcvThread: EXTENDED_QUAL_STATE ret is "
							+ (ret==null?"":"NOT") + " null. stateComplete=" + stateComplete
							+ ". msgHasExtendedQual=" + msgHasExtendedQual);
					}
					numThisHour++;
					return ret;
				}
			}
		}
		return null;
	}


	/**
	  Seek for the 4-byte sync pattern, change states when it is found.
	  Return true if pattern found, false if available input exhausted without 
	  success, which means that caller should pause before trying again.
	*/
	private boolean huntState()
		throws IOException
	{
		workingMsg = null;
		wasNone = false;

		if (input.available() <= 0)
			return false;
		while (input.available() > 0)
		{
			byte c = (byte)input.read();

			// Alert user if we read excessive amounts of data with no sync.
			if ((++totalReadBeforeSync % 100) == 0)
				log(Logger.E_DEBUG1, 0, "Skipped "
					+ totalReadBeforeSync + " bytes looking for sync. c=" + (int)c);

			spr[startIndex++] = c;
			if (c == startPattern[startIndex-1])
			{
				if (startIndex >= 4)
				{
					// Success! found complete start pattern.
					startIndex = 0;
					if (totalReadBeforeSync > 4)
						log(Logger.E_DEBUG2, 0, "Skipped "
							+ (totalReadBeforeSync-4) + " bytes before sync.");
					else
						log(Logger.E_DEBUG3, 0, "Acquired sync.");
					totalReadBeforeSync = 0;
					state = HEADER_STATE;
					return true;
				}
			}
			else // mismatch: shift & keep trying
				shiftStartPattern();

			// Simultaneously look for the NONE pattern.
			npr[noneIndex++] = c;
			if (c == nonePattern[noneIndex-1])
			{
				if (noneIndex >= 4)
				{
					// Found complete NONE pattern.
					noneIndex = 0;
					if (totalReadBeforeSync > 4)
						log(Logger.E_DEBUG1, 0, "Skipped "
							+ (totalReadBeforeSync-4) + " bytes before NONE.");
					totalReadBeforeSync = 0;
//					log(Logger.E_DEBUG1, 0, "NONE pattern received.");
					state = TERMCRLF_STATE;
					wasNone = true;
					return true;
				}
			}
			else
				shiftNonePattern();
		}
		return false;
	}

	/** 
	  Called on start-pattern mismatch. We may have already read part of the
	  good sync pattern so shift my scratch-pad spr buffer.
	  Example: startPattern:   0 B 0 A
	           data on stream: 0 B 0 B 0 A
	  The match fails on the 4th char, but we can't just discard all 4, 
	  instead we shift by 2 chars and keep going.
	*/
	private void shiftStartPattern()
	{
		int shift = 1;
		for(; shift < startIndex; shift++)
		{
			boolean match = true;
			for(int i = shift; i < startIndex && match; i++)
				if (spr[i] != startPattern[i-shift])
					match = false;

			if (match)
			{
				for(int i=shift; i<startIndex; i++)
					spr[i-shift] = spr[i];
				startIndex -= shift;
				return;
			}
		}
		if (shift == startIndex) // No match, just start over.
			startIndex = 0;
	}

	private void shiftNonePattern()
	{
		int shift = 1;
		for(; shift < noneIndex; shift++)
		{
			boolean match = true;
			for(int i = shift; i < noneIndex && match; i++)
				if (npr[i] != nonePattern[i-shift])
					match = false;

			if (match)
			{
				for(int i=shift; i<noneIndex; i++)
					npr[i-shift] = npr[i];
				noneIndex -= shift;
				return;
			}
		}
		if (shift == noneIndex) // No match, just start over.
			noneIndex = 0;
	}
	/**
	  Wait for 51-byte header to be available on the socket, then read
	  the header and convert it to a DOMSAT DcpMsg object.
	  Return true if header found, false if available input exhausted and
	  header is unfinished.
	  If the 51 byte header has syntax errors, state will be set back to HUNT.
	*/
	private boolean headerState()
		throws IOException
	{
		if (input.available() < 51)
			return false;
		int n = input.read(headerBuf, 4, 51);
		if (n != 51)
		{
			// This should not happen, we checked above for 51 bytes avail.
			log(Logger.E_WARNING, DrgsRecv.EVT_BAD_HEADER,
				"Socket error, 51 available but failed to read 51 -- skipped");
			state = HUNT_STATE;
			return false;
		}
log(Logger.E_DEBUG3, 0, "headerState: header='"
+ new String(headerBuf, 4, 51) + "'");
		try { workingMsg = parseHeader(headerBuf); }
		catch(BadHeader ex)
		{
			log(Logger.E_WARNING, 0, ex.toString());
			state = HUNT_STATE;
			return false;
		}
		state = MSGDATA_STATE;
		msgBytesRead = 0;
		return true;
	}

	/**
	  Parses the DAMS-NT header, constructing a corresponding DcpMsg object
	  for return. Returns null if parse error in the header.
	  Assumes that a complete 51-byte header has been passed.
	*/
	private DcpMsg parseHeader(byte[] buf)
		throws BadHeader
	{
//log(Logger.E_DEBUG3, 0, "parseHeader: header='"
//+ new String(buf, 4, 51) + "'");
		// Get length from DAMS-NT header & validate
		dataLength = 0;
		for(int i=0; i<5; i++)
		{
			// Change leading blanks to zero in the length field.
			byte digit = buf[50+i];
			if (dataLength == 0 && digit == (byte)' ')
				digit = (byte)'0';

			if (!Character.isDigit((char)digit))
				throw new BadHeader("Non-digit '" + (char)digit 
					+ "' in msg length field -- message skipped.");
			dataLength = (dataLength*10) + ((int)digit - (int)'0');
		}
		if (dataLength <= 0 || dataLength > 16000)
			throw new BadHeader("Invalid message length (" + dataLength
				+ ") -- message skipped.");

log(Logger.E_DEBUG3, 0, "parseHeader, dataLength=" + dataLength);
		byte[] domsatData = new byte[37 + dataLength];

		// DCP address
		for(int i=0; i<8; i++)
		{
            // Convert to uppercase -- SED 02/08/2006
			domsatData[i] = (byte)Character.toUpperCase((char)buf[42+i]);
			if (!ByteUtil.isHexChar(domsatData[i]))
				throw new BadHeader("Non hex-digit in DCP address field '"
				+ (char)domsatData[i] + "' -- message skipped.");
		}
		// Msg Start Time YYDDDHHMMSS
		for(int i=0; i<11; i++)
		{
			byte digit = buf[15+i];
			if (!Character.isDigit((char)digit))
				throw new BadHeader("Non-digit '" + (char)digit 
					+ "' in msg start-time field -- message skipped.");
			domsatData[8+i] = digit;
		}
		// Flag determines G or ? in DOMSAT header
		int f = (ByteUtil.fromHexChar((char)buf[32]) << 4)
			   + ByteUtil.fromHexChar((char)buf[33]);
		msgHasCarrierTimes = (f & DamsNt.CARRIER_TIMES) != 0;
		msgHasExtendedQual = (f & DamsNt.EXTENDED_QUAL) != 0;
		isBinaryMsg = (f & DamsNt.BINARY_MSG) != 0;

		// Both bits 0 and 3 have to be clear
		// bit 1 = parity errors, bit 3 = no EOT seen.
		int flagErrors = f & DamsNt.ANY_ERROR;
		if (flagErrors == 0)
			domsatData[19] = (byte)'G';
		else
		{
			if (flagErrors == DamsNt.NO_EOT
			 && LrgsConfig.instance().ignoreDrgsNoEotTermination)
				domsatData[19] = (byte)'G';
			else
				domsatData[19] = (byte)'?';
		}

		// Signal Strength
		domsatData[20] = buf[26];
		domsatData[21] = buf[27];

		// Frequency Offset
		domsatData[22] = buf[28];
		domsatData[23] = buf[29];

		// Modulation Index
		domsatData[24] = buf[30];

		// Data Quality Indicator
		domsatData[25] = buf[31];

		// GOES channel & spacecraft
		domsatData[26] = buf[7];
		domsatData[27] = buf[8];
		domsatData[28] = buf[9];
		domsatData[29] = buf[10];

		// Uplink Carrier Status (not in DAMS-NT
		domsatData[30] = sourceCode[0];
		domsatData[31] = sourceCode[1];

		// Copy length field -- we already know it's valid.
		for(int i=0; i<5; i++)
			domsatData[32+i] = buf[50+i];

//log(Logger.E_DEBUG1, 0, 
//"parseHeader domsatheader='" + new String(domsatData, 0, 37) + "'");
		// Make DcpMsg for return
		DcpMsg ret = new DcpMsg();
		ret.flagbits = 
			  DcpMsgFlag.MSG_PRESENT
			| DcpMsgFlag.SRC_DRGS 
			| DcpMsgFlag.MSG_NO_SEQNUM
			| DcpMsgFlag.HAS_CARRIER_TIMES
			| getMsgTypeFlag();
		ret.setData(domsatData);
		if (isBinaryMsg)
			ret.flagbits |= DcpMsgFlag.BINARY_MSG;

		for(int i=0; i<8; i++)
		{
			origAddr[i] = buf[34+i];
			if (origAddr[i] != buf[42+i])
				ret.flagbits |= DcpMsgFlag.ADDR_CORRECTED;
		}

		ret.setLocalReceiveTime(new Date());
		ret.setSeqFileName(null);
		ret.setDataSourceId(dataSourceId);
//log(Logger.E_INFORMATION,0,"dataSourceId = " + dataSourceId);

		// baud
		String bs = new String(buf, 11, 4);
		if (bs.startsWith("0")) bs = bs.substring(1);
		try { ret.setBaud(Integer.parseInt(bs)); }
		catch(NumberFormatException ex)
		{
			Logger.instance().warning(getInputName() 
				+ " Invalid baud rate '" +bs+ "': Assuming 300.");
			ret.setBaud(300);
		}

		if (!msgHasCarrierTimes)
			computeCarrierTimes(ret);

		return ret;
	}
	
	protected int getMsgTypeFlag()
	{
		return DcpMsgFlag.MSG_TYPE_GOES;
	}

	/**
	  Keep reading socket until all message data has been received.
	  If done, switch state to TERMCRLF_STATE and return true.
	  @return false if input exhausted and message not yet finished.
	*/
	private boolean msgDataState()
		throws IOException
	{
		int avail = input.available();
		if (avail > 0)
		{
			// Read # bytes left in msg or whatever is available now.
			int r = dataLength - msgBytesRead;
			if (r > avail)
				r = avail;
			byte[] data = workingMsg.getData();
			int n = input.read(data, 37+msgBytesRead, r);
			msgBytesRead += n;
			if (msgBytesRead >= dataLength)
			{
				// Strip parity bit from ascii data bytes.
				if (!isBinaryMsg)
					for(int i=0; i<dataLength; i++)
						data[37+i] &= 0x7f;

				state = TERMCRLF_STATE;
				return true;
			}
		}
		return false;
	}


	/**
	 * This state is entered when the header indicates that carrier
     * times are present. They should be in the form:<p>
	 * <pre>YYDDDHHMMSSmmm YYDDDHHMMSSmmm\r\n</pre>
	 * @return true if workingMsg is now finished and should be
	 * archived, false if still waiting for carrier time bytes.
	 */
	private boolean carrierTimesState()
		throws IOException
	{
		if (input.available() < 31) // length of carrier times
			return false;
		int n;
		for(n = 0; n < 31; n++ ) {
			input.read(carrierBuf, n, 1);
			if ( carrierBuf[n] == '\r' ) {
				break;
			}
		}
		if ( n != 29 )
		{
			log(Logger.E_WARNING, DrgsRecv.EVT_CARRIER_TIMES,
				"Bad date format on carrier times '" +
				(new String(carrierBuf)) + "'");
			state = HUNT_STATE;
			computeCarrierTimes(workingMsg);
			return true;
		}
		n++;
		input.read(carrierBuf,n,1);
		Date sd = parseCarrierTime(new String(carrierBuf, 0, 14));
		Date ed = parseCarrierTime(new String(carrierBuf, 15, 14));
		if ( sd == null || ed == null ) {
			log(Logger.E_WARNING, DrgsRecv.EVT_CARRIER_TIMES,
				"Bad date format on carrier times '" + 
				(new String(carrierBuf)) );
			computeCarrierTimes(workingMsg);
		} else {
			workingMsg.setCarrierStart(sd);
			workingMsg.setCarrierStop(ed);
		}
		state = msgHasExtendedQual ? EXTENDED_QUAL_STATE : HUNT_STATE;
		extQualIdx = 0;
		return true;
	}
	
	private boolean extendedQualState() 
		throws IOException
	{
		// Scan ahead to see if we have a complete line. If not, return false.
		int n = input.available();
		int c = 0;
		for(int idx = 0; idx < n && extQualIdx < extQualBuf.length; idx++)
		{
			c = input.read();
			if (c == '\n')
				break;
			if (c != '\r')
				extQualBuf[extQualIdx++] = (byte)c;
		}
		
		if (c == '\n') // means we have a complete line.
		{
			// Parse it.
			String line = new String(extQualBuf, 0, extQualIdx);
			String values[] = line.split(" ");
			String field = "slvl";
			try
			{
				field = "slvl";
				if (values.length >= 1)
					workingMsg.setGoesSignalStrength(Double.parseDouble(values[0]));
				field = "phns";
				if (values.length >= 2)
					workingMsg.setGoesPhaseNoise(Double.parseDouble(values[1]));
				field = "gdph";
				if (values.length >= 3)
					workingMsg.setGoesGoodPhasePct(Double.parseDouble(values[2]));
				field = "freq";
				if (values.length >= 4)
					workingMsg.setGoesFreqOffset(Double.parseDouble(values[3]));
				field = "type";
				if (values.length >= 5)
				{
					int typ = Integer.parseInt(values[4]);
					int f = 
						typ == 0 ? (DcpMsgFlag.BAUD_100|DcpMsgFlag.PLATFORM_TYPE_CS1) :
						typ == 1 ? (DcpMsgFlag.BAUD_300|DcpMsgFlag.PLATFORM_TYPE_CS1) :
						typ == 2 ? (DcpMsgFlag.BAUD_300|DcpMsgFlag.PLATFORM_TYPE_CS2) :
							       (DcpMsgFlag.BAUD_UNKNOWN|DcpMsgFlag.PLATFORM_TYPE_CS1);
					workingMsg.flagbits =
						(workingMsg.flagbits & ~(DcpMsgFlag.BAUD_MASK|DcpMsgFlag.PLATFORM_TYPE_MASK))
						| f;
				}
				field = "armf";
				if (values.length >= 6)
				{
					int armFlags = Integer.parseInt(values[5], 16);
					if ((armFlags & 0x01) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ADDR_CORRECTED;
					if ((armFlags & 0x02) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_UNCORRECTABLE_ADDR;
					if ((armFlags & 0x04) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_ADDR_NOT_IN_PDT;
					if ((armFlags & 0x08) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_PDT_INCOMPLETE;
					if ((armFlags & 0x10) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_TIMING_ERROR;
					if ((armFlags & 0x20) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_UNEXPECTED_MSG;
					if ((armFlags & 0x40) != 0)
						workingMsg.flagbits |= DcpMsgFlag.ARM_WRONG_CHANNEL;
				}
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(module + " Bad DAMS-NT extended quality line '" + line 
					+ "' in the " + field + " field: " + ex);
			}
			state = HUNT_STATE;
			extQualIdx = 0;
			return true;
		}
		else if (extQualIdx >= extQualBuf.length)
		{
			// Line too long. Issue warning and go into hunt mode
			Logger.instance().warning(module + " Extended quality line too long. Skipping.");
			state = HUNT_STATE;
			extQualIdx = 0;
			return true;
		}
		else // don't have a complete line yet. Stay in same state.
			return false;
	}


	
	/**
 	* A SimpleDateFormat object is not thread-safe and must be in a 
 	* synchronized object if used by multiple threads.
	* MJM - no need for synchronized because formatter is now an instance var.
	* This method is now just a wrapper to catch exceptions.
 	*/
	public Date parseCarrierTime(String ct)
		throws IOException
	{
		try 
		{
			Date d = carrierDateFmt.parse(ct);
			return(d);
		}
		catch(Exception ex)
		{
			return null;
		}
	}
	
	public String formatCarrierTime(Date d)
	{
		 return carrierDateFmt.format(d);
	}
	
	/**
	  Gobble the terminating CR/LF after the message, set the last response
	  time variable. Then go back to hunt state.
	  @return true if success, false if available input exhausted.
	*/
	private boolean termCrLfState()
		throws IOException
	{
		int avail = input.available();
		if (avail >= 2)
		{
			// This will apply to a real msg or to a NONE response:
			lastResponseTime = System.currentTimeMillis();

			byte cr = (byte)input.read();
			byte lf = (byte)input.read();
			if (cr != (byte)'\r' || lf != (byte)'\n')
			{
				log(Logger.E_DEBUG2, 0,
					"Improper terminating sequence, expected 0x0D0A, got 0x"
					+ ByteUtil.toHexChar(cr) + ByteUtil.toHexChar(lf));
				state = HUNT_STATE;
			}
			else
			{
				if (wasNone)
				{
					state = HUNT_STATE;
				}
				else
					state = 
						msgHasCarrierTimes ? CARRIERTIMES_STATE : HUNT_STATE;
			}
if (state == CARRIERTIMES_STATE)
log(Logger.E_DEBUG2, 0, "Got term CRLF, entering CARRIERTIMES_STATE");

			return true;
		}
		return false;
	}

	/** Prints a log message with a host/port prefix. */
	protected void log(int level, int evtNum, String text)
	{
		Logger.instance().log(level, module
			+ (evtNum == 0 ? "" : (":" + evtNum + "-"))
			+ " " + getName() + ": " + text);
	}

	/** Returns string of the form "DRGS:host:port". */
	public String getName()
	{
		return myName;
	}


//	/** This method is for debug only, dumps the message to System.out. */
//	private void dumpMsg(DcpMsg msg)
//	{
//		java.io.PrintStream out = System.out;
//		out.println("--------------------");
//		out.println("flag = 0x" + Integer.toHexString(msg.flagbits));
//		out.println("time = " + 
//			IDateFormat.toString(msg.getLocalReceiveTime(), false));
//		out.println(msg.toString() + "\n\n");
//	}

	//=================================================================
	// The following methods are from the LrgsInputInterface interface
	//=================================================================

	/**
	 * @return the type of this input interface.
	 */
	public int getType()
	{
		return myType;
	}

	/**
	 * All inputs must keep track of their 'slot', which is a unique index
	 * into the LrgsMain's vector of all input interfaces.
	 * @param slot the slot number.
	 */
	public void setSlot(int slot)
	{
		mySlot = slot;
	}
	
	/** @return the slot number that this interface was given at startup */
	public int getSlot() { return mySlot; }

	/**
	 * @return the name of this interface.
	 */
	public String getInputName()
	{
		return getName();
	}

	/**
	 * Initializes the interface.
	 * May throw LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException
	{
		// Don't need to do anything here: Init handled as part of the
		// config function.
	}

	/**
	 * Shuts down the interface.
	 * Any errors encountered should be handled within this method.
	 */
	public void shutdownLrgsInput()
	{
		_isShutdown = true;
	}

	public void shutdown()
	{
		shutdownLrgsInput();
	}

	public boolean isShutdown() { return _isShutdown; }
	
	/**
	 * Enable or Disable the interface. 
	 * The interface should only attempt to archive messages when enabled.
	 * @param enabled true if the interface is to be enabled, false if disabled.
	 */
	public void enableLrgsInput(boolean enabled)
	{
		this._enabled = enabled;
		Logger.instance().info(module + " " + myName + " Enabled set to " + enabled);
	}

	/**
	 * @return true if this downlink can report a Bit Error Rate.
	 */
	public boolean hasBER() { return false; }

	/**
	 * @return the Bit Error Rate as a string.
	 */
	public String getBER() { return ""; }

	/**
	 * @return true if this downlink assigns a sequence number to each msg.
	 */
	public boolean hasSequenceNums()
	{
		return true;
	}

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode()
	{
		return DL_STRSTAT;
	}
	
	
	private static final long MS_PER_HR = 3600*1000L;

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus()
	{
		long now = System.currentTimeMillis();
		
		if (now/MS_PER_HR > lastStatusTime/MS_PER_HR)  // Hour just changed
		{
			String s = 
				(this instanceof lrgs.lrit.LritDamsNtReceiver) ? "lritMinHourly" : "drgsMinHourly";
				
Logger.instance().debug3("Looking for property '" + s + "'");

			int minHourly = 
				(this instanceof lrgs.lrit.LritDamsNtReceiver) ? 
					LrgsConfig.instance().lritMinHourly : LrgsConfig.instance().drgsMinHourly;
			if (minHourly > 0                               // Feature Enabled
			 && isConnected()
			 && (now - lastConnectTime > 3*MS_PER_HR))      // Have been up for at least 3 hours
			{
				if (numThisHour < minHourly)
				{
					Logger.instance().warning(module + " " + getInputName()
						+ " for hour ending " + new Date((now / MS_PER_HR) * MS_PER_HR)
						+ " number of messages received=" + numThisHour 
						+ " which is under minimum threshold of " + minHourly);
				}
				if (numThisHour < (numLastHour/2))
				{
					Logger.instance().warning(module + " " + getInputName()
						+ " for hour ending " + new Date((now / MS_PER_HR) * MS_PER_HR)
						+ " number of messages received=" + numThisHour 
						+ " which is under half previous hour's total of " + numLastHour);
				}
			}

			// Rollover the counts.
			numLastHour = numThisHour;
			numThisHour = 0;
		}
		
		
		lastStatusTime = now;
		return status;
	}


	/**
	 * This method is called if the DRGS does not provide measured
	 * carrier times. It estimates carrier times based on assumptions
	 * about the baud rate and the GOES time stamp.
	 */
	private void computeCarrierTimes(DcpMsg msg)
	{
		long durationMsec = 
			(msg.getDcpDataLength() * 8 * 1000L) / msg.getBaud();

		// Figure the overhead.
		long overheadMsec = (msg.getBaud() == 100) ? 1750 : 
			(msg.getBaud() == 1200) ? 550 : 950;

		Date d = msg.getDapsTime();
		long t = d.getTime() - (overheadMsec/2);
		msg.setCarrierStart(new Date(t));
		msg.setCarrierStop(new Date(t + durationMsec + overheadMsec));
		msg.flagbits |= DcpMsgFlag.CARRIER_TIME_EST;
	}


	private void loadChannels(String cfgFileName)
	{
		if (cfgFileName == null || cfgFileName.length() == 0)
			cfgFileName = "$LRGSHOME/" + myName + ".cfg";
		cfgFile = new File(EnvExpander.expand(cfgFileName));
		if (!cfgFile.canRead())
			log(Logger.E_DEBUG1, DrgsRecv.EVT_BAD_CONFIG,
				"Cannot read channel config '" + cfgFileName + "'");
		Vector chanvec = new Vector();
		BufferedReader br = null;
		try
		{
			br = new BufferedReader(new FileReader(cfgFile));
			String line;
			while( (line = br.readLine()) != null)
			{
				line = line.trim().toLowerCase();
				if (line.startsWith("assign"))
				{
					StringTokenizer st = new StringTokenizer(line);
					st.nextToken();
					if (!st.hasMoreTokens()) continue;
					st.nextToken();
					if (!st.hasMoreTokens()) continue;
					String cs = st.nextToken();
					try
					{
						int chan = Integer.parseInt(cs);
						if (chan > 0)
							chanvec.add(new Integer(chan));
					}
					catch(Exception ex) {}
				}
			}
			br.close();
			br = null;
			synchronized(cfgFile)
			{
				chanArray = new int[chanvec.size()];
				for(int i=0; i<chanArray.length; i++)
					chanArray[i] = ((Integer)chanvec.get(i)).intValue();
			}
			Logger.instance().debug1("DRGS " + getName() + " monitoring "
				+ chanArray.length + " channels: ");
			for(int i=0; i<chanArray.length; i++)
				Logger.instance().debug1("" + chanArray[i]);
					
		}
		catch(IOException ex)
		{
			log(Logger.E_DEBUG1, DrgsRecv.EVT_BAD_CONFIG,
				"Error reading channel config '" + cfgFileName + "': " + ex);
			if (br != null)
			{
				try { br.close(); } catch(Exception ex2) {}
			}
		}
	}
	
	protected int getNextSeqNum()
	{
		seqNum = (seqNum + 1) % 100000;
		return seqNum;
	}

	public int[] getChanArray()
	{
		synchronized(cfgFile)
		{
			int ln = chanArray == null ? 0 : chanArray.length;
			int ret[] = new int[ln];
			for(int i=0; i<ln; i++)
				ret[i] = chanArray[i];
			return ret;
		}
	}

	public void disconnect()
	{
		super.disconnect(); // disconnect the socket.
		if (channelOutage == null && isEnabled())
		{
			channelOutage = new Outage();
			channelOutage.setOutageType(LrgsConstants.damsntOutageType);
			channelOutage.setBeginTime( new Date(lastResponseTime) );
			channelOutage.setSourceId( getDataSourceId() );
			LrgsDatabaseThread.instance().assertOutage(channelOutage);
		}
	}

//	private void doPoll()
//	{
//		try { output.write(crlf); }
//		catch(IOException ex)
//		{
//			Logger.instance().warning("IO Error writing DRGS cr/lf poll: " + ex);
//		}
//	}
	
	/** @return DRGS never receives APR messages */
	public boolean getsAPRMessages() { return false; }
	
	public boolean isEnabled() { return _enabled; }

	@Override
	public String getGroup() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Test main program for parsing data captured from a file.
	 * @param args
	 */
	public static void main(String args[])
		throws Exception
	{
		Logger.setLogger(new StderrLogger("DAMS-NT-Test"));
		Logger.instance().setMinLogPriority(Logger.E_DEBUG3);
		DrgsRecvMsgThread me = new DrgsRecvMsgThread(null, null);
		me.testFileInput(args[0]);
		
	}
	
	private void testFileInput(String filename) throws Exception
	{
		this.input = new FileInputStream(filename);
		DcpMsg msg = null;
		while((msg = getMsg()) != null)
		{
			System.out.println("=====================");
			System.out.println("Message from " + msg.getDcpAddress());
			System.out.println("" + new String(msg.getData()));
		}
	}
}

class BadHeader extends Exception
{
	public BadHeader(String s) { super(s); }
}

