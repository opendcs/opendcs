/*
*  $Id$
*  
*  Open source Software
*  Author: Mike Maloney, Cove Software, LLC
*  
*  $Log$
*  Revision 1.1  2013/02/28 16:44:26  mmaloney
*  New SearchCriteriaEditPanel implementation.
*
*/
package lrgs.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ilex.util.IDateFormat;
import ilex.util.Logger;

import decodes.gui.*;
import decodes.util.NwsXrefEntry;
import decodes.util.Pdt;
import decodes.util.PdtEntry;

/**
Displays a sorting-list of Platform objects in the database.
*/
public class PdtSelectPanel extends JPanel
{
	PdtSelectTableModel model;
	SortingListTable pdtTable;
	JDialog parentDialog = null;
	JPanel parentPanel = null;

	/** Constructor. */
	public PdtSelectPanel(JDialog dlg)
	{
		parentDialog = dlg;
		model = new PdtSelectTableModel();
		pdtTable = new SortingListTable(model,
			PdtSelectTableModel.columnWidths);

		setMultipleSelection(true);
		try
		{
			guiInit();
			model.reload();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setMultipleSelection(boolean ok)
	{
		pdtTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
	private void guiInit() throws Exception
	{
		this.setPreferredSize(new Dimension(980,500));
		JScrollPane scrollPane = new JScrollPane();
		this.setLayout(new BorderLayout());
		this.add(scrollPane, BorderLayout.CENTER);
		scrollPane.getViewport().add(pdtTable, null);
	}

	/**
	 * @return all currently-selected platforms, or empty array if none.
	 */
	public PdtEntry[] getSelectedEntries()
	{
		int idx[] = pdtTable.getSelectedRows();
		PdtEntry ret[] = new PdtEntry[idx.length];
		for(int i=0; i<idx.length; i++)
		{
			int modelRow = pdtTable.convertRowIndexToModel(idx[i]);
			ret[i] = model.getEntryAt(modelRow);
		}
		return ret;
	}

	/** Resorts the list by the current column selection. */
	public void reSort()
	{
		model.reSort();
	}

	public void setParentPanel(JPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}
	
	public void reload()
	{
		model.reload();
	}
}

@SuppressWarnings("serial")
class PdtSelectTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	PdtSelectColumnizer columnizer;

	private static String columnNames[] =
	{
		"DCP Address", "Agency", "S Chn", "R Chn", "1st Xmit",
		"State", "NWS ID", "USGS ID", "Description"
	};
	public static int columnWidths[] =
	{
		10, 8, 6, 6, 9, 7, 9, 12, 30
	};
	private int sortColumn = 0;
	private ArrayList<PdtEntry> entries = new ArrayList<PdtEntry>();
	
	public PdtSelectTableModel()
	{
		super();
		columnizer = new PdtSelectColumnizer();
	}
	
	public void reload()
	{
		entries.clear();
		Pdt pdt = Pdt.instance();
		Logger.instance().info("PdtSelectPanel.reload(): pdt has " 
			+ pdt.getEntries().size() + " entries in it.");
		for(PdtEntry pe : pdt.getEntries())
			entries.add(pe);
		sortByColumn(sortColumn);
		this.fireTableDataChanged();
	}
	
	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public int getRowCount()
	{
		return entries.size();
	}

	public Object getValueAt(int r, int c)
	{
		return columnizer.getColumn(getEntryAt(r), c);
	}

	PdtEntry getEntryAt(int r)
	{
		return (PdtEntry)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < entries.size())
			return entries.get(r);
		else return null;
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(entries, new PdtColumnComparator(c, columnizer));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}
}

/**
 Helper class to retrieve platform fields by column number. Used for
 displaying values in the table and for sorting.
*/
class PdtSelectColumnizer
{
	String getColumn(PdtEntry p, int c)
	{
		switch(c)
		{
			case 0: return p.dcpAddress.toString();
			case 1: return p.agency;
			case 2: return p.st_channel > 0 ? "" + p.st_channel : "";
			case 3: return p.rd_channel > 0 ? "" + p.rd_channel : "";
			case 4: return p.st_channel > 0 ?
				IDateFormat.printSecondOfDay(p.st_first_xmit_sod, true) : "";
			case 5: return p.state_abbr;
			case 6: 
			{
				NwsXrefEntry nxe = p.getNwsXrefEntry();
				return nxe != null ? nxe.getNwsId() : "";
			}
			case 7: 
			{
				NwsXrefEntry nxe = p.getNwsXrefEntry();
				return nxe != null ? nxe.getUsgsNum() : "";
			}
			case 8: 
			{
				NwsXrefEntry nxe = p.getNwsXrefEntry();
				return nxe != null ? nxe.getLocationName()
					: p.description != null ? p.description : "";
			}
			default:
				return "";
		}
	}
}

class PdtColumnComparator implements Comparator
{
	int col;
	PdtSelectColumnizer columnizer;

	PdtColumnComparator(int col, PdtSelectColumnizer columnizer)
	{
		this.col = col;
		this.columnizer = columnizer;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		PdtEntry p1 = (PdtEntry)ob1;
		PdtEntry p2 = (PdtEntry)ob2;
		String s1 = columnizer.getColumn(p1, col);
		String s2 = columnizer.getColumn(p2, col);
		// Sort blank strings to the end.
		if (s1.length() == 0 ^ s2.length() == 0)
			return s1.length() == 0 ? 1 : -1;
		return s1.compareToIgnoreCase(s2);
	}
}
