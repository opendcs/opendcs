/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lritdcs;

import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.QueueLogger;
import ilex.util.ServerLock;
import ilex.util.FileServerLock;
import ilex.util.ServerLockable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

public class LritDcsMain implements ServerLockable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		myServerLock = new FileServerLock(lockName);
		if (!myServerLock.obtainLock(this))
		{
			log.error("Lock file '{}' already taken. Is another instance of '{}' already running?",
					  lockName, progname);
			System.exit(1);
		}


		try
		{
			//final UIServer uiServer = new UIServer(cfg.getLritUIPort());
			uiServer = new UIServer(cfg.getLritUIPort());
			Thread uiServerThread = new Thread()
			{
				public void run() {
					try
					{
						uiServer.listen();
					}
					catch (IOException ex)
					{
						log.atError()
						   .setCause(ex)
						   .log("LRIT:{}- User Interface Listening Socket Error.", Constants.EVT_UI_LISTEN_ERR);
					}
				}
			};
			uiServerThread.start();

		}
		catch (IOException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("LRIT:{}- Cannot create User Interface server.", Constants.EVT_UI_LISTEN_ERR);
			System.exit(1);
		}
	}





	/**
	 * This method starts LRIT in active mode
	 */

	public void startActiveMode() {

		isActive = true;
		File cfgFile = cfg.getConfigFile();
		File tf = new File(cfgFile.getPath() + ".tmp");
		try
		{
			FileUtil.copyFile(cfgFile, tf);
			try (PrintWriter tw = new PrintWriter(new FileOutputStream(tf)))
			{
				cfg.saveState(tw, "Active");
			}
			FileUtil.moveFile(tf, cfgFile);
			log.info("Saved LRIT state to {}", cfgFile.getPath());
			cfg.load();
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Error saving LRIT state in config file.");
			return;
		}


		initialize();
		lritconn = LritDcsConnection.instance();
		// Create & initialize the threads.
		getMessageThread = new GetMessageThread();
		sendFileThread = new SendFileThread();
		scrubberThread = new ScrubberThread();
		manualRetransThread = new ManualRetransThread();
		try
		{
			getMessageThread.init();
			sendFileThread.init();
			scrubberThread.init();
			manualRetransThread.init();
		}
		catch (InitFailedException ex)
		{
			log.atError().setCause(ex).log("Init Failed.");
			System.exit(1);
		}

		getMessageThread.start();
		sendFileThread.start();
		scrubberThread.start();
		manualRetransThread.start();

		try
		{
			lqmServer = new LqmInterfaceServer(cfg.getLqmPort());
			if (cfg.getEnableLqm())
				lqmServer.startListeningThread();
			else
				lqmServer.shutdown();
		}
		catch (Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("LRIT:{}- Cannot create LQM Interface server -- No LQM connections will be accepted.",
			   		Constants.EVT_LQM_LISTEN_ERR);
		}

		// main loop here.
		log.info("Starting LRIT in 'Active' mode.");

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

			try
			{
				getMessageThread.init();
				sendFileThread.init();
				scrubberThread.init();
				manualRetransThread.init();
			}
			catch (InitFailedException ex)
			{
				log.atError().setCause(ex).log("Init Failed.");
				System.exit(1);
			}

			getMessageThread.start();
			sendFileThread.start();
			scrubberThread.start();
			manualRetransThread.start();

			try
			{
				if (lqmServer == null)
					lqmServer = new LqmInterfaceServer(cfg.getLqmPort());

				if (cfg.getEnableLqm())
					lqmServer.startListeningThread();
				else
					lqmServer.shutdown();

			}
			catch (Exception ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("LRIT:{}- Cannot create LQM Interface server -- No LQM connections will be accepted.",
				   		Constants.EVT_LQM_LISTEN_ERR);
			}

			// main loop here.
			log.info(	"LRIT is running in  'Active' mode now.");
			long lastStatusSave = System.currentTimeMillis();
			whileActive();
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unexpected error.");
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

		try {			Thread.sleep(1000L);
		} catch (InterruptedException ex) {
		}
		}
		getMessageThread.shutdown();

		while (getMessageThread.isAlive())
		{
			log.debug("Waiting for message thread to terminate.");
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException ex) {
			}
		}

		sendFileThread.shutdown();
		while (sendFileThread.isAlive())
		{
			log.debug("Waiting for SendFileThread to terminate.");
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
		log.info("Exiting.");
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
		File cfgFile = cfg.getConfigFile();
		File tf = new File(cfgFile.getPath() + ".tmp");
		try
		{
			FileUtil.copyFile(cfgFile, tf);
			try (PrintWriter tw = new PrintWriter(new FileOutputStream(tf)))
			{
				cfg.saveState(tw, "Dormant");
			}
			FileUtil.moveFile(tf, cfgFile);
			log.info("Saved LRIT state to {}", cfgFile.getPath());
			cfg.load();
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Error saving LRIT state in config file.");
			return;
		}

		initialize();
		try
		{
			if (lqmServer == null)
				lqmServer = new LqmInterfaceServer(cfg.getLqmPort());

			if (!cfg.getEnableLqm())
				lqmServer.shutdown();

		}
		catch (Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("LRIT:{}- Cannot create LQM Interface server -- No LQM connections will be accepted.",
			   		Constants.EVT_LQM_LISTEN_ERR);
		}


		// main loop here.
		log.info("Starting LRIT in 'Dormant' mode.");

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

		try
		{
			if (lqmServer == null)
				lqmServer = new LqmInterfaceServer(cfg.getLqmPort());

			if (!cfg.getEnableLqm())
				lqmServer.shutdown();

		}
		catch (Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("LRIT:{}- Cannot create LQM Interface server -- No LQM connections will be accepted.",
			   		Constants.EVT_LQM_LISTEN_ERR);
		}

		log.info("LRIT is running in  'Dormant' mode now.");

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
			}
			catch (Exception ex)
			{
				log.atTrace().setCause(ex).log("Unexpected error while dormant");
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
		log.info("Exiting.");
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
		log.info("Retrieval Process Scheduler Exiting -- Lock File Removed.");
		shutdown();
	}

	// / Returns queue of log messages so UI server can distribute them.
	public QueueLogger getLogQueue() {
		return null;
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
	 * @param domain2Status the domain2AStatus to set
	 * @param domain2 'a', 'b', or 'c'
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
       		log.error("Invalid domain.");
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