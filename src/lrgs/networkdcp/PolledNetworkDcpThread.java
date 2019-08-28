/**
 * 
 */
package lrgs.networkdcp;

import ilex.util.Logger;

import java.io.IOException;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.drgsrecv.DrgsRecvMsgThread;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

/**
 * This thread polls network DCPs.
 *
 */
public class PolledNetworkDcpThread
    extends DrgsRecvMsgThread
{
	private DcpConfigList cfgList;
	private NetworkDcpStatusList statusList;

	public PolledNetworkDcpThread(int threadNum,
		MsgArchive msgArchive, LrgsMain lrgsMain,
		DcpConfigList cfgList, NetworkDcpStatusList statusList)
	{
		super(msgArchive, lrgsMain);
		this.cfgList = cfgList;
		this.statusList = statusList;
		myName = "NetDcpPoll[" + threadNum + "]";
		noChannelFile = true;
	}

	/**
	 * Called every time through the superclass loop. Check to see if
	 * the configuration state has changed to dead. If so, it means we
	 * need to shutdown.
	 */
	public boolean isEnabled()
	{
		if (myCfg != null && myCfg.getState() == NetworkDcpState.Dead)
			return false;
		return super.isEnabled();
	}

	/**
	 * Thread run method
	 * Algorithm is:
	 * 	- Try to get a config off the queue. Sleep & try again if none 
	 *    available.
	 *  - Once you have a config, call configure, they behave similarly to 
	 *    super class, but once you get a timeout or NONE, update the poll 
	 *    times in the config and return it to the queue.
	 */
	public void run()
	{
		try{ Thread.sleep(2000L); }
		catch(InterruptedException ex) {}
		
		lastResponseTime = System.currentTimeMillis();

		int numMsgsThisPoll = 0;
		while(!_isShutdown)
		{
			if (!_enabled)
			{
				try{ Thread.sleep(1000L); }
				catch(InterruptedException ex) {}
				continue;
			}
			long now = System.currentTimeMillis();

			if (myCfg == null)
			{
				status = "Idle";
				numMsgsThisPoll = 0;
				DrgsConnectCfg cfg = cfgList.getNextConfigToPoll();
				if (cfg == null)
				{
					try{ Thread.sleep(1000L); }
					catch(InterruptedException ex) {}
					continue;
				}
				info("Polling " + cfg.name);
				status = "Connecting";
				configure(cfg);
				tryConnect();
				if (!isConnected())
				{
					cfgList.pollFinished(cfg);
					info("Failed to connect to " + cfg.name);
					statusList.pollAttempt(myCfg, false, 0);
					myCfg = null;
					continue;
				}
				status = "Polling";
			}

			try 
			{
				DcpMsg msg = getMsg();
				if (msg != null)
				{
					msg.setSequenceNum(getNextSeqNum());
					msgArchive.archiveMsg(msg, this);
					numMsgsThisPoll++;
					if (activityLogger != null)
						activityLogger.info(msg.getHeader());
				}
				else // no msg yet.
				{
					// Returned NONE or has been set to DEAD?
					if (wasNone || !isEnabled())
					{
						statusList.pollAttempt(myCfg, true, numMsgsThisPoll);
						disconnect();
					}
					else if (now - lastResponseTime > 20000)
					{
						// More than 20 seconds since either msg or NONE.
						log(Logger.E_WARNING, DrgsRecv.EVT_TIMEOUT,
							"Timeout on Network DCP -- disconnecting.");
						statusList.pollAttempt(myCfg, true, numMsgsThisPoll);
						disconnect();
					}
					else
					{	// Brief pause waiting for more data to arrive.
						try { Thread.sleep(100L); }
						catch(InterruptedException ex) {}
					}
				}
			}
			catch(IOException ex)
			{
				log(Logger.E_WARNING, DrgsRecv.EVT_SOCKIO,
					"Error on Network DCP: " + ex);
				disconnect();
			}
		}
		info("Disconnecting and exiting.");
		try { disconnect(); }
		catch(Exception ex){}
		status = "Shutdown";
	}
	
	public void disconnect()
	{
		if (myCfg != null)
			cfgList.pollFinished(myCfg);

		super.disconnect();
		status = "Idle";
	}

	private void info(String msg)
	{
		Logger.instance().info(myName + " " + msg);
	}

	/**
	 * @return the name of this interface.
	 */
	public String getInputName()
	{
		return myName != null ? myName : LrgsInputInterface.DL_NETDCPPOLL_TYPESTR;
	}

	public int getType()
	{
		return LrgsInputInterface.DL_NETDCPPOLL;
	}
	
	@Override
	protected int getMsgTypeFlag()
	{
		return DcpMsgFlag.MSG_TYPE_NETDCP;
	}

}
