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
import javax.swing.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.gui.TopFrame;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.PresentationGroup;

/**
Presents a list of presentation groups in the database.
*/
public class PresentationGroupListPanel extends JPanel implements ListOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
    JLabel jLabel1 = new JLabel();
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    ListOpsPanel listOpsPanel = new ListOpsPanel(this);
    PresentationGroupSelectPanel presentationGroupSelectPanel
		= new PresentationGroupSelectPanel(this);
	DbEditorFrame parent;

	/** Constructor. */
    public PresentationGroupListPanel() 
	{
        try {
            jbInit();
        }
        catch(Exception ex) {
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
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setHorizontalTextPosition(SwingConstants.CENTER);
        jLabel1.setText(
			dbeditLabels.getString("PresentationGroupListPanel.title"));
        this.setLayout(borderLayout1);
        borderLayout1.setVgap(10);
        jPanel1.setLayout(borderLayout2);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(presentationGroupSelectPanel, BorderLayout.CENTER);
        this.add(listOpsPanel, BorderLayout.SOUTH);
    }

	/** @return type of entity that this panel edits. */
	public String getEntityType() 
	{ 
		return dbeditLabels.getString("ListPanel.presentationGroupEntity"); 
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		PresentationGroup pg = presentationGroupSelectPanel.getSelection();
		if (pg == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectOpen"),getEntityType()));
		else
		    doOpen(pg, false);
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

		if (Database.getDb().presentationGroupList.find(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		PresentationGroup ob = new PresentationGroup(newName);
		presentationGroupSelectPanel.addPG(ob);
		doOpen(ob, true);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		PresentationGroup pg = presentationGroupSelectPanel.getSelection();
		if (pg == null)
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

		if (Database.getDb().presentationGroupList.find(newName) != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.alreadyExists"),
					getEntityType()));
			return;
		}

		try
		{
			pg.read();
		}
		catch (DatabaseException ex)
		{
			DbEditorFrame.instance().showError("Cannot read presentation group '"
				+ pg.groupName + "': " + ex);
		}
		
		PresentationGroup ob = pg.noIdCopy();
		ob.groupName = newName;
		presentationGroupSelectPanel.addPG(ob);
		doOpen(ob, true);
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		PresentationGroup pg = presentationGroupSelectPanel.getSelection();
		if (pg == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityType()));
			return;
		}

		DbEditorTabbedPane pgTabbedPane = parent.getPresentationTabbedPane();
		DbEditorTab tab = pgTabbedPane.findEditorFor(pg);
		if (tab != null)
		{
			DbEditorFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.beingEdited"),
					getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this,
			LoadResourceBundle.sprintf(
				dbeditLabels.getString("ListPanel.confirmDeleteMsg"),
				getEntityType()));
		if (r == JOptionPane.YES_OPTION)
			presentationGroupSelectPanel.deleteSelection();
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
		try
		{
			Database.getDb().presentationGroupList.read();
			presentationGroupSelectPanel.refill();
		}
		catch (DatabaseException ex)
		{
			parent.showError("Cannot refresh presentation group list: " + ex);
		}
	}

	/**
	  Opens an object in an Edit Panel.
	  @param pg the object to be edited.
	  @param isNew
	*/
	private void doOpen(PresentationGroup pg, boolean isNew)
	{
		DbEditorTabbedPane dbtp = parent.getPresentationTabbedPane();
		if (!pg.wasRead())
		{
			try { pg.read(); }
			catch(DatabaseException ex)
			{
				if (!isNew)
					DbEditorFrame.instance().showError(
						"Cannot open presentation group '" 
						+ pg.getDisplayName()
						+ "'");
			}
		}
		DbEditorTab tab = dbtp.findEditorFor(pg);
		if (tab != null)
			dbtp.setSelectedComponent(tab);
		else
		{
			PresentationGroupEditPanel newTab = new PresentationGroupEditPanel(pg);
			newTab.setParent(parent);
			String title = pg.groupName;
			dbtp.add(title, newTab);
			dbtp.setSelectedComponent(newTab);
		}
	}
}
