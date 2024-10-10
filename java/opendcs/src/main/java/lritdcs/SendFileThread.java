/*
 *  $Id$
 *  
 *  $Log$
 *  Revision 1.12  2012/12/12 16:01:31  mmaloney
 *  Several updates for 5.2
 *
 *  Revision 1.11  2011/07/26 18:47:32  shweta
 *  added code to reconnect if any of domain 2 servers goes down.
 *
 *  Revision 1.10  2011/06/27 17:11:20  mmaloney
 *  Added info-level debugs to track how long it takes for SCP transfer and renaming on domain2.
 *:
 */
package lritdcs;

import ilex.util.FileUtil;
import ilex.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SendFileThread extends LritDcsThread 
{
	public static final String module = "SendFileThread";
	// Local copies of config vars.

	private String dom2ADirLow;
	private String dom2ADirMedium;
	private String dom2ADirHigh;

	private String dom2BDirLow;
	private String dom2BDirMedium;
	private String dom2BDirHigh;

	private String dom2CDirLow;
	private String dom2CDirMedium;
	private String dom2CDirHigh;

	private FileQueue highQ;
	private FileQueue mediumQ;
	private FileQueue lowQ;
	private FileQueue autoRetransQ;
	private FileQueue manualRetransQ;
	private LinkedList pendingList;

	private File highSentDir;
	private File mediumSentDir;
	private File lowSentDir;

	private LritDcsStatus myStatus;
	private int offStartSecOfHour;
	private int onStartSecOfHour;

	private static final String validPri = "HML";

	private LritDcsConfig cfg;
	private LritDcsConnection lritconn;

	boolean flgErr;
	FileStatsFile fileStatsFile = new FileStatsFile();

	public SendFileThread() 
	{
		super("SendFileThread");
	}

	public void init() 
		throws InitFailedException 
	{
		LritDcsMain theMain = LritDcsMain.instance();
		highQ = theMain.getFileQueueHigh();
		mediumQ = theMain.getFileQueueMedium();
		lowQ = theMain.getFileQueueLow();
		autoRetransQ = theMain.getFileQueueAutoRetrans();
		manualRetransQ = theMain.getFileQueueManualRetrans();
		pendingList = theMain.getFileNamesPending();
		lritconn = LritDcsConnection.instance();
		cfg = LritDcsConfig.instance();
		getConfigValues(cfg);
		// LritDcsConfig cfg = LritDcsConfig.instance();

		try {
			highSentDir = new File(cfg.getLritDcsHome() + File.separator
					+ "high.sent");
			if (!highSentDir.isDirectory())
				highSentDir.mkdirs();
		} catch (Exception ex) {
			throw new InitFailedException("Cannot create '"
					+ highSentDir.getPath() + "': " + ex);
		}

		try {
			mediumSentDir = new File(cfg.getLritDcsHome() + File.separator
					+ "medium.sent");
			if (!mediumSentDir.isDirectory())
				mediumSentDir.mkdirs();
		} catch (Exception ex) {
			throw new InitFailedException("Cannot create '"
					+ mediumSentDir.getPath() + "': " + ex);
		}

		try {
			lowSentDir = new File(cfg.getLritDcsHome() + File.separator
					+ "low.sent");
			if (!lowSentDir.isDirectory())
				lowSentDir.mkdirs();
		} catch (Exception ex) {
			throw new InitFailedException("Cannot create '"
					+ lowSentDir.getPath() + "': " + ex);
		}

		putOldMessagesInQueues();
		registerForConfigUpdates();
		myStatus = LritDcsMain.instance().getStatus();
		getConfigValues(cfg);

		flgErr = false;
	}

	public void run() 
	{
		while (!shutdownFlag) 
		{
			// Get highest-priority filename off of the queue.
			LritDcsFileStats fs = highQ.dequeue();
			if (fs != null)
				transferFile(fs, Constants.HighPri, true);
			else if ((fs = mediumQ.dequeue()) != null)
				transferFile(fs, Constants.MediumPri, true);
			else if ((fs = lowQ.dequeue()) != null)
				transferFile(fs, Constants.LowPri, true);
			else 
			{
				checkPending();
				if ((fs = autoRetransQ.dequeue()) != null) 
				{
					String fn = fs.getFile().getName();
					if (fn.length() >= 2 && validPri.indexOf(fn.charAt(1)) != -1)
						transferFile(fs, fn.charAt(1), false);
				}
				else if ((fs = manualRetransQ.dequeue()) != null) 
				{					
					transferFile(fs, Constants.LowPri, true);
				}
				else // Nothing to do, pause 500ms.
				{
					try
					{
						sleep(500L);
					}
					catch (InterruptedException ex)
					{
					}
					continue;
				}
			}
		}
	}

	/**
	 * Called from base-class update method whenever the configuration has
	 * changed. Find my parameters in the confiugration object and reset.
	 */
	protected synchronized void getConfigValues(LritDcsConfig cfg) {

		lritconn.getConfigValues(cfg);
		dom2ADirLow = cfg.getDom2ADirLow();
		dom2ADirMedium = cfg.getDom2ADirMedium();
		dom2ADirHigh = cfg.getDom2ADirHigh();

		dom2BDirLow = cfg.getDom2BDirLow();
		dom2BDirMedium = cfg.getDom2BDirMedium();
		dom2BDirHigh = cfg.getDom2BDirHigh();

		dom2CDirLow = cfg.getDom2CDirLow();
		dom2CDirMedium = cfg.getDom2CDirMedium();
		dom2CDirHigh = cfg.getDom2CDirHigh();

		offStartSecOfHour = onStartSecOfHour = 0;
	}

	private synchronized void transferFile(LritDcsFileStats fileStats,
		char priority, boolean trackPending) 
	{
		File localFile = fileStats.getFile();

		debug2("Transferring priority " + priority + " file '"
				+ localFile.getPath() + "'");
		
		String localDir = priority == Constants.HighPri ? cfg.getLritDcsHome()+"/high"
			: priority == Constants.MediumPri ? cfg.getLritDcsHome()+"/medium"
			: cfg.getLritDcsHome()+"/low";

		String targetDirA = priority == Constants.HighPri ? dom2ADirHigh
			: priority == Constants.MediumPri ? dom2ADirMedium
			: dom2ADirLow;

		String targetDirB = priority == Constants.HighPri ? dom2BDirHigh
			: priority == Constants.MediumPri ? dom2BDirMedium
			: dom2BDirLow;

		String targetDirC = priority == Constants.HighPri ? dom2CDirHigh
			: priority == Constants.MediumPri ? dom2CDirMedium
			: dom2CDirLow;
		
		// MJM 20070305: If !trackPending, then we've already sent this
		// once.
		// So we will be reading the file from the 'sent' folder.
		if (!trackPending)
			localDir = localDir + ".sent";

		// Use SCP to transfer file with no extension
		
		transferToDomain2(localFile, 'a', localDir, targetDirA, fileStats);
		transferToDomain2(localFile, 'b', localDir, targetDirB, fileStats);
		transferToDomain2(localFile, 'c', localDir, targetDirC, fileStats);
		
		fileStats.setAllTransfersCompleteTime(new Date());
		if (fileStats.getNumMessages() > 0)
		{
			// num messages will be > 0 only for newly constructed
			// files from the incoming message stream. It will be
			// 0 for files read (or re-read) out of the h/m/l dirs.
			fileStatsFile.append(fileStats);
			
		}

		LritDcsConfig cfg = LritDcsConfig.instance();
//		if (cfg.ptpEnabled)
//		{
//			File tmpFile = new File(cfg.ptpDir, localFile.getName() + ".tmp");
//			try
//			{
//				FileUtil.copyFile(localFile, tmpFile);
//				File dcsFile = new File(cfg.ptpDir, localFile.getName());
//				if (!tmpFile.renameTo(dcsFile))
//					Logger.instance().warning("SendFileThread Cannot rename '"
//						+ tmpFile.getPath() + "' to '" + dcsFile.getPath() + "'");
//				myStatus.ptpLastSave = System.currentTimeMillis();
//			}
//			catch (IOException ex)
//			{
//				Logger.instance().warning("SendFileThread Cannot create file for PTP '"
//					+ tmpFile.getPath() + "': " + ex);
//			}
//		}
		myStatus.lastFileName = localFile.getName();
		
		// Move the file into the 'sent' directory.
		if (priority == Constants.HighPri) 
		{
			if (trackPending)
				move(localFile, highSentDir);
			myStatus.incrementFileHigh();
		} 
		else if (priority == Constants.MediumPri) 
		{
			if (trackPending)
				move(localFile, mediumSentDir);
			myStatus.incrementFileMedium();
		} 
		else 
		{
			if (trackPending)
				move(localFile, lowSentDir);
			myStatus.incrementFileLow();
		}

		if (trackPending && cfg.getEnableLqm()
		 && LritDcsMain.instance().isLqmConnected()) 
		{
			long t = System.currentTimeMillis();
			int on = onStartSecOfHour;
			int off = offStartSecOfHour;

			if (on != off) {
				int soh = (int) (t / 1000L) % 3600; // second of hour

				if (on < off) {
					if (soh < on)
						t += ((on - soh) * 1000L);
					else if (soh >= off)
						t += ((on + 3600 - soh) * 1000L);
				} else // on > off
				{
					if (soh >= off && soh < on)
						t += ((on - soh) * 1000L);
				}
			}
			t += (cfg.getLqmPendingTimeout() * 1000L);

			debug1("Placing '" + localFile.getName() + "' on the pending list."
					+ " Will expire at " + new Date(t));
			synchronized (pendingList) {
				pendingList.addLast(new SentFile(localFile.getName(), t));
				if (pendingList.size() > 200)
					pendingList.removeFirst();
			}
		}
	}

	
	
	/**
	 * Transfers files from LRIT client to Domain2 servers
	 * @param localFile
	 * @param domain
	 * @param localDir
	 * @param targetDir
	 */
	private void transferToDomain2(File localFile, char domain,
	    String localDir, String targetDir, LritDcsFileStats fileStats)
	{
		Logger.instance().debug1(module + " File=" + localFile.getPath()
			+ ", size=" + localFile.length() + ", domain=" + domain);

		String dom2HostName = "";
		String domain2 = "";

		try
		{
			Connection connDom2 = null;
			SCPClient scp;
			Session sess =null;
			// Get local directory name and local file name.
//			File parentFile = localFile.getParentFile();
			String fname = localFile.getName();
			String connStatus = "";

			// Strip off & save the file's extension.
			int idx = fname.lastIndexOf('.');
			String extension = fname.substring(idx);
			fname = fname.substring(0, idx);

			switch (domain)
			{
			case 'a':
				connDom2 = lritconn.getConnDom2A();
				domain2 = "DOMAIN A";
				dom2HostName = cfg.getDom2AHostName();
				connStatus = lritconn.connAStatus;
				break;
			case 'b':
				connDom2 = lritconn.getConnDom2B();
				domain2 = "DOMAIN B";
				dom2HostName = cfg.getDom2BHostName();
				connStatus = lritconn.connBStatus;
				break;
			case 'c':
				connDom2 = lritconn.getConnDom2C();
				dom2HostName = cfg.getDom2CHostName();
				domain2 = "DOMAIN C";
				connStatus = lritconn.connBStatus;
				break;
			default:
				Logger.instance().failure("Invalid domain :" + domain);
				break;
			}

			if (connDom2 != null )
			{
				try
				{
				Logger.instance().debug1(module + " Making client connect to " + dom2HostName);
				scp = new SCPClient(connDom2);
				Logger.instance().debug1(module + " Connection established to " + dom2HostName);
				sess = connDom2.openSession();
				Logger.instance().debug1(module + " Started session to " + dom2HostName);
				
					Logger.instance().debug1(module + " sending " + localFile.getName());
					scp.put(localDir + "/" + localFile.getName(), fname,
					    targetDir, "0600");
					Logger.instance().debug1(module + " send complete for " + localFile.getName());
					
					Date now = new Date();
					if (domain == 'a')
					{
						myStatus.lastFileSendA = now.getTime();
						fileStats.setDom2AXferCompleteTime(now);
					}
					else if (domain == 'b')
					{
						myStatus.lastFileSendB = now.getTime();
						fileStats.setDom2BXferCompleteTime(now);
					}
					else if (domain == 'c')
					{
						myStatus.lastFileSendC = now.getTime();
						fileStats.setDom2CXferCompleteTime(now);
					}
					
					flgErr = false;
				}
				catch (Exception e)
				{
					Logger.instance().failure(
					    "File Transfer failed to " + domain2 + " :"
					        + dom2HostName + " . " + e.getMessage() + "."
					        + e.getCause() + ", constat=" + connStatus);
					LritDcsMain.instance()
					    .setDomain2Status("Error", domain);
					flgErr = true;
					if (domain == 'a')
						lritconn.setConnDom2A(null);
					
					if (domain == 'b')
						lritconn.setConnDom2B(null);
					
					if (domain == 'c')
						lritconn.setConnDom2C(null);
					

				}
				if (!flgErr)
				{
					String cmd = "mv " + targetDir + "/" + fname
				    + " " + targetDir + "/" + fname + extension;
					if (sess != null)
					{
						
						Logger.instance().debug1(module + " executing remote command '" +
							cmd + "' on " + dom2HostName);
						sess.execCommand(cmd);
						Logger.instance().debug1(module + " remote command complete '" +
							cmd + "' on " + dom2HostName);

						InputStream stderr = new StreamGobbler(sess
						    .getStderr());

						BufferedReader brerr = new BufferedReader(
						    new InputStreamReader(stderr));

						while (true)
						{
							String line = brerr.readLine();
							if (line == null)
								break;
							Logger.instance().failure(
							    "Command Failed ( "+cmd +" ) on " + domain2
							        + " :" + dom2HostName
							        + " . Exit Status : "
							        + sess.getExitStatus());
							Logger.instance().failure("ERROR !!! " + line);
							flgErr = true;
						}

						if (!flgErr)
						{
							LritDcsMain.instance().setDomain2Status(
							    "Active", domain);
							Date now = new Date();
							if (domain == 'a')
								fileStats.setDom2ARenameCompleteTime(now);
							else if (domain == 'b')
								fileStats.setDom2BRenameCompleteTime(now);
							else if (domain == 'c')
								fileStats.setDom2CRenameCompleteTime(now);
						}
						else
							LritDcsMain.instance().setDomain2Status(
							    "Error", domain);

						/* Close this session */
						stderr.close();
						brerr.close();						
						sess.close();
					}
					else
					{
						Logger.instance().failure(
						    "ERROR !!! No Session available to execute command ("+cmd+") on "
						        + domain2 + " :" + dom2HostName );
						       
						LritDcsMain.instance().setDomain2Status("Error",
						    domain);
						flgErr = true;

					}
				}
				else
				{						
					LritDcsMain.instance()
					    .setDomain2Status("Error", domain);
					if (sess != null)
						sess.close();
				}
			}
			else
			{
				
				long now = System.currentTimeMillis();
				LritDcsConnection ldc = LritDcsConnection.instance();
				if (domain == 'a')
				{
					if (now - ldc.lastDom2AConnAttempt > 30000L
					 && dom2HostName != null && dom2HostName.length() > 0)
					{
						ldc.connectSessionDom2A();
						if (ldc.getConnDom2A() == null)
						{
							Logger.instance().warning("No connection to "
								+ dom2HostName + ": " + connStatus);
							LritDcsMain.instance().setDomain2Status("Error", domain);
							flgErr = true;
						}
					}
					else
					{
//						Logger.instance().warning("No connection to "
//									+ dom2HostName + ": " + connStatus);
						LritDcsMain.instance().setDomain2Status("Error", domain);
						flgErr = true;
					}
				}
				else if (domain == 'b')
				{
					if (now - ldc.lastDom2BConnAttempt > 30000L
					 && dom2HostName != null && dom2HostName.length() > 0)
					{
						ldc.connectSessionDom2B();
						{
							Logger.instance().warning("No connection to "
								+ dom2HostName + ": " + connStatus);
							LritDcsMain.instance().setDomain2Status("Error", domain);
							flgErr = true;
						}
					}
					else
					{
//						Logger.instance().warning("No connection to "
//									+ dom2HostName + ": " + connStatus);
						LritDcsMain.instance().setDomain2Status("Error", domain);
						flgErr = true;
					}
				}
				else if (domain == 'c')
				{
					if (now - ldc.lastDom2CConnAttempt > 30000L
					 && dom2HostName != null && dom2HostName.length() > 0)
					{
						ldc.connectSessionDom2C();
						if (ldc.getConnDom2C() == null)
						{
							Logger.instance().warning("No connection to "
								+ dom2HostName + ": " + connStatus);
							LritDcsMain.instance().setDomain2Status("Error", domain);
							flgErr = true;
						}
					}
					else
					{
//						Logger.instance().warning("No connection to "
//									+ dom2HostName + ": " + connStatus);
						LritDcsMain.instance().setDomain2Status("Error", domain);
						flgErr = true;
					}
				}
			}
		}

		catch (Exception e)
		{

			if (!cfg.getFileSenderState().equalsIgnoreCase("dormant"))
			{
				Logger.instance().failure(
					"ERROR !!! FILE TRANSFER TO " + domain2 + " :" + dom2HostName
			        + " . " + e.getMessage() + "." + e.getCause());
				LritDcsMain.instance().setDomain2Status("Error", domain);
				flgErr = true;
			}
		}
	}

	private void move(File orig, File targetDir)
	{
		// If already in the correct dir, do nothing.
		if (orig.getParentFile().equals(targetDir))
			return;

		File target = new File(targetDir, orig.getName());
		debug2("Moving '" + orig.getPath() + "' to '" + target.getPath() + "'");
		try
		{
			FileOutputStream fos = new FileOutputStream(target);
			FileInputStream fis = new FileInputStream(orig);
			byte buf[] = new byte[4096];
			int len;
			while ((len = fis.read(buf)) > 0)
				fos.write(buf, 0, len);
			fos.close();
			fis.close();
			orig.delete();
		}
		catch (Exception ex)
		{
			if (!cfg.getFileSenderState().equalsIgnoreCase("dormant"))
				failure(Constants.EVT_FILE_MOVE_ERR, "- Error moving '"
				    + orig.getPath() + "' to '" + target.getPath() + "': " + ex);
		}
	}

//	private void requeue(File f, char priority) {
//		if (priority == Constants.HighPri)
//			highQ.enqueue(f);
//		else if (priority == Constants.MediumPri)
//			mediumQ.enqueue(f);
//		else
//			lowQ.enqueue(f);
//	}
//
//	private boolean execAndWait(String cmd) {
//		debug2("Executing '" + cmd + "'");
//		int exitStatus;
//		try {
//			Process proc = Runtime.getRuntime().exec(cmd);
//			final InputStream is = proc.getInputStream();
//			final InputStream es = proc.getErrorStream();
//			Thread isr = new Thread() {
//				public void run() {
//					try {
//						byte buf[] = new byte[1024];
//						int n = is.read(buf);
//						if (n > 0)
//							warning(0, "stdout returned(" + n + ") '"
//									+ new String(buf, 0, n) + "'");
//					} catch (IOException ex) {
//					}
//				}
//			};
//			isr.start();
//			Thread esr = new Thread() {
//				public void run() {
//					try {
//						byte buf[] = new byte[1024];
//						int n = es.read(buf);
//						if (n > 0)
//							warning(0, "stderr returned(" + n + ") '"
//									+ new String(buf, 0, n) + "'");
//					} catch (IOException ex) {
//					}
//				}
//			};
//			esr.start();
//
//			exitStatus = proc.waitFor();
//		} catch (IOException ex) {
//			warning(Constants.EVT_SSH_EXEC_ERR, "- Error executing '" + cmd
//					+ "': " + ex);
//			return false;
//		} catch (InterruptedException ex) {
//			warning(Constants.EVT_SSH_EXEC_ERR,
//					"- Wait interrupted, aborting transfer.");
//			return false;
//		}
//
//		// If bad exit code, issue warning.
//		if (exitStatus == 0) {
//			debug2("Execution of '" + cmd + "' SUCCESS.");
//			return true;
//		} else {
//			warning(Constants.EVT_SSH_EXEC_ERR, "- Execution of '" + cmd
//					+ "' failed with exit status " + exitStatus);
//			return false;
//		}
//	}

	/**
	 * Called once at start-up, this method puts any existing files in the
	 * queues so that they will be transfered.
	 */
	public void putOldMessagesInQueues() {
		LritDcsConfig cfg = LritDcsConfig.instance();

		File f = new File(cfg.getLritDcsHome() + File.separator + "high");
		File list[] = f.listFiles();
		for (int i = 0; list != null && i < list.length; i++)
			if (list[i].getName().startsWith("p" + Constants.HighPri)) {
				debug2("Queuing existing file '" + list[i].getPath() + "'");
				highQ.enqueue(list[i]);
			}

		f = new File(cfg.getLritDcsHome() + File.separator + "medium");
		list = f.listFiles();
		for (int i = 0; list != null && i < list.length; i++)
			if (list[i].getName().startsWith("p" + Constants.MediumPri)) {
				debug2("Queuing existing file '" + list[i].getPath() + "'");
				mediumQ.enqueue(list[i]);
			}

		f = new File(cfg.getLritDcsHome() + File.separator + "low");
		list = f.listFiles();
		for (int i = 0; list != null && i < list.length; i++)
			if (list[i].getName().startsWith("p" + Constants.LowPri)) {
				debug2("Queuing existing file '" + list[i].getPath() + "'");
				lowQ.enqueue(list[i]);
			}
	}

	private void checkPending() {
		long now = System.currentTimeMillis();
		LritDcsConfig cfg = LritDcsConfig.instance();
		try {

			// If LQM not connected, flush Pending queue & warn user.
			if (!LritDcsMain.instance().isLqmConnected()) {
				if (pendingList.size() > 0) {
					warning(
							Constants.EVT_LQM_CON_LOST,
							"- "
									+ pendingList.size()
									+ " files removed from "
									+ "Pending Queue because LQM connection has been lost.");
					pendingList.clear();
				}
				return;
			}

			// note: list always sorted by time with oldest at start.
			synchronized (pendingList) {
				SentFile sf = (SentFile) pendingList.getFirst();
				if (now > sf.expireTime) {
					pendingList.removeFirst();
					if (sf.filename.length() > 2 && cfg.getEnableLqm()) {
						char pri = sf.filename.charAt(1);
						if (validPri.indexOf(pri) != -1) {
							File f = new File(
									pri == Constants.HighPri ? highSentDir
											: pri == Constants.MediumPri ? mediumSentDir
													: lowSentDir, sf.filename);
							autoRetransQ.enqueue(f);
							info(Constants.EVT_PENDING_TIMEOUT,
									"- Pending Timeout on file '" + f.getName()
											+ "'");
						}
					}
				}
			}
		} catch (NoSuchElementException ex) {
		}
	}
}
