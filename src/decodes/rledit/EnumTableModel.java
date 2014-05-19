/*
*  $Id$
*
*  $Log$
*  Revision 1.3  2009/01/22 00:31:33  mjmaloney
*  DB Caching improvements to make msgaccess start quicker.
*  Remove the need to cache the entire database.
*
*  Revision 1.2  2008/12/29 21:55:42  dlittle
*  Enum value sort number fixed
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2008/02/10 20:17:33  mmaloney
*  dev
*
*  Revision 1.2  2008/02/01 15:20:40  cvs
*  modified files for internationalization
*
*  Revision 1.6  2005/03/15 16:52:01  mjmaloney
*  Rename Enum to DbEnum for Java 5 compatibility
*
*  Revision 1.5  2005/03/15 16:11:28  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.4  2004/12/21 14:46:05  mjmaloney
*  Added javadocs
*
*  Revision 1.3  2004/04/01 22:37:23  mjmaloney
*  Implemented controls for enumerations.
*
*  Revision 1.2  2004/03/31 17:02:56  mjmaloney
*  Implemented decodes db interface and enum list table.
*
*  Revision 1.1  2004/02/02 22:12:57  mjmaloney
*  dev.
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
Table Model for a DECODES Enumeration. The table will show all of the
values defined in the database for a specific Enum.
*/
public class EnumTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static ResourceBundle labels = RefListEditor.getLabels();
	
	/** The Enum currently being shown. */
	decodes.db.DbEnum currentEnum;

	/** Column headers. */
	static String columnNames[] = null;

	/** Constructor. */
	public EnumTableModel()
	{
		columnNames = new String[4];
		columnNames[0] = labels.getString("EnumTableModel.DfltColumn");
		columnNames[1] = labels.getString("EnumTableModel.nameColumn");
		columnNames[2] = labels.getString("EnumTableModel.descriptionColumn");
		columnNames[3] = labels.getString("EnumTableModel.javaClassColumn");

		currentEnum = null;
	}

	/** @return number of enum values (rows). */
	public int getRowCount()
	{
		if (currentEnum == null)
			return 0;
		Vector v = (Vector)currentEnum.values();
		return v.size();
	}

	/** @return number of enum columns (constant). */
	public int getColumnCount() { return columnNames.length; }
	
	public void fireTableDataChanged()
	{
		super.fireTableDataChanged();
		for(int pos=1;pos<=getRowCount();pos++)
		{
			EnumValue myvalue = getEnumValueAt(pos-1);
			if(myvalue!=null)
			{
				//System.out.println("Enum sort value changed: "+myvalue.getSortNumber()+" "+pos);
				myvalue.setSortNumber(pos);
			}
		}
	}

	/**
	 * Return String vale at specified row/column.
	 * @param row the row
	 * @param column the column
	 * @return String value
	 */
	public Object getValueAt(int row, int column)
	{
		EnumValue ev = getEnumValueAt(row);
		if (ev == null)
			return "";
		if (column == 0)
			return ev.value.equals(currentEnum.getDefault()) ? "*" : "";
		else if (column == 1)
			return ev.value;
		else if (column == 2)
			return ev.description;
		else if (column == 3)
			return ev.execClassName;
		else return "";
	}

	/**
	 * Return EnumValue object at specified row.
	 * @param row the row
	 * @return EnumValue object at specified row
	 */
	public EnumValue getEnumValueAt(int row)
	{
		if (currentEnum == null)
			return null;
		Vector v = (Vector)currentEnum.values();
		if (row >= v.size())
			return null;
		return (EnumValue)v.elementAt(row);
	}

	/**
	 * Return column name.
	 * @param col column number
	 * @return column name
	 */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/**
	 * Sets the enum being shown in the table. This will totally change the
	 * contents of the table.
	 * @param enumName name of the DECODES enumeration to display
	 */
	public void setEnum(String enumName)
	{
		decodes.db.DbEnum en = Database.getDb().getDbEnum(enumName);
		if (en == null)
		{
			System.err.println("Cannot find enum '" + enumName + "'");
			return;
		}
		currentEnum = en;
		fireTableDataChanged();
	}

	/**
	 * Does nothing -- we don't sort enum values, the user controls the
	 * order directly.
	 * @param column ignored.
	 */
	public void sortByColumn(int column)
	{
	}

	/**
	 * Return EnumValue object at specified row.
	 * @param row the row
	 * @return EnumValue object at specified row
	 */
	public Object getRowObject(int row)
	{
		return getEnumValueAt(row);
	}

	/**
	 * Move the specified row up one.
	 * @param row the row
	 */
	public boolean moveUp(int row)
	{
		Vector v = (Vector)currentEnum.values();
		if (row <= 0 || row >= v.size())
			return false;
		Object obj = v.elementAt(row);
		v.removeElementAt(row);
		v.insertElementAt(obj, row-1);
		fireTableDataChanged();
		return true;
	}

	/**
	 * Move the specified row down one.
	 * @param row the row
	 */
	public boolean moveDown(int row)
	{
		Vector v = (Vector)currentEnum.values();
		if (row < 0 || row >= v.size()-1)
			return false;
		Object obj = v.elementAt(row);
		v.removeElementAt(row);
		v.insertElementAt(obj, row+1);
		fireTableDataChanged();
		return true;
	}
}
