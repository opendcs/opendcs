/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2004/06/03 15:34:16  mjmaloney
*  LRIT release prep
*
*  Revision 1.6  2004/05/27 13:21:58  mjmaloney
*  dev.
*
*  Revision 1.5  2004/05/27 13:15:01  mjmaloney
*  DR fixes.
*
*  Revision 1.4  2004/05/24 17:11:08  mjmaloney
*  release prep
*
*  Revision 1.3  2004/05/19 14:19:00  mjmaloney
*  dev
*
*  Revision 1.2  2004/05/19 14:03:44  mjmaloney
*  dev.
*
*  Revision 1.1  2004/05/18 01:01:58  mjmaloney
*  Created.
*
*/
package lqm;

import java.io.*;
import ilex.util.DirectoryMonitorThread;
import ilex.util.Logger;
import lritdcs.*;

/**
This class extends the ILEX DirectoryMonitorThread utility to monitor
for incoming LRIT files in the specified directory.
When a complete incoming file is detected, it is processed.
*/
public class LqmDirectoryMonitor extends DirectoryMonitorThread
{
	/// We send messages to senderThread for notifying the LRIT sender.
	SenderThread senderThread = null;

	/// Last time this object retrieved its configuration.
	long lastConfigGet;

	/// Use LRIT File Headers -- turned off by cmd line arg for test mode.
	boolean useFileHeaders;

	LqmDirectoryMonitor(boolean useFileHeaders)
	{
		super();
		this.useFileHeaders = useFileHeaders;
		setSleepEveryCycle(true);
		setSleepInterval(1000L);
		configure();
		setFilenameFilter(
			new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					return name.startsWith("DCS")
						&& name.endsWith(".lrit");
				}
			});
		lastConfigGet = 0;
	}

	/// Called from LQM main after constructing the SenderThread.
	public void setSenderThread(SenderThread st)
	{
		senderThread = st;
	}

	/// Called from base class when a new file is detected.
	protected void processFile(File file)
	{
		Logger.instance().log(Logger.E_INFORMATION,
			"Monitor found file '" + file.getPath() + "'");

		LritDcsFileReader ldfr = new LritDcsFileReader(file.getPath(), 
			useFileHeaders);
	
		try{ ldfr.load(); }
		catch(IOException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Can't load " + file + ": " + ex);
			return;
		}
		catch(BadMessageException ex)
		{
			Logger.instance().log(Logger.E_FAILURE,
				"Bad LRIT header in file '" + file.getName() + "': " + ex);
			if (System.currentTimeMillis() - file.lastModified()
				> 60000L)
			{
				file.renameTo(
					new File(LqmConfiguration.instance().dcsDoneDir, 
						file.getName()));
			}
			return;
		}

		boolean check = true;
		String nm = ldfr.origFileName();
		if (!ldfr.checkLength())
		{
			Logger.instance().log(Logger.E_INFORMATION,
				"File " + file + " failed checkLength, not completed");
			check = false;
			if (System.currentTimeMillis() - file.lastModified() < 60000L)
			{
				return;
			}
		}
		else if (!ldfr.checkCRC())
		{
			Logger.instance().log(Logger.E_INFORMATION,
				"CRC failed on " + file.getPath());
			check = false;
		}
		
		// Tell sender thread to send notification.
		senderThread.sendResult(nm, check);

		if (!file.renameTo(
			new File(LqmConfiguration.instance().dcsDoneDir, file.getName())))
		{
			Logger.instance().warning(
				"Could not move " + file.getPath() + " to DONE directory. "
				+ "Attempting to delete.");
			file.delete();
		}
	}
	
	/// Will be called after each dir scan, about once per second.
	public void finishedScan()
	{
		if (lastConfigGet < LqmConfiguration.instance().getLastLoadTime())
			configure();
	}
	
	/// Check to se if config has been updated, retrieve my variables if so.
	public void configure()
	{	
		Logger.instance().log(Logger.E_INFORMATION, 
			"LqmDirectoryMonitor Getting configuration");
		emptyDirectories();
		addDirectory(LqmConfiguration.instance().dcsInputDir);
		Logger.instance().log(Logger.E_INFORMATION,
			"LqmDirectoryMonitor input dir='"
			+ LqmConfiguration.instance().dcsInputDir.getPath()
			+ "'");
		lastConfigGet = System.currentTimeMillis();
	}

	@Override
	protected void cleanup()
	{
	}

	/// Test main obsolete.
//	public static void main(String[] args)
//	{
//		Logger.instance().log(Logger.E_INFORMATION, "Program Starting");
//		LqmConfiguration cfg = LqmConfiguration.instance();
//		try { cfg.loadConfig();}
//		catch(IOException ex)
//		{
//			Logger.instance().log(Logger.E_FATAL,
//				"Cannot read config file: " + ex);
//		}
//	
//		LqmDirectoryMonitor myMonitor = new LqmDirectoryMonitor();
//		myMonitor.senderThread = new SenderThread();
//		myMonitor.senderThread.start();
//		myMonitor.start();
//	}

}
