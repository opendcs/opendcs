/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.5  2013/03/21 18:27:40  mmaloney
 *  DbKey Implementation
 *
 *  Revision 1.4  2009/08/12 19:56:02  mjmaloney
 *  usgs merge
 *
 *  Revision 1.3  2008/11/20 18:49:21  mjmaloney
 *  merge from usgs mods
 *
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import javax.swing.table.AbstractTableModel;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;
import decodes.db.*;
import decodes.gui.*;
import decodes.util.TimeOfDay;
import decodes.util.DecodesSettings;



/**
 * Dialog called from PlatformEditPanel to edit a Transport Medium.
 */
@SuppressWarnings("serial")
public class TransportMediaEditDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private JTextField platformNameField = new JTextField(20);
	private JButton okButton = new JButton();
	private JTextField transmitTimeField = new JTextField();
	private JTextField transmitDurationField = new JTextField();
	private JTextField channelNumberField = new JTextField();
	private JTextField mediumIdField = new JTextField();
	private EnumComboBox mediumTypeCombo = 
		new EnumComboBox(Constants.enum_TMType, Constants.medium_GoesST);
	private DecodesScriptCombo decodesScriptCombo;
	Platform myPlatform;
	TransportMedium myTM;
//	EquipmentModel selectedEquipmentModel;
//	EquipmentModelCombo equipmentModelCombo = new EquipmentModelCombo();
	AbstractTableModel listModel;
	JTextField timeAdjustmentField = new JTextField();
	JComboBox transmitIntervalCombo = new JComboBox(new String[]
	{ "04:00:00", "03:00:00", "02:00:00", "01:00:00", "00:30:00", "00:15:00", "00:12:00" });
	JComboBox preambleCombo = new JComboBox(new String[]
	{ "Short", "Long", "Unknown" });
	JLabel timeZoneLabel = new JLabel();
	TimeZoneSelector timeZoneCombo = new TimeZoneSelector();

	boolean okPressed = false;

	/**
	 * Constructor.
	 * 
	 * @param platform
	 *            the platform being edited.
	 * @param tm
	 *            the TransportMedium to edit
	 * @param listModel
	 *            the AbstractTableModel in the PlatformEditPanel.
	 */
	public TransportMediaEditDialog(Platform platform, TransportMedium tm,
		AbstractTableModel listModel)
	{
		super(getDbEditFrame(), "", true);
		myPlatform = platform;
		myTM = tm;
//		selectedEquipmentModel = null;
//		if (DecodesSettings.instance().editDatabaseTypeCode == DecodesSettings.DB_NWIS)
//		{
//			selectedEquipmentModel = platform.getConfig().getEquipmentModel();
//			myTM.equipmentModel = selectedEquipmentModel;
//		}
		this.listModel = listModel;
		decodesScriptCombo = new DecodesScriptCombo(myPlatform.getConfig(), myTM);

		try
		{
			jbInit();
			pack();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		fillFields();
		getRootPane().setDefaultButton(okButton);
	}

	/** Fill the GUI controls from the object. */
	private void fillFields()
	{
		mediumIdField.setText(myTM.getMediumId());
//		equipmentModelCombo.set(myTM.equipmentModel);
		platformNameField.setText(myPlatform.makeFileName());
		transmitTimeField.setText(myTM.assignedTime != -1 ? TimeOfDay
			.seconds2hhmmss(myTM.assignedTime) : "");
		transmitIntervalCombo.setSelectedItem(myTM.transmitInterval != -1 ? TimeOfDay
			.seconds2hhmmss(myTM.transmitInterval) : "");
		transmitDurationField.setText(myTM.transmitWindow != -1 ? TimeOfDay
			.seconds2hhmmss(myTM.transmitWindow) : "");
		mediumTypeCombo.setSelection(myTM.getMediumType());
		if (myTM.channelNum != -1)
			channelNumberField.setText("" + myTM.channelNum);
		else
			channelNumberField.setText("");
		decodesScriptCombo.set(myTM.scriptName);
		timeAdjustmentField.setText("" + myTM.getTimeAdjustment());
		char c = myTM.getPreamble();
		preambleCombo.setSelectedItem(c == Constants.preambleShort ? "Short"
			: c == Constants.preambleLong ? "Long" : "Unknown");
		if (myTM.getTimeZone() == null)
			myTM.setTimeZone("UTC");
		timeZoneCombo.setTZ(myTM.getTimeZone());
		mediumTypeSelected();
	}

	/** JBuilder-generated method to initialize the GUI components */
	void jbInit() throws Exception
	{
		this.setTitle(dbeditLabels.getString("TransportMediaEditDialog.title"));

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel platNamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		platformNameField.setBackground(Color.white);
		platformNameField.setEditable(false);
		platformNameField.setText(myPlatform.makeFileName());
		JPanel southButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 35, 10));
		mediumTypeCombo.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					mediumTypeSelected();
				}
			});

		okButton.setText("   " + genericLabels.getString("OK") + "   ");
		okButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					okButtonPressed();
				}
			});
		JButton cancelButton = new JButton(genericLabels.getString("cancel"));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelButtonPressed();
			}
		});

		JPanel goesParamsPanel = new JPanel(new GridBagLayout());
		goesParamsPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("TransportMediaEditDialog.goesBorder")));
		JPanel sharedParamsPanel = new JPanel(new GridBagLayout());
		JPanel jPanel5 = new JPanel(new GridBagLayout());
		mediumTypeCombo.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.mediumTypeTT"));
		mediumIdField.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.mediumIdTT"));
