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
package lrgs.drgs;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.drgsrecv.DrgsRecvMsgThread;

/**
Envelope class holding the connections (message and/or event) to a particular
DRGS.
*/
public class DrgsConnection
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		log.debug("Configuring drgs[{}]: {}", connectNum, cfg.toString());

		// Reconfigure the threads
		myMsgThread.configure(cfg);
		myEvtThread.configure(cfg.host, cfg.evtPort, cfg.evtEnabled);
	}

	public DrgsConnectCfg getConfig() { return myConfig; }

	/** Shuts this DRGS connection down. */
	public void shutdown()
	{
		log.debug("Killing drgs[{}]", connectNum);

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