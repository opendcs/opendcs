/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2001/02/28 01:53:46  mike
*  Track changes better.
*
*  Revision 1.3  2001/02/23 03:04:01  mike
*  Working version.
*
*  Revision 1.2  2001/02/21 14:49:29  mike
*  dev
*
*  Revision 1.1  2001/02/21 13:19:29  mike
*  Created nleditor
*
*/
package lrgs.nledit;

import java.io.IOException;
import java.io.File;
import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;
import lrgs.common.NetworkList;
import lrgs.common.NetworkListItem;
import lrgs.common.DcpAddress;

public class NetworkListTableModel extends AbstractTableModel
{
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
System.out.println("Improperly formatted DCP address '"+value+"' - Enter 8 hex digits.");
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

