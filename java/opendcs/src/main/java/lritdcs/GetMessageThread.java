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

import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import org.opendcs.authentication.AuthSourceService;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.io.File;

import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import decodes.datasource.*;
import decodes.db.*;
import decodes.util.*;
import ilex.util.IDateFormat;
import ilex.util.TextUtil;

/**
 This class retrieves messages from the DDS servers, segregates them into
 priority H, M, and L by search-criteria, and then saves them in the
 appropriate file.
*/
public class GetMessageThread extends LritDcsThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/// One file writer for each priority.
	LritDcsFileWriter outputHigh;
	LritDcsFileWriter outputMedium;
	LritDcsFileWriter outputLow;

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

	private LinkedList<QueuedMessage> mediumLowQueue = new LinkedList<QueuedMessage>();
	private static final long QUEUE_TIME = 10000L;

	public GetMessageThread()
	{
		super("GetMessageThread");

		outputHigh = null;
		outputMedium = null;
		outputLow = null;

		shutdownFlag = false;
		dataSource = null;
		myStatus = LritDcsMain.instance().getStatus();

		host1 = host2 = host3 = null;
		port1 = port2 = port3 = 16003;
		user1 = user2 = user3 = null;

		rsProps = new Properties();

		reconfigSource = false;

		retryPeriod = 600;
		timeoutPeriod = 30;
	}

	/// Called whenever config has changed from Observer.
	protected void getConfigValues(LritDcsConfig cfg)
	{
		outputHigh.setMaxMsgs(cfg.getMaxMsgsHigh());
		outputHigh.setMaxFileBytes(cfg.getMaxBytesHigh());
		outputHigh.setMaxFileSeconds(cfg.getMaxSecondsHigh());
		outputMedium.setMaxMsgs(cfg.getMaxMsgsMedium());
		outputMedium.setMaxFileBytes(cfg.getMaxBytesMedium());
		outputMedium.setMaxFileSeconds(cfg.getMaxSecondsMedium());
		outputLow.setMaxMsgs(cfg.getMaxMsgsLow());
		outputLow.setMaxFileBytes(cfg.getMaxBytesLow());
		outputLow.setMaxFileSeconds(cfg.getMaxSecondsLow());
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
		String dir = cfg.getLritDcsHome() + File.separator + "high";
		outputHigh = new LritDcsFileWriter(Constants.HighPri, dir);
		outputHigh.checkSearchCriteria();

		dir = cfg.getLritDcsHome() + File.separator + "medium";
		outputMedium = new LritDcsFileWriter(Constants.MediumPri, dir);
		outputMedium.checkSearchCriteria();

		dir = cfg.getLritDcsHome() + File.separator + "low";
		outputLow = new LritDcsFileWriter(Constants.LowPri, dir);
		outputLow.checkSearchCriteria();

		// Decodes database used for data source records.
		// Initialize MINIMAL database here with no db IO capability.
		try
		{
			new decodes.db.Database(); // this sets the singleton.
			DbEnum dataSourceTypeEnum = new DbEnum("DataSourceType");
			dataSourceTypeEnum.addValue("lrgs", "LRGS Data Source",
				"decodes.datasource.LrgsDataSource", null);
			dataSourceTypeEnum.addValue("hotbackupgroup", "Hot Backup Group",
				"decodes.datasource.HotBackupGroup", null);
			// Note - don't need to explicitely add, DbEnum ctor will do it.

		}
		catch(DecodesException ex)
		{
			throw new InitFailedException("Cannot initialize DECODES: ", ex);
		}

		// Call update an initial time, then register for updates.
		getConfigValues(LritDcsConfig.instance());
		registerForConfigUpdates();
	}

	/**
	  Thread.run method to run retrieval.
	*/
	public void run()
	{
		boolean noDataSource = false;

		try
		{
			long lastScCheck = System.currentTimeMillis();
			while(!shutdownFlag)
			{
				long now = System.currentTimeMillis();
				if (now - lastScCheck > 1000L)
				{
					outputHigh.checkSearchCriteria();
					outputMedium.checkSearchCriteria();
					outputLow.checkSearchCriteria();
					lastScCheck = System.currentTimeMillis();
				}

				if (reconfigSource)
				{
					reconfigSource = false;
					try { initDataSource(); }
					catch(DataSourceInitException ex)
					{
						dataSource = null;
					}
				}

				if (dataSource == null)
				{
					if (!noDataSource)
					{
						log.warn("{} Data source invalid, pausing for 10 seconds.", Constants.EVT_NO_DATA_SOURCE);
						noDataSource = true;
					}
					try { sleep(10000L); }
					catch (InterruptedException e) {}
					reconfigSource = true;
					//MJM Comment out the continue so queue processing can continue even if we have no DS.
					continue;
				}
				else if (noDataSource)
				{
					log.info("{} Data Source valid.", Constants.EVT_NO_DATA_SOURCE);
					noDataSource = false;
				}

				try
				{
					RawMessage lrgsMessage = null;
					// Retrieve the next raw message from the data source.
					if (dataSource != null)
						lrgsMessage = dataSource.getRawMessage();
					if (lrgsMessage != null)
					{
						LrgsDataSource cds =
							(LrgsDataSource)dataSource.getActiveMember();
						myStatus.lastDataSource = cds == null ? "" : cds.getHostName();
						myStatus.lastRetrieval = System.currentTimeMillis();

						byte[] data = lrgsMessage.getData();
						DcpMsg dcpMsg = lrgsMessage.getOrigDcpMsg();
						if (dcpMsg == null)
						{
							dcpMsg = new DcpMsg(data, data.length, 0);
						}
						DcpMsgIndex msgIndex = new DcpMsgIndex();
						msgIndex.setMessageParams(dcpMsg);
						if (outputHigh.passesCriteria(msgIndex))
						{
							processMessage(dcpMsg, msgIndex);
						}
						else
							enqueueMediumLow(new QueuedMessage(dcpMsg, msgIndex));
					}

					QueuedMessage queuedMessage = null;
					now = System.currentTimeMillis();
					int nQ = 0;
					while (!mediumLowQueue.isEmpty()
					 && (queuedMessage = mediumLowQueue.peekLast()) != null
					 && (now - queuedMessage.enqueueMsec) > QUEUE_TIME)
					{
						queuedMessage = mediumLowQueue.removeLast();
						processMessage(queuedMessage.msg, queuedMessage.idx);
						nQ++;
					}

					if (lrgsMessage == null)
					{
						log.info("{} Data source failed to return message, pausing for 2 seconds.", 0);
						try { sleep(2000L); }
						catch (InterruptedException e) {}
						continue;
					}
				}
				catch(UnknownPlatformException ex)
				{
					// This shouldn't happen, we turned it off in DataSourceExec.
					log.atWarn()
					   .setCause(ex)
					   .log("{} Data source '{}' -- skipped", 0, dataSource.getName());
					continue;
				}
				catch(DataSourceEndException ex)
				{
					// This shouldn't happen, we never set an until time.
					log.atWarn()
					   .setCause(ex)
					   .log("{} Normal termination on data source '{}'", dataSource.getName());
					continue;
				}
				catch(DataSourceException ex)
				{
					log.atWarn()
					   .setCause(ex)
					   .log("{}- Error on data source '{}'",
					        Constants.EVT_DATA_SOURCE_ERR, dataSource.getName());
					dataSource = null;
					reconfigSource = true;
					continue;
				}

				// Check file time ranges.
				outputHigh.checkFileTimeRange();
				outputMedium.checkFileTimeRange();
				outputLow.checkFileTimeRange();
			}
		}
		catch(Exception ex)
		{
			log.atError()
			   .setCause(ex)
			   .log("{} Unexpected Exception in main loop. Exiting.", Constants.EVT_INTERNAL_ERR);
			LritDcsMain.instance().shutdown();
		}

		LritDcsConfig.instance().deleteObserver(this);

		// Finish off any partial files.
		if (outputHigh.getNumMessages() > 0)
			outputHigh.saveFile();
		if (outputMedium.getNumMessages() > 0)
			outputMedium.saveFile();
		if (outputLow.getNumMessages() > 0)
			outputLow.saveFile();

		if (dataSource != null)
		{
			dataSource.close();
			dataSource = null;
		}
	}

	private void enqueueMediumLow(QueuedMessage newMsg)
	{
		// Traverse the queue to see if this message is already here.
		for(QueuedMessage qm : mediumLowQueue)
		{
			MatchVal mv = isMatch(newMsg.idx, qm.idx);
			if (mv == MatchVal.SAVE_QUEUED || mv == MatchVal.DISCARD)
			{
				debug1("" + mv.toString());
				return;
			}
			if (mv == MatchVal.SAVE_NEW)
			{
				qm.idx = newMsg.idx;
				qm.msg = newMsg.msg;
				return;
			}
		}

		// Fell through without finding match
		mediumLowQueue.addFirst(newMsg);
	}

	enum MatchVal { NO_MATCH, SAVE_NEW, SAVE_QUEUED, DISCARD };

	public MatchVal isMatch(DcpMsgIndex newIdx, DcpMsgIndex queuedIdx)
	{
		// Don't transmit DAPS Status messages over LRIT.
		char newFC = newIdx.getFailureCode();
		if (newFC != 'G' && newFC != '?')
			return MatchVal.DISCARD;

		if (!newIdx.getDcpAddress().equals(queuedIdx.getDcpAddress()))
			return MatchVal.NO_MATCH;

		if (newIdx.getChannel() != queuedIdx.getChannel())
			return MatchVal.NO_MATCH;

		long deltaT = newIdx.getXmitTime().getTime() -
			queuedIdx.getXmitTime().getTime();
		if (deltaT < -5000L || deltaT > 5000L)
			return MatchVal.NO_MATCH;

		char queuedFC = queuedIdx.getFailureCode();

		if (newFC == 'G')
		{
			if (queuedFC == '?')
				return MatchVal.SAVE_NEW;
		}
		else if (queuedFC == 'G')
			return MatchVal.SAVE_QUEUED;

		// they both have equal quality.
		if (DcpMsgFlag.hasAccurateCarrier(queuedIdx.getFlagbits()))
			return MatchVal.SAVE_QUEUED;
		else if (DcpMsgFlag.hasAccurateCarrier(newIdx.getFlagbits()))
			return MatchVal.SAVE_NEW;

		// else either they both have accurate carrier or they both don't.
		return MatchVal.SAVE_QUEUED;
	}

	private void processMessage(DcpMsg dcpMsg, DcpMsgIndex msgIndex)
	{
		if (outputHigh.passesCriteria(msgIndex))
		{
			outputHigh.saveMessage(dcpMsg);
			myStatus.incrementMsgHigh();
		}
		else if (outputMedium.passesCriteria(msgIndex))
		{
			outputMedium.saveMessage(dcpMsg);
			myStatus.incrementMsgMedium();
		}
		else if (outputLow.passesCriteria(msgIndex))
		{
			outputLow.saveMessage(dcpMsg);
			myStatus.incrementMsgLow();
		}
		else
		{
			log.warn("{}- Message from platform '{}' discarded because it doesn't " +
					 "pass criteria for any priority.",
					 Constants.EVT_MSG_DISCARDED, dcpMsg.getDcpAddress().toString());
		}
	}

	// Initialize the data source object.
	protected void initDataSource()
		throws DataSourceInitException
	{
		log.trace("initDataSource(retry={}, timeout={})", retryPeriod, timeoutPeriod);

		// Initialize the routing spec's data source.
		rsProps.setProperty("recheck", "" + retryPeriod);
		rsProps.setProperty("lrgs.timeout", "" + timeoutPeriod);
		try
		{
			// Construct hot backup group DS with my components.
			DataSource groupDbDs = new DataSource("DdsGroup", "hotbackupgroup");
			int numMembers = 0;

			// Initialize up to 3 DDS connections in the group.
			if (host1 != null && user1 != null)
			{
				DataSource dbds = new DataSource("dds1", "lrgs");
				String pw = tryGetPassword(user1);
				dbds.setDataSourceArg("hostname=" + host1 + ", "
				+ "port=" + port1 + ", "
				+ "user=" + user1);
				if (pw != null)
					dbds.setDataSourceArg(dbds.getDataSourceArg() + ", password=" + pw);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host2 != null && user2 != null)
			{
				DataSource dbds = new DataSource("dds2", "lrgs");
				String pw = tryGetPassword(user2);
				dbds.setDataSourceArg("hostname=" + host2 + ", "
				+ "port=" + port2 + ", "
				+ "user=" + user2);
				if (pw != null)
					dbds.setDataSourceArg(dbds.getDataSourceArg() + ", password=" + pw);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host3 != null && user3 != null)
			{
				DataSource dbds = new DataSource("dds3", "lrgs");
				String pw = tryGetPassword(user3);
				dbds.setDataSourceArg("hostname=" + host3 + ", "
				+ "port=" + port3 + ", "
				+ "user=" + user3);
				if (pw != null)
					dbds.setDataSourceArg(dbds.getDataSourceArg() + ", password=" + pw);

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (numMembers == 0)
			{
				String msg = "No data sources specified!";
				log.warn("{} {}", Constants.EVT_NO_DATA_SOURCE, msg);
				throw new DataSourceInitException(msg);
			}

			String since="";
			long lastRetrieval =
				LritDcsMain.instance().getStatus().lastRetrieval;
			if (lastRetrieval == 0L)
				// First time ever? Just back up 5 minutes
				since = "now - 5 minutes";
			else
			{
				// Never back up more than an hour.
				if (System.currentTimeMillis() - lastRetrieval > 3600000L)
					lastRetrieval = System.currentTimeMillis() - 3600000L;
				since = IDateFormat.toString(new Date(lastRetrieval), false);
			}

			dataSource = (HotBackupGroup)groupDbDs.makeDelegate();
			dataSource.setAllowNullPlatform(true);
			dataSource.setAllowDapsStatusMessages(true);

			dataSource.init(rsProps, since, "", new Vector<NetworkList>());
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

	private String tryGetPassword(String username)
	{
		File authFile = new File(username + ".auth");
		if (!authFile.canRead())
		{
			log.debug("{} does not exist.", authFile.getPath());
			return null;
		}
		try
		{
			Properties credentials = AuthSourceService.getFromString(authFile.getPath())
													  .getCredentials();
			log.debug("Read password from {}", authFile.getPath());
			return credentials.getProperty("password");
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Error reading auth file '{}'", authFile.getPath());
			return null;
		}
	}
}
