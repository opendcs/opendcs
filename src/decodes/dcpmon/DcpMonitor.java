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
import decodes.tsdb.TsdbAppTemplate;
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
	RoutingSpecThread routingSpecThread;

	/** We mock-up a dynamic routing spec for this application */
	private RoutingSpec routingSpec;

	/** The server lock ensures only one instance runs at a time. */
	ServerLock mylock;

	/** Tells the application to die. */
	private boolean shutdownFlag = false;

	private DcpMonitorConfig dcpMonitorConfig;

	private DcpNameDescResolver dcpNameDescResolver = null;
	
//	private XmitRecord lastRec = null;

	// Start time for the real-time routing spec.
//	private long rtStartMsec = 0L;

	// Handles in-line computations for HTML DCP Message displays
	private ComputationProcessor compProcessor = null;


	// Singleton Instance
	private static DcpMonitor _instance = null;

	// list of channels we are accepting data from
	private int myChannels[];
	
	/** Holds app name, id, & description. */
	private CompAppInfo appInfo = null;
	private int evtPort = -1;
	private CompEventSvr compEventSvr = null;

	
	private XmitRecordDAI xmitRecordDao = null;

	/** Singleton Access Method. */
	public static DcpMonitor instance()
	{
		if (_instance == null)
			_instance = new DcpMonitor();
		return _instance;
	}

	/** Constructor. */
	private DcpMonitor()
	{
		super("dcpmon");
		routingSpecThread = null;
		routingSpec = null;
		myChannels = null;
	}

	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
	}

	/**
	 * Called after a successful connect to the database, or after detecting
	 * that the configuration has changed.
	 */
	private void loadConfig()
	{
		// Load the app info. The config is stored in the app's properties.
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(appNameArg.getValue());
		}
		catch(Exception ex)
		{
			warning("Cannot read app info for '" + appNameArg.getValue() + ": " + ex);
			disconnectFromDb();
			return;
		}
		finally
		{
			loadingAppDao.close();
		}

		// Load the config object.
		dcpMonitorConfig = DcpMonitorConfig.instance();
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
	}
	
	/**
	 * Called when a database error has been detected. Throws away all database
	 * references & caches so that a fresh start can be made.
	 */
	private void disconnectFromDb()
	{
		//TODO disconnect from db and set references to null
	}


	/**
	 * Specific initialization for DCP Monitor Configuration.
	 */
	public void initDcpMonConfig()
	{
		dcpNameDescResolver = new DcpNameDescResolver();
		dcpMonitorConfig.checkAndLoadNetworkLists();
		initCompProc();
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
	 * Overloaded from template. Also init for decoding.
	 */
	public void initDecodes()
		throws DecodesException
	{
		super.initDecodes();
		DecodesInterface.initializeForDecoding();
		
		HtmlFormatter.metaDataCgi = true;

		// For backward compatibility with folks who may not have updated
		// their enumeration list, Force the HtmlFormatter enum entry.
		DbEnum formatEnum = Database.getDb().getDbEnum("OutputFormat");

		if (formatEnum.findEnumValue("HtmlFormatter") == null)
		{
			formatEnum.replaceValue("HtmlFormatter", "HTML-Formatter",
				"decodes.consumer.HtmlFormatter", null);
		}
		
		//In case we don't have the Null Formatter- add it
		if (formatEnum.findEnumValue("NullFormatter") == null)
		{
			formatEnum.replaceValue("NullFormatter", "Null-Formatter",
				"decodes.consumer.NullFormatter", null);
		}
		System.out.println("Database Initialization Done.");
	}

	/**
	* The main method does all the one-time initialization of the
	* DECODES database and dcpmon configuration. Then it instantiates
	* a DcpMonitor object to do the real work.
	*
	* If you start dcpmon as a non-main thread, you must ensure that
	* this one-time init is done elsewhere.
	* @param args command line arguments
	*/
    public static void main(String args[]) throws Exception
    {
		// Start the dcpmon object in the current thread.
		DcpMonitor dcpmon = DcpMonitor.instance();		
		dcpmon.execute(args);
	}
	
	/**
	  Called from main method to execute the main thread.
	  This is implemented as a Runnable.run() method in case you want to run
	  dcp monitor as part of a large application.
	*/
	public void runApp()
	{
		info("DCP Monitor Starting =======================");
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		// Get the server lock, & fail if error.
		
		
		//TODO: Copy code from ComputationApp to get the database lock.
		//Note: get the lock as part of the reconnectDatabase() method.
		//See ComputationApp, which, I believe, can also reconnect if DB goes down.
//		String lockpath = EnvExpander.expand(lockFileArg.getValue());
//		mylock = new ServerLock(lockpath);
//
//		if (mylock.obtainLock() == false)
//		{
//			Logger.instance().log(Logger.E_FATAL,
//				"DCP Monitor not started: lock file busy");
//			System.exit(0);
//		}
		
		//TODO: Start a thread that periodically updates my status in the database.
		
		

		this.initDcpMonConfig();

		if (theDb != null && !theDb.isConnected())
		{
			// DcpMonitor will not start until the DB connection is established
			while (super.tryConnect() == false)
			{
				Logger.instance().warning("DcpMonitor will try " +
						"to connect to Database in 20 seconds");
				try { Thread.sleep(20000L); }
				catch(InterruptedException ex) {}
			}
			try
			{
				loadConfig();
				initDecodes();
			}
			catch (Exception ex)
			{
				Logger.instance().warning("DcpMonitor cannot initialize" +
						"Decodes DB " + ex.getMessage());
				ex.printStackTrace();
			}
		}

		XRWriteThread.instance().start();

		mylock.releaseOnExit();
		Runtime.getRuntime().addShutdownHook(
			new Thread()
			{
				public void run()
				{
					Logger.instance().log(Logger.E_INFORMATION,
						"DCP Monitor Cleaning Up ...");

					// Stop the DB write thread.
					XRWriteThread.instance().shutdown();
					XRWriteThread.instance().processQueue();

					Logger.instance().info(
						"DCP Monitor Server exiting " +
						(mylock.wasShutdownViaLock() ? "(lock file removed)"
						: ""));
				}
			});

		// Initialize the PDT and Channel Map. */
		Logger.instance().info("Initializing PDT");
		Pdt pdt = Pdt.instance();
		pdt.startMaintenanceThread(dcpMonitorConfig.pdtUrl, dcpMonitorConfig.pdtLocalFile);
		
		Logger.instance().info("Initializing Channel Map");
		ChannelMap cmap = ChannelMap.instance();
		cmap.startMaintenanceThread(dcpMonitorConfig.channelMapUrl, 
			dcpMonitorConfig.channelMapLocalFile);

		//If hadsUse - this is to download the National Weather Service
		//file and use it to fill out the dcp names in case no names
		//are found according to the name rules - check the DcpMonitorServer
		//Thread class - setDcpName method
		Logger.instance().info("Config.hadsUse=" + dcpMonitorConfig.hadsUse);
		if (dcpMonitorConfig.hadsUse)
		{
			Logger.instance().info("Initializing HADS file");
			Hads hads = Hads.instance();
			hads.startMaintenanceThread(dcpMonitorConfig.hadsUrl, dcpMonitorConfig.hadsLocalFile);
		}
		
		//Read Drgs Receiver file and store it in memory
		Logger.instance().info("Reading DRGS Receiver File");
		DrgsReceiverIo.readDrgsReceiverFile();

		// Build a routing spec from the configuration.
		Logger.instance().info("Making Real-Time Routing Spec.");
		try { makeRtRoutingSpec(); }
		catch(DatabaseException ex)
		{
			Logger.instance().log(Logger.E_FATAL,
				"DcpMonitor cannot construct routing spec: " + ex);
			return;
		}

/*
/*		// Build a routing spec to handle history back to last run.
/*		try { makeHistRoutingSpec(); }
/*		catch(DatabaseException ex)
/*		{
/*			Logger.instance().log(Logger.E_FATAL,
/*				"DcpMonitor cannot construct history routing spec: " + ex);
/*			return;
/*		}
*/

		// Put my consumer type into the consumer type enum.
		Database db = Database.getDb();
		decodes.db.DbEnum ctenum = db.getDbEnum(Constants.enum_DataConsumer);
		ctenum.replaceValue("DcpMonitorConsumer", 
			"Custom consumer for the DCP Monitor Application",
			"decodes.dcpmon1.DcpMonitorConsumer", null);
		
		// Instantiate and start RoutingSpecThread from it.
		routingSpecThread = startRoutingSpec();

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

		// Instantiate & start the socket server.
		try 
		{
			DcpMonitorServer dms = new DcpMonitorServer(); 
			dms.listen();
		}
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FATAL,
				"Cannot start socket server for DcpMonitor: " + ex);
			return;
		}
		cleanUpAndDie("Server listening socket failed.");
	}


	/**
	  Starts the routing spec thread execution.
	  @return the RoutingSpecThread object.
	*/
	RoutingSpecThread startRoutingSpec()
	{
		String username = DcpMonitorConfig.instance().ddsUserName;
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
		decodes.db.Database db = decodes.db.Database.getDb();

		routingSpec.outputFormat = "NullFormatter";
		routingSpec.outputTimeZoneAbbr = "UTC";

		//Get the latest time stamp on the database if there is one
		//If not start getting data for that last day - I'm not sure about this
		//it takes too long to get old data, why not start from today
		int curday = DcpMonitorServerThread.getCurrentDay();
		int earliestDay = curday - DcpMonitorConfig.instance().numDaysStorage;
		
		Logger.instance().info("Current day=" + curday + ", earliestDay=" + earliestDay);
		long lastTimeStamp = System.currentTimeMillis() -
			(3600000L * 24L * DcpMonitorConfig.instance().numDaysStorage);

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

		String dsName = DcpMonitorConfig.instance().dataSourceName;
		routingSpec.dataSource = db.dataSourceList.get(dsName);
		if (routingSpec.dataSource == null)
		{
			routingSpec.dataSource = new DataSource();
			routingSpec.dataSource.setName(dsName);
			routingSpec.dataSource.dataSourceType = "lrgs";
		}

		DcpMonitorConfig cfg = DcpMonitorConfig.instance();
		if (cfg.lrgsTimeout == 0)
			routingSpec.getProperties().setProperty("lrgs.timeout", "600");
		else
			routingSpec.getProperties().setProperty("lrgs.timeout", "" + 
				cfg.lrgsTimeout);

		String args = DcpMonitorConfig.instance().lrgsDataSourceArg;
		if (args != null)
		{
			routingSpec.dataSource.dataSourceType = "lrgs";
			routingSpec.dataSource.dataSourceArg = args;
			Logger.instance().info("Explicite LRGS data source, args='" + args + "'");
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
			reconnectDatabase();
		}
		try
		{
			xmitRecordDao.deleteDcpXmitsBefore(dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":deleteDcpXmitsBefore(" + 
					dayNum + ")", ex);
		}
	}

	public synchronized XmitRecord findDcpTranmission(DcpAddress dcpAddress, 
		Date timestamp, int dayNum)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.findDcpTranmission(dcpAddress, timestamp, dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":findDcpTranmission", ex);
			return null;
		}
	}

	public synchronized void saveDcpTranmission(XmitRecord xmitRecord)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			xmitRecordDao.saveDcpTranmission(xmitRecord);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":saveDcpTranmission", ex);
		}
	}
	
	public synchronized int readXmitsByGroup(
			Collection<XmitRecord> results, int dayNum, DcpGroup grp)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.readXmitsByGroup(results, dayNum, grp);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByGroup", ex);
			return 0;
		}
	}
	
	public synchronized int readXmitsByChannel(
			Collection<XmitRecord> results, int dayNum, int chan)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.readXmitsByChannel(results, dayNum, chan);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByChannel", ex);
			return 0;
		}
	}
	
	//Used from DcpMonitorServerThread - sendWazzupDcp() method
	public synchronized int readXmitsByDcpAddress(
		Collection<XmitRecord> results, int dayNum, DcpAddress dcpAddress)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.readXmitsByDcpAddress(results, dayNum, dcpAddress);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByDcpAddress", ex);
			return 0;
		}
	}
	
	public synchronized XmitRecord readXmitRawMsg(int dayNum, 
		DcpAddress dcpAddress, Date timestamp)
	{
		if (theDb == null)
		{
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.readXmitRawMsg(dayNum, dcpAddress, timestamp);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":readXmitsByDcpAddress", ex);
			return null;
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
			reconnectDatabase();
		}
		try
		{
			return xmitRecordDao.getLatestTimeStamp(dayNum);
		}
		catch(DbIoException ex)
		{
			handleDbIoException(module + ":getLatestTimeStamp", ex);
			return null;
		}
	}
	
	private void handleDbIoException(String doingWhat, Throwable ex)
	{
		String msg = "Error " + doingWhat + ": " + ex;
		Logger.instance().failure(msg);
		System.err.println("" + (new Date()) + " " + msg);
		ex.printStackTrace();
		Logger.instance().failure("Closing Database connection.");
		try { theDb.closeConnection(); } catch(Throwable ex2) {}
		theDb = null;
	}

	private void reconnectDatabase()
	{	//Do init decode first like it is done when the Dcp Monitor
		//starts up.
		reInitDecodesDb();
		
		try { createDatabase(); }
		catch(Throwable ex)
		{
			Logger.instance().failure(module + 
					":reconnectDatabase Error connecting to database: " + ex);
		}
		if (!tryConnect())
		{
			Logger.instance().failure(module + 
				":reconnectDatabase Cannot connect to database.");
			return;
		}
		if (xmitRecordDao != null)
			xmitRecordDao.close();
		xmitRecordDao = theDb.makeXmitRecordDao(31);
	}

	/**
	 * This method is called to re-connect to the Decodes Database after
	 * we have lost the DB connection. Gets the current Database object and
	 * set it with a new dbio object that will contain the new establish
	 * DB connection.
	 */
	private void reInitDecodesDb()
	{
		try
		{
			Database dbOld = Database.getDb();
			if (dbOld != null)
				if (dbOld.getDbIo() != null)
					dbOld.getDbIo().close();
			DatabaseIO dbio;
//			String dbloc = dbLocArg.getValue();
//			DecodesSettings settings = DecodesSettings.instance();
//			if (dbloc.length() > 0)
//			{
//				dbio = DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XML, dbloc);
//			}
//			else
//			{
//				dbio = 
//				DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
//					settings.editDatabaseLocation);
//			}
//			Database currentDb = Database.getDb();
//			currentDb.setDbIo(dbio);
		} catch (Exception ex)
		{
			Logger.instance().warning("DcpMonitor cannot re-initialize" +
					"Decodes Database connection " + ex.getMessage());
			ex.printStackTrace();
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
	
	public DcpNameDescResolver getDcpNameDescResolver()
	{
		return dcpNameDescResolver;
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
    public void debug(String msg)
    {
    	Logger.instance().debug1("DCPMON " + msg);
    }

}
