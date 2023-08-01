/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 *  OPENDCS 6.0 Initial Checkin
 *
 *  Revision 1.6  2012/12/12 16:01:31  mmaloney
 *  Several updates for 5.2
 *
 *  Revision 1.5  2009/10/16 12:39:00  mjmaloney
 *  LRIT updates
 *
 *  Revision 1.4  2009/10/09 18:11:42  mjmaloney
 *  Added flag bytes and carrier times to LRIT File.
 *
 *  Revision 1.3  2009/08/24 13:48:13  shweta
 *  Code added to accept connections from LQM in dormant mode.
 *
 *  Revision 1.2  2009/08/14 14:09:09  shweta
 *  Changes done to incorporate backup LRIT .
 *  The files are transfered to Domain 2 servers using Ganyemade api.
 *
 *  Revision 1.1  2008/04/04 18:21:16  cvs
 *  Added legacy code to repository
 *
 *  Revision 1.15  2005/12/30 19:40:59  mmaloney
 *  dev
 *
 *  Revision 1.14  2004/05/25 15:06:52  mjmaloney
 *  Propegate debug level to all loggers.
 *
 *  Revision 1.13  2004/05/24 13:55:05  mjmaloney
 *  dev
 *
 *  Revision 1.12  2004/05/21 18:27:44  mjmaloney
 *  Release prep.
 *
 *  Revision 1.11  2004/05/18 22:52:40  mjmaloney
 *  dev
 *
 *  Revision 1.10  2004/05/18 18:02:09  mjmaloney
 *  dev
 *
 *  Revision 1.9  2004/05/11 20:46:25  mjmaloney
 *  LQM Impl
 *
 *  Revision 1.8  2004/05/06 21:48:14  mjmaloney
 *  Implemented Lqm Server. Modified GUI to read events over the net.
 *
 *  Revision 1.7  2004/05/05 19:52:46  mjmaloney
 *  Integrated UIServer & UISvrThread to LritDcsMain
 *
 *  Revision 1.6  2003/08/15 20:13:07  mjmaloney
 *  dev
 *
 *  Revision 1.5  2003/08/11 23:38:11  mjmaloney
 *  dev
 *
 *  Revision 1.4  2003/08/11 15:59:19  mjmaloney
 *  dev
 *
 *  Revision 1.3  2003/08/11 01:33:58  mjmaloney
 *  dev
 *
 *  Revision 1.2  2003/08/10 02:22:47  mjmaloney
 *  dev.
 *
 *  Revision 1.1  2003/08/06 23:29:24  mjmaloney
 *  dev
 *
 */
package lritdcs;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import ilex.util.QueueLogger;
import ilex.util.ServerLock;
import ilex.util.ServerLockable;
import ilex.util.TeeLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TimeZone;

