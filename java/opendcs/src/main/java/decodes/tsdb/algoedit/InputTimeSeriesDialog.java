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
package decodes.tsdb.algoedit;

import ilex.util.LoadResourceBundle;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.ResourceBundle;

import decodes.gui.GuiDialog;
import decodes.tsdb.algo.RoleTypes;
import decodes.tsdb.BadTimeSeriesException;

public class InputTimeSeriesDialog extends GuiDialog
{
	private ResourceBundle labels = null;
	private ResourceBundle genericLabels = null;
	
	private JPanel mainPanel = null;
	private JLabel roleNameLabel = null;
	private JTextField roleNameField = null;
	private JLabel javaTypeLabel = null;
	private JLabel roleTypeLabel = null;
	private JComboBox javaTypeCombo = null;
	private JPanel buttonPanel = null;
	private JButton okButton = null;
	private JButton cancelButton = null;
	private JComboBox roleTypeCombo = null;
	private BorderLayout topLayout = new BorderLayout();

	private boolean isOK=false;
	private InputTimeSeries inputTS=null;
	private AlgoData algoData;

	public InputTimeSeriesDialog(InputTimeSeries inputTS, AlgoData algoData)
	{
		super(AlgorithmWizard.instance().getFrame(), "", true);

		this.inputTS = inputTS;
		this.algoData = algoData;
		
		labels = AlgorithmWizard.getLabels();
		genericLabels = AlgorithmWizard.getGenericLabels();

		this.setTitle(labels.getString("InputTimeSeriesDialog.title"));

		getContentPane().setLayout(topLayout);
		getContentPane().add(getMainPanel(), BorderLayout.CENTER);
		getContentPane().add(getButtonPanel(), BorderLayout.SOUTH);
		pack();
		getRootPane().setDefaultButton(okButton);
	}
	
