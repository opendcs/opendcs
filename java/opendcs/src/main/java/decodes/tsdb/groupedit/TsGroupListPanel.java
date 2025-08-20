/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package decodes.tsdb.groupedit;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.SearchPanel;
import org.slf4j.LoggerFactory;

import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TsGroup;

/**
 * Displays a sorting-list of TimeSeries Group objects in the database
 * or in a TsGroup obj. Use by the TsGroupDefinitionPanel class
 * and the TsGroupSelectDialog class.
 */
public class TsGroupListPanel extends JPanel
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(TsGroupListPanel.class);
	//Panel
	public String module = "TsGoupListPanel";
	//Panel Owner
	private TopFrame frameOwner = null;
	private GuiDialog dialogOwner = null;
	//Panel Components
	private JScrollPane jScrollPane = new JScrollPane();
	private JTable tsGroupsListTable;
	private TsGroupsSelectTableModel model;
	//Time Series DB
	TimeSeriesDb theTsDb;
	//Titles, Labels defined here for internationalization
	private GroupSelector ctrlPanel = null;

	/** Constructor. */
	public TsGroupListPanel(TimeSeriesDb theTsDb, GuiDialog dialogOwner, GroupSelector ctrlPanel)
	{
		this(theTsDb, dialogOwner, null, ctrlPanel);
	}

	public TsGroupListPanel(TimeSeriesDb theTsDb, TopFrame frameOwner, GroupSelector ctrlPanel)
	{
		this(theTsDb, null, frameOwner, ctrlPanel);
	}

	private TsGroupListPanel(TimeSeriesDb theTsDb, GuiDialog dialogOwner,
		TopFrame frameOwner, GroupSelector ctrlPanel)
	{
		this.theTsDb = theTsDb;
		this.dialogOwner = dialogOwner;
		this.frameOwner = frameOwner;
		this.ctrlPanel = ctrlPanel;

		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			log.atInfo().log("Error initializing ",ex);
		}
	}
	
	/** Initializes GUI components. */
	private void jbInit()
	{
		jScrollPane = new JScrollPane();
		model = new TsGroupsSelectTableModel(this);
		tsGroupsListTable = new JTable(model);
		TableRowSorter sorter = new TableRowSorter<>(model);
		tsGroupsListTable.setRowSorter(sorter);
		SwingUtilities.invokeLater(() -> {
			sorter.setSortKeys( // initial sort on Name
					Collections.singletonList(
							new RowSorter.SortKey(1, SortOrder.ASCENDING)
					)
			);
			sorter.sort();
		});

		SearchPanel searchPanel = new SearchPanel(sorter, model);

		setMultipleSelection(false);
		if (ctrlPanel != null)
			tsGroupsListTable.addMouseListener(
				new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
							ctrlPanel.groupSelected();
						}
					}
				});

		setLayout(new BorderLayout());
		this.add(searchPanel,BorderLayout.NORTH);
		add(jScrollPane, BorderLayout.CENTER);
		jScrollPane.setViewportView(tsGroupsListTable);
	}

	public void setMultipleSelection(boolean ok)
	{
		tsGroupsListTable.getSelectionModel().setSelectionMode(
				ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
						: ListSelectionModel.SINGLE_SELECTION);
	}

	/**
	 * This method will do two things: Add a new ts group to the list if obj
	 * does not exists -or- Modify a ts group obj if already in the list
	 * 
	 * @param oldG
	 *            the old object
	 * @param newG
	 *            the new object
	 */
	public void modifyTsGroupList(TsGroup oldG, TsGroup newG)
	{
		model.modifyTsGroupList(oldG, newG);
	}

	/**
	 * @return the currently-selected Ts Group, or null if none selected
	 */
	public TsGroup getSelectedTsGroup()
	{
		int r = tsGroupsListTable.getSelectedRow();
		if (r == -1)
			return null;
		//Get the correct selected row from the model
		int rowModel = this.tsGroupsListTable.convertRowIndexToModel(r);
		TsGroupsSelectTableModel model = (TsGroupsSelectTableModel) tsGroupsListTable.getModel();
		return model.getTsGroupAt(rowModel);
	}

	public int[] getSelectedRows()
	{
		return tsGroupsListTable.getSelectedRows();
	}
	
	public TsGroup getTsGroupAt(int index)
	{
		return model.getTsGroupAt(index);
	}
	
	/**
	 * @return all currently-selected TS Groups, or empty array if none.
	 */
	public TsGroup[] getSelectedTsGroups()
	{
		int idx[] = tsGroupsListTable.getSelectedRows();
		TsGroup ret[] = new TsGroup[idx.length];
		for (int i = 0; i < idx.length; i++)
			ret[i] = model.getTsGroupAt(idx[i]);
		return ret;
	}

	public void refreshTsGroupList()
	{
		model.refresh();
	}

	public TimeSeriesDb getTsDb()
	{
		return theTsDb;
	}

	public void setTsDb(TimeSeriesDb theTsDb)
	{
		this.theTsDb = theTsDb;
	}

	/**
	 * Deletes the specified Ts Group from the list.
	 * 
	 * @param ob
	 *            the object to delete.
	 */
	public void deleteTsGroup(TsGroup ob)
	{
		model.deleteTsGroup(ob);
	}

	/**
	 * Delete Ts Group of the given array list
	 * 
	 * @param tsGroups
	 *            list of Ts Groups to delete
	 */
	//public void deleteTsGroups(ArrayList<TsGroup> tsGroups)
	public void deleteTsGroups(TsGroup[] tsGroups)
	{
		for (TsGroup g : tsGroups)
		{
			deleteTsGroup(g);
		}
	}

	/**
	 * Verify is the given group name exists in the current list or not
	 * 
	 * @param groupName group name
	 * @return true if the group name exitst in the list, false otherwise
	 */
	public boolean tsGroupExistsInList(String groupName)
	{
		return model.tsGroupExistsInList(groupName);
	}
	

	public void setTsGroupListFromDb()
	{
		model.setTsGroupListFromDb();	
	}
	
	/**
	 * This method is used from the TsGroupDefinitionPanel when
	 * adding new sub groups.
	 * 
	 * @param tsGroupToAdd group to add
	 */
	private void addTsGroup(TsGroup tsGroupToAdd)
	{
		model.addTsGroup(tsGroupToAdd);
	}

	private ArrayList<TsGroup> getAllTsGroupsInList()
	{
		return model.getAllTsGroupsInList();
	}
	
	private int getSelectedRowCount()
	{
		return tsGroupsListTable.getSelectedRowCount();
	}
	
	public void clearSelection()
	{
		tsGroupsListTable.clearSelection();
	}
	
	void showError(String msg)
	{
		if (frameOwner != null)
			frameOwner.showError(msg);
		else if (dialogOwner != null)
			dialogOwner.showError(msg);
	}
	
	public TsGroupsSelectTableModel getModel()
	{
		return model;
	}
}


