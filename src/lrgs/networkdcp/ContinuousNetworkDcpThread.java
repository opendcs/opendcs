/**
 * 
 */
package lrgs.networkdcp;

import ilex.util.Logger;

import java.io.IOException;
import java.util.Date;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.drgsrecv.DrgsRecvMsgThread;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

public class ContinuousNetworkDcpThread
    extends DrgsRecvMsgThread
{
	private DcpConfigList cfgList;
	private NetworkDcpStatusList statusList;

	public ContinuousNetworkDcpThread(MsgArchive msgArchive, LrgsMain lrgsMain,
		DcpConfigList cfgList, NetworkDcpStatusList statusList)
	{
		super(msgArchive, lrgsMain);
		this.cfgList = cfgList;
		this.statusList = statusList;
		noChannelFile = true;
		myType = DL_NETDCPCONT;
		noChannelFile = true;
		module = "ContNetDcp";
		connectRetryDelay = 60000L;
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

	/* Don't overload run() method, use the super-class version */

	protected void tryConnect()
	{
		NetworkDcpStatus myStat = statusList.getStatus(getHost(), getPort());
		myStat.setLastPollAttempt(new Date());
		Logger.instance().debug1(module + " try connect name=" + myName
			+ ", host=" + getHost() + ", port=" + getPort() 
			+ ", lastPoll=" + myStat.getLastPollAttempt());
		super.tryConnect();
		if (isConnected())
			myStat.setLastContact(new Date());
		else
    		myStat.setNumFailedPolls(myStat.getNumFailedPolls() + 1);
	}

	/**
	 * DRGS only allows real-time msgs, must be within an hour old.
	 * Network DCPs do not impose that restriction.
	 */
	protected boolean checkMsgOk(DcpMsg msg)
	{
		NetworkDcpStatus myStat = statusList.getStatus(getHost(), getPort());
		myStat.setNumMessages(myStat.getNumMessages() + 1);
		return true;
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
}