	/**
	 * This method initializes mainPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getMainPanel() 
	{
		if (mainPanel == null) 
		{
			GridBagConstraints roleNameLabelConstraints = new GridBagConstraints();
			roleNameLabelConstraints.gridx = 0;
			roleNameLabelConstraints.anchor = java.awt.GridBagConstraints.EAST;
			roleNameLabelConstraints.insets = new java.awt.Insets(10,15,5,2);
			roleNameLabelConstraints.gridy = 0;

			roleNameLabel = new JLabel();
			roleNameLabel.setText(
					labels.getString("InputTimeSeriesDialog.roleName"));

			GridBagConstraints roleNameFieldConstraints = 
				new GridBagConstraints();
			roleNameFieldConstraints.fill = 
				java.awt.GridBagConstraints.HORIZONTAL;
			roleNameFieldConstraints.gridy = 0;
			roleNameFieldConstraints.weightx = 1.0;
			roleNameFieldConstraints.insets = new java.awt.Insets(10,0,5,10);
			roleNameFieldConstraints.gridx = 1;

			GridBagConstraints javaTypeLabelConstraints = new GridBagConstraints();
			javaTypeLabelConstraints.gridx = 0;
			javaTypeLabelConstraints.anchor = java.awt.GridBagConstraints.EAST;
			javaTypeLabelConstraints.insets = new java.awt.Insets(5,15,5,2);
			javaTypeLabelConstraints.gridy = 1;

			javaTypeLabel = new JLabel();
			javaTypeLabel.setText(
					labels.getString("InputTimeSeriesDialog.javaType"));

			GridBagConstraints javaTypeComboConstraints = new GridBagConstraints();
			javaTypeComboConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			javaTypeComboConstraints.gridy = 1;
			javaTypeComboConstraints.weightx = 1.0;
			javaTypeComboConstraints.insets = new java.awt.Insets(5,0,5,10);
			javaTypeComboConstraints.gridx = 1;

			GridBagConstraints roleLabelConstraints = new GridBagConstraints();
			roleLabelConstraints.anchor = java.awt.GridBagConstraints.EAST;
			roleLabelConstraints.insets = new java.awt.Insets(5,10, 10, 2);
			roleLabelConstraints.gridx = 0;
			roleLabelConstraints.gridy = 2;

			roleTypeLabel = new JLabel();
			roleTypeLabel.setText(
					labels.getString("InputTimeSeriesDialog.roleTypeCode"));
			
			GridBagConstraints roleComboConstraints = new GridBagConstraints();
			roleComboConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			roleComboConstraints.weightx = 1.0;
			roleComboConstraints.insets = new java.awt.Insets(5,0,10,10);
			roleComboConstraints.gridx = 1;
			roleComboConstraints.gridy = 2;

			mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());
			mainPanel.add(roleNameLabel, roleNameLabelConstraints);
			mainPanel.add(getRoleNameField(), roleNameFieldConstraints);
			mainPanel.add(javaTypeLabel, javaTypeLabelConstraints);
			mainPanel.add(getJavaTypeCombo(), javaTypeComboConstraints);
			mainPanel.add(roleTypeLabel, roleLabelConstraints);
			mainPanel.add(getRoleTypeCombo(), roleComboConstraints);
		}
		return mainPanel;
	}

	/**
	 * This method initializes roleNameField	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getRoleNameField() 
	{
		if (roleNameField == null) 
		{
			roleNameField = new JTextField();
			roleNameField.setText(inputTS.roleName);
		}
		return roleNameField;
	}

	/**
	 * This method initializes javaTypeCombo	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JComboBox getJavaTypeCombo() 
	{
		if (javaTypeCombo == null) 
		{
			javaTypeCombo = new JComboBox();
			javaTypeCombo.addItem("double");
			javaTypeCombo.addItem("long");
			javaTypeCombo.addItem("String");
			javaTypeCombo.setToolTipText(
				labels.getString("InputTimeSeriesDialog.javaTypeComboTT"));
			javaTypeCombo.setSelectedItem(inputTS.javaType);
		}
		return javaTypeCombo;
	}

	/**
	 * This method initializes roleTypeCombo	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JComboBox getRoleTypeCombo() 
	{
		if (roleTypeCombo == null) 
		{
			roleTypeCombo = new JComboBox(RoleTypes.getExpandedRoleTypes());
			roleTypeCombo.setToolTipText(
				labels.getString("InputTimeSeriesDialog.roleTypeComboTT"));
			int idx = RoleTypes.getIndex(inputTS.roleTypeCode);
			if (idx != -1)
				roleTypeCombo.setSelectedIndex(idx);
		}
		return roleTypeCombo;
	}

	/**
	 * This method initializes buttonPanel	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getButtonPanel() 
	{
		if (buttonPanel == null) 
		{
			FlowLayout bpl = new FlowLayout();
			bpl.setVgap(10);
			bpl.setHgap(15);
			buttonPanel = new JPanel();
			buttonPanel.add(getOkButton(), null);
			buttonPanel.add(getCancelButton(), null);
		}
		return buttonPanel;
	}

	/**
	 * This method initializes okButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getOkButton() {
		if (okButton == null) {
			Action action = new AbstractAction("OK") {
		        public void actionPerformed(ActionEvent evt) {
		        	doOK();
		        }
		    };
			okButton = new JButton(action);
			okButton.setText(genericLabels.getString("OK"));//"  OK  "
		}
		return okButton;
	}

	public boolean isOK()
	{
		return isOK;
	}
	
	public void doOK()
	{
		String roleName = roleNameField.getText();
		try { roleName = algoData.validateName(roleName); }
		catch(BadTimeSeriesException ex)
		{
			showError(
				labels.getString("InputTimeSeriesDialog.invalidInputNameErr") 
				+ ex.getMessage());
		}

		for(InputTimeSeries its : algoData.getAllInputTimeSeries())
			if (inputTS != its && its.roleName.equalsIgnoreCase(roleName))
			{
				showError(LoadResourceBundle.sprintf(						
				labels.getString("InputTimeSeriesDialog.inputNameExistsErr"),
				roleName));
				return;
			}
		for(String outName : algoData.getAllOutputTimeSeries())
			if (outName.equalsIgnoreCase(roleName))
			{
				showError(LoadResourceBundle.sprintf(						
				labels.getString("InputTimeSeriesDialog.outputNameExistsErr"),
				roleName));
				return;
			}
		for(AlgoProp ap : algoData.getAllAlgoProps())
			if (ap.name.equalsIgnoreCase(roleName))
			{
				showError(LoadResourceBundle.sprintf(						
				labels.getString("InputTimeSeriesDialog.propertyNameExistsErr"),
				roleName));
				return;
			}
			
		inputTS.roleName = roleName;
		inputTS.javaType = (String)javaTypeCombo.getSelectedItem();
		int idx = roleTypeCombo.getSelectedIndex();
		if (idx == -1)
			idx = 0;
		inputTS.roleTypeCode = RoleTypes.roleTypes[idx];

		isOK = true;
		setVisible(false);
	}
	
	public void doCancel()
	{
		isOK=false;
		setVisible(false);
	}
	
	/**
	 * This method initializes cancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getCancelButton() 
	{
		if (cancelButton == null) 
		{
			Action action = 
				new AbstractAction("Cancel") 
				{
		        	public void actionPerformed(ActionEvent evt) 
					{
		        		doCancel();
		        	}
		    	};
			cancelButton = new JButton(action);
			cancelButton.setText(genericLabels.getString("cancel"));
		}
		return cancelButton;
	}
}
