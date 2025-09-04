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
import java.awt.event.*;
import java.util.Vector;
import java.util.ResourceBundle;

import javax.swing.border.*;

import ilex.util.AsciiUtil;

import decodes.gui.TopFrame;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.EquipmentModel;
import decodes.db.Site;
import decodes.db.TransportMedium;
import decodes.dbeditor.PlatformSelectDialogforSubset;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
/**
Initial wizard panel. Gather info about what the user wants to do.
*/
public class StartPanel extends JPanel implements WizardPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle platwizLabels = 
				PlatformWizard.getPlatwizLabels();
	TitledBorder titledBorder1;
	TitledBorder titledBorder2;
	GridLayout gridLayout1 = new GridLayout();
	JTextField rdChanField = new JTextField();
	JCheckBox edlCheck = new JCheckBox();
	JPanel jPanel1 = new JPanel();
	JLabel jLabel2 = new JLabel();
	JCheckBox selfTimedCheck = new JCheckBox();
	JTextField stChanField = new JTextField();
	JCheckBox randomCheck = new JCheckBox();
	JLabel jLabel3 = new JLabel();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	JTextField usgsSiteNumField = new JTextField();
	JTextField dcpAddrField = new JTextField();
	JCheckBox goesCheck = new JCheckBox();
	JButton decodesButton = new JButton();
	JLabel jLabel1 = new JLabel();
	JPanel jPanel2 = new JPanel();
	JButton hadsButton = new JButton();
	JButton dapsPdtButton = new JButton();
	JComboBox<String> decodesRepositoryCombo = new JComboBox<>(
			new String[] {"Unfortunately, there are currently no decodes repositories"});
	JButton myEditDbButton = new JButton();
	GridBagLayout gridBagLayout1 = new GridBagLayout();

	/** Numeric GOES self timed channel number */
	private int goesSTChannel;

	/** Numeric GOES random channel number */
	private int goesRDChannel;

	/** Construct the StartPanel */
	public StartPanel()
	{
		try 
		{
			jbInit();
		}
		catch(Exception ex) 
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
		goesSTChannel = -1;
		goesRDChannel = -1;
	}

	/** Initialize GUI components */
	void jbInit() throws Exception {
		titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				platwizLabels.getString("StartPanel.platformTypeChkTitle"));
		titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,new Color(165, 163, 151)),
				platwizLabels.getString("StartPanel.retrieveInitialInfoTitle"));
		this.setLayout(gridLayout1);
		gridLayout1.setColumns(1);
		gridLayout1.setRows(2);
		rdChanField.setPreferredSize(new Dimension(45, 20));
		edlCheck.setSelected(true);
    edlCheck.setText(platwizLabels.getString("StartPanel.USGSElectronicDL"));
		edlCheck.addActionListener(new StartPanel_edlCheck_actionAdapter(this));
		jPanel1.setLayout(gridBagLayout2);
		jPanel1.setBorder(titledBorder1);
		jLabel2.setText(platwizLabels.getString("StartPanel.NESDIS-AssignedDCP"));
		selfTimedCheck.setSelected(true);
    selfTimedCheck.setText(platwizLabels.getString("StartPanel.selfTimedMsgsCh"));
		selfTimedCheck.addActionListener(new StartPanel_selfTimedCheck_actionAdapter(this));
		selfTimedCheck.addActionListener(new StartPanel_selfTimedCheck_actionAdapter(this));
		stChanField.setPreferredSize(new Dimension(45, 20));
		randomCheck.setSelected(true);
    randomCheck.setText(platwizLabels.getString("StartPanel.randomMsgsCh"));
		randomCheck.addActionListener(new StartPanel_randomCheck_actionAdapter(this));
		randomCheck.addActionListener(new StartPanel_randomCheck_actionAdapter(this));
		jLabel3.setText(platwizLabels.getString("StartPanel.USGSNumSiteId"));
		usgsSiteNumField.setPreferredSize(new Dimension(160, 20));
		dcpAddrField.setPreferredSize(new Dimension(120, 20));
		goesCheck.setSelected(true);
    goesCheck.setText(platwizLabels.getString("StartPanel.GOESDataCollection"));
		goesCheck.addActionListener(new StartPanel_goesCheck_actionAdapter(this));
		goesCheck.addActionListener(new StartPanel_goesCheck_actionAdapter(this));
		decodesButton.setPreferredSize(new Dimension(210, 23));
		decodesButton.setToolTipText(
			platwizLabels.getString("StartPanel.searchDCPInDBTT"));
		decodesButton.setText(
			platwizLabels.getString("StartPanel.remoteDECODES"));
		decodesButton.addActionListener(new StartPanel_decodesButton_actionAdapter(this));
		jLabel1.setText(
			platwizLabels.getString("StartPanel.platformInfoAvailability"));
		jPanel2.setBorder(titledBorder2);
    jPanel2.setRequestFocusEnabled(true);
    jPanel2.setToolTipText("");
		jPanel2.setLayout(gridBagLayout1);
		hadsButton.setMinimumSize(new Dimension(322, 23));
		hadsButton.setPreferredSize(new Dimension(322, 23));
		hadsButton.setToolTipText(
				platwizLabels.getString("StartPanel.lookDCPOnHADSTT"));
		hadsButton.setText(
			platwizLabels.getString("StartPanel.NWSHydrometerologicAuto"));
		hadsButton.addActionListener(new StartPanel_hadsButton_actionAdapter(this));
		dapsPdtButton.setMinimumSize(new Dimension(322, 23));
		dapsPdtButton.setPreferredSize(new Dimension(322, 23));
		dapsPdtButton.setToolTipText(
			platwizLabels.getString("StartPanel.platformFromDAPSTT"));
		dapsPdtButton.setText(
			platwizLabels.getString("StartPanel.NESDISDAPSPdt"));
		dapsPdtButton.addActionListener(new StartPanel_dapsPdtButton_actionAdapter(this));
		myEditDbButton.setToolTipText(
			platwizLabels.getString("StartPanel.pullExistingPlatTT"));
    myEditDbButton.setText(
    		platwizLabels.getString("StartPanel.myEditableDatabase"));
    myEditDbButton.addActionListener(new StartPanel_myEditDbButton_actionAdapter(this));
    decodesRepositoryCombo.setEditable(true);
    this.add(jPanel1, null);
		jPanel1.add(goesCheck,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 15, 2, 0), 0, 0));
		jPanel1.add(dcpAddrField,  new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 0), 0, 0));
		jPanel1.add(stChanField,  new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 0), 0, 0));
		jPanel1.add(edlCheck,  new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 15, 2, 0), 0, 0));
		jPanel1.add(usgsSiteNumField,  new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 0), 0, 0));
		jPanel1.add(selfTimedCheck,  new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 60, 2, 0), 0, 0));
		jPanel1.add(jLabel3,   new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(2, 60, 2, 0), 0, 0));
		jPanel1.add(jLabel2,  new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 60, 2, 0), 0, 0));
		jPanel1.add(randomCheck,  new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 60, 2, 0), 0, 0));
		jPanel1.add(rdChanField,  new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 5, 2, 0), 0, 0));
		jPanel2.add(decodesButton,      new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 50, 5, 0), 0, 0));
		jPanel2.add(dapsPdtButton,      new GridBagConstraints(0, 4, 2, 1, 1.0, 0.0
            ,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 50, 5, 70), 0, 0));
		jPanel2.add(hadsButton,      new GridBagConstraints(0, 3, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 50, 5, 70), 0, 0));
		jPanel2.add(decodesRepositoryCombo,     new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 10, 5, 70), -6, 0));
    jPanel2.add(jLabel1,    new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 0, 5, 0), 0, 0));
    jPanel2.add(myEditDbButton,      new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 50, 5, 70), 0, 0));
		this.add(jPanel2, null);
	}


	/**
	  Called when user presses the "DECODES Repository" button.
	*/
	void decodesButton_actionPerformed(ActionEvent e) {

	}

	/**
	  Called when user presses the "HADS" button.
	*/
	void hadsButton_actionPerformed(ActionEvent e) {

	}

	/**
	  Called when user presses the "NESDIS PDT" button.
	*/
	void dapsPdtButton_actionPerformed(ActionEvent e)
	{
	}

	/** Called when user clicks the GOES DCP checkbox. */
	void goesCheck_actionPerformed(ActionEvent e)
	{
		if (goesCheck.isSelected())
		{
			dcpAddrField.setEnabled(true);
			randomCheck.setEnabled(true);
			selfTimedCheck.setEnabled(true);
			rdChanField.setEnabled(randomCheck.isSelected());
			stChanField.setEnabled(selfTimedCheck.isSelected());
		}
		else
		{
			dcpAddrField.setEnabled(false);
			randomCheck.setEnabled(false);
			selfTimedCheck.setEnabled(false);
			rdChanField.setEnabled(false);
			stChanField.setEnabled(false);
		}
	}

	/** Called when user clicks the self-timed GOES check box. */
	void selfTimedCheck_actionPerformed(ActionEvent e)
	{
		stChanField.setEnabled(selfTimedCheck.isSelected());
	}

	/** Called when user clicks the random GOES check box. */
	void randomCheck_actionPerformed(ActionEvent e)
	{
		rdChanField.setEnabled(randomCheck.isSelected());
	}

	/** Called when user clicks the USGS EDL checkbox. */
	void edlCheck_actionPerformed(ActionEvent e)
	{
		usgsSiteNumField.setEnabled(edlCheck.isSelected());
	}

	// ------- From Wizard Panel Interface -------

	/** Called once at start-up. */
	public void initialize()
		throws PanelException
	{
	}
  /**
   * Launches a dialog, centered in the frame.
   */
  public void launchDialog(JDialog dlg)
  {
    TopFrame.instance().launchDialog(dlg);
  }
	public String getPanelTitle()
	{
		return platwizLabels.getString("StartPanel.welcomeMsg");
	}

	public String getDescription()
	{
		return platwizLabels.getString("StartPanel.description");
	}

	public boolean shouldSkip() { return false; }

	public void activate()
		throws PanelException
	{
		hadsButton.setEnabled(false);
		decodesButton.setEnabled(false);
		dapsPdtButton.setEnabled(false);
	}

	public boolean deactivate()
		throws PanelException
	{
		if (!goesCheck.isSelected())
			goesSTChannel = goesRDChannel = -1;
		else
		{
			String dcpaddr = getDcpAddress();
			if (dcpaddr == null)
			{
				TopFrame.instance().showError(
						platwizLabels.getString("StartPanel.dcpAddrErr"));
				return false;
			}

			String s = rdChanField.getText().trim();
			if (!randomCheck.isSelected() || s.length() == 0)
				goesRDChannel = -1;
			else
			{
				try { goesRDChannel = Integer.parseInt(s); }
				catch(NumberFormatException ex)
				{
					String msg = platwizLabels.getString("StartPanel.randomChErr");
					log.atError().setCause(ex).log(msg);
					PlatformWizard.instance().showError(msg);
					return false;
				}
			}

			s = stChanField.getText().trim();
			if (!selfTimedCheck.isSelected() || s.length() == 0)
				goesSTChannel = -1;
			else
			{
				try { goesSTChannel = Integer.parseInt(s); }
				catch(NumberFormatException ex)
				{
					String msg = platwizLabels.getString("StartPanel.selfChErr");
					log.atError().setCause(ex).log(msg);
					PlatformWizard.instance().showError(msg);
					return false;
				}
			}
		}
		if (PlatformWizard.instance().getPlatform() == null)
		{
			if (existsInMyDb())
			{
				int choice = JOptionPane.showConfirmDialog(
					PlatformWizard.instance().getFrame(),
					AsciiUtil.wrapString(
					platwizLabels.getString("StartPanel.platExistMsg"),
					50),
					platwizLabels.getString("StartPanel.platOverwriteMsg"), 
					JOptionPane.YES_NO_CANCEL_OPTION);
				if (choice == JOptionPane.YES_OPTION)
				{
					myEditDbButton_actionPerformed(null);
					return PlatformWizard.instance().getPlatform() != null;
				}
				else if (choice == JOptionPane.CANCEL_OPTION)
					return false;
				// else fall through & initialize a new platform.
			}
		}
		//Find out if we have a Platform created already, if we do, use it. 
		Platform p = PlatformWizard.instance().getPlatform();
		if (p == null)
			p = new Platform();
		else
		{
			try { p.read(); }
			catch(DatabaseException ex)
			{
				log.atError().setCause(ex).log("Unable to read platform.");
			}
		}
		String dcpaddr = getDcpAddress();

		if (dcpaddr != null)
		{
			if (processGoesST())
			{
				//If we have a Platform, find the transport media and
				//updated it					
				TransportMedium tm = 
					p.getTransportMedium(Constants.medium_GoesST);
				int tmIndex = p.transportMedia.indexOf(tm); 
				if (tm == null)
				{	//Create Transport Media
					tm = new TransportMedium(p,
								Constants.medium_GoesST, dcpaddr);
					tm.scriptName = "ST";
					tm.channelNum = goesSTChannel;						
					p.transportMedia.add(tm);
				}
				else
				{
					tm.channelNum = goesSTChannel;
					//Update the transportMedia
					if (tmIndex != -1)
						p.transportMedia.set(tmIndex, tm);						
				}
			}
			if (processGoesRD())
			{
				TransportMedium tm = 
					p.getTransportMedium(Constants.medium_GoesRD);
				int tmIndex = p.transportMedia.indexOf(tm); 
				if (tm == null)
				{	//Create Transport Media
					tm = new TransportMedium(p,
								Constants.medium_GoesRD, dcpaddr);
					tm.scriptName = "ST";
					tm.channelNum = goesRDChannel;
					p.transportMedia.add(tm);	
				}
				else
				{
					tm.channelNum = goesRDChannel;
					//Update the transportMedia
					if (tmIndex != -1)
						p.transportMedia.set(tmIndex, tm);
				}
			}
		}
		if (processEDL())
		{
			String siteId = getUsgsSiteId();
			if (siteId == null)
			{
				TopFrame.instance().showError(
					platwizLabels.getString("StartPanel.USGSSiteIdErr"));
				return false;
			}
			TransportMedium tm = 
				p.getTransportMedium(Constants.medium_EDL);
			int tmIndex = p.transportMedia.indexOf(tm); 
			if (tm == null)
			{
				String mediumId = null;
				PlatformConfig pc = p.getConfig();
				if ( pc != null ) {
					EquipmentModel em = pc.getEquipmentModel();
					if ( em != null )
						mediumId = siteId+"-"+em.getName()+"-1";
				}
				if ( mediumId == null )
					mediumId = siteId;
				
				tm = new TransportMedium(p,
						Constants.medium_EDL, mediumId);
				tm.scriptName = "EDL";
				p.transportMedia.add(tm);	
			}
			else
			{
//				tm.setMediumId(siteId);
				//Update the transportMedia
				if (tmIndex != -1)
					p.transportMedia.set(tmIndex, tm);
			}
		}
		PlatformWizard.instance().setPlatform(p);
			
		return true;
	}

	public void shutdown()
	{
	}

	/** @returns numeric GOES Self-time Channel */
	public int getSTChannel()
	{
		return goesSTChannel;
	}

	/** @returns numeric GOES Random Channel */
	public int getRDChannel()
	{
		return goesRDChannel;
	}

	/** @returns DCP address if one specified, or null if not. */
	public String getDcpAddress()
	{
		if (!goesCheck.isSelected())
			return null;
		String s = dcpAddrField.getText().trim();
		if (s.length() == 0)
			return null;
		return s;
	}

	/** @returns USGS Site ID if one specified, or null if not. */
	public String getUsgsSiteId()
	{
		if (!edlCheck.isSelected())
			return null;
		String s = usgsSiteNumField.getText().trim();
		if (s.length() == 0)
			return null;
		return s;
	}

	/** Returns true if GOES self-timed messages should be processed. */
	public boolean processGoesST()
	{
		return goesCheck.isSelected() && selfTimedCheck.isSelected();
	}

	/** Returns true if GOES Random messages should be processed. */
	public boolean processGoesRD()
	{
		return goesCheck.isSelected() && randomCheck.isSelected();
	}

	/** Returns true if EDL files should be processed. */
	public boolean processEDL()
	{
		return edlCheck.isSelected();
	}

	private boolean existsInMyDb()
	{
		String dcpaddr = dcpAddrField.getText().toUpperCase().trim();
		Platform goesPlat = null;
		Platform edlPlat = null;
		String siteNum = usgsSiteNumField.getText().trim();

		// If GOES ID was entered, Look it up as a Transport ID.
		try
		{
			if (goesCheck.isSelected() && dcpaddr.length() > 0)
			{
				goesPlat = Database.getDb().platformList.getPlatform(
					Constants.medium_Goes, dcpaddr);
			}
	
			// If USGS Site ID was entered, look it up.
			if (edlCheck.isSelected() && siteNum.length() > 0)
			{
				edlPlat = Database.getDb().platformList.getPlatform(
					Constants.medium_EDL, siteNum);
			}
		}
		catch(DatabaseException ex)
		{
			log.atTrace().setCause(ex).log("Unable to look up GOES Id in current database.");
			return false;
		}

		if (edlPlat == null && goesPlat == null)
			return false;
		else
			return true;
	}

	/** Called when My Editable Database is pressed. */
	void myEditDbButton_actionPerformed(ActionEvent e) 
	{
		// If we previously had a complete platform, set it to incomplete
		// to force it to be re-read from the database (overwriting any
		// changes made on other panels.
		Platform p = PlatformWizard.instance().getPlatform();
		if (p != null)
			p.setIsComplete(false);

		String dcpaddr = dcpAddrField.getText().toUpperCase().trim();
		Platform goesPlat = null;
		Platform edlPlat = null;
		String siteNum = usgsSiteNumField.getText().trim();

		// If GOES ID was entered, Look it up as a Transport ID.
		try
		{
			if (goesCheck.isSelected() && dcpaddr.length() > 0)
			{
				goesPlat = Database.getDb().platformList.getPlatform(
					Constants.medium_Goes, dcpaddr);
			}
	
			// If USGS Site ID was entered, look it up.
			if (edlCheck.isSelected() && siteNum.length() > 0)
			{
				Site edlSite = Database.getDb().siteList.getSite("USGS",siteNum);
				if ( edlSite != null ) {
					Vector<Platform> pvec = Database.getDb().platformList.getPlatforms(edlSite);
					for(int j=0; j < pvec.size(); j++ ) {
							edlPlat = pvec.elementAt(j);
							if (  edlPlat.getDcpAddress() != null )
								pvec.removeElementAt(j);
					}
					if ( pvec.size() == 1 ) 
						edlPlat = pvec.elementAt(0);
					else if ( pvec.size() > 1 ) {
						PlatformSelectDialogforSubset psdlg = new
								PlatformSelectDialogforSubset(pvec);
						psdlg.setMultipleSelection(false);
						launchDialog(psdlg);
						edlPlat = psdlg.getSelectedPlatform();
					} else
						edlPlat = null;
				} else
						edlPlat = null;
			}
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Unable to run edit db button.");
			PlatformWizard.instance().showError(ex.toString());
			return;
		}
		if (edlPlat == null && goesPlat == null)
			PlatformWizard.instance().showError(
			platwizLabels.getString("StartPanel.cannotFindPlatErr"));
		// If both were entered and they refer to different sites -- error.
		else if (edlPlat != null && goesPlat != null && edlPlat != goesPlat)
		{
			Object options[] = { "Use GOES Platform",
				"Use EDL Platform", "Cancel" };

			int choice = JOptionPane.showOptionDialog(
				PlatformWizard.instance().getFrame(),
				platwizLabels.getString("StartPanel.dcpAddrEDLIdErr"),
				platwizLabels.getString("StartPanel.platformConflict"), 
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE, 
				(Icon)null,
				options,
				options[2]);

			if (choice == JOptionPane.YES_OPTION) // GOES
			{
				PlatformWizard.instance().setPlatform(goesPlat);
//				Set the seft time channel number
				String s = stChanField.getText().trim();
					TransportMedium tm = 
						goesPlat.getTransportMedium(Constants.medium_GoesST);
					if (tm != null)
					{
						stChanField.setText(""+tm.channelNum);
					}
					tm = goesPlat.getTransportMedium(Constants.medium_GoesRD);
					if ( tm != null ) {
						rdChanField.setText(""+tm.channelNum);
					}
			}
			else if (choice == JOptionPane.NO_OPTION) // EDL
				PlatformWizard.instance().setPlatform(edlPlat);
			else if (choice == JOptionPane.CANCEL_OPTION)
				PlatformWizard.instance().setPlatform(null);
		}
		else if (goesPlat != null)
		{
			PlatformWizard.instance().setPlatform(goesPlat);
			//Set the seft time channel number
			String s = stChanField.getText().trim();
				TransportMedium tm = 
					goesPlat.getTransportMedium(Constants.medium_GoesST);
				if (tm != null)
				{	
					stChanField.setText(""+tm.channelNum);
				}
				tm = goesPlat.getTransportMedium(Constants.medium_GoesRD);
				if ( tm != null ) {
					rdChanField.setText(""+tm.channelNum);
				}
				if ( goesPlat.getSite() != null ) {
					if ( goesPlat.getSite() != null )
						usgsSiteNumField.setText(goesPlat.getSite().getDisplayName());
				}
		}
		else if (edlPlat != null)
			PlatformWizard.instance().setPlatform(edlPlat);
	}
}


class StartPanel_decodesButton_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_decodesButton_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.decodesButton_actionPerformed(e);
	}
}

class StartPanel_hadsButton_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_hadsButton_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.hadsButton_actionPerformed(e);
	}
}

class StartPanel_dapsPdtButton_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_dapsPdtButton_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.dapsPdtButton_actionPerformed(e);
	}
}

class StartPanel_goesCheck_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_goesCheck_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.goesCheck_actionPerformed(e);
	}
}

class StartPanel_selfTimedCheck_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_selfTimedCheck_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.selfTimedCheck_actionPerformed(e);
	}
}

class StartPanel_randomCheck_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_randomCheck_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.randomCheck_actionPerformed(e);
	}
}

class StartPanel_edlCheck_actionAdapter implements java.awt.event.ActionListener {
	StartPanel adaptee;

	StartPanel_edlCheck_actionAdapter(StartPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.edlCheck_actionPerformed(e);
	}
}

class StartPanel_myEditDbButton_actionAdapter implements java.awt.event.ActionListener {
  StartPanel adaptee;

  StartPanel_myEditDbButton_actionAdapter(StartPanel adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.myEditDbButton_actionPerformed(e);
  }
}