//		equipmentModelCombo.setToolTipText(dbeditLabels
//			.getString("TransportMediaEditDialog.equipModTT"));
		decodesScriptCombo.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.scriptTT"));
		channelNumberField.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.channelNumTT"));
		transmitTimeField.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.1stXmitTimeTT"));
		transmitIntervalCombo.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.xmitIntervalTT"));
		transmitIntervalCombo.setEditable(true);
		transmitDurationField.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.xmitDurationTT"));
		timeAdjustmentField.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.timeAdjTT"));
		preambleCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.preambleTT"));
		timeZoneLabel.setText(genericLabels.getString("timeZoneLabel"));
		getContentPane().add(mainPanel);
		mainPanel.add(platNamePanel, BorderLayout.NORTH);
		platNamePanel.add(new JLabel(dbeditLabels.getString("TransportMediaEditDialog.platLabel")), null);
		platNamePanel.add(platformNameField);
		mainPanel.add(southButtonPanel, BorderLayout.SOUTH);
		southButtonPanel.add(okButton, null);
		southButtonPanel.add(cancelButton, null);
		mainPanel.add(jPanel5, BorderLayout.CENTER);
		jPanel5.add(sharedParamsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 2, 0), 0, 0));

		
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.mediumType")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(mediumTypeCombo, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 58, 0));

		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.mediumId")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(mediumIdField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 120, 0));
		
//		sharedParamsPanel.add(
//			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.equipMod")),
//			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
//				GridBagConstraints.EAST, GridBagConstraints.NONE, 
//				new Insets(2, 0, 2, 2), 0, 0));
//		sharedParamsPanel.add(equipmentModelCombo, 
//			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
//				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
//				new Insets(2, 2, 2, 20), 0, 0));

		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.script")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(decodesScriptCombo, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 53, 0));
		
		sharedParamsPanel.add(timeZoneLabel, 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(timeZoneCombo,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), -60, 0));

		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.timeAdj")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(timeAdjustmentField,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 0, 0));
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.numSeconds")),
			new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));
		
		jPanel5
			.add(goesParamsPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
				GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 5, 0));

		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.channelNum")), 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(channelNumberField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 150, 0));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.1stXmitTime")), 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitTimeField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 150, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 0), 0, 0));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.xmitInterval")), 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitIntervalCombo,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0),150, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"), 
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 0), 0, 0));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.xmitDuration")), 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitDurationField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 150, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"),
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 0), 0, 0));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.preamble")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 5, 2), 0, 0));
		goesParamsPanel.add(preambleCombo,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 5, 0), 150, 0));
	}

	/**
	 * Called when Transport Medium changes
	 */
	void mediumTypeSelected()
	{
		String tmType = (String) mediumTypeCombo.getSelectedItem();
		if (tmType == null)
			return; // Shouldn't happen
System.out.println("Selected mediumType " + tmType);
		
		if (tmType.toLowerCase().startsWith("goes"))
		{
			// Enable all fields
			transmitTimeField.setEnabled(true);
			transmitDurationField.setEnabled(true);
			channelNumberField.setEnabled(true);
			transmitIntervalCombo.setEnabled(true);
			preambleCombo.setEnabled(true);
		}
		else
		{
			// Disable the GOES fields.
			transmitTimeField.setEnabled(false);
			transmitDurationField.setEnabled(false);
			channelNumberField.setEnabled(false);
			transmitIntervalCombo.setEnabled(false);
			preambleCombo.setEnabled(false);
		}
	}

	/**
	 * Called when OK button is pressed.
	 */
	void okButtonPressed()
	{
		String id = mediumIdField.getText();
		if (id == null || TextUtil.isAllWhitespace(id))
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("TransportMediaEditDialog.blankMediumId"));
			return;
		}

		String chan = channelNumberField.getText();
		if (TextUtil.isAllWhitespace(chan))
			myTM.channelNum = -1;
		else
		{
			try
			{
				myTM.channelNum = Integer.parseInt(chan);
			}
			catch (NumberFormatException nfe)
			{
				TopFrame.instance().showError(
					LoadResourceBundle.sprintf(
						dbeditLabels.getString("TransportMediaEditDialog.invalidChannel"), chan));
				return;
			}
		}

		try
		{
			String s = transmitTimeField.getText();
			myTM.assignedTime = !TextUtil.isAllWhitespace(s) ? TimeOfDay.hhmmss2seconds(s) : -1;

			s = transmitDurationField.getText();
			myTM.transmitWindow = !TextUtil.isAllWhitespace(s) ? TimeOfDay.hhmmss2seconds(s) : -1;

			s = (String) transmitIntervalCombo.getSelectedItem();
			myTM.transmitInterval = !TextUtil.isAllWhitespace(s) ? TimeOfDay.hhmmss2seconds(s) : -1;

			s = timeAdjustmentField.getText();
			myTM.setTimeAdjustment(!TextUtil.isAllWhitespace(s) ? Integer.parseInt(s) : 0);

			s = (String) preambleCombo.getSelectedItem();
			myTM.setPreamble(s.charAt(0));

			myTM.setTimeZone(timeZoneCombo.getTZ());
		}
		catch (NumberFormatException nfe)
		{
			TopFrame.instance().showError(nfe.toString());
			return;
		}

		myTM.setMediumId(id);
//		myTM.equipmentModel = equipmentModelCombo.getSelection();
		myTM.setMediumType(mediumTypeCombo.getSelection());
		myTM.scriptName = decodesScriptCombo.getSelection();

		if (listModel != null)
			listModel.fireTableDataChanged();

		okPressed = true;
		closeDlg();
	}

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * Called when Cancel button is pressed.
	 */
	void cancelButtonPressed()
	{
		closeDlg();
	}
}
