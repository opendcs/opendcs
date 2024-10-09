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

public class NetworkDcpDialog
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
	private JLabel drgsDialogPatternLabel = null;
	private JTextField drgsDialogNumberField = new JTextField();
	private JTextField drgsDialogNameField = new JTextField();
	private JTextField drgsDialogHostField = new JTextField();
	private JTextField drgsDialogPortField = new JTextField();
	private JCheckBox drgsDialogMessageCheck = null;
	private JTextField drgsDialogPatternField = new JTextField();
	private String[] periodChoices = { "Continuous", "5 min", "10 min",
		"15 min", "20 min", "30 min", "1 hr", "2 hr", "3 hr", "4 hr" };
	private int[] periodValues = { 0, 5, 10, 15, 20, 30, 60, 120,
		180, 240 };
	private JLabel periodLabel = new JLabel("Polling Period:");
	private JComboBox periodCombo = new JComboBox(periodChoices);

	private boolean wasOk = false;
	private DrgsConnectCfg drgsConnectCfg = null;

	public NetworkDcpDialog(LrgsConfigDialog parent)
	{
		super(parent, 
			labels.getString("LrgsConfigDialog.networkDcpDlgTitle"), true);
		guiInit();
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

	private JPanel getDrgsDialogCenterPanel()
	{
		if (drgsDialogCenterPanel == null)
		{
			GridBagConstraints drgsDialogNumberLabelConstraints = 
				new GridBagConstraints();
			drgsDialogNumberLabelConstraints.gridx = 0;
			drgsDialogNumberLabelConstraints.anchor = 
				GridBagConstraints.SOUTHEAST;
			drgsDialogNumberLabelConstraints.insets = new Insets(10, 20, 4, 2);
			drgsDialogNumberLabelConstraints.gridy = 0;
			drgsDialogNumberLabelConstraints.weighty = .5;
			drgsDialogNumberLabel = new JLabel();
			drgsDialogNumberLabel.setText(labels
			    .getString("DdsRecvConDialog.connectionNum"));

			GridBagConstraints drgsDialogNumberFieldConstraints = 
				new GridBagConstraints();
			drgsDialogNumberFieldConstraints.fill = 
				GridBagConstraints.HORIZONTAL;
			drgsDialogNumberFieldConstraints.anchor = 
				GridBagConstraints.SOUTHWEST;
			drgsDialogNumberFieldConstraints.gridy = 0;
			drgsDialogNumberFieldConstraints.weightx = 1.0;
			drgsDialogNumberFieldConstraints.weighty = .5;
			drgsDialogNumberFieldConstraints.insets = new Insets(10, 0, 4, 0);
			drgsDialogNumberFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogNameLabelConstraints = 
				new GridBagConstraints();
			drgsDialogNameLabelConstraints.gridx = 0;
			drgsDialogNameLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogNameLabelConstraints.insets = new Insets(4, 20, 4, 2);
			drgsDialogNameLabelConstraints.gridy = 1;
			drgsDialogNameLabel = new JLabel();
			drgsDialogNameLabel.setText(labels
			    .getString("DdsRecvConDialog.connectionName"));

			GridBagConstraints drgsDialogNameFieldConstraints = 
				new GridBagConstraints();
			drgsDialogNameFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			drgsDialogNameFieldConstraints.gridy = 1;
			drgsDialogNameFieldConstraints.weightx = 1.0;
			drgsDialogNameFieldConstraints.insets = new Insets(4, 0, 4, 0);
			drgsDialogNameFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogHostLabelConstraints = 
				new GridBagConstraints();
			drgsDialogHostLabelConstraints.gridx = 0;
			drgsDialogHostLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogHostLabelConstraints.insets = new Insets(4, 20, 4, 2);
			drgsDialogHostLabelConstraints.gridy = 2;
			drgsDialogHostLabel = new JLabel();
			drgsDialogHostLabel.setText(labels
			    .getString("DdsRecvConDialog.hostIpAddr"));
			
			GridBagConstraints drgsDialogHostFieldConstraints = 
				new GridBagConstraints();
			drgsDialogHostFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			drgsDialogHostFieldConstraints.gridy = 2;
			drgsDialogHostFieldConstraints.weightx = 1.0;
			drgsDialogHostFieldConstraints.gridwidth = 2;
			drgsDialogHostFieldConstraints.insets = new Insets(4, 0, 4, 0);
			drgsDialogHostFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogPortLabelConstraints = 
				new GridBagConstraints();
			drgsDialogPortLabelConstraints.gridx = 0;
			drgsDialogPortLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogPortLabelConstraints.insets = new Insets(4, 20, 4, 2);
			drgsDialogPortLabelConstraints.gridy = 3;
			drgsDialogPortLabel = new JLabel();
			drgsDialogPortLabel.setText(labels
			    .getString("DrgsConDialog.messagePort"));
			
			GridBagConstraints drgsDialogPortFieldConstraints = 
				new GridBagConstraints();
			drgsDialogPortFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			drgsDialogPortFieldConstraints.gridy = 3;
			drgsDialogPortFieldConstraints.weightx = 1.0;
			drgsDialogPortFieldConstraints.insets = new Insets(4, 0, 4, 0);
			drgsDialogPortFieldConstraints.gridx = 1;

			GridBagConstraints drgsDialogMessageCheckConstraints = 
				new GridBagConstraints();
			drgsDialogMessageCheckConstraints.gridx = 2;
			drgsDialogMessageCheckConstraints.weightx = 1.0D;
			drgsDialogMessageCheckConstraints.gridwidth = 2;
			drgsDialogMessageCheckConstraints.anchor = GridBagConstraints.WEST;
			drgsDialogMessageCheckConstraints.gridy = 3;
			drgsDialogMessageCheckConstraints.insets = new Insets(4, 3, 4, 15);

			GridBagConstraints drgsDialogPatternLabelConstraints = 
				new GridBagConstraints();
			drgsDialogPatternLabelConstraints.gridx = 0;
			drgsDialogPatternLabelConstraints.anchor = GridBagConstraints.EAST;
			drgsDialogPatternLabelConstraints.insets = new Insets(4, 20, 4, 2);
			drgsDialogPatternLabelConstraints.gridy = 4;
			drgsDialogPatternLabel = new JLabel();
			drgsDialogPatternLabel.setText(labels
			    .getString("DrgsConDialog.startPattern"));

			GridBagConstraints drgsDialogPatternFieldConstraints = 
				new GridBagConstraints();
			drgsDialogPatternFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			drgsDialogPatternFieldConstraints.gridy = 4;
			drgsDialogPatternFieldConstraints.weightx = 1.0;
			drgsDialogPatternFieldConstraints.gridwidth = 1;
			drgsDialogPatternFieldConstraints.insets = new Insets(4, 0, 4, 0);
			drgsDialogPatternFieldConstraints.gridx = 1;

			drgsDialogCenterPanel = new JPanel();
			drgsDialogCenterPanel.setLayout(new GridBagLayout());
			drgsDialogCenterPanel.add(drgsDialogNumberLabel,
			    drgsDialogNumberLabelConstraints);
			drgsDialogCenterPanel.add(drgsDialogNameLabel,
			    drgsDialogNameLabelConstraints);
			drgsDialogCenterPanel.add(drgsDialogHostLabel,
			    drgsDialogHostLabelConstraints);
			drgsDialogCenterPanel.add(drgsDialogPortLabel,
			    drgsDialogPortLabelConstraints);
			drgsDialogCenterPanel.add(drgsDialogPatternLabel,
			    drgsDialogPatternLabelConstraints);
			drgsDialogCenterPanel.add(drgsDialogPortField,
			    drgsDialogPortFieldConstraints);
			drgsDialogCenterPanel.add(getDrgsDialogMessageCheck(),
			    drgsDialogMessageCheckConstraints);
			drgsDialogCenterPanel.add(drgsDialogPatternField,
			    drgsDialogPatternFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogHostField,
			    drgsDialogHostFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogNameField,
			    drgsDialogNameFieldConstraints);
			drgsDialogCenterPanel.add(drgsDialogNumberField,
			    drgsDialogNumberFieldConstraints);

			drgsDialogCenterPanel.add(periodLabel,
				new GridBagConstraints(0, 5, 1, 1, 0.5, 1.0,
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
					new Insets(4, 20, 4, 2), 0, 0));
			drgsDialogCenterPanel.add(periodCombo,
				new GridBagConstraints(1, 5, 1, 1, 0.5, 1.0,
					GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
					new Insets(4, 0, 4, 0), 0, 0));
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
		drgsDialogPatternField.setText(
			ByteUtil.toHexString(cfg.startPattern));
		int closest = 0;
		for(int i=0; i<periodValues.length; i++)
			if (periodValues[i] == cfg.pollingPeriod)
			{
				closest = i;
				break;
			}
			else
			{
				int diff = cfg.pollingPeriod - periodValues[i];
				if (diff < 0) diff = -diff;
				int closestDiff = cfg.pollingPeriod - periodValues[closest];
				if (closestDiff < 0) closestDiff = -closestDiff;
				if (diff < closestDiff)
					closest = i;
			}
		periodCombo.setSelectedIndex(closest);
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
		drgsConnectCfg.msgPort = msgPort;
		drgsConnectCfg.evtPort = evtPort;
		drgsConnectCfg.startPattern = ByteUtil.fromHexString(
			drgsDialogPatternField.getText().trim());
		drgsConnectCfg.pollingPeriod = 
			periodValues[periodCombo.getSelectedIndex()];
		return true;
	}

}
