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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.SearchPanel;


import decodes.db.*;
import decodes.dbeditor.platform.PlatformSelectTableModel;

/**
Displays a sorting-list of Platform objects in the database.
 */
@SuppressWarnings("serial")
public class PlatformSelectPanel extends JPanel
{
	BorderLayout borderLayout1 = new BorderLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	PlatformSelectTableModel model;
	private TableRowSorter<PlatformSelectTableModel> sorter;
	JTable platformListTable;
	PlatformListPanel parentPanel = null;


	public PlatformSelectPanel(Runnable opener, Site site, String mediumType)
	{
		if ( site == null ) 
			model = new PlatformSelectTableModel(this, mediumType, Database.getDb());
		else
			model = new PlatformSelectTableModel(this, site, Database.getDb());
		platformListTable = new JTable(model);
		sorter = new TableRowSorter<>(model);
		platformListTable.setRowSorter(sorter);
		setMultipleSelection(false);
		platformListTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (e.getClickCount() == 2)
				{
					if (opener != null)
						opener.run();
				}
			}
		} );
		try
		{
			jbInit();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void setMultipleSelection(boolean ok)
	{
		platformListTable.getSelectionModel().setSelectionMode(
			ok ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
			ListSelectionModel.SINGLE_SELECTION);
	}

	/** Initializes GUI components. */
		private void jbInit() throws Exception
	{
		this.setPreferredSize(new Dimension(800,500));
		this.setLayout(borderLayout1);
		SearchPanel searchPanel = new SearchPanel(sorter, model);
		this.add(searchPanel,BorderLayout.NORTH);
		this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.setViewportView(platformListTable);
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	 */
	public void replacePlatform(Platform oldp, Platform newp)
	{
		model.replacePlatform(oldp, newp);
	}

	/**
	 * Adds a new platform to the list.
	  @param newp the new object
	 */
	public void addPlatform(Platform newp)
	{
		model.addPlatform(newp);
	}

	/**
	 * @return the currently-selected platform, or null if none selected.
	 */
	public Platform getSelectedPlatform()
	{
		int r = platformListTable.getSelectedRow();
		if (r == -1)
		{
			return null;
		}
		int modelRow = platformListTable.convertRowIndexToModel(r);
		return model.getPlatformAt(modelRow);
	}

	/**
	 * @return all currently-selected platforms, or empty array if none.
	 */
	public Platform[] getSelectedPlatforms()
	{
		int idx[] = platformListTable.getSelectedRows();
		Platform ret[] = new Platform[idx.length];
		for(int i=0; i<idx.length; i++)
		{
			int modelRow = platformListTable.convertRowIndexToModel(idx[i]);
			ret[i] = model.getPlatformAt(modelRow);
		}
		return ret;
	}


	/**
	 * Deletes the specified platform from the list.
	 * @param ob the object to delete.
	 */
	public void deletePlatform(Platform ob)
	{
		model.deletePlatform(ob);
	}

}
