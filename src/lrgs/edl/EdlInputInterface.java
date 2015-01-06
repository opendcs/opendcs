/*
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lrgs.edl;

import ilex.util.Logger;
import lrgs.common.DcpMsg;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.lrgsmain.LrgsInputException;
import lrgs.lrgsmain.LrgsInputInterface;
import lrgs.lrgsmain.LrgsMain;

/**
 * Watches hot directories for incoming EDL (Electronic Data Logger) files and ingests
 * them into the LRGS archive.
 */
public class EdlInputInterface
	extends Thread
	implements LrgsInputInterface
{
	private LrgsMain lrgsMain;
	private int slot = -1;
	private int statusCode = 0;
	private String statusStr = "";
	private int dataSourceId = -1;
	private EdlMonitorThread monitorThread = null;
	private boolean _shutdown = false;
	private long lastConfigCheck = 0L;
	
	public EdlInputInterface(LrgsMain lrgsMain)
	{
		this.lrgsMain = lrgsMain;
		this.dataSourceId = 
			lrgsMain.getDbThread().getDataSourceId(LrgsInputInterface.DL_EDL_TYPESTR, "EDL");
	}
	
	@Override
	public void run()
	{
Logger.instance().info("EDL Ingest config thread starting.");

		// Watch for config changes.
		while(!_shutdown)
		{
Logger.instance().info("EDL Ingest config check last="+lastConfigCheck + ", loaded=" 
+ LrgsConfig.instance().getLastLoadTime());

			if (LrgsConfig.instance().getLastLoadTime() > lastConfigCheck)
			{
				lastConfigCheck = LrgsConfig.instance().getLastLoadTime();

				// On any config change, stop and restart (if enabled) the file monitor thread.
				if (LrgsConfig.instance().edlIngestEnable)
				{
					if (monitorThread != null)
					{
						enableLrgsInput(false);
						try { sleep(5000L); }
						catch (InterruptedException e) { e.printStackTrace(); }
					}
					enableLrgsInput(true);
				}
				else
					enableLrgsInput(false);
			}

			try { sleep(1000L); }
			catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
	public void saveMessage(DcpMsg dcpMsg)
	{
		lrgsMain.msgArchive.archiveMsg(dcpMsg, this);
	}
	
	@Override
	public int getType()
	{
		return LrgsInputInterface.DL_EDL;
	}

	@Override
	public void setSlot(int slot)
	{
		this.slot = slot;
	}

	@Override
	public int getSlot()
	{
		return slot;
	}

	@Override
	public String getInputName()
	{
		return "EDL-Input";
	}

	@Override
	public void initLrgsInput() throws LrgsInputException
	{
Logger.instance().info("EDL Ingest initializing.");
		// Start the thread that checks for config changes.
		start();
	}

	@Override
	public void shutdownLrgsInput()
	{
		enableLrgsInput(false);
		_shutdown = true;
	}

	@Override
	public void enableLrgsInput(boolean enabled)
	{
		if (enabled)
		{
			Logger.instance().info("EDL Ingest Enabled");
			monitorThread = new EdlMonitorThread(this);
			monitorThread.start();
			statusStr = "Enabled";
		}
		else
		{
			if (monitorThread != null)
			{
				Logger.instance().info("EDL Ingest Disabled");
				monitorThread.shutdown();
			}
			monitorThread = null;
			statusStr = "Disabled";
		}
	}

	@Override
	public boolean hasBER()
	{
		return false;
	}

	@Override
	public String getBER()
	{
		return null;
	}

	@Override
	public boolean hasSequenceNums()
	{
		return false;
	}

	@Override
	public int getStatusCode()
	{
		return statusCode;
	}

	@Override
	public String getStatus()
	{
		return statusStr;
	}

	@Override
	public int getDataSourceId()
	{
		return dataSourceId;
	}

	@Override
	public boolean getsAPRMessages()
	{
		return false;
	}

	@Override
	public String getGroup()
	{
		return null;
	}
}
