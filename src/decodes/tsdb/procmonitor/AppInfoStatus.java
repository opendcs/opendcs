/**
 * $Id$
 * 
 * Open Source Software 
 * 
 * $Log$
 * Revision 1.8  2013/04/18 13:53:28  mmaloney
 * Event socket bug fix.
 *
 * Revision 1.7  2013/03/25 19:21:15  mmaloney
 * cleanup
 *
 * Revision 1.6  2013/03/25 18:14:44  mmaloney
 * dev
 *
 * Revision 1.5  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.4  2013/03/25 15:02:20  mmaloney
 * dev
 *
 * Revision 1.3  2013/03/23 18:20:04  mmaloney
 * dev
 *
 * Revision 1.2  2013/03/23 18:01:03  mmaloney
 * dev
 *
 * Revision 1.1  2013/03/21 18:27:40  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb.procmonitor;

import ilex.util.TextUtil;

import javax.swing.JCheckBox;

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TsdbCompLock;

/**
 * Aggregates process AppInfo and Lock Status.
 * Used by the process status monitor in the table model
 * @author mmaloney Mike Maloney, Cove Software, LLC.
 */
public class AppInfoStatus
{
	private CompAppInfo compAppInfo = null;
	
	private TsdbCompLock compLock = null;
	
	private Boolean retrieveEvents = new Boolean(false);
	
	private EventClient eventPollClient = null;
	private ProcessMonitorFrame frame = null;
	
	/** Constructed with immutable compAppInfo */
	public AppInfoStatus(CompAppInfo compAppInfo, ProcessMonitorFrame frame)
	{
		this.compAppInfo = compAppInfo;
		this.frame = frame;
	}

	/** @return the lock record or null if none exists */
	public TsdbCompLock getCompLock()
	{
		return compLock;
	}

	/** Set the lock record */
	public void setCompLock(TsdbCompLock compLock)
	{
		this.compLock = compLock;
	}

	/** @return the application info */
	public CompAppInfo getCompAppInfo()
	{
		return compAppInfo;
	}
	
	/** Convenience method to get App ID */
	public DbKey getAppId() { return compAppInfo.getAppId(); }

	public Boolean getRetrieveEvents()
	{
		return retrieveEvents;
	}

	public void setRetrieveEvents(Boolean retrieveEvents)
		throws ProcMonitorException
	{
		this.retrieveEvents = retrieveEvents;
		if (retrieveEvents)
		{
			stopEventsClient();
			int eventPort = -1; // placeholder. Thread will set port from property.
			eventPollClient = new EventClient(eventPort, this, frame);
			Thread t = new Thread(eventPollClient);
			t.start();
		}
		else if (eventPollClient != null)
			stopEventsClient();
	}
	
	public void stopEventsClient()
	{
		if (eventPollClient != null)
		{
			eventPollClient.shutdown();
			eventPollClient = null;
		}
	}
}
