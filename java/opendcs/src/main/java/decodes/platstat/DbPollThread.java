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
package decodes.platstat;

import java.util.ArrayList;
import java.util.HashMap;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
public class DbPollThread extends Thread
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
					log.atWarn().setCause(ex).log("Error reading platstat info.");
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
