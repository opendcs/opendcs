/*
 * Open source software by Cove Software, LLC
*/
package lrgs.iridium;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.util.Date;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

/**
This is an LRGS data source that reads data from Iridum Short Burst Data (SBD)
devices over the Iridium DirectIP system.
<p>
Reference: Iridium Short Burst Data Subscriber Device Interface Control Doc
and the SDB DirectIP Specification version 1.0, dated 7/3/2006.
<p>
This module opens a listening socket on port 10800 and waits for connections from the
GSS (Gateway SBD Subsystem). After connecting, the GSS will send a single SBD
message and then hangup. The message protocol is handled by separate 
IridiumRecvThread, spawned for each connection.
<p>Miscellaneous Config Properties:
<ul>
  <li>iridium.port - listening port number (default=10800)</li
</ul>
*/
public class IridiumRecv
	implements LrgsInputInterface
{
	private int mySlot = -1;
	private String currentStatus = "Initializing";
	private int dataSourceId = lrgs.db.LrgsConstants.undefinedId;
	IridiumListener listener;
	private MsgArchive msgArchive;
	private LrgsMain lrgsMain;

	public static final int DEFAULT_LISTENING_PORT = 10800;

	/** Module name for use in log messages. */
	public static final String module = "iridium";

	public static final int EVT_LISTEN_FAILED = 1;
	public static final int EVT_RECV_FAILED = 2;
	public static final int EVT_HEADER_PARSE = 3;
	public static final int EVT_MESSAGE_PARSE = 4;
	public static final int EVT_BAD_CONFIG = 5;

	public IridiumRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		listener = null;
		this.lrgsMain = lrgsMain;
		this.msgArchive = msgArchive;
	}

	public int getType()
	{
		return DL_IRIDIUM;
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
		return DL_IRIDIUM_TYPESTR;
	}

	public void initLrgsInput()
		throws LrgsInputException
	{
		dataSourceId =
			lrgsMain.getDbThread().getDataSourceId(DL_IRIDIUM_TYPESTR, "");
		Logger.instance().info(module + " Initialized " + DL_IRIDIUM_TYPESTR
			+ " interface with ID=" + dataSourceId);
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
			int port = LrgsConfig.instance().iridiumPort;
			Logger.instance().info(module + " Enabling on port " + port);

			// open listening socket & wait for connections.
			try
			{
				listener = new IridiumListener(this, port);
				currentStatus = "Starting";
				listener.start();
			}
			catch(IOException ex)
			{
				Logger.instance().failure(module + ":" + EVT_BAD_CONFIG
                    + " Cannot listen on port '" + port + "': " + ex);
				listener = null;
			}
		}
		else
		{
			Logger.instance().info(module + " Disabling");
			// Shutting down listener will also kill msg receive client thread.
			if (listener != null)
				listener.shutdown();
			listener = null;
			currentStatus = "Shutdown";
//			if (captureStream != null)
//			{
//				try
//				{
//					captureStream.flush();
//					captureStream.close();
//				}
//				catch(IOException ex) {}
//				finally
//				{
//					captureStream = null;
//				}
//			}
		}
	}
	
//	public BufferedOutputStream getCaptureStream() { return captureStream; }

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
			| DcpMsgFlag.SRC_IRIDIUM 
			| DcpMsgFlag.MSG_TYPE_IRIDIUM;
		msg.setLocalReceiveTime(new Date());
		msg.setSeqFileName(null);
		msg.setDataSourceId(dataSourceId);

		// For Iridium, the recv-thread will set seq# from the MOMSN in the msg.
		//msg.setSequenceNum(sequenceNum++);
		msgArchive.archiveMsg(msg, this);
	}

	public void setStatus(String status)
	{
		if (!currentStatus.equalsIgnoreCase("shutdown"))
			currentStatus = status;
	}
	
	/** @return false Iridium never receives APR messages */
	public boolean getsAPRMessages() { return false; }

	@Override
	public String getGroup() {
		// TODO Auto-generated method stub
		return null;
	}

}
