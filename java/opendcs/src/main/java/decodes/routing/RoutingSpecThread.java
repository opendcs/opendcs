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
package decodes.routing;

import java.util.*;

import org.opendcs.database.DatabaseService;

import java.io.*;
import java.net.InetAddress;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import lrgs.common.DcpMsg;
import ilex.util.*;
import ilex.var.Variable;
import ilex.cmdline.*;
import decodes.db.*;
import decodes.datasource.*;
import decodes.polling.PollingDataSource;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.util.*;
import decodes.decoder.DecodedMessage;
import decodes.decoder.DecoderException;
import decodes.decoder.SummaryReportGenerator;
import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.consumer.OutputFormatter;
import decodes.consumer.OutputFormatterException;
import decodes.comp.ComputationProcessor;

/**
This class executes a single routing spec.
Implements the Runnable interface.
Each routing spec is intended to run in its own thread.
*/
public class RoutingSpecThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** The routing spec record that this thread is executing. */
	protected RoutingSpec rs;

	/** The data source that is providing raw messages. */
	protected DataSourceExec source;

	/** The consumer to whom data is being sent. */
	protected DataConsumer consumer;

	/** The object that is formatting data for output. */
	protected OutputFormatter formatter;

	/** The presentation group used to set units and rounding. */
	protected PresentationGroup presentationGroup;

	/** If true, apply sensor limits in config or platform sensor records. */
	public boolean applySensorLimits;

	/** List of script names to execute. If null, execute any script. */
	protected ArrayList<String> scriptNames2Exec = new ArrayList<String>();

	/** Internal flag used to terminate the thread. */
	protected boolean done;

	/** Default=true, meaning periodically check for updates in the DB. */
	public boolean doRoutingSpecCheck;

	/** File to which status properties are written. */
	private File statmonFile;

	/** Current status as displayable String */
	protected String currentStatus = "";
	
	/** Hash Set of distinct medium IDs */
	protected HashSet<String> mediumIdsSeen = new HashSet<String>();

	public void setCurrentStatus(String currentStatus)
	{
		this.currentStatus = currentStatus;
	}

	/** Time this spec was started */
	protected long runStartTime;

	/** Number of messages processed this run */
	protected long numMsgsRun;

	/** Number of errors this run */
	protected long numErrsRun;

	/** Number of messages today */
	protected long numMsgsToday;

	/** Number of erroneous messages today */
	protected long numErrsToday;

	/** Time last msg was receivdd. */
	protected long lastRecvTime;

	/** Set from the -R switch, removes redundant DCP data from output. */
	protected boolean removeRedundantData;

	protected long lastDay;

	/** Computations configuration file */
	public String compConfigFile = "$DECODES_INSTALL_DIR/computations.conf";
	
	public String usgsSummaryFile = null;

	/** The Computation Processor */
	protected ComputationProcessor compProcessor;

	/** Provides controller with a way to force re-init. */
	protected boolean reinitForced = false;

	/** Time that the platform list was last (re)read. */
	protected long lastPlatlistRead;

	/** Explicit LRGS data source, if one provided on command line. */
	protected DataSource explicitDataSource;
	
	/** Explicit Directory Consumer privded on rs command line */
	protected String explicitConsumerDir = null;

	private boolean exitOnCompletion = false;

	private RotatingFile summaryFile = null;
	private SummaryReportGenerator sumGen = null;

	/** If processGoodMsgCheck true, verify that the DCP raw msg is a good
	 * message, by default is false. This comes from routing spec properties */
	private boolean processGoodMsgCheck;
	
	static RoutingSpecThread mainThread = null; // Thread started from main
	public boolean implicitAllUsed = false;
	private long lastInitDone = 0L;
	
	protected boolean closeDbOnQuit = true;
	
	public void setCloseDbOnQuit(boolean tf) { closeDbOnQuit = tf; }
	
	DbKey scheduleEntryStatusId = Constants.undefinedId;
	private ScheduleEntryExecutive myExec = null;
	private ScheduleEntryStatus myStatus = null;

	private RawArchive rawArchive = null;
	private DcpMsg lastDcpMsg = null;
	private StatusWriteThread statusWriteThread = null;
	
	private boolean purgeOldEvents = true;
	private boolean updatePlatformStatus = true;
	
	private Runnable shutdownHook = null;
	
	/** Used only by rs command (this.main), and only if no -k is specified
	 * AND a process record exists with the same name as the routing spec.
	 */
	private TsdbCompLock myLock = null;
	
	CompAppInfo rsProcRecord = null;
	
	private ArrayList<String> includePMs = null;
	
	/**
	 * Constructs an empty, uninitialized RoutingSpecThread.
	 */
	public RoutingSpecThread()
	{
		super();
		setDaemon(true);
	}

	/**
	  Constructs a thread to execute the passed routing spec.
	  @param rs the routing spec
	*/
	public RoutingSpecThread(RoutingSpec rs)
	{
		this();
		setRoutingSpec(rs);
		this.setName(rs.getName());
	}

	/**
	 * Sets the routing spec that this thread will execute.
	 * Must be called prior to running the spec.
	 * @param rs the routing spec
	 */
	public void setRoutingSpec(RoutingSpec rs)
	{
		this.rs = rs;
		source = null;
		consumer = null;
		formatter = null;
		presentationGroup = null;
		applySensorLimits = true;
		scriptNames2Exec.clear();
		doRoutingSpecCheck = true;
		statmonFile = null;
		lastRecvTime = 0L;
		compProcessor = null;
		reinitForced = false;
		exitOnCompletion = false;
		explicitDataSource = null;
		processGoodMsgCheck = false;

		rs.setProperty("RoutingSpec.name", rs.getName());
	}
	
	public RoutingSpec getRoutingSpec() { return rs; }

	/** 
	  Sets the status monitor file to the passed filename.
	  Each routing spec thread will periodically place its current status
	  into a status file.
	  @param name the name of the status file
	*/
	public void setStatusFile(String name)
	{
		if (name == null)
			statmonFile = null;
		else
			statmonFile = new File(EnvExpander.expand(name));
	}

	protected void startupMsg()
	{
		log.info("Starting ({}) ==============", DecodesVersion.startupTag());
	}

	/**
	Runs this routing spec.
	*/
	public void run()
	{
		numMsgsToday = 0;
		numErrsToday = 0;
		initStatus();
		runStartTime = System.currentTimeMillis();
		numMsgsRun = 0;
		numErrsRun = 0;
		lastDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L);
		log.debug("run() starting.");
		writeStatus();

		done = false;
		long lastMsgReceiveMsec = 0L;
		checkRoutingSpec();
		checkNetworkLists();
		checkPresentationGroup();
		init(lastMsgReceiveMsec);
		if (done)
		{
			quit();
			return; // Initialization failed.
		}
		
		decodes.db.DatabaseIO dbio = Database.getDb().getDbIo();
		
		long lastUp2DateCheck = System.currentTimeMillis();
		//long lastStatusWrite = System.currentTimeMillis();

		if (usgsSummaryFile != null && usgsSummaryFile.length() > 0)
		{
			try
			{
				summaryFile = 
					new RotatingFile(EnvExpander.expand(usgsSummaryFile), 10000000);
				sumGen = new SummaryReportGenerator();
				sumGen.setTimeZone(rs.outputTimeZoneAbbr);
			}
			catch(Exception ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Cannot initialize summary file '{}' -- will proceed without summaries.",
				   		usgsSummaryFile);
				summaryFile = null;
				sumGen = null;
			}
		}
		
		if (officeIdArg.getValue() != null && officeIdArg.getValue().length() > 0)
			DecodesSettings.instance().CwmsOfficeId = officeIdArg.getValue();

		String rawArchivePath = rs.getProperty("RawArchivePath");
		if (rawArchivePath != null)
			rawArchive = new RawArchive(rs);

		log.trace("Starting, applySensorLimits={}", applySensorLimits);
		currentStatus = "Running";
		statusWriteThread = new StatusWriteThread(this);
		statusWriteThread.start();

			
		if (myLock != null)
		{
			// MJM 20170220 Added TsdbCompLock capability. The main() method, will
			// create a lock if: 1.) No [-k lockfile] is specified, 2.) There is a
			// process record matching the rs name, and 3.) the process' monitor flag
			// is true.
			new Thread()
			{
				long lastLockCheck = System.currentTimeMillis();
				public void run()
				{
					while (!done)
					{
						if (System.currentTimeMillis() - lastLockCheck > 5000L)
						{
							LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
							myLock.setStatus(myStatus.getStats());
							try { loadingAppDAO.checkCompProcLock(myLock); }
							catch (LockBusyException ex)
							{
								log.atError().setCause(ex).log("Database Lock removed -- exiting.");
								shutdown();
								currentStatus = "Lock Removed";
								return;
							}
							catch (DbIoException ex)
							{
								log.atError().setCause(ex).log("Error checking database lock.");
							}
							finally
							{
								loadingAppDAO.close();
							}
							lastLockCheck = System.currentTimeMillis();
						}
						try{ sleep(500L); } catch(InterruptedException ex) {}
					}
				}
			}.start();

			
		}
		
		while(!done)
		{
			myExec.setSubsystem(null);

			// Periodically check to see if my objects have changed.			
			long now = System.currentTimeMillis();
			if (now - lastUp2DateCheck > 30000L)
			{
				myExec.setSubsystem("update");
				log.debug("Doing up2date checks...");
				lastUp2DateCheck = now;
				
				// Use non-short circuit OR operator so they all get evaluated.
				if (checkRoutingSpec() | checkNetworkLists()
				  | checkPresentationGroup() | reinitForced)
				{
					reinitForced = false;
					lastInitDone = System.currentTimeMillis();
					init(lastMsgReceiveMsec);
					continue;
				}
			}
			

			
			// MJM 20041027 Added the following check:
			// Every 10 minutes, re-read platform list to see if any platforms
			// have been added.
			if (now - Database.getDb().platformList.getLastReadTime() > 10*60000L)
			{
				myExec.setSubsystem("platlist");
				try { Database.getDb().platformList.read(); }
				catch(DatabaseException ex)
				{
					log.atError().setCause(ex).log("Could not refresh platform list.");
				}
			}
			
			if (purgeOldEvents && now - myExec.getLastEventsPurge() > 3600000L) // every hour, or on first run
			{
				myExec.setLastEventsPurge(now);
				if ((dbio instanceof SqlDatabaseIO)
				 && ((SqlDatabaseIO)dbio).getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_11)
				{
					// TODO: determine if can just be removed or needs to be moved.
					DacqEventDAI dao = ((SqlDatabaseIO)dbio).makeDacqEventDAO();
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DAY_OF_MONTH, -DecodesSettings.instance().eventPurgeDays);
					log.info("Purging old DACQ_EVENTs before {}", cal.getTime());
					try { dao.deleteBefore(cal.getTime()); }
					catch(Exception ex)
					{
						log.atWarn().setCause(ex).log("Failed to delete events before {}", cal.getTime());
					}
					finally { dao.close(); }
				}
			}
			
			//=====================================================
			// Retrieve the next raw message from the data source.
			//=====================================================
			myExec.setSubsystem("acquire");
			RawMessage rm = null;
			try
			{
				rm = source.getRawMessage();
				if (rm == null)
				{
					log.trace("Data source failed to return message, pausing for 1 seconds.");
					currentStatus = "Wait-Msg";
					if (formatter != null)
						formatter.dataSourceCaughtUp(false);
					try { sleep(1000L); }
					catch (InterruptedException e) {}
					continue;
				}
				numMsgsRun++;
				numMsgsToday++;
				lastRecvTime = System.currentTimeMillis();
				log.trace("Message from '{}': {}, length={}",
						  rs.dataSource.getName(), new String(rm.getHeader()), rm.getMessageData().length);
				mediumIdsSeen.add(rm.getMediumId());
			}
			catch(DataSourceEndException ex)
			{
				log.atInfo()
				   .setCause(ex)
				   .log("Normal termination of data source '{}'", rs.dataSource.getName());
				if (formatter != null)
					formatter.dataSourceCaughtUp(true);
				currentStatus = "Completed";
				done = true;
				continue;
			}
			catch(UnknownPlatformException ex)
			{
				numErrsRun++;
				numErrsToday++;
				if (formatter.requiresDecodedMessage())
				{
					log.atWarn().setCause(ex).log("Data source '{}' -- skipped", rs.dataSource.getName());
					currentStatus = "Running";
					continue;
				}
			}
			catch(DataSourceException ex)
			{
				
				log.atError()
				   .setCause(ex)
				   .log("Error on data source '{}' -- exiting", rs.dataSource.getName());
				done = true;
				currentStatus = "ERROR-Source";
				continue;
			}
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Unexpected exception in data source '{}'", rs.dataSource.getName());
				done = true;
				currentStatus = "ERROR-Source";
				continue;
			}
			lastMsgReceiveMsec = System.currentTimeMillis();

			myExec.setPlatform(rm.getPlatformOrNull());
			myExec.setMessageStart(rm.getTimeStamp());

			if (rawArchive != null)
			{
				myExec.setSubsystem("raw-archive");
				rawArchive.archive(rm);
			}
			
			PlatformStatus platstat = null;
			Platform platform = rm.getPlatformOrNull();
			try(PlatformStatusDAI platformStatusDAO = Database.getDb().getDbIo().makePlatformStatusDAO();)
			{
				if (platform != null && updatePlatformStatus)
				{
					try
					{
						platstat = platformStatusDAO.readPlatformStatus(platform.getId());
					}
					catch (DbIoException ex)
					{
						log.atWarn().setCause(ex).log("Cannot read platform status.");
						platstat = null;
					}
					if (platstat == null)
						platstat = new PlatformStatus(platform.getId());
					Date x = new Date();
					// PollingDataSource manages its own contact and message times.
					if (!(source instanceof PollingDataSource))
					{
						platstat.setLastContactTime(x);
						platstat.setLastMessageTime(x);
					}
					String annot = platstat.getAnnotation();
					if (annot != null && annot.toUpperCase().startsWith("STALE"))
						platstat.setAnnotation("");
					Variable fc = rm.getPM(GoesPMParser.FAILURE_CODE);
					if (fc != null)
						platstat.setLastFailureCodes(fc.toString());
					platstat.setLastScheduleEntryStatusId(scheduleEntryStatusId);
				}

				//=====================================================
				// Retrieve the platform, transport medium, decodes script,
				// and prepare them for execution. Then decode the message.
				//=====================================================
				DecodedMessage dm = null;
				DcpMsg dcpMsg = rm.getOrigDcpMsg();
				if (dcpMsg != null)
					lastDcpMsg = dcpMsg;

				if (formatter.attemptDecode()
				&& (dcpMsg == null || !dcpMsg.isDapsStatusMsg()))
				{
					myExec.setSubsystem("decode");
					dm = attemptDecode(rm, platstat);
				}
				else // Just make the Decoded message a wrapper around raw.
				{
					try
					{
						dm = new DecodedMessage(rm, false);
					}
					catch(Exception ex)
					{
						log.atError().setCause(ex).log("Unexpected Error processing raw message.");
					}
				}

				if (dm != null)
				{
					myExec.setSubsystem("format-output");
					formatAndOutputMessage(dm, platstat);
				}

				if (platstat != null && updatePlatformStatus)
				{
					try
					{
						myExec.setSubsystem("platstat");
						platformStatusDAO.writePlatformStatus(platstat);
					}
					catch (DbIoException ex)
					{
						log.atWarn().setCause(ex).log("Cannot write platform status.");
					}
				}
				myExec.setPlatform(null);
				myExec.setMessageStart(null);

				currentStatus = "Running";
			}
		}

		quit();
	}
	
	public void assertPlatformError(String msg, PlatformStatus platstat,Throwable cause, Object... args)
	{
		numErrsRun++;
		numErrsToday++;
		
		LoggingEventBuilder le = formatter==null || formatter.requiresDecodedMessage() ? log.atWarn() : log.atTrace();
		le.setCause(cause).log(msg, args);

		if (platstat != null)
		{
			platstat.setLastErrorTime(new Date());
			platstat.setAnnotation(msg);
		}
	}
	
	private void formatAndOutputMessage(DecodedMessage dm, PlatformStatus platstat)
	{
		//=====================================================
		// Use the formatter & consumer to output the message.
		//=====================================================
		try 
		{
			formatter.formatMessage(dm, consumer); 
			doSummary(dm);
		}
		catch(OutputFormatterException ex)
		{
			String msg = "Error on output formatter '{}', message skipped.";
			assertPlatformError(msg, platstat, ex, rs.outputFormat);
			currentStatus = "ERROR-Format";
		}
		catch(DataConsumerException ex)
		{
			String msg = "Error on data consumer '{}'";
			assertPlatformError(msg, platstat, ex, rs.consumerType);
			currentStatus = "ERROR-Format";
			done = true;
			consumer = null;
			currentStatus = "ERROR-Output";
		}
		catch(Exception ex)
		{
			String msg = "Unexpected exception in formatter.";
			assertPlatformError(msg, platstat, ex);
		}
	}

	/**
	 * Try to decode. Return decoded message if success, null if error.
	 * @param rm raw message
	 * @param platformStatus platform status in case of errors
	 * @return decoded message if success, null if error
	 */
	private DecodedMessage attemptDecode(RawMessage rm, PlatformStatus platstat)
	{
		Platform p = null;
		TransportMedium tm = null;
		try 
		{
			p = rm.getPlatform(); 
			tm = rm.getTransportMedium();
			log.trace("Message is for platform '{}' with transport medium '{}' and script '{}'",
					  p.makeFileName(), tm.makeFileName(), tm.scriptName);

			// Make sure this platform is prepared for execution.
			if (!p.isPrepared())
				p.prepareForExec();
			if (!tm.isPrepared())
				tm.prepareForExec();

			// Get decodes script & use it to decode message.
			DecodesScript ds = tm.getDecodesScript();
			if (ds == null)
				throw new InvalidDatabaseException(
					"Transport medium does not have a DecodesScript");

			if (!shouldBeExecuted(tm.scriptName))
			{
				log.debug("Skipping message for platform '{}', script name '{}': not in script name list.",
						  p.makeFileName(), tm.scriptName);
				return null;
			}

			//This comes from Routing Spec Property: 
			//processgoodmsg - true -or- yes
			if (processGoodMsgCheck)
			{
				Variable v;
				v = rm.getPM(GoesPMParser.FAILURE_CODE);
				if (v != null)
				{
					char failureCode = v.getCharValue();
					if (failureCode != 'G')
					{
						log.warn("ProcessGoodMsg property set to true. Found bad Message from '{}' " +
								 "For platform =  '{}', Transport medium = '{}', Msg header = '{}' -- skipped",
								 rs.dataSource.getName(), p.makeFileName(),
								 tm.makeFileName(), new String(rm.getHeader()));
						return null;
					}
				}
			}
			
			ds.setIncludePMs(includePMs);
			DecodedMessage dm = ds.decodeMessage(rm);

			dm.applyScaleAndOffset();

			// If we are to apply min/max (default==true), do it.
			if (applySensorLimits)
			{
				myExec.setSubsystem("limits");
				dm.applySensorLimits();
			}

			// Use presentation group to convert units & format values
			myExec.setSubsystem("presentation");
			dm.formatSamples(presentationGroup);

			if (removeRedundantData)
				dm.removeRedundantData();
			
			// MJM 2021/09/10 This block used to be above right after "dm.applyScaleAndOffset()"
			// It was moved here requested by Art Armour (NWP) so that sensor values that are
			// omitted by sensor property, out of limits, or redundancy, will not be in-line
			// rated by the rating computation algorithm.
			if (compProcessor != null)
			{
				myExec.setSubsystem("computation");
				compProcessor.applyComputations(dm);
			}

			return dm;
		}
		catch(UnknownPlatformException ex)
		{
			String msg = "Decoding failed";
			assertPlatformError(msg, platstat,ex);
			if (formatter.requiresDecodedMessage())
			{
				currentStatus = "Running";
				return null;
			}
			else
			{
				log.trace("Processing raw message from data source.");
				try { return new DecodedMessage(rm, false); }
				catch(Exception ex2) 
				{
					log.atError().setCause(ex2).log("Cannot create DecodedMessage wrapper for raw message.");
					return null;
				}
			}
		}
		catch(DecoderException ex)
		{
			String msg = "Failed to decode message from platform {}, transport={}";
			assertPlatformError(msg, platstat, ex, p.makeFileName(), tm.makeFileName());
			if (formatter.requiresDecodedMessage())
			{
				return null;
			}
			else
			{
				try { return new DecodedMessage(rm, false); }
				catch(Exception ex2) 
				{
					log.atError().setCause(ex).log("Cannot create DecodedMessage wrapper for raw message.");
					return null;
				}
			}
		}
		catch(InvalidDatabaseException ex)
		{
			String pid = pidString(p);
			String msg = "Invalid Database error in platform: {}";
			assertPlatformError(msg, platstat, ex, pid.toString());
			if (formatter.requiresDecodedMessage())
				return null;
			else
			{
				try { return new DecodedMessage(rm, false); }
				catch(Exception ex2) 
				{
					log.atError().setCause(ex2).log("Cannot create DecodedMessage wrapper for raw message.");
					return null;
				}
			}
		}
		catch(IncompleteDatabaseException ex)
		{
			String pid = pidString(p);
			String msg = "Incomplete Database error in platform: {}";
			assertPlatformError(msg, platstat, ex, pid.toString());
			if (formatter.requiresDecodedMessage())
				return null;
			else
			{
				try { return new DecodedMessage(rm, false); }
				catch(Exception ex2) 
				{
					log.atError().setCause(ex2).log("Cannot create DecodedMessage wrapper for raw message.");
					return null;
				}
			}
		}
		catch(Exception ex)
		{
			String platname = "(null)";
			if (p != null)
				platname = p.makeFileName();
			String msg = "Exception processing data from platform '{}'";
			assertPlatformError(msg, platstat, ex, platname);
			return null;
		}		
	}


	protected void closeResources()
	{
		if (formatter != null)
		{
			formatter.shutdown();
			formatter = null;
		}
		if (consumer != null)
		{
			consumer.close();
			consumer = null;
		}
		if (source != null)
		{
			source.close();
			source = null;
		}
		if (summaryFile != null)
		{
			summaryFile.close();
			summaryFile = null;
		}
		if (compProcessor != null)
		{
			compProcessor.shutdown();
			compProcessor = null;
		}
		if (rawArchive != null)
		{
			rawArchive.shutdown();
			rawArchive = null;
		}
		if (closeDbOnQuit)
		{
			Database db = Database.getDb();
			db.getDbIo().close();
		}
	}

	protected void quit()
	{
		if (shutdownHook != null)
			shutdownHook.run();

		closeResources();
		if (statusWriteThread != null)
			statusWriteThread.shutdown = true;
		if (myStatus != null)
			myStatus.setRunStop(new Date());
		if (myExec != null)
			myExec.rsFinished();

		writeStatus();

		log.info("-------------- RoutingSpec '{}' Terminating --------------", rs.getName());
		
	}

	private void init(long lastMsgRecvMsec)
	{
		currentStatus = "Initializing";
		log.debug("Routing spec init");

		if (!rs.isPrepared())
		{
			try { rs.prepareForExec(); }
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Cannot execute routing spec {}.", rs.getName());
				done = true;
				currentStatus = "ERROR-Database";
				return;
			}
		}
		
		if (compProcessor != null)
		{
			compProcessor.shutdown();
			compProcessor = null;
		}
		
		String s = rs.getProperty("compConfig");
		if (s != null && s.trim().length() > 0)
			compConfigFile = s.trim();

		if (rs.enableEquations)
		{
			compProcessor = new ComputationProcessor();
			try { compProcessor.init(compConfigFile, rs); }
			catch(decodes.comp.BadConfigException ex)
			{
				log.atWarn().setCause(ex).log("Cannot configure computation processor.");
				compProcessor = null;
			}
		}

		// Initialize the PresentationGroup for this routing spec.
		if (rs.presentationGroupName != null
		 && !rs.presentationGroupName.equals("(none)")
		 && rs.presentationGroupName.length() > 0)
		{
			presentationGroup =
				Database.getDb().presentationGroupList.find(
					rs.presentationGroupName);
			if (presentationGroup == null)
			{
				log.error("Cannot find presentation group '{}'", rs.presentationGroupName);
				done = true;
				currentStatus = "ERR-PresGrp";
				return;
			}
			try { presentationGroup.read(); }
			catch(DatabaseException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Cannot read presentation group '{}': will proceed without it.",
				   		rs.presentationGroupName);
				presentationGroup = null;
			}
			if (presentationGroup != null
			 && !presentationGroup.isPrepared())
			{
				try { presentationGroup.prepareForExec(); }
				catch(InvalidDatabaseException ex)
				{
					log.atWarn().setCause(ex).log("Unable to prepare presentation group");
				}
			}
		}

		// Instantiate and open the data consumer and output formatter.
		try
		{
			if (consumer != null)
			{
				consumer.close();
				consumer = null;
			}
			
			// If an explicit FOLDER consumer was provided on command line,
			// then set rs.consumerType and rs.conumerArg.
			if (explicitConsumerDir != null && explicitConsumerDir.length() > 0)
			{
				log.info("Explicit directory consumer '{}'", explicitConsumerDir);
				rs.consumerType = "directory";
				rs.consumerArg = explicitConsumerDir;
			}
			
			consumer = DataConsumer.makeDataConsumer(rs.consumerType);
			log.debug("Instantiated consumer with type '{}'", rs.consumerType);
			consumer.open(rs.consumerArg, rs.getProperties());
			consumer.setTimeZone(rs.outputTimeZone);
			consumer.setRoutingSpecThread(this);
			
			formatter = OutputFormatter.makeOutputFormatter(
				rs.outputFormat, rs.outputTimeZone,
				presentationGroup, rs.getProperties(), this);
		}
		catch(OutputFormatterException ex)
		{
			log.atError().setCause(ex).log("Cannot initialize output formatter '{}'", rs.outputFormat);
			consumer.close();
			done = true;
			currentStatus = "ERR-FormatInit";
			return;
		}
		catch(DataConsumerException ex)
		{
			log.atError().setCause(ex).log("Cannot initialize consumer '{}'", rs.consumerType);
			done = true;
			currentStatus = "ERR-OutputInit";
			return;
		}

		// Initialize the routing spec's data source.
		try
		{
			if (explicitDataSource != null)
				rs.dataSource = explicitDataSource;
			else if (rs.dataSource == null)
			{
				log.error("No data source in routing spec, cannot initialize!");
				done = true;
				return;
			}

			if (source != null)
			{
				source.close();
				source = null;
			}
			source = rs.dataSource.makeDelegate();
			source.setRoutingSpecThread(this);

			String sinceTime = rs.sinceTime;
			// If we are REinitializing, adjust sinceTime to last rcv time - 60 sec.
			if (lastMsgRecvMsec != 0)
				sinceTime = IDateFormat.time_t2string(
					(int)((lastMsgRecvMsec - 60000L) / 1000L));

			// If the formatter can accept DAPS status messages, tell the source.
			source.setAllowDapsStatusMessages(
				!formatter.acceptRealDcpMessagesOnly());
			source.setAllowNullPlatform(!formatter.requiresDecodedMessage());
			log.debug("rst.init: calling source init, there are {} NLs, and {} NL Names.",
					  rs.networkLists.size(), rs.networkListNames.size());
			
			source.init(rs.getProperties(), sinceTime, rs.untilTime, rs.networkLists);
		}
		catch(InvalidDatabaseException ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("Cannot make delegate for data source '{}'", rs.dataSource.getName());
			done = true;
			currentStatus = "ERR-Database";
			return;
		}
		catch(Exception ex) // includes DataSourceException and anything unexpected.
		{
			String msg = "Cannot initialize data source '";
			if (rs == null)
				msg = msg + "(null routingspec)";
			else if (rs.dataSource == null)
				msg = msg + "(null datasource)";
			else
				msg = msg + rs.dataSource.getName();
			log.atError()
			   .setCause(ex)
			   .log(msg);

			done = true;
			currentStatus = "ERR-SourceInit";

			return;
		}

		// If 'properties' contains script names, get them.
		s = PropertiesUtil.getIgnoreCase(rs.getProperties(),"scriptname");
		if (s == null)
			s = PropertiesUtil.getIgnoreCase(rs.getProperties(),"scriptnames");
		if (s != null)
		{
			StringTokenizer st = new StringTokenizer(s);
			while(st.hasMoreTokens())
				addScriptName(st.nextToken());
		}

		s = PropertiesUtil.getIgnoreCase(rs.getProperties(),"nolimits");
		if (s != null &&
			(s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("true")))
		{
			applySensorLimits = false;
			log.info("Set applySensorLimits={} because 'nolimits' property = '{}'", applySensorLimits, s);
		}

		s = PropertiesUtil.getIgnoreCase(rs.getProperties(),"processgoodmsg");
		if (s != null && s.trim().length() > 0)
			processGoodMsgCheck = TextUtil.str2boolean(s);
		
		s = rs.getProperty("removeRedundantData");
		if (s != null && TextUtil.str2boolean(s))
			removeRedundantData = true;
		
		s = rs.getProperty("usgsSummaryFile");
		if (s != null && s.trim().length() > 0)
			usgsSummaryFile = s;
		
		s = rs.getProperty("purgeOldEvents");
		purgeOldEvents = s == null || s.trim().length() == 0 ? true : TextUtil.str2boolean(s);

		s = rs.getProperty("updatePlatformStatus");
		updatePlatformStatus = s == null || s.trim().length() == 0 ? true : TextUtil.str2boolean(s);
		
		s = rs.getProperty("includePMs");
		if (s == null || s.trim().length() == 0)
			includePMs = null;
		else
		{
			includePMs = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(s, ", ");
			while(st.hasMoreTokens())
				includePMs.add(st.nextToken());
			log.debug("includePMs='{}', {} names parsed.", s, includePMs.size());
		}
	}

 	/**
	  This utility function generates a string with information about
	  a platform, for use in error messages.
	  @param p the platform
	  @return a string identifying the platform, site, and transport ID.
	*/
	public String pidString(Platform p)
	{
		StringBuffer pid = new StringBuffer();
		if (p == null) {
			return "(null platform)";
		}
		else
		{
			pid.append("PlatformID=");
			pid.append(p.getId());
			pid.append(", site=");
			pid.append(p.getSiteName());
			pid.append(", Transport ID=");
			pid.append(p.getPreferredTransportId());
		}
		return pid.toString();
	}

	/**
	  If specific scripts are specified, this method adds the name to the list.
	  This feature allows the user to specify, for example, that only ST
	  messages are to be processed.
	*/
	public void addScriptName(String name)
	{
		if (name == null || name.length() == 0)
			return;

		// Create vector if it doesn't already exist.
		log.info("Adding script name '{}' to list of scripts to be executed.", name);
		if (shouldBeExecuted(name))
			return;
		scriptNames2Exec.add(name);
	}

	/**
	  Return true if this script should be executed.
	  @param name the script name
	  @return true if this script should be executed
	*/
	public boolean shouldBeExecuted(String name)
	{
		if (scriptNames2Exec.size() == 0)
			return true;
		for(String sn : scriptNames2Exec)
			if (name.equalsIgnoreCase(sn))
				return true;
		return false;
	}

	/**
	  Checks to see if the routing spec has been modified since it was 
	  started, and if so, reloads and re-initializes this executable.

	  @return true if spec was reloaded.
	*/
	protected boolean checkRoutingSpec()
	{
		if (!doRoutingSpecCheck)
			return false;

		try
		{
			Date dbLMT = rs.getDatabase().getDbIo().getRoutingSpecLMT(rs);
			if (dbLMT == null)
			{
				// This indicates that the spec was deleted. I should exit.
				log.info("Exiting because RoutingSpec object deleted from database.");
				done = true;
				currentStatus = "ERR-Deleted";
				return false;
			}

			if (rs.lastModifyTime.compareTo(dbLMT) >= 0)
				return false;  // no change!

			// Reload the new spec & re-init this executable.
			log.info("RoutingSpec object changed in database - Re-initializing.");
			rs.read();
			rs.prepareForExec();
			return true;
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Error reading routing spe.");
			done = true;
			currentStatus = "ERR-DB/IO";
			return false;
		}
	}

	/**
	  Checks to see if any of the network lists used by this routing spec 
	  have been modified since it was started, and if so, reloads them.

	  @return true if any network list was modified.
	*/
	protected boolean checkNetworkLists()
	{
		boolean ret = false;
		try
		{
			if (implicitAllUsed)
			{
				Date plLMT = rs.getDatabase().getDbIo().getPlatformListLMT();
				if (plLMT.getTime() > lastInitDone)
				{
					log.info("Implicit list is used and platform list was modified, will re-init");
					ret = true;
				}
			}

			log.debug("checkNetworkLists: There are {} explicit lists.", rs.networkLists.size());
			log.debug("checkNetworkLists: There are {} explicit list names.", rs.networkListNames.size());
			for(Iterator<NetworkList> it = rs.networkLists.iterator(); 
				it.hasNext(); )
			{
				NetworkList nl = it.next();
				if (nl == NetworkList.dummy_all || nl == NetworkList.dummy_production)
					continue;
				Date dbLMT = rs.getDatabase().getDbIo().getNetworkListLMT(nl);
				if (dbLMT != null && nl.lastModifyTime.compareTo(dbLMT) < 0)
				{
					// This indicates that this list was modified.
					nl.clear();
					nl.read();
					nl.prepareForExec();
					ret = true;
					log.info("Reloaded network list '{}'", nl.name);
				}
			}
			return ret;
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Error reading network list.");
			return false;
		}
	}

	/**
	  Checks to see if the presentation group used by this routing spec 
	  has been modified since it was started, and if so, reloads it.

	  @return true if PG was modified.
	*/
	private boolean checkPresentationGroup()
	{
		try
		{
			if (presentationGroup == null
		 	 || rs.presentationGroupName.equals("(none)"))
				return false;
			
			ArrayList<PresentationGroup> checked = new ArrayList<PresentationGroup>();
			boolean changesMade = false;
			for(PresentationGroup checkPG = presentationGroup; checkPG != null; checkPG = checkPG.parent)
			{
				if (checked.contains(checkPG))
				{
					log.info("Circular presentation group references detected. Aborting check.");
					break;
				}
				checked.add(checkPG);
				
				Date dbLMT = rs.getDatabase().getDbIo().getPresentationGroupLMT(checkPG);
				if (dbLMT == null)
				{
					log.info("PresentationGroup was deleted from database, proceeding without it.");
					if (checkPG == presentationGroup)
						presentationGroup = null; // This routing spec no longer has a PG.
					else
						checked.get(checked.size()-1).parent = null; // Remove from chain.
					return true;
				}
				else 
				{
					Date lmt = checkPG.lastModifyTime;
					if (lmt == null)
					{
						log.warn("Presentation Group '{}', LMT is null.", checkPG.groupName);
					}
					else if (lmt.compareTo(dbLMT) < 0)
					{
						log.info("PresentationGroup '{}' was modified, reloading it.", checkPG.groupName);
						checkPG.clear();
						checkPG.read();
						checkPG.prepareForExec();
						changesMade = true;
					}
				}
			}
			return changesMade;
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Error reading presentation group.");
			return false;
		}
	}

	/**
	  @return the DataConsumer in use.
	*/
	public DataConsumer getConsumer()
	{
		return consumer;
	}

	/**
	  Causes the routing spec to terminate.
	*/
	public void shutdown()
	{
		log.info("shutdown called.");
		done = true;
		if (source != null)
		{
			source.close();
		}
		if (this.statusWriteThread != null)
		{
			this.statusWriteThread.shutdown = true;
		}
	}

	private void doSummary(DecodedMessage dm)
	{
		if (summaryFile == null)
			return;
		String srcName = "Routing Spec: " + rs.getName();
		if (source != null)
			srcName = srcName + ", source=" + source.getActiveSource();
		try
		{
			summaryFile.write(sumGen.makeReport(dm, srcName));
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Cannot write summary to file '{}'", summaryFile.getPath());
		}
	}

	//===============================================================
	// Main Method and Command Line Arguments
	//===============================================================
	static String defaultLogFile = "routing.log";
	static BooleanToken noLimitsArg = new BooleanToken("m", 
		"Do NOT apply Sensor min/max limits.", "", 
		TokenOptions.optSwitch, false);
	static StringToken scriptNameArg = new StringToken("s", "ScriptName", "",
		TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static StringToken netlistArg = new StringToken("n", "Netlist Name", "",
		TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static StringToken sinceArg = new StringToken("S", "Since Time", "",
		TokenOptions.optSwitch, "");
	static StringToken untilArg = new StringToken("U", "Until Time", "",
		TokenOptions.optSwitch, "");
	static StringToken statmonArg = new StringToken("o","Status Output File","",
		TokenOptions.optSwitch, "");
	static BooleanToken removeRedundantArg = new BooleanToken("R", 
		"Remove Redundant DCP Message Data.", "", 
		TokenOptions.optSwitch, false);
	static BooleanToken compEnableArg = new BooleanToken("c", 
		"Enable computations", "", 
		TokenOptions.optSwitch, false);
	static StringToken compConfigArg = new StringToken("C", 
		"Computation Config File", "", TokenOptions.optSwitch, "");
	static StringToken dbLocArg = new StringToken("E", 
		"Explicit Database Location", "", TokenOptions.optSwitch, "");
	static StringToken lockFileArg = new StringToken("k", 
		"Optional Lock File", "", TokenOptions.optSwitch, "");
	static StringToken propSetArg = new StringToken("p", 
		"name=value", "", TokenOptions.optSwitch|TokenOptions.optMultiple, "");
	static StringToken lrgsSourceArg = new StringToken("L", 
		"host:port:user[:password]", "", TokenOptions.optSwitch, "");
	static StringToken usgsSummaryFileArg = new StringToken("M", 
		"Optional Summary File", "", TokenOptions.optSwitch, "");
	static StringToken rsArg = new StringToken("", "Routing Spec Name", "",
		TokenOptions.optArgument |TokenOptions.optRequired, "");
	static StringToken officeIdArg = new StringToken(
		"O", "OfficeID", "", TokenOptions.optSwitch, "");
	static StringToken dirConsumerArg = new StringToken("F", 
		"Explicit directory-consumer folder-name", "", TokenOptions.optSwitch, "");
	static BooleanToken editDbArg = new BooleanToken("e", 
		"(deprecated -- does nothing)", "", TokenOptions.optSwitch, false);

	private static void setupArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(noLimitsArg);
		scriptNameArg.setType("Script-Name");
		cmdLineArgs.addToken(scriptNameArg);
		netlistArg.setType("Netlist-Name");
		cmdLineArgs.addToken(netlistArg);
		cmdLineArgs.addToken(sinceArg);
		cmdLineArgs.addToken(untilArg);
		statmonArg.setType("filename");
		cmdLineArgs.addToken(statmonArg);
		cmdLineArgs.addToken(removeRedundantArg);
		cmdLineArgs.addToken(compEnableArg);
		compConfigArg.setType("filename");
		cmdLineArgs.addToken(compConfigArg);
		dbLocArg.setType("dirname");
		cmdLineArgs.addToken(dbLocArg);
		lockFileArg.setType("filename");
		cmdLineArgs.addToken(lockFileArg);
		propSetArg.setType("property-set");
		cmdLineArgs.addToken(propSetArg);
		cmdLineArgs.addToken(lrgsSourceArg);
		cmdLineArgs.addToken(usgsSummaryFileArg);
		rsArg.setType("RoutingSpecName");
		cmdLineArgs.addToken(officeIdArg);
		cmdLineArgs.addToken(dirConsumerArg);
		cmdLineArgs.addToken(editDbArg);
		rsArg.reset(); // Required for integration tests since the objects are currently static and the JVM is thus shared.
		cmdLineArgs.addToken(rsArg);
	}
	
	/** Kludge for buffering in data source */
	public void lrgsDataSourceCaughtUp()
	{
		if (this.formatter != null)
			formatter.dataSourceCaughtUp(false);
	}


	/**
	Main method.
	Usage: java decodes.decoder.RoutingSpecThread [options] [specname]
	<p>
	Executes a single routing specification from the command line.
	@param args the command line arguments
	*/
	public static void main(String args[])
		throws DecodesException, IOException, DbIoException, InterruptedException
	{
		final CmdLineArgs cmdLineArgs = new CmdLineArgs(false, defaultLogFile);
		setupArgs(cmdLineArgs);
		// MJM 20171206 set to false to allow command line args to override settings
		// in the routing spec.
		ScheduleEntryExecutive.setRereadRsBeforeExec(false);

		// Parse command line arguments.
		try { cmdLineArgs.parseArgs(args); }
		catch(IllegalArgumentException ex)
		{
			log.atError().setCause(ex).log("Invalid arguments provided.");
			System.exit(1);
		}

		DecodesSettings settings = DecodesSettings.fromProfile(cmdLineArgs.getProfile());

		File routmonDir = new File(
			EnvExpander.expand(settings.routingStatusDir));
		if (!routmonDir.isDirectory())
			routmonDir.mkdirs();

		String rsName = rsArg.getValue();

		/** Optional server lock ensures only one instance runs at a time. */
		String lockpath = lockFileArg.getValue();
		if (lockpath != null && lockpath.trim().length() > 0)
		{
			lockpath = EnvExpander.expand(lockpath.trim());
			final ServerLock mylock = new FileServerLock(lockpath);

			if (mylock.obtainLock() == false)
			{
				log.error("Routing Spec not started: lock file busy: {}", lockpath);
				Database db = Database.getDb();
				db.getDbIo().close();
				System.exit(0);
			}

			mylock.releaseOnExit();
			Runtime.getRuntime().addShutdownHook(
				new Thread()
				{
					public void run()
					{
						if (mainThread != null
						 && mainThread.statmonFile != null)
						{
							mainThread.currentStatus = "Stopped";
							mainThread.writeStatus();
						}
						log.info("Routing Spec exiting {}",
								 (mylock.wasShutdownViaLock() ? "(lock file removed)" : ""));
					}
				});
		}

		
		// Construct the database and the interface specified by properties.
		ResourceFactory.instance();
		
		Database db = DatabaseService.getDatabaseFor(null, settings)
									 .getLegacyDatabase(Database.class)
									 .get();
		Database.setDb(db);

		// Initialize standard collections:
		db.enumList.read();
		db.dataTypeSet.read();
		db.engineeringUnitList.read();
		db.equipmentModelList.read();
		db.platformConfigList.read();
		db.platformList.read();
		db.presentationGroupList.read();
		db.routingSpecList.read();
		
		RoutingSpec rs = db.routingSpecList.find(rsName);
		if (rs == null)
			throw new DecodesException(
				"No such routing spec '" + rsName + "' in database");
		
		// Since & Until argument overrides value in DB routing spec.
		String s = sinceArg.getValue().trim();
		if (s.length() > 0)
			rs.sinceTime = s;
		s = untilArg.getValue().trim();
		if (s.length() > 0)
			rs.untilTime = s;

		// Enable computations arg overrides value in DB routing spec.
		if (compEnableArg.getValue())
			rs.enableEquations = true;

		// Add network lists from command line.
		for(int i=0; i<netlistArg.NumberOfValues(); i++)
		{
			s = netlistArg.getValue(i).trim();
			if (s != null && s.length() > 0 && !rs.networkListNames.contains(s))
				rs.networkListNames.add(s);
		}

		// Add properties from command line.
		for(int i=0; i<propSetArg.NumberOfValues(); i++)
		{
			s = propSetArg.getValue(i).trim();
			if (s == null || s.length() == 0)
				continue;
			int eidx = s.indexOf('=');
			if (eidx == -1)
				throw new DecodesException("Syntax on -p properties set: "
					+ "must be 'name=value'");
			String n = s.substring(0,eidx);
			String v = s.substring(eidx+1);
			log.debug("Setting property '{}' to '{}'", n, v);
			rs.setProperty(n, v);
		}

		mainThread = makeInstance(rs);
		if (noLimitsArg.getValue() == true)
			rs.setProperty("noLimits", "true");

		for(int i=0; i<scriptNameArg.NumberOfValues(); i++)
		{
			String sn = scriptNameArg.getValue(i);
			mainThread.addScriptName(sn);
		}

		// Set status monitor file.
		String statmonFile = settings.routingStatusDir
			+ File.separator + rsName + ".stat";
		s = statmonArg.getValue().trim();
		if (s.equals("-"))
			statmonFile = null;
		else if (s.length() > 0)
			statmonFile = s;
		mainThread.setStatusFile(statmonFile);

		if (removeRedundantArg.getValue())
			rs.setProperty("removeRedundantData", "true");

		if (compConfigArg.getValue() != null && compConfigArg.getValue().length() > 0)
			rs.setProperty("compConfig", compConfigArg.getValue());

		if (usgsSummaryFileArg.getValue() != null && usgsSummaryFileArg.getValue().length() > 0)
			rs.setProperty("usgsSummaryFile", usgsSummaryFileArg.getValue());

		mainThread.exitOnCompletion = true;

		String ds = lrgsSourceArg.getValue();
		if (ds != null && ds.length() > 0)
		{
			StringTokenizer st = new StringTokenizer(ds," :");
			String host = st.nextToken();
			mainThread.explicitDataSource = new DataSource("exp:" + host, "lrgs");
			mainThread.explicitDataSource.setDataSourceArg("hostname=" + host);
			if (st.hasMoreTokens())
			{
				String port = st.nextToken();
				mainThread.explicitDataSource.setDataSourceArg(mainThread.explicitDataSource.getDataSourceArg() + (", port=" + port));
			}

			String user = "decodes";
			if (st.hasMoreTokens())
				user = st.nextToken();
			mainThread.explicitDataSource.setDataSourceArg(mainThread.explicitDataSource.getDataSourceArg() + (", username=" + user));
			if (st.hasMoreTokens())
			{
				String pw = st.nextToken();
				mainThread.explicitDataSource.setDataSourceArg(mainThread.explicitDataSource.getDataSourceArg() + (", password=" + pw));
			}
		}
		
		// Set rs.explicitConsumerDir from -F command line arg here.
		if (dirConsumerArg.getValue() != null && dirConsumerArg.getValue().trim().length() > 0)
			mainThread.explicitConsumerDir = EnvExpander.expand(dirConsumerArg.getValue()).trim();
		
		// Establish a TSDB Comp Lock if 1.) No -k arg on command line, 2.) a proc record
		// exists that matches the RS name with type 'routingspec', 
		// and 3.) the proc's monitor flag is true.
		if (lockpath == null || lockpath.trim().length() == 0)
		{
			LoadingAppDAI loadingAppDAO = Database.getDb().getDbIo().makeLoadingAppDAO();
			try
			{
				try { mainThread.rsProcRecord = loadingAppDAO.getComputationApp(rsName); }
				catch (NoSuchObjectException e) { mainThread.rsProcRecord = null; }
				if (mainThread.rsProcRecord != null
				 && TextUtil.strEqualIgnoreCase(
					 mainThread.rsProcRecord.getProperty("appType"), "routingspec")
				 && TextUtil.str2boolean(
					 mainThread.rsProcRecord.getProperty("monitor")))
				{
					// Create a TSDB_COMP_PROC_LOCK record
					//=====================================================
					int pid = TsdbAppTemplate.determinePID();
					
					String hostname = "unknown";
					try { hostname = InetAddress.getLocalHost().getHostName(); }
					catch(Exception e) 
					{
						try { hostname = InetAddress.getLocalHost().toString(); }
						catch(Exception ex) { hostname = "unknown"; }
					}
	
					mainThread.myLock = loadingAppDAO.obtainCompProcLock(
						mainThread.rsProcRecord, pid, hostname);
					
					// If this process can be monitored, start an Event Server.
					try 
					{
						CompEventSvr compEventSvr = new CompEventSvr(
							TsdbAppTemplate.determineEventPort(
								mainThread.rsProcRecord));
						compEventSvr.startup();
					}
					catch(IOException ex)
					{
						log.atError()
						   .setCause(ex)
						   .log("Cannot create Event server -- no events available to external clients.");
					}
				}
					
			}
			catch (LockBusyException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("Routing Spec not started: database lock for process '{}' is busy.", rsName);
				Database.getDb().getDbIo().close();
				System.exit(0);
			}
			finally
			{
				loadingAppDAO.close();
			}
		}

		mainThread.start();
		mainThread.join();
	}
	
	/**
	  Called periodically to write the status to a file.
	  The status file will be a Java-style properties file and will
	  be stored in the 'routstat' directory under the DECODES installation.
	*/
	protected void writeStatus()
	{
		if (myStatus != null)
		{
			myStatus.setLastMessageTime(new Date(lastRecvTime));
			myStatus.setNumMessages((int)numMsgsRun);
			myStatus.setNumDecodesErrors((int)numErrsRun);
			myStatus.setNumPlatforms(mediumIdsSeen.size());
			if (source != null)
				myStatus.setLastSource(source.getActiveSource());
			if (consumer != null)
				myStatus.setLastConsumer(consumer.getActiveOutput());
			myExec.writeStatus(currentStatus);
		}

		long curDay = System.currentTimeMillis() / (24 * 60 * 60 * 1000L);
		if (lastDay != curDay)
		{
			numMsgsToday = 0;
			numErrsToday = 0;
			lastDay = curDay;
		}
		if (statmonFile == null)
			return;
		try
		{
			FileWriter fw = new FileWriter(statmonFile);
			long ts = System.currentTimeMillis();
			fw.write("StartTime=" + ts + "\r\n");
			fw.write("SpecName=" + rs.getName() + "\r\n");
			fw.write("Status=" + currentStatus + "\r\n");
			fw.write("RunStartTime=" + runStartTime + "\r\n");
			fw.write("LastRecvTime=" + lastRecvTime + "\r\n");
			fw.write("NumMsgsRun=" + numMsgsRun+"/"+numErrsRun + "\r\n");
			fw.write("NumMsgsToday=" + numMsgsToday+"/"+numErrsToday + "\r\n");
			String s = (source == null ? "(none)" : source.getActiveSource());
			fw.write("CurrentServer=" + s + "\r\n");
			s = (consumer==null ? "(none)" : consumer.getActiveOutput());
			fw.write("Output=" + s + "\r\n");
			fw.write("Format=" + rs.outputFormat + "\r\n");
			fw.write("EndTime=" + ts + "\r\n");
			fw.close();
		}
		catch(IOException ex)
		{
			log.atWarn().setCause(ex).log("Cannot write status to '{}'", statmonFile.getPath());
		}
	}

	protected void initStatus()
	{
		if (statmonFile == null)
			return;
		try
		{
			File parent = statmonFile.getParentFile();
			if (parent != null)
			{
				if (!parent.isDirectory())
					parent.mkdirs();
			}
			Properties props = new Properties();
			props.load(new FileInputStream(statmonFile));
			String s = props.getProperty("LastRecvTime");
			try
			{
				lastRecvTime = Integer.parseInt(s);
			}
			catch(NumberFormatException ex) 
			{
				lastRecvTime = System.currentTimeMillis();
			}
			s = props.getProperty("NumMsgsToday");
			int idx;
			if (s != null && (idx = s.indexOf('/')) != -1)
			{
				try
				{
					numMsgsToday = Integer.parseInt(s.substring(0,idx));
					numErrsToday = Integer.parseInt(s.substring(idx+1));
				}
				catch(NumberFormatException ex) 
				{
					log.atWarn().setCause(ex).log("Cannot parse cumulative daily status from '{}'", s);
				}
			}
		}
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Cannot load previous run status from '{}' -- assuming 1st run.", statmonFile.getPath());
		}
	}

	public void forceReInit()
	{
		reinitForced = true;
	}

	/**
	 * This method is ONLY called when a RoutingSpec is started manually
	 * from the command line.
	 * @param rs the RoutingSpec object to use.
	 */
	public static RoutingSpecThread makeInstance(RoutingSpec rs)
		throws DbIoException
	{
		ScheduleEntryDAI scheduleEntryDAO = 
			Database.getDb().getDbIo().makeScheduleEntryDAO();
		
		// Look up the 'manual' schedule entry name for this routing spec
		String seName = rs.getName() + "-manual";
		try
		{
			
			ScheduleEntry scheduleEntry = null;
			if (scheduleEntryDAO != null)
				scheduleEntry = scheduleEntryDAO.readScheduleEntry(seName);
			
			// create one if it doesn't exist.
			if (scheduleEntry == null)
			{
				scheduleEntry = new ScheduleEntry(seName);
				scheduleEntry.setRoutingSpecId(rs.getId());
				scheduleEntry.setRoutingSpecName(rs.getName());
				scheduleEntry.setEnabled(false);
			}
			scheduleEntry.setEnabled(true);
			scheduleEntry.setLastModified(new Date());
			scheduleEntry.setTimezone(rs.outputTimeZoneAbbr);
			if (scheduleEntryDAO != null)
				scheduleEntryDAO.writeScheduleEntry(scheduleEntry);
			
			// Construct a ScheduleEntryExecutive with the SE.
			ScheduleEntryExecutive executive = new ScheduleEntryExecutive(scheduleEntry, null);
			executive.initialize();
			return executive.makeThread();
		}
		catch (DbIoException ex)
		{
			throw new DbIoException("Cannot read manual sched entry: " + ex, ex);
		}
		finally
		{
			if (scheduleEntryDAO != null)
				scheduleEntryDAO.close();
		}
	}

	public ScheduleEntryExecutive getMyExec()
	{
		return myExec;
	}

	public void setMyExec(ScheduleEntryExecutive myExec)
	{
		this.myExec = myExec;
	}

	public ScheduleEntryStatus getMyStatus()
	{
		return myStatus;
	}

	public void setMyStatus(ScheduleEntryStatus myStatus)
	{
		this.myStatus = myStatus;
	}

	public DcpMsg getLastDcpMsg()
	{
		return lastDcpMsg;
	}

	public void setShutdownHook(Runnable shutdownHook)
	{
		this.shutdownHook = shutdownHook;
	}

	public CompAppInfo getRsProcRecord()
	{
		return rsProcRecord;
	}
}

class StatusWriteThread extends Thread
{
	RoutingSpecThread myrs;
	boolean shutdown;

	StatusWriteThread(RoutingSpecThread rs)
	{
		myrs = rs;
		shutdown = false;
		setDaemon(true);
		setName(rs.getName()+"-status writer");
	}

	public void run()
	{
		while(!shutdown)
		{
			try { sleep(5000L); }
			catch(InterruptedException ex) {}
			myrs.writeStatus();
		}
	}
}
