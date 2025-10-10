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

import java.util.ArrayList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;

/**
 * Manages the list/queue of DCPs to poll.
 */
public class DcpConfigList
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private ArrayList<DrgsConnectCfg> configs = new ArrayList<DrgsConnectCfg>();

	public DcpConfigList()
	{
	}

	/**
	 * Called whenever the configuration file was modified, after loading
	 * the settings. Resets the queue.
	 * @param settings the new settings.
	 */
	public synchronized void processNewConfig(DrgsInputSettings settings,
		NetworkDcpStatusList statusList)
	{
		log.info("Processing new Network DCP Configuration");
		ArrayList<DrgsConnectCfg> toDelete = new ArrayList<DrgsConnectCfg>();
		toDelete.addAll(configs);
		ArrayList<DrgsConnectCfg> toAdd = new ArrayList<DrgsConnectCfg>();
		for(DrgsConnectCfg cfg : settings.connections)
		{
			// Is there a matching old config
			DrgsConnectCfg oldCfg = getConfig(cfg.host, cfg.msgPort);
			if (oldCfg != null)
			{
				if (oldCfg.equalToNetDcp(cfg)) // Is the same?
				{
					log.info("Already have equal config for host={}, port={}, poll={}",
							 cfg.host, cfg.msgPort, cfg.pollingPeriod);
					toDelete.remove(oldCfg);
				}
				else // different!
				{
					log.info("Config Changed for host={}, port={}, poll={}",
							 cfg.host, cfg.msgPort, cfg.pollingPeriod);
					// This will kill any thread servicing this connection.
					oldCfg.setState(NetworkDcpState.Dead);
					cfg.setState(NetworkDcpState.Waiting);
					toAdd.add(cfg);
				}
			}
			else // no matching old config
			{
				log.info("New Config loaded for host={}, port={}, poll={}",
						 cfg.host, cfg.msgPort, cfg.pollingPeriod);
				toAdd.add(cfg);
			}
		}
		try { Thread.sleep(5000L); } // Allow threads time to die.
		catch(InterruptedException ex) {}
		for(DrgsConnectCfg cfg : toDelete)
		{
			configs.remove(cfg);
			statusList.remove(cfg.host, cfg.msgPort);
			log.info("Removed Network DCP with host={}, port={}, poll={}",
					 cfg.host, cfg.msgPort, cfg.pollingPeriod);
		}
		for(DrgsConnectCfg cfg : toAdd)
		{
			configs.add(cfg);
			NetworkDcpStatus nds = statusList.getStatus(cfg.host, cfg.msgPort);
			nds.setDisplayName(cfg.name);
			nds.setPollingMinutes(cfg.pollingPeriod);
			log.info("Created status for {} host={}, port={}, period={}",
					 nds.getDisplayName(), nds.getHost(), nds.getPort(), nds.getPollingMinutes());
		}
	}

	public synchronized void killAll()
	{
		for(DrgsConnectCfg cfg : configs)
			cfg.setState(NetworkDcpState.Dead);
		configs.clear();
	}

	/**
	 * @return next config to poll, or null if none are ready now.
	 */
	public synchronized DrgsConnectCfg getNextConfigToPoll()
	{
		// Find the config that is the most past its ideal polling time.
		DrgsConnectCfg ret = null;
		long now = System.currentTimeMillis();
		for (DrgsConnectCfg cfg : configs)
		{
			if (cfg.pollingPeriod == 0
			 || cfg.getState() != NetworkDcpState.Waiting)
				continue;
			long thisDelta = now - cfg.getLastPollTime();
			if (thisDelta > cfg.pollingPeriod * 60000L
			 &&(ret == null || cfg.getLastPollTime() < ret.getLastPollTime()))
				ret = cfg;
		}
		if (ret != null)
		{
			ret.setState(NetworkDcpState.Running);
			ret.setLastPollTime(now);
		}
		return ret;
	}

	public synchronized void pollFinished(DrgsConnectCfg cfg)
	{
		// Won't hurt anything if this cfg was removed in the meantime,
		// It just is no longer in the list.
		cfg.setState(NetworkDcpState.Waiting);
	}

	public synchronized ArrayList<DrgsConnectCfg> getContinuousDcps()
	{
		ArrayList<DrgsConnectCfg> ret = new ArrayList<DrgsConnectCfg>();
		for (DrgsConnectCfg cfg : configs)
			if (cfg.pollingPeriod == 0)
				ret.add(cfg);
		return ret;
	}

	public synchronized int getNumPolled()
	{
		int ret = 0;
		for (DrgsConnectCfg cfg : configs)
			if (cfg.pollingPeriod > 0)
				ret++;
		return ret;
	}

	public DrgsConnectCfg getConfig(String host, int port)
	{
		for(DrgsConnectCfg cfg : this.configs)
			if (cfg.host.equalsIgnoreCase(host)
			 && cfg.msgPort == port)
				return cfg;
		return null;
	}

}