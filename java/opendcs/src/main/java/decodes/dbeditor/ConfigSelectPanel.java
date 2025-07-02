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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.Collections;
import java.util.ResourceBundle;
import javax.swing.*;
import javax.swing.table.TableRowSorter;

import decodes.db.*;
import org.opendcs.gui.SearchPanel;
import org.slf4j.LoggerFactory;

/**
Panel for selecting a Platform Configuration. Used inside ConfigSelectDialog.
*/
public class ConfigSelectPanel extends JPanel
{
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigSelectPanel.class);

	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	ConfigSelectTableModel model;
	private final TableRowSorter<ConfigSelectTableModel> sorter;
	JTable configTable;

	/** Constructor. */
	public ConfigSelectPanel(Runnable onDoubleClick)
	{
	    model = new ConfigSelectTableModel();
		configTable = new JTable(model);
		sorter = new TableRowSorter<>(model);
		configTable.setRowSorter(sorter);
		sorter.setSortKeys(
				Collections.singletonList(
						new RowSorter.SortKey(0, SortOrder.ASCENDING)
				)
		);
		sorter.setComparator(0, String.CASE_INSENSITIVE_ORDER);
		sorter.setComparator(1, String.CASE_INSENSITIVE_ORDER);
		// skip column 2  - natural Integer order.
		sorter.setComparator(3, String.CASE_INSENSITIVE_ORDER);

		configTable.getSelectionModel().setSelectionMode(
			ListSelectionModel.SINGLE_SELECTION);

		configTable.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					onDoubleClick.run();
				}
			}
		});
		try {
		    jbInit();
		}
		catch(Exception ex) {
		    log.atInfo().log("Error initializing ConfigSelectPanel",ex);
		}
	}

	/** Initializes GUI components. */
	private void jbInit()
	{
		this.setLayout(borderLayout1);
		SearchPanel searchPanel = new SearchPanel(sorter, model);
		this.add(searchPanel,BorderLayout.NORTH);
		jScrollPane1.setPreferredSize(new Dimension(453, 300));
        this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.setViewportView(configTable);
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

}



