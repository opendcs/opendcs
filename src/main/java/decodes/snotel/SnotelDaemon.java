package decodes.snotel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import decodes.util.CmdLineArgs;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.EnvExpander;
import ilex.util.IDateFormat;
import ilex.util.PropertiesUtil;
import ilex.util.ServerLock;
import ilex.util.Logger;

/**
 * This is the main class for the Snotel retriever/formatter.
 * 
 * @author mmaloney
 *
 */
public class SnotelDaemon
	implements Runnable
{
	public static final String snotelConfigFile = "$DCSTOOL_USERDIR/snotel.conf";
	private SnotelConfig snotelConfig = new SnotelConfig();
	private long lastConfigLoad = 0L;
	
	public static final String snotelStatusFile = "$DCSTOOL_USERDIR/snotel.stat";
	private SnotelStatus snotelStatus = new SnotelStatus();
	
	public static final String snotelRtStationListFile = "$DCSTOOL_USERDIR/snotel-platforms.csv";
	
	private ControlmMonitor controlmMonitor = null;
	
	// Static command line arguments and initialization for main method.
	protected CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "snoteldaemon.log");
	static StringToken lockFileArg = new StringToken("k", 
		"Optional Lock File", "", TokenOptions.optSwitch, "");

	private RetrievalThread rtRetrieval = null, hstRetrieval = null;
	long historyStarted = 0L;
	int rtSequence = 1;

	

	public SnotelDaemon(String[] args)
	{
		cmdLineArgs.addToken(lockFileArg);
		// Parse command line arguments.
		try { cmdLineArgs.parseArgs(args); }
		catch(IllegalArgumentException ex)
		{
			System.exit(1);
		}
	}

	public static void main(String[] args)
	{
		SnotelDaemon me = new SnotelDaemon(args);
		me.run();
	}

	@Override
	public void run()
	{
		/** Optional server lock ensures only one instance runs at a time. */
		String lockpath = lockFileArg.getValue();
		if (lockpath != null && lockpath.trim().length() > 0)
		{
			lockpath = EnvExpander.expand(lockpath.trim());
			final ServerLock mylock = new ServerLock(lockpath);

			if (mylock.obtainLock() == false)
			{
				Logger.instance().failure("Routing Spec not started: lock file busy: " + lockpath);
				System.exit(1);
			}
			
			mylock.releaseOnExit();
			Runtime.getRuntime().addShutdownHook(
				new Thread()
				{
					public void run()
					{
						shutdown();
					}
				});
		}

		loadStatus();
		checkConfig();
		
		// Instantiate monitor AFTER reading config, which contains the directory names.
		controlmMonitor = new ControlmMonitor(this);
		
		// Run the monitor in the main thread.
		controlmMonitor.run();
	}
	
	public synchronized void shutdown()
	{
		if (controlmMonitor != null)
			controlmMonitor.shutdown();
		
		// if any retrievals are in process, stop them.
		if (historyInProgress())
		{
			hstRetrieval.shutdown();
			hstRetrieval = null;
		}
		if (realtimeInProgress())
		{
			rtRetrieval.shutdown();
			rtRetrieval = null;
		}
	}
	
	// Called at startup and after each directory scan.
	public void checkConfig()
	{
		File f = new File(EnvExpander.expand(snotelConfigFile));
		if (f.lastModified() > lastConfigLoad)
		{
			lastConfigLoad = System.currentTimeMillis();
			Properties p = new Properties();
			FileReader fr = null;
			try
			{
				fr = new FileReader(f);
				p.load(fr);
				PropertiesUtil.loadFromProps(snotelConfig, p);
			}
			catch(Exception ex)
			{
				Logger.instance().warning("Cannot load config from '" + f.getPath() + "': " + ex);
			}
			finally
			{
				if (fr != null)
					try { fr.close(); } catch(Exception ex) {}
			}
		}
	}
	
	public void saveConfig()
	{
		Properties p = new Properties();
		PropertiesUtil.storeInProps(snotelConfig, p, "");
		FileWriter fw = null;
		File f = new File(EnvExpander.expand(snotelConfigFile));
		try
		{
			fw = new FileWriter(f);
			p.store(fw, "Snotel Config as of " + new Date());
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot store config to '" + f.getPath() + "': " + ex);
		}
		finally
		{
			if (fw != null)
				try { fw.close(); } catch(Exception ex) {}
		}	
	}

	
	public SnotelConfig getConfig() { return snotelConfig; }
	
	public SnotelStatus getStatus() { return snotelStatus; }
	
	public void saveStatus()
	{
		Properties p = new Properties();
		PropertiesUtil.storeInProps(snotelStatus, p, "");
		FileWriter fw = null;
		File f = new File(EnvExpander.expand(snotelStatusFile));
		try
		{
			fw = new FileWriter(f);
			p.store(fw, "Snotel Status as of " + new Date());
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot store status to '" + f.getPath() + "': " + ex);
		}
		finally
		{
			if (fw != null)
				try { fw.close(); } catch(Exception ex) {}
		}	
	}
	
	private void loadStatus()
	{
		File f = new File(EnvExpander.expand(snotelStatusFile));
		Properties p = new Properties();
		FileReader fr = null;
		try
		{
			fr = new FileReader(f);
			p.load(fr);
			PropertiesUtil.loadFromProps(snotelStatus, p);
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot load status from '" + f.getPath() + "': " + ex);
		}
		finally
		{
			if (fr != null)
				try { fr.close(); } catch(Exception ex) {}
		}
Logger.instance().info("Status file loaded, configLMT=" + snotelStatus.configLMT);
	}
	
	/**
	 * Called from ControlmMonitor thread when directory scan is complete.
	 * Check to see if it's time to run realtime retrieval.
	 */
	public synchronized void scanFinished()
	{
		checkConfig();
		
		long curHour = System.currentTimeMillis() / 3600000L;
		long lastRunHour = snotelStatus.lastRealtimeRun / 3600000L;
Logger.instance().debug1("scanFinished curHour=" + curHour + ", lastRunHour=" + lastRunHour);
		if (curHour > lastRunHour)
			runRealtime();
		
		String msg = "scanFinished()";
		if (historyInProgress())
			msg = msg + " History " + hstRetrieval.getSequencNum() + " in progress.";
		if (realtimeInProgress())
			msg = msg + " RealTime " + rtRetrieval.getSequencNum() + " in progress.";
		Logger.instance().debug1(msg);
		saveStatus();
	}

	public void runRealtime()
	{
		if (rtRetrieval != null && rtRetrieval.isAlive())
			rtRetrieval.shutdown();
		
		File f = new File(EnvExpander.expand(snotelRtStationListFile));
		try
		{
			SnotelPlatformSpecList specList = new SnotelPlatformSpecList();
			specList.loadFile(f, Logger.instance());
			rtRetrieval = new RetrievalThread(this, specList, snotelConfig.realtimeSince, "now",
				rtSequence++, "rt");
			rtRetrieval.start();
			snotelStatus.lastRealtimeRun = System.currentTimeMillis();
		}
		catch(IOException ex)
		{
			Logger.instance().failure("Error loading real time station list '" + f.getPath()
				+ "': " + ex + " -- Cannot run real time retrieval!");
		}
		catch(Exception ex)
		{
			Logger.instance().failure("Unexpected error starting real time retrieval '" 
				+ f.getPath() + "': " + ex);
		}
	}
	
	public void runHistory(HistoryRetrieval hr)
	{
		if (hstRetrieval != null && hstRetrieval.isAlive())
		{
			// Don't let any single history retrieval take more than 5 minutes.
			if (System.currentTimeMillis() - historyStarted > 300000L)
			{
				Logger.instance().warning("History retrieval taking too long. Aborting.");
				hstRetrieval.shutdown();
			}
		}
		historyStarted = System.currentTimeMillis();
		
		try
		{
			SnotelPlatformSpecList hstSpecList = new SnotelPlatformSpecList();
			hstSpecList.addHistoryRetrieval(hr);
			String since = IDateFormat.toString(hr.getStart(), false);
			String until = IDateFormat.toString(hr.getEnd(), false);
			hstRetrieval = new RetrievalThread(this, hstSpecList, since, until, rtSequence++, "hs");
			hstRetrieval.start();
		}
		catch(Exception ex)
		{
			Logger.instance().failure("Unexpected error starting hitory retrieval for " 
				+ hr.getSpec().getDcpAddress() + ", start=" + hr.getStart() + ", end="
				+ hr.getEnd() + ": " + ex);
		}
	}

	public boolean historyInProgress()
	{
		return hstRetrieval != null && hstRetrieval.isAlive();
	}

	public boolean realtimeInProgress()
	{
		return rtRetrieval != null && rtRetrieval.isAlive();
	}

	public synchronized void retrievalFinished(RetrievalThread rt)
	{
		if (rt == rtRetrieval)
		{
			Logger.instance().info("RealTime Retrieval Sequence " + rt.getSequencNum()
				+ " Finished.");
			rtRetrieval = null;
		}
		else if (rt == hstRetrieval)
		{
			Logger.instance().info("History Retrieval Sequence " + rt.getSequencNum()
				+ " Finished.");
			hstRetrieval = null;
		}
		else
		{
			Logger.instance().info("Untracked Retrieval Sequence " + rt.getSequencNum()
				+ " Finished.");
		}
	}

}
