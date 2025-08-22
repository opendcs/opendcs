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
import javax.swing.*;
import java.util.ResourceBundle;
import javax.swing.JLabel;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.LoadResourceBundle;

import decodes.gui.*;
import decodes.db.*;

/**
Displays a sorting-list of Platform objects in the database.
 */
public class PlatformListPanel extends JPanel implements ListOpsController
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	ListOpsPanel listOpsPanel;
	JLabel jLabelTitle = new JLabel();
	PlatformSelectPanel platformSelectPanel;
	DbEditorFrame parent;
	int newIndex = 1;

	/** Constructor. */
	public PlatformListPanel() {
		parent = null;
		listOpsPanel = new ListOpsPanel(this);
		listOpsPanel.enableCopy(true);
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 *
	 * @param parent the DbEditorFrame
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		platformSelectPanel = new PlatformSelectPanel(this::openPressed,null, null);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		jLabelTitle.setHorizontalAlignment(SwingConstants.CENTER);
		jLabelTitle.setText(dbeditLabels.getString("PlatformListPanel.title"));
		this.add(jLabelTitle);
		this.add(platformSelectPanel, BorderLayout.CENTER); // The JTable
		this.add(listOpsPanel);
	}


	/** @return type of entity that this panel edits. */
	public String getEntityType() { return "Platform"; }


	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
		{
			TopFrame.instance().showError(dbeditLabels.getString("PlatformListPanel.selectOpen"));
		}
		else
		{
			try
			{
				p.read();
				doOpen(p);
			}
			catch (Exception ex)
			{
				log.atError().setCause(ex).log("Unabel to read platform {}", p.makeFileName());
				TopFrame.instance().showError(
						LoadResourceBundle.sprintf(
								dbeditLabels.getString("PlatformListPanel.cannotOpen"),
								p.makeFileName(), ex.toString()));
			}
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		doNew();
	}

	public PlatformEditPanel doNew()
	{
		Platform ob = new Platform();
		platformSelectPanel.addPlatform(ob);
		return doOpen(ob);
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
		Platform p = platformSelectPanel.getSelectedPlatform();
		if (p == null)
		{
			TopFrame.instance().showError(dbeditLabels.getString("PlatformListPanel.selectCopy"));
		}
		else
		{
			if (!p.isComplete())
			{
				try { p.read(); }
				catch (DatabaseException ex)
				{
					log.atError().setCause(ex).log("Unable to copy platform {}", p.makeFileName());
					TopFrame.instance().showError(
							LoadResourceBundle.sprintf(
									dbeditLabels.getString("PlatformListPanel.cannotRead"),
									p.makeFileName(), ex.toString()));
					return;
				}
			}

			Platform newOb = p.noIdCopy();
			platformSelectPanel.addPlatform(newOb);
			doOpen(newOb);
		}
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed() {
		Platform ob = platformSelectPanel.getSelectedPlatform();
		if (ob == null)
			TopFrame.instance().showError(
					dbeditLabels.getString("PlatformListPanel.selectDelete"));
		else {
			DbEditorTabbedPane platformsTabbedPane = parent.getPlatformsTabbedPane();
			DbEditorTab tab = platformsTabbedPane.findEditorFor(ob);
			if (tab != null) {
				TopFrame.instance().showError(
						dbeditLabels.getString("PlatformListPanel.platformEdited"));
				return;
			}
			int ok = JOptionPane.showConfirmDialog(this,
					dbeditLabels.getString("PlatformListPanel.confirmDelete"),
					dbeditLabels.getString("PlatformListPanel.confirmDeleteTitle"),
					JOptionPane.YES_NO_OPTION);
			if (ok == JOptionPane.YES_OPTION) {
				platformSelectPanel.deletePlatform(ob);
				try
				{
					Database.getDb().platformList.write();
				}
				catch (DatabaseException ex)
				{
					log.atError().setCause(ex).log(dbeditLabels.getString("PlatformListPanel.errorDeleting"));
					TopFrame.instance().showError(dbeditLabels.getString("PlatformListPanel.errorDeleting") + ex);
				}
			}
		}
	}

	/** Called when the 'Help' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	  Opens a PlatformEditPanel for the passed platform.
	  @param ob the object to be edited.
	  @return the PlatformEditPanel opened.
	 */
	public PlatformEditPanel doOpen(Platform ob)
	{
		DbEditorTabbedPane tp = parent.getPlatformsTabbedPane();
		DbEditorTab tab = tp.findEditorFor(ob);
		if (tab != null)
		{
			tp.setSelectedComponent(tab);
			return (PlatformEditPanel)tab;
		}
		else
		{
			PlatformEditPanel newTab = new PlatformEditPanel(ob);
			newTab.setParent(parent);
			String title = ob.getPreferredTransportId();
			if (title.equals("unknown"))
				title = "New DCP " + (newIndex++);
			tp.add(title, newTab);
			tp.setSelectedComponent(newTab);
			return newTab;
		}
	}

	/**
	  After saving, and edit panel will need to replace the old object
	  with the newly modified one. It calls this method to do this.
	  @param oldp the old object
	  @param newp the new object
	 */
	public void replacePlatform(Platform oldp, Platform newp)
	{
		platformSelectPanel.replacePlatform(oldp, newp);
	}

	/**
	 * Called if a new platform is abandoned before it was ever saved.
	 * @param p the platform.
	 */
	void abandonNewPlatform(Platform p)
	{
		platformSelectPanel.deletePlatform(p);
	}
}