public class LritDcsMain implements ServerLockable
// , Observer
{
	private static LritDcsMain _instance = null;
	private FileQueue fileQueueHigh;
	private FileQueue fileQueueMedium;
	private FileQueue fileQueueLow;
	private FileQueue fileQueueAutoRetrans;
	private FileQueue fileQueueManualRetrans;
	private LinkedList fileNamesPending;
	private ServerLock myServerLock;
	private boolean shutdownFlag;
	private LritDcsStatus myStatus;
	private QueueLogger logQueue;
	ManualRetransThread manualRetransThread;
	LqmInterfaceServer lqmServer;

	// / ID for log messages, lock files, log files, etc.
	public static final String progname = "lritdcs";
	LritDcsConnection lritconn;
	LritDcsConfig cfg;

	private boolean isActive;
	private String lritStartState;

	GetMessageThread getMessageThread;
	SendFileThread sendFileThread;
	ScrubberThread scrubberThread;
	
	public String domain2AStatus;
	public String domain2BStatus;
	public String domain2CStatus;
	
	private  UIServer uiServer;
	
	private String currentState = "nada";
	private String rsaKeyFile = "";


	private LritDcsMain() {
		fileQueueHigh = new FileQueue();
		fileQueueMedium = new FileQueue();
		fileQueueLow = new FileQueue();
		fileQueueAutoRetrans = new FileQueue();
		fileQueueManualRetrans = new FileQueue();
		fileNamesPending = new LinkedList();		
	}

	public static LritDcsMain instance() {
		if (_instance == null)
			_instance = new LritDcsMain();
		return _instance;
	}

	/**
	 * @return the lritStartState
	 */
	public String getLritStartState() {
		return lritStartState;
	}

	/**
	 * @param lritStartState
	 *            the lritStartState to set
	 */
	public void setLritStartState(String lritStartState) {
		this.lritStartState = lritStartState;
	}
	
	public void newrun()
	{
		initialize();
		// TODO modify initialize to create local 'cfg' reference too.
		
		cfg = LritDcsConfig.instance();
		while (!shutdownFlag) 
		{
			// Check for switching state.
			if (!currentState.equalsIgnoreCase(cfg.fileSenderState))
			{
				currentState = cfg.fileSenderState;
				if (currentState.equalsIgnoreCase("active"))
				{
					// Switch to active state;
				}
				else
				{
					// Switch to dormant state
				}
			}
			try { Thread.sleep(1000L); }
            catch (InterruptedException ex) {}
		}
	}

	// / Main loop for running the LRIT DCS Program.
	public void run() 
	{
		cfg = LritDcsConfig.instance();
		
		// Default is to use that last known state.
		if (lritStartState.equalsIgnoreCase("last"))
			lritStartState = cfg.fileSenderState;
		
		if (lritStartState.equalsIgnoreCase("active"))
			startActiveMode();
		else
			startDormantMode();
	}
	
	/**
	 * Initializes LRIT
	 */
	private void initialize()
	{
		
		
		
		/*
		 * After cmdline args, logger will be a file logger. Reset it to a Tee
		 * that logs to both the file and a queue.
		 */		
		Logger fl = Logger.instance();
		logQueue = new QueueLogger(fl.getProcName(),fl.getMinLogPriority());
		TeeLogger tl = new TeeLogger(fl.getProcName(), fl, logQueue);
		Logger.setLogger(tl);
		tl.setTimeZone(TimeZone.getTimeZone("UTC"));
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Logger.setDateFormat(df);

		String home = cfg.getLritDcsHome();

		// Initialize status from last-run values. Then call the rotate
		// function to make it current.
		myStatus = new LritDcsStatus(home + File.separator
				+ LritDcsStatus.STAT_FILE_NAME);
		try {
			myStatus.loadFromFile();
			myStatus.rotateStatus();
		} catch (StatusInvalidException ex) {
			// shouldn't happen. If it does, clear status & start fresh.
			myStatus.clear();
		}

		// Establish a server lock file & start the server lock monitor
		String lockName = home + File.separator + progname + ".lock";
		myServerLock = new ServerLock(lockName);
		if (!myServerLock.obtainLock(this)) {
			Logger.instance().fatal(
					"Lock file '" + lockName + "' already taken. "
							+ "Is another instance of '" + progname
							+ "' already running?");
			System.exit(0);
		}
		

		try {
			//final UIServer uiServer = new UIServer(cfg.getLritUIPort());
			uiServer = new UIServer(cfg.getLritUIPort());
			Thread uiServerThread = new Thread() {
				public void run() {
					try {
						uiServer.listen();
					} catch (IOException ex) {
						ex.printStackTrace();
						Logger
								.instance()
								.failure(
										"LRIT:"
												+ Constants.EVT_UI_LISTEN_ERR
												+ "- User Interface Listening Socket Error: "
												+ ex);
					}
				}
			};
			uiServerThread.start();
			
		} catch (IOException ex) {
			ex.printStackTrace();
			Logger.instance().fatal(
					"LRIT:" + Constants.EVT_UI_LISTEN_ERR
							+ "- Cannot create User Interface server: " + ex);
			System.exit(0);
		}
	}

	

	
	
	/**
	 * This method starts LRIT in active mode
	 */
	
	public void startActiveMode() {

		isActive = true;
		
		try
		{
			File cfgFile = cfg.getConfigFile();
			File tf = new File(cfgFile.getPath() + ".tmp");
			FileUtil.copyFile(cfgFile, tf);
			PrintWriter tw = new PrintWriter(new FileOutputStream(tf));
			cfg.saveState(tw, "Active");
			tw.close();
			FileUtil.moveFile(tf, cfgFile);
			Logger.instance().info("Saved LRIT state to "
				+ cfgFile.getPath());
			cfg.load();
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error saving LRIT state in config file: " + ex);
			return;
		}
		
		
		initialize();
		lritconn = LritDcsConnection.instance();
		// Create & initialize the threads.
		getMessageThread = new GetMessageThread();
		sendFileThread = new SendFileThread();
		scrubberThread = new ScrubberThread();
		manualRetransThread = new ManualRetransThread();
		try {
			getMessageThread.init();
			sendFileThread.init();
			scrubberThread.init();
			manualRetransThread.init();
		} catch (InitFailedException ex) {
			System.err.println("Init Failed: " + ex.toString());
			System.exit(0);
		}

		getMessageThread.start();
		sendFileThread.start();
		scrubberThread.start();
		manualRetransThread.start();

		try {
			lqmServer = new LqmInterfaceServer(cfg.getLqmPort());
			if (cfg.getEnableLqm())
				lqmServer.startListeningThread();
			else
				lqmServer.shutdown();
		} catch (Exception ex) {
			Logger.instance().failure(
					"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
							+ "- Cannot create LQM Interface server: " + ex
							+ " -- No LQM connections will be accepted.");
		}

		// main loop here.
		Logger.instance().log(Logger.E_INFORMATION,
				"Starting LRIT in 'Active' mode.");
		
		domain2AStatus = "Active";
		domain2BStatus="Active";
		domain2CStatus="Active";
	
		whileActive();
	}

	
	/**
	 * This method makes LRIT active.
	 */
	public void runActive() {
		
		
		try {
			isActive = true;

			if(lritconn==null)
				lritconn = LritDcsConnection.instance();
			else
				lritconn.openConnections();

			// Create & initialize the threads.
			
			
				getMessageThread = new GetMessageThread();

				sendFileThread = new SendFileThread();

				scrubberThread = new ScrubberThread();

				manualRetransThread = new ManualRetransThread();

			try {
				getMessageThread.init();
				sendFileThread.init();
				scrubberThread.init();
				manualRetransThread.init();
			} catch (InitFailedException ex) {
				System.err.println("Init Failed: " + ex.toString());
				System.exit(0);
			}

			getMessageThread.start();
			sendFileThread.start();
			scrubberThread.start();
			manualRetransThread.start();

			try {
				if (lqmServer == null)
					lqmServer = new LqmInterfaceServer(cfg.getLqmPort());

				if (cfg.getEnableLqm())
					lqmServer.startListeningThread();
				else
					lqmServer.shutdown();

			} catch (Exception ex) {
				Logger.instance().failure(
						"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
								+ "- Cannot create LQM Interface server: " + ex
								+ " -- No LQM connections will be accepted.");
			}

			// main loop here.
			Logger.instance().log(Logger.E_INFORMATION,
					"LRIT is running in  'Active' mode now.");
			long lastStatusSave = System.currentTimeMillis();
			whileActive();
		} catch (Exception e) {
			
			e.printStackTrace();
		}
	}
	

	/**
	 * Runs LRIT in active mode.
	 */
	public void whileActive()
	{

		while (!shutdownFlag) {
			
			
		// Check for config changes, reload & notify if necessary.
		if (LritDcsConfig.instance().checkConfigFile()) {
			
			//System.out.println("Config Changed in active mode");
			lqmServer.updateConfig();
			
			// load config changes in LRITDCS Connection.			
			//lritconn.getConfigValues(cfg);
			sendFileThread.getConfigValues(cfg);

			String strState = cfg.getFileSenderState();
			
			if (strState.equalsIgnoreCase("dormant")) {
				if (isActive)
					runDormant();
			} else {
				if (!isActive)
					runActive();
			}

		}

		myStatus.rotateStatus();
		myStatus.serverGMT = System.currentTimeMillis();
		myStatus.status = "Running";
		myStatus.filesQueuedHigh = fileQueueHigh.size();
		myStatus.filesQueuedMedium = fileQueueMedium.size();
		myStatus.filesQueuedLow = fileQueueLow.size();
		myStatus.filesQueuedAutoRetrans = fileQueueAutoRetrans.size();
		myStatus.filesQueuedManualRetrans = fileQueueManualRetrans.size();
		myStatus.filesPending = fileNamesPending.size();
		
		myStatus.domain2Ahost = cfg.getDom2AHostName();
		myStatus.domain2Bhost = cfg.getDom2BHostName();
		myStatus.domain2Chost = cfg.getDom2CHostName();
		myStatus.domain2AStatus = domain2AStatus;
		myStatus.domain2BStatus = domain2BStatus;
		myStatus.domain2CStatus = domain2CStatus;	
		myStatus.writeToFile();

		try {
			Thread.sleep(1000L);
		} catch (InterruptedException ex) {
		}
		}
		getMessageThread.shutdown();

		while (getMessageThread.isAlive()) {
			Logger.instance().log(Logger.E_DEBUG1,
					"Waiting for message thread to terminate.");
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
			}
		}

		sendFileThread.shutdown();
		while (sendFileThread.isAlive()) {
			Logger.instance().log(Logger.E_DEBUG1,
					"Waiting for SendFileThread to terminate.");
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
			}
		}

		// Write the final status after everything is quiet.
		myStatus.status = "Shutdown";
		myStatus.writeToFile();

		
		//shutdown UIserver socket
		uiServer.shutdown();
		if (lqmServer != null)
		lqmServer.shutdown();
		
		// Release lock & die.
		Logger.instance().log(Logger.E_INFORMATION, "Exiting.");
		myServerLock.releaseLock();
		try {
			Thread.sleep(2000L);
		} catch (InterruptedException ex) {
		}
		System.exit(0);
	}
	
	
	/**
	 * Starts LRIT in Dormant mode.
	 */
	public void startDormantMode() {

		isActive = false;
		try
		{
			File cfgFile = cfg.getConfigFile();
			File tf = new File(cfgFile.getPath() + ".tmp");
			FileUtil.copyFile(cfgFile, tf);
			PrintWriter tw = new PrintWriter(new FileOutputStream(tf));
			cfg.saveState(tw, "Dormant");
			tw.close();
			FileUtil.moveFile(tf, cfgFile);
			Logger.instance().info("Saved LRIT state to "
				+ cfgFile.getPath());
			cfg.load();
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error saving LRIT state in config file: " + ex);
			return;
		}
		
		initialize();			
		try {
			if (lqmServer == null)
				lqmServer = new LqmInterfaceServer(cfg.getLqmPort());	
			
			if (!cfg.getEnableLqm())				
				lqmServer.shutdown();
			
		} catch (Exception ex) {
			Logger.instance().failure(
					"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
							+ "- Cannot create LQM Interface server: " + ex
							+ " -- No LQM connections will be accepted.");
		}
		
		
		// main loop here.
		Logger.instance().log(Logger.E_INFORMATION,
				"Starting LRIT in 'Dormant' mode.");	
		
		domain2AStatus = "Dormant";
		domain2BStatus="Dormant";
		domain2CStatus="Dormant";
		whileDormant();

	}

	/**
	 * This method make LRIT dormant.
	 */
	public void runDormant() {
		
		isActive = false;
		lritconn.closeConnections();

		// Stops the file generator and file sender threads

		LritDcsMain.instance().getMessageThread.shutdown();
		LritDcsMain.instance().sendFileThread.shutdown();
		LritDcsMain.instance().scrubberThread.shutdown();
		LritDcsMain.instance().manualRetransThread.shutdown();
		//lqmServer.shutdown();
		try {
			if (lqmServer == null)
				lqmServer = new LqmInterfaceServer(cfg.getLqmPort());

			if (!cfg.getEnableLqm())				
				lqmServer.shutdown();

		} catch (Exception ex) {
			Logger.instance().failure(
					"LRIT:" + Constants.EVT_LQM_LISTEN_ERR
							+ "- Cannot create LQM Interface server: " + ex
							+ " -- No LQM connections will be accepted.");
		}
		
		Logger.instance().log(Logger.E_INFORMATION,
				"LRIT is running in  'Dormant' mode now.");

		whileDormant();
		
	}

	/**
	 * Runs LRIT in dormant mode.
	 */	
	public void whileDormant() {
		while (!shutdownFlag) {
			
			try {
				if (cfg.checkConfigFile()) {
				
					lqmServer.updateConfigDormant();				
					
				String strState = cfg.getFileSenderState();
				//System.out.println("Config Changed in dormant mode");
				if (strState.equalsIgnoreCase("dormant")) {
					if (isActive)
						runDormant();
				} else {
					if (!isActive)
						runActive();
				}
				}
				myStatus.rotateStatus();
				myStatus.serverGMT = System.currentTimeMillis();
				myStatus.status = "Dormant";
				myStatus.filesQueuedHigh = fileQueueHigh.size();
				myStatus.filesQueuedMedium = fileQueueMedium.size();
				myStatus.filesQueuedLow = fileQueueLow.size();
				myStatus.filesQueuedAutoRetrans = fileQueueAutoRetrans.size();
				myStatus.filesQueuedManualRetrans = fileQueueManualRetrans.size();
				myStatus.filesPending = fileNamesPending.size();
				myStatus.domain2Ahost = cfg.getDom2AHostName();
				myStatus.domain2Bhost = cfg.getDom2BHostName();
				myStatus.domain2Chost = cfg.getDom2CHostName();
				myStatus.domain2AStatus = domain2AStatus;
				myStatus.domain2BStatus = domain2BStatus;
				myStatus.domain2CStatus = domain2CStatus;
				myStatus.writeToFile();

				try {
					Thread.sleep(1000L);
				} catch (InterruptedException ex) {
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

		// Write the final status after everything is quiet.
		myStatus.status = "Shutdown";
		myStatus.writeToFile();
		
		//shutdown UIserver socket
		uiServer.shutdown();
		if (lqmServer != null)
		lqmServer.shutdown();
		

		// Release lock & die.
		Logger.instance().log(Logger.E_INFORMATION, "Exiting.");
		myServerLock.releaseLock();
		try {
			Thread.sleep(2000L);
		} catch (InterruptedException ex) {
		}

		System.exit(0);

	}
	
	
	// / This method will shut the entire application down.
	public void shutdown() {
		shutdownFlag = true;
	}

	// / Called from ServerLockable when the lock file is removed.
	public void lockFileRemoved() {
		Logger.instance().log(Logger.E_INFORMATION,
				"Retrieval Process Scheduler Exiting -- Lock File Removed.");
		shutdown();
	}

	// / Returns queue of log messages so UI server can distribute them.
	public QueueLogger getLogQueue() {
		return logQueue;
	}

	// / Returns true if LQM is connected or has been so within the last minute.
	public boolean isLqmConnected() {
		long now = System.currentTimeMillis();
		return now - myStatus.lastLqmContact < 60000L;
	}

	// ==================================================================
	// public getters for various component objects.
	// ==================================================================
	public FileQueue getFileQueueHigh() {
		return fileQueueHigh;
	}

	public FileQueue getFileQueueMedium() {
		return fileQueueMedium;
	}

	public FileQueue getFileQueueLow() {
		return fileQueueLow;
	}

	public FileQueue getFileQueueAutoRetrans() {
		return fileQueueAutoRetrans;
	}

	public FileQueue getFileQueueManualRetrans() {
		return fileQueueManualRetrans;
	}

	public LinkedList getFileNamesPending() {
		return fileNamesPending;
	}

	public ManualRetransThread getManualRetransThread() {
		return manualRetransThread;
	}

	public LritDcsStatus getStatus() {
		return myStatus;
	}

	/**
	 * @return the domain2AStatus
	 */
	public String getDomain2AStatus() {
		return domain2AStatus;
	}

	/**
	 * @param domain2AStatus the domain2AStatus to set
	 */
	public void setDomain2AStatus(String domain2AStatus) {
		this.domain2AStatus = domain2AStatus;
	}

	/**
	 * @param domain2AStatus the domain2AStatus to set
	 */
	public void setDomain2Status(String domain2Status, char domain2) {
		switch (domain2) {
        case 'a': 
        	this.domain2AStatus = domain2Status;
        	 break;
        case 'b': 
        	this.domain2BStatus = domain2Status;
        	break;
        case 'c': 
        	this.domain2CStatus = domain2Status;
        break;           
        default:
       Logger.instance().failure("Invalid domain.");
        break;
    }
	}
	/**
	 * @return the domain2BStatus
	 */
	public String getDomain2BStatus() {
		return domain2BStatus;
	}

	/**
	 * @param domain2BStatus the domain2BStatus to set
	 */
	public void setDomain2BStatus(String domain2BStatus) {
		this.domain2BStatus = domain2BStatus;
	}

	/**
	 * @return the domain2CStatus
	 */
	public String getDomain2CStatus() {
		return domain2CStatus;
	}

	/**
	 * @param domain2CStatus the domain2CStatus to set
	 */
	public void setDomain2CStatus(String domain2CStatus) {
		this.domain2CStatus = domain2CStatus;
	}

	public String getRsaKeyFile()
	{
		return rsaKeyFile;
	}

	private static DcsCmdLineArgs cmdLineArgs = new DcsCmdLineArgs(progname);
	

	public static void main(String args[])
	{
		StringToken rsaKeyArg = new StringToken("r", "rsa key file",
            "", TokenOptions.optSwitch, "$LRITDCS_HOME/ssh/id_rsa");
		cmdLineArgs.addToken(rsaKeyArg);
		cmdLineArgs.parseArgs(args);
		LritDcsMain.instance().rsaKeyFile = EnvExpander.expand(rsaKeyArg.getValue());
		
		LritDcsMain.instance().run();
	}
}
