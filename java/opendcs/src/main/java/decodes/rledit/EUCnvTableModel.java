/*
*  $Id$
*
*  $Log$
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
*  Revision 1.2  2004/04/09 18:59:43  mjmaloney
*  dev.
*
*  Revision 1.1  2004/02/03 15:19:54  mjmaloney
*  Working GUI prototype complete.
*
*/
package decodes.rledit;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;

import decodes.db.*;
import decodes.gui.*;

/**
This is the table model that displays the database-defined EU conversions.
*/
public class EUCnvTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = RefListEditor.getLabels();
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	/** Constant column names */
	static String columnNames[] = null;

	/** Vector of converters. */
	Vector cnv;

	/** Remember the column last used for sorting. */
	int lastSortColumn;

	/**
	 * No args constructor for JBuilder.
	 */
	public EUCnvTableModel()
	{
		columnNames = new String[9];
		columnNames[0] = labels.getString("EUCnvTableModel.fromColumn"); 
		columnNames[1] = labels.getString("EUCnvTableModel.toColumn");
		columnNames[2] = genericLabels.getString("algorithm");
		columnNames[3] = "A";
		columnNames[4] = "B";
		columnNames[5] = "C";
		columnNames[6] = "D";
		columnNames[7] = "E";
		columnNames[8] = "F";
		
		lastSortColumn = 0;
		rebuild();
	}

	/**
	 * Called at init, and after DB is changed, re-build the model.
	 */
	public void rebuild()
	{
		cnv = new Vector();
		UnitConverterSet ucs = Database.getDb().unitConverterSet;
		for(Iterator it = ucs.iteratorDb(); it.hasNext(); )
			cnv.add(it.next());
		resort();
	}

	/** @return number of unit converters (rows) */
	public int getRowCount()
	{
		return cnv.size();
	}

	/** @return constant number of columns */
	public int getColumnCount() { return columnNames.length; }

	/**
	 * Return String value at specified row column.
	 * @param row the row
	 * @param column the column
	 * @return String value at specified row column
	 */
	public Object getValueAt(int row, int column)
	{
		UnitConverterDb uc = (UnitConverterDb)getRowObject(row);
		if (uc == null)
			return "";
		return getUcColumn(uc, column);
	}

	/**
	 * Columnizes a converter.
	 * @param uc the converter.
	 * param column the column number.
	 * @return String to display in specified column
	 */
	public static String getUcColumn(UnitConverterDb uc, int column)
	{
		//{ "From", "To", "Algorithm", "A", "B", "C", "D", "E", "F" };

		String r = null;
		switch(column)
		{
		case 0: r = uc.fromAbbr; break;
		case 1: r = uc.toAbbr; break;
		case 2: r = uc.algorithm; break;
		default:
			r = uc.getCoeffString(column-3); break;
		}
		return r == null ? "" : r;
	}

	/**
	 * Return column name.
	 * @param col the column
	 * @return column name.
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/**
	 * Resort by last selected column.
	 */
	public void resort()
	{
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
	}

	/**
	 * Sort by selected column.
	 * @param column the column
	 */
	public void sortByColumn(int column)
	{
		lastSortColumn = column;
		Collections.sort(cnv, new UcComparator(column));
		fireTableDataChanged();
	}

	/**
	 * Return unit converter at specified row.
	 * @param row the row
	 * @return unit converter at specified row
	 */
	public Object getRowObject(int row)
	{
		if (row < 0 || row >= cnv.size())
			return null;
		return cnv.elementAt(row);
	}
}

/**
Used to sort the table by a specified column.
*/
class UcComparator implements Comparator
{
	int column;

	public UcComparator(int column)
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
		UnitConverterDb uc1 = (UnitConverterDb)ob1;
		UnitConverterDb uc2 = (UnitConverterDb)ob2;

		String s1 = EUCnvTableModel.getUcColumn(uc1, column);
		String s2 = EUCnvTableModel.getUcColumn(uc2, column);

		int r = s1.compareToIgnoreCase(s2);
		if (r == 0 && column != 0)
		{
			s1 = EUCnvTableModel.getUcColumn(uc1, 0);
			s2 = EUCnvTableModel.getUcColumn(uc2, 0);
			r = s1.compareToIgnoreCase(s2);
			if (r == 0 && column != 1)
			{
				s1 = EUCnvTableModel.getUcColumn(uc1, 1);
				s2 = EUCnvTableModel.getUcColumn(uc2, 1);
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
