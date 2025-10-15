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

import java.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.File;

import lrgs.common.SearchCriteria;
import decodes.datasource.*;
import decodes.db.*;
import ilex.util.TextUtil;

/**
 This class handles manual retransmit requests via a separate connection
 to the DDS servers.
*/
public class ManualRetransThread extends LritDcsThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	LritDcsFileWriter output;

	/// The data source that is providing raw messages.
	private HotBackupGroup dataSource;

	/// The status object that we update.
	LritDcsStatus myStatus;

	// Local copies of DDS config so I can detect changes:
	String host1, host2, host3;
	int port1, port2, port3;
	String user1, user2, user3;
	boolean reconfigSource;
	int retryPeriod;
	int timeoutPeriod;

	/// Could be used in future to pass params to data source;
	Properties rsProps;

	/// Needed by DECODES datasource for init method, but no used here.
	Vector<NetworkList> nlVector;

	/// Each request is represented by a search criteria on the queue.
	LinkedList<SearchCriteria> scQueue;

	/// flag allowing UI to cancel when manual queue is flushed.
	public boolean cancelled;

	public ManualRetransThread()
	{
		super("ManualRetransThread");

		output = null;

		shutdownFlag = false;
		dataSource = null;
		myStatus = LritDcsMain.instance().getStatus();

		host1 = host2 = host3 = null;
		port1 = port2 = port3 = 16003;
		user1 = user2 = user3 = null;

		rsProps = new Properties();
		rsProps.setProperty("single", "false");  // Multi mode for retrans requests.

		nlVector = new Vector<NetworkList>();
		reconfigSource = false;

		retryPeriod = 600;
		timeoutPeriod = 30;

		scQueue = new LinkedList<SearchCriteria>();
	}

	/// Called whenever config has changed from Observer.
	protected void getConfigValues(LritDcsConfig cfg)
	{
		/*
		  Ideally, every manual request should result in a single LRIT file,
		  so we set the limits pretty high, and then flush the output manually
		  when we get the all-done response from the DDS server.
		*/
		output.setMaxMsgs(300);
		output.setMaxFileBytes(20000);
		output.setMaxFileSeconds(600);

		// Note: changes to searchcrits will be auto detected by writers.

		// Check for changes to server list & reconfigure HBG if nec.
		reconfigSource =
			!TextUtil.strEqual(host1, cfg.getDds1HostName())
		 || port1 != cfg.getDds1Port()
		 || !TextUtil.strEqual(user1, cfg.getDds1UserName())
		 ||	!TextUtil.strEqual(host2, cfg.getDds2HostName())
		 || port2 != cfg.getDds2Port()
		 || !TextUtil.strEqual(user2, cfg.getDds2UserName())
		 ||	!TextUtil.strEqual(host3, cfg.getDds3HostName())
		 || port3 != cfg.getDds3Port()
		 || !TextUtil.strEqual(user3, cfg.getDds3UserName())
		 || retryPeriod != cfg.getDdsRetryPeriod()
		 || timeoutPeriod != cfg.getDdsTimeOut();

		if (reconfigSource)
		{
			host1 = cfg.getDds1HostName();
			port1 = cfg.getDds1Port();
			user1 = cfg.getDds1UserName();
			host2 = cfg.getDds2HostName();
			port2 = cfg.getDds2Port();
			user2 = cfg.getDds2UserName();
			host3 = cfg.getDds3HostName();
			port3 = cfg.getDds3Port();
			user3 = cfg.getDds3UserName();
			retryPeriod = cfg.getDdsRetryPeriod();
			timeoutPeriod = cfg.getDdsTimeOut();
		}
		// Note: the reconfigSource flag will be used in the main thread loop
		// to create a new HotBackupGroup. Don't do it here because of
		// potential sync problems.
	}

	public void init()
		throws InitFailedException
	{
		LritDcsConfig cfg = LritDcsConfig.instance();

		// Initialize the LritDcsFileWriter objects that provide output.
		String dir = cfg.getLritDcsHome() + File.separator + "low";
		output = new LritDcsFileWriter(Constants.LowPri, dir, false);

		// Note - we rely on the GetMessageThread to initialize Decodes DB.

		// Call update at initial time, then register for updates.
		getConfigValues(LritDcsConfig.instance());
		registerForConfigUpdates();
	}

	public synchronized void queueSC(SearchCriteria sc)
	{
		scQueue.addLast(sc);
	}

	private synchronized SearchCriteria getNextSC()
	{
		try { return (SearchCriteria)scQueue.removeFirst(); }
		catch(NoSuchElementException ex)
		{
			return null;
		}
	}

	/**
	  Thread.run method to work the manual requests, one at a time.
	*/
	public void run()
	{
		log.info("Starting Manual Retransmit Thread");

		try
		{
			while(!shutdownFlag)
			{
				SearchCriteria sc = getNextSC();
				if (sc == null)
				{
					try { sleep(5000L); }
					catch(InterruptedException ex) {}
					continue;
				}

				try
				{
					initDataSource(sc);
					log.info("{}- Initialized data source for manual retransmits.",
							 -Constants.EVT_DATA_SOURCE_ERR);
				}
				catch(DataSourceInitException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("Could not initialize data source for manual retransmit -- aborted request.",
					   		Constants.EVT_DATA_SOURCE_ERR);
					try { sleep(10000L); }
					catch (InterruptedException e) {}
					continue;
				}

				processRetransRequest(sc);
			}
		}
		catch(Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("{}- Unexpected Exception in main loop. Exiting.", Constants.EVT_INTERNAL_ERR);
			LritDcsMain.instance().shutdown();
		}

		LritDcsConfig.instance().deleteObserver(this);
	}


	private void processRetransRequest(SearchCriteria sc)
	{
		log.info("Manual Retrans Request.");
		cancelled = false;
		while(!cancelled)
		{
			RawMessage rm = null;
			try
			{
				// Retrieve the next raw message from the data source.
				rm = dataSource.getRawMessage();
				if (rm == null)
				{
					log.info("Data source failed to return message, pausing for 5 seconds.");
					try { sleep(5000L); }
					catch (InterruptedException e) {}
					continue;
				}

				LrgsDataSource cds =
					(LrgsDataSource)dataSource.getActiveMember();
				myStatus.lastDataSource = cds == null ? "" : cds.getHostName();
			}
			catch(UnknownPlatformException ex)
			{
				// This shouldn't happen, we turned it off in DataSourceExec.
				log.atWarn().setCause(ex).log("Data source '{}' -- Skipped", dataSource.getName());
			}
			catch(DataSourceEndException ex)
			{
				int n = output.getNumMessages();
				log.info("Retrieved data for manual retransmit request: {} messages resulted.", n);
				break;
			}
			catch(DataSourceException ex)
			{
				log.atError()
				   .setCause(ex)
				   .log("{}- Error on manual retrans data source '{}'",
				   		Constants.EVT_DATA_SOURCE_ERR, dataSource.getName());
				dataSource = null;
			}

			// We now have a raw message. Construct DcpMsgIndex from it.
			myStatus.lastRetrieval = System.currentTimeMillis();
			try
			{
				// note - all messages must pass criteria!
				output.saveMessage(rm.getOrigDcpMsg());
				myStatus.incrementMsgLow();
			}
			catch(IllegalStateException ex)
			{
				log.atWarn().setCause(ex).log("Message from platform '{}' discarded", rm.getMediumId());
			}

			// Check file time ranges.
			output.checkFileTimeRange();
		}
		if (cancelled)
		{
			log.warn("Manual retransmit request cancelled by user.");
		}

		// Broke out of above loop -- means this request is done!
		output.saveFile();
		dataSource.close();
		dataSource = null;
	}

	// Initialize the data source object.
	protected void initDataSource(SearchCriteria sc)
		throws DataSourceInitException
	{
		log.trace("initDataSource(retry={}, timeout={})", retryPeriod, timeoutPeriod);

		LritDcsConfig cfg = LritDcsConfig.instance();
		File f = new File(cfg.getLritDcsHome(), "searchcrit.manual");
		try { sc.saveFile(f); }
		catch(IOException ex)
		{
			String msg = "Cannot save '" + f.getPath() + "'";
			throw new DataSourceInitException(msg, ex);
		}

		// Initialize the routing spec's data source.
		rsProps.setProperty("recheck", "" + retryPeriod);
		rsProps.setProperty("lrgs.timeout", "" + timeoutPeriod);
		rsProps.setProperty("searchcrit", f.getPath());
		try
		{
			// Construct hot backup group DS with my components.
			DataSource groupDbDs = new DataSource("DdsGroup", "hotbackupgroup");
			int numMembers = 0;

			// Initialize up to 3 DDS connections in the group.
			if (host1 != null && user1 != null)
			{
				DataSource dbds = new DataSource("dds1", "lrgs");
				dbds.setDataSourceArg("hostname=" + host1 + ", "
				+ "port=" + port1 + ", "
				+ "user=" + user1);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host2 != null && user2 != null)
			{
				DataSource dbds = new DataSource("dds2", "lrgs");
				dbds.setDataSourceArg("hostname=" + host2 + ", "
				+ "port=" + port2 + ", "
				+ "user=" + user2);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host3 != null && user3 != null)
			{
				DataSource dbds = new DataSource("dds3", "lrgs");
				dbds.setDataSourceArg("hostname=" + host3 + ", "
				+ "port=" + port3 + ", "
				+ "user=" + user3);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (numMembers == 0)
				throw new DataSourceInitException("No data sources specified!");

			/*
			  Retrans requests really SHOULD have a finite time range. If the
			  user didn't provide one, provide defaults of last day here.
			*/
			String s = sc.getLrgsUntil();
			if (s == null || s.trim().length() == 0)
			{
				s = sc.getDapsUntil();
				if (s != null && s.trim().length() > 0)
					sc.setLrgsUntil(s);
				else
					sc.setLrgsUntil("now");
			}

			s = sc.getLrgsSince();
			if (s == null || s.trim().length() == 0)
			{
				s = sc.getDapsSince();
				if (s != null && s.trim().length() > 0)
					sc.setLrgsSince(s);
				else
					sc.setLrgsSince("now - 1 day");
			}

			dataSource = (HotBackupGroup)groupDbDs.makeDelegate();
			dataSource.setAllowNullPlatform(true);
			dataSource.setAllowDapsStatusMessages(true);

			dataSource.init(rsProps, sc.getLrgsSince(), sc.getLrgsUntil(), nlVector);
		}
		catch(InvalidDatabaseException ex)
		{
			throw new DataSourceInitException(ex.toString(), ex);
		}
		catch(DataSourceException ex)
		{
			throw new DataSourceInitException(ex.toString(), ex);
		}
	}
}
