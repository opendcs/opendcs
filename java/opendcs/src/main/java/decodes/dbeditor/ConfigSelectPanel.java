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
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;
import java.util.ResourceBundle;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.TextUtil;
import decodes.db.*;
import decodes.gui.*;

/**
Panel for selecting a Platform Configuration. Used inside ConfigSelectDialog.
*/
public class ConfigSelectPanel extends JPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	ConfigSelectTableModel model;
	SortingListTable configTable;
	ConfigSelectDialog parentDialog = null;

	/** Constructor. */
	public ConfigSelectPanel()
	{
	    model = new ConfigSelectTableModel(this);
		configTable = new SortingListTable(model, null);
		configTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);

		configTable.addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent e)
				{
					if (e.getClickCount() == 2)
					{
						JPanel p = (JPanel)ConfigSelectPanel.this.getParent();
						if (p instanceof ConfigsListPanel)
							((ConfigsListPanel)p).openPressed();
						else if (parentDialog != null)
						{
							parentDialog.selectButtonPressed();
						}
					}
				}
			});
		try
		{
		    jbInit();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(configTable, null);
	}

	/**
	 * @return the currently-selected object, or null if none is selected.
	 */
	public PlatformConfig getSelection()
	{
		int r = configTable.getSelectedRow();
		if (r == -1)
			return null;
		//Get the correct row from the table model
		int modelrow = configTable.convertRowIndexToModel(r);
		ConfigSelectTableModel tablemodel = (ConfigSelectTableModel)configTable.getModel();
		return (PlatformConfig)tablemodel.getRowObject(modelrow);
	}

	/** Tells the panel to refresh itself from the database config list. */
	public void refill()
	{
		model.refill();
		invalidate();
		repaint();
	}

	/**
	  Adds a configuration to the model.
	  @param ob the new configuration.
	*/
	public void addConfig(PlatformConfig ob)
	{
		model.add(ob);
		invalidate();
		repaint();
	}

	/**
	  Deletes the currently-selected configuration.
	*/
	public void deleteSelection()
	{
		int r = configTable.getSelectedRow();
		if (r == -1)
			return;
		//Get the correct row from the table model
		int modelrow = configTable.convertRowIndexToModel(r);
		ConfigSelectTableModel tablemodel = (ConfigSelectTableModel)configTable.getModel();
		tablemodel.deleteAt(modelrow);
	}

	/**
	  Replaces a configuration with another. Called when the Save button
	  is pressed in the config edit panel. Recall that the panel edits a
	  copy of the config. Thus when it is saved, the new (modified) copy
	  replaces the old one.
	  @param oldPc the old platform config
	  @param newPc the new platform config
	*/
	public void replace(PlatformConfig oldPc, PlatformConfig newPc)
	{
		model.replace(oldPc, newPc);
	}

	/**
	  Sets the current selection.
	  @param name the name of the selected configuration.
	*/
	public void setSelection(String name)
	{
		int n = model.getRowCount();
		for(int i=0; i<n; i++)
		{
			PlatformConfig pc = model.getConfigAt(i);
			if (pc.configName.equalsIgnoreCase(name))
			{
				configTable.setRowSelectionInterval(i, i);
				Rectangle rect = configTable.getCellRect(i, 0, true);
				configTable.scrollRectToVisible(rect);
				break;
			}
		}
	}

	/** Clears any selections which have been made. */
	public void clearSelection()
	{
		configTable.clearSelection();
	}

	public void setParentDialog(ConfigSelectDialog parentDialog)
	{
		this.parentDialog = parentDialog;
	}
}

class ConfigSelectTableModel extends javax.swing.table.AbstractTableModel
	implements SortingListTableModel
{
	private String colNames[] =
	{
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col0"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col1"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col2"),
		ConfigSelectPanel.dbeditLabels.getString("ConfigSelectPanel.col3")
	};
	private ConfigSelectPanel panel;
	private Vector vec;
	private int lastSortColumn;

	public ConfigSelectTableModel(ConfigSelectPanel ssp)
	{
		super();
		lastSortColumn = -1;
		this.panel = ssp;
		refill();
		this.sortByColumn(0);
	}

	void refill()
	{
		try
		{
			Database.getDb().platformConfigList.read();
			Database.getDb().platformConfigList.countPlatformsUsing();
		} catch (DatabaseException dbe ) { };
		vec = new Vector(Database.getDb().platformConfigList.values());
	}

	void add(PlatformConfig ob)
	{
		for(int i=0; i<vec.size(); i++)
		{
			PlatformConfig pc = (PlatformConfig)vec.elementAt(i);
			if (pc.configName.equals(ob.configName))
				return;
		}
		vec.add(ob);
		fireTableDataChanged();
	}

	void replace(DatabaseObject oldOb, DatabaseObject newOb)
	{
		vec.remove(oldOb);
		vec.add(newOb);
		if (lastSortColumn != -1)
			sortByColumn(lastSortColumn);
		else
			fireTableDataChanged();
	}

	void deleteAt(int index)
	{
		PlatformConfig ob = (PlatformConfig)vec.elementAt(index);
		vec.remove(index);
		try { Database.getDb().getDbIo().deleteConfig(ob); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(e.toString());
		}
		Database.getDb().platformConfigList.remove(ob);
		fireTableDataChanged();
	}

	public int getColumnCount()
	{
		int r = colNames.length;
		return r;
	}

	public String getColumnName(int c)
	{
		return colNames[c];
	}

	public boolean isCellEditable(int r, int c) { return false; }

	public int getRowCount()
	{
		return vec.size();
	}

	public Object getValueAt(int r, int c)
	{
		return ConfigColumnizer.getColumn(getConfigAt(r), c);
	}

	PlatformConfig getConfigAt(int r)
	{
		return (PlatformConfig)getRowObject(r);
	}

	public Object getRowObject(int r)
	{
		return vec.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		lastSortColumn = c;
		Collections.sort(vec, new pcColumnComparator(c));
		fireTableDataChanged();
	}
}

class ConfigColumnizer
{
	static String getColumn(PlatformConfig pc, int c)
	{
		switch(c)
		{
		case 0: return pc.configName;
		case 1: return pc.equipmentModel == null ? "" :
			pc.equipmentModel.name;
		case 2: return "" + pc.numPlatformsUsing;
		case 3: return pc.description;
		default: return "";
		}
	}
}

class pcColumnComparator implements Comparator
{
	int col;

	pcColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		PlatformConfig pc1 = (PlatformConfig)ob1;
		PlatformConfig pc2 = (PlatformConfig)ob2;
		switch(col)
		{
		case 0: return TextUtil.strCompareIgnoreCase(
			pc1.configName, pc2.configName);
		case 1: return TextUtil.strCompareIgnoreCase(
			pc1.getEquipmentModelName(), pc2.getEquipmentModelName());
		case 2: return pc1.numPlatformsUsing - pc2.numPlatformsUsing;
		case 3: return TextUtil.strCompareIgnoreCase(
			pc1.description, pc2.description);
		default: return 0;
		}
	}
}
