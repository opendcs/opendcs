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
package lrgs.domsatrecv;

import java.io.File;
import java.util.Date;

import ilex.util.ByteUtil;
import ilex.util.Logger;
import ilex.util.EnvExpander;

import lrgs.common.BadConfigException;
import lrgs.common.DcpAddress;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.JavaLrgsStatusProvider;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.archive.MsgArchive;
import lrgs.drgs.DrgsInputSettings;
import lrgs.db.LrgsConstants;
import lrgs.db.LrgsDatabaseThread;
import lrgs.db.Outage;

/** Main class for the LRGS-DOMSAT Input Process. */
public class DomsatRecv
	extends Thread
	implements LrgsInputInterface
{
	/** Module name for log messages. */
	public static final String module = "DomsatRecv";

	/** Event number meaning hardware init problem */
	public static final int EVT_HW_INIT_FAILED = 1;

	/** Event number meaning could not enable. */
	public static final int EVT_HW_CANNOT_ENABLE = 2;

	/** Event number meaning hardware problem during read. */
	public static final int EVT_HW_READ_FAILED = 3;

	/** Event number meaning hardware returned bad packet. */
	public static final int EVT_HW_BAD_PACKET = 4;

	/** Event number meaning hardware timeout . */
	public static final int EVT_HW_TIMEOUT = 5;

	/** Event number meaning hardware timeout . */
	public static final int EVT_DOMSAT_SEQ = 6;

	/** We check for config changes this often. */
	private static final long cfgCheckTime = 5000L;

	/** LrgsMain provides links to other modules. */
	private LrgsMain lrgsMain;

	/** Messages are archived here. */
	private MsgArchive msgArchive;

	/** Last time that settings were read, used to detect changes. */
	private long lastConfigRead;

	/** shutdown flag. */
	private boolean isShutdown;

	/** current status code (see LrgsInputInterface) */
	private int statusCode;

	/** Explanatory status string */
	private String status;

	/** My slot */
	private int slot;

	/** enable flag */
	public boolean isEnabled; 

	/** Interface to the DOMSAT Hardware. */
	public DomsatHardware domsatHardware;

	/** Used to read a single packet from the hardware. */
	private byte[] packetbuf;

	/** Used to build the entire message. */
	private byte[] msgbuf;

	/** Current amount of data waiting in msgbuf. */
	private int msgbuflen;

	/** Msg sequence number of last frame received. */
	private int lastMsgSeq;

	/** Pkt sequence number of last frame received. */
	private int lastPktSeq;

	/** Time that last packet was received from hardware. */
	private long lastPktRecvTime;

	/** Last complete DOMSAT message received (for msg sequence checking) */
	private int lastCompleteMsgSeq;

	/** Time that last complete DOMSAT message received  */
	private long lastCompleteMsgTime;

	/** Buffer in which to construct complete messages. */
	private byte[] msgBuf;

	/** Current # bytes in the buffer. */
	private int msgBufLen;

	/** The status gatherer */
	private JavaLrgsStatusProvider statusProvider;

	/** The data source ID that I should use. */
	private int dataSourceId;

	/** Asserted when we are in a timeout situation. */
	private Outage currentRtOutage = null;
	
	private long lastTimeoutTweek = 0L;

	/**
	  Constructor called from main.
	*/
	public DomsatRecv(LrgsMain lrgsMain, MsgArchive msgArchive,
		JavaLrgsStatusProvider statusProvider)
	{
		this.lrgsMain = lrgsMain;
		this.msgArchive = msgArchive;
		this.statusProvider = statusProvider;
		lastConfigRead = 0L;
		isShutdown = false;
		statusCode = DL_INIT;
		status = "Initializing";
		slot = -1;
		isEnabled = false;

		// MJM 2006 09/23 - Franklin interface will always return an entire
		// message as a single packet. Therefore packetbuf must be able to
		// accomodate the largest possible message.
		packetbuf = new byte[16000];
		// packetbuf = new byte[512];
		msgbuf = new byte[20000];
		msgbuflen = 0;
		lastMsgSeq = -1;
		lastPktSeq = -1;
		lastCompleteMsgSeq = -1;
		lastPktRecvTime = 0L;
		msgBuf = new byte[20000];
		msgBufLen = 0;
		lastCompleteMsgTime = 0L;
		Logger.instance().debug3(module + " created.");
		String clsname = LrgsConfig.instance().domsatClass;
		try
		{
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			Class domsatClass = cl.loadClass(clsname);
			domsatHardware = (DomsatHardware)domsatClass.newInstance();
		}
		catch(Exception ex)
		{
			Logger.instance().failure(module + ":" + EVT_HW_INIT_FAILED
				+ " Cannot load domsat interface from class name '"
				+ clsname + "': " + ex);
			domsatHardware = new DomsatSangoma();
		}
		dataSourceId = 
			lrgsMain.getDbThread().getDataSourceId(DL_DOMSAT_TYPESTR, "DOMSAT");
	}

	/** Main thread run method. */
	public void run()
	{
		Logger.instance().info(module + " starting.");
		//checkConfig();

		statusCode = DL_ACTIVE;
		status = "Active";

		long lastCfgCheck = 0L;
		lastPktRecvTime = System.currentTimeMillis();
		while(!isShutdown)
		{
			if (System.currentTimeMillis() - lastCfgCheck > cfgCheckTime)
			{
				checkConfig();
				lastCfgCheck = System.currentTimeMillis();
			}
			if (!isEnabled)
			{
				try { sleep(1000L); } catch(InterruptedException ex) {}
				continue;
			}
			long now = System.currentTimeMillis();

			boolean pause = false;
			int n = domsatHardware.getPacket(packetbuf);
/*
System.out.println(
"  java: len=" + n + "  "
+ Integer.toHexString((int)packetbuf[0] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[1] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[2] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[3] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[4] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[5] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[6] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[7] & 0xff) + ", ... "
+ Integer.toHexString((int)packetbuf[n-2] & 0xff) + ", "
+ Integer.toHexString((int)packetbuf[n-1] & 0xff));
*/
			if (n == -2)
			{
				Logger.instance().failure(module + ":" + EVT_HW_READ_FAILED
					+ "- " + domsatHardware.getErrorMsg());
				status = "HWErr";
				pause = true;
			}
			else if (n == -1)
			{
				Logger.instance().warning(module
					+ " Bad Frame: " + domsatHardware.getErrorMsg());
			}
			else if (n == -3)
			{
				// Do nothing - this means pause & try again later.
				pause = true;
			}
			else if (n == 0)
			{
				Logger.instance().debug1(module
					+ " Short Frame: " + domsatHardware.getErrorMsg());
				pause = true;
			}
			else if (n < 8)
			{
				Logger.instance().warning(module
					+ " Ignoring too-small frame (" + n + " bytes)");
			}
			else // n >= 8
			{
				lastPktRecvTime = now;
				status = "Active";
				if (statusCode != DL_ACTIVE)
				{
					if (statusCode == DL_TIMEOUT)
						Logger.instance().info(module+":"+(-EVT_HW_TIMEOUT)
							+ " DOMSAT Link Recovered.");
					statusCode = DL_ACTIVE;
					if (currentRtOutage != null)
					{
						currentRtOutage.setEndTime(new Date(now));
						currentRtOutage = null;
					}
				}
				processPacket(packetbuf, n);
			}

			if (statusCode == DL_ACTIVE
			 && now - lastPktRecvTime > 
				(LrgsConfig.instance().domsatTimeout * 1000L))
			{
				statusCode = DL_TIMEOUT;
				status = "Timeout";
				lastTimeoutTweek = now;
				Logger.instance().warning(module + ":" + EVT_HW_TIMEOUT
					+ " No data in more than 60 seconds.");

				currentRtOutage = new Outage();
				currentRtOutage.setOutageType(LrgsConstants.realTimeOutageType);
				currentRtOutage.setBeginTime(new Date(lastPktRecvTime - 5000L));
				LrgsDatabaseThread.instance().assertOutage(currentRtOutage);
				domsatHardware.timeout();
			}
			else if (statusCode == DL_TIMEOUT
			 && now - lastTimeoutTweek > 60000L)
			{
				lastTimeoutTweek = now;
				Logger.instance().info("DOMSAT still in timeout.");
				domsatHardware.timeout();
			}

			if (pause)
			{
				try { Thread.sleep(50L); }
				catch(InterruptedException ex) {}
			}
		}

		domsatHardware.shutdown();
		statusCode = DL_DISABLED;
		status = "Shutdown";
	}

	/**
	 * Check the configuration file to see if it has changed. If so, reload
	 * it and put the changes into effect.
	 */
	private void checkConfig()
	{
		Logger.instance().debug3(module + " checkConfig");

		if (isEnabled != LrgsConfig.instance().enableDomsatRecv)
			do_enableLrgsInput(LrgsConfig.instance().enableDomsatRecv);

	}

	/**
	 * Process the passed packet.
	 * @param packet the packet.
	 * @param len the length of the packet.
	 */
	private void processPacket(byte[] packet, int len)
	{
		int msgseq = 
			((((int)packet[5]) & 0xff) << 8) 
		   | (((int)packet[6]) & 0xff);
		int pktseq = packet[7] & 0x7F;

		if (msgseq == lastMsgSeq) // Continuation of previous message?
		{
			if (pktseq == lastPktSeq + 1)
			{
				addToBuf(packet, len, msgseq);
				lastPktSeq = pktseq;
			}
			else
			{
				// MJM - reduced this to debug, because it always leads to
				// a msg seq error in the log. This is too much noise.
				Logger.instance().debug1(
					module + " Packet Sequence Error, got "
					+ msgseq + ":" + pktseq + " expected " + msgseq
					+ ":" + (lastPktSeq+1) + " -- discarded.");
				lastPktSeq = 0;
			}
		}
		else // this is start of new message.
		{
			flushBuf();  // In case buffer not empty, flush it.

			if (pktseq == 1)
			{
				addToBuf(packet, len, msgseq);
				lastPktSeq = pktseq;
			}
			else
			{
				Logger.instance().warning(module
					+ " Bad start packet, got "
					+ msgseq + ":" + pktseq + " expected " + msgseq
					+ ":1 -- discarded.");
				lastPktSeq = 0;
			}
		}

		lastMsgSeq = msgseq;
	}

	private void addToBuf(byte[] packet, int len, int msgseq)
	{
		for(int i=8; i<len; i++)
			msgBuf[msgBufLen++] = packet[i];

		boolean more = (packet[4] & 0x10) != 0;
		if (!more)
		{
			if (msgBufLen < DcpMsg.IDX_DATA)
			{
				Logger.instance().warning(module 
					+ " too-short-message, length=" + msgBufLen
					+ " " + ByteUtil.toHexAsciiString(msgBuf, 0, msgBufLen)
					+ " -- ignored.");
				msgBufLen = 0;
				return;
			}
			DcpMsg msg = new DcpMsg(msgBuf, msgBufLen);
			msg.setSequenceNum(msgseq);
			msg.flagbits = DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.SRC_DOMSAT;
			msg.setDomsatTime(new Date());
			msg.setDataSourceId(dataSourceId);
			long addr = 0x0000;
			try {addr = msg.getDcpAddress().getAddr();}
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(module 
					+ " msg with bad dcpaddr -- ignored.");
			}
			char fc = msg.getFailureCode();
			boolean isARM = fc != 'G' && fc != '?';
			if (!isARM || LrgsConfig.instance().acceptDomsatARMs)
				msgArchive.archiveMsg(msg, this);
			checkDomsatSeq(msgseq, addr);
			msgBufLen = 0;
		}
	}

	private void flushBuf()
	{
		if (msgBufLen > 0)
		{
			Logger.instance().debug1(module + " discarding " + msgBufLen
				+ " bytes.");
			msgBufLen = 0;
		}
	}

	private void checkDomsatSeq(int msgseq, long dcpaddr)
	{
		long now = System.currentTimeMillis();
		if (lastCompleteMsgSeq == -1) // This is the first msg since init.
		{
			// 1st msg ever -- do nothing.
			Logger.instance().info(module + " Received first Message seqnum="
				+ msgseq);
		}
		else 
		{
			int expect = (lastCompleteMsgSeq + 1) % 0x10000;
			if (msgseq != expect)
			{
				int circ_dist = msgseq - lastCompleteMsgSeq;
				if (circ_dist < 0)
					circ_dist += 0x10000;
				int numMissing = circ_dist - 1;

				if (dcpaddr == DcpAddress.DapsSwitchoverAddr
				 || (numMissing > 100 && msgseq <= 2))
				{
					Logger.instance().info(module 
						+ " DAPS Switchover detected.");
				}
				else
				{
					/* Check for suspiciously large gaps. (> 30 drops/sec) */
					int elapsedSec = (int)((now-lastCompleteMsgTime)/1000L) + 1;
					if (numMissing > elapsedSec * 30)
					{
						Logger.instance().warning(module +
							" Ignoring large gap (" + numMissing 
							+ " dropped in only " + elapsedSec + " seconds).");
//						domsatHardware.reset();
					}
					else
					{
						Logger.instance().warning(module + ":" 
							+ EVT_DOMSAT_SEQ + "- Sequence Gap: "
							+ lastCompleteMsgSeq + "," + numMissing);
						statusProvider.domsatDropped(numMissing, 
							(int)(now / 1000L), expect, elapsedSec);
						Outage otg = new Outage();
						otg.setOutageType(LrgsConstants.domsatGapOutageType);
						Date startDate = new Date(now - elapsedSec*1000L);
						Date endDate = new Date(now);
						otg.setBeginTime(startDate);
						otg.setEndTime(endDate);
						otg.setBeginSeq(expect);
						int gapEnd = msgseq - 1;
						if (gapEnd < 0)
							gapEnd = 65535;
						if (gapEnd < expect)
						{
							// Gap occurred during wrap - have to generate 2
							// outages.
							otg.setEndSeq(65535);
							LrgsDatabaseThread.instance().assertOutage(otg);
							otg = new Outage();
							otg.setOutageType(
								LrgsConstants.domsatGapOutageType);
							otg.setBeginTime(startDate);
							otg.setEndTime(endDate);
							otg.setBeginSeq(0);
						}
						otg.setEndSeq(gapEnd);
						LrgsDatabaseThread.instance().assertOutage(otg);
					}
				}
			}
		}
		lastCompleteMsgTime = now;
		lastCompleteMsgSeq = msgseq;
	}
	//=====================================================================
	// Methods from LrgsInputInterface
	//=====================================================================

	/**
	 * @return the type of this input interface.
	 */
	public int getType() { return DL_DOMSAT; }

	/**
	 * All inputs must keep track of their 'slot', which is a unique index
	 * into the LrgsMain's vector of all input interfaces.
	 * @param slot the slot number.
	 */
	public void setSlot(int slot) { this.slot = slot; }

	/** @return the slot numbery that this interface was given at startup */
	public int getSlot() { return this.slot; }

	/**
	 * @return the name of this interface.
	 */
	public String getInputName() { return module; }

	/**
	 * Initializes the interface.
	 * May throw LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException
	{
		Logger.instance().info(module + " initLrgsInput()");
		if (domsatHardware.init() != 0)
		{
			throw new LrgsInputException("DOMSAT Hardware Init Failed: "
				+ domsatHardware.getErrorMsg());
		}
		this.start();
	}

	/**
	 * Shuts down the interface.
	 * Any errors encountered should be handled within this method.
	 */
	public void shutdownLrgsInput()
	{
		Logger.instance().info(module + " Shutting down.");
		isShutdown = true;
	}

	/**
	 * Enable or Disable the interface. 
	 * The interface should only attempt to archive messages when enabled.
	 * @param enabled true if the interface is to be enabled, false if disabled.
	 */
	public void enableLrgsInput(boolean enabled) {}

	// DOMSAT receiv thread will manage it's own enable/disable.
	public void do_enableLrgsInput(boolean enabled)
	{
		if (isEnabled && !enabled)
		{
			statusCode = DL_DISABLED;
			status = "Disabled";
			domsatHardware.setEnabled(enabled);
			isEnabled = false;
			Logger.instance().info(module + " " + status);
		}
		else if (!isEnabled && enabled)
		{
			statusCode = DL_ACTIVE;
			status = "Active";
			lastPktRecvTime = System.currentTimeMillis();
			if (!domsatHardware.setEnabled(enabled))
			{
				statusCode = DL_ERROR;
				status = "Init-Failed";
				Logger.instance().warning(module + " " + status);
				isEnabled = false;
			}
			else
			{
				isEnabled = true;
				Logger.instance().info(module + " Enabled, acceptARMs="
					+ LrgsConfig.instance().acceptDomsatARMs);
			}
		}
	}

	/**
	 * @return true if this downlink can report a Bit Error Rate.
	 */
	public boolean hasBER() { return true; }

	/**
	 * @return the Bit Error Rate as a string.
	 */
	public String getBER() { return "89"; }

	/**
	 * @return true if this downlink assigns a sequence number to each msg.
	 */
	public boolean hasSequenceNums() { return true; }

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode() { return statusCode; } 

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus() { return status; }

	public int getDataSourceId() { return dataSourceId; }
	
	/** @return true if this interface receives APR messages */
	public boolean getsAPRMessages() 
	{ return LrgsConfig.instance().acceptDomsatARMs; }

	@Override
	public String getGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}
