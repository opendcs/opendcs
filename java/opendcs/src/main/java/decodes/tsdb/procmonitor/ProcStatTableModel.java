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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTableModel;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.TsdbCompLock;

@SuppressWarnings("serial")
class ProcStatTableModel extends AbstractTableModel implements SortingListTableModel
{
	String[] colnames =
		{ "App ID", "App Name", "App Type", "Host", "PID", "Heartbeat (UTC)", "Status", "Events?" };
	int [] widths =
		{ 8, 15, 15, 12, 8, 17, 17, 8 };
	private int sortColumn = 0;
	private ArrayList<AppInfoStatus> apps = new ArrayList<AppInfoStatus>();
	private AppColumnizer columnizer = new AppColumnizer();
	private ProcessMonitorFrame frame = null;

	public ProcStatTableModel(ProcessMonitorFrame frame)
	{
		this.frame = frame;
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

	public boolean isCellEditable(int row, int col)
	{
		return col == 7;
	}
	public void setValueAt(Object value, int row, int col)
	{
		if (col != 7)
			return;
		try { getAppAt(row).setRetrieveEvents((Boolean)value); }
		catch(ProcMonitorException ex)
		{
			// no logging, this exception is never actually thrown, 
			// deciding to review later.
			frame.showError(ex.getMessage());
			super.setValueAt(Boolean.FALSE, row, col);
		}
	}

	public Class getColumnClass(int col)
	{
		return col == 7 ? Boolean.class : String.class;
	}

	@Override
	public int getRowCount()
	{
		return apps.size();
	}

	@Override
	public Object getValueAt(int row, int col)
	{
		return columnizer.getColumnObject(getAppAt(row), col);
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(apps, new AppComparator(sortColumn, columnizer));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return apps.get(row);
	}

	public synchronized int getAppNameIndex(String appName)
	{
		for(int i=0; i<apps.size(); i++)
			if (TextUtil.strEqual(appName, apps.get(i).getCompAppInfo().getAppName()))
				return i;
		return -1;
	}

	public AppInfoStatus getAppAt(int row)
	{
		return (AppInfoStatus)getRowObject(row);
	}

	public synchronized AppInfoStatus getAppByName(String name)
	{
		for (AppInfoStatus ais : apps)
			if (ais.getCompAppInfo().getAppName().equalsIgnoreCase(name))
				return ais;
		return null;
	}


	public void addApp(CompAppInfo appInfo)
	{
		synchronized(this)
		{
			AppInfoStatus ais = new AppInfoStatus(appInfo, frame);
			apps.add(ais);
		}
		sortByColumn(sortColumn);
	}
	public void rmApp(CompAppInfo appInfo)
	{
		synchronized(this)
		{
			for(Iterator<AppInfoStatus> ait = apps.iterator(); ait.hasNext(); )
			{
				AppInfoStatus ais = ait.next();
				if (ais.getAppId().equals(appInfo.getAppId()))
				{
					ais.stopEventsClient();
					ait.remove();
					break;
				}
			}
		}
		sortByColumn(sortColumn);
	}

	public void clearChecked()
	{
		for(AppInfoStatus app : apps)
			app.setChecked(false);
	}

	/**
	 * Delete any records that are unchecked.
	 * @return true if any were deleted.
	 */
	public synchronized boolean deleteUnchecked()
	{
		boolean ret = false;
		for(Iterator<AppInfoStatus> ait = apps.iterator(); ait.hasNext(); )
		{
			AppInfoStatus ais = ait.next();
			if (!ais.isChecked())
			{
				ais.stopEventsClient();
				ait.remove();
				ret = true;
			}
		}
		sortByColumn(sortColumn);

		return ret;
	}

}

class AppColumnizer
{
	SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd-yyyy HH:mm:ss");
	AppColumnizer()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	public Object getColumnObject(AppInfoStatus app, int col)
	{
		TsdbCompLock lock = app.getCompLock();
		switch(col)
		{
		case 0:
			return app.getAppId() == null || app.getAppId().isNull() ? "N/A" : app.getAppId().toString();
		case 1: return app.getCompAppInfo().getAppName();
		case 2: return app.getCompAppInfo().getAppType();
		case 3: return lock != null ? lock.getHost() : "N/A";
		case 4: return lock != null ? ("" + lock.getPID()) : "N/A";
		case 5: return lock != null ? sdf.format(lock.getHeartbeat()) : "~";
		case 6: return lock != null ? app.getCompLock().getStatus() : "Not Running";
		case 7: return app.getRetrieveEvents();
		default: return "";
		}
	}
	public String getColumnString(AppInfoStatus app, int col)
	{
		Object obj = getColumnObject(app, col);
		if (obj instanceof String)
			return (String)obj;
		else
			return obj.toString();
	}
}

class AppComparator implements Comparator<AppInfoStatus>
{
	private int sortColumn = 0;
	AppColumnizer columnizer = null;

	AppComparator(int sortColumn, AppColumnizer columnizer)
	{
		this.sortColumn = sortColumn;
		this.columnizer = columnizer;
	}

	@Override
	public int compare(AppInfoStatus app1, AppInfoStatus app2)
	{
		if (sortColumn == 0)
			return app1.getAppId().compareTo(app2.getAppId());
		return TextUtil.strCompareIgnoreCase(
			columnizer.getColumnString(app1, sortColumn),
			columnizer.getColumnString(app2, sortColumn));
	}
}