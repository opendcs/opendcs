/*
 *  $Id$
 */
package decodes.dbeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.EngineeringUnit;
//import decodes.db.EquipmentModel;
import decodes.gui.EUSelectDialog;
import decodes.gui.GuiDialog;

/**
 * A simple dialog for seleting a data type standard and code. Used in the
 * PresentationGroupEditPanel.
 */
@SuppressWarnings("serial")
public class EditPresentationDialog 
	extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private DataTypeCodeCombo standardCombo = new DataTypeCodeCombo();
	private JTextField codeField = new JTextField();
	private JTextField unitsField = new JTextField();
//	private JTextField equipmentModelField = new JTextField();
	private JComboBox maxDecCombo = new JComboBox(
		new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8" });
	private JTextField minValueField = new JTextField();
	private JTextField maxValueField = new JTextField();

	
	private DataPresentation editOb = null;
//	private EquipmentModel selectedEquipmentModel = null;
	private boolean cancelled = false;
	private boolean supportsMinMaxValue = false;

	public EditPresentationDialog(DataPresentation editOb, boolean supportsMinMaxValue)
	{
		super(DecodesDbEditor.getTheFrame(), "", true);
		this.supportsMinMaxValue = supportsMinMaxValue;
		try
		{
			jbInit();
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		this.editOb = editOb;
		fillFields(editOb);
	}

	/** JBuilder-generated method to initialize the GUI components */
	void jbInit() throws Exception
	{
		this.setTitle(dbeditLabels.getString("PresentationGroupEditPanel.editPETitle"));

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel paramPanel = new JPanel(new GridBagLayout());
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
		mainPanel.add(paramPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(mainPanel);
		
		JButton okButton = new JButton(genericLabels.getString("OK"));
		okButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton(genericLabels.getString("cancel"));
		cancelButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelPressed();
				}
			});
		buttonPanel.add(okButton, null);
		buttonPanel.add(cancelButton);

		paramPanel.add(
			new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.dtStandard") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(standardCombo, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, 
				new Insets(5, 0, 5, 10), 0, 0));

		paramPanel.add(
			new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.dtCode") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(codeField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 0, 0));

		paramPanel.add(
			new JLabel(genericLabels.getString("units") + ":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		unitsField.setEditable(false);
		paramPanel.add(unitsField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 0, 0));
		JButton selectUnitsButton = new JButton(genericLabels.getString("select"));
		selectUnitsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					selectUnitsPressed();
				}
			});
		paramPanel.add(selectUnitsButton,
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 5, 5, 10), 0, 0));

//		paramPanel.add(
//			new JLabel(dbeditLabels.getString("ConfigEditPanel.equipmentModelLabel")),
//			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
//				GridBagConstraints.EAST, GridBagConstraints.NONE, 
//				new Insets(5, 10, 5, 2), 0, 0));
//		equipmentModelField.setEditable(false);
//		paramPanel.add(equipmentModelField,
//			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
//				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
//				new Insets(5, 0, 5, 10), 0, 0));
//		JButton selectEquipmentButton = new JButton(genericLabels.getString("select"));
//		selectEquipmentButton.addActionListener(
//			new java.awt.event.ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
//					selectEquipmentPressed();
//				}
//			});
//		paramPanel.add(selectEquipmentButton,
//			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.5,
//				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
//				new Insets(5, 5, 5, 10), 0, 0));
		
		paramPanel.add(
			new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.maxDec")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(maxDecCombo, 
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 25, 0));
		if (supportsMinMaxValue)
		{
			paramPanel.add(
				new JLabel("Min Value:"),
				new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
					GridBagConstraints.EAST, GridBagConstraints.NONE, 
					new Insets(5, 10, 5, 2), 0, 0));
			paramPanel.add(minValueField, 
				new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
					GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 10), 25, 0));
			paramPanel.add(
				new JLabel("Max Value:"),
				new GridBagConstraints(0, 5, 1, 1, 0.0, 0.5,
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, 
					new Insets(5, 10, 5, 2), 0, 0));
			paramPanel.add(maxValueField, 
				new GridBagConstraints(1, 5, 1, 1, 1.0, 0.5,
					GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
					new Insets(5, 0, 5, 10), 25, 0));
		}
		else
		{
			paramPanel.add(
				new JLabel(dbeditLabels.getString("PresentationGroupEditPanel.maxDec")),
				new GridBagConstraints(0, 3, 1, 1, 0.0, 0.5,
					GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, 
					new Insets(5, 10, 5, 2), 0, 0));
			paramPanel.add(maxDecCombo, 
				new GridBagConstraints(1, 3, 1, 1, 1.0, 0.5,
					GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
					new Insets(5, 0, 5, 10), 25, 0));
		}
	}

//	protected void selectEquipmentPressed()
//	{
//		EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
//		if (selectedEquipmentModel != null)
//			dlg.setSelection(selectedEquipmentModel);
//		launchDialog(dlg);
//		if (!dlg.cancelled())
//		{
//			selectedEquipmentModel = dlg.getSelectedEquipmentModel();
//			equipmentModelField.setText(
//				selectedEquipmentModel == null ? "" : selectedEquipmentModel.name);
//		}
//	}

	private void selectUnitsPressed()
	{
		EUSelectDialog dlg = new EUSelectDialog(this);
		launchDialog(dlg);
		EngineeringUnit eu = dlg.getSelection();
		unitsField.setText(eu == null ? "" : eu.abbr);
	}

	private void okPressed()
	{
		if (getDataFromFields())
			closeDlg();
	}

	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when the dialog is initialized. Fill the GUI controls.
	 * @param dp
	 */
	private void fillFields(DataPresentation dp)
	{
		DataType dt = dp.getDataType();
		// New elements will have null data type. Allow user to specify.
		if (dt == null)
		{
			standardCombo.setSelectedIndex(0);
			codeField.setText("");
		}
		else // Editing existing element. Don't allow data type change.
		{
			standardCombo.setDataTypeStandard(dt.getStandard());
			standardCombo.setEditable(false);
			codeField.setText(dt.getCode());
			codeField.setEditable(false);
		}
		
//		if (dp.getEquipmentModelName() == null)
//		{
//			equipmentModelField.setText("");
//			selectedEquipmentModel = null;
//		}
//		else
//		{
//			equipmentModelField.setText(dp.getEquipmentModelName());
//			selectedEquipmentModel = Database.getDb().equipmentModelList.get(
//				dp.getEquipmentModelName());
//		}
		unitsField.setText(dp.getUnitsAbbr() == null ? "" : dp.getUnitsAbbr());
		if (dp.getMaxDecimals() < 0 || dp.getMaxDecimals() > 8)
			dp.setMaxDecimals(3);
		maxDecCombo.setSelectedIndex(dp.getMaxDecimals());
		if (supportsMinMaxValue)
		{
			NumberFormat numFmt = NumberFormat.getNumberInstance();
			numFmt.setGroupingUsed(false);
			numFmt.setMaximumFractionDigits(5);
			minValueField.setText(
				dp.getMinValue() == Constants.undefinedDouble ? ""
				: numFmt.format(dp.getMinValue()));
			maxValueField.setText(
				dp.getMaxValue() == Constants.undefinedDouble ? ""
				: numFmt.format(dp.getMaxValue()));
		}
	}

	private boolean getDataFromFields()
	{
		String standard = standardCombo.getDataTypeStandard();
		String code = codeField.getText().trim();
		if (code.length() == 0)
		{
			showError(dbeditLabels.getString("PresentationGroupEditPanel.noDataType"));
			return false;
		}
		
		int maxDec = maxDecCombo.getSelectedIndex();
		
		String unitsAbbr = unitsField.getText().trim();
		if (unitsAbbr.length() == 0)
		{
			showError(dbeditLabels.getString("PresentationGroupEditPanel.noUnits"));
			return false;
		}
		
//		String emName = equipmentModelField.getText().trim();
//		if (emName.length() == 0)
//			emName = null;
		
		editOb.setDataType(DataType.getDataType(standard, code));
		editOb.setUnitsAbbr(unitsAbbr);
//		editOb.setEquipmentModelName(emName);
		editOb.setMaxDecimals(maxDec);
		
		if (supportsMinMaxValue)
		{
			editOb.setMinValue(Constants.undefinedDouble);
			editOb.setMaxValue(Constants.undefinedDouble);
			String s = minValueField.getText().trim();
			if (s.length() > 0)
				try { editOb.setMinValue(Double.parseDouble(s)); }
				catch(Exception ex)
				{
					showError("Invalid min value '" + s + "' -- setting min to undefined.");
				}
			s = maxValueField.getText().trim();
			if (s.length() > 0)
				try { editOb.setMaxValue(Double.parseDouble(s)); }
				catch(Exception ex)
				{
					showError("Invalid max value '" + s + "' -- setting max to undefined.");
				}
		}
				
		return true;
	}
	
	/**
	 * Called when Cancel button is pressed.
	 * 
	 * @param e
	 *            ignored
	 */
	private void cancelPressed()
	{
		cancelled = true;
		closeDlg();
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

}
