/*
*  $Id$
*/
package decodes.dcpmon;

import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.Logger;
import ilex.util.ServerLock;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.XmitRecordDAI;

import lrgs.common.DcpAddress;
import lrgs.common.SearchCriteria;
import lrgs.gui.DecodesInterface;

import decodes.comp.BadConfigException;
import decodes.comp.ComputationProcessor;
import decodes.consumer.HtmlFormatter;
import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.RoutingSpec;
import decodes.drgsinfogui.DrgsReceiverIo;
import decodes.dupdcpgui.DuplicateIo;
import decodes.routing.RoutingSpecThread;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.util.ChannelMap;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;
import decodes.util.DecodesSettings;
import decodes.util.Pdt;
import decodes.util.hads.Hads;

/**
Main class for DCP Monitor Server
*/
public class DcpMonitor
	extends TsdbAppTemplate
{
	private String module = "DcpMonitor";
	
	/** The application uses a RoutingSpecThread to get & decode messages. */
	RoutingSpecThread routingSpecThread = null;

	/** We mock-up a dynamic routing spec for this application */
	private RoutingSpec routingSpec = null;

	/** Tells the application to die. */
	private boolean shutdownFlag = false;

//	private XmitRecord lastRec = null;

	// Start time for the real-time routing spec.
//	private long rtStartMsec = 0L;

	// Handles in-line computations for HTML DCP Message displays
	private ComputationProcessor compProcessor = null;

	// list of channels we are accepting data from
	private int myChannels[] = null;
	
	/** Holds app name, id, & description. */
	private CompAppInfo appInfo = null;
	private int evtPort = -1;
	private CompEventSvr compEventSvr = null;

	private TsdbCompLock myLock;

	private int pid = -1;
	private String hostname;
	private XRWriteThread xrWriteThread = null;
	private LoadingAppDAI loadingAppDao = null;
	private DcpMonitorConfig dcpMonitorConfig = new DcpMonitorConfig();

	private DcpMonitor()
	{
		super("dcpmon");
	}

    public static void main(String args[]) throws Exception
    {
		DcpMonitor dcpmon = new DcpMonitor();		
		dcpmon.execute(args);
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
	}

    @Override
	public void runApp()
	{
		info("DCP Monitor Starting =======================");
		
		// Determine process ID. Note -- We can't really do this in Java
		// without assuming a particular OS. Therefore, we rely on the
		// script that started us to set an environment variable PPID
		// for parent-process-ID. If not present, we default to 1.
		pid = 1;
		String ppids = System.getProperty("PPID");
		if (ppids != null)
		{
			try { pid = Integer.parseInt(ppids); }
			catch(NumberFormatException ex) { pid = 1; }
		}

		try { hostname = InetAddress.getLocalHost().getHostName(); }
		catch(Exception e) { hostname = "unknown"; }

		if (!loadConfig())
			System.exit(1);

		// Initialize the PDT and Channel Map. */
		Logger.instance().info("Initializing PDT");
		Pdt pdt = Pdt.instance();
		pdt.startMaintenanceThread(dcpMonitorConfig.pdtUrl, dcpMonitorConfig.pdtLocalFile);
		
		Logger.instance().info("Initializing Channel Map");
		ChannelMap cmap = ChannelMap.instance();
		cmap.startMaintenanceThread(dcpMonitorConfig.channelMapUrl, 
			dcpMonitorConfig.channelMapLocalFile);

		startThreads();
		
		long lastCheck = 0L;
		while(!shutdownFlag)
		{
			// If DB went down, periodically try to reconnect.
			if (theDb == null)
			{
				if (!attemptRestart())
				{
					warning("Database Connect Failed. Will retry in 20 seconds");
					try { Thread.sleep(20000L); }
					catch(InterruptedException ex) {}
					continue;
				}
				lastCheck = 0L; // force lock & config check right away
			}

			// Check lock and config file every 5 seconds.
			if (System.currentTimeMillis() - lastCheck > 5000L)
			{
				try
				{
					if (!checkLock())
					{
						failure("Lock is taken! Will exit.");
						System.exit(0);
					}
				}
				catch (DbIoException ex)
				{
					failure("Error attempting to get lock for app name '" + appNameArg.getValue() + ex);
					goDormant();
					continue;
				}
				checkConfig();
				lastCheck = System.currentTimeMillis();
			}
		}
		
		// The 'checker' does periodic checking for config & NL changes.
//		DcpMonitorChecker checker = new DcpMonitorChecker(this, 
//			dcpmonCfgFileName);
//		checker.start();

		// enter loop to monitor routing spec, restart it if it dies.
		//final DcpMonitor dcpmon = this;
		Thread rsmon = 
			new Thread()
			{
				public void run()
				{
					while(!shutdownFlag)
					{
						try 
						{
							routingSpecThread.join(); 
							Logger.instance().log(Logger.E_FAILURE,
								"RoutingSpec Thread Exited.");
							if (!shutdownFlag)
							{
								Logger.instance().info(
							"Will restart RoutingSpecThread after 1 minute.");
								DcpMonitorConsumer dmCsm = (DcpMonitorConsumer)
									routingSpecThread.getConsumer();
								if (dmCsm != null && dmCsm.lastTimeStamp != null)
								{
									long ms = dmCsm.lastTimeStamp.getTime();
									ms -= (1000L * 60 * 2); // 2 min.
									long now = System.currentTimeMillis();
									if (ms > now)
									{
										Logger.instance().warning(
						"Last timestamp in future, using current - 1 hour.");
										ms = now - 3600000L;
									}
									routingSpec.sinceTime = 
									  IDateFormat.toString(new Date(ms), false);
								}
								try { sleep(60000L); }
								catch(InterruptedException ex) {}
								routingSpecThread = startRoutingSpec();
							}
						}
						catch(InterruptedException ex)
						{
							cleanUpAndDie("RoutingSpec Thread Interrupted.");
						}
					}
				}
			};
		rsmon.start();

		
		//TODO 		release the lock
	}
    
	private void startThreads()
	{

		(xrWriteThread = new XRWriteThread(this)).start();

		//TODO ???
		initCompProc();

		//TODO ???
		// Put my consumer type into the consumer type enum.
		Database db = Database.getDb();
		decodes.db.DbEnum ctenum = db.getDbEnum(Constants.enum_DataConsumer);
		ctenum.replaceValue("DcpMonitorConsumer", 
			"Custom consumer for the DCP Monitor Application",
			"decodes.dcpmon1.DcpMonitorConsumer", null);
		
		//TODO ???
		// Instantiate and start RoutingSpecThread from it.
		// Build a routing spec from the configuration.
		Logger.instance().info("Making Real-Time Routing Spec.");
		try { makeRtRoutingSpec(); }
		catch(DatabaseException ex)
		{
			Logger.instance().log(Logger.E_FATAL,
				"DcpMonitor cannot construct routing spec: " + ex);
			return;
		}
		routingSpecThread = startRoutingSpec();

	}

	/**
     * Go into a dormant state.
     * Close the db connections and any DAOs that are using them.
     * Halt the background threads.
     */
    public void goDormant()
    {
    	if (xrWriteThread != null)
    	{
    		xrWriteThread.shutdown();
    		try { Thread.sleep(1000L); } catch (InterruptedException ex) {}
    	}
    	if (loadingAppDao != null)
    	{
    		loadingAppDao.close();
    		loadingAppDao = null;
    	}
		//TODO halt the routing spec thread 
    	
    	Database.getDb().getDbIo().close();
    	closeDb(); // This closes the connection and sets theDb to null.
		myLock = null;
		
		
    }
    
	public void handleDbIoException(String doingWhat, Throwable ex)
	{
		String msg = "Error " + doingWhat + ": " + ex;
		failure(msg);
		System.err.println("\n" + (new Date()) + " " + msg);
		ex.printStackTrace();
		goDormant();
	}


    
    /**
     * Reconnect both DECODES and TSDB databases. This is attempted after the db goes
     * down every 20 seconds.
     * @return true if success, false if failure.
     */
	private boolean attemptRestart()
	{
		try { initDecodes(); }
		catch (Exception ex)
		{
			failure("Cannot re-initialize Decodes Database connection " + ex.getMessage());
			return false;
		}

		try { createDatabase(); }
		catch(Throwable ex)
		{
			failure("Cannot create time series database: " + ex);
			Database.getDb().getDbIo().close();
			return false;
		}
		
		if (!tryConnect())
		{
			failure("Cannot connect to time series database.");
			theDb = null;
			Database.getDb().getDbIo().close();
			return false;
		}
		if (!loadConfig())
		{
			goDormant();
			return false;
		}
		startThreads();
		loadingAppDao = theDb.makeLoadingAppDAO();

		return true;
	}

    private void checkConfig()
	{
		Date cfgLMT = loadingAppDao.getLastModified(appId);
		if (cfgLMT == null)
			cfgLMT = new Date(System.currentTimeMillis() - 3600000L);
		if (cfgLMT.getTime() > dcpMonitorConfig.lastLoadTime)
			loadConfig();
	}


	/**
	 * Called after a successful connect to the database, or after detecting
	 * that the configuration has changed.
	 * @return true on success, false on failure
	 */
	private boolean loadConfig()
	{
		// Load the app info. The config is stored in the app's properties.
		try
		{
			appInfo = loadingAppDao.getComputationApp(appNameArg.getValue());
		}
		catch(DbIoException ex)
		{
			failure("Cannot read app info for '" + appNameArg.getValue() + ": " + ex);
			return false;
		}
		catch (NoSuchObjectException ex)
		{
			failure("Cannot read app info for '" + appNameArg.getValue() + ": " + ex 
				+ " -- will shutdown.");
			shutdownFlag = true;
			return false;
		}

		// Load the config object.
		dcpMonitorConfig.loadFromProperties(appInfo.getProperties());

		// Look for EventPort and EventPriority properties. If found,
		String evtPorts = appInfo.getProperty("EventPort");
		if (evtPorts != null)
		{
			try 
			{
				evtPort = Integer.parseInt(evtPorts.trim());
				// If we were formerly serving out event to a different port, shut it down.
				if (compEventSvr != null && compEventSvr.getPort() != evtPort)
				{
					compEventSvr.shutdown();
					compEventSvr = null;
				}
				if (evtPort > 0)
				{
					compEventSvr = new CompEventSvr(evtPort);
					compEventSvr.startup();
				}
			}
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("App Name " + appInfo.getAppName()
					+ ": Bad EventPort property '" + evtPorts
					+ "' must be integer. -- ignored.");
			}
			catch(IOException ex)
			{
				Logger.instance().failure(
					"Cannot create Event server: " + ex
					+ " -- no events available to external clients.");
			}
		}
		else if (compEventSvr != null)
		{
			compEventSvr.shutdown();
			compEventSvr = null;
		}
		dcpMonitorConfig.checkAndLoadNetworkLists();
		
		//Read Drgs Receiver file and store it in memory
		Logger.instance().info("Reading DRGS Receiver File");
		DrgsReceiverIo.readDrgsReceiverFile();

		return true;
	}
	
	/**
	 * Called when a database error has been detected. Throws away all database
	 * references & caches so that a fresh start can be made.
	 */
	private void disconnectFromDb()
	{
		theDb.closeConnection();
		Database.getDb().getDbIo().close();
		myLock = null;
	}

	
	/**
	 * Overloaded from template. Also init for decoding.
	 */
	public void initDecodes()
		throws DecodesException
	{
		super.initDecodes();
		DecodesInterface.initializeForDecoding();
		System.out.println("DECODES Database Initialization Done.");
	}

	/**
	 * Called periodically to check or get lock and set lock's status in db. 
	 * @return true on success, false if lock is being used by another app.
	 */
	private boolean checkLock()
		throws DbIoException
	{
		LoadingAppDAI loadingAppDAO = theDb.makeLoadingAppDAO();
		try
		{
			if (myLock == null)
				myLock = loadingAppDAO.obtainCompProcLock(appInfo, pid, hostname); 
			else
			{
				myLock.setStatus(getCurrentStatus());
				loadingAppDAO.checkCompProcLock(myLock);
			}
			return true;
		}
		catch (LockBusyException ex)
		{
			failure("Lock for app name '" + appNameArg.getValue() + "' is not available: "
				+ ex);
			return false;
		}
		finally
		{
			loadingAppDAO.close();
		}
	}

	private String getCurrentStatus()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void initCompProc()
	{
		if (!dcpMonitorConfig.enableComputations)
			setCompProcessor(null);
		else
		{
			ComputationProcessor cp = new ComputationProcessor();
			String cfgPath = EnvExpander.expand(dcpMonitorConfig.compConfig);
			try 
			{
				cp.init(cfgPath, null);
				setCompProcessor(cp);
			}
			catch(BadConfigException ex)
			{
				Logger.instance().warning("Cannot load computation config '"
					+ cfgPath + "': " + ex 
					+ " -- No computations will be performed.");
				dcpMonitorConfig.enableComputations = false;
			}
		}
	}

	/**
	  Starts the routing spec thread execution.
	  @return the RoutingSpecThread object.
	*/
	RoutingSpecThread startRoutingSpec()
	{
		String username = dcpMonitorConfig.ddsUserName;
		if (username != null)
		{
			Logger.instance().info("Data source username will be '" 
				+ username + "'");
			routingSpec.getProperties().setProperty("username", username);
		}
		routingSpecThread = new RoutingSpecThread(routingSpec);
		routingSpecThread.doRoutingSpecCheck = false;
		routingSpecThread.useThreadDbCon = true;
		routingSpecThread.setCloseDbOnQuit(false);
		routingSpecThread.setName("DcpMonRoutSpecThread");
		routingSpecThread.start();
		return routingSpecThread;
	}

	/**
	  Shuts down threads & exits.
	*/
	private void cleanUpAndDie(String reason)
	{
		Logger.instance().log(Logger.E_INFORMATION,
			"DCP Monitor Exiting: " + reason);
		Pdt.instance().stopMaintenanceThread();
		ChannelMap.instance().stopMaintenanceThread();
		try { Thread.sleep(5000L); }
		catch(InterruptedException ex) {}
		shutdownFlag = true;
		System.exit(0);
	}


	/**
	  Makes the real-time routing spec object for internal use.
	*/
	void makeRtRoutingSpec()
		throws DatabaseException
	{
		routingSpec = new RoutingSpec("DcpMonitor");

		routingSpec.outputFormat = "NullFormatter";
		routingSpec.outputTimeZoneAbbr = "UTC";

		//Get the latest time stamp on the database if there is one
		//If not start getting data for that last day - I'm not sure about this
		//it takes too long to get old data, why not start from today

//TODO Figure out what curday and earliestDay values should be.
//TODO Reconsider the two routing specs on start up. A real-time and a historical.
// In otherwords refactor the routing spec so I can have multiple instances of it.
int curday = 0;	
int earliestDay = 0;
//		int curday = DcpMonitorServerThread.getCurrentDay();
//		int earliestDay = curday - dcpMonitorConfig.numDaysStorage;
		
		Logger.instance().info("Current day=" + curday + ", earliestDay=" + earliestDay);
		long lastTimeStamp = System.currentTimeMillis() -
			(3600000L * 24L * dcpMonitorConfig.numDaysStorage);

		for (int day = earliestDay; day <= curday; day++)
		{
			Logger.instance().info("Getting latest time stamp for day " + day);
			Date latest = getLatestTimeStamp(day);
			if (latest != null && latest.getTime() > lastTimeStamp)
			{
				lastTimeStamp = latest.getTime();
			}
		}
		Logger.instance().info("For Real-Time Retrieval, lastTimeStamp = "
			+ new Date(lastTimeStamp));

		routingSpec.consumerType = "DcpMonitorConsumer";

		String dsName = dcpMonitorConfig.dataSourceName;
		routingSpec.dataSource = Database.getDb().dataSourceList.get(dsName);
		if (routingSpec.dataSource == null)
		{
			routingSpec.dataSource = new DataSource();
			routingSpec.dataSource.setName(dsName);
			routingSpec.dataSource.dataSourceType = "lrgs";
		}

		if (dcpMonitorConfig.lrgsTimeout == 0)
			routingSpec.getProperties().setProperty("lrgs.timeout", "600");
		else
			routingSpec.getProperties().setProperty("lrgs.timeout", "" + 
				dcpMonitorConfig.lrgsTimeout);

		String args = dcpMonitorConfig.lrgsDataSourceArg;
		if (args != null)
		{
			routingSpec.dataSource.dataSourceType = "lrgs";
			routingSpec.dataSource.dataSourceArg = args;
			Logger.instance().info("Explicit LRGS data source, args='" + args + "'");
		}
		else
		{
			try 
			{
				Logger.instance().info("Attempting to read data source '"
					+ routingSpec.dataSource.getDisplayName() + "'");
				routingSpec.dataSource.read();
			}
			catch(DatabaseException ex)
			{
				if (dsName.equals("localhost"))
				{
					Logger.instance().log(Logger.E_WARNING,
						"No datasource record for 'localhost' -- will attempt "
						+ "to construct one.");
					routingSpec.dataSource.dataSourceType = "lrgs";
					routingSpec.dataSource.dataSourceArg = "localhost";
				}
				else
				{
					Logger.instance().log(Logger.E_FATAL,
						"No data source '" + dsName + "': " + ex);
					throw ex;
				}
			}
		}
		Logger.instance().info("DataSource name="
			+routingSpec.dataSource.getName() + ", type=" + routingSpec.dataSource.dataSourceType);
		
		if (dcpMonitorConfig.allChannels == false)
		{
			// We only want to monitor channels that I have on my groups.
			// Add the 'channel' property to the routing spec. It will get
			// processed by LrgsDataSource when it builds the search-crit.

			DcpGroupList dgl = DcpGroupList.instance();
			myChannels = dgl.getAggregateChannelList();
			if (myChannels != null)
			{
				StringBuilder chanprop = new StringBuilder();
				for(int i=0; i<myChannels.length; i++)
				{
					if (i>0)
						chanprop.append(':');
					chanprop.append("" + myChannels[i]);
				}
				routingSpec.getProperties().setProperty("channel", 
					chanprop.toString());
			}
		}
		else
		{
			//Monitor all channels 1 to 266
			//Read all Channels and set myChannels[]
			//channels from https://dcs1.noaa.gov/chans_by_baud.txt
			myChannels = ChannelMap.instance().getChannelList();

			//If for some reason we can not get all channels from above link - 
			if (myChannels == null || myChannels.length == 0)
			{	//Try to at least get them from the groups, this works with 
				//pdt set to true (need pdt file)
				DcpGroupList dgl = DcpGroupList.instance();
				myChannels = dgl.getAggregateChannelList();
			}
		}

		// Build search critera to send to LRGS.
		SearchCriteria searchCrit = new SearchCriteria();
		String ss = IDateFormat.toString(new Date(lastTimeStamp), false);
		Logger.instance().info(module + " setting searchcrit since to '"
			+ ss + "'");
		searchCrit.setLrgsSince(ss);
		searchCrit.setRealtimeSettlingDelay(true);
		searchCrit.DapsStatus = SearchCriteria.NO;
		if (dcpMonitorConfig.allChannels == false && myChannels != null)
			for(int i=0; i<myChannels.length; i++)
				searchCrit.addChannelToken("" + myChannels[i]);
		File scFile = new File(
			EnvExpander.expand("$DECODES_INSTALL_DIR/dcpmon.sc"));
		try
        {
	        searchCrit.saveFile(scFile);
	        routingSpec.setProperty("searchcrit", scFile.getPath());
        }
        catch (IOException ex)
        {
	        Logger.instance().warning(module + 
	        	" Cannot write DCP mon searchcrit '" 
	        	+ scFile.getPath() + "': " + ex);
        }
	}

	/**
	  Called when netlist have changed.
	  We need to rebuild the rs & restart it so that changes in network
	  lists on the server take effect.
	*/
	void restartRoutingSpec()
	{
		routingSpecThread.forceReInit();
	}

	public synchronized void deleteDcpXmitsBefore(int dayNum)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			xmitRecordDao.deleteDcpXmitsBefore(dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":deleteDcpXmitsBefore(" + 
					dayNum + ")", ex);
		}
		finally
		{
			xmitRecordDao.close();
		}
	}

	public synchronized XmitRecord findDcpTranmission(DcpAddress dcpAddress, 
		Date timestamp, int dayNum)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.findDcpTranmission(dcpAddress, timestamp, dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":findDcpTranmission", ex);
			return null;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}

	public synchronized void saveDcpTranmission(XmitRecord xmitRecord)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			xmitRecordDao.saveDcpTranmission(xmitRecord);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":saveDcpTranmission", ex);
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	
	public synchronized int readXmitsByGroup(
			Collection<XmitRecord> results, int dayNum, DcpGroup grp)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.readXmitsByGroup(results, dayNum, grp);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByGroup", ex);
			return 0;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	
	public synchronized int readXmitsByChannel(
			Collection<XmitRecord> results, int dayNum, int chan)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.readXmitsByChannel(results, dayNum, chan);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByChannel", ex);
			return 0;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	
	//Used from DcpMonitorServerThread - sendWazzupDcp() method
	public synchronized int readXmitsByDcpAddress(
		Collection<XmitRecord> results, int dayNum, DcpAddress dcpAddress)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.readXmitsByDcpAddress(results, dayNum, dcpAddress);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByDcpAddress", ex);
			return 0;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	
	public synchronized XmitRecord readXmitRawMsg(int dayNum, 
		DcpAddress dcpAddress, Date timestamp)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.readXmitRawMsg(dayNum, dcpAddress, timestamp);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByDcpAddress", ex);
			return null;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	
	/**
	 * This method is used to get the higher date on the database, it is
	 * used at start up time so that we can start collecting messages
	 * since the last time the Dcp Monitor ran.
	 * 
	 * @param dayNum
	 * @return
	 */
	private synchronized Date getLatestTimeStamp(int dayNum)
	{
		if (theDb == null)
		{
			attemptRestart();
		}
		XmitRecordDAI xmitRecordDao = theDb.makeXmitRecordDao(31);
		try
		{
			return xmitRecordDao.getLatestTimeStamp(dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":getLatestTimeStamp", ex);
			return null;
		}
		finally
		{
			xmitRecordDao.close();
		}
	}
	

	public boolean isMyChannel(int chan)
	{
		if (dcpMonitorConfig.allChannels)
			return true;

		if (myChannels == null)
			return true;

		for(int i=0; i<myChannels.length; i++)
			if (myChannels[i] == chan)
				return true;
		return false;
	}

	public int[] getMyChannels()
	{
		return myChannels;
	}
	
	/**
     * @return the compProcessor
     */
    public ComputationProcessor getCompProcessor()
    {
    	return compProcessor;
    }

	/**
     * @param compProcessor the compProcessor to set
     */
    public void setCompProcessor(ComputationProcessor compProcessor)
    {
    	this.compProcessor = compProcessor;
    }
    
    public void info(String msg)
    {
    	Logger.instance().info("DCPMON " + msg);
    }
    public void warning(String msg)
    {
    	Logger.instance().warning("DCPMON " + msg);
    }
    public void failure(String msg)
    {
    	Logger.instance().failure("DCPMON " + msg);
    }
    public void debug(String msg)
    {
    	Logger.instance().debug1("DCPMON " + msg);
    }

}
