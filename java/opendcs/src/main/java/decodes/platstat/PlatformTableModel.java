/**
 * $Id: PlatformTableModel.java,v 1.2 2016/08/05 14:50:18 mmaloney Exp $
 * 
 * Open Source Software
 * 
 * $Log: PlatformTableModel.java,v $
 * Revision 1.2  2016/08/05 14:50:18  mmaloney
 * Station and Routing Status GUI updates.
 *
 * Revision 1.1  2016/07/20 15:40:12  mmaloney
 * First platstat impl GUI.
 *
 */
package decodes.platstat;

import ilex.util.Logger;
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

import decodes.db.Database;
import decodes.db.Platform;
import decodes.db.PlatformStatus;
import decodes.db.ScheduleEntry;
import decodes.gui.SortingListTableModel;
import decodes.sql.DbKey;

@SuppressWarnings("serial")
class PlatformTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String[] colnames = null;
	int [] widths = { 8, 20, 15, 12, 15, 15, 15 };
	private int sortColumn = 0;
	private ArrayList<PlatformStatus> beans = new ArrayList<PlatformStatus>();
	private PSColumnizer columnizer = new PSColumnizer();
	private PlatformMonitorFrame frame = null;
	
	public PlatformTableModel(PlatformMonitorFrame frame)
	{
		this.frame = frame;
		colnames = new String[]{ 
			PlatformMonitorFrame.procmonLabels.getString("platmon.site"), 
			PlatformMonitorFrame.procmonLabels.getString("platmon.designator"), 
			PlatformMonitorFrame.procmonLabels.getString("platmon.lastcontact"), 
			PlatformMonitorFrame.procmonLabels.getString("platmon.lastmsg"),
			PlatformMonitorFrame.procmonLabels.getString("platmon.lastqual"),
			PlatformMonitorFrame.procmonLabels.getString("platmon.lasterr"),
			PlatformMonitorFrame.procmonLabels.getString("platmon.routingspec") };
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

	
	@Override
	public int getRowCount()
	{
		return beans.size();
	}

	@Override
	public synchronized Object getValueAt(int row, int col)
	{
		return columnizer.getColumn(beans.get(row), col);
	}

	@Override
	public synchronized void sortByColumn(int column)
	{
		this.sortColumn = column;
		Collections.sort(beans, new PSComparator(sortColumn, columnizer));
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return beans.get(row);
	}
	
	/**
	 * Called periodically from Db Update Thread with a new schedule entry list.
	 * Merge the list into the list being displayed, and if any changes are made,
	 * fireTableDataChanged event.
	 * @param seList the new list from the database.
	 */
	public synchronized int merge(ArrayList<PlatformStatus> list)
	{
		int numChanges = 0;
		for(PlatformStatus bean : beans)
		{
			bean.setChecked(false);
			if (bean.getSiteName() == null)
			{
				Platform p = Database.getDb().platformList.getById(bean.getPlatformId());
				if (p != null)
				{
					bean.setSiteName(p.getSiteName(false));
					bean.setDesignator(p.getPlatformDesignator());
				}
				else
					bean.setSiteName("null");
			}
			
			if (!DbKey.isNull(bean.getLastScheduleEntryStatusId()))
			{
				
			}
		}
		
	  nextBean:
		for(int psIdx = 0; psIdx < list.size(); psIdx++)
		{
			PlatformStatus ps = list.get(psIdx);
			
			for(PlatformStatus bean : beans)
				if (bean.getPlatformId().equals(ps.getPlatformId()))
				{
					bean.setChecked(true);
					if (!bean.equals(ps))
					{
						numChanges++;
						list.set(psIdx, ps);
					}
					continue nextBean;
				}
			// Fell through means this is a new Sched Entry that I don't yet have.
			ps.setChecked(true);
			beans.add(ps);
			
			numChanges++;
		}
		// Any beans not checked are no longer in the database. Remove from list.
		for(Iterator<PlatformStatus> beanit = beans.iterator(); beanit.hasNext(); )
		{
			PlatformStatus bean = beanit.next();
			if (!bean.isChecked())
			{
				beanit.remove();
				numChanges++;
			}
		}
		return numChanges;
	}
	
	/**
	 * Call from swing thread
	 */
	public void updated()
	{
		fireTableDataChanged();
	}

	public ArrayList<PlatformStatus> getBeans()
	{
		return beans;
	}

	public int indexOf(PlatformStatus bean)
	{
		if (bean == null)
			return -1;
		for(int idx = 0; idx < beans.size(); idx++)
			if (beans.get(idx) == bean)
				return idx;
		return -1;
	}	
}

class PSColumnizer
{
	SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd HH:mm:ss");
	PSColumnizer()
	{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public String getColumn(PlatformStatus rsb, int col)
	{
		switch(col)
		{
		case 0: return rsb.getSiteName();
		case 1: return rsb.getDesignator() == null ? "" : rsb.getDesignator();
		case 2: return rsb.getLastContactTime() == null ? "" : sdf.format(rsb.getLastContactTime());
		case 3: return rsb.getLastMessageTime() == null ? "" : sdf.format(rsb.getLastMessageTime());
		case 4: return rsb.getLastFailureCodes() == null ? "" : rsb.getLastFailureCodes();
		case 5: return rsb.getLastErrorTime() == null ? "" : sdf.format(rsb.getLastErrorTime());
		case 6: return rsb.getLastRoutingSpecName() == null ? "" : rsb.getLastRoutingSpecName();
		default: return "";
		}
	}
}

class PSComparator implements Comparator<PlatformStatus>
{
	private int sortColumn = 0;
	PSColumnizer columnizer = null;
	
	PSComparator(int sortColumn, PSColumnizer columnizer)
	{
		this.sortColumn = sortColumn;
		this.columnizer = columnizer;
	}

	@Override
	public int compare(PlatformStatus rs1, PlatformStatus rs2)
	{
		int r = strcmp(columnizer.getColumn(rs1, sortColumn), columnizer.getColumn(rs2, sortColumn));
		if (r != 0)
			return r;
		// Sort column is equal for 2 records, use secondary sort sitename/designator, which must be unique.
		r = strcmp(columnizer.getColumn(rs1, 0), columnizer.getColumn(rs2, 0));
		if (r != 0)
			return r;
		return strcmp(columnizer.getColumn(rs1, 1), columnizer.getColumn(rs2, 1));
	}
	
	/**
	 * Do a case INsensitive compare, but always sort blanks to the end.
	 * @param s1
	 * @param s2
	 * @return
	 */
	private int strcmp(String s1, String s2)
	{
		if (s1.length() == 0)
		{
			if (s2.length() == 0)
				return 0;
			else
				return 1;
		}
		else if (s2.length() == 0)
			return -1;
		else
			return TextUtil.strCompareIgnoreCase(s1, s2);
	}
}