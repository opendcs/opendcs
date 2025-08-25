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

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import decodes.gui.*;
import decodes.db.*;

/**
 * Shows a sorting list of sites in the database editor.
 */
@SuppressWarnings("serial")
public class SiteListPanel extends JPanel implements ListOpsController
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	ListOpsPanel listOpsPanel = new ListOpsPanel(this);
	SiteSelectPanel siteSelectPanel = new SiteSelectPanel();
	DbEditorFrame parent;

	/** Constructor. */
	public SiteListPanel()
	{
		parent = null;
		siteSelectPanel.setParentPanel(this);
		try
		{
			jbInit();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
		listOpsPanel.enableCopy(false);
	}

	/**
	 * Sets the parent frame object. Each list panel needs to know this.
	 *
	 * @param parent
	 *            the DbEditorFrame
	 */
	void setParent(DbEditorFrame parent)
	{
		this.parent = parent;
	}

	/** Initializes GUI components. */
	private void jbInit() throws Exception
	{
		this.setLayout(new BorderLayout());
		JLabel titleLabel = new JLabel(dbeditLabels.getString("SiteListPanel.title"));
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.add(listOpsPanel, BorderLayout.SOUTH);
		this.add(titleLabel, BorderLayout.NORTH);
		this.add(siteSelectPanel, BorderLayout.CENTER);
	}

	/** @return type of entity that this panel edits. */
	public String getEntityType()
	{
		return dbeditLabels.getString("ListPanel.siteEntity");
	}

	/** Called when the 'Open' button is pressed. */
	public void openPressed()
	{
		Site site = siteSelectPanel.getSelectedSite();
		if (site == null)
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectOpen"),
					getEntityType()));
		else
		{
			try
			{
				site.read();
			}
			catch (DatabaseException ex)
			{
				log.atError().setCause(ex).log("Unable to open site.");
			}
			doOpen(site);
		}
	}

	/** Called when the 'New' button is pressed. */
	public void newPressed()
	{
		SiteNameEntryDialog dlg = new SiteNameEntryDialog();
		TopFrame.instance().launchDialog(dlg);
		Site site = dlg.getSite();
		if (site != null)
		{
			site.isNew = true;
			Database.getDb().siteList.addSite(site);
			siteSelectPanel.addSite(site);
			doOpen(site);
		}
	}

	/** Called when the 'Copy' button is pressed. */
	public void copyPressed()
	{
	}

	/** Called when the 'Delete' button is pressed. */
	public void deletePressed()
	{
		Site site = siteSelectPanel.getSelectedSite();
		if (site == null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.selectDelete"),
					getEntityType()));
			return;
		}
		DbEditorTabbedPane sitesTabbedPane = parent.getSitesTabbedPane();
		DbEditorTab tab = sitesTabbedPane.findEditorFor(site);
		if (tab != null)
		{
			TopFrame.instance().showError(
				LoadResourceBundle.sprintf(
					dbeditLabels.getString("ListPanel.beingEdited"),
					getEntityType()));
			return;
		}
		int r = JOptionPane.showConfirmDialog(this, LoadResourceBundle.sprintf(
			dbeditLabels.getString("ListPanel.confirmDeleteMsg"),
			getEntityType()), dbeditLabels
			.getString("ListPanel.confirmDeleteTitle"),
			JOptionPane.YES_NO_OPTION);
		if (r == JOptionPane.YES_OPTION)
			siteSelectPanel.deleteSelectedSite();
	}

	/** Called when the 'Refresh' button is pressed. */
	public void refreshPressed()
	{
	}

	/**
	 * Opens a SiteEditPanel for the passed site.
	 *
	 * @param site
	 *            the site to open.
	 * @return the new SiteEditPanel
	 */
	public SiteEditPanel doOpen(Site site)
	{
		DbEditorTabbedPane sitesTabbedPane = parent.getSitesTabbedPane();
		DbEditorTab tab = sitesTabbedPane.findEditorFor(site);
		if (tab != null)
		{
			sitesTabbedPane.setSelectedComponent(tab);
			return (SiteEditPanel) tab;
		}
		else
		{
			SiteEditPanel newTab = new SiteEditPanel(site);
			newTab.setParent(parent);
			String title = site.getPreferredName().getNameValue();
			sitesTabbedPane.add(title, newTab);
			sitesTabbedPane.setSelectedComponent(newTab);
			return newTab;
		}
	}
}
