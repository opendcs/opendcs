package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Comparator;
import java.util.Collections;
import java.util.Vector;
import java.util.ResourceBundle;

import decodes.gui.*;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpecList;
import decodes.db.RoutingSpec;
import decodes.db.Constants;

@SuppressWarnings("serial")
public class RoutingSpecSelectPanel 
	extends JPanel 
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	private JTable rslTable;
	private RSListTableModel tableModel;
	private RoutingSpecListPanel parentPanel = null;
	private RoutingSpecSelectDialog parentDialog = null;

	/** Constructor. */
	public RoutingSpecSelectPanel()
	{
		try
		{
			tableModel = new RSListTableModel();
			rslTable = new SortingListTable(tableModel, 
				new int[] { 20, 30, 30, 25 });
			rslTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);

			rslTable.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						if (parentDialog != null)
							parentDialog.openPressed();
						else if (parentPanel != null)
							parentPanel.openPressed();
					}
				}
			});
			jbInit();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	void setParentPanel(RoutingSpecListPanel parentPanel)
	{
		this.parentPanel = parentPanel;
	}
	void setParentDialog(RoutingSpecSelectDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane();
		this.add(scrollPane, BorderLayout.CENTER);
		scrollPane.getViewport().add(rslTable, null);
	}

	/** @return the currently selected RoutingSpec. */
	public RoutingSpec getSelection()
	{
		int idx = rslTable.getSelectedRow();
		if (idx == -1)
			return null;
		return tableModel.getObjectAt(idx);
	}
	
	public void clearSelection()
	{
		rslTable.clearSelection();
	}

	public void refill()
	{
		tableModel.refill();
	}

	public void addRoutingSpec(RoutingSpec rs)
	{
		tableModel.addObject(rs);
	}

	public void deleteSelection()
	{
		int r = rslTable.getSelectedRow();
		if (r == -1)
			return;
		tableModel.deleteObjectAt(r);
	}

	public void setSelection(String rsName)
	{
		for(int row=0; row < tableModel.getRowCount(); row++)
			if (rsName.equals(tableModel.getObjectAt(row).getName()))
			{
				rslTable.clearSelection();
				rslTable.setRowSelectionInterval(row, row);
				return;
			}
	}
	
	/** Resorts the list by currently selected column. */
	public void resort()
	{
		this.tableModel.resort();
	}

}

@SuppressWarnings("serial")
class RSListTableModel extends AbstractTableModel implements
	SortingListTableModel
{
	static String columnNames[] =
	{
		RoutingSpecSelectPanel.genericLabels.getString("name"),
		RoutingSpecSelectPanel.dbeditLabels
			.getString("RoutingSpecListPanel.dataSource"),
		RoutingSpecSelectPanel.dbeditLabels
			.getString("RoutingSpecListPanel.consumer"),
		RoutingSpecSelectPanel.genericLabels.getString("lastMod") };
	private int lastSortColumn = -1;
	private RoutingSpecList theList;

	public RSListTableModel()
	{
		super();
		theList = Database.getDb().routingSpecList;
		refill();
		this.sortByColumn(0);
	}

	public void deleteObjectAt(int r)
	{
		theList.remove(getObjectAt(r));
		fireTableDataChanged();
	}

	void refill()
	{
		theList.clear();
		try
		{
			Database.getDb().getDbIo().readRoutingSpecList(theList);
		}
		catch (DatabaseException ex)
		{
			System.err.println("Cannot read routing spec list: " + ex);
			ex.printStackTrace(System.err);
		}
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		return theList.size();
	}

	public int getColumnCount()
	{
		return columnNames.length;
	}

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	RoutingSpec getObjectAt(int r)
	{
		return (RoutingSpec) getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		if (r >= 0 && r < getRowCount())
			return theList.getList().elementAt(r);
		else
			return null;
	}

	void addObject(RoutingSpec ob)
	{
		theList.add(ob);
		fireTableDataChanged();
	}

	void deleteObject(RoutingSpec ob)
	{
		theList.remove(ob);

		try
		{
			Database.getDb().getDbIo().deleteRoutingSpec(ob);
		}
		catch (DatabaseException e)
		{
			DbEditorFrame.instance().showError(
				RoutingSpecSelectPanel.dbeditLabels
					.getString("RoutingSpecListPanel.errorDelete")
					+ e.toString());
		}
		fireTableDataChanged();
	}

	public Object getValueAt(int r, int c)
	{
		RoutingSpec ob = getObjectAt(r);
		if (ob == null)
			return "";
		else
			return getRsColumn(ob, c);
	}

	public static String getRsColumn(RoutingSpec ob, int c)
	{
		switch (c)
		{
		case 0:
			return ob.getName();
		case 1:
			return ob.dataSource == null ? "" : (ob.dataSource.getName() + " ("
				+ ob.dataSource.dataSourceType + ")");
		case 2:
			return ob.consumerType + "(" + 
				(ob.consumerArg == null ? "" : ob.consumerArg) + ")";
		case 3:
			if (ob.lastModifyTime == null)
				return "";
			else
				return Constants.defaultDateFormat.format(ob.lastModifyTime);
		default:
			return "";
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
		Vector<RoutingSpec> v = theList.getList();
		Collections.sort(v, new RSComparator(c));
		fireTableDataChanged();
	}

	/*
	 * void replace(NetworkList oldOb, NetworkList newOb) {
	 * theList.remove(oldOb); theList.add(newOb); if (lastSortColumn != -1)
	 * sortByColumn(lastSortColumn); else fireTableDataChanged(); }
	 */
}

class RSComparator 
	implements Comparator<RoutingSpec>
{
	int column;

	public RSComparator(int column)
	{
		this.column = column;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(RoutingSpec ob1, RoutingSpec ob2)
	{
		if (ob1 == ob2)
			return 0;

		String s1 = RSListTableModel.getRsColumn(ob1, column);
		String s2 = RSListTableModel.getRsColumn(ob2, column);

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
