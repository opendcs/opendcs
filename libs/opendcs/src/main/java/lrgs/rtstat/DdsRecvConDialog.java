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
import decodes.gui.GuiDialog;
import lrgs.ddsrecv.DdsRecvConnectCfg;

public class DdsRecvConDialog
	extends GuiDialog
{
	private static ResourceBundle labels = 
		RtStat.getLabels();
	private static ResourceBundle genericLabels = 
		RtStat.getGenericLabels();
	private JPanel ddsRecvConnEditDialogPane = null;
	private JPanel ddsDialogCenterPane = null;
	private JTextField ddsDialogNumberField = null;
	private JCheckBox ddsDialogEnabledCheck = null;
	private JTextField ddsDialogHostField = null;
	private JTextField ddsDialogNameField = null;
	private JTextField ddsDialogPortField = null;
	private JTextField ddsDialogUserField = null;
	private JCheckBox ddsDialogPasswordCheck = null;
	private JCheckBox ddsDialogDomsatCheck = null;
	private JPanel ddsDialogButtonPane = null;
	private JButton ddsDialogOKButton = null;
	private JButton ddsDialogCancelButton = null;
	private JLabel ddsDialogNumberLabel = null;
	private JLabel ddsDialogNameLabel = null;
	private JLabel ddsDialogHostLabel = null;
	private JLabel ddsDialogPortLabel = null;
	private JLabel ddsDialogUserLabel = null;
	private JCheckBox acceptARMsCheck = new JCheckBox();
	
	private JComboBox ddsGroupCombo = null;
	private JLabel ddsGrouplabel = null;

	private boolean wasOk = false;
	DdsRecvConnectCfg ddsConnectCfg = null;

	public DdsRecvConDialog(LrgsConfigDialog parent)
	{
		super(parent, labels.getString(
				"DdsRecvConDialog.title"), true);
		guiInit();
		pack();
	}

	private void guiInit()
	{
		ddsRecvConnEditDialogPane = new JPanel();
		ddsRecvConnEditDialogPane.setLayout(new BorderLayout());
		ddsDialogButtonPane = new JPanel();
		ddsRecvConnEditDialogPane.add(ddsDialogButtonPane, BorderLayout.SOUTH);
		ddsDialogCenterPane = new JPanel();
		ddsRecvConnEditDialogPane.add(ddsDialogCenterPane, BorderLayout.CENTER);
		getContentPane().add(ddsRecvConnEditDialogPane);

		GridBagConstraints ddsDialogPasswordCheckConstraints = 
			new GridBagConstraints();
		ddsDialogPasswordCheckConstraints.anchor = GridBagConstraints.WEST;
		ddsDialogPasswordCheckConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogPasswordCheckConstraints.gridx = 2;
		ddsDialogPasswordCheckConstraints.gridy = 4;

		GridBagConstraints ddsDialogUserFieldConstraints = 
			new GridBagConstraints();
		ddsDialogUserFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogUserFieldConstraints.gridy = 4;
		ddsDialogUserFieldConstraints.weightx = 1.0;
		ddsDialogUserFieldConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogUserFieldConstraints.gridx = 1;

		GridBagConstraints ddsDialogPortFieldConstraints = 
			new GridBagConstraints();
		ddsDialogPortFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogPortFieldConstraints.gridy = 3;
		ddsDialogPortFieldConstraints.weightx = 1.0;
		ddsDialogPortFieldConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogPortFieldConstraints.gridx = 1;

		GridBagConstraints ddsDialogNameFieldConstraints = 
			new GridBagConstraints();
		ddsDialogNameFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogNameFieldConstraints.gridy = 1;
		ddsDialogNameFieldConstraints.weightx = 1.0;
		ddsDialogNameFieldConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogNameFieldConstraints.gridx = 1;

		GridBagConstraints ddsDialogHostFieldConstraints = 
			new GridBagConstraints();
		ddsDialogHostFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogHostFieldConstraints.gridy = 2;
		ddsDialogHostFieldConstraints.weightx = 1.0;
		ddsDialogHostFieldConstraints.gridwidth = 2;
		ddsDialogHostFieldConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogHostFieldConstraints.gridx = 1;

		GridBagConstraints ddsDialogGroupComboConstraints = 
			new GridBagConstraints();
		ddsDialogGroupComboConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogGroupComboConstraints.gridy = 5;
		ddsDialogGroupComboConstraints.weightx = 1.0;
		ddsDialogGroupComboConstraints.gridwidth = 2;
		ddsDialogGroupComboConstraints.insets = new Insets(2, 0, 2, 10);
		ddsDialogGroupComboConstraints.gridx = 1;
		
		GridBagConstraints ddsDialogEnabledCheckConstraints = 
			new GridBagConstraints();
		ddsDialogEnabledCheckConstraints.anchor = GridBagConstraints.WEST;
		ddsDialogEnabledCheckConstraints.insets = new Insets(5, 0, 2, 10);
		ddsDialogEnabledCheckConstraints.gridx = 2;
		ddsDialogEnabledCheckConstraints.weightx = 1.0D;
		ddsDialogEnabledCheckConstraints.gridy = 0;

		GridBagConstraints ddsDialogNumberFieldConstraints = 
			new GridBagConstraints();
		ddsDialogNumberFieldConstraints.fill = GridBagConstraints.HORIZONTAL;
		ddsDialogNumberFieldConstraints.gridy = 0;
		ddsDialogNumberFieldConstraints.weightx = 1.0;
		ddsDialogNumberFieldConstraints.insets = new Insets(5, 0, 2, 10);
		ddsDialogNumberFieldConstraints.gridx = 1;
		ddsDialogNumberFieldConstraints.ipadx = 80;

		GridBagConstraints ddsDialogUserLabelConstraints = 
			new GridBagConstraints();
		ddsDialogUserLabelConstraints.gridx = 0;
		ddsDialogUserLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogUserLabelConstraints.insets = new Insets(2, 2, 2, 2);
		ddsDialogUserLabelConstraints.gridy = 4;
		ddsDialogUserLabel = new JLabel();
		ddsDialogUserLabel.setText(labels.getString(
				"EditUserDialog.DDSUserName"));

		GridBagConstraints ddsDialogPortLabelConstraints = 
			new GridBagConstraints();
		ddsDialogPortLabelConstraints.gridx = 0;
		ddsDialogPortLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogPortLabelConstraints.insets = new Insets(2, 2, 2, 2);
		ddsDialogPortLabelConstraints.gridy = 3;
		ddsDialogPortLabel = new JLabel();
		ddsDialogPortLabel.setText(labels.getString(
				"DdsRecvConDialog.TCPPort"));

		GridBagConstraints ddsDialogHostLabelConstraints = 
			new GridBagConstraints();
		ddsDialogHostLabelConstraints.gridx = 0;
		ddsDialogHostLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogHostLabelConstraints.insets = new Insets(2, 2, 2, 2);
		ddsDialogHostLabelConstraints.gridy = 2;
		ddsDialogHostLabel = new JLabel();
		ddsDialogHostLabel.setText(labels.getString(
				"DdsRecvConDialog.hostIpAddr"));

		GridBagConstraints ddsDialogNameLabelConstraints = 
			new GridBagConstraints();
		ddsDialogNameLabelConstraints.gridx = 0;
		ddsDialogNameLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogNameLabelConstraints.insets = new Insets(2, 2, 2, 2);
		ddsDialogNameLabelConstraints.gridy = 1;
		ddsDialogNameLabel = new JLabel();
		ddsDialogNameLabel.setText(labels.getString(
				"DdsRecvConDialog.connectionName"));

		GridBagConstraints ddsDialogNumberLabelConstraints = 
			new GridBagConstraints();
		ddsDialogNumberLabelConstraints.gridx = 0;
		ddsDialogNumberLabelConstraints.insets = new Insets(5, 10, 2, 2);
		ddsDialogNumberLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogNumberLabelConstraints.gridy = 0;
		ddsDialogNumberLabel = new JLabel();
		ddsDialogNumberLabel.setText(labels.getString(
				"DdsRecvConDialog.connectionNum"));

		GridBagConstraints ddsDialogDomsatCheckConstraints = 
			new GridBagConstraints(0, 6, 3, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0);

		GridBagConstraints ddsDialogGroupLabelConstraints = 
			new GridBagConstraints();
		ddsDialogGroupLabelConstraints.gridx = 0;
		ddsDialogGroupLabelConstraints.anchor = GridBagConstraints.EAST;
		ddsDialogGroupLabelConstraints.insets = new Insets(2, 2, 2, 2);
		ddsDialogGroupLabelConstraints.gridy = 5;
		ddsGrouplabel = new JLabel();
		ddsGrouplabel.setText(labels.getString(
				"DdsRecvConDialog.DDSGroup"));
		
		ddsDialogCenterPane.setLayout(new GridBagLayout());
		ddsDialogCenterPane.add(ddsDialogNumberLabel, ddsDialogNumberLabelConstraints);
		ddsDialogCenterPane.add(ddsDialogNameLabel, ddsDialogNameLabelConstraints);
		ddsDialogCenterPane.add(ddsDialogHostLabel, ddsDialogHostLabelConstraints);
		ddsDialogCenterPane.add(ddsDialogPortLabel, ddsDialogPortLabelConstraints);
		ddsDialogCenterPane.add(ddsDialogUserLabel, ddsDialogUserLabelConstraints);
		ddsDialogCenterPane.add(ddsGrouplabel, ddsDialogGroupLabelConstraints);
		ddsDialogCenterPane.add(getDdsDialogNumberField(), ddsDialogNumberFieldConstraints);
		ddsDialogCenterPane.add(getDdsDialogEnabledCheck(), ddsDialogEnabledCheckConstraints);
		ddsDialogCenterPane.add(getDdsDialogHostField(), ddsDialogHostFieldConstraints);
		ddsDialogCenterPane.add(getDdsDialogNameField(), ddsDialogNameFieldConstraints);
		ddsDialogCenterPane.add(getDdsDialogPortField(), ddsDialogPortFieldConstraints);
		ddsDialogCenterPane.add(getDdsDialogUserField(), ddsDialogUserFieldConstraints);
		ddsDialogCenterPane.add(getDdsGroupCombo(), ddsDialogGroupComboConstraints);
		ddsDialogCenterPane.add(getDdsDialogPasswordCheck(), ddsDialogPasswordCheckConstraints);
		ddsDialogCenterPane.add(getDdsDialogDomsatCheck(), ddsDialogDomsatCheckConstraints);

		acceptARMsCheck.setText("Accept Abnormal Response Messages?");
		ddsDialogCenterPane.add(acceptARMsCheck, 
			new GridBagConstraints(0, 7, 3, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 10), 0, 0));

		ddsDialogButtonPane.setLayout(new FlowLayout());
		ddsDialogButtonPane.add(getDdsDialogOKButton(), null);
		ddsDialogButtonPane.add(getDdsDialogCancelButton(), null);
	}

	/**
	 * This method initializes ddsDialogNumberField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsDialogNumberField()
	{
		if (ddsDialogNumberField == null)
		{
			ddsDialogNumberField = new JTextField();
		}
		return ddsDialogNumberField;
	}

	/**
	 * This method initializes ddsDialogEnabledCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDdsDialogEnabledCheck()
	{
		if (ddsDialogEnabledCheck == null)
		{
			ddsDialogEnabledCheck = new JCheckBox();
			ddsDialogEnabledCheck.setText(labels.getString(
					"DdsRecvConDialog.enabled"));
		}
		return ddsDialogEnabledCheck;
	}

	/**
	 * This method initializes ddsDialogHostField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsDialogHostField()
	{
		if (ddsDialogHostField == null)
		{
			ddsDialogHostField = new JTextField();
		}
		return ddsDialogHostField;
	}

	/**
	 * This method initializes ddsDialogNameField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsDialogNameField()
	{
		if (ddsDialogNameField == null)
		{
			ddsDialogNameField = new JTextField();
		}
		return ddsDialogNameField;
	}

	/**
	 * This method initializes ddsDialogPortField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsDialogPortField()
	{
		if (ddsDialogPortField == null)
		{
			ddsDialogPortField = new JTextField();
		}
		return ddsDialogPortField;
	}

	/**
	 * This method initializes ddsDialogUserField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getDdsDialogUserField()
	{
		if (ddsDialogUserField == null)
		{
			ddsDialogUserField = new JTextField();
		}
		return ddsDialogUserField;
	}

	/**
	 * This method initializes ddsDialogPasswordCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDdsDialogPasswordCheck()
	{
		if (ddsDialogPasswordCheck == null)
		{
			ddsDialogPasswordCheck = new JCheckBox();
			ddsDialogPasswordCheck.setText(
				labels.getString("DdsRecvConDialog.usePassword"));
		}
		return ddsDialogPasswordCheck;
	}

	
	/**
	 * This method initializes ddsGroup Combobox
	 * 	
	 * @return javax.swing.JComboBox	
	 */
	private JComboBox getDdsGroupCombo()
	{
		if (ddsGroupCombo == null)
		{
			ddsGroupCombo = new JComboBox();
			ddsGroupCombo.addItem("Primary");
			ddsGroupCombo.addItem("Secondary");	
		}
		return ddsGroupCombo;
	}
	/**
	 * This method initializes ddsDialogDomsatCheck	
	 * 	
	 * @return javax.swing.JCheckBox	
	 */
	private JCheckBox getDdsDialogDomsatCheck()
	{
		if (ddsDialogDomsatCheck == null)
		{
			ddsDialogDomsatCheck = new JCheckBox();
			ddsDialogDomsatCheck.setText(
					labels.getString("DdsRecvConDialog.seqNumbers"));
		}
		return ddsDialogDomsatCheck;
	}

	/**
	 * This method initializes ddsDialogOKButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDdsDialogOKButton() 
	{
		if (ddsDialogOKButton == null) 
		{
			ddsDialogOKButton = new JButton();
			ddsDialogOKButton.setText(genericLabels.getString("OK"));
			ddsDialogOKButton.setPreferredSize(new Dimension(82, 26));
			ddsDialogOKButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okButtonPressed();
				}
			});
		}
		return ddsDialogOKButton;
	}

	/**
	 * This method initializes ddsDialogCancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getDdsDialogCancelButton()
	{
		if (ddsDialogCancelButton == null)
		{
			ddsDialogCancelButton = new JButton();
			ddsDialogCancelButton.setText(genericLabels.getString("cancel"));
			ddsDialogCancelButton.setPreferredSize(new Dimension(82, 26));
			ddsDialogCancelButton.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelButtonPressed();
				}
			});
		}
		return ddsDialogCancelButton;
	}

	public boolean okPressed()
	{
		return wasOk;
	}

	private void okButtonPressed()
	{
		wasOk = copyBackToObject();
		if (wasOk)
		{
			setVisible(false);
		}
	}

	private void cancelButtonPressed()
	{
		setVisible(false);
		wasOk = false;
	}

	public void setInfo(DdsRecvConnectCfg cfg)
	{
		this.ddsConnectCfg = cfg;
		ddsDialogNumberField.setText("" + cfg.connectNum);
		ddsDialogNumberField.setEditable(false);
		ddsDialogNameField.setText(cfg.name);
		ddsDialogPortField.setText("" + cfg.port);
		ddsDialogHostField.setText(cfg.host);
		ddsDialogEnabledCheck.setSelected(cfg.enabled);
		ddsDialogUserField.setText("" + cfg.username);
		ddsDialogPasswordCheck.setSelected(cfg.authenticate);
		ddsDialogDomsatCheck.setSelected(cfg.hasDomsatSeqNums);
		acceptARMsCheck.setSelected(cfg.acceptARMs);
		ddsGroupCombo.setSelectedItem(cfg.group);
	}

	/**
	 * Copies the data in the controls back tothe object.
	 * @return true if successful.
	 */
	private boolean copyBackToObject()
	{
		int port = -1;
		try 
		{
			port = Integer.parseInt(ddsDialogPortField.getText().trim());
		}
		catch(NumberFormatException ex)
		{
			showError(labels.getString("DdsRecvConDialog.DDSPortErr"));
			return false;
		}
		String nm = ddsDialogNameField.getText().trim();
		String host = ddsDialogHostField.getText().trim();
		String username = ddsDialogUserField.getText().trim();
		if (nm.length() == 0 || host.length() == 0 || username.length() == 0)
		{
			showError(labels.getString(
					"DdsRecvConDialog.reqFieldsErr"));
			return false;
		}

		ddsConnectCfg.port = port;
		ddsConnectCfg.name = nm;
		ddsConnectCfg.host = host;
		ddsConnectCfg.enabled = ddsDialogEnabledCheck.isSelected();
		ddsConnectCfg.username = username;
		ddsConnectCfg.authenticate = ddsDialogPasswordCheck.isSelected();
		ddsConnectCfg.hasDomsatSeqNums = ddsDialogDomsatCheck.isSelected();
		ddsConnectCfg.acceptARMs = acceptARMsCheck.isSelected();
		ddsConnectCfg.group = (String)ddsGroupCombo.getSelectedItem();
		return true;
	}
}
