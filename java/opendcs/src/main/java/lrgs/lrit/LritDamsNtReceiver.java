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
package lrgs.lrit;

import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.ByteUtil;
import lrgs.archive.MsgArchive;
import lrgs.common.DcpMsg;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgsrecv.DrgsRecv;
import lrgs.drgsrecv.DrgsRecvMsgThread;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsMain;

public class LritDamsNtReceiver extends DrgsRecvMsgThread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private int slot = 0;
	private long lastConfigure = 0L;

	public LritDamsNtReceiver(MsgArchive msgArchive, LrgsMain lrgsMain)
	{
		super(msgArchive, lrgsMain);
		module = "LritRcv";
		myType = DL_LRIT;
		myTypeStr = DL_LRIT_TYPESTR;
	}

	/**
	 * Instead of reading from a DRGS Connect Config object, read info
	 * directly out of LRGS Config.
	 */
	@Override
	public void configure(DrgsConnectCfg ignore)
	{
		myName = "LRIT-DAMS-NT";
		LrgsConfig lrgsCfg = LrgsConfig.instance();
		_enabled = lrgsCfg.enableLritRecv;

		setHost(lrgsCfg.lritHostName);
		setPort(lrgsCfg.lritPort);
		configChanged = true;

		if (!_enabled)
		{
			status = "Disabled";
			disconnect();
		}

		dataSourceId =
			lrgsMain.getDbThread().getDataSourceId(DL_LRIT_TYPESTR, lrgsCfg.lritHostName);

		startPattern = ByteUtil.fromHexString(lrgsCfg.lritDamsNtStartPattern);
		log.debug("Configured: Enabled={}, host={}, port={}, startPat={}",
				  _enabled, lrgsCfg.lritHostName, lrgsCfg.lritPort, lrgsCfg.lritDamsNtStartPattern);
		lastConfigure = System.currentTimeMillis();
	}


	@Override
	public void setSlot(int slot) { this.slot = slot; }

	@Override
	public int getSlot() { return slot; }

	@Override
	public String getInputName()
	{
		return "LRIT:" + getHost();
	}

	@Override
	public void initLrgsInput()
		throws LrgsInputException
	{
		// In the normal DAMS-NT, starting the thread is handled by the parent.
		// For LRIT, we do it here.
		Thread t = new Thread(this);
		t.start();
	}

	@Override
	public void shutdownLrgsInput()
	{
		super.shutdownLrgsInput();
	}

	@Override
	public void enableLrgsInput(boolean enabled)
	{
		super.enableLrgsInput(enabled);
	}

	@Override
	protected void checkConfig()
	{
		LrgsConfig lrgsCfg = LrgsConfig.instance();
		if (lrgsCfg.getLastLoadTime() > lastConfigure)
			configure(null);
	}

	@Override
	public String getName()
	{
		LrgsConfig lrgsCfg = LrgsConfig.instance();
		return "LRIT:" + lrgsCfg.lritHostName + ":" + lrgsCfg.lritPort;
	}

	@Override
	protected boolean checkMsgOk(DcpMsg msg)
	{
		//If Drgs receives msgs older than an hour ago
		//- ignore then Check Msg Date
		Date msgDate = msg.getDapsTime();
		long currentTime = System.currentTimeMillis();
		if (msgDate != null)
		{
			if ((currentTime - msgDate.getTime()) > LrgsConfig.instance().lritMaxMsgAgeSec*1000L )
			{
				log.warn("{} Received msg older than threshold. DCP address: {}, msgtime={}, curtime={}, " +
						 "threshold = {} seconds",
						 DrgsRecv.EVT_MSG_TOO_OLD, msg.getDcpAddress(), msgDate,
						 new Date(currentTime), LrgsConfig.instance().lritMaxMsgAgeSec);
				return false;
			}
		}
		return true;
	}


}
