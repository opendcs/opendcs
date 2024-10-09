/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package lrgs.rtstat;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.*;

import ilex.util.ByteUtil;
import decodes.gui.GuiDialog;
import lrgs.drgs.DrgsConnectCfg;

public class DrgsConDialog
	extends GuiDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private JPanel drgsDialogPane = null;
	private JPanel drgsDialogCenterPanel = null;
	private JPanel drgsDialogButtonPanel = null;
	private JButton drgsDialogOKButton = null;
	private JButton drgsDialogCancelButton = null;
	private JLabel drgsDialogNumberLabel = null;
	private JLabel drgsDialogNameLabel = null;
	private JLabel drgsDialogHostLabel = null;
	private JLabel drgsDialogPortLabel = null;
	private JLabel drgsDialogEventLabel = null;
	private JLabel drgsDialogPatternLabel = null;
	private JLabel drgsDialogChannelLabel = null;
	private JLabel emptyLabel7 = null;
	private JTextField drgsDialogNumberField = null;
	private JTextField drgsDialogNameField = null;
	private JTextField drgsDialogHostField = null;
	private JTextField drgsDialogPortField = null;
	private JCheckBox drgsDialogMessageCheck = null;
	private JTextField drgsDialogEventField = null;
	private JCheckBox drgsDialogEventCheck = null;
	private JTextField drgsDialogPatternField = null;
	private JTextField drgsDialogChannelField = null;
	private JTextField sourceField = new JTextField();

	private boolean wasOk = false;
	private DrgsConnectCfg drgsConnectCfg = null;

	public DrgsConDialog(LrgsConfigDialog parent)
	{
		super(parent, labels.getString(
				"DrgsConDialog.title"), true);
		guiInit();
		//setPreferredSize(new Dimension(260, 400));
		pack();
	}

	private void guiInit()
	{
		drgsDialogPane = new JPanel();
		getContentPane().add(drgsDialogPane);
		drgsDialogPane.setLayout(new BorderLayout());
		drgsDialogPane.add(getDrgsDialogButtonPanel(), BorderLayout.SOUTH);
		drgsDialogPane.add(getDrgsDialogCenterPanel(), BorderLayout.CENTER);
	}

	/**
	 * This method initializes drgsDialogCenterPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsDialogCenterPanel() 
	{
		if (drgsDialogCenterPanel == null) 
		{
			GridBagConstraints drgsDialogNumberLabelConstraints = new GridBagConstraints();
			drgsDialogNumberLabelConstraints.gridx = 0;
			drgsDialogNumberLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogNumberLabelConstraints.insets = new Insets(10, 40, 2, 2);
			drgsDialogNumberLabelConstraints.gridy = 0;
			drgsDialogNumberLabel = new JLabel();
			drgsDialogNumberLabel.setText(labels.getString(
					"DdsRecvConDialog.connectionNum"));

			GridBagConstraints drgsDialogNumberFieldConstraints = new GridBagConstraints();
			drgsDialogNumberFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogNumberFieldConstraints.gridy = 0;
			drgsDialogNumberFieldConstraints.weightx = 1.0;
			drgsDialogNumberFieldConstraints.insets = new Insets(10, 0, 2, 0);
			drgsDialogNumberFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogNameLabelConstraints = new GridBagConstraints();
			drgsDialogNameLabelConstraints.gridx = 0;
			drgsDialogNameLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogNameLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogNameLabelConstraints.gridy = 1;
			drgsDialogNameLabel = new JLabel();
			drgsDialogNameLabel.setText(labels.getString(
					"DdsRecvConDialog.connectionName"));

			GridBagConstraints drgsDialogNameFieldConstraints = new GridBagConstraints();
			drgsDialogNameFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogNameFieldConstraints.gridy = 1;
			drgsDialogNameFieldConstraints.weightx = 1.0;
			drgsDialogNameFieldConstraints.insets = new Insets(2, 0, 2, 0);
			drgsDialogNameFieldConstraints.gridx = 1;

			GridBagConstraints emptyLabel7Constraints = new GridBagConstraints();
			emptyLabel7Constraints.gridx = 2;
			emptyLabel7Constraints.weightx = 1.0D;
			emptyLabel7Constraints.fill = GridBagConstraints.BOTH;
			emptyLabel7Constraints.gridy = 1;
			emptyLabel7 = new JLabel();
			emptyLabel7.setText("");

			GridBagConstraints drgsDialogHostLabelConstraints = new GridBagConstraints();
			drgsDialogHostLabelConstraints.gridx = 0;
			drgsDialogHostLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogHostLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogHostLabelConstraints.gridy = 2;
			drgsDialogHostLabel = new JLabel();
			drgsDialogHostLabel.setText(labels.getString(
					"DdsRecvConDialog.hostIpAddr"));

			GridBagConstraints drgsDialogHostFieldConstraints = new GridBagConstraints();
			drgsDialogHostFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogHostFieldConstraints.gridy = 2;
			drgsDialogHostFieldConstraints.weightx = 1.0;
			drgsDialogHostFieldConstraints.gridwidth = 2;
			drgsDialogHostFieldConstraints.insets = new Insets(2, 0, 2, 10);
			drgsDialogHostFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogPortLabelConstraints = new GridBagConstraints();
			drgsDialogPortLabelConstraints.gridx = 0;
			drgsDialogPortLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogPortLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogPortLabelConstraints.gridy = 3;
			drgsDialogPortLabel = new JLabel();
			drgsDialogPortLabel.setText(labels.getString(
					"DrgsConDialog.messagePort"));

			GridBagConstraints drgsDialogPortFieldConstraints = new GridBagConstraints();
			drgsDialogPortFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogPortFieldConstraints.gridy = 3;
			drgsDialogPortFieldConstraints.weightx = 1.0;
			drgsDialogPortFieldConstraints.insets = new Insets(2, 0, 2, 0);
			drgsDialogPortFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogMessageCheckConstraints = new GridBagConstraints();
			drgsDialogMessageCheckConstraints.gridx = 2;
			drgsDialogMessageCheckConstraints.weightx = 1.0D;
			drgsDialogMessageCheckConstraints.gridwidth = 2;
			drgsDialogMessageCheckConstraints.anchor = GridBagConstraints.WEST;
			drgsDialogMessageCheckConstraints.gridy = 3;

			GridBagConstraints drgsDialogEventLabelConstraints = new GridBagConstraints();
			drgsDialogEventLabelConstraints.gridx = 0;
			drgsDialogEventLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogEventLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogEventLabelConstraints.gridy = 4;
			drgsDialogEventLabel = new JLabel();
			drgsDialogEventLabel.setText(labels.getString(
					"DrgsConDialog.eventPort"));

			GridBagConstraints drgsDialogEventFieldConstraints = new GridBagConstraints();
			drgsDialogEventFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogEventFieldConstraints.gridy = 4;
			drgsDialogEventFieldConstraints.weightx = 1.0;
			drgsDialogEventFieldConstraints.insets = new Insets(2, 0, 2, 0);
			drgsDialogEventFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogEventCheckConstraints = new GridBagConstraints();
			drgsDialogEventCheckConstraints.gridx = 2;
			drgsDialogEventCheckConstraints.anchor = GridBagConstraints.WEST;
			drgsDialogEventCheckConstraints.gridwidth = 2;
			drgsDialogEventCheckConstraints.gridy = 4;

			GridBagConstraints drgsDialogPatternLabelConstraints = new GridBagConstraints();
			drgsDialogPatternLabelConstraints.gridx = 0;
			drgsDialogPatternLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogPatternLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogPatternLabelConstraints.gridy = 5;
			drgsDialogPatternLabel = new JLabel();
			drgsDialogPatternLabel.setText(labels.getString(
					"DrgsConDialog.startPattern"));

			GridBagConstraints drgsDialogPatternFieldConstraints = new GridBagConstraints();
			drgsDialogPatternFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogPatternFieldConstraints.gridy = 5;
			drgsDialogPatternFieldConstraints.weightx = 1.0;
			drgsDialogPatternFieldConstraints.gridwidth = 1;
			drgsDialogPatternFieldConstraints.insets = new Insets(2, 0, 2, 0);
			drgsDialogPatternFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogChannelLabelConstraints = new GridBagConstraints();
			drgsDialogChannelLabelConstraints.gridx = 0;
			drgsDialogChannelLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogChannelLabelConstraints.insets = new Insets(2, 2, 2, 2);
			drgsDialogChannelLabelConstraints.gridy = 7;
			drgsDialogChannelLabel = new JLabel();
			drgsDialogChannelLabel.setText(labels.getString(
					"DrgsConDialog.chConfigFile"));

			
			GridBagConstraints drgsDialogChannelFieldConstraints = new GridBagConstraints();
			drgsDialogChannelFieldConstraints.fill = GridBagConstraints.BOTH;
			drgsDialogChannelFieldConstraints.gridy = 7;
			drgsDialogChannelFieldConstraints.weightx = 1.0;
			drgsDialogChannelFieldConstraints.gridwidth = 2;
			drgsDialogChannelFieldConstraints.insets = new Insets(2, 0, 2, 10);
			drgsDialogChannelFieldConstraints.gridx = 1;

			drgsDialogCenterPanel = new JPanel();
			drgsDialogCenterPanel.setLayout(new GridBagLayout());
			drgsDialogCenterPanel.add(drgsDialogNumberLabel, drgsDialogNumberLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogNumberField(), drgsDialogNumberFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogNameLabel, drgsDialogNameLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogNameField(), drgsDialogNameFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogHostLabel, drgsDialogHostLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogHostField(), drgsDialogHostFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogPortLabel, drgsDialogPortLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogPortField(), drgsDialogPortFieldConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogMessageCheck(), drgsDialogMessageCheckConstraints);
			drgsDialogCenterPanel.add(drgsDialogEventLabel, drgsDialogEventLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogEventField(), drgsDialogEventFieldConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogEventCheck(), drgsDialogEventCheckConstraints);
			drgsDialogCenterPanel.add(drgsDialogPatternLabel, drgsDialogPatternLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogPatternField(), drgsDialogPatternFieldConstraints);
			
			JLabel sourceLabel = new JLabel(labels.getString(
				"DrgsConDialog.sourceCode"));
			JLabel sourceTip = new JLabel(labels.getString(
				"DrgsConDialog.sourceTip"));
			
			drgsDialogCenterPanel.add(sourceLabel,
				new GridBagConstraints(0,6, 1,1, 0.0,0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE,
					new Insets(2, 2, 2, 2), 0, 0));
			drgsDialogCenterPanel.add(sourceField,
				new GridBagConstraints(1,6, 1,1, 0.0,0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(2, 2, 2, 2), 25, 0));
			drgsDialogCenterPanel.add(sourceTip,
				new GridBagConstraints(2,6, 1,1, 0.0,0.0,
					GridBagConstraints.WEST, GridBagConstraints.NONE,
					new Insets(2, 2, 2, 2), 0, 0));
			
			drgsDialogCenterPanel.add(drgsDialogChannelLabel, drgsDialogChannelLabelConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogChannelField(), drgsDialogChannelFieldConstraints);
			drgsDialogCenterPanel.add(emptyLabel7, emptyLabel7Constraints);
		}
		return drgsDialogCenterPanel;
	}

	/**
	 * This method initializes drgsDialogButtonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getDrgsDialogButtonPanel() {
		if (drgsDialogButtonPanel == null) {
			drgsDialogButtonPanel = new JPanel();
			drgsDialogButtonPanel.setLayout(new FlowLayout());
			drgsDialogButtonPanel.add(getDrgsDialogOKButton(), null);
			drgsDialogButtonPanel.add(getDrgsDialogCancelButton(), null);
		}
		return drgsDialogButtonPanel;
	}

	/**
	 * This method initializes drgsDialogOKButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDrgsDialogOKButton() {
		if (drgsDialogOKButton == null) {
			drgsDialogOKButton = new JButton();
			drgsDialogOKButton.setText(genericLabels.getString("OK"));
			drgsDialogOKButton.setPreferredSize(new Dimension(82, 26));
			drgsDialogOKButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okButtonPressed();
				}
			});
		}
		return drgsDialogOKButton;
	}

	/**
	 * This method initializes drgsDialogCancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDrgsDialogCancelButton() {
		if (drgsDialogCancelButton == null) {
			drgsDialogCancelButton = new JButton();
			drgsDialogCancelButton.setText(
					genericLabels.getString("cancel"));
			drgsDialogCancelButton.setPreferredSize(new Dimension(82, 26));
			drgsDialogCancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelButtonPressed();
				}
			});
		}
		return drgsDialogCancelButton;
	}

	/**
	 * This method initializes drgsDialogPortField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogPortField() {
		if (drgsDialogPortField == null) {
			drgsDialogPortField = new JTextField();
		}
		return drgsDialogPortField;
	}

	/**
	 * This method initializes drgsDialogMessageCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDrgsDialogMessageCheck() {
		if (drgsDialogMessageCheck == null) {
			drgsDialogMessageCheck = new JCheckBox();
			drgsDialogMessageCheck.setText(labels.getString(
					"DrgsConDialog.enableMsgRecv"));
		}
		return drgsDialogMessageCheck;
	}

	/**
	 * This method initializes drgsDialogChannelField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogChannelField() {
		if (drgsDialogChannelField == null) {
			drgsDialogChannelField = new JTextField();
		}
		return drgsDialogChannelField;
	}

	/**
	 * This method initializes drgsDialogPatternField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogPatternField() {
		if (drgsDialogPatternField == null) {
			drgsDialogPatternField = new JTextField();
		}
		return drgsDialogPatternField;
	}

	/**
	 * This method initializes drgsDialogEventField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogEventField() {
		if (drgsDialogEventField == null) {
			drgsDialogEventField = new JTextField();
			drgsDialogEventField.setPreferredSize(new Dimension(100, 20));
		}
		return drgsDialogEventField;
	}

	/**
	 * This method initializes drgsDialogHostField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogHostField() {
		if (drgsDialogHostField == null) {
			drgsDialogHostField = new JTextField();
		}
		return drgsDialogHostField;
	}

	/**
	 * This method initializes drgsDialogNameField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogNameField() {
		if (drgsDialogNameField == null) {
			drgsDialogNameField = new JTextField();
		}
		return drgsDialogNameField;
	}

	/**
	 * This method initializes drgsDialogNumberField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDrgsDialogNumberField() {
		if (drgsDialogNumberField == null) {
			drgsDialogNumberField = new JTextField();
		}
		return drgsDialogNumberField;
	}

	/**
	 * This method initializes drgsDialogEventCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDrgsDialogEventCheck() {
		if (drgsDialogEventCheck == null) {
			drgsDialogEventCheck = new JCheckBox();
			drgsDialogEventCheck.setText(labels.getString(
					"DrgsConDialog.enableEvntRecv"));
		}
		return drgsDialogEventCheck;
	}

	public boolean okPressed()
	{
		return wasOk;
	}

	private void okButtonPressed()
	{
		wasOk = copyBackToObject();
		if (wasOk)
			setVisible(false);
	}

	private void cancelButtonPressed()
	{
		wasOk = false;
		setVisible(false);
	}

	public void setInfo(DrgsConnectCfg cfg)
	{
		this.drgsConnectCfg = cfg;
		drgsDialogNumberField.setText("" + cfg.connectNum);
		drgsDialogNumberField.setEditable(false);
		drgsDialogNameField.setText(cfg.name);
		drgsDialogHostField.setText(cfg.host);
		drgsDialogPortField.setText("" + cfg.msgPort);
		drgsDialogMessageCheck.setSelected(cfg.msgEnabled);
		drgsDialogEventField.setText("" + cfg.evtPort);
		drgsDialogEventCheck.setSelected(cfg.evtEnabled);
		drgsDialogPatternField.setText(
			ByteUtil.toHexString(cfg.startPattern));
		drgsDialogChannelField.setText(cfg.cfgFile);
		if (cfg.drgsSourceCode != null 
		 && cfg.drgsSourceCode[0] != 0
		 && cfg.drgsSourceCode[1] != 0)
			sourceField.setText(new String(cfg.drgsSourceCode));
	}

	/**
	 * Copies the data in the controls back tothe object.
	 * @return true if successful.
	 */
	private boolean copyBackToObject()
	{
		int msgPort = -1;
		int evtPort = -1;
		try 
		{
			msgPort = Integer.parseInt(drgsDialogPortField.getText().trim());
		}
		catch(NumberFormatException ex)
		{
			showError(labels.getString(
					"DrgsConDialog.msgPortNumErr"));
			return false;
		}
		try
		{
			evtPort = Integer.parseInt(drgsDialogEventField.getText().trim());
		}
		catch(NumberFormatException ex)
		{
			showError(labels.getString(
					"DrgsConDialog.evntPortNumErr"));
			return false;
		}

		String s = drgsDialogPatternField.getText().trim();
		for(int i=0; i<s.length(); i++)
			if (!ByteUtil.isHexChar((byte)s.charAt(i)))
			{
				showError(labels.getString(
				"DrgsConDialog.startPattErr"));
				return false;
			}
		drgsConnectCfg.name = drgsDialogNameField.getText().trim();
		drgsConnectCfg.host = drgsDialogHostField.getText().trim();
		drgsConnectCfg.msgEnabled = drgsDialogMessageCheck.isSelected();
		drgsConnectCfg.cfgFile = drgsDialogChannelField.getText().trim();
		drgsConnectCfg.msgPort = msgPort;
		drgsConnectCfg.evtPort = evtPort;
		drgsConnectCfg.evtEnabled = drgsDialogEventCheck.isSelected();
		drgsConnectCfg.startPattern = ByteUtil.fromHexString(
			drgsDialogPatternField.getText().trim());
		s = sourceField.getText().trim();
		if (s.length() > 0)
		{
			if (s.length() != 2)
			{
				showError(labels.getString("DrgsConDialog.badSourceCode"));
				return false;
			}
			drgsConnectCfg.drgsSourceCode[0] = (byte)s.charAt(0);
			drgsConnectCfg.drgsSourceCode[1] = (byte)s.charAt(1);
		}
		return true;
	}
}
