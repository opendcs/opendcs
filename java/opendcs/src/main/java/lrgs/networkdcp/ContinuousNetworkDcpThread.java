/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package lrgs.networkdcp;


import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.common.DcpMsgFlag;
import lrgs.drgsrecv.DrgsRecvMsgThread;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

public class ContinuousNetworkDcpThread extends DrgsRecvMsgThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.debug("try connect name={}, host={}, port={}, lastPoll={}",
				  myName,getHost(),getPort(), myStat.getLastPollAttempt());
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