/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import decodes.gui.*;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;

import decodes.db.Database;
import decodes.db.DataSource;
import decodes.dbeditor.sources.DataSourceListTableModel;

/**
DBEDIT panel containing sorting list of data sources.
*/
public class SourcesListPanel extends JPanel implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	BorderLayout borderLayout1 = new BorderLayout();
	ListOpsPanel sourcesListOpsPanel = new ListOpsPanel(this);
	JLabel jLabel1 = new JLabel();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTable dataSourceListTable;
	DataSourceListTableModel tableModel;
	DbEditorFrame parent;

	/** Constructor. */
	public SourcesListPanel()
	{
		try
		{
			tableModel = new DataSourceListTableModel(this,Database.getDb());
			dataSourceListTable = new JTable(tableModel);
			dataSourceListTable.setAutoCreateRowSorter(true);
			//new int[] { 25, 15, 50, 10 });
	 		dataSourceListTable.getSelectionModel().setSelectionMode(
		    	ListSelectionModel.SINGLE_SELECTION);

			dataSourceListTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e){
					if (e.getClickCount() == 2){
		       				openPressed();
					}
				}
			} );
			jbInit();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	  Sets the parent frame object. Each list panel needs to know this.
	  @param parent the DbEditorFrame
	*/
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
    private void jbInit() throws Exception {
        this.setLayout(borderLayout1);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(
			dbeditLabels.getString("SourcesListPanel.title"));
        this.add(sourcesListOpsPanel, BorderLayout.SOUTH);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(dataSourceListTable, null);
    }

	/** @return type of entity that this panel edits. */
	public String getEntityType() 
	{ 
		return dbeditLabels.getString("ListPanel.dataSourceEntity"); 
	}

	/** @return currently selected DataSource */
	private DataSource getSelection()
	{
		int idx = dataSourceListTable.getSelectedRow();
		if (idx == -1)
		{
			return null;
		}
		int modelRow = dataSourceListTable.convertRowIndexToModel(idx);
	    return tableModel.getObjectAt(modelRow);
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectOpen"),
					getEntityType()));
		else
		    doOpen(ds);
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		String newName = JOptionPane.showInputDialog(
		LoadResourceBundle.sprintf(
		dbeditLabels.getString("ListPanel.enterNewName"),
		getEntityType()));
		if (newName == null)
			return;
		if (Database.getDb().dataSourceList.get(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		DataSource ob = new DataSource(newName, "LRGS");
		Database.getDb().dataSourceList.add(ob);
		tableModel.fireTableDataChanged();
		doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectCopy"),
					getEntityType()));
			return;
		}
		String newName = JOptionPane.showInputDialog(
			dbeditLabels.getString("ListPanel.enterCopyName"));
		if (newName == null)
			return;

		if (Database.getDb().dataSourceList.get(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		DataSource ob = ds.copy();
		ob.clearId();
		ob.setName(newName);
		this.tableModel.addDataSource(ob);
		doOpen(ob);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		DataSource ds = getSelection();
		if (ds == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityType()));
			return;
		}

		// Don't allow delete if this source is being used by a routing spec.
		ds.countUsedBy();
		if (ds.numUsedBy > 0)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("SourcesListPanel.beingUsed"),
					ds.numUsedBy));
			return;
		}

		// Also don't allow delete if this source is a group member of 
		// another data source.
		for(Iterator<DataSource> it = Database.getDb().dataSourceList.iterator();
			it.hasNext(); )
		{
			DataSource x = it.next();
			if (x == ds)
				continue;
			if (x.isInGroup(ds.getName()))
			{
				TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("SourcesListPanel.isGroupMember"),
						x.getName()));
				return;
			}
		}

		DbEditorTabbedPane sourcesTabbedPane = parent.getSourcesTabbedPane();
		DbEditorTab tab = sourcesTabbedPane.findEditorFor(ds);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.beingEdited"),
					getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				dbeditLabels.getString("ListPanel.confirmDeleteMsg"),
				getEntityType()),
			dbeditLabels.getString("ListPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			tableModel.deleteObject(ds);
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens the data source in a new edit panel.
	  @param ds the data source to open. 
	*/
	private void doOpen(DataSource ds)
	{
		DbEditorTabbedPane dbtp = parent.getSourcesTabbedPane();
		DbEditorTab tab = dbtp.findEditorFor(ds);
		if (tab != null)
			dbtp.setSelectedComponent(tab);
		else
		{
			SourceEditPanel newTab = new SourceEditPanel(ds);
			newTab.setParent(parent);
			String title = ds.getName();
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
		}
	}	
}
