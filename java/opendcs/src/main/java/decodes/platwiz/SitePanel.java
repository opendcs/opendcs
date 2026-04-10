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
package decodes.platwiz;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.DatabaseException;
import decodes.dbeditor.SiteEditPanel;
import decodes.dbeditor.SiteNameEntryDialog;
import decodes.dbeditor.SiteSelectDialog;
import decodes.gui.TopFrame;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/** 
The SitePanel.
This is a thin layer around decodes.dbeditor.SiteEditPanel.
*/
public class SitePanel extends JPanel implements WizardPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle genericLabels = 
		PlatformWizard.getGenericLabels();
	private static ResourceBundle platwizLabels = 
		PlatformWizard.getPlatwizLabels();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	TitledBorder titledBorder3;
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	FlowLayout flowLayout1 = new FlowLayout();
	JLabel jLabel1 = new JLabel();
	SiteEditPanel siteEditPanel = new SiteEditPanel();
	JTextField siteNameField = new JTextField();
	JButton selectSiteButton = new JButton();
	JButton newButton = new JButton();

	/** Constructs the SitePanel for the platform wizard */
	public SitePanel() {
		try 
		{
			jbInit();
		}
		catch(Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
	}

	/** Initializes the gui components */
	void jbInit() throws Exception {
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Names for this Site");
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Site Parameters");
		titledBorder3 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),"Description, Directions, etc.");
		this.setLayout(borderLayout1);
		jPanel1.setLayout(flowLayout1);
		jLabel1.setText(platwizLabels.getString("SitePanel.selectSite"));
		siteNameField.setPreferredSize(new Dimension(150, 20));
		siteNameField.setEditable(false);
		siteNameField.setText("");
//		selectSiteButton.setPreferredSize(new Dimension(85, 23));
		selectSiteButton.setText(genericLabels.getString("select"));
		selectSiteButton.addActionListener(new SitePanel_selectSiteButton_actionAdapter(this));
		newButton.setPreferredSize(new Dimension(85, 23));
		newButton.setText(genericLabels.getString("new"));
		newButton.addActionListener(new SitePanel_newButton_actionAdapter(this));
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		jPanel1.add(siteNameField, null);
		this.add(siteEditPanel, BorderLayout.CENTER);
		jPanel1.add(selectSiteButton, null);
		jPanel1.add(newButton, null);
	}


	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}

	public String getPanelTitle()
	{
		return platwizLabels.getString("SitePanel.siteLocInfo");
	}

	public String getDescription()
	{
		return platwizLabels.getString("SitePanel.description");
	}

	/** @return false Never skip this panel. */
	public boolean shouldSkip() { return false; }

	/** Called when this panel becomes the active one. */
	public void activate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		if ( p != null ) {
			Site s = p.getSite();
			siteEditPanel.setSite(s);
			SiteName sn = s == null ? null : s.getPreferredName();
			siteNameField.setText(sn == null ? "" : sn.toString());
		}	
		siteEditPanel.redisplayModel();
	}

	/** 
	  Called when user leaves this panel.
	  @return true if Site record is valid.
	*/
	public boolean deactivate()
		throws PanelException
	{
		Platform p = PlatformWizard.instance().getPlatform();
		Site s = p.getSite();
		if (s != null)
			siteEditPanel.copyValuesToObject();
		return true;
	}

	/** Called when application exits */
	public void shutdown()
	{
	}

	/**
	  Called when Select button is pressed.
	  @param e ignored
	*/
	void selectSiteButton_actionPerformed(ActionEvent e)
	{
		SiteSelectDialog dlg = new SiteSelectDialog();
		dlg.setMultipleSelection(false);
		TopFrame.instance().launchDialog(dlg);
		Site site = dlg.getSelectedSite();
		if (site != null) // selection was made?
		{
			Platform p = PlatformWizard.instance().getPlatform();
			p.setSite(site);
			try { p.getSite().read(); }
			catch(DatabaseException ex)
			{
				log.atError().setCause(ex).log("Unable to read in selected site.");
				TopFrame.instance().showError(ex.toString());
			}
			try { activate(); }
			catch(PanelException ex)
			{
				log.atError().setCause(ex).log("Unable to update panel.");
				TopFrame.instance().showError(ex.toString());
			}
		}
	}

	/**
	  Called when New button is pressed.
	  @param e ignored
	*/
	void newButton_actionPerformed(ActionEvent e) 
	{
		Platform p = PlatformWizard.instance().getPlatform();
		//p.site = new Site(p);
		//siteEditPanel.redisplayModel();
		//=========
		SiteNameEntryDialog dlg = new SiteNameEntryDialog();
		TopFrame.instance().launchDialog(dlg);
		Site site = dlg.getSite();
		if (site != null)
		{
			p.setSite(site);
		}
		//==========
		siteNameField.setText("<<new>>");
		try { activate(); }
		catch(PanelException ex)
		{
			log.atError().setCause(ex).log("Unable to update panel.");
			TopFrame.instance().showError(ex.toString());
		}
	}
}

class SitePanel_selectSiteButton_actionAdapter implements java.awt.event.ActionListener {
	SitePanel adaptee;

	SitePanel_selectSiteButton_actionAdapter(SitePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.selectSiteButton_actionPerformed(e);
	}
}

class SitePanel_newButton_actionAdapter implements java.awt.event.ActionListener {
	SitePanel adaptee;

	SitePanel_newButton_actionAdapter(SitePanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.newButton_actionPerformed(e);
	}
}
