/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 *
 */
package decodes.platstat;

import ilex.util.Logger;

import opendcs.dai.PlatformStatusDAI;
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
				
				try
				{
					platformMonitor.getFrame().updateFromDb(psDAO.listPlatformStatus());
				}
				catch(DbIoException ex)
				{
					Logger.instance().warning("Error reading platstat info: " + ex);
				}
				finally
				{
					psDAO.close();
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
