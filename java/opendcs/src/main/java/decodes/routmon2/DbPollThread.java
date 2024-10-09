/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.2  2016/07/20 15:40:53  mmaloney
 * First routmon impl.
 *
 * Revision 1.1  2016/06/27 15:15:40  mmaloney
 * Initial checkin.
 *
 * Revision 1.3  2015/06/04 21:37:39  mmaloney
 * Added control buttons to process monitor GUI.
 *
 * Revision 1.2  2015/05/14 13:52:20  mmaloney
 * RC08 prep
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
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
package decodes.routmon2;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.util.ArrayList;
import java.util.List;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
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
	private RoutingMonitor routingMonitor = null;
	private boolean _shutdown = false;
	public static final long updateInterval = 5000L;
	private long lastUpdate;
	RsRunModel rsRunModel = null;

	public DbPollThread(RoutingMonitor routmon)
	{
		this.routingMonitor = routmon;
		rsRunModel = routingMonitor.getFrame().getRsRunModel();
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
				ScheduleEntryDAI seDAO = decodes.db.Database.getDb().getDbIo().makeScheduleEntryDAO();
				try
				{

					ArrayList<ScheduleEntry> seList = seDAO.listScheduleEntries(null);
					// readScheduleStatus(null) returns list of ALL SES's in the database.
					ArrayList<ScheduleEntryStatus> statusList = seDAO.readScheduleStatus(null);

					routingMonitor.getFrame().updateFromDb(seList, statusList);
					
//					RSBean selectedRS = routingMonitor.getFrame().getSelectedRS();
					
					
//					if (selectedRS != null)
//					{
//						// Poll the database for any new 'runs' of the selected RS.
//						ArrayList<ScheduleEntryStatus> statusList = 
//							seDAO.readScheduleStatus(selectedRS.getScheduleEntry());
//
//						selectedRS.setRunHistory(statusList);
//						routingMonitor.getFrame().updateRunHistory();
//					}
				}
				catch(DbIoException ex)
				{
					Logger.instance().warning("Error reading schedule info: " + ex);
				}
				finally
				{
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
