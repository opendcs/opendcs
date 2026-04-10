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
package decodes.routmon;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.PropertiesUtil;
import ilex.util.EnvExpander;
import ilex.util.ServerLock;
import ilex.util.FileServerLock;
import ilex.cmdline.*;
import decodes.util.*;

/**
Main class for the Routing Spec Monitor web application.
*/
public class RoutingMonitor	implements Runnable
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Config param: period between directory scans */
	private long scanPeriod;

	/** Config param: directory to be scanned */
	private Vector directories=new Vector();

	/** Config param: directory in which to place output HTML file */
	private File outputDir;

	/** Config param: directory to be scanned for routing status files. */
	private File inputDir;

	/** Config param: Name of file to create in the outputDir */
	private String htmlFilename;

	/** Config param: hostname to use in report header */
	private String hostname;

	/** Config param: purge routstat files older than this many seconds. */
	private long purgetime;

	/** Used to format dates in the header of the HTML report. */
	private SimpleDateFormat dfHeader;

	/** Used to format dates in the columns of the HTML report. */
	private SimpleDateFormat dfColumn;

	/** Output File object. */
	private File htmlFile;

	/** monitor object */
	RoutingDirMonitor myDirMonitor=new RoutingDirMonitor(this);

	/** collection of status objects, one per routing spec. */
	private Vector routingSpecStats=new Vector();

	/** Prevents multiple instances & provide way to shut down. */
	private ServerLock mylock;

	/// Command line arguments, set default log name to ./routmon.log
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "routmon.log");
	static private StringToken host_arg = 
		new StringToken("h", "host-name", "", TokenOptions.optSwitch, "");
	static private StringToken outputDir_arg = 
		new StringToken("o", "Output Directory","", TokenOptions.optSwitch, 
			"$DECODES_INSTALL_DIR/routmon");
	static private StringToken outputFile_arg = 
		new StringToken("f", "Output File","", TokenOptions.optSwitch, 
			"routmon.html");
	static private IntegerToken sleepSecs_arg = 
		new IntegerToken("s", "Sleep Seconds","", TokenOptions.optSwitch, 10);
	static private IntegerToken purgeSecs_arg = 
		new IntegerToken("p", "Purge Seconds","", TokenOptions.optSwitch, 
			3600*24);
	/** -k argument provides the lock file */
	static StringToken lockFileArg = new StringToken(
		"k", "dcpmon lock file","",TokenOptions.optSwitch,"$HOME/routmon.lock");
	static
	{
		cmdLineArgs.addToken(host_arg);
		cmdLineArgs.addToken(outputDir_arg);
		cmdLineArgs.addToken(outputFile_arg);
		cmdLineArgs.addToken(sleepSecs_arg);
		cmdLineArgs.addToken(purgeSecs_arg);
		cmdLineArgs.addToken(lockFileArg);
	}
	
	/**
	 * Main method. See command line arguments listed above.
	 * @param args command line args
	 */
	public static void main(String[] args)
	{
		// This parses all args & sets up the logger & debug level.
		cmdLineArgs.parseArgs(args);
		
		log.debug("starting");

		// Instantiate my monitor.
		RoutingMonitor mymonitor=new RoutingMonitor();

		// Put argument values into monitor.
		mymonitor.scanPeriod = sleepSecs_arg.getValue();
		mymonitor.outputDir = 
			new File(EnvExpander.expand(outputDir_arg.getValue()));
		mymonitor.htmlFilename = outputFile_arg.getValue();
		mymonitor.hostname = host_arg.getValue().trim();
		if (mymonitor.hostname.length() == 0)
		{
			try
			{
				mymonitor.hostname = InetAddress.getLocalHost().getHostName();
			}
			catch(UnknownHostException ex)
			{
				log.atWarn().setCause(ex).log("Cannot determine host name -- using 'unknown'");
				mymonitor.hostname = "unknown";
			}
		}
		mymonitor.purgetime = purgeSecs_arg.getValue();

		// Run the monitor in the main thread.
		mymonitor.run();
	}
	
	/**
	 * Application run method reads configuration, starts monitor thread, etc.
	 */
	public void run()
	{
		// Get the server lock, & fail if error.
		String lockpath = EnvExpander.expand(lockFileArg.getValue());
		mylock = new FileServerLock(lockpath);

		if (mylock.obtainLock() == false)
		{
			log.error("Routing Monitor not started: lock file busy");
			System.exit(1);
		}

		mylock.releaseOnExit();
		Runtime.getRuntime().addShutdownHook(
			new Thread()
			{
				public void run()
				{
					log.info("Routing Monitor Server exiting {}",
							(mylock.wasShutdownViaLock() ? "(lock file removed)" : ""));
				}
			});

		TimeZone utc = TimeZone.getTimeZone("UTC");
		dfHeader = new SimpleDateFormat("MM/dd/yyyy  HH:mm:ss zzz");
		dfHeader.setTimeZone(utc);

		dfColumn = new SimpleDateFormat("MM/dd HH:mm:ss");
		dfColumn.setTimeZone(utc);

		if (!outputDir.isDirectory())
		{
			outputDir.mkdirs();
			if (!outputDir.isDirectory())
			{
				log.error("Cannot access or create output directory '{}' -- aborting.", outputDir.getPath());
				System.exit(1);
			}
		}

		htmlFile = new File(outputDir, htmlFilename);

		// create, configure, & start the dir monitor thread.
		myDirMonitor = new RoutingDirMonitor(this);
		inputDir = new File(EnvExpander.expand(
			DecodesSettings.instance().routingStatusDir));
		if (!inputDir.isDirectory())
		{
			inputDir.mkdirs();
			if (!inputDir.isDirectory())
			{
				log.error("Cannot access or create input directory '{}' -- aborting.", inputDir.getPath());
				System.exit(1);
			}
		}
		myDirMonitor.addDirectory(inputDir);
		myDirMonitor.setSleepEveryCycle(true);
		myDirMonitor.setSleepInterval(scanPeriod*1000);
		myDirMonitor.start();

		while(true)
		{
			try{Thread.sleep(60000);}
			catch(Exception e){}
			purgeOldStatusRecs();
		}
		
	}
	
	
	/*
	 * Called from the dir monitor thread with a properties set read from
	 * a routing-spec's status file.
	 * Extract the relevant properties & store them in a RoutingSpecStatus 
	 * object in my vector.
	 *
	 * @param props the properties representing routing spec status
	 * @param lastModifyTime last time this rout stat set was changed.
	 */
	public synchronized void setStatus(Properties props, long lastModifyTime)
		throws BadStatusFile
	{
		/*
		  Concurrency check: we don't want to try to read a file that isn't
		  complete. The set must contain start & end times and they must
		  be equal.
		*/
		String sts = PropertiesUtil.getIgnoreCase(props, "StartTime");
		String ets = PropertiesUtil.getIgnoreCase(props, "EndTime");
		if (sts == null || ets == null)
			throw new BadStatusFile("Missing start or end time.");
		if (!sts.equals(ets))
			throw new BadStatusFile("Start time doesn't equal end time.");

		String specName = PropertiesUtil.getIgnoreCase(props, "SpecName");
		if (specName == null)
			throw new BadStatusFile("Missing required 'SpecName' property.");

		RoutingSpecStatus myspec = null;
		for(int pos=0;pos<routingSpecStats.size();pos++)
		{
			RoutingSpecStatus tempstats=
				(RoutingSpecStatus)routingSpecStats.get(pos);
			if (tempstats.name.equalsIgnoreCase(specName))
			{
				myspec=tempstats;
				break;
			}
		}

		// Not found? Must be new -- Add it to the vector.
		if(myspec==null)
		{
			log.info("New routing spec status '{}'", specName);
			myspec = new RoutingSpecStatus(specName);
			routingSpecStats.add(myspec);
		}

		String s = PropertiesUtil.getIgnoreCase(props, "Status");
		if (s != null)
			myspec.status = s;
		s = PropertiesUtil.getIgnoreCase(props, "CurrentServer");
		if (s != null)
			myspec.currentServer = s;
		s = PropertiesUtil.getIgnoreCase(props, "Format");
		if (s != null)
			myspec.outputFormat = s;
		s = PropertiesUtil.getIgnoreCase(props, "Output");
		if (s != null)
			myspec.outputName = s;

		s = PropertiesUtil.getIgnoreCase(props, "LastRecvTime");
		if (s != null)
		{
			try { myspec.lastMsgRecieveTime = new Date(Long.parseLong(s)); }
			catch(NumberFormatException ex)
			{
				log.atWarn().setCause(ex).log("Invalid LastRecvTime long integer '{}' -- skipped.", s);
			}
		}

		s = PropertiesUtil.getIgnoreCase(props, "RunStartTime");
		if (s != null)
		{
			try { myspec.startTime = new Date(Long.parseLong(s)); }
			catch(NumberFormatException ex)
			{
				log.atWarn().setCause(ex).log("Invalid RunStartTime long integer '{}' -- skipped.", s);
			}
		}

		s = PropertiesUtil.getIgnoreCase(props, "NumMsgsRun");
		if (s != null)
			myspec.msgsThisRun = s;

		s = PropertiesUtil.getIgnoreCase(props, "NumMsgsToday");
		if (s != null)
			myspec.msgsToday = s;

		myspec.lastStatusChangeMsec = lastModifyTime;
	}
	
	/**
	 * Called from the monitor thread at completion of directory scan, this
	 * method generates the HTML report.
	 */
	public synchronized void generateReport()
	{
		Collections.sort(routingSpecStats);
		String htmlHeader =
			  "<html>\n"
			+ "<head>\n"
			+ "  <title>Routing Status Monitor</title>\n"
			+ "  <meta http-equiv=\"refresh\" CONTENT=\"" + scanPeriod + "\">\n"
			+ "  <meta http-equiv=\"Content-Type\" content=\"text/html; "
				+ "charset=iso-8859-1\">\n"
			+ "</head>\n";

		String htmlTitleArea
		= "<table cellpadding=\"2\" cellspacing=\"2\" border=\"0\"\n"
		+ "style=\"text-align: left; width: 984px; height: 59px;\">\n"
		+ "<tbody>\n"
		+ "<tr>\n"
			+ "<td style=\"vertical-align: top;\"><img\n"
 			+ "src=\"gears.png\"\n"
 			+ "alt=\"Decodes Icon\" style=\"width: 100px; height: 100px;\"><br>\n"
			+ "</td>\n"
			+ "<td style=\"vertical-align: top; text-align: center;\">\n"
			+ "<h2 style=\"text-align: center;\">DECODES Routing Spec Status</h2>\n"
			+ "Hostname: " + hostname + "<br>\n"
			+ dfHeader.format(new Date(System.currentTimeMillis())) + "<br>\n"
			+ "</td>\n"

			+ "<td style=\"vertical-align: top; text-align: right;\"><br>\n"
   			+ "</td>\n"
   		+ "</tr>\n"
		+ "</tbody>\n"
		+ "</table>\n"
		+ "<br>\n";

		String htmlTableStart
			= "<table cellpadding=\"2\" cellspacing=\"2\" border=\"1\"\n"
 			+ "style=\"text-align: left; width: 100%;\">\n"
  			+ "<tbody>";

		String htmlTableHeader
		= "<tr>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Routing\n"
		+ "Spec Name<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Current\n"
		+ "Status<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Time\n"
		+ "Started<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Last\n"
		+ "Msg Rcv'd<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">\n"
		+ "Msgs/Errs This Run<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">"
		+ "Msgs/Errs Today<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Current\n"
		+ "Server<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">Output\n"
		+ "Name<br>\n"
		+ "</td>\n"
		+ "<td\n"
		+ "style=\"vertical-align: top; font-weight: bold; text-align: center; font-style: italic;\">OutputFormat<br>\n"
		+ "</td>\n"
		+ "</tr>\n";

		Vector tableRows = new Vector();

		Enumeration e = routingSpecStats.elements();
		while(e.hasMoreElements())
		{
			RoutingSpecStatus myspec=(RoutingSpecStatus)e.nextElement();
			String row = 
				"<tr><td style=\"vertical-align: top; width: 10%;\">" 
					+ "<a href=\"../" + inputDir.getName() + "/" 
						+ (myspec.name.toLowerCase() + ".log") + "\">"
					+ myspec.name + "</a><br></td>"
				+ "<td style=\"vertical-align: top; width: 8%; color: rgb(0, 102, 0);\">" + myspec.status + "</td>"
				+ "<td style=\"vertical-align: top; width: 12%;\">" + dfColumn.format(myspec.startTime) + "</td>"
				+ "<td style=\"vertical-align: top; width: 12%;\">" + dfColumn.format(myspec.lastMsgRecieveTime) + "</td>"
				+ "<td style=\"vertical-align: top; width: 8%;\">" + myspec.msgsThisRun + "</td>"
				+ "<td style=\"vertical-align: top; width: 8%;\">" + myspec.msgsToday + "</td>"
				+ "<td style=\"vertical-align: top; width: 15%;\">" + myspec.currentServer + "</td>"
				+ "<td style=\"vertical-align: top; width: 15%;\">" + myspec.outputName + "</td>"
				+ "<td style=\"vertical-align: top; width: 12%;\">" + myspec.outputFormat + "</td></tr>";
			tableRows.add(row);
		}

		try
		{
			FileWriter mywriter = new FileWriter(htmlFile);
			mywriter.write(htmlHeader);
			mywriter.write(htmlTitleArea);
			mywriter.write(htmlTableStart);
			mywriter.write(htmlTableHeader);
			for(Iterator it = tableRows.iterator(); it.hasNext(); )
				mywriter.write((String)it.next());
			mywriter.write("</tbody></table>\n</body>\n</html>\n");
			mywriter.close();
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Failed Writing HTML File '{}'", htmlFile.getPath());
		}
	}
	
	
	/**
	  Scans the status files in the input directory & removes any
	  that are older than the purge time.
	*/
	private synchronized void purgeOldStatusRecs()
	{
		long currenttime = System.currentTimeMillis();
		for(Iterator it = routingSpecStats.iterator(); it.hasNext(); )
		{
			RoutingSpecStatus tempstats = (RoutingSpecStatus)it.next();

			int timepassed = (int)
				((currenttime - tempstats.lastStatusChangeMsec) / 1000L);

			if(timepassed > purgetime)
			{
				it.remove();
				File f = new File(inputDir, tempstats.name);
				f.delete();
			}
		}
	}
}
