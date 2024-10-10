/*
*  $Id$
*/
package decodes.tsdb.algoedit;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.ResourceBundle;

import decodes.gui.GuiDialog;
import decodes.tsdb.algo.RoleTypes;
import decodes.tsdb.BadTimeSeriesException;
import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;

public class PropDialog extends GuiDialog
{
	private ResourceBundle labels = null;
	private ResourceBundle genericLabels = null;
	
	private JPanel mainPanel = null;
	private JLabel propNameLabel = null;
	private JTextField propNameField = null;
	private JLabel javaTypeLabel = null;
	private JLabel defaultLabel = null;
	private JComboBox javaTypeCombo = null;
	private JPanel buttonPanel = null;
	private JButton okButton = null;
	private JButton cancelButton = null;
	private JTextField defaultField = null;
	private BorderLayout topLayout = new BorderLayout();

	private boolean isOK=false;
	private AlgoProp theProp = null;
	private AlgoData algoData;

	public PropDialog(AlgoProp theProp, AlgoData algoData)
	{
		super(AlgorithmWizard.instance().getFrame(), "", true);

		labels = AlgorithmWizard.getLabels();
		genericLabels = AlgorithmWizard.getGenericLabels();
		
		this.setTitle(labels.getString("PropDialog.title"));
		
		this.theProp = theProp;
		this.algoData = algoData;
		
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
			GridBagConstraints propNameLabelConstraints = new GridBagConstraints();
			propNameLabelConstraints.gridx = 0;
			propNameLabelConstraints.anchor = java.awt.GridBagConstraints.EAST;
			propNameLabelConstraints.insets = new java.awt.Insets(10,15,5,2);
			propNameLabelConstraints.gridy = 0;

			propNameLabel = new JLabel();
			propNameLabel.setText(labels.getString("PropDialog.propertyName"));

			GridBagConstraints propNameFieldConstraints = new GridBagConstraints();
			propNameFieldConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			propNameFieldConstraints.gridy = 0;
			propNameFieldConstraints.weightx = 1.0;
			propNameFieldConstraints.insets = new java.awt.Insets(10,0,5,10);
			propNameFieldConstraints.gridx = 1;

			GridBagConstraints javaTypeLabelConstraints = new GridBagConstraints();
			javaTypeLabelConstraints.gridx = 0;
			javaTypeLabelConstraints.anchor = java.awt.GridBagConstraints.EAST;
			javaTypeLabelConstraints.insets = new java.awt.Insets(5,15,5,2);
			javaTypeLabelConstraints.gridy = 1;

			javaTypeLabel = new JLabel();
			javaTypeLabel.setText(labels.getString("PropDialog.javaType"));

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

			defaultLabel = new JLabel();
			defaultLabel.setText(labels.getString("PropDialog.defaultValue"));
			
			GridBagConstraints roleComboConstraints = new GridBagConstraints();
			roleComboConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
			roleComboConstraints.weightx = 1.0;
			roleComboConstraints.insets = new java.awt.Insets(5,0,10,10);
			roleComboConstraints.gridx = 1;
			roleComboConstraints.gridy = 2;

			mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());
			mainPanel.add(propNameLabel, propNameLabelConstraints);
			mainPanel.add(getRoleNameField(), propNameFieldConstraints);
			mainPanel.add(javaTypeLabel, javaTypeLabelConstraints);
			mainPanel.add(getJavaTypeCombo(), javaTypeComboConstraints);
			mainPanel.add(defaultLabel, roleLabelConstraints);
			mainPanel.add(getDefaultField(), roleComboConstraints);
		}
		return mainPanel;
	}

	/**
	 * This method initializes propNameField	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getRoleNameField() 
	{
		if (propNameField == null) 
		{
			propNameField = new JTextField();
			propNameField.setText(theProp.name);
		}
		return propNameField;
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
			javaTypeCombo = new JComboBox(
				new String[] { "double", "long", "String", "boolean" });
			javaTypeCombo.setToolTipText(
					labels.getString("PropDialog.javaTypeComboTT"));
			javaTypeCombo.setSelectedItem(theProp.javaType);
		}
		return javaTypeCombo;
	}

	private JTextField getDefaultField() 
	{
		if (defaultField == null) 
		{
			defaultField = new JTextField();
			defaultField.setToolTipText(
					labels.getString("PropDialog.defaultValueTT"));
			defaultField.setText(theProp.defaultValue);
		}
		return defaultField;
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
		String propName = propNameField.getText();
		try { propName = algoData.validateName(propName); }
		catch(BadTimeSeriesException ex)
		{
			showError(labels.getString("PropDialog.invalidPropertyName")
					+ ex.getMessage());
		}

		for(InputTimeSeries its : algoData.getAllInputTimeSeries())
			if (its.roleName.equalsIgnoreCase(propName))
			{
				showError(LoadResourceBundle.sprintf(
						labels.getString("PropDialog.propNameExistsErr"),
						propName));
				return;
			}
		for(String outName : algoData.getAllOutputTimeSeries())
			if (outName.equalsIgnoreCase(propName))
			{
				showError(LoadResourceBundle.sprintf(
				labels.getString("InputTimeSeriesDialog.outputNameExistsErr"),
				propName));
				return;
			}
		for(AlgoProp ap : algoData.getAllAlgoProps())
			if (theProp != ap && ap.name.equalsIgnoreCase(propName))
			{
				showError(LoadResourceBundle.sprintf(
				labels.getString("InputTimeSeriesDialog.propertyNameExistsErr"),
				propName));
				return;
			}

		String jtype = (String)javaTypeCombo.getSelectedItem();

		String v = defaultField.getText().trim();
		if (v.length() == 0 && !jtype.equals("String"))
		{
			showError(labels.getString("PropDialog.defaultValueErr"));
			return;
		}

		if (jtype.equals("long"))
		{
			if (!v.equals("Long.MAX_VALUE") && !v.equals("Long.MIN_VALUE"))
			{
				try { long x = Long.parseLong(v); }
				catch(NumberFormatException ex)
				{
					showError(LoadResourceBundle.sprintf(
							labels.getString("PropDialog.defaultIntegerErr"),
							v));
					return;
				}
			}
		}
		else if (jtype.equals("double"))
		{
			if (!v.equals("Double.MAX_VALUE") 
			 && !v.equals("Double.MIN_VALUE")
			 && !v.equals("Double.NEGATIVE_INFINITY"))
			{
				try { double x = Double.parseDouble(v); }
				catch(NumberFormatException ex)
				{
					showError(LoadResourceBundle.sprintf(
							labels.getString("PropDialog.defaultDoubleErr"),
							v));
					return;
				}
			}
		}
		else if (jtype.equals("boolean"))
		{
			boolean tf = TextUtil.str2boolean(v);
			v = "" + tf;
		}
		else // String
		{
			if (v.length() == 0)
				v = "\"\"";
			else
			{
				if (!v.startsWith("\""))
					v = "\"" + v;
				if (!v.endsWith("\""))
					v = v + "\"";
			}
		}

		theProp.name = propName;
		theProp.javaType = jtype;
		theProp.defaultValue = v;

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
