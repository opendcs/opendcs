/*
 *  $Id$
 *
 *  $Log$
 *  Revision 1.4  2016/08/13 17:44:00  mmaloney
 *  Incoming-Tcp transport Medium type.
 *
 *  Revision 1.3  2015/01/14 17:22:51  mmaloney
 *  Polling implementation
 *
 *  Revision 1.2  2015/01/06 16:09:32  mmaloney
 *  First cut of Polling Modules
 *
 *  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 *  OPENDCS 6.0 Initial Checkin
 *
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

import org.opendcs.gui.GuiConstants;
import org.opendcs.gui.PasswordWithShow;

import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.TextUtil;
import decodes.polling.Parity;
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
	private JPanel typeSpecificParamsPanel = new JPanel(new BorderLayout());
	private JPanel goesParamsPanel = new JPanel(new GridBagLayout());
	private JPanel polledTcpParamsPanel = new JPanel(new GridBagLayout());
	private JPanel polledModemParamsPanel = new JPanel(new GridBagLayout());
	private EnumComboBox loggerTypeTcpCombo = new EnumComboBox(Constants.enum_LoggerType, "");
	private EnumComboBox loggerTypeModemCombo = new EnumComboBox(Constants.enum_LoggerType, "");
	private JComboBox baudCombo = new JComboBox(
		new Integer[] { 300, 1200, 2400, 4800, 9600} );
	private JComboBox parityCombo = new JComboBox(
		new Parity[] { Parity.None, Parity.Even, Parity.Odd,
			Parity.Mark, Parity.Space, Parity.Unknown } );
	private JComboBox stopbitsCombo = new JComboBox(
		new Integer[] { 0, 1, 2} );
	private JComboBox databitsCombo = new JComboBox(
		new Integer[] { 7, 8 } );
	private JCheckBox polledTcpDoLoginCheck = new JCheckBox();
	private JTextField polledTcpUserName = new JTextField();
	private PasswordWithShow polledTcpPassword = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
	
	private JCheckBox polledModemDoLoginCheck = new JCheckBox();
	private JTextField polledModemUserName = new JTextField();
	private PasswordWithShow polledModemPassword = new PasswordWithShow(GuiConstants.DEFAULT_PASSWORD_WITH);
	private JLabel mediumIdLabel = new JLabel();
	private JPanel previousSpecialParamsPanel = null;


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
		
//System.out.println("loggerType=" + myTM.getLoggerType() + ", doLogin=" + myTM.isDoLogin()
//+ ", username=" + myTM.getUsername() + ", passwd=" + myTM.getPassword());
		loggerTypeTcpCombo.setSelection(myTM.getLoggerType());
		loggerTypeModemCombo.setSelection(myTM.getLoggerType());
		polledTcpDoLoginCheck.setSelected(myTM.isDoLogin());
		polledModemDoLoginCheck.setSelected(myTM.isDoLogin());
		polledTcpUserName.setText(myTM.getUsername());
		polledModemUserName.setText(myTM.getUsername());
		polledTcpPassword.setText(myTM.getPassword());
		polledModemPassword.setText(myTM.getPassword());
		baudCombo.setSelectedItem((Integer)myTM.getBaud());
		parityCombo.setSelectedItem(Parity.fromCode(myTM.getParity()));
		stopbitsCombo.setSelectedItem((Integer)myTM.getStopBits());
		databitsCombo.setSelectedItem((Integer)myTM.getDataBits());
		
		previousSpecialParamsPanel = null;
		
		mediumTypeSelected();
	}

	/** JBuilder-generated method to initialize the GUI components */
	void jbInit() throws Exception
	{
		this.setTitle(dbeditLabels.getString("TransportMediaEditDialog.title"));

		JPanel mainPanel = new JPanel(new BorderLayout());
		getContentPane().add(mainPanel);

		// North panel contains platform name
		JPanel platNamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		mainPanel.add(platNamePanel, BorderLayout.NORTH);
		platNamePanel.add(new JLabel(dbeditLabels.getString("TransportMediaEditDialog.platLabel")), null);
		platNamePanel.add(platformNameField);
		platformNameField.setBackground(Color.white);
		platformNameField.setEditable(false);
		platformNameField.setText(myPlatform.makeFileName());

		// South panel contains OK and Cancel buttons
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
		mainPanel.add(southButtonPanel, BorderLayout.SOUTH);
		southButtonPanel.add(okButton, null);
		southButtonPanel.add(cancelButton, null);

		// Center panel has two sub panels: sharedParams, and type-specific params
		JPanel centerPanel = new JPanel(new GridBagLayout());
		JPanel sharedParamsPanel = new JPanel(new GridBagLayout());
		mainPanel.add(centerPanel, BorderLayout.CENTER);
		centerPanel.add(sharedParamsPanel, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
			GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 2, 0), 0, 0));

		// the 'sharedParamsPanel' contains parameters that are common to all medium types
		mediumTypeCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.mediumTypeTT"));
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.mediumType")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(mediumTypeCombo, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));

		mediumIdField.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.mediumIdTT"));
		mediumIdLabel.setText(dbeditLabels.getString("TransportMediaEditDialog.mediumId"));
		sharedParamsPanel.add(mediumIdLabel,
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(mediumIdField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 40, 0));

		decodesScriptCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.scriptTT"));
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.script")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(decodesScriptCombo, 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		
		timeZoneLabel.setText(genericLabels.getString("timeZoneLabel"));
		sharedParamsPanel.add(timeZoneLabel, 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(timeZoneCombo,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), -80, 0));

		timeAdjustmentField.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.timeAdjTT"));
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.timeAdj")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		sharedParamsPanel.add(timeAdjustmentField,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 0, 2, 0), 80, 0));
		sharedParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.numSeconds")),
			new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));
		

		// The goesParamsPanel is visible when one of the GOES medium types is selected.
		centerPanel.add(typeSpecificParamsPanel, 
			new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, 
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(0, 0, 0, 0), 5, 0));
		typeSpecificParamsPanel.add(goesParamsPanel, BorderLayout.CENTER);
		goesParamsPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("TransportMediaEditDialog.goesBorder")));
		
		channelNumberField.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.channelNumTT"));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.channelNum")), 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(channelNumberField,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 40, 0));

		transmitTimeField.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.1stXmitTimeTT"));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.1stXmitTime")), 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitTimeField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 60, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 2, 2, 10), 0, 0));

		transmitIntervalCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.xmitIntervalTT"));
		transmitIntervalCombo.setEditable(true);
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.xmitInterval")), 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitIntervalCombo,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 60, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"), 
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));

		transmitDurationField.setToolTipText(dbeditLabels
			.getString("TransportMediaEditDialog.xmitDurationTT"));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.xmitDuration")), 
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 2), 0, 0));
		goesParamsPanel.add(transmitDurationField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 0), 60, 0));
		goesParamsPanel.add(new JLabel("(HH:MM:SS)"),
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 10), 0, 0));

		
		preambleCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.preambleTT"));
		goesParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.preamble")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 0, 5, 2), 0, 0));
		goesParamsPanel.add(preambleCombo,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 0, 5, 0), 0, 0));

		// polledTcpParamsPanel is visible when "polled-tcp" type is selected.
		polledTcpParamsPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("TransportMediaEditDialog.polledTcpBorder")));
		loggerTypeTcpCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.loggerTypeTT"));
		polledTcpParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.loggerType")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledTcpParamsPanel.add(loggerTypeTcpCombo,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		polledTcpDoLoginCheck.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					polledTcpUserName.setEnabled(polledTcpDoLoginCheck.isSelected());
					polledTcpPassword.setEnabled(polledTcpDoLoginCheck.isSelected());
				}
			});
		polledTcpDoLoginCheck.setText(dbeditLabels.getString("TransportMediaEditDialog.doLogin"));
		polledTcpDoLoginCheck.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.doLoginTT"));
		polledTcpParamsPanel.add(polledTcpDoLoginCheck,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		polledTcpParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.username")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledTcpParamsPanel.add(polledTcpUserName,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 120, 0));
		polledTcpParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.password")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledTcpParamsPanel.add(polledTcpPassword,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 120, 0));
	
		// polledModemParamsPanel is visible when "polled-modem" medium type is selected.
		polledModemParamsPanel.setBorder(new TitledBorder(
			dbeditLabels.getString("TransportMediaEditDialog.polledModemBorder")));
		polledModemParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.loggerType")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		loggerTypeModemCombo.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.loggerTypeTT"));
		polledModemParamsPanel.add(loggerTypeModemCombo,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		polledModemParamsPanel.add(
			new JLabel("Baud:"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		baudCombo.setEditable(true);
		polledModemParamsPanel.add(baudCombo,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		polledModemParamsPanel.add(
			new JLabel("Parity:"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledModemParamsPanel.add(parityCombo,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		polledModemParamsPanel.add(
			new JLabel("Stop Bits:"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledModemParamsPanel.add(stopbitsCombo,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		polledModemParamsPanel.add(
			new JLabel("Data Bits:"),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledModemParamsPanel.add(databitsCombo,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 0, 0));
		
		polledModemDoLoginCheck.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					polledModemUserName.setEnabled(polledModemDoLoginCheck.isSelected());
					polledModemPassword.setEnabled(polledModemDoLoginCheck.isSelected());
				}
			});
		polledModemDoLoginCheck.setText(dbeditLabels.getString("TransportMediaEditDialog.doLogin"));
		polledModemDoLoginCheck.setToolTipText(dbeditLabels.getString("TransportMediaEditDialog.doLoginTT"));
		polledModemParamsPanel.add(polledModemDoLoginCheck,
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		polledModemParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.username")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledModemParamsPanel.add(polledModemUserName,
			new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 120, 0));
		polledModemParamsPanel.add(
			new JLabel(dbeditLabels.getString("TransportMediaEditDialog.password")),
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		polledModemParamsPanel.add(polledModemPassword,
			new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 0), 120, 0));
	}

	/**
	 * Called when Transport Medium changes
	 */
	void mediumTypeSelected()
	{
		String tmType = (String) mediumTypeCombo.getSelectedItem();
		if (tmType == null)
			return; // Shouldn't happen

		typeSpecificParamsPanel.removeAll();
		
		if (previousSpecialParamsPanel == polledTcpParamsPanel)
		{
			loggerTypeModemCombo.setSelectedIndex(loggerTypeTcpCombo.getSelectedIndex());
			polledModemDoLoginCheck.setSelected(polledTcpDoLoginCheck.isSelected());
			polledModemUserName.setText(polledTcpUserName.getText());
			polledModemPassword.setText(new String(polledTcpPassword.getPassword()));
		}
		else if (previousSpecialParamsPanel == polledModemParamsPanel)
		{
			loggerTypeTcpCombo.setSelectedIndex(loggerTypeModemCombo.getSelectedIndex());
			polledTcpDoLoginCheck.setSelected(polledModemDoLoginCheck.isSelected());
			polledTcpUserName.setText(polledModemUserName.getText());
			polledTcpPassword.setText(new String(polledModemPassword.getPassword()));
		}
		
		if (tmType.toLowerCase().startsWith("goes"))
		{
			typeSpecificParamsPanel.add(previousSpecialParamsPanel = goesParamsPanel, BorderLayout.CENTER);
			mediumIdLabel.setText(dbeditLabels.getString("TransportMediaEditDialog.DcpAddress"));

//			// Enable all fields
//			transmitTimeField.setEnabled(true);
//			transmitDurationField.setEnabled(true);
//			channelNumberField.setEnabled(true);
//			transmitIntervalCombo.setEnabled(true);
//			preambleCombo.setEnabled(true);
		}
		else if (tmType.toLowerCase().equals(Constants.medium_PolledTcp)
			  || tmType.toLowerCase().equals("incoming-tcp"))
		{
			typeSpecificParamsPanel.add(previousSpecialParamsPanel = polledTcpParamsPanel, BorderLayout.CENTER);
			mediumIdLabel.setText(
				tmType.toLowerCase().equals("polled-tcp") ?
					dbeditLabels.getString("TransportMediaEditDialog.HostPort") :
					dbeditLabels.getString("TransportMediaEditDialog.LoggerId")	);
			polledTcpUserName.setEnabled(polledTcpDoLoginCheck.isSelected());
			polledTcpPassword.setEnabled(polledTcpDoLoginCheck.isSelected());
		}
		else if (tmType.toLowerCase().equals(Constants.medium_PolledModem))
		{
			typeSpecificParamsPanel.add(previousSpecialParamsPanel = polledModemParamsPanel, BorderLayout.CENTER);
			mediumIdLabel.setText(dbeditLabels.getString("TransportMediaEditDialog.Telnum"));
			polledModemUserName.setEnabled(polledTcpDoLoginCheck.isSelected());
			polledModemPassword.setEnabled(polledTcpDoLoginCheck.isSelected());
		}
		else
		{
			mediumIdLabel.setText(dbeditLabels.getString("TransportMediaEditDialog.mediumId"));

//			// Disable the GOES fields.
//			transmitTimeField.setEnabled(false);
//			transmitDurationField.setEnabled(false);
//			channelNumberField.setEnabled(false);
//			transmitIntervalCombo.setEnabled(false);
//			preambleCombo.setEnabled(false);
		}
		pack();
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
		myTM.setMediumType(mediumTypeCombo.getSelection());
		myTM.scriptName = decodesScriptCombo.getSelection();

		String tmType = (String) mediumTypeCombo.getSelectedItem();
		if (tmType.toLowerCase().equals("polled-tcp") || tmType.toLowerCase().equals("incoming-tcp"))
		{
			String s = loggerTypeTcpCombo.getSelection();
			myTM.setLoggerType(s == null || s.trim().length()==0 ? null : s);
			myTM.setDoLogin(polledTcpDoLoginCheck.isSelected());
			s = polledTcpUserName.getText();
			if (s != null && s.trim().length() > 0)
				myTM.setUsername(s);
			else
				myTM.setUsername(null);
			s = new String(polledTcpPassword.getPassword());
			if (s.trim().length() > 0)
				myTM.setPassword(s);
			else
				myTM.setPassword(null);
		}
		else if (tmType.toLowerCase().equals("polled-modem"))
		{
			String s = loggerTypeModemCombo.getSelection();
			myTM.setLoggerType(s == null || s.trim().length()==0 ? null : s);
			myTM.setDoLogin(polledModemDoLoginCheck.isSelected());
			s = polledModemUserName.getText();
			if (s != null && s.trim().length() > 0)
				myTM.setUsername(s);
			else
				myTM.setUsername(null);
			s = new String(polledModemPassword.getPassword());
			if (s.trim().length() > 0)
				myTM.setPassword(s);
			else
				myTM.setPassword(null);
			myTM.setBaud((Integer)baudCombo.getSelectedItem());
			myTM.setStopBits((Integer)stopbitsCombo.getSelectedItem());
			myTM.setDataBits((Integer)databitsCombo.getSelectedItem());
			myTM.setParity(((Parity)parityCombo.getSelectedItem()).getCode());
		}

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
