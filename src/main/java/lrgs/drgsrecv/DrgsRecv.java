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

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import lrgs.common.BadConfigException;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.archive.MsgArchive;
import lrgs.drgs.DrgsConnection;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;

/** Main class for the LRGS-DRGS Input Process. */
public class DrgsRecv
	extends Thread
	implements LrgsInputInterface
{
	/** Module name for log messages. */
	public static final String module = "DrgsRecv";

	/** Event Number meaning Cannot Connect to a DRGS System. */
	public static final int EVT_CANNOT_CONNECT = 1;

	/** Event Number meaning Connect Success to a DRGS System. */
	public static final int EVT_CONNECTED = -1;

	/** Event number meaning connection timed out and will be disconnected. */
	public static final int EVT_TIMEOUT = 2;

	/** Event number meaning IO error on socket -- will be disconnected. */
	public static final int EVT_SOCKIO = 3;

	/** Event number meaning bad message header on socket. */
	public static final int EVT_BAD_HEADER = 4;

	/** Event number meaning bad message header on socket. */
	public static final int EVT_BAD_CONFIG = 5;

	/** Event number meaning bad carrier-times socket. */
	public static final int EVT_CARRIER_TIMES = 6;

	/** Event number meaning message too old. */
	public static final int EVT_MSG_TOO_OLD = 7;
	
	public static final int EVT_BAD_EXT_QUAL = 8;

	/** We check for config changes this often. */
	private static final long cfgCheckTime = 30000L;

	/** LrgsMain provides links to other modules. */
	private LrgsMain lrgsMain;

	/** Messages are archived here. */
	private MsgArchive msgArchive;

	/** Last time that settings were read, used to detect changes. */
	private long lastConfigRead;

	/** shutdown flag. */
	private boolean isShutdown;

	/** Vector of DrgsConnection objects. */
	Vector<DrgsConnection> myConnections;

	/** current status code (see LrgsInputInterface) */
	private int statusCode;

	/** Explanatory status string */
	private String status;

	/** enable flag */
	public boolean isEnabled; 

	/** Number of milliseconds in a day. */
	public static final long MS_PER_DAY = 1000L*3600L*24L;

	private int dataSourceId;

	/**
	  Constructor called from main.
	  @param args command line arguments.
	  @see DrgsCmdLineArgs for details on command line arguments.
	*/
	public DrgsRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		this.lrgsMain = lrgsMain;
		this.msgArchive = msgArchive;
		lastConfigRead = 0L;
		isShutdown = false;
		myConnections = new Vector<DrgsConnection>();
		statusCode = DL_INIT;
		status = "Initializing";
		isEnabled = true;
		dataSourceId = lrgs.db.LrgsConstants.undefinedId;
	}

	/** Main thread run method. */
	public void run()
	{
		Logger.instance().info(module + " starting.");
		checkConfig();

		// Read the configuration file.
		DrgsInputSettings settings = DrgsInputSettings.instance();

		statusCode = DL_ACTIVE;
		status = "Active";

		long lastCfgCheck = 0L;
		while(!isShutdown)
		{
			long now = System.currentTimeMillis();
			if (now - lastCfgCheck > cfgCheckTime)
			{
				checkConfig();
				lastCfgCheck = now;
			}

			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}

		statusCode = DL_DISABLED;
		status = "Shutdown";

		// Shutdown stuff here...
		for(Iterator<DrgsConnection> it = myConnections.iterator(); it.hasNext(); )
			(it.next()).shutdown();
	}

	/** @return connection by number */
	DrgsConnection getConnection(int num)
	{
		for(Iterator<DrgsConnection> it = myConnections.iterator(); it.hasNext(); )
		{
			DrgsConnection con = it.next();
			if (con.getConnectNum() == num)
				return con;
		}
		return null;
	}

	/**
	 * Check the configuration file to see if it has changed. If so, reload
	 * it and put the changes into effect.
	 */
	private void checkConfig()
	{
		Logger.instance().debug3(module + " checkConfig");

		if (isEnabled && !LrgsConfig.instance().enableDrgsRecv)
		{
			status = "Disabled";
			isEnabled = false;
		}
		else if (!isEnabled && LrgsConfig.instance().enableDrgsRecv)
		{
			status = "Active";
			isEnabled = true;
		}

		if (!isEnabled)
		{
			for(Iterator<DrgsConnection> it = myConnections.iterator(); it.hasNext(); )
			{
				DrgsConnection con = it.next();
				con.shutdown();
				DrgsRecvMsgThread msgThread =
					(DrgsRecvMsgThread)con.getMsgThread();
				lrgsMain.freeInput(msgThread.getSlot());
				it.remove();
			}
			return;
		}

		DrgsInputSettings settings = DrgsInputSettings.instance();
		String fn = EnvExpander.expand(LrgsConfig.instance().drgsRecvConfig);
		File cf = new File(fn);
		if (cf.lastModified() > lastConfigRead)
		{
			lastConfigRead = System.currentTimeMillis();
			try
			{
				settings.setFromFile(fn);
			}
			catch(BadConfigException ex)
			{
				Logger.instance().failure(module + 
					" Cannot read DRGS Recv Config File '" + cf + "': " + ex);
				return;
			}

			// First mark each existing connection as NOT configured.
			for(Iterator<DrgsConnection> it = myConnections.iterator(); it.hasNext(); )
				it.next().clearConfigured();

			// Go through each connection specified in the new config.
			for(Iterator it = settings.getConnectConfigs(); it.hasNext(); )
			{
				DrgsConnectCfg cfg = (DrgsConnectCfg)it.next();
				DrgsConnection con = getConnection(cfg.connectNum);
				if (con == null)
				{
					DrgsRecvMsgThread msgThread = 
						new DrgsRecvMsgThread(msgArchive, lrgsMain);
					lrgsMain.addInput(msgThread);
					con = new DrgsConnection(cfg.connectNum, msgThread);
					myConnections.add(con);
				}
				con.configure(cfg);
			}

			// Now kill & remove any connections that are not configured.
			boolean allConfigured = false;
		  nextCfg:
			while(!allConfigured)
			{
				for(Iterator<DrgsConnection> it = myConnections.iterator(); it.hasNext(); )
				{
					DrgsConnection con = it.next();
					if (!con.isConfigured())
					{
						con.shutdown();
						it.remove();
						DrgsRecvMsgThread msgThread =
							(DrgsRecvMsgThread)con.getMsgThread();
						lrgsMain.freeInput(msgThread.getSlot());
						continue nextCfg;
					}
				}
				allConfigured = true;
			}
		}
	}

	//=====================================================================
	// Methods from LrgsInputInterface
	//=====================================================================

	/**
	 * @return the type of this input interface.
	 */
	public int getType() { return DL_DRGS; }

	/**
	 * All inputs must keep track of their 'slot', which is a unique index
	 * into the LrgsMain's vector of all input interfaces.
	 * @param slot the slot number.
	 */
	public void setSlot(int slot) {}

	/** @return the slot number that this interface was given at startup */
	public int getSlot() { return -1; }

	/**
	 * @return the name of this interface.
	 */
	public String getInputName() { return "DRGS-Recv:Main"; }

	/**
	 * Initializes the interface.
	 * May throw LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException
	{
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
	public void enableLrgsInput(boolean enabled)
	{
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
	public boolean hasSequenceNums() { return false; }

	/**
	 * @return the numeric code representing the current status.
	 */
	public int getStatusCode() { return statusCode; } 

	/**
	 * @return a short string description of the current status.
	 */
	public String getStatus() { return status; }

	
	public int getDataSourceId() { return dataSourceId; }
	
	/** @return DRGS never receives APR messages */
	public boolean getsAPRMessages() { return false; }

	@Override
	public String getGroup() {
		// TODO Auto-generated method stub
		return null;
	}

}

