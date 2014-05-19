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
package lrgs.noaaportrecv;

import java.io.IOException;
import java.util.Date;

import ilex.util.Logger;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

/**
Reads data from a Marta Systems or Unisys NOAAPORT Receiver.
Format is as follows:

FRAME := [SOH] [\r]\r\n [SEQNUM [\r]\r\n PRODHEAD \r\r\n [0x1E] DCPMSG [\r\r\n] [ETX] '\r\n####5\r\n'

SEQNUM := 0-9*
	(note: SEQNUM present for UNISYS receiver only)

PRODHEAD := pppppp <sp> ffff <sp> HHMMSS
	pppppp := (6 character product ID)
	ffff := (4 char office ID, usually KWAL)
	HHMMSS := time of day product was generated

DCPMSG := MSGHEADER MSGDATA MSGTRAILER
	MSGHEADER := AAAAAAAA Q DDDHHMMSS
	Q := either space or '?', indicating good or parity error respectively

MSGDATA := (actual msg data - variable length)

MSGTRAILER := <sp> SSFFNN <sp> CCCs
	SS := (2 digit signal strength)
	FF := (Frequence Offset units=50Hz. E.g. -3 = -150Hz.)
	NN := 2-char status indicator.
	CCCs := 3-digit channel followed by 'E' or 'W' indicating Spacecraft).

Parsing strategy:
1. Have a property specifing a blank-specified list of office IDs that I should
   process. Default to "KWAL".
2. Property for port number on which to listen to connectsion from the noaaport.
3. State Machine with the following States:
	HUNT: Look for SOH, when found --> PROPHEAD
	PROPHEAD: buffer until I hit \0x1E, When I do ...
		WMO header must begin with an 'S' and office ID must be on my list.
		if not, --> HUNT, else --> DCPMSG
	DCPMSG: buffer until ETX, when found:
		Get DCP Address & date/time from header
			Assume current year but if (doy < currentDOY && currentDOY>=364) ++
		Backup from end and parse trailer: SS, FF, NN, CCCs
		Construct DcpMsg object & call archive
		--> HUNT
*/
public class NoaaportRecv
	implements LrgsInputInterface
{
	private int mySlot = -1;
	private String currentStatus = "Initializing";
	private int dataSourceId = lrgs.db.LrgsConstants.undefinedId;
	private NoaaportListener listener = null;
	private NoaaportClient client = null;
	private MsgArchive msgArchive;
	private LrgsMain lrgsMain;

	/** Module name for use in log messages. */
	public static final String module = "noaaport";

	public static final int EVT_LISTEN_FAILED = 1;
	public static final int EVT_RECV_FAILED = 2;
	public static final int EVT_HEADER_PARSE = 3;
	public static final int EVT_MESSAGE_PARSE = 4;
	public static final int EVT_BAD_CONFIG = 5;

	private static int sequenceNum = 0;

	public NoaaportRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		this.lrgsMain = lrgsMain;
		this.msgArchive = msgArchive;
	}

	public int getType()
	{
		return DL_NOAAPORT;
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
		return "NOAAPORT-" + LrgsConfig.instance().noaaportReceiverType;
	}

	public void initLrgsInput()
		throws LrgsInputException
	{
		dataSourceId =
			lrgsMain.getDbThread().getDataSourceId(DL_NOAAPORT_TYPESTR, "");
		Logger.instance().info("Initialized NOAAPORT interface with ID="
			+ dataSourceId);
	}

	public void shutdownLrgsInput()
	{
		enableLrgsInput(false);
	}

	/**
	 * Enable or Disable the interface. 
	 * The interface should only attempt to archive messages when enabled.
	 * @param enabled true if the interface is to be enabled, false if disabled.
	 */
	public void enableLrgsInput(boolean enabled)
	{
		if (enabled)
		{
			// Get port number from config
			LrgsConfig cfg = LrgsConfig.instance();

			// open listening socket & wait for connections.
			try
			{
				if (cfg.noaaportReceiverType != null
				 && cfg.noaaportReceiverType.toLowerCase().contains("unisys"))
				{
					client = new NoaaportClient(
						cfg.noaaportHostname, cfg.noaaportPort, this);
					Thread clientThread = new Thread(client);
					currentStatus = "Starting";
					clientThread.start();
				}
				else // either marta or PDI. We are a listening socket
				{
					listener = new NoaaportListener(this, cfg.noaaportPort);
					currentStatus = "Starting";
					listener.start();
				}
			}
			catch(IOException ex)
			{
				Logger.instance().failure(module + ":" + EVT_BAD_CONFIG
                    + " Cannot listen on port " + cfg.noaaportPort + ": " + ex);
				listener = null;
			}
		}
		else
		{
			// Shutting down listener will also kill msg receive client thread.
			if (listener != null)
				listener.shutdown();
			listener = null;
			if (client != null)
				client.shutdown();
			client = null;
			currentStatus = "Shutdown";
		}
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
	public boolean hasSequenceNums() { return true; }

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode() { return DL_STRSTAT; }

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus() { return currentStatus; }

	/**
	 * @return the unique data source ID for this input interface.
	 */
	public int getDataSourceId() { return dataSourceId; }

	public void archive(DcpMsg msg)
	{
		msg.flagbits = 
			  DcpMsgFlag.MSG_PRESENT
			| DcpMsgFlag.SRC_NOAAPORT 
			| DcpMsgFlag.MSG_NO_SEQNUM;
		msg.setLocalReceiveTime(new Date());
		msg.setSeqFileName(null);
		msg.setDataSourceId(dataSourceId);
		if (msg.getSequenceNum() < 0)
		{
			msg.setSequenceNum(sequenceNum++);
			if (sequenceNum >= 1000000)
				sequenceNum = 0;
		}
		msgArchive.archiveMsg(msg, this);
	}


	public void setStatus(String status)
	{
		if (!currentStatus.equalsIgnoreCase("shutdown"))
			currentStatus = status;
	}
	
	/** @return false because NOAAPORT never receives APR messages */
	public boolean getsAPRMessages() { return false; }

	@Override
	public String getGroup()
	{
		return null;
	}

}
