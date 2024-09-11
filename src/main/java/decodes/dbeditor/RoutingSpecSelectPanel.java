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
import decodes.dbeditor.routing.RSListTableModel;
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
			tableModel = new RSListTableModel(Database.getDb());
			rslTable = new JTable(tableModel);
			rslTable.setAutoCreateRowSorter(true);
			rslTable.getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);

			rslTable.addMouseListener(new MouseAdapter()
			{
				@Override
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
		int modelRow = rslTable.convertRowIndexToModel(idx);
		return tableModel.getObjectAt(modelRow);
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

	public RSListTableModel getModel()
	{
		return this.tableModel;
	}

	public void deleteSelection()
	{
		int r = rslTable.getSelectedRow();
		if (r == -1)
			return;
		int modelRow = rslTable.convertRowIndexToModel(r);
		tableModel.deleteObjectAt(modelRow);
	}

	public void setSelection(String rsName)
	{
		for(int row=0; row < tableModel.getRowCount(); row++)
		{
			int modelRow = rslTable.convertRowIndexToModel(row);
			if (rsName.equals(tableModel.getObjectAt(modelRow).getName()))
			{
				rslTable.clearSelection();
				rslTable.setRowSelectionInterval(row, row);
				return;
			}
		}
	}
}
