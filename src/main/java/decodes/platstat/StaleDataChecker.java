package decodes.platstat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.PlatformStatusDAI;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.PlatformList;
import decodes.db.PlatformStatus;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.CompEventSvr;
import decodes.tsdb.DbIoException;
import decodes.tsdb.LockBusyException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.tsdb.TsdbCompLock;
import decodes.util.CmdLineArgs;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

public class StaleDataChecker
	extends TsdbAppTemplate
	implements PropertiesOwner
{
	private static String module = "StaleDataChecker";
	private CompAppInfo appInfo = null;
	private TsdbCompLock myLock = null;

	private boolean shutdown = false;
	private int checkPeriodMinutes = 1;
	private int maxAgeHours = 24;
	private String triggerFile1 = null;
	private String triggerFile2 = null;
	private String fileContents = null;
	private ArrayList<String> netlistNames = new ArrayList<String>();
	private ArrayList<NetworkList> loadedLists = new ArrayList<NetworkList>();
	private SimpleDateFormat sdf = new SimpleDateFormat("ddMMM HH:mm");
	private CompEventSvr compEventSvr = null;

	private PropertySpec propSpecs[] = 
	{
		new PropertySpec("StaleDataMaxAgeHours", PropertySpec.NUMBER, 
			"Stations not reporting in more than this # of hours are considered stale."),
		new PropertySpec("StaleDataTriggerFile1", PropertySpec.STRING, 
			"File name of first PageGate trigger file"),
		new PropertySpec("StaleDataTriggerFile2", PropertySpec.STRING, 
			"File name of second PageGate trigger file"),
		new PropertySpec("StaleDataFileContents", PropertySpec.STRING, 
			"Template for file contents"),
		new PropertySpec("StaleDataFileTZ", PropertySpec.TIMEZONE, 
			"Timezone for date/times in trigger files.")
	};

	private StringToken netlistArg = new StringToken("n", "Network List Name", "", 
		TokenOptions.optSwitch|TokenOptions.optMultiple, "");

	public StaleDataChecker()
	{
		super(module);
		setSilent(true);
	}
	
	@Override
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(netlistArg);
		appNameArg.setDefaultValue("StaleDataChecker");
	}


	private void init()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			appInfo = loadingAppDao.getComputationApp(getAppId());
			
			// If this process can be monitored, start an Event Server.
			if (TextUtil.str2boolean(appInfo.getProperty("monitor")) && compEventSvr == null)
			{
				try 
				{
					compEventSvr = new CompEventSvr(determineEventPort(appInfo));
					compEventSvr.startup();
				}
				catch(IOException ex)
				{
					failure("Cannot create Event server: " + ex
						+ " -- no events available to external clients.");
				}
			}
			
			String hostname = "unknown";
			try { hostname = InetAddress.getLocalHost().getHostName(); }
			catch(Exception e) { hostname = "unknown"; }

			myLock = loadingAppDao.obtainCompProcLock(appInfo, getPID(), hostname); 

		}
		catch (LockBusyException ex)
		{
			warning("Cannot run: lock busy: " + ex);
			shutdown = true;
			return;
		}
		catch (DbIoException ex)
		{
			warning("Database I/O Error: " + ex);
			shutdown = true;
			return;
		}
		catch (NoSuchObjectException ex)
		{
			warning("Cannot run: No such app name '" + appNameArg.getValue() + "': " + ex);
			shutdown = true;
			return;
		}
		finally
		{
			loadingAppDao.close();
		}
		
		String s = appInfo.getProperty("StaleDataCheckMinutes");
		if (s != null && s.length() > 0)
		{
			try { checkPeriodMinutes = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				warning(" Invalid StaleDataCheckPeriodMinutes"
					+ " property '" + s + "' -- ignored.");
				checkPeriodMinutes = 1;
			}
		}
		s = appInfo.getProperty("StaleDataMaxAgeHours");
		if (s != null)
		{
			try { maxAgeHours = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning(module + " Invalid StaleDataMaxAgeHours"
					+ " property '" + s + "' -- ignored.");
				maxAgeHours = 24;
			}
		}
		
		triggerFile1 = appInfo.getProperty("StaleDataTriggerFile1");
		triggerFile2 = appInfo.getProperty("StaleDataTriggerFile2");
		fileContents = appInfo.getProperty("StaleDataFileContents");
		if (fileContents == null)
		{
			fileContents = "DACQ-Alert: Data not current MST=${LASTMSGTIME} ${SITENAME}";
		}
		
		if (netlistArg.NumberOfValues() == 0
		 || (netlistArg.NumberOfValues() == 1 && netlistArg.getValue().length() == 0))
		{
			netlistNames.add("<Production>");
		}
		else for(int idx = 0; idx < netlistArg.NumberOfValues(); idx++)
		{
			netlistNames.add(netlistArg.getValue(idx));
		}
		
		String tzid = appInfo.getProperty("StaleDataFileTZ");
		if (tzid == null)
			tzid = "MST";
		sdf.setTimeZone(TimeZone.getTimeZone(tzid));
	}
	
	private void doCheck()
	{
		Logger.instance().debug2(module + " checking platform statuses...");
		PlatformStatusDAI platformStatusDAO = Database.getDb().getDbIo().makePlatformStatusDAO();
		
		try
		{
			ArrayList<PlatformStatus> statusList = platformStatusDAO.listPlatformStatus();
			for(PlatformStatus platstat : statusList)
			{
				DbKey platId = platstat.getPlatformId();
				Platform plat = Database.getDb().platformList.getById(platId);
				if (plat == null)
				{
					plat = new Platform(platId);
					try
					{
						plat.read();
					}
					catch (DatabaseException ex)
					{
						warning("Platform status with platform id=" + platId 
							+ ", but no matching platform: "+ ex);
						continue;
					}
				}
				boolean isInList = false;
				for(NetworkList nl : loadedLists)
					if (nl.contains(plat))
					{
						isInList = true;
						break;
					}
				int mah = maxAgeHours;
				String s = plat.getProperty("StaleDataMaxAgeHours");
				if (s != null)
					try { mah = Integer.parseInt(s); }
					catch(NumberFormatException ex)
					{
						warning("Platform '" + plat.getDisplayName() + "' bad StaleDataMaxAgeHours property '"
							+ s + "' -- should be integer.");
						mah = maxAgeHours;
					}
				if (isInList)
				{
					Date lastMsgTime = platstat.getLastMessageTime();
					if (lastMsgTime == null || 
						(System.currentTimeMillis() - lastMsgTime.getTime()) > mah * 3600000L)
					{
						assertStale(plat, platstat);
					}
				}
			}
		}
		catch (DbIoException ex)
		{
			warning("Database I/O error in doCheck: " + ex);
		}
		finally
		{
			platformStatusDAO.close();
		}
	}
	
	private void assertStale(Platform plat, PlatformStatus platstat)
	{
		// Make sure a 'stale' assertion isn't already in place.
		String annot = platstat.getAnnotation();
		if (annot == null)
			annot = "";
		if (annot.toLowerCase().contains("stale"))
			return;
		
		// Also don't overwrite other errors with a stale assertion. Only assert
		// if it is not currently in an error state.
		Date lastError = platstat.getLastErrorTime();
		Date lastMsg = platstat.getLastMessageTime();
		if (lastError != null
		 && (lastMsg == null || lastError.after(lastMsg)))
			return;
		
		String msg = "Stale: No data in more than " + maxAgeHours + " hours.";
		platstat.setAnnotation(msg);
		platstat.setLastErrorTime(new Date());
		PlatformStatusDAI platformStatusDAO = theDb.makePlatformStatusDAO();
		DacqEventDAI eventDAO = theDb.makeDacqEventDAO();
		try
		{
			platformStatusDAO.writePlatformStatus(platstat);
			DacqEvent evt = new DacqEvent();
			evt.setAppId(getAppId());
			evt.setEventPriority(Logger.E_FAILURE);
			evt.setEventText(msg);
			evt.setEventTime(new Date());
			evt.setPlatformId(plat.getId());
			eventDAO.writeEvent(evt);
		}
		catch (DbIoException ex)
		{
			warning("Cannot save Platform Status entry: " + ex);
		}
		finally
		{
			eventDAO.close();
			platformStatusDAO.close();
		}

		Properties props = new Properties(System.getProperties());
		props.setProperty("SITENAME", plat.getSiteName(false));
		props.setProperty("PLATFORMNAME", plat.getDisplayName());
		props.setProperty("LASTMSGTIME", 
			platstat.getLastMessageTime() == null ? "Never" : sdf.format(platstat.getLastMessageTime()));
		
		if (triggerFile1 != null && triggerFile1.length() > 0)
		{
			File f = new File(EnvExpander.expand(triggerFile1, props));
			try
			{
				PrintWriter pw = new PrintWriter(f);
				pw.println(EnvExpander.expand(fileContents, props));
				pw.close();
			}
			catch (FileNotFoundException ex)
			{
				warning("Cannot write to trigger file 1 '" + f.getPath() + "': " + ex);
			}
		}
		if (triggerFile2 != null && triggerFile2.length() > 0)
		{
			File f = new File(EnvExpander.expand(triggerFile2, props));
			try
			{
				PrintWriter pw = new PrintWriter(f);
				pw.println(EnvExpander.expand(fileContents, props));
				pw.close();
			}
			catch (FileNotFoundException ex)
			{
				warning("Cannot write to trigger file 2 '" + f.getPath() + "': " + ex);
			}
		}
	}

	@Override
	protected void runApp() throws Exception
	{
		shutdown = false;
		init();
		
		// Set lastCheck to cause first check 5 seconds after startup.
		long lastCheck = System.currentTimeMillis() - (checkPeriodMinutes*60000L) + 5000L;
		long lastLockCheck = System.currentTimeMillis();
		while(!shutdown)
		{
			if (System.currentTimeMillis() - lastCheck >= checkPeriodMinutes*60000L)
			{
				nlCheck();
				doCheck();
				lastCheck = System.currentTimeMillis();
			}
			if (System.currentTimeMillis() - lastLockCheck >= 10000L)
			{
				if (!lockCheck())
					shutdown = true;
				lastLockCheck = System.currentTimeMillis();
			}
			try { Thread.sleep(1000L); }
			catch(InterruptedException ex) {}
		}
		cleanup();
	}
	
	private boolean lockCheck()
	{
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			loadingAppDao.checkCompProcLock(myLock);
			return true;
		}
		catch(Exception ex)
		{
			info("Exiting because lock deleted: " + ex);
			return false;
		}
		finally
		{
			loadingAppDao.close();
		}

	}

	private void cleanup()
	{
		if (myLock == null)
			return;
		LoadingAppDAI loadingAppDao = theDb.makeLoadingAppDAO();
		try
		{
			loadingAppDao.releaseCompProcLock(myLock);
		}
		catch (DbIoException ex)
		{
			warning("Error attempting to release lock: " + ex);
		}
		finally
		{
			loadingAppDao.close();
		}

	}
	
	/**
	 * Check network lists for any changes. Reload if necessary.
	 */
	private void nlCheck()
	{
		Logger.instance().debug3(module + ".nlCheck Checking " + netlistNames.size() + " lists.");
		// Check all the lists & reload if necessary
		for(String nlName : netlistNames)
		{
			boolean alreadyLoaded = false;
			for(NetworkList nl : loadedLists)
			{
				if (nl.name.equalsIgnoreCase(nlName))
				{
					alreadyLoaded = true;
					try
					{
						Date dbLMT = nl.getDatabase().getDbIo().getNetworkListLMT(nl);
						if (dbLMT.getTime() > nl.getTimeLastRead())
						{
							nl.clear();
							nl.read();
						}
					}
					catch (DatabaseException ex)
					{
						warning("Cannot read network list '" + nlName + "': " + ex);
					}
				}
			}
			if (!alreadyLoaded)
			{
				NetworkList nl = Database.getDb().networkListList.getNetworkList(nlName);
				if (nl == null)
				{
					warning("No such network list '" + nlName + "'");
					continue;
				}
				loadedLists.add(nl);
			}
		}
		
		// Now make sure there is a platform status structure for every platform in my list.
		PlatformStatusDAI platformStatusDAO = Database.getDb().getDbIo().makePlatformStatusDAO();
		
		try
		{
			PlatformList platlist = Database.getDb().platformList;
			ArrayList<PlatformStatus> statusList = platformStatusDAO.listPlatformStatus();
			for(NetworkList nl : loadedLists)
			{
				for(Iterator<NetworkListEntry> nleit = nl.iterator(); nleit.hasNext(); )
				{
					NetworkListEntry nle = nleit.next();
					Logger.instance().debug3(module + ".nlCheck checking list " 
						+ nl.getDisplayName() + " entry " + nle.getTransportId());
					Platform p = platlist.getPlatform(nl.transportMediumType, nle.getTransportId());
					Logger.instance().debug3(module + ".nlCheck checking list " 
						+ nl.getDisplayName() + " entry " + nle.getTransportId() 
						+ " platform is " + (p != null ? "not" : "") + " null");
					if (p != null)
					{
						boolean hasPlatStat = false;
						for(PlatformStatus platstat : statusList)
						{
							if (platstat.getPlatformId().equals(p.getId()))
							{
								hasPlatStat = true;
								break;
							}
						}
						if (!hasPlatStat)
						{
							PlatformStatus platstat = new PlatformStatus(p.getId());
							Logger.instance().info("Writing empty PlatformStats for platform " 
								+ p.getDisplayName());
							platformStatusDAO.writePlatformStatus(platstat);
						}
					}
					// else okay -- netlists are allowed to have TMs for which there is no platform.
				}
			}
		}
		catch (DbIoException ex)
		{
			warning("Database I/O error in nlCheck: " + ex);
		}
		catch (DatabaseException ex)
		{
			warning("DECODES Database I/O error in nlCheck: " + ex);
		}
		finally
		{
			platformStatusDAO.close();
		}

	}
	
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.instance().warning(module + " " + msg);
	}
	
	/**
	 * The main method.
	 * @param args command line arguments.
	 */
	public static void main( String[] args )
		throws Exception
	{
		TsdbAppTemplate theApp = new StaleDataChecker();
		theApp.execute(args);
	}

	@Override
	public PropertySpec[] getSupportedProps()
	{
		return propSpecs;
	}

	@Override
	public boolean additionalPropsAllowed()
	{
		return true;
	}

}
