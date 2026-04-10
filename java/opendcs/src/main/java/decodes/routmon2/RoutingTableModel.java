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
package decodes.routmon2;

import ilex.util.TextUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.gui.SortingListTableModel;

@SuppressWarnings("serial")
class RoutingTableModel extends AbstractTableModel implements SortingListTableModel
{
	String[] colnames = null;
	int [] widths = { 8, 20, 15, 12, 15, 15, 15 };
	private int sortColumn = 0;
	private ArrayList<RSBean> beans = new ArrayList<RSBean>();
	private RSColumnizer columnizer = new RSColumnizer();
	private RoutingMonitorFrame frame = null;
	
	public RoutingTableModel(RoutingMonitorFrame frame)
	{
		this.frame = frame;
		colnames = new String[]{ 
			RoutingMonitorFrame.procmonLabels.getString("enabled"), 
			RoutingMonitorFrame.genericLabels.getString("name"), 
			RoutingMonitorFrame.procmonLabels.getString("process"), 
			RoutingMonitorFrame.procmonLabels.getString("interval"),
			RoutingMonitorFrame.procmonLabels.getString("lastAct"),
			RoutingMonitorFrame.procmonLabels.getString("lastMsg"),
			RoutingMonitorFrame.procmonLabels.getString("lastStats") };
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
		return col == 6;
	}
	
	public Class getColumnClass(int col)
	{
		return col == 0 ? Boolean.class : String.class;
	}
	
	@Override
	public int getRowCount()
	{
		return beans.size();
	}

	@Override
	public synchronized Object getValueAt(int row, int col)
	{
		return columnizer.getColumnObject(getRSAt(row), col);
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(beans, new RsComparator(sortColumn, columnizer));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return beans.get(row);
	}
	
	public RSBean getRSAt(int row)
	{
		return (RSBean)getRowObject(row);
	}
	
	
	public void addBean(RSBean bean)
	{
		synchronized(this)
		{
			beans.add(bean);
		}
		sortByColumn(sortColumn);
	}

	/**
	 * Called periodically from Db Update Thread with a new schedule entry list.
	 * Merge the list into the list being displayed, and if any changes are made,
	 * fireTableDataChanged event.
	 * @param seList the new list from the database.
	 */
	public synchronized int merge(ArrayList<ScheduleEntry> seList,
		ArrayList<ScheduleEntryStatus> seStatuses)
	{
		int numChanges = 0;
		for(RSBean bean : beans)
		{
			bean.setChecked(false);
			bean.setModified(false);
		}

	nextSchedEntry:
		for(int seIdx = 0; seIdx < seList.size(); seIdx++)
		{
			ScheduleEntry se = seList.get(seIdx);
			for(RSBean bean : beans)
				if (bean.getScheduleEntry().getName().equals(se.getName()))
				{
					bean.setChecked(true);
					if (!bean.getScheduleEntry().equals(se))
					{
						numChanges++;
						bean.setScheduleEntry(se);
						bean.setModified(true);
					}
					continue nextSchedEntry;
				}
			// Fell through means this is a new Sched Entry that I don't yet have.
			beans.add(new RSBean(se));
			numChanges++;
		}
		
	nextStatus:
		for(ScheduleEntryStatus ses : seStatuses)
		{
			for(RSBean bean : beans)
			{
				if (ses.getScheduleEntryName().equalsIgnoreCase(bean.getScheduleEntry().getName()))
				{
					for(int idx = 0; idx < bean.getRunHistory().size(); idx++)
					{
						ScheduleEntryStatus beanSes = bean.getRunHistory().get(idx);
						if (ses.getRunStart().equals(beanSes.getRunStart()))
						{
							if (ses.getLastModified().after(beanSes.getLastModified()))
							{
								bean.getRunHistory().set(idx, ses);
								bean.setModified(true);
								numChanges++;
							}
							continue nextStatus;
						}
					}
					// Fell through. This is a new run
					int insertPoint = 0;
					for(; insertPoint < bean.getRunHistory().size(); insertPoint++)
						if (ses.getLastModified().after(bean.getRunHistory().get(insertPoint).getLastModified()))
							break;
					
					bean.getRunHistory().add(insertPoint, ses);
					continue nextStatus;
				}
			}
		}
		
		// Any beans not checked are no longer in the database. Remove from list.
		for(Iterator<RSBean> beanit = beans.iterator(); beanit.hasNext(); )
		{
			RSBean bean = beanit.next();
			if (!bean.isChecked())
			{
				beanit.remove();
				numChanges++;
			}
		}
		
		if (numChanges > 0)
		{
			SwingUtilities.invokeLater(
				new Runnable()
				{
					@Override
					public void run()
					{
						fireTableDataChanged();
					}
				});

		}
		
		return numChanges;
	}
	
	public ArrayList<RSBean> getBeans()
	{
		return beans;
	}

	public int indexOf(RSBean selectedRS)
	{
		if (selectedRS == null)
			return -1;
		for(int idx = 0; idx < beans.size(); idx++)
			if (beans.get(idx) == selectedRS)
				return idx;
		return -1;
	}	
}

class RSColumnizer
{
	SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd HH:mm:ss");
	RSColumnizer()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public Object getColumnObject(RSBean rsb, int col)
	{
		switch(col)
		{
		case 0: return Boolean.valueOf(rsb.isEnabled());
		case 1: return rsb.getScheduleEntry().getName().toLowerCase().endsWith("manual")
					? (rsb.getRsName() + " (manual)") : rsb.getRsName();
		case 2: return rsb.getAppName();
		case 3: return rsb.getInterval();
		case 4: 
			{
				Date t = rsb.getLastActivityTime();
				return t != null ? sdf.format(t) : "-";
			}
		case 5: 
			{
				Date t = rsb.getLastMsgTime();
				return t != null ? sdf.format(t) : "-";
			}
		case 6: return rsb.getLastStats();
		default: return "";
		}
	}
	public String getColumnString(RSBean rsb, int col)
	{
		Object obj = getColumnObject(rsb, col);
		if (obj == null)
			return "";
		else if (obj instanceof String)
			return (String)obj;
		else
			return obj.toString();
	}
}

class RsComparator implements Comparator<RSBean>
{
	private int sortColumn = 0;
	RSColumnizer columnizer = null;
	
	RsComparator(int sortColumn, RSColumnizer columnizer)
	{
		this.sortColumn = sortColumn;
		this.columnizer = columnizer;
	}

	@Override
	public int compare(RSBean rs1, RSBean rs2)
	{
		return TextUtil.strCompareIgnoreCase(
			columnizer.getColumnString(rs1, sortColumn),
			columnizer.getColumnString(rs2, sortColumn));
	}
}