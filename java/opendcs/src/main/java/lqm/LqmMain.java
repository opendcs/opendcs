/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/03/16 14:16:58  mjmaloney
*  dev
*
*  Revision 1.5  2004/06/03 15:34:17  mjmaloney
*  LRIT release prep
*
*  Revision 1.4  2004/05/24 20:34:49  mjmaloney
*  Release Prep
*
*  Revision 1.3  2004/05/24 17:11:08  mjmaloney
*  release prep
*
*  Revision 1.2  2004/05/19 14:03:44  mjmaloney
*  dev.
*
*  Revision 1.1  2004/05/18 01:01:58  mjmaloney
*  Created.
*
*/
package lqm;

import java.util.*;
import java.io.*;

import lritdcs.DcsCmdLineArgs;
import ilex.util.Logger;


public class LqmMain
{
	LqmDirectoryMonitor myMonitor;
	SenderThread senderThread;

	LqmMain()
	{
	}

	public void run()
	{
		Logger.instance().info("Program Starting");
		LqmConfiguration cfg = LqmConfiguration.instance();

		String cfgName = cmdLineArgs.getConfigFile();
		cfg.setConfigFileName(cfgName);
		try { cfg.loadConfig();}
		catch(IOException ex)
		{
			String msg = 
				"Cannot read config file '" + cfgName + "': " + ex;
			Logger.instance().fatal(msg);
			System.err.println(msg);
			System.exit(1);
		}
	
		myMonitor = new LqmDirectoryMonitor(cmdLineArgs.useFileHeaders());
		senderThread = new SenderThread();
		myMonitor.setSenderThread(senderThread);

		senderThread.start();
		myMonitor.start();

		// Main Loop
		while(true)
		{
			try { Thread.sleep(10000); }
			catch(InterruptedException ex) {}
			cfg.checkConfig();
			checkDoneFileAges();
			senderThread.sendStatus("OK");
		}
	}


	private void checkDoneFileAges()
	{
		LqmConfiguration cfg = LqmConfiguration.instance();
		long allowableFileAge = System.currentTimeMillis() 
			- (cfg.maxFileAge * 86400000L);

		try
		{
			File path = cfg.dcsDoneDir;
			File pathFiles[] = path.listFiles();
			int pathLength = pathFiles.length;
			for(int a = 0; a < pathLength; a++)
			{
				if(pathFiles[a].lastModified() < allowableFileAge)
				{
					Logger.instance().info(
 						"Deleting Old File: " + pathFiles[a].getName());
					pathFiles[a].delete();
				}
			}
		}
		catch(Exception ex)
		{
			Logger.instance().log(Logger.E_WARNING,
				"Exception listing directory '" + cfg.dcsDoneDir.getPath()
					+ "' -- will not delete any old files.");
		}
	}

	private static LqmCmdLineArgs cmdLineArgs = new LqmCmdLineArgs();

	public static void main(String args[])
		throws IOException
	{
		cmdLineArgs.parseArgs(args);
		Logger.instance().setTimeZone(TimeZone.getTimeZone("UTC"));
		LqmMain lm = new LqmMain();
		lm.run();
	}
}
