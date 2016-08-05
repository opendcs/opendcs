/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.1  2016/07/20 15:40:12  mmaloney
 * First platstat impl GUI.
 *
 *
 */
package decodes.platstat;

import java.util.ArrayList;
import java.util.HashMap;

import ilex.util.Logger;
import opendcs.dai.PlatformStatusDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

/**
 * Polls Time Series Database for process status and updates model.
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class DbPollThread 
	extends Thread
{
	private PlatformMonitor platformMonitor = null;
	private boolean _shutdown = false;
	public static final long updateInterval = 5000L;
	private long lastUpdate;

	public DbPollThread(PlatformMonitor routmon)
	{
		this.platformMonitor = routmon;
	}
	
	public void shutdown()
	{
		_shutdown = true;
	}
	
	public void run()
	{
		lastUpdate = 0L;
		while(!_shutdown)
		{
			if (System.currentTimeMillis() - lastUpdate > updateInterval)
			{
				PlatformStatusDAI psDAO = decodes.db.Database.getDb().getDbIo().makePlatformStatusDAO();
				ScheduleEntryDAI seDAO = decodes.db.Database.getDb().getDbIo().makeScheduleEntryDAO();
				
				try
				{
					ArrayList<ScheduleEntry> seList = seDAO.listScheduleEntries(null);
					HashMap<DbKey, String> ses2rsName = new HashMap<DbKey, String>();
					for(ScheduleEntry se : seList)
					{
						ScheduleEntryStatus ses = seDAO.getLastScheduleStatusFor(se);
						if (ses != null)
						{
							se.setLastScheduleEntryStatusId(ses.getKey());
							ses2rsName.put(ses.getKey(), se.getRoutingSpecName() +
								(se.getName().toLowerCase().endsWith("manual") ? " (manual)" : ""));
						}
					}
					
					ArrayList<PlatformStatus> psList = psDAO.listPlatformStatus();
					for(PlatformStatus ps : psList)
						ps.setLastRoutingSpecName(ses2rsName.get(ps.getLastScheduleEntryStatusId()));
					platformMonitor.getFrame().updateFromDb(psList);
				}
				catch(DbIoException ex)
				{
					Logger.instance().warning("Error reading platstat info: " + ex);
				}
				finally
				{
					psDAO.close();
					seDAO.close();
				}
				lastUpdate = System.currentTimeMillis();
			}

			try { sleep(500L); } catch(InterruptedException ex) {}
		}
	}

	public void updateNow()
	{
		lastUpdate = 0L;
	}
}
