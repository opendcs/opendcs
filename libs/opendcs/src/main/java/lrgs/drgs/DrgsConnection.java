/*
*  $Id$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2008/09/21 16:08:51  mjmaloney
*  network DCPs
*
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/08/09 18:20:01  mjmaloney
*  dev
*
*  Revision 1.5  2005/07/12 00:03:26  mjmaloney
*  Implemented DRGS interface for Java-Only-Lrgs
*
*  Revision 1.4  2004/09/02 13:09:03  mjmaloney
*  javadoc
*
*  Revision 1.3  2003/04/09 19:38:03  mjmaloney
*  impl
*
*  Revision 1.2  2003/04/09 15:16:11  mjmaloney
*  dev.
*
*  Revision 1.1  2003/03/27 21:17:43  mjmaloney
*  drgs dev
*
*/
package lrgs.drgs;

import ilex.util.Logger;
import lrgs.drgsrecv.DrgsRecvMsgThread;

/**
Envelope class holding the connections (message and/or event) to a particular
DRGS.
*/
public class DrgsConnection
{
	/** The connection number */
	int connectNum;
	/** True if configured. Used to coordinate re-configuration by parent. */
	boolean configured;
	/** My configuration data structure */
	DrgsConnectCfg myConfig;
	/** Thread to handle the message socket */
	DrgsRecvMsgThread myMsgThread;
	/** Thread to handle the events socket */
	DrgsEvtThread myEvtThread;

	/**
	  Constructor.
	  @param num connection number (must be unique for each DRGS).
	*/
	public DrgsConnection(int num, DrgsRecvMsgThread msgThread)
	{
		connectNum = num;
		configured = false;
		myMsgThread = msgThread;
		myMsgThread.setConnectNum(connectNum);
		Thread t = new Thread(myMsgThread);
		t.start();
		myEvtThread = new DrgsEvtThread();
//		t = new Thread(myEvtThread);
//		t.start();
	}

	/**
	  Sets the configuration to be used on this connection.
	  Save configuration locally and then call the <code>configure</code>
	  method in the DrgsMsgThread and DrgsEvtThread constituents.

	  @param cfg the configuration
	*/
	public void configure(DrgsConnectCfg cfg)
	{
		configured = true;
		myConfig = cfg;
		Logger.instance().log(Logger.E_DEBUG1,
			"Configuring drgs[" + connectNum + "]: " + cfg.toString());

		// Reconfigure the threads
		myMsgThread.configure(cfg);
		myEvtThread.configure(cfg.host, cfg.evtPort, cfg.evtEnabled);
	}
	
	public DrgsConnectCfg getConfig() { return myConfig; }

	/** Shuts this DRGS connection down. */
	public void shutdown()
	{
		Logger.instance().log(Logger.E_DEBUG1,
			"Killing drgs[" + connectNum + "]");

		myMsgThread.shutdown();
		myEvtThread.shutdown();
	}

	/** @return the connection number. */
	public int getConnectNum()
	{
		return connectNum;
	}

	/** @return true if this link was configured since last call to clear. */
	public boolean isConfigured()
	{
		return configured;
	}

	/** Clears the configuration flag. */
	public void clearConfigured()
	{
		configured = false;
	}

	public DrgsRecvMsgThread getMsgThread() { return myMsgThread; }
}
