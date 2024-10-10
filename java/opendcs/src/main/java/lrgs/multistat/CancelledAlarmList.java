package lrgs.multistat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Vector;
import java.text.SimpleDateFormat;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTableModel;

public class CancelledAlarmList extends AbstractTableModel
	implements SortingListTableModel
{
	Vector cancelledAlarms;
	static String columnNames[] =
	{ "Prio", "Host/Module", "Date/Time", "Cancelled On", "By", "Count", 
	  "Message"
	};
	private int lastSortColumn = -1;
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd-HH:mm");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }

	public CancelledAlarmList()
	{
		super();
		cancelledAlarms = new Vector();
	}

	public void add(Alarm alarm)
	{
		alarm.cancelledBy = MultiStatConfig.instance().operator;
		alarm.cancelledOn = new Date();
		cancelledAlarms.add(alarm);
		notifyGui();
	}

	private void notifyGui()
	{
		SwingUtilities.invokeLater(
			new Runnable()
		{
			public void run()
			{
				resort();
				fireTableDataChanged();
			}
		});
	}

	public synchronized void purgeOld()
	{
		long cutoff = System.currentTimeMillis() - (4L * 24L * 3600L * 1000L);
		for(Iterator it = cancelledAlarms.iterator(); it.hasNext(); )
		{
			Alarm a = (Alarm)it.next();
			if (a.cancelledOn.getTime() < cutoff)
				it.remove();
		}
		notifyGui();
	}

	public int getRowCount()
	{
		return cancelledAlarms.size();
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	Alarm getObjectAt(int r)
	{
		return (Alarm)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return cancelledAlarms.elementAt(r);
		else return null;
	}

	public Object getValueAt(int r, int c)
	{
		Alarm ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getAlarmColumn(ob, c);
	}

	public static String getAlarmColumn(Alarm ob, int c)
	{
		switch(c)
		{
		case 0: return ob.priority + (ob.isInstantaneous ? "-" : "");
		case 1: return ob.source + "/" + ob.module + "/" + ob.alarmNum;
		case 2: return ob.date;
		case 3: return sdf.format(ob.cancelledOn);
		case 4: return ob.cancelledBy;
		case 5: return "" + ob.assertionCount;
		case 6: return ob.text;
		default: return "";
		}
	}

	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(cancelledAlarms, new AlarmComparator(c));
		fireTableDataChanged();
	}
}

class AlarmComparator implements Comparator
{
	int column;

	public AlarmComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		Alarm a1 = (Alarm)ob1;
		Alarm a2 = (Alarm)ob2;

		String s1 = CancelledAlarmList.getAlarmColumn(a1, column);
		String s2 = CancelledAlarmList.getAlarmColumn(a2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}


