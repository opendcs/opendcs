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
package lrgs.nledit;

import java.io.IOException;
import java.io.File;
import java.util.Vector;
import javax.swing.table.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.common.NetworkList;
import lrgs.common.NetworkListItem;

public class NetworkListTableModel extends AbstractTableModel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private NetworkList networkList;
	boolean modified;

	static String columnNames[] =
	{ "NESS-ID", "Name", "Comment" };

	public NetworkListTableModel()
	{
		this.networkList = new NetworkList();
		modified = false;
	}

	public NetworkList getNetworkList() { return networkList; }

	public int getRowCount()
	{
		return networkList.size();
	}

	public int getColumnCount() { return columnNames.length; }

	public Object getValueAt(int row, int column)
	{
		if (row >= getRowCount())
			return null;
		NetworkListItem nli = (NetworkListItem)networkList.elementAt(row);
		if (column == 0)
			return nli.addr;
		else if (column == 1)
			return nli.name;
		else if (column == 2)
			return nli.description;
		else return "?";
	}

	public void setValueAt(Object value, int row, int col)
	{
		if (row >= getRowCount())
			return;
		NetworkListItem nli = (NetworkListItem)networkList.elementAt(row);
		if (col == 0)
		{
			try { nli.addr.fromString((String)value); }
			catch (NumberFormatException nfe)
			{
				log.atError().setCause(nfe).log("Improperly formatted DCP address '{}' - Enter 8 hex digits.", value);
				return;
			}
			modified = true;
		}
		else if (col == 1)
		{
			String s = (String)value;
			if (nli.name.equals(s))
				return;
			nli.name = s;
			modified = true;
		}
		else if (col == 2)
		{
			String s = (String)value;
			if (nli.description.equals(s))
				return;
			nli.description = s;
			modified = true;
		}
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c) { return true; }

	public boolean isModified() { return modified; }

	public void mergeFile(File input)
		throws IOException
	{
		networkList.parseFile(input);
		modified = true;
		this.fireTableDataChanged();
	}

	public void loadFile(File input)
		throws IOException
	{
		networkList.clear();
		networkList.parseFile(input);
		modified = false;
		fireTableDataChanged();
	}

	public void saveFile(File output)
		throws IOException
	{
		networkList.saveFile(output);
		modified = false;
	}

	public void clear()
	{
		networkList.clear();
		modified = false;
		fireTableDataChanged();
	}

	public void sortByAddress()
	{
		networkList.sortByAddress();
		fireTableDataChanged();
		modified = true;
	}

	public void sortByName()
	{
		networkList.sortByName();
		fireTableDataChanged();
		modified = true;
	}
	public void sortByDescription()
	{
		networkList.sortByDescription();
		fireTableDataChanged();
		modified = true;
	}

	public NetworkListItem getItemAt(int row)
	{
		return (NetworkListItem)networkList.elementAt(row);
	}

	public void	deleteItemAt(int row)
	{
		networkList.removeElementAt(row);
		fireTableDataChanged();
		modified = true;
	}

	public void addItemsAt(int row, Vector items)
	{
		networkList.addAll(row, items);
		fireTableDataChanged();
		modified = true;
	}

	public void addItems(Vector items)
	{
		networkList.addAll(items);
		fireTableDataChanged();
		modified = true;
	}

	public void insertBefore(int row)
	{
		networkList.insertElementAt(new NetworkListItem("000000000::"), row);
		fireTableDataChanged();
		modified = true;
	}

	public void insertAfter(int row)
	{
		networkList.insertElementAt(new NetworkListItem("000000000::"), row+1);
		fireTableDataChanged();
		modified = true;
	}
}