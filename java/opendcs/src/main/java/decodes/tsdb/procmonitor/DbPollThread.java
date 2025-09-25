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
package decodes.tsdb.procmonitor;

import ilex.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.LoadingAppDAI;

import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbCompLock;

/**
 * Polls Time Series Database for process status and updates model.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class DbPollThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private ProcessMonitor processMonitor = null;
	private boolean _shutdown = false;
	public static final long LockPollInterval = 5000L;
	public static final long AppInfoPollInterval = 30000L;
	private long lastLockPoll;
	private long lastAppInfoPoll;

	public DbPollThread(ProcessMonitor processMonitor)
	{
		this.processMonitor = processMonitor;
	}
	
	public void shutdown()
	{
		_shutdown = true;
	}
	
	public void run()
	{
		lastLockPoll = System.currentTimeMillis();
		lastAppInfoPoll = 0L;
		while(!_shutdown)
		{
			LoadingAppDAI loadingAppDAO = //processMonitor.getTsdb().makeLoadingAppDAO();
				decodes.db.Database.getDb().getDbIo().makeLoadingAppDAO();
			try
			{
				ProcStatTableModel model = processMonitor.getFrame().getModel();
				if (System.currentTimeMillis() - lastAppInfoPoll > AppInfoPollInterval)
				{
					ArrayList<CompAppInfo> apps = loadingAppDAO.listComputationApps(false);
					boolean changed = false;
					model.clearChecked();
					for(CompAppInfo app : apps)
					{
						// Apps to be displayed must have property monitor = true.
						boolean doMonitor = true;
						String monitor = app.getProperty("monitor");
						if (monitor != null && monitor.trim().length() > 0)
							doMonitor = TextUtil.str2boolean(monitor);
						
						// Get this app from the currently-displayed model.
						AppInfoStatus appStatus = model.getAppByName(app.getAppName());
						if (appStatus == null)
						{
							if (doMonitor)
							{
								model.addApp(app);
								changed = true;
							}
						}
						else // already in model.
						{
							appStatus.setChecked(true);
							if (!doMonitor) // Was the monitor property was turned off?
							{
								model.rmApp(app);
								changed = true;
							}
							else
							{
								CompAppInfo eapp = appStatus.getCompAppInfo();
								if (!app.getAppName().equals(eapp.getAppName()))
								{
									eapp.setAppName(app.getAppName());
									changed = true;
								}
							}
						}
					}
					if (model.deleteUnchecked())
						changed = true;
					if (changed)
						model.fireTableDataChanged();
					lastAppInfoPoll = System.currentTimeMillis();
				}

				if (System.currentTimeMillis() - lastLockPoll > LockPollInterval)
				{
					List<TsdbCompLock> locks = loadingAppDAO.getAllCompProcLocks();
					for(int r = 0; r < model.getRowCount(); r++)
					{
						AppInfoStatus appStatus = model.getAppAt(r);
						boolean found = false;
						for (TsdbCompLock lock : locks)
						{
							if ((!lock.getAppId().isNull() && appStatus.getAppId().equals(lock.getAppId()))
							 || (lock.getAppId().isNull() && 
								 lock.getAppName().equalsIgnoreCase(appStatus.getCompAppInfo().getAppName())))
							{
								appStatus.setCompLock(lock);
								found = true;
								break;
							}
						}
						if (!found)
							appStatus.setCompLock(null);
					}
					model.fireTableDataChanged();
					lastLockPoll = System.currentTimeMillis();
				}
			}
			catch (DbIoException ex)
			{
				log.atError().setCause(ex).log("Error polling database for process status.");
			}
			finally
			{
				loadingAppDAO.close();
			}

			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}
	
	public void pollNow()
	{
		lastLockPoll = lastAppInfoPoll = 0L;
	}
}
