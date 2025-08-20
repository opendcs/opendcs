/**
 * @(#) LritDcsReceiver.java
 */

package lritdcs.recv;


import java.util.TimeZone;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import ilex.util.Logger;
import ilex.util.FileServerLock;
import ilex.util.EnvExpander;
import ilex.util.ServerLock;

public class LritDcsReceiver
{
	private LritDcsRecvConfig config;
	
	private LritDcsDirMonitor myMonitor;
	
	private static LdrCmdLineArgs cmdLineArgs = new LdrCmdLineArgs();

	private ServerLock mylock;

	private LritDcsReceiver()
	{
	}

	public void run()
	{
		Logger.instance().info("Program Starting");

		// Check lock file.
		String lockpath = EnvExpander.expand(cmdLineArgs.getLockFile());
		mylock = new FileServerLock(lockpath);
        if (mylock.obtainLock() == false)
        {
            Logger.instance().log(Logger.E_FATAL,
                "NOT started: lock file busy");
            System.exit(0);
        }
                                                                                
        mylock.releaseOnExit();
        Runtime.getRuntime().addShutdownHook(
            new Thread()
            {
                public void run()
                {
                    Logger.instance().log(Logger.E_INFORMATION,
                        "DCP LRIT Receiver exiting " +
                        (mylock.wasShutdownViaLock() ? "(lock file removed)"
                        : ""));
                }
            });

		LritDcsRecvConfig cfg = LritDcsRecvConfig.instance();
		String cfgName = cmdLineArgs.getConfigFile();
		cfg.setPropFile(cfgName);
		try { cfg.load(); }
		catch(IOException ex)
		{
			String msg = 
				"Cannot read config file '" + cfgName + "': " + ex;
			Logger.instance().fatal(msg);
			System.err.println(msg);
			System.exit(1);
		}

		MsgArch arch = new MsgArch();
		arch.init();
	
		myMonitor = new LritDcsDirMonitor(arch); 
		myMonitor.start();

		// Main Loop
		while(true)
		{
			try 
			{
				Thread.sleep(60000); 
				cfg.checkConfig();
				scrubArchive();
				scrubDoneFiles();
			}
			catch(InterruptedException ex) {}
			catch(IOException ex2)
			{
				Logger.instance().warning("Config file error: " + ex2);
			}
		}
	}

	private void scrubArchive()
	{
		File arcDir = new File(LritDcsRecvConfig.instance().msgFileDir);
		File files[] = arcDir.listFiles();
		int pfxLen = MsgPerArch.filePrefix.length();
		int dateLen = MsgPerArch.dateSpec.length();
		int neededLen = pfxLen + dateLen + MsgPerArch.fileSuffix.length();
		for(int i=0; files != null && i < files.length; i++)
		{
			String nm = files[i].getName();
			if (nm.length() != neededLen)
			{
				Logger.instance().warning(
					"Scrubber - skipping file with bad name length '" 
					+ nm + "'");
				continue;
			}
			try
			{
				Date d = MsgPerArch.fnf.parse(
					nm.substring(pfxLen, pfxLen+dateLen));
				if (System.currentTimeMillis() - d.getTime() >
					(1000L * 60L * 60L * 24L))
				{
					Logger.instance().info("Scrubber deleting '" + nm + "'");
					if (!files[i].delete())
						Logger.instance().warning("Scrubber could not delete '"
							+ nm + "'");
				}
			}
			catch(ParseException pex)
			{
				Logger.instance().warning(
					"Scrubber - skipping file with bad date component '"
					+ nm + "': " + pex);
			}
		}
		
	}

	private void scrubDoneFiles()
	{
		File doneDir = new File(LritDcsRecvConfig.instance().fileDoneDir);
		if (!doneDir.isDirectory())
			return;
		long now = System.currentTimeMillis();
		File list[] = doneDir.listFiles();
		for(int i=0; list != null && i < list.length; i++)
		{
			if (now - list[i].lastModified() > (1000L*60L*60L*24L))
			{
				try 
				{
					if (!list[i].delete())
						Logger.instance().warning("Cannot delete '"
							+ list[i].getPath() + "'");
				}
				catch(Exception ex)
				{
					Logger.instance().warning("Cannot delete '"
						+ list[i].getPath() + "': " + ex);
				}
			}
		}
	}


	public static void main( String[] args )
		throws IOException
	{
		cmdLineArgs.parseArgs(args);
		Logger.instance().setTimeZone(TimeZone.getTimeZone("UTC"));
		LritDcsReceiver main = new LritDcsReceiver();
		main.run();
	}
}
