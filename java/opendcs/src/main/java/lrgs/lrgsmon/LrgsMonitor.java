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
package lrgs.lrgsmon;

import java.io.*;
import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.DecodesVersion;
import ilex.util.EnvExpander;
import ilex.util.ServerLock;
import ilex.util.FileServerLock;
import ilex.util.TextUtil;
import lrgs.statusxml.LrgsStatusSnapshotExt;

/**
Main class for the Lrgs Monitor web application.
*/
public class LrgsMonitor extends ThreadBase
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Config param: period between LRGS scans */
	private int scanSeconds;

	/** File object constructed from name. */
	private File lrgsListFile;

	/** Config param: directory in which to place output HTML file */
	private File outputDir;

	/** Vector of LrgsMonThread objects constructed from the LRGS list file. */
	private ArrayList<LrgsMonThread> lrgsList = new ArrayList<LrgsMonThread>();

	/** Used to generated the detailed per-LRGS reports. */
	private DetailReportGenerator dRptGen;

	/** Used to generated the Summary reports. */
	private SummaryReportGenerator sRptGen;

	/** Prevents multiple simultaneous instances */
	private String lockFileName;

	/** Prevents multiple simultaneous instances */
	private ServerLock mylock;

	/** Script to run after generating summary */
	private String afterSummaryScript = null;

	/// Construct main Lrgs Monitor.
	public LrgsMonitor(String conf, String outputDir, int scanSeconds,
		String lockFileName, String iconFileName, String headerFileName,
		String summaryHeaderFile)
	{
		super("LrgsMonitor");
		setLrgsListFileName(conf);
		setOutputDir(outputDir);
		this.scanSeconds = scanSeconds;
		this.lockFileName = lockFileName;

		String fn = EnvExpander.expand(iconFileName);
		sRptGen = new SummaryReportGenerator(fn);
		dRptGen = new DetailReportGenerator(fn);

		if (headerFileName != null)
		{
			fn = EnvExpander.expand(headerFileName);
			try { dRptGen.setHeader(fn); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Cannot open specified header file '{}' -- will use default header.", fn);
				try { dRptGen.setHeader(null); }
				catch(Exception ignore) {}
			}
		}

		if (summaryHeaderFile != null)
		{
			fn = EnvExpander.expand(summaryHeaderFile);
			try { sRptGen.setHeader(fn); }
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Cannot open specified Summary header file '{}' -- will use default Summary header.",
				   		fn);
				try { sRptGen.setHeader(null); }
				catch(Exception ignore) {}
			}
		}
	}

	public void setLrgsListFileName(String nm)
	{
		lrgsListFile = new File(nm);
	}

	public void setOutputDir(String dir)
	{
		outputDir = new File(dir);
	}

	public static void main(String[] args)
	{
		// This parses all args & sets up the logger & debug level.
		LrgsMonCmdLineArgs cmdLineArgs = new LrgsMonCmdLineArgs();
		cmdLineArgs.parseArgs(args);

		log.info("LRGS Monitor Starting, OpenDCS Version {}", DecodesVersion.startupTag());

		// Instantiate & run my monitor.
		LrgsMonitor mymonitor=new LrgsMonitor(
			cmdLineArgs.getLrgsListFile(),
			cmdLineArgs.getOutputDir(),
			cmdLineArgs.getScanPeriod(),
			cmdLineArgs.getLockFile(),
			cmdLineArgs.getIconFile(),
			cmdLineArgs.getHeaderFile(),
			cmdLineArgs.getSummaryHeaderFile());

		// Even though it's a Thread, run it in the main thread.
		mymonitor.run();
	}

	/**
	  Called from static main, this is the main thread run method.
	  The main thread handles setting up the environment, spawning
	  other threads to monitor individual LRGS's, and periodically
	  generating the reports.
	*/
	public void run()
	{
		// Get the server lock, & fail if error.
		String lockpath = EnvExpander.expand(lockFileName);
		mylock = new FileServerLock(lockpath);

		if (mylock.obtainLock() == false)
		{
			log.error("API Server not started: lock file busy");
			System.exit(0);
		}
		mylock.releaseOnExit();
		Runtime.getRuntime().addShutdownHook(
			new Thread()
			{
				public void run()
				{
					log.info("LRGS Monitor Server exiting {}",
							(mylock.wasShutdownViaLock() ? "(lock file removed)" : ""));
				}
			});


		if (!outputDir.isDirectory())
		{
			outputDir.mkdirs();
			if (!outputDir.isDirectory())
			{
				log.error("Cannot access or create output directory '{}' -- aborting.", outputDir.getPath());
				System.exit(1);
			}
		}

		if (!lrgsListFile.canRead())
		{
			log.warn("No list file, No monitoring will be done.");
		}
		else
			loadList();

		long lastListCheck = 0L;
		long lastListLoad = 0L;
		long lastReportGen = 0L;
		while(true)
		{
			try{Thread.sleep(1000L);}
			catch(Exception e){}
			long now = System.currentTimeMillis();

			// Check for LRGS list changes once per minute.
			if (now - lastListCheck > 10000L
			 && lrgsListFile.canRead()
			 && lastListLoad < lrgsListFile.lastModified())
			{
				lastListLoad = now;
				loadList();
			}

			if (now - lastReportGen > (scanSeconds * 1000L))
			{
				lastReportGen = now;
				generateSummary(scanSeconds);
			}
		}
	}

	/**
	  Called from the LrgsMonThreads when they get a status update from
	  the remote server. Parse the XML message and save the status structure
	  for this host.
	*/
	public synchronized void updateStatus(String host,
		LrgsStatusSnapshotExt status, String extHost)
	{
		log.debug("Received updated status from {}", host);

		// Generate a detail file for this host.
		File outfile = new File(outputDir, host + ".html");
		dRptGen.write(outfile, host, status, scanSeconds);

		// Generate a table-entry in the summary file for this host.
		sRptGen.update(host, status, extHost);
	}

	public synchronized void generateSummary(int scanSeconds)
	{
		sRptGen.write(new File(outputDir, "LrgsSummaryStatus.html"), scanSeconds);
		if (afterSummaryScript != null && afterSummaryScript.length() > 0)
		{
			try
			{
				Runtime.getRuntime().exec(afterSummaryScript);
			}
			catch (IOException ex)
			{
				log.atWarn().setCause(ex).log("Cannot execute '{}'", afterSummaryScript);
			}
		}
	}

	/**
	  Reads the LRGS list file and constructs a LrgsMonThread for
	  each host specified.
	  Entries in the file have the form: host port username [password]
	  Port may be -, in which case it defaults to 16003.
	*/
	private void loadList()
	{
		log.info("Loading configuration.");

		// discard collection of LrgsMonThreads
		while(lrgsList.size() > 0)
		{
			LrgsMonThread dmt = lrgsList.get(0);
			dmt.shutdown();
			lrgsList.remove(0);
		}
		sRptGen.clear();
		afterSummaryScript = null;

		try (FileReader fr= new FileReader(lrgsListFile);
			 LineNumberReader lnr = new LineNumberReader(fr))
		{
			// Read file with BufferedInputStream
			

			// For each line create & start new LrgsMonThread
			String line;
			while((line = lnr.readLine()) != null)
			{
				line = line.trim();
				if (line.length() == 0 || line.charAt(0) == '#')
					continue;

				// Check for special named properties
				if (TextUtil.startsWithIgnoreCase(line, "afterSummaryScript="))
				{
					afterSummaryScript = line.substring(line.indexOf('=')+1);
					continue;
				}

				StringTokenizer st = new StringTokenizer(line);
				int n = st.countTokens();
				if (n < 3 || n > 5)
				{
					log.warn("{}({}): Invalid line '{}', expected host port user passwd exthost",
							 lrgsListFile.getPath(), lnr.getLineNumber(), line);
					continue;
				}
				String host = st.nextToken();
				int port = 16003;
				String s = st.nextToken();
				if (Character.isDigit(s.charAt(0)))
				{
					try { port = Integer.parseInt(s); }
					catch(NumberFormatException ex)
					{
						log.atWarn()
						   .setCause(ex)
						   .log("{}({}): Bad port number -- defaulted to 16003.",
						   		lrgsListFile.getPath(), lnr.getLineNumber());
						port = 16003;
					}
				}
				String user = st.nextToken();

				String passwd = null;
				if (n > 3)
				{
					s = st.nextToken();
					if (!s.equals("-"))
						passwd = s;
				}
				String extHost = null;
				if (n > 4)
					extHost = st.nextToken();

				addLRGS(
					new LrgsMonThread(this, host, port, user, passwd, extHost),
						host);
			}
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("IO Error reading '{}'", lrgsListFile.getPath());
		}
	}

	private void addLRGS(LrgsMonThread lmt, String host)
	{
		lrgsList.add(lmt);
		lmt.start();
		sRptGen.monitor(host);
	}

	public int getScanSeconds() { return scanSeconds; }
}