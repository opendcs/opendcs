package decodes.snotel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import decodes.util.CmdLineArgs;
import decodes.util.DecodesVersion;
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
	private long historyStarted = 0L;
	private int rtSequence = 1;
	private long nextRtRun = 0L;
	

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
		Logger.instance().info("====== SnotelDaemon Starting " + DecodesVersion.getVersion()
			+ " ======");
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

		// Check last run time and determine the next time to start rt retrieval.
		computeNextRtRun();
		
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
		
		long now = System.currentTimeMillis();
		
		String msg = "scanFinished()";

		if (historyInProgress())
			msg = msg + " History " + hstRetrieval.getSequencNum() + " in progress.";

		if (realtimeInProgress())
			msg = msg + " RealTime " + rtRetrieval.getSequencNum() + " in progress.";
		else if (now >= nextRtRun)
		{
			msg = msg + " RealTime " + rtSequence + " starting.";
			runRealtime();
		}
			
		Logger.instance().debug2(msg);
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
			
			computeNextRtRun();
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
	
	/**
	 * Computes the next time to run a real-time retrieval. Assume that it is in the future.
	 * Called once at startup and then after each rt retrieval thread exits.
	 */
	private void computeNextRtRun()
	{
		if (snotelConfig.retrievalFreq <= 0)
		{
			// RT thread is supposed to always run in real-time. Start it now.
			Logger.instance().info("retrievalFreq==0. Will start rt immediately.");
			nextRtRun = System.currentTimeMillis();
			return;
		}
		
		long now = System.currentTimeMillis();
		// Make a calendar set to today at midnight.
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone(snotelConfig.outputTZ));
		cal.setTimeInMillis(now);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		// Add interval until > now
		while(cal.getTimeInMillis() <= now)
			cal.add(Calendar.MINUTE, snotelConfig.retrievalFreq);
		
		Logger.instance().info("Next realtime retrieval set for " + cal.getTime());
		nextRtRun = cal.getTimeInMillis();
	}

}
