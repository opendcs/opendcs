/*
* $Id$
*
* $Log$
* Revision 1.4  2014/08/22 17:23:10  mmaloney
* 6.1 Schema Mods and Initial DCP Monitor Implementation
*
* Revision 1.3  2014/05/30 13:15:35  mmaloney
* dev
*
* Revision 1.2  2014/05/28 13:09:31  mmaloney
* dev
*
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.5  2012/12/12 16:01:31  mmaloney
* Several updates for 5.2
*
* Revision 1.4  2009/10/09 14:52:26  mjmaloney
* Added flag bytes and carrier times to LRIT File.
*
* Revision 1.3  2008/09/05 13:18:00  mjmaloney
* LRGS 7 dev
*
* Revision 1.2  2008/08/06 19:41:04  mjmaloney
* dev
*
* Revision 1.1  2008/04/04 18:21:16  cvs
* Added legacy code to repository
*
* Revision 1.13  2005/12/30 19:40:59  mmaloney
* dev
*
* Revision 1.12  2005/07/28 20:33:06  mjmaloney
* dev
*
* Revision 1.11  2005/03/21 14:33:55  mjmaloney
* Enum --> DbEnum
*
* Revision 1.10  2004/09/08 12:40:36  mjmaloney
* dev
*
* Revision 1.9  2004/05/18 22:52:39  mjmaloney
* dev
*
* Revision 1.8  2003/08/18 14:47:59  mjmaloney
* bug fixes.
*
* Revision 1.7  2003/08/15 20:13:07  mjmaloney
* dev
*
* Revision 1.6  2003/08/11 23:38:11  mjmaloney
* dev
*
* Revision 1.5  2003/08/11 17:29:51  mjmaloney
* dev
*
* Revision 1.4  2003/08/11 15:59:19  mjmaloney
* dev
*
* Revision 1.3  2003/08/11 01:33:58  mjmaloney
* dev
*
* Revision 1.2  2003/08/10 02:22:47  mjmaloney
* dev.
*
* Revision 1.1  2003/08/06 23:29:24  mjmaloney
* dev
*
*/
package lritdcs;

import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Vector;

import java.io.File;

import ilex.util.*;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.common.DcpMsgIndex;
import decodes.datasource.*;
import decodes.db.*;
import decodes.util.*;

/**
 This class retrieves messages from the DDS servers, segregates them into
 priority H, M, and L by search-criteria, and then saves them in the 
 appropriate file.
*/
public class GetMessageThread
	extends LritDcsThread
{
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
//		rsProps.setProperty("single", "true");

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
			throw new InitFailedException("Cannot initialize DECODES: " + ex);
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
//		DcpMsg dcpMsg = new DcpMsg();
//		DcpMsgIndex msgIndex = new DcpMsgIndex();

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
						warning(Constants.EVT_NO_DATA_SOURCE,
							"Data source invalid, pausing for 10 seconds.");
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
					info(Constants.EVT_NO_DATA_SOURCE, "Data Source valid.");
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
						//debug3(
						//	"Message from '" + dataSource.getName() + "': "
						//	+ new String(rm.getHeader()));
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
//					if (nQ > 0)
//						info(0, "Processed " + nQ + " from the mediumLowQueue");
					
					if (lrgsMessage == null)
					{
						info(0, 
				"Data source failed to return message, pausing for 2 seconds.");
						try { sleep(2000L); }
						catch (InterruptedException e) {}
						continue;
					}
				}
				catch(UnknownPlatformException ex)
				{
					// This shouldn't happen, we turned it off in DataSourceExec.
					warning(0, "Data source '" + dataSource.getName() 
						+ "': " + ex + " -- skipped");
					continue;
				}
				catch(DataSourceEndException ex)
				{
					// This shouldn't happen, we never set an until time.
					warning(0, "Normal termination on data source '"
						+ dataSource.getName());
					continue;
				}
				catch(DataSourceException ex)
				{
					warning(Constants.EVT_DATA_SOURCE_ERR,
						"- Error on data source '" 
						+ dataSource.getName() + "': " + ex);
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
			fatal(Constants.EVT_INTERNAL_ERR,
				"Unexpected Exception in main loop. Exiting: " + ex);
			ex.printStackTrace(Logger.instance().getLogOutput());
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
			warning(Constants.EVT_MSG_DISCARDED,
				"- Message from platform '"
				+ dcpMsg.getDcpAddress().toString() 
				+"' discarded because it doesn't pass criteria for "
				+ "any priority.");
	}

	// Initialize the data source object.
	protected void initDataSource()
		throws DataSourceInitException
	{
		debug3("initDataSource(retry=" + retryPeriod +
			", timeout=" + timeoutPeriod + ")");

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
				dbds.dataSourceArg =
					"hostname=" + host1 + ", "
					+ "port=" + port1 + ", "
					+ "user=" + user1;
				if (pw != null)
					dbds.dataSourceArg = dbds.dataSourceArg + ", password=" + pw;

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host2 != null && user2 != null)
			{
				DataSource dbds = new DataSource("dds2", "lrgs");
				String pw = tryGetPassword(user2);
				dbds.dataSourceArg =
					"hostname=" + host2 + ", "
					+ "port=" + port2 + ", "
					+ "user=" + user2;
				if (pw != null)
					dbds.dataSourceArg = dbds.dataSourceArg + ", password=" + pw;

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (host3 != null && user3 != null)
			{
				DataSource dbds = new DataSource("dds3", "lrgs");
				String pw = tryGetPassword(user3);
				dbds.dataSourceArg =
					"hostname=" + host3 + ", "
					+ "port=" + port3 + ", "
					+ "user=" + user3;
				if (pw != null)
					dbds.dataSourceArg = dbds.dataSourceArg + ", password=" + pw;

				groupDbDs.addGroupMember(numMembers++, dbds);
			}

			if (numMembers == 0)
			{
				String msg = "No data sources specified!";
				warning(Constants.EVT_NO_DATA_SOURCE, msg);
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
			throw new DataSourceInitException(ex.toString());
		}
		catch(DataSourceException ex)
		{
			throw new DataSourceInitException(ex.toString());
		}
	}
	
	private String tryGetPassword(String username)
	{
		File authFile = new File(username + ".auth");
		if (!authFile.canRead())
		{
			Logger.instance().debug1(authFile.getPath() + " does not exist.");
			return null;
		}
		try
		{
			UserAuthFile uaf = new UserAuthFile(authFile);
			uaf.read();
			Logger.instance().debug1("Read password from " + authFile.getPath());
			return uaf.getPassword();
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Error reading auth file '" + authFile.getPath() + "': " + ex);
			return null;
		}
	}
}
