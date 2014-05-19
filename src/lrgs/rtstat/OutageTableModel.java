/*
*  $Id$
*/
package lrgs.rtstat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import lrgs.common.DcpAddress;
import lrgs.db.Outage;
import lrgs.db.LrgsConstants;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;

/**
This table model shows all of the defined Engineering Units in a DECODES
database.
*/
public class OutageTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static String columnNames[] = null;
	public static final int columnWidths[] = 
		{ 6, 18, 21, 21, 10, 24 };

	private ArrayList<Outage> data;
	private int lastSortColumn;
	private static SimpleDateFormat sdf = 
		new SimpleDateFormat("yyyy/MM/dd-HH:mm");
	static { sdf.setTimeZone(TimeZone.getTimeZone("UTC")); }

	/**
	 * No args constructor for JBuilder.
	 */
	public OutageTableModel()
	{
		columnNames = new String[6];
		columnNames[0] = labels.getString("OutageTableModel.IDColumn");
		columnNames[1] = labels.getString("OutageTableModel.typeColumn");
		columnNames[2] = labels.getString("OutageTableModel.startColumn");
		columnNames[3] = labels.getString("OutageTableModel.endColumn");
		columnNames[4] = labels.getString("OutageTableModel.statusColumn");
		columnNames[5] = labels.getString("OutageTableModel.infoColumn");
		
		lastSortColumn = 0;
		data = new ArrayList<Outage>();
	}

	public void setData(ArrayList<Outage> data)
	{
		this.data = data;
		resort();
		fireTableDataChanged();
	}

	/** @return number of EUs (rows) */
	public int getRowCount()
	{
		return data.size();
	}

	/** @return constant number of columns. */
	public int getColumnCount() { return columnNames.length; }

	/** 
	 * Return String at specified row/column.
	 * @param row the row
	 * @param column the column
	 * @return String at specified row/column.
	 */
	public Object getValueAt(int row, int column)
	{
		Outage ob = data.get(row);
		if (ob == null)
			return "";
		return getObColumn(ob, column);
	}

	/**
	 * Columnizes an object.
	 * @param ob the object
	 * @param the column to get
	 * @return String value for the table display
	 */
	public static String getObColumn(Outage ob, int column)
	{
		String r = null;
		char typeCode = ob.getOutageType();
		switch(column)
		{
		case 0: return "" + ob.getOutageId();
		case 1: return LrgsConstants.outageTypeName(typeCode);
		case 2: return sdf.format(ob.getBeginTime());
		case 3:
			{
				Date d = ob.getEndTime();
				return d == null ? "" : sdf.format(d);
			}
		case 4: return LrgsConstants.outageStatusName(ob.getStatusCode());
		case 5: 
			if (typeCode == LrgsConstants.systemOutageType
			 || typeCode == LrgsConstants.realTimeOutageType)
				return "";
			else if (typeCode == LrgsConstants.domsatGapOutageType)
				return "Seq: " + ob.getBeginSeq() + "..." + ob.getEndSeq();
			else if (typeCode == LrgsConstants.damsntOutageType)
				return "source=" + ob.getDataSourceName();
			else if (typeCode == LrgsConstants.missingDCPMsgOutageType)
				return "addr="+(new DcpAddress(ob.getDcpAddress())).toString();
		default: return "";
		}
	}

	/**
	 * Return column name.
	 * @param col the column number
	 * @return column name
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/** Resort table by last selected column. */
	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	/**
	 * Sort the table by the specified column.
	 * @param column the column
	 */
	public void sortByColumn(int column)
	{
		lastSortColumn = column;
		Collections.sort(data, new DataComparator(column));
		fireTableDataChanged();
	}

	/**
	 * Return EngineeringUnit object at specified row.
	 * @param row the row
	 * @return EngineeringUnit object at specified row
	 */
	public Object getRowObject(int row)
	{
		if (row < 0 || row >= data.size())
			return null;
		return data.get(row);
	}
}


/**
Used to sort the table by a specified column. 
*/
class DataComparator implements Comparator
{
	int column;

	public DataComparator(int column)
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
		Outage out1 = (Outage)ob1;
		Outage out2 = (Outage)ob2;

		String s1 = OutageTableModel.getObColumn(out1, column);
		String s2 = OutageTableModel.getObColumn(out2, column);

		int r = s1.compareToIgnoreCase(s2);
		if (r == 0)
		{
			if (column != 0)
			{
				s1 = OutageTableModel.getObColumn(out1, 0);
				s2 = OutageTableModel.getObColumn(out2, 0);
				r = s1.compareToIgnoreCase(s2);
			}
		}
		return r;
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
