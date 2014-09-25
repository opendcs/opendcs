/*
*  $Id$
*  
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/01/08 20:48:27  mmaloney
*  Relax 100 sensor limit.
*  Get rid of 'setPreferredSize' calls.
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.Properties;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.TextUtil;
import ilex.util.PropertiesUtil;

import decodes.gui.*;
import decodes.db.ConfigSensor;
import decodes.db.EquipmentModel;
import decodes.db.DataType;
import decodes.db.EnumValue;
import decodes.db.Database;
import decodes.util.TimeOfDay;
import decodes.db.Constants;
import decodes.gui.GuiDialog;

/**
Dialog for editing a configuration sensor.
*/
@SuppressWarnings("serial")
public class ConfigSensorEditDialog extends GuiDialog
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

	private static final int MID_COL_WIDTH = 60;

    JPanel outerPanel = new JPanel();
    JPanel jPanel1 = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JLabel configurationLabel = new JLabel();
    JTextField configNameField = new JTextField();
    JLabel jLabel2 = new JLabel();
    JTextField sensorNumberField = new JTextField();
    JPanel southButtonPanel = new JPanel();
    JButton okButton = new JButton();
    JButton cancelButton = new JButton();
    FlowLayout flowLayout2 = new FlowLayout();
    JPanel centerPanel = new JPanel();
    PropertiesEditPanel propertiesEditPanel;
    JPanel fieldEntryPanel = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    TitledBorder titledBorder1;
    Border border1;
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JLabel sensorNameLabel = new JLabel();
    JTextField sensorNameField = new JTextField();
    JLabel dataTypeLabel = new JLabel();
    DataTypeCodeCombo dtStdCombo[] = new DataTypeCodeCombo[3];
    JLabel codeLabel1 = new JLabel();
    JLabel codeLabel2 = new JLabel();
    JLabel codeLabel3 = new JLabel();
    JTextField dataTypeField[] = new JTextField[3];
    JLabel valueRangeLabel = new JLabel();
    JTextField absoluteMinField = new JTextField();
    JLabel maxLabel = new JLabel();
    JTextField absoluteMaxField = new JTextField();
    JLabel recordingModeLabel = new JLabel();
    RecordingModeCombo recordingModeCombo = new RecordingModeCombo();
    JLabel firstSampleTimeLabel = new JLabel();
    JTextField firstSampleTimeField = new JTextField();
    JLabel hhmmss1label = new JLabel();
    JLabel samplingIntervalLabel = new JLabel();
    JTextField equipmentModelField = new JTextField();
    JButton selectEquipmentModelButton = new JButton();
    JLabel equipmentModelLabel = new JLabel();
    JLabel dataTypesStandardLabel = new JLabel();
    JLabel jLabel6 = new JLabel();
    JLabel jLabel7 = new JLabel();
    JComboBox samplingIntervalCombo = new JComboBox(
		new String[] { "01:00:00", "00:30:00", "00:20:00", "00:15:00",
			"00:10:00", "00:05:00" });
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel jLabel4 = new JLabel();
    JLabel usgsStatCodeLabel = new JLabel();
    JTextField usgsStatCodeField = new JTextField();

	/** The object being edited */
	ConfigSensor theSensor;

	/** The equipment model associated with this sensor (if there is one). */
	EquipmentModel theEquipmentModel;

	/** Configuration Sensor Properties */
	Properties theProperties;

  	/**
   	* This member, if true, indicates that the ConfigSensor being edited by
   	* this box was newly added with the "Add" button.
   	* If that's the case, and the user presses "Cancel", then we'll need to
   	* delete this ConfigSensor.
   	*/
    boolean _csNewlyAdded;

  	/** Default constructor for jbuilder.  */
	public ConfigSensorEditDialog()
	{
		this(null);
	}

  	/**
	  Construct with an object to be edited.
	  @param cs the sensor to edit.
	*/
	public ConfigSensorEditDialog(ConfigSensor cs)
	{
	    this(cs, false);
	}

  	/**
   	* Construct with an object to be editted, and flag which allows the
   	* caller to indicate that this is a newly added ConfigSensor.
	  @param cs the sensor to edit.
	  @param csNewlyAdded true if this sensor should be deleted on Cancel.
   	*/
	public ConfigSensorEditDialog(ConfigSensor cs, boolean csNewlyAdded)
	{
        super(getDbEditFrame(), "", true);

		_csNewlyAdded = csNewlyAdded;
		theSensor = cs;
		theEquipmentModel = cs.equipmentModel;
        try
		{
			theProperties = new Properties();
			if (cs != null)
				PropertiesUtil.copyProps(theProperties, cs.getProperties());

			String sc = PropertiesUtil.rmIgnoreCase(theProperties,
				"StatisticsCode");
			if (sc != null && sc.length() > 0
			 && theSensor.getUsgsStatCode() == null)
				theSensor.setUsgsStatCode(sc); 

		    propertiesEditPanel = new PropertiesEditPanel(theProperties);
			propertiesEditPanel.setOwnerDialog(this);
            jbInit();
            pack();
			fillValues();
			getRootPane().setDefaultButton(okButton);
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
		addWindowListener(
			new WindowAdapter()
			{
				boolean started=false;
				public void windowActivated(WindowEvent e)
				{
					if (!started)
						sensorNameField.requestFocus();
					started = true;
				}
			});
    }


	/** Fills the GUI components with values from the object being edited. */
	void fillValues()
	{
		if (theSensor == null)
			return;
		if (theSensor.platformConfig != null)
	        configNameField.setText(theSensor.platformConfig.configName);
		sensorNumberField.setText("" + theSensor.sensorNumber);
	    sensorNameField.setText(theSensor.sensorName);

		int i=0;
		for(Iterator it = theSensor.getDataTypes(); it.hasNext() && i < 3; i++)
		{
			DataType dt = (DataType)it.next();
			dtStdCombo[i].setDataTypeStandard(dt.getStandard());
		    dataTypeField[i].setText(dt.getCode());
			dtStdCombo[i].setEnabled(true);
		    dataTypeField[i].setEnabled(true);
		}
		// Set the remaining 'Standard' settings to something different
		// than the ones above.
		decodes.db.DbEnum dtsenum = Database.getDb().getDbEnum("DataTypeStandard");
		for(; i < 3; i++)  // for each remaining slot...
		{
			Iterator enit = dtsenum.iterator();
			boolean set = false;
			while(enit.hasNext())
			{
				EnumValue ev = (EnumValue)enit.next();
				// Is this 'standard' used in one of the slots above?
				int j=0;
				for(; j<i; j++)
				{
					String std = dtStdCombo[j].getDataTypeStandard();
					if (ev.getValue().equalsIgnoreCase(std))
						break;
				}
				if (j == i) // Didn't use this std yet?
				{
					dtStdCombo[j].setSelectedItem(ev.getValue());
		    		dataTypeField[i].setText("");
					dtStdCombo[i].setEnabled(true);
		    		dataTypeField[i].setEnabled(true);
					set = true;
					break; // go on to next slot.
				}
			}
			if (!set) // fell through while loop, all standards represented.
			{
		    	dataTypeField[i].setEnabled(false);
				dtStdCombo[i].setEnabled(false);
			}
		}

		if (theEquipmentModel != null)
		    equipmentModelField.setText(theEquipmentModel.name);
		recordingModeCombo.setSelection(theSensor.recordingMode);
		firstSampleTimeField.setText(
		    TimeOfDay.seconds2hhmmss(theSensor.timeOfFirstSample));
		String si = TimeOfDay.seconds2hhmmss(theSensor.recordingInterval);
		samplingIntervalCombo.setSelectedItem(si);
//		samplingIntervalField.setText(
//		    TimeOfDay.seconds2hhmmss(theSensor.recordingInterval));
		if (theSensor.absoluteMin != Constants.undefinedDouble)
			absoluteMinField.setText("" + theSensor.absoluteMin);
		if (theSensor.absoluteMax != Constants.undefinedDouble)
			absoluteMaxField.setText("" + theSensor.absoluteMax);

		String c = theSensor.getUsgsStatCode();
		usgsStatCodeField.setText(c == null ? "" : c);
	}

	/**
	  Validates and saves the values being edited back into the object.
	  DOES NO Database IO -- just back to the object.
	  @return true if validation passed.
	*/
	boolean saveValues()
	{
		if (theSensor == null)
			return true;

		int firstSampleTime = 0;
		int recordingInterval = 0;
		try
		{
			firstSampleTime =
				TimeOfDay.hhmmss2seconds(firstSampleTimeField.getText());
		}
		catch(NumberFormatException e)
		{
			showError(
				dbeditLabels.getString("ConfigSensorEditDialog.badTOD"));
			return false;
		}
		try
		{
			recordingInterval =
				TimeOfDay.hhmmss2seconds(
					(String)samplingIntervalCombo.getSelectedItem());
//				TimeOfDay.hhmmss2seconds(samplingIntervalField.getText());
		}
		catch(NumberFormatException e)
		{
			showError(
				dbeditLabels.getString("ConfigSensorEditDialog.badInterval"));
			return false;
		}

		String s = absoluteMinField.getText();
		if (s != null && !TextUtil.isAllWhitespace(s))
			try { theSensor.absoluteMin = Double.parseDouble(s); }
			catch(NumberFormatException e)
			{
				showError(
					dbeditLabels.getString("ConfigSensorEditDialog.badMin"));
				return false;
			}
		else
			theSensor.absoluteMin = Constants.undefinedDouble;

		s = absoluteMaxField.getText();
		if (s != null && !TextUtil.isAllWhitespace(s))
			try { theSensor.absoluteMax = Double.parseDouble(s); }
			catch(NumberFormatException e)
			{
				showError(
					dbeditLabels.getString("ConfigSensorEditDialog.badMax"));
				return false;
			}
		else
			theSensor.absoluteMax = Constants.undefinedDouble;

		theSensor.sensorName = sensorNameField.getText();
		if (theSensor.sensorName == null
		 || TextUtil.isAllWhitespace(theSensor.sensorName))
		{
			showError(
				dbeditLabels.getString("ConfigSensorEditDialog.blankName"));
			return false;
		}

		theSensor.clearDataTypes();
		int ndt = 0;
		for(int i = 0; i<3; i++)
		{
		    String std = dtStdCombo[i].getDataTypeStandard();
			String code = dataTypeField[i].getText().trim();
			if (code.length() > 0)
			{
				theSensor.addDataType(DataType.getDataType(std, code));
				ndt++;
			}
		}
		if (ndt == 0)
		{
			showError(
				dbeditLabels.getString("ConfigSensorEditDialog.oneDataType"));
			return false;
		}

		theSensor.equipmentModel = theEquipmentModel;
		theSensor.recordingMode = recordingModeCombo.getSelection();

		s = usgsStatCodeField.getText().trim();
		theSensor.setUsgsStatCode(s.length() > 0 ? s : null);

		theSensor.timeOfFirstSample = firstSampleTime;
		theSensor.recordingInterval = recordingInterval;
		propertiesEditPanel.saveChanges();
		theSensor.getProperties().clear();
		PropertiesUtil.copyProps(theSensor.getProperties(), theProperties);
		return true;
	}

	/** Initializes GUI components. */
    void jbInit() throws Exception
	{
    	dtStdCombo[0] = new DataTypeCodeCombo();
    	dtStdCombo[1] = new DataTypeCodeCombo();
    	dtStdCombo[2] = new DataTypeCodeCombo();
        dtStdCombo[0].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.selectStd"));
        dtStdCombo[1].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.selectStd"));
        dtStdCombo[2].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.selectStd"));
    	dataTypeField[0] = new JTextField();
    	dataTypeField[1] = new JTextField();
    	dataTypeField[2] = new JTextField();
        dataTypeField[0].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.enterCode"));
        dataTypeField[1].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.enterCode"));
        dataTypeField[2].setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.enterCode"));
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153),2),"");
        border1 = BorderFactory.createCompoundBorder(titledBorder1,BorderFactory.createEmptyBorder(2,2,2,2));
        outerPanel.setLayout(borderLayout2);
        jPanel1.setLayout(flowLayout1);
        configurationLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.configLabel"));
        configNameField.setEditable(false);

        jLabel2.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.sensorLabel"));
        sensorNumberField.setEditable(false);

        okButton.setText(genericLabels.getString("OK"));
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        cancelButton.setText(genericLabels.getString("cancel"));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        southButtonPanel.setLayout(flowLayout2);
        flowLayout2.setHgap(35);
        flowLayout2.setVgap(10);
        this.setTitle(dbeditLabels.getString("ConfigSensorEditDialog.title"));

        fieldEntryPanel.setLayout(gridBagLayout2);



        centerPanel.setLayout(gridBagLayout1);
        borderLayout2.setVgap(10);
        outerPanel.setPreferredSize(new Dimension(480, 570));//450, 570
        jPanel1.setBorder(border1);
        sensorNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        sensorNameLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.nameLabel"));
        sensorNameField.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.nameTT"));
        dataTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        dataTypeLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.stdLabel"));
        codeLabel1.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.codeLabel"));
        codeLabel2.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.codeLabel"));
        codeLabel3.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.codeLabel"));
        valueRangeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        valueRangeLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.validMinLabel"));
        absoluteMinField.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.validMinTT"));
        maxLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        maxLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.validMaxLabel"));
        absoluteMaxField.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.validMaxTT"));
        recordingModeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        recordingModeLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.recModeLabel"));
        recordingModeCombo.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.recModeTT"));
        firstSampleTimeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        firstSampleTimeLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.firstSampTimeLabel"));
        firstSampleTimeField.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.firstSampTimeTT"));
        hhmmss1label.setText("(HH:MM:SS)");
        samplingIntervalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        samplingIntervalLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.intLabel"));
        equipmentModelField.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.equipTT"));
        equipmentModelField.setEditable(false);
        selectEquipmentModelButton.setText(genericLabels.getString("select"));
        selectEquipmentModelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectEquipmentModelButton_actionPerformed(e);
            }
        });
        equipmentModelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        equipmentModelLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.equipLabel"));
        dataTypesStandardLabel.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.dtStdLabel"));
        jLabel6.setText(
			dbeditLabels.getString("ConfigSensorEditDialog.stdLabel"));
        jLabel4.setText("(HH:MM:SS)");
    	usgsStatCodeLabel.setText("USGS Stat Code:");
        samplingIntervalCombo.setToolTipText(
			dbeditLabels.getString("ConfigSensorEditDialog.intTT"));
        getContentPane().add(outerPanel);
        outerPanel.add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(configurationLabel, null);
        jPanel1.add(configNameField, null);
        jPanel1.add(jLabel2, null);
        jPanel1.add(sensorNumberField, null);
        outerPanel.add(southButtonPanel, BorderLayout.SOUTH);
        southButtonPanel.add(okButton, null);
        southButtonPanel.add(cancelButton, null);
        outerPanel.add(centerPanel, BorderLayout.CENTER);
        centerPanel.add(fieldEntryPanel, 
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
            	GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(0, 0, 0, 0), 0, 0));
        fieldEntryPanel.add(sensorNameLabel, 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 0, 3, 3), 35, 0));
        fieldEntryPanel.add(sensorNameField, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
           		GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(dataTypeLabel, 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(dtStdCombo[0], 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,
            	GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(codeLabel1, 
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(dataTypeField[0], 
			new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 20), 0, 0));

        fieldEntryPanel.add(dataTypesStandardLabel, 
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 10, 3, 3), 0, 0));
        fieldEntryPanel.add(dtStdCombo[1], 
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(codeLabel2, 
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(dataTypeField[1], 
			new GridBagConstraints(3, 2, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 20), 0, 0));

        fieldEntryPanel.add(jLabel6,
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(dtStdCombo[2],
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(codeLabel3, 
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 5, 3, 3), 0, 0));
        fieldEntryPanel.add(dataTypeField[2], 
				new GridBagConstraints(3, 3, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 3, 20), 0, 0));

        fieldEntryPanel.add(usgsStatCodeLabel,
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(usgsStatCodeField,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

        fieldEntryPanel.add(valueRangeLabel,
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(absoluteMinField,
			new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(maxLabel,
			new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 4), 0, 0));
        fieldEntryPanel.add(absoluteMaxField,
			new GridBagConstraints(3, 5, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 20), 0, 0));

        fieldEntryPanel.add(recordingModeLabel,
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(recordingModeCombo,
			new GridBagConstraints(1, 6, 1, 1, 1.0, 1.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));

        fieldEntryPanel.add(firstSampleTimeLabel,
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(firstSampleTimeField,
			new GridBagConstraints(1, 7, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(hhmmss1label,
			new GridBagConstraints(2, 7, 2, 1, 0.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 0), 0, 0));

        fieldEntryPanel.add(samplingIntervalLabel,
			new GridBagConstraints(0, 8, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 3), 0, 0));
        fieldEntryPanel.add(samplingIntervalCombo,
			new GridBagConstraints(1, 8, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 0), 0, 0));
        fieldEntryPanel.add(jLabel4,
			new GridBagConstraints(2, 8, 2, 1, 0.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 5, 3, 0), 0, 0));

        fieldEntryPanel.add(equipmentModelLabel, 
			new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0,
            	GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(3, 0, 10, 3), 0, 0));
        fieldEntryPanel.add(equipmentModelField, 
			new GridBagConstraints(1, 9, 1, 1, 1.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(3, 0, 10, 0), 0, 0));
        fieldEntryPanel.add(selectEquipmentModelButton,
			new GridBagConstraints(2, 9, 2, 1, 0.0, 0.0,
            	GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 10, 10, 0), 0, 0));

        centerPanel.add(propertiesEditPanel, 
			new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
            	GridBagConstraints.CENTER, GridBagConstraints.BOTH, 
				new Insets(0, 0, 0, 0), 0, 0));
		samplingIntervalCombo.setEditable(true);
    }

	/**
	  Called when the Select Equipment Model button is pressed.
	  @param e ignored.
	*/
    void selectEquipmentModelButton_actionPerformed(ActionEvent e)
	{
		EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
		dlg.setSelection(theEquipmentModel);
		dlg.setVisible(true);
		if (!dlg.cancelled())
		{
			theEquipmentModel = dlg.getSelectedEquipmentModel();
			if (theEquipmentModel != null) // selection was made?
				equipmentModelField.setText(theEquipmentModel.name);
			else
				equipmentModelField.setText("");
		}
    }

	/**
	  Called when the OK button is pressed.
	  @param e ignored.
	*/
    void okButton_actionPerformed(ActionEvent e)
	{
		if (saveValues() == false)
			return;
		closeDlg();
    }

	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when the Cancel button is pressed.
	  @param e ignored.
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		if (_csNewlyAdded)
			theSensor.platformConfig.removeSensor(theSensor);
		closeDlg();
    }
}
