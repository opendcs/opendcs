package lrgs.drgsrecv;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;

import ilex.net.BasicClient;
import ilex.util.AsciiUtil;
import ilex.util.ByteUtil;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;

import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.drgsrecv.DamsNt;

import decodes.util.ChannelMap;

/**
Stand alone program to generate DRGS connection statistics.
*/
public class DrgsStats
	extends BasicClient
{
	private byte[] startPattern;
	static byte[] nonePattern = { (byte)'N', (byte)'O', (byte)'N', (byte)'E' };
	private boolean enabled;
	private long lastResponseTime;

	// States for reading data from the socket:
	public static final int HUNT_STATE = 0;
	public static final int HEADER_STATE = 1;
	public static final int MSGDATA_STATE = 2;
	public static final int TERMCRLF_STATE = 3;
	public static final int CARRIERTIMES_STATE = 4;
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

	private String status;
	private String myName;
	private static final int SEC_PER_DAY = 86400;

	private File cfgFile; // Configuration file containing chan assigns.
	private int chanArray[];

	/** Original address for last message received. */
	private byte[] origAddr;

	private byte sourceCode[];

	private static SimpleDateFormat domsatDateFmt
		= new SimpleDateFormat("yyDDDHHmmss");

	private static SimpleDateFormat carrierDateFmt
		= new SimpleDateFormat("yyDDDHHmmssSSS");

	private boolean msgHasCarrierTimes;
	private boolean isBinaryMsg;
	private byte[] carrierBuf = new byte[31];
	private boolean normalTerm = true;

	private static final long DRGS_TIMEOUT_MS = 180000L;

	private static NumberFormat lenFormat = NumberFormat.getIntegerInstance();
	static
	{
		domsatDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		carrierDateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
		lenFormat.setMinimumIntegerDigits(5);
		lenFormat.setGroupingUsed(false);
		IDateFormat.alwaysIncludeSeconds = true;
	}

	/**
	  Constructor.
	*/
	public DrgsStats()
	{
		super("", 17010);
		startPattern = new byte[] { (byte)'S', (byte)'M', (byte)'\r',
			 (byte)'\n' };
		enabled = true;
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
		lastResponseTime = 0L;
		status = "initializing";
		myName = "DRGS";
		origAddr = new byte[8];
		chanArray = new int[0];
		sourceCode = new byte[2];
		sourceCode[0] = (byte)'D';
		sourceCode[1] = (byte)'R';
	}

	/**
	  Thread run method
	*/
	public void run()
	{
		try{ Thread.sleep(2000L); }
		catch(InterruptedException ex) {}

		lastResponseTime = System.currentTimeMillis();

		int lastMin = -1;
		while(true)
		{
			long now = System.currentTimeMillis();

			if (!isConnected() && now - getLastConnectAttempt() > 10000L)
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
						collectStats(msg);
					else if (now - lastResponseTime > DRGS_TIMEOUT_MS)
					{
						// too many seconds since either msg or NONE.
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
	}

	/**
	  Called from parent when this connection has been reconfigured.
	  @param the connection configuration for this DRGS
	*/
	public void configure(DrgsConnectCfg cfg)
	{
		for(int i=0; i<4; i++)
			startPattern[i] = cfg.startPattern[i];

		if (!getHost().equalsIgnoreCase(cfg.host) || getPort() != cfg.msgPort)
		{
			setHost(cfg.host);
			setPort(cfg.msgPort);
		}

		myName = "DRGS:" + (cfg.name == null ? cfg.host : cfg.name);

		if (cfg.drgsSourceCode != null && cfg.drgsSourceCode.length >= 2)
		{
			sourceCode[0] = cfg.drgsSourceCode[0];
			sourceCode[1] = cfg.drgsSourceCode[1];
		}
	}

	private void tryConnect()
	{
		log(Logger.E_DEBUG1, 0, "Attempting connection.");
		status = "Connecting";
		try { connect(); }
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
		lastResponseTime = System.currentTimeMillis();
	}

	/**
	  Walk through the parser-states and return a DCP message when one is
	  completed. Return null if no complete message available at this time.
	  @return DcpMsg or null if no messge available.
	*/
	private DcpMsg getMsg()
		throws IOException
	{
		boolean stateComplete = true;
		while(stateComplete)
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
					return workingMsg;
			}
			if (state == CARRIERTIMES_STATE)
			{
				stateComplete = carrierTimesState();
				if (stateComplete)
					return workingMsg;
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
		if (input.available() <= 0)
			return false;
		while (input.available() > 0)
		{
			byte c = (byte)input.read();

			// Alert user if we read excessive amounts of data with no sync.
			if ((++totalReadBeforeSync % 100) == 0)
				log(Logger.E_DEBUG1, 0, "Skipped "
					+ totalReadBeforeSync + " bytes looking for sync.");

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
//log(Logger.E_DEBUG3, 0, "headerState: header='"
//+ new String(headerBuf, 4, 51) + "'");
		try { workingMsg = parseHeader(headerBuf); }
		catch(xBadHeader ex)
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
		throws xBadHeader
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
				throw new xBadHeader("Non-digit '" + (char)digit 
					+ "' in msg length field -- message skipped.");
			dataLength = (dataLength*10) + ((int)digit - (int)'0');
		}
		if (dataLength <= 0 || dataLength > 16000)
			throw new xBadHeader("Invalid message length (" + dataLength
				+ ") -- message skipped.");

//log(Logger.E_DEBUG3, 0, "parseHeader, dataLength=" + dataLength);
		byte[] domsatData = new byte[37 + dataLength];

		// DCP address
		for(int i=0; i<8; i++)
		{
            // Convert to uppercase -- SED 02/08/2006
			domsatData[i] = (byte)Character.toUpperCase((char)buf[42+i]);
			if (!ByteUtil.isHexChar(domsatData[i]))
				throw new xBadHeader("Non hex-digit in DCP address field '"
				+ (char)domsatData[i] + "' -- message skipped.");
		}
		// Msg Start Time YYDDDHHMMSS
		for(int i=0; i<11; i++)
		{
			byte digit = buf[15+i];
			if (!Character.isDigit((char)digit))
				throw new xBadHeader("Non-digit '" + (char)digit 
					+ "' in msg start-time field -- message skipped.");
			domsatData[8+i] = digit;
		}
		// Flag determines G or ? in DOMSAT header
		int f = (ByteUtil.fromHexChar((char)buf[32]) << 4)
			   + ByteUtil.fromHexChar((char)buf[33]);
		msgHasCarrierTimes = (f & DamsNt.CARRIER_TIMES) != 0;
		isBinaryMsg = (f & DamsNt.BINARY_MSG) != 0;

		// Both bits 0 and 3 have to be clear
		// bit 1 = parity errors, bit 3 = no EOT seen.
		int flagErrors = f & DamsNt.ANY_ERROR;
		normalTerm = true;
		if (flagErrors == 0)
			domsatData[19] = (byte)'G';
		else
		{
			domsatData[19] = (byte)'?';
			if ((flagErrors & DamsNt.NO_EOT) != 0)
				normalTerm = false;
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
			| DcpMsgFlag.MSG_NO_SEQNUM;
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
//log(Logger.E_INFORMATION,0,"dataSourceId = " + dataSourceId);

		// baud
		String bs = new String(buf, 11, 4);
		if (bs.startsWith("0")) bs = bs.substring(1);
		try { ret.setBaud(Integer.parseInt(bs)); }
		catch(NumberFormatException ex)
		{
//			Logger.instance().warning(getInputName() 
//				+ " Invalid baud rate '" +bs+ "': Attempting channel lookup.");
//			String cs = new String(buf, 7, 3);
//			try { ret.setBaud(channelMap.getBaud(Integer.parseInt(cs))); }
//			catch(NumberFormatException ex2)
//			{
//				Logger.instance().warning(getInputName() 
//					+ " Invalid channel '" + cs + "': Setting baud to 300.");
//				ret.setBaud(300);
//			}
		}

		if (!msgHasCarrierTimes)
			computeCarrierTimes(ret);

		return ret;
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
			int n = input.read(workingMsg.getData(), 37+msgBytesRead, r);
			msgBytesRead += n;
			if (msgBytesRead >= dataLength)
			{
				// Strip parity bit from ascii data bytes.
				if (!isBinaryMsg)
					for(int i=0; i<dataLength; i++)
						workingMsg.getData()[37+i] &= 0x7f;

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
		int n = input.read(carrierBuf, 0, 31);
		if (n != 31)
		{
			// This should not happen, we checked above for 31 bytes avail.
			log(Logger.E_WARNING, DrgsRecv.EVT_CARRIER_TIMES,
				"Socket error, 31 available but failed to read 31 -- skipped");
			state = HUNT_STATE;
			computeCarrierTimes(workingMsg);
			return false;
		}
		
		try
		{
			Date sd = carrierDateFmt.parse(new String(carrierBuf, 0, 14));
			workingMsg.setCarrierStart(sd);
			Date ed = carrierDateFmt.parse(new String(carrierBuf, 15, 14));
			workingMsg.setCarrierStop(ed);
//log(Logger.E_INFORMATION, 0, "carrierTimes='" + (new String(carrierBuf))
//+ "' start='" + carrierDateFmt.format(sd)
//+ "' end='" + carrierDateFmt.format(ed) + "'");
		}
		catch(ParseException ex)
		{
			log(Logger.E_WARNING, DrgsRecv.EVT_CARRIER_TIMES,
				"Bad date format on carrier times '" + 
				(new String(carrierBuf)) + "': " + ex);
			computeCarrierTimes(workingMsg);
		}
		state = HUNT_STATE;
		return true;
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
				state = msgHasCarrierTimes ?  CARRIERTIMES_STATE : HUNT_STATE;
if (state == CARRIERTIMES_STATE)
log(Logger.E_DEBUG2, 0, "Got term CRLF, entering CARRIERTIMES_STATE");

			return true;
		}
		return false;
	}

	/** Prints a log message with a host/port prefix. */
	private void log(int level, int evtNum, String text)
	{
		Logger.instance().log(level, DrgsRecv.module
			+ (evtNum == 0 ? "" : (":" + evtNum + "-"))
			+ " " + getName() + ": " + text);
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
		msg.setCarrierStart(new Date(d.getTime() - (overheadMsec/2)));
		msg.setCarrierStop(
			new Date(msg.getCarrierStart().getTime()
				+ durationMsec + overheadMsec));
		msg.flagbits |= DcpMsgFlag.CARRIER_TIME_EST;
	}

	/**
	 * @return String in the format cccS, where ccc is 3-digit channel number
	 * and S is the spacecraft designator.
	 */
	private String fmtChan(int chan)
	{
		byte b[] = new byte[4];
		b[0] = (byte)((int)'0' + chan / 100);
		chan %= 100;
		b[1] = (byte)((int)'0' + chan / 10);
		b[2] = (byte)((int)'0' + chan % 10);
		b[3] = (chan/2 == 1 ? (byte)'E' : (byte)'W');
		return new String(b);
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


	static private char sep = '\t';

	/**
	 Usage: prognam host port DRGSName startpatt
	*/
	public static void main(String args[])
		throws Exception
	{
		DrgsStats drgsStats = new DrgsStats();
		DrgsConnectCfg cfg = new DrgsConnectCfg(0, args[0]);
		// Fill in config stuff here...
		cfg.msgPort = Integer.parseInt(args[1]);
		cfg.name = args[2];
		cfg.startPattern = ByteUtil.fromHexString(args[3]);
		drgsStats.configure(cfg);
		System.out.println(
			"Addr" + sep + "Timestamp" + sep + "Length" + sep + "Type" + sep 
			+ "FailCode" + sep + "NormalTerm" + sep + "Chan" + sep
			+ "Baud" + sep + "SigStr" + sep + "FreqOff" 
			+ sep + "ModIdx" + sep + "1sParPos" + sep + "MaxPbRun" + sep 
			+ "NumAsc" + sep + "TruncMsg");
		drgsStats.run();
	}

	public void collectStats(DcpMsg msg)
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			int firstParPos = 0;
			char fc = msg.getFailureCode();
			int msgLen = msg.getDcpDataLength();
			byte msgData[] = msg.getDcpData();
			if (fc == '?')
				for(firstParPos = 0; firstParPos<msgLen; firstParPos++)
				{
					if (msgData[firstParPos] == (byte)'$')
						break;
				}
			String asciiMsg = AsciiUtil.bin2ascii(msgData);
			String truncMsg = (asciiMsg.length() < 50) ? asciiMsg 
				: asciiMsg.substring(0,50);

			sb.append(msg.getDcpAddress().toString() + sep);
			sb.append(new String(msg.getField(msg.IDX_YEAR, 11)) + sep);
			sb.append("" + msgLen + sep);
			sb.append("" + determineType(msg) + sep);
			sb.append("" + fc + sep);
			sb.append("" + (normalTerm ? 'Y' : 'N') + sep);
			sb.append("" + msg.getGoesChannel() + sep);
			sb.append("" + msg.getBaud() + sep);
			sb.append("" + msg.getSignalStrength() + sep);
			sb.append("" + msg.getFrequencyOffset() + sep);
			sb.append("" + msg.getModulationIndex() + sep);
			sb.append("" + firstParPos + sep);
			sb.append("" + maxPbRun + sep);
			sb.append("" + numAscii + sep);
			sb.append("" + truncMsg);
			System.out.println(sb.toString());
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error processing msg: " + ex);
		}
	}

	private double computeCompressionRatio(DcpMsg msg)
	{
		byte[] data = msg.getDcpData();
		if (data.length == 0)
			return 0.0;

		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			gzos.write(data);
			gzos.finish();
			gzos.close();
			int complen = baos.size();
			return (double)complen / (double)data.length;
		}
		catch(IOException ex)
		{
			Logger.instance().warning("Error compressing: " + ex);
			return 0.0;
		}
	}

	// Lower & upper range of PB chars
	public static final int pbLow = (int)0x3f;
	public static final int pbHigh = (int)0x7e;

	// Chars that indicate ASCII and are not possible in PB
	String asciiIndicators = " #:0123456789+-";
	int maxPbRun = 0;
	int numAscii = 0;

	private char determineType(DcpMsg msg)
	{
		byte[] data = msg.getDcpData();
		if (data.length == 0)
			return 'U';
		
		maxPbRun = 0;
		numAscii = 0;
		int pbRun = 0;
		for(int i=0; i<data.length; i++)
		{
			int x = (int)data[i];
			if (x >= pbLow && x <= pbHigh)
				if (++pbRun > maxPbRun)
					maxPbRun = pbRun;

			char c = (char)data[i];
			if (asciiIndicators.indexOf(c) >= 0)
				numAscii++;
		}
		boolean isPB = maxPbRun >= data.length-2 || maxPbRun > 10;
		boolean isAscii = numAscii > 0;
		return isPB ? (isAscii ? 'H' : 'P') : (isAscii ? 'A' : 'U');
	}
}

class xBadHeader extends Exception
{
	public xBadHeader(String s) { super(s); }
}
