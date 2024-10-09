/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2010/12/09 17:36:18  mmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2008/02/10 20:17:33  mmaloney
*  dev
*
*  Revision 1.2  2008/02/01 15:20:40  cvs
*  modified files for internationalization
*
*  Revision 1.3  2004/12/21 14:46:05  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2004/04/02 15:50:46  mjmaloney
*  Implemented EU editing functions.
*
*  Revision 1.1  2004/02/03 15:19:54  mjmaloney
*  Working GUI prototype complete.
*
*/
package decodes.rledit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ResourceBundle;

import javax.swing.table.*;

import decodes.db.*;
import decodes.gui.*;


/**
This table model shows all of the defined Engineering Units in a DECODES
database.
*/
@SuppressWarnings("serial")
public class EUTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = RefListEditor.getLabels();
	private static String columnNames[] = null;

	private ArrayList<EngineeringUnit> eus;
	private int lastSortColumn;

	/**
	 * No args constructor for JBuilder.
	 */
	public EUTableModel()
	{
		columnNames = new String[4];
		columnNames[0] = labels.getString("EUTableModel.abbrev");
		columnNames[1] = labels.getString("EUDialog.fullName");
		columnNames[2] = labels.getString("EUDialog.family");
		columnNames[3] = labels.getString("EUDialog.measures");

		lastSortColumn = 0;
		rebuild();
	}

	/**
	 * Rebuild model from possibly-changed database.
	 */
	public void rebuild()
	{
		eus = new ArrayList<EngineeringUnit>();
		EngineeringUnitList eul = Database.getDb().engineeringUnitList;
		for(Iterator<EngineeringUnit> it = eul.iterator(); it.hasNext(); )
		{
			EngineeringUnit eu = (EngineeringUnit)it.next();
			eus.add(eu);
		}
		resort();
	}

	/** @return number of EUs (rows) */
	public int getRowCount()
	{
		return eus.size();
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
		EngineeringUnit eu = (EngineeringUnit)getRowObject(row);
		if (eu == null)
			return "";
		return getEuColumn(eu, column);
	}

	/**
	 * Columnizes an EU object.
	 * @param eu the EngineeringUnit
	 * @param the column to get
	 * @return String value for the table display
	 */
	public static String getEuColumn(EngineeringUnit eu, int column)
	{
		String r = null;
		switch(column)
		{
		case 0: r = eu.abbr; break;
		case 1: r = eu.getName(); break;
		case 2: r = eu.family; break;
		case 3: r = eu.measures; break;
		}
		return r == null ? "" : r;
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
		Collections.sort(eus, new EuComparator(column));
		fireTableDataChanged();
	}

	/**
	 * Return EngineeringUnit object at specified row.
	 * @param row the row
	 * @return EngineeringUnit object at specified row
	 */
	public Object getRowObject(int row)
	{
		if (row < 0 || row >= eus.size())
			return null;
		return eus.get(row);
	}
}


/**
Used to sort the table by a specified column. 
*/
class EuComparator implements Comparator<EngineeringUnit>
{
	int column;

	public EuComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(EngineeringUnit eu1, EngineeringUnit eu2)
	{
		if (eu1 == eu2)
			return 0;

		String s1 = EUTableModel.getEuColumn(eu1, column);
		String s2 = EUTableModel.getEuColumn(eu2, column);

		int r = s1.compareToIgnoreCase(s2);
		if (r == 0)
		{
			if (column != 0)
			{
				s1 = EUTableModel.getEuColumn(eu1, 0);
				s2 = EUTableModel.getEuColumn(eu2, 0);
				r = s1.compareToIgnoreCase(s2);
			}
		}
		return r;
	}

	public boolean equals(EngineeringUnit ob)
	{
		return false;
	}
}
