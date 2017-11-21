/**
 * $Id$
 * 
 * Open Source Software
 * 
 * $Log$
 * Revision 1.4  2017/11/20 19:26:52  mmaloney
 * Fix 2 bugs: Selecting RS Run was messing up the panel header. RS Runs in the middle panel were not sorted in descending last-modify-time order like they should have been.
 *
 * Revision 1.3  2016/08/05 14:53:36  mmaloney
 * Station and Routing Status GUI updates.
 *
 * Revision 1.2  2016/07/20 15:40:53  mmaloney
 * First routmon impl.
 *
 * Revision 1.1  2016/06/27 15:15:41  mmaloney
 * Initial checkin.
 *
 * Revision 1.4  2016/06/07 22:03:51  mmaloney
 * Numeric sort on app ID in proc monitor.
 *
 * Revision 1.3  2015/06/04 21:37:40  mmaloney
 * Added control buttons to process monitor GUI.
 *
 * Revision 1.2  2015/05/14 13:52:19  mmaloney
 * RC08 prep
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.9  2013/03/25 19:21:15  mmaloney
 * cleanup
 *
 * Revision 1.8  2013/03/25 18:14:44  mmaloney
 * dev
 *
 * Revision 1.7  2013/03/25 17:50:54  mmaloney
 * dev
 *
 * Revision 1.6  2013/03/25 17:13:11  mmaloney
 * dev
 *
 * Revision 1.5  2013/03/25 16:58:38  mmaloney
 * dev
 *
 * Revision 1.4  2013/03/25 15:02:20  mmaloney
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
class RoutingTableModel extends AbstractTableModel
	implements SortingListTableModel
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
//	public void setValueAt(Object value, int row, int col)
//	{
//		if (col != 0)
//			return;
//		if (row >= apps.size())
//			return;
//		
//		try { ap.setRetrieveEvents((Boolean)value); }
//		catch(ProcMonitorException ex)
//		{
//			frame.showError(ex.getMessage());
//			super.setValueAt(Boolean.FALSE, row, col);
//		}
//	}
	
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
		
//System.out.println("Retrieved " + seStatuses.size() + " statuses.");
	nextStatus:
		for(ScheduleEntryStatus ses : seStatuses)
		{
//System.out.println("...Status for SE " + ses.getScheduleEntryName() 
//+ " starting at " + ses.getRunStart() + " with LMT=" + ses.getLastModified());
			for(RSBean bean : beans)
			{
//System.out.println("......Checking against bean for '" + bean.getScheduleEntry().getName() + "'");
				if (ses.getScheduleEntryName().equalsIgnoreCase(bean.getScheduleEntry().getName()))
				{
//System.out.println(".........Match, bean already has " + bean.getRunHistory().size() + " runs.");
					for(int idx = 0; idx < bean.getRunHistory().size(); idx++)
					{
						ScheduleEntryStatus beanSes = bean.getRunHistory().get(idx);
//System.out.println("............run["+ idx + "] start=" + beanSes.getRunStart() 
//+ ", LMT=" + beanSes.getLastModified());
						if (ses.getRunStart().equals(beanSes.getRunStart()))
						{
							if (ses.getLastModified().after(beanSes.getLastModified()))
							{
//System.out.println("...............Doing Update");
								bean.getRunHistory().set(idx, ses);
								bean.setModified(true);
								numChanges++;
							}
							continue nextStatus;
						}
					}
//System.out.println("...............Fell through, adding");
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
		
//System.out.println("RTM.merge: after doing merge...");
//for(RSBean bean : beans)
//{
//System.out.println("bean " + bean.getRsName() + " has " + bean.getRunHistory().size() + " runs.");
//}
		
		return numChanges;
	}
	
//	/**
//	 * Call from swing thread
//	 */
//	public void updated()
//	{
//System.out.println("RTM.updated");
//		fireTableDataChanged();
//	}

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
		case 0: return new Boolean(rsb.isEnabled());
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
		if (obj instanceof String)
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