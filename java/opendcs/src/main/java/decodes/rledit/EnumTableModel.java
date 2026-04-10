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
package decodes.rledit;

import javax.swing.table.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.*;

import decodes.db.*;

/**
Table Model for a DECODES Enumeration. The table will show all of the
values defined in the database for a specific Enum.
*/
@SuppressWarnings("serial")
public class EnumTableModel extends AbstractTableModel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
		{
			return 0;
		}
		return currentEnum.values().size();
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
			return ev.getValue().equals(currentEnum.getDefault()) ? "*" : "";
		else if (column == 1)
			return ev.getValue();
		else if (column == 2)
			return ev.getDescription();
		else if (column == 3)
			return ev.getExecClassName();
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
		Vector<?> v = (Vector<EnumValue>)currentEnum.values();
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
	 * @param en DECODES enumeration to display
	 */
	public void setEnum(DbEnum en)
	{
		if (en == null)
		{
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
		Vector<EnumValue> v = (Vector<EnumValue>)currentEnum.values();
		if (row <= 0 || row >= v.size())
			return false;
		EnumValue obj = v.elementAt(row);
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
		Vector<EnumValue> v = (Vector<EnumValue>)currentEnum.values();
		if (row < 0 || row >= v.size()-1)
			return false;
		EnumValue obj = v.elementAt(row);
		v.removeElementAt(row);
		v.insertElementAt(obj, row+1);
		fireTableDataChanged();
		return true;
	}
}
