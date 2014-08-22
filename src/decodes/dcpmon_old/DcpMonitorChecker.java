package decodes.dcpmon_old;
///*
//*  $Id$
//*
//*  $Log$
//*  Revision 1.2  2014/06/27 20:36:38  mmaloney
//*  Initial unfinished version moved from dcpmon1 package to here.
//*
//*  Revision 1.1  2014/06/02 14:28:50  mmaloney
//*  rc5 includes initial refactory for dcpmon
//*
//*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
//*  OPENDCS 6.0 Initial Checkin
//*
//*  Revision 1.6  2012/06/28 13:35:07  mmaloney
//*  synchronization deadlock issue at MVR.
//*
//*  Revision 1.5  2011/09/27 01:24:33  mmaloney
//*  Bug Fixes at AENV
//*
//*  Revision 1.4  2009/06/18 18:01:56  mjmaloney
//*  Run computations from HTML display in dcp monitor
//*
//*  Revision 1.3  2008/09/13 21:42:53  mjmaloney
//*  dev
//*
//*  Revision 1.2  2008/09/12 15:40:57  mjmaloney
//*  Mods for DCP Monitor 7.5
//*
//*  Revision 1.1  2008/04/04 18:21:01  cvs
//*  Added legacy code to repository
//*
//*  Revision 1.7  2007/12/04 15:02:33  mmaloney
//*   fixed bug
//*
//*  Revision 1.12  2007/10/26 17:02:34  mmaloney
//*  added code to check for update in controlling dist file and/or drgs file
//*
//*  Revision 1.11  2006/12/04 17:35:05  mmaloney
//*  dev
//*
//*  Revision 1.10  2005/11/21 19:14:28  mmaloney
//*  LRGS 5.4 prep
//*
//*  Revision 1.9  2005/05/23 20:55:55  mjmaloney
//*  Scrub old directories on startup.
//*
//*  Revision 1.8  2005/01/05 19:21:05  mjmaloney
//*  Bug fixes & updates.
//*
//*  Revision 1.7  2004/09/23 13:41:54  mjmaloney
//*  javadoc clean-up
//*
//*  Revision 1.6  2004/07/05 14:35:22  mjmaloney
//*  dev.
//*
//*  Revision 1.5  2004/07/02 14:40:04  mjmaloney
//*  Cosmetic mods.
//*
//*  Revision 1.4  2004/06/28 14:24:40  mjmaloney
//*  Cosmetic bug fixes
//*
//*  Revision 1.3  2004/05/17 17:22:31  mjmaloney
//*  Improvements to DCP Monitor GUI for msg time fudge-factor of +- 5 sec.
//*  Fixed perl bug caused when server hangs up.
//*
//*  Revision 1.2  2004/03/31 14:16:34  mjmaloney
//*  Updates to DCP Monitor.
//*
//*  Revision 1.1  2004/03/18 16:18:43  mjmaloney
//*  Working server version beta 01
//*
//*/
//package decodes.dcpmon;
//
//import java.io.*;
//
//import decodes.comp.BadConfigException;
//import decodes.comp.ComputationProcessor;
//import decodes.drgsinfogui.DrgsReceiverIo;
//import decodes.dupdcpgui.DuplicateIo;
//import decodes.util.ChannelMap;
//import ilex.util.*;
//
///**
//This class is a Thread that runs in the background to periodically
//check for changes in the configuration file, the channel map, or any
//of the network lists that represent groups.
//*/
//public class DcpMonitorChecker extends Thread
//{
//	/** Period (msec) for checking configuration. */
//	public final static long ConfigCheckTime = 60000L;
//
//	/** Period (msec) for checking network lists. */
//	public final static long NetlistCheckTime = 60000;
//
//	/** the configuration file */
//	File cfgFile;
//
//	/** The main, parent module */
//	DcpMonitor dcpMonitor;
//
//	/**
//	  Constructor.
//	  @param dcpm The main, parent module
//	  @param cfgFileName the configuration file, will be EnvExpanded
//	*/
//	public DcpMonitorChecker(DcpMonitor dcpm, String cfgFileName)
//	{
//		dcpMonitor = dcpm;
//		cfgFile = new File(EnvExpander.expand(cfgFileName));
//	}
//
//	/** Thread run method */
//	public void run()
//	{
//		long now = System.currentTimeMillis();
//		long lastConfigCheck = now;
//		long lastNetlistCheck = now;
//		//Use to verify if the controlling Distrit file has been
//		//modified or not
//		DuplicateIo dupIo = 
//			DcpMonitor.instance().getDcpNameDescResolver().getDuplicateIo();
//		File dupIoFile = dupIo.getDistFile();
//		long lastControlDistMod = dupIoFile.lastModified();
//		//Use to verify if the DRGS Receiver list file has been modified
//		File dr = new File(DrgsReceiverIo.getDrgsRecvXmlFname());
//		long lastDRGSMod = dr.lastModified();
//
//		while(true)
//		{
//			now = System.currentTimeMillis();
//			if (now - lastConfigCheck > ConfigCheckTime)
//			{
//				checkConfig();
//				lastConfigCheck = now;
//			}
//			if (now - lastNetlistCheck > NetlistCheckTime)
//			{
//				checkNetlist();
//				lastNetlistCheck = now;
//			}
//
//			//Check Controlling District and DRGS Receivers file
//			long cdCurrentM = dupIoFile.lastModified();
//			if (lastControlDistMod != cdCurrentM)
//			{
//				//controllingDistrict file was modified
//				dupIo.readControllingDist();
//				lastControlDistMod = cdCurrentM;
//			}
//			long drCurrentM = dr.lastModified();
//			if (lastDRGSMod != drCurrentM)
//			{
//				//Read Drgs Receiver file and store it in memory
//				DrgsReceiverIo.readDrgsReceiverFile();
//				lastDRGSMod = drCurrentM;
//			}
//			
//			try { sleep(5000L); }
//			catch(InterruptedException ex) {}
//		}
//	}
//	private void checkConfig()
//	{
//		Logger.instance().debug2("Checking configuration");
//		DcpMonitorConfig cfg =  DcpMonitorConfig.instance();
//
//		if (cfgFile.lastModified() > cfg.lastLoadTime)
//		{
////			boolean useEditDb = cfg.useEditDb;
////			int serverPort = cfg.serverPort;
////			String dataSourceName = cfg.dataSourceName;
////			int numDaysStorage = cfg.numDaysStorage;
////			String channelMapUrl = cfg.channelMapUrl;
////
////			cfg.loadFromProperties(cfgFile.getPath());
////
////			if (useEditDb != cfg.useEditDb)
////			{
////				Logger.instance().log(Logger.E_FAILURE,
////					"Cannot change choice of DECODES databases "
////					+ " -- you must stop and restart the DCP Monitor Server.");
////				cfg.useEditDb = useEditDb;
////			}
////			if (serverPort != cfg.serverPort)
////			{
////				Logger.instance().log(Logger.E_FAILURE,
////					"Cannot change the server port"
////					+ " -- you must stop and restart the DCP Monitor Server.");
////				cfg.serverPort = serverPort;
////			}
////			if (!dataSourceName.equals(cfg.dataSourceName))
////			{
////				cfg.dataSourceName = dataSourceName;
////				Logger.instance().info("dataSourceName has changed");
////				rebuildRoutingSpec();
////			}
////			if (numDaysStorage != cfg.numDaysStorage)
////			{
////				Logger.instance().log(Logger.E_FAILURE,
////					"Cannot change Number of Days Storage"
////					+ " -- you must stop and restart the DCP Monitor Server.");
////				cfg.numDaysStorage = numDaysStorage;
////			}
////
////			if (!channelMapUrl.equals(cfg.channelMapUrl))
////			{
////				ChannelMap.instance().stopMaintenanceThread();
////				try { Thread.sleep(5000L);}
////				catch(InterruptedException ex) {}
////				ChannelMap.instance().startMaintenanceThread(
////					cfg.channelMapUrl, cfg.channelMapLocalFile);
////			}
////
////			if (cfg.checkAndLoadNetworkLists())
////			{
////				Logger.instance().info(
////					"Configured network list groups have changed.");
////				rebuildRoutingSpec();
////			}
////			
////			dcpMonitor.initCompProc();
//		}
//	}
//
//	private void checkNetlist()
//	{
//		Logger.instance().info("Checking network lists");
//		DcpGroupList dgl = DcpGroupList.instance();
//		if (dgl.checkGroups())
//		{
//			Logger.instance().info("Groups are modified.");
//			rebuildRoutingSpec();
//		}
//	}
//
//	private void rebuildRoutingSpec()
//	{
//		try 
//		{
//			Logger.instance().info("rebuildRoutingSpec");
//			dcpMonitor.makeRtRoutingSpec(); 
//			dcpMonitor.routingSpecThread.shutdown();
//			// This will cause the currently running routing spec to exit.
//			// The monitor will notice and restart with the new routing spec.
//		}
//		catch(decodes.db.DatabaseException ex)
//		{
//			Logger.instance().log(Logger.E_FAILURE,
//				"Cannot re-make routing spec: " + ex
//				+ " -- will leave old one running.");
//		}
//	}
//
//}
