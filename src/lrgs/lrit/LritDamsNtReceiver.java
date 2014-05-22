package lrgs.lrit;

import ilex.util.ByteUtil;
import ilex.util.Logger;
import decodes.util.PropertySpec;
import lrgs.archive.MsgArchive;
import lrgs.drgs.DrgsConnectCfg;
import lrgs.drgsrecv.DrgsRecvMsgThread;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsMain;

public class LritDamsNtReceiver
	extends DrgsRecvMsgThread
{
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
		debug("Configured: Enabled=" + _enabled
			+ ", host=" + lrgsCfg.lritHostName
			+ ", port=" + lrgsCfg.lritPort
			+ ", startPat=" + lrgsCfg.lritDamsNtStartPattern);
		lastConfigure = System.currentTimeMillis();
	}
	
	private void debug(String msg)
	{
		Logger.instance().info(module + " " + msg);
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
}
