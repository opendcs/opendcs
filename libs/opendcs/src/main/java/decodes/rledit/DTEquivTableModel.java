/*
*  $Id$
*/
package decodes.rledit;

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

import decodes.db.*;
import decodes.gui.*;
import ilex.util.*;

/**
Table model for data type equivalence entries.
*/
public class DTEquivTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String columnNames[];
	Vector dataTypes;
	int numColumns;
	int lastSortColumn = 0;

	/** Constructor. */
	public DTEquivTableModel()
	{
		//columnNames = new String[] { "SHEF-PE", "EPA-Code", "Hydstra-Code" };
		rebuild();
	}

	/**
	 * Called at initialization, or after the database has changed. Rebuilds
	 * the table model with a fresh read from the database.
	 */
	public void rebuild()
	{
		decodes.db.DbEnum dtEnum = 
			Database.getDb().getDbEnum(Constants.enum_DataTypeStd);
		numColumns = dtEnum.size();
		columnNames = new String[numColumns];
		int i=0;
		for(Iterator it = dtEnum.iterator(); it.hasNext(); )
		{
			EnumValue ev = (EnumValue)it.next();
			columnNames[i++] = ev.getValue();
		}

		dataTypes = new Vector();

	  nextDT:
		for(Iterator it1 = Database.getDb().dataTypeSet.iterator();
			it1.hasNext(); )
		{
			DataType dt = (DataType)it1.next();
			int dtColnum = getColNum(dt.getStandard());
			if (dtColnum == -1)
				continue;

			// Do I already have an equivalent to this DT in the vector?
			for(DataType dte = dt.equivRing; dte != null && dte != dt;
				dte = dte.equivRing)
			{
				int dteColnum = getColNum(dte.getStandard());
				if (dteColnum == -1)
					continue;
				for(Iterator dtit = dataTypes.iterator(); dtit.hasNext(); )
				{
					String[] ent = (String[])dtit.next();
					if (ent[dteColnum] != null 
					 && ent[dteColnum].equals(dte.getCode()))
					{
						ent[dtColnum] = dt.getCode();
						continue nextDT;
					}
				}
			}

			// Fell through equivalents. This is a new DT.
			if (dt.equivRing != null && dt.equivRing != dt)
			{
				String[] ent = new String[numColumns];
				ent[dtColnum] = dt.getCode();
				dataTypes.add(ent);
			}
		}

		resort();
	}

	/**
	 * Give a column name, return the number.
	 * @param colName the column name.
	 * @return column number
	 */
	public int getColNum(String colName)
	{
		for(int i=0; i<columnNames.length; i++)
			if (colName.equalsIgnoreCase(columnNames[i]))
				return i;
		return -1;
	}

	/** @return number of rows. */
	public int getRowCount()
	{
		return dataTypes.size();
	}

	/** @return number of columns. */
	public int getColumnCount() { return numColumns; }

	/**
	 * Return string at specified row/column.
	 * @param row the row
	 * @param column the column
	 * @return string
	 */
	public Object getValueAt(int row, int column)
	{
		String[] ent = (String[])getRowObject(row);
		if (ent == null)
			return "";
		return getDtColumn(ent, column);
	}

	public static String getDtColumn(String[] ent, int column)
	{
		return ent[column] == null ? "" : ent[column];
	}

	/**
	 * Return a column name.
	 * @param col the column number
	 * @return column name
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/**
	 * Resorts the table according to last selected column header.
	 */
	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	/**
	 * Sorts the table by values in the selected column.
	 * @param column the column to sort by
	 */
	public void sortByColumn(int column)
	{
		lastSortColumn = column;
		Collections.sort(dataTypes, new DtComparator(column));
		fireTableDataChanged();
	}

	/**
	 * Return an object representing the DTE at the specified row.
	 * @param row the row.
	 * @return an object representing the DTE at the specified row
	*/
	public Object getRowObject(int row)
	{
		if (row < 0 || row >= dataTypes.size())
			return null;
		return dataTypes.elementAt(row);
	}

	/**
	 * @return true if std/code exists in some row OTHER than the current Row.
	 */
	public boolean exists(String std, String code, int currentRow)
	{
		int colNum = getColNum(std);
		if (colNum == -1)
			return false; // shouldn't happen.

		for(int r = 0; r < dataTypes.size(); r++)
		{
			if (r == currentRow)
				continue;
			String[] ent = (String[])dataTypes.elementAt(r);
			if (ent[colNum] != null
			 && code.equalsIgnoreCase(ent[colNum]))
				return true;
		}
		return false;
	}
}


/**
Used for sorting by specified column.
*/
class DtComparator implements Comparator
{
	int column;

	public DtComparator(int column)
	{
		this.column = column;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		String[] sa1 = (String[])ob1;
		String[] sa2 = (String[])ob2;

		String s1 = sa1[column];
		String s2 = sa2[column];

		int r = TextUtil.strCompareIgnoreCase(s1, s2);
		if (r == 0 && sa1.length > 1)
		{
			int col2 = column == 0 ? 1 : 0;
			s1 = sa1[col2];
			s2 = sa2[col2];
			r = TextUtil.strCompareIgnoreCase(s1, s2);
		}
		return r;
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
