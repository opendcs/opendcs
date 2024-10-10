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
package lrgs.networkdcp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.xml.DomHelper;
import ilex.xml.XmlOutputStream;

import lrgs.common.BadConfigException;
import lrgs.lrgsmain.LrgsMain;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.archive.MsgArchive;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;
import lrgs.drgsrecv.DrgsRecvMsgThread;

/** Main class for the LRGS-DRGS Input Process. */
public class NetworkDcpRecv
	extends Thread
	implements LrgsInputInterface
{
	/** Module name for log messages. */
	public static final String module = "NetworkDcpRecv";

	/** Event Number meaning Cannot Connect to a DCP. */
	public static final int EVT_CANNOT_CONNECT = 1;

	/** Event Number meaning Connect Success to a DCP. */
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

	/** We check for config changes this often. */
	private static final long cfgCheckTime = 30000L;
	
	public static final String cfgFileName = "$LRGSHOME/network-dcp.conf";
	public static final String statFileName = "$LRGSHOME/network-dcp.stat";

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

	/** enable flag */
	public boolean isEnabled; 

	private int dataSourceId;
	
	private DrgsInputSettings networkDcpSettings = new DrgsInputSettings();
	
	private DcpConfigList dcpConfigList = new DcpConfigList();
	
	private ArrayList<ContinuousNetworkDcpThread> continuousThreads = 
		new ArrayList<ContinuousNetworkDcpThread>();
	
	private ArrayList<PolledNetworkDcpThread> polledThreads = 
		new ArrayList<PolledNetworkDcpThread>();
	
	private NetworkDcpStatusList statusList = 
		new NetworkDcpStatusList();
	
	/** We checkpoint the DCP status this often. */
	private static final long checkpointStatusMsec = 300000L; // 5 min.

	
	/**
	  Constructor called from main.
	  @param args command line arguments.
	  @see DrgsCmdLineArgs for details on command line arguments.
	*/
	public NetworkDcpRecv(LrgsMain lrgsMain, MsgArchive msgArchive)
	{
		this.lrgsMain = lrgsMain;
		this.msgArchive = msgArchive;
		lastConfigRead = 0L;
		isShutdown = false;
		statusCode = DL_INIT;
		status = "Initializing";
		isEnabled = true;
		dataSourceId = lrgs.db.LrgsConstants.undefinedId;
		networkDcpSettings.setModule(module);
	}

	/** Main thread run method. */
	public void run()
	{
		Logger.instance().info(module + " starting.");
		checkConfig();

		initStatusList();

		statusCode = DL_ACTIVE;
		status = "Active";

		long lastCfgCheck = 0L;
		long lastStatusCheckpoint = System.currentTimeMillis();
		while(!isShutdown)
		{
			long now = System.currentTimeMillis();
			if (now - lastCfgCheck > cfgCheckTime)
			{
				checkConfig();
				lastCfgCheck = now;
			}
			if (now - lastStatusCheckpoint > checkpointStatusMsec)
			{
				checkpointStatus();
				lastStatusCheckpoint = now;
			}
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}

		statusCode = DL_DISABLED;
		status = "Shutdown";

		// Shutdown stuff here...
		for (DrgsRecvMsgThread drmt : continuousThreads)
			drmt.shutdown();
		continuousThreads.clear();
		
		for (PolledNetworkDcpThread pndt : polledThreads)
			pndt.shutdown();
		polledThreads.clear();
		checkpointStatus();
		Logger.instance().info(module + " exiting.");
	}

	/**
	 * Check the configuration file to see if it has changed. If so, reload
	 * it and put the changes into effect.
	 */
	private void checkConfig()
	{
		Logger.instance().debug3(module + " checkConfig");

		if (isEnabled && !LrgsConfig.instance().networkDcpEnable)
		{
			status = "Disabled";
			isEnabled = false;
		}
		else if (!isEnabled && LrgsConfig.instance().networkDcpEnable)
		{
			status = "Active";
			isEnabled = true;
		}

		if (!isEnabled)
		{
			dcpConfigList.killAll();
			manageThreads();
			return;
		}

		String fn = EnvExpander.expand(cfgFileName);
		File cf = new File(fn);
		if (cf.lastModified() > lastConfigRead)
		{
			lastConfigRead = System.currentTimeMillis();
			try
			{
				networkDcpSettings.setFromFile(fn);
				dcpConfigList.processNewConfig(networkDcpSettings,
					statusList);
				manageThreads();
			}
			catch(BadConfigException ex)
			{
				Logger.instance().failure(module + 
					" Cannot read DRGS Recv Config File '" + cf.getPath()
					+ "': " + ex);
				return;
			}
		}
	}
	
	private void manageThreads()
	{
		// Remove any dedicated thread that have died.
		for(Iterator<ContinuousNetworkDcpThread> it = 
			continuousThreads.iterator(); it.hasNext(); )
		{
			DrgsRecvMsgThread drmt = it.next();
			if (drmt.getConfig().getState() == NetworkDcpState.Dead)
			{
				lrgsMain.freeInput(drmt.getSlot());
				drmt.shutdown();
				it.remove();
			}
		}
		
		// Add dedicated threads for any new connections.
		for (DrgsConnectCfg dcc : dcpConfigList.getContinuousDcps())
		{
			boolean haveThread = false;
			for (DrgsRecvMsgThread drmt : continuousThreads)
				if (dcc == drmt.getConfig())
				{
					haveThread = true;
					break;
				}
			if (!haveThread)
			{
				ContinuousNetworkDcpThread cndt = 
					new ContinuousNetworkDcpThread(msgArchive, lrgsMain,
						dcpConfigList, statusList);
				lrgsMain.addInput(cndt);
				cndt.configure(dcc);
				Thread t = new Thread(cndt);
Logger.instance().info(module + " starting ContinuousNetworkDcpThread");
				t.start();
				continuousThreads.add(cndt);
			}
		}

		// Number of polling threads will be numDcps/10 but not less than 2.
		int numPolled = dcpConfigList.getNumPolled();
		int idealNumThreads = numPolled / 10;
		if (idealNumThreads < 2) idealNumThreads = 2;
Logger.instance().debug1(module + " current numPolled=" + numPolled +
", ideal=" + idealNumThreads);

		// Never reduce number of polling threads.
		while(polledThreads.size() < idealNumThreads)
		{
			PolledNetworkDcpThread pndt = 
				new PolledNetworkDcpThread(polledThreads.size(),
					msgArchive, lrgsMain, dcpConfigList, statusList);
//			lrgsMain.addInput(pndt);
			Thread t = new Thread(pndt);
			t.start();
			polledThreads.add(pndt);
		}
		
		for(PolledNetworkDcpThread pndt : polledThreads)
			pndt.enableLrgsInput(isEnabled);
	}

	//=====================================================================
	// Methods from LrgsInputInterface
	//=====================================================================

	/**
	 * @return the type of this input interface.
	 */
	public int getType() { return DL_NETWORKDCP; }

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
	public String getInputName() { return "NetworkDcp:Main"; }

	/**
	 * Initializes the interface.
	 * May throw LrgsInputException when an unrecoverable error occurs.
	 */
	public void initLrgsInput()
		throws LrgsInputException
	{
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
	
	private void initStatusList()
	{
		String path = EnvExpander.expand(statFileName);
		Logger.instance().info(module + ": Parsing '" + path + "'");

		Document doc;
		try
		{
			doc = DomHelper.readFile(module, path);
		}
		catch(ilex.util.ErrorException ex)
		{
			Logger.instance().warning(ex.toString());
			return;
		}

		Node statusElem = doc.getDocumentElement();
		if (!statusElem.getNodeName().equalsIgnoreCase("NetworkDcpList"))
		{
			String s = module + " File '" + path + "'"
				+ ": Wrong type of status file -- Cannot initialize status. "
				+ "Root element is not 'NetworkDcpList'.";
			Logger.instance().warning(s);
		}

		// MJM Don't do this. It causes bogus connections to never go away
		//statusList.initFromXml((Element)statusElem);
	}
	
	private void checkpointStatus()
	{
		String path = EnvExpander.expand(statFileName);
		Logger.instance().debug1(module + ": Writing '" + path + "'");
		try
		{
			XmlOutputStream xos = new XmlOutputStream( 
				new FileOutputStream(path), "NetworkDcpList");
			statusList.saveToXml(xos);
		}
		catch(FileNotFoundException ex)
		{
			Logger.instance().warning(module + " Cannot write '"
				+ path + "': " + ex);
		}
	}
	public NetworkDcpStatusList getStatusList() { return statusList; }

	@Override
	public String getGroup() {
		// TODO Auto-generated method stub
		return null;
	}
}

