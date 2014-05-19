/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.7  2013/04/17 15:35:38  mmaloney
 * If lock disappears, update display to "Not Running".
 *
 * Revision 1.6  2013/03/25 19:21:15  mmaloney
 * cleanup
 *
 * Revision 1.5  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.4  2013/03/23 18:20:04  mmaloney
 * dev
 *
 * Revision 1.3  2013/03/23 18:14:07  mmaloney
 * dev
 *
 * Revision 1.2  2013/03/23 18:01:03  mmaloney
 * dev
 *
 * Revision 1.1  2013/03/23 15:33:55  mmaloney
 * dev
 *
 */
package decodes.tsdb.procmonitor;

import ilex.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

import opendcs.dai.LoadingAppDAI;

import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbCompLock;

/**
 * Polls Time Series Database for process status and updates model.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class DbPollThread 
	extends Thread
{
	private ProcessMonitor processMonitor = null;
	private boolean _shutdown = false;
	public static final long LockPollInterval = 5000L;
	public static final long AppInfoPollInterval = 30000L;
	
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
		long lastLockPoll = System.currentTimeMillis();
		long lastAppInfoPoll = 0L;
		while(!_shutdown)
		{
			LoadingAppDAI loadingAppDAO = processMonitor.getTsdb().makeLoadingAppDAO();
			try
			{
				ProcStatTableModel model = processMonitor.getFrame().getModel();
				if (System.currentTimeMillis() - lastAppInfoPoll > AppInfoPollInterval)
				{
					ArrayList<CompAppInfo> apps = loadingAppDAO.listComputationApps(false);
					boolean changed = false;
					for(CompAppInfo app : apps)
					{
						// Apps to be displayed must have property monitor = true.
						boolean doMonitor = TextUtil.str2boolean(app.getProperty("monitor"));
						// Get this app from the currently-displayed model.
						AppInfoStatus appStatus = model.getAppById(app.getAppId());
						if (appStatus == null && doMonitor)
						{
							model.addApp(app);
							changed = true;
						}
						else // already in model.
						{
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
								if (!app.getAppName().equals(eapp.getAppName()))
								{
									eapp.setAppName(app.getAppName());
									changed = true;
								}
							}
						}
					}
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
							if (appStatus.getAppId().equals(lock.getAppId()))
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
				System.err.println("Error polling database for process status: " + ex);
				ex.printStackTrace();
			}
			finally
			{
				loadingAppDAO.close();
			}

			try { sleep(1000L); } catch(InterruptedException ex) {}
		}
	}
}
