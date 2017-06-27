/**
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. No warranty is provided or implied 
 * other than specific contractual terms between COVE and the U.S. Government
 * 
 * Copyright 2017 U.S. Government.
 *
 * $Log$
 */
package decodes.eventmon;

import ilex.util.TextUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import opendcs.dai.DacqEventDAI;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;
import decodes.db.Platform;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.gui.SortingListTableModel;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.sql.SqlDatabaseIO;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesSettings;

@SuppressWarnings("serial")
public class DacqEventTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String[] colnames = null;
	int [] widths = { 10, 10, 6, 8, 8, 10, 48 };
	private int sortColumn = 0;
	private ArrayList<DacqEvent> events = new ArrayList<DacqEvent>();
	private EventMonitorFrame frame = null;
	private HashMap<DbKey, CompAppInfo> appMap = new HashMap<DbKey, CompAppInfo>();
	
	/** Maps Sched Entry Status ID to routing spec name */
	private HashMap<DbKey, String> ses2rsname = new HashMap<DbKey, String>();
	
	private ArrayList<CompAppInfo> allApps = null;
	
	public DacqEventTableModel(EventMonitorFrame frame)
	{
		this.frame = frame;
		colnames = new String[] 
		{
			frame.eventmonLabels.getString("process_col"),
			frame.eventmonLabels.getString("evttime_col") 
				+ "(" + DecodesSettings.instance().guiTimeZone + ")",
			frame.eventmonLabels.getString("severity_col"),
			frame.eventmonLabels.getString("rs_col"),
			frame.eventmonLabels.getString("platform_col"),
			frame.eventmonLabels.getString("msgtime_col")
				+ "(" + DecodesSettings.instance().guiTimeZone + ")",
			frame.eventmonLabels.getString("evttext_col")
		};
	}
	
	/**
	 * Reload event list and apply filters specified by user
	 * @param appName 
	 * @param since
	 * @param until
	 * @param minSeverity
	 * @param containing
	 */
	public void reload(String appName, Date since, Date until, int minSeverity, String containing)
	{
		SqlDatabaseIO sqldbio = (SqlDatabaseIO)decodes.db.Database.getDb().getDbIo();
		LoadingAppDAI loadingAppDAO = sqldbio.makeLoadingAppDAO();
		ScheduleEntryDAI schedDAO = sqldbio.makeScheduleEntryDAO();
		RoutingSpecList rslist = decodes.db.Database.getDb().routingSpecList;
		DacqEventDAI evtDAO = sqldbio.makeDacqEventDAO();
		
		try
		{
			allApps = loadingAppDAO.listComputationApps(false);
			appMap.clear();
			DbKey filterAppId = DbKey.NullKey;
			for(CompAppInfo app : allApps)
			{
				appMap.put(app.getAppId(), app);
				if (appName != null && app.getAppName().equalsIgnoreCase(appName))
					filterAppId = app.getAppId();
			}
			
			ses2rsname.clear();
			ArrayList<ScheduleEntry> schedEntries = schedDAO.listScheduleEntries(null);
			ArrayList<ScheduleEntryStatus> stati = schedDAO.readScheduleStatus(null);
			for(ScheduleEntryStatus ses : stati)
				for(ScheduleEntry se : schedEntries)
					if (ses.getScheduleEntryId().equals(se.getId()))
					{
						RoutingSpec rs = rslist.getById(se.getRoutingSpecId());
						if (rs != null)
							ses2rsname.put(ses.getId(), rs.getDisplayName());
						break;
					}
			
			// Read the events and apply filters
			events.clear();
			evtDAO.readEventsAfter(since, events);
			for(Iterator<DacqEvent> evtit = events.iterator(); evtit.hasNext(); )
			{
				DacqEvent evt = evtit.next();
				if ((!DbKey.isNull(filterAppId) && !filterAppId.equals(evt.getAppId()))
				 || (since != null && evt.getEventTime().before(since))
				 || (until != null && evt.getEventTime().after(until))
				 || minSeverity > evt.getEventPriority()
				 || (containing != null && 
				 		!evt.getEventText().toLowerCase().contains(containing.toLowerCase())))
				{
//System.out.println("Filtered out evt=" + evt.getEventTime() + ", since=" + since + ", until=" + until);
					evtit.remove();
				}
			}
			
		}
		catch (DbIoException ex)
		{
			System.err.println(ex.toString());
			ex.printStackTrace();
		}
		finally
		{
			evtDAO.close();
			schedDAO.close();
			loadingAppDAO.close();
		}
		sortByColumn(sortColumn);
	}
	
	@Override
	public int getColumnCount()
	{
		return colnames.length;
	}
	
	public String getColumnName(int col)
	{
		return colnames[col];
	}

	@Override
	public int getRowCount()
	{
		return events.size();
	}
	
	private String getAppName(DbKey appId)
	{
		CompAppInfo app = appMap.get(appId);
		return app == null ? "" : app.getAppName();
	}
	
	private String getRsName(DbKey sesId)
	{
		String ret = ses2rsname.get(sesId);
		return ret == null ? "" : ret;
	}
	
	private String getPlatName(DbKey platId)
	{
		if (DbKey.isNull(platId))
			return "";
		Platform plat = decodes.db.Database.getDb().platformList.getById(platId);
		return plat == null ? "" : plat.getDisplayName();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		return getColumnValue(getEvtAt(row), col);
	}
	
	public String getColumnValue(DacqEvent evt, int col)
	{
		switch(col)
		{
		case 0: return getAppName(evt.getAppId()) 
			+ (evt.getSubsystem() != null ? ("/" + evt.getSubsystem()) : "");
		case 1: return evt.getTimeStr();
		case 2: return evt.getPriorityStr();
		case 3: return getRsName(evt.getScheduleEntryStatusId());
		case 4: return getPlatName(evt.getPlatformId());
		case 5: return evt.getMsgTimeStr();
		case 6: return evt.getEventText();
		default: return "";
		}
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(events, new EvtComparator(sortColumn, this));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return events.get(row);
	}
	
	public DacqEvent getEvtAt(int row)
	{
		return (DacqEvent)getRowObject(row);
	}

	public ArrayList<CompAppInfo> getAllApps()
	{
		return allApps;
	}
}


class EvtComparator implements Comparator<DacqEvent>
{
	private int sortColumn = 0;
	private DacqEventTableModel model = null;
	
	EvtComparator(int sortColumn, DacqEventTableModel model)
	{
		this.sortColumn = sortColumn;
		this.model = model;
	}

	@Override
	public int compare(DacqEvent evt1, DacqEvent evt2)
	{
		return TextUtil.strCompareIgnoreCase(
			model.getColumnValue(evt1, sortColumn),
			model.getColumnValue(evt2, sortColumn));
	}
}