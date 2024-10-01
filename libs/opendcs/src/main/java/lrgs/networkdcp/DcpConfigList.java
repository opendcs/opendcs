/**
 * 
 */
package lrgs.networkdcp;

import ilex.util.Logger;

import java.util.ArrayList;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgs.DrgsInputSettings;

/**
 * Manages the list/queue of DCPs to poll.
 */
public class DcpConfigList
{
	private ArrayList<DrgsConnectCfg> configs
		= new ArrayList<DrgsConnectCfg>();
	
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
		Logger.instance().info(NetworkDcpRecv.module + 
			" Processing new Network DCP Configuration");
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
					Logger.instance().info(NetworkDcpRecv.module + 
						" Already have equal config for host="
						+ cfg.host + ", port=" + cfg.msgPort 
						+ ", poll=" + cfg.pollingPeriod); 
					toDelete.remove(oldCfg);
				}
				else // different!
				{
					Logger.instance().info(NetworkDcpRecv.module + 
						" Config Changed for host="
						+ cfg.host + ", port=" + cfg.msgPort 
						+ ", poll=" + cfg.pollingPeriod); 
					// This will kill any thread servicing this connection.
					oldCfg.setState(NetworkDcpState.Dead);
					cfg.setState(NetworkDcpState.Waiting);
					toAdd.add(cfg);
				}
			}
			else // no matching old config
			{
				Logger.instance().info(NetworkDcpRecv.module + 
					" New Config loaded for host="
					+ cfg.host + ", port=" + cfg.msgPort 
					+ ", poll=" + cfg.pollingPeriod); 
				toAdd.add(cfg);
			}
		}
		try { Thread.sleep(5000L); } // Allow threads time to die.
		catch(InterruptedException ex) {}
		for(DrgsConnectCfg cfg : toDelete)
		{
			configs.remove(cfg);
			statusList.remove(cfg.host, cfg.msgPort);
			Logger.instance().info("Removed Network DCP with host="
				+ cfg.host + ", port=" + cfg.msgPort + ", poll=" + cfg.pollingPeriod); 
		}
		for(DrgsConnectCfg cfg : toAdd)
		{
			configs.add(cfg);
			NetworkDcpStatus nds = statusList.getStatus(cfg.host, cfg.msgPort);
			nds.setDisplayName(cfg.name);
			nds.setPollingMinutes(cfg.pollingPeriod);
			Logger.instance().info(NetworkDcpRecv.module +
				" Created status for " 
				+ nds.getDisplayName() + " host=" + nds.getHost()
				+ ", port=" + nds.getPort() 
				+ ", period=" + nds.getPollingMinutes());
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
