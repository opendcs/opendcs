/*
 * Open source software by Cove Software, LLC.
 * Prepared under contract to the U.S. Government.
 * Copyright 2014 United States Government, U.S. Geological Survey
 * 
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2014/10/07 12:51:20  mmaloney
 * Created.
 *
*/
package lrgs.iridiumsbd;

import java.io.*;
import java.util.Date;
import java.net.Socket;

import ilex.net.*;
import ilex.util.Logger;
import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.db.LrgsDatabaseThread;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;

/**
 * LRGS Input Interface to open a listening socket to accept connections
 * from the Iridium Gateway.
 * This code is based on the interface defined at:
 * http://www.satcom.ws/upload/iblock/757/IRDM_IridiumSBDService_V3_DEVGUIDE_9Mar2012.pdf
 * It then creates and archives an ASCII message in the format required by the open-source
 * government-owned IridiumPMParser bundled with DECODES.
 */
public class IridiumSbdInterface
	implements LrgsInputInterface
{
	private String module = "IridiumSBD";
	
	/** Default port as defined in SBD spec */
	public static final int SBD_LISTEN_PORT = 10800;
	
	/** The actual port configured */
	private int listenPort = SBD_LISTEN_PORT;
	
	/** Reference back to the main module parent */
	private LrgsMain theMain = null;

	/** Reference to the archive module */
	private MsgArchive msgArchive = null;
	
	/** Part of LRGS IF Spec, have to be able to set and get the slot number */
	private int iridiumSbdSlot = -1;
	
	/** Must be able to return status as a string */
	private String myStat = "Starting";
	
	/** Every input interface will be assigned a data source ID */
	private int sourceId = -1;
	
	/** handles listening port */
	private BasicServer sbdListener = null;
	
	/** Counts number of SBD sessions received. A separate thread handles each one. */
	private int sessionCount=0;

	/**
	 * Called by LrgsMain on startup if Iridium SBD is enabled.
	 * @param theMain
	 * @param msgArchive
	 */
	public IridiumSbdInterface(LrgsMain theMain, MsgArchive msgArchive)
	{
		this.theMain = theMain;
		this.msgArchive = msgArchive;
	}

	// ====== Template methods required by LrgsInterface ======
	@Override
	public void initLrgsInput()
	{
		LrgsDatabaseThread ldt = theMain.getDbThread();
		if (ldt != null)
			sourceId = ldt.getDataSourceId(getInputName(), "(n/a)");
		LrgsConfig cfg = LrgsConfig.instance();
		if (cfg.iridiumPort > 0)
			listenPort = cfg.iridiumPort;

		info("initLrgsInput sourceId=" + sourceId
			+ ", will listen on port " + listenPort);
	}

	@Override
	public void shutdownLrgsInput()
	{
		// No separate steps for shutdown. Just make sure it's disabled.
		enableLrgsInput(false);
	}

	@Override
	public void setSlot(int slot) { iridiumSbdSlot = slot; }
	
	@Override
	public int getSlot() { return iridiumSbdSlot; }

	@Override
	public int getType() { return LrgsInputInterface.DL_IRIDIUM; }

	@Override
	public int getStatusCode() { return DL_STRSTAT; }

	@Override
	public String getStatus() { return myStat; }

	@Override
	public String getInputName() { return LrgsInputInterface.DL_IRIDIUM_TYPESTR; }

	@Override
	public boolean hasBER() { return false; }

	@Override
	public String getBER() { return ""; }

	@Override
	public boolean hasSequenceNums() { return true; }

	@Override
	public int getDataSourceId() { return sourceId; }
	
	@Override
	public String getGroup() { return null; }

	@Override
	public boolean getsAPRMessages() { return false; }

	@Override
	public void enableLrgsInput(boolean enabled)
	{
		if (enabled)
		{
			setStatus("Enabling", true);
			try
			{
				final IridiumSbdInterface parent = this;
				sbdListener = 
					new BasicServer(listenPort)
					{
						protected BasicSvrThread newSvrThread(Socket sock)
							throws IOException
						{
							parent.debug1("Session # " + sessionCount
								+ " starting. Currently receiving " 
								+ (mySvrThreads.size()+1) + " sessions.");
							parent.setStatus("Sessions=" + sessionCount, false);
							return new SbdSessionThread(this, sock, parent, sessionCount++);
						}
					};
				Thread listenThread = 
					new Thread()
					{
						public void run()
						{
							// Will continue to listen until shutdown called.
							try { parent.sbdListener.listen(); }
							catch(Exception ex)
							{
								parent.failure(IridiumEvent.CannotListen,
									"Listen Failed on port " + sbdListener.getPort() + ": " + ex);
								parent.setStatus(IridiumEvent.CannotListen.toString(), true);
							}
						}
					};
				listenThread.start();
				recovery(IridiumEvent.CannotListen, "Listening port created." + listenPort);
			}
			catch(Exception ex)
			{
				failure(IridiumEvent.CannotListen, "Listen on port " + listenPort
					+ " failed: " + ex);
				if (sbdListener != null)
				{
					sbdListener.shutdown();
					sbdListener = null;
				}
			}
		}
		else
		{
			if (sbdListener != null)
			{
				sbdListener.shutdown();
				sbdListener = null;
			}
			setStatus("Disabled", true);
		}
	}

	/**
	 * Archives a message received from Iridium.
	 * Default scope: Called only from client thread.
	 * @param msg The DCP Message to archive
	 */
	public void doArchive(DcpMsg msg)
	{
		msg.setDataSourceId(sourceId);
		msg.setLocalReceiveTime(new Date());
		msg.flagbits = DcpMsgFlag.MSG_PRESENT | DcpMsgFlag.SRC_IRIDIUM | DcpMsgFlag.MSG_TYPE_IRIDIUM;
		msgArchive.archiveMsg(msg, this);
	}
	
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	public void debug1(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.instance().warning(module + " " + msg);
	}
	public void failure(IridiumEvent evt, String msg)
	{
		Logger.instance().failure(module + ":" + evt.getEvtNum() 
			+ (evt.isAutoRecover() ? "-" : "")
			+ " " + msg);
	}
	public void recovery(IridiumEvent evt, String msg)
	{
		Logger.instance().info(module + ":" + (-evt.getEvtNum()) + " " + msg);
	}
	
	/**
	 * Sets status and issues an info message.
	 * Default scope: may be called internally and from the receive thread(s).
	 * @param status
	 */
	synchronized void setStatus(String status, boolean log)
	{
		myStat = status;
		if (log)
			info("Status=" + myStat);
	}
	
	/**
	 * Called by thread after SBD session is complete.
	 */
	synchronized void threadComplete(int sessionNum)
	{
		debug1("Thread complete for session" + sessionNum);
	}
}
