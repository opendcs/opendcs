/*
*  $Id$
*  
*  $Log$
*  Revision 1.6  2013/07/12 11:50:15  mmaloney
*  Dev
*
*  Revision 1.5  2013/05/20 19:24:20  shweta
*  fixed bug-- http://jirasvr:8080/browse/TPD-104
*
*  Revision 1.4  2013/01/08 20:48:27  mmaloney
*  Relax 100 sensor limit.
*  Get rid of 'setPreferredSize' calls.
*
*/
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;
import decodes.gui.*;
import decodes.db.PlatformConfig;
import decodes.datasource.GoesPMParser;
import decodes.datasource.IridiumPMParser;
import decodes.db.ConfigSensor;
import decodes.db.ScriptSensor;
import decodes.db.EquipmentModel;
import decodes.db.Constants;
import decodes.db.DecodesScript;
import decodes.db.UnitConverterDb;
import decodes.util.TimeOfDay;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.DataType;
import decodes.util.DecodesSettings;

/** 
Editor panel for Platform Configurations.
This panel is opened from the ConfigListPanel when the user selects 'Open'.
*/
public class ConfigEditPanel extends DbEditorTab
	implements ChangeTracker, EntityOpsController
{
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    EntityOpsPanel entityOpsPanel;
    TitledBorder titledBorder3;
    JPanel jPanel5 = new JPanel();
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel8 = new JPanel();
    JScrollPane jScrollPane1 = new JScrollPane();
    BorderLayout borderLayout2 = new BorderLayout();
    JTextArea descriptionArea = new JTextArea();
    JPanel jPanel2 = new JPanel();
    JPanel jPanel1 = new JPanel();

	private PlatformConfig theConfig, origConfig;
	DbEditorFrame parent = null;
    GridBagLayout gridBagLayout1 = new GridBagLayout();
    JButton editSensorButton = new JButton();
    JScrollPane jScrollPane2 = new JScrollPane();
    JButton addSensorButton = new JButton();
    JButton deleteSensorButton = new JButton();
    BorderLayout borderLayout3 = new BorderLayout();
    JTable configSensorTable;
    JPanel jPanel6 = new JPanel();
    JPanel jPanel3 = new JPanel();
    JButton addScriptButton = new JButton();
    JButton deleteScriptButton = new JButton();
    JTable decodingScriptTable;
    JScrollPane jScrollPane3 = new JScrollPane();
    BorderLayout borderLayout4 = new BorderLayout();
    JPanel jPanel7 = new JPanel();
    JButton editScriptButton = new JButton();
    JPanel decodingScriptsPanel = new JPanel();
    GridBagLayout gridBagLayout2 = new GridBagLayout();
    JLabel nameLabel = new JLabel();
    JTextField nameField = new JTextField();
    JLabel equipmentModelLabel = new JLabel();
    JTextField equipmentModelField = new JTextField();
    JButton equipmentModelSelectButton = new JButton();
    JLabel numPlatformsLabel = new JLabel();
    JTextField numPlatformsField = new JTextField();
    GridBagLayout gridBagLayout3 = new GridBagLayout();
    GridBagLayout gridBagLayout4 = new GridBagLayout();
    GridBagLayout gridBagLayout5 = new GridBagLayout();
    JButton addDcpPmsButton = new JButton();

	/** Manages the view for this table-list of configuration sensors. */
	ConfigSensorTableModel configSensorTableModel;

	/** Manages the view for this table-list of decoding scripts. */
	DecodingScriptTableModel decodingScriptTableModel;

	/** 
	  Added to facilitate PlatformWizard, if false, then don't add the
	  the list of decoding scripts to the bottom of the panel.
	*/
	//public static boolean AddDecodingScriptsPanel = true;
	public boolean AddDecodingScriptsPanel = true;//JP - remove static so 
	//that we can use it from dbedit and platwiz - combine in the dcstool
	//launcher

	DecodingScriptEditDialog dsEditDlg = null;
	
	// If this edit panel was created from a PlatformEditPanel by hitting the
	// "Edit" (config) button, it will also set a transport medium select list
	// to be used by the LoadMessageDialog.
	private ArrayList<String> tmSelectList = null;

	/** no args c'tor for JBuilder only. */
    public ConfigEditPanel()
	{
	    theConfig = null;
        try {
		    entityOpsPanel = new EntityOpsPanel(this);
			configSensorTableModel = new ConfigSensorTableModel(null);
			configSensorTable = new JTable(configSensorTableModel);
			TableColumnAdjuster.adjustColumnWidths(configSensorTable,
				new int[] { 10, 15, 15, 15, 20, 25 });
			configSensorTable.getTableHeader().setReorderingAllowed(false);
 			decodingScriptTableModel = new DecodingScriptTableModel(null);
 			decodingScriptTable = new JTable(decodingScriptTableModel);
			TableColumnAdjuster.adjustColumnWidths(decodingScriptTable,
				new int[] { 30, 70 });
			decodingScriptTable.getTableHeader().setReorderingAllowed(false);
            jbInit();
            
            configSensorTable.addMouseListener(
    			new MouseAdapter()
    			{
    				public void mouseClicked(MouseEvent e)
    				{
    					if (e.getClickCount() == 2)
    					{
    						editSensorButtonPressed();
    					}
    				}
    			});
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }

	/**
	  Construct new panel to edit specified object.
	  @param pc the object to edit in this panel.
	*/
	public ConfigEditPanel(PlatformConfig pc)
	{
		this();
		setConfig(pc);
	}

	/**
	 * Constructor used by the Platform Wizard
	  Construct new panel to edit specified object.
	  @param addDecodingScriptsPanel
	*/
	public ConfigEditPanel(boolean addDecodingScriptsPanel)
	{
		AddDecodingScriptsPanel = addDecodingScriptsPanel;
		theConfig = null;
        try {
		    entityOpsPanel = new EntityOpsPanel(this);
			configSensorTableModel = new ConfigSensorTableModel(null);
			configSensorTable = new JTable(configSensorTableModel);
			TableColumnAdjuster.adjustColumnWidths(configSensorTable,
				new int[] { 10, 15, 15, 15, 45 });
			configSensorTable.getTableHeader().setReorderingAllowed(false);
 			decodingScriptTableModel = new DecodingScriptTableModel(null);
 			decodingScriptTable = new JTable(decodingScriptTableModel);
			TableColumnAdjuster.adjustColumnWidths(decodingScriptTable,
				new int[] { 30, 70 });
			decodingScriptTable.getTableHeader().setReorderingAllowed(false);
            jbInit();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
	}
	
	/**
	  Sets the object being edited in this panel.
	  @param pc the object to edit in this panel.
	*/
	public void setConfig(PlatformConfig pc)
	{
		origConfig = pc;
		theConfig = pc == null ? null : pc.copy();
		setTopObject(origConfig);
		fillFields();
		configSensorTableModel.setConfig(theConfig);
		decodingScriptTableModel.setConfig(theConfig);
		if (pc == null)
			disableFields();
		else
			enableFields();

	}

	/**
	 * Used just for import, sets the fields from the imported config.
	 * @param imported the imported config.
	 */
	public void setImportedConfig(PlatformConfig imported)
	{
		theConfig.copyFrom(imported);
		fillFields();
		enableFields();
	}

	/**
	  @return the config being edited, or null if none yet.
	*/
	public PlatformConfig getConfig() { return theConfig; }

	/** 
	  This method only called in dbedit.
	  Associates this panel with enclosing frame.
	  @param parent   Enclosing frame
	*/
	void setParent(DbEditorFrame parent)
	{
        this.add(entityOpsPanel, BorderLayout.SOUTH);
		this.parent = parent;

		// Kludge - When called from platform wizard
        jPanel1.add(equipmentModelSelectButton, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
	}

	/** Fills the GUI controls with values from the object. */
	private void fillFields()
	{
		if (theConfig == null)
			return;
		descriptionArea.setText(theConfig.description);
		nameField.setText(theConfig.configName);
		EquipmentModel em = theConfig.equipmentModel;
		equipmentModelField.setText(em == null ? "" : em.name);
		numPlatformsField.setText("" + theConfig.numPlatformsUsing);
	}

	/**
	  Gets the data from the fields & puts it back into the config object.
	  @return the internal copy of the configuration.
	*/
	public PlatformConfig getDataFromFields()
	{
		if (theConfig == null)
			return null;
		theConfig.description = descriptionArea.getText();
		theConfig.configName = nameField.getText();
		return theConfig;
	}

	/** Initializes GUI components */
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			dbeditLabels.getString("ConfigEditPanel.sensors"));
        titledBorder2 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			dbeditLabels.getString("ConfigEditPanel.decodingScripts"));
        titledBorder3 = new TitledBorder(
			BorderFactory.createLineBorder(new Color(153, 153, 153),2),
			genericLabels.getString("description"));
        this.setLayout(borderLayout1);
        jPanel5.setLayout(gridBagLayout1);
        jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(titledBorder3);
        jPanel1.setLayout(gridBagLayout2);
        jPanel8.setLayout(gridBagLayout3);
        editSensorButton.setText(
			dbeditLabels.getString("ConfigEditPanel.editSensorButton"));
        editSensorButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.editSensorButtonTT"));
        editSensorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                editSensorButtonPressed();
            }
        });
        addSensorButton.setText(
			dbeditLabels.getString("ConfigEditPanel.addSensorButton"));
        addSensorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addSensorButton_actionPerformed(e);
            }
        });
        deleteSensorButton.setText(
			dbeditLabels.getString("ConfigEditPanel.deleteSensorButton"));
        deleteSensorButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.deleteSensorButtonTT"));
        deleteSensorButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteSensorButton_actionPerformed(e);
            }
        });
        borderLayout3.setHgap(10);
        jPanel6.setLayout(gridBagLayout4);
        jPanel3.setLayout(borderLayout3);
        jPanel3.setBorder(titledBorder1);
        addScriptButton.setText(genericLabels.getString("add"));
        addScriptButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.addScriptButtonTT"));
        addScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addScriptButton_actionPerformed(e);
            }
        });
        deleteScriptButton.setText(genericLabels.getString("delete"));
        deleteScriptButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.deleteScriptButtonTT"));
        deleteScriptButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                deleteScriptButton_actionPerformed(e);
            }
        });
        borderLayout4.setHgap(10);
        jPanel7.setLayout(gridBagLayout5);
        editScriptButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.editScriptButtonTT"));
        editScriptButton.setText(genericLabels.getString("edit"));
        editScriptButton.addActionListener(new java.awt.event.ActionListener() 
		{
            public void actionPerformed(ActionEvent e) {
                editScriptButtonPressed();
            }
        });
        decodingScriptsPanel.setLayout(borderLayout4);
        decodingScriptsPanel.setBorder(titledBorder2);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nameLabel.setText(genericLabels.getString("nameLabel"));
        nameField.setEditable(false);
        equipmentModelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        equipmentModelLabel.setText(
			dbeditLabels.getString("ConfigEditPanel.equipmentModelLabel"));
        equipmentModelField.setEditable(false);
        equipmentModelSelectButton.setMnemonic('0');
        equipmentModelSelectButton.setText(genericLabels.getString("select"));
        equipmentModelSelectButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                equipmentModelSelectButton_actionPerformed(e);
            }
        });
        numPlatformsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        numPlatformsLabel.setText(
			dbeditLabels.getString("ConfigEditPanel.numPlatforms"));
        numPlatformsField.setEditable(false);
        addDcpPmsButton.setText(
			dbeditLabels.getString("ConfigEditPanel.addDcpPMs"));
        addDcpPmsButton.setToolTipText(
			dbeditLabels.getString("ConfigEditPanel.addDcpPMsTT"));
        addDcpPmsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addDcpPmsButton_actionPerformed(e);
            }
        });
        this.add(jPanel5, BorderLayout.CENTER);
        jPanel5.add(jPanel8, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        jPanel8.add(jPanel1, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 5, 0), 0, 0));
        jPanel1.add(nameLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 0, 2, 2), 0, 0));
        jPanel1.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        jPanel1.add(equipmentModelLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 5, 2, 2), 0, 0));
        jPanel1.add(equipmentModelField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));


        jPanel1.add(numPlatformsLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        jPanel1.add(numPlatformsField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        jPanel8.add(jPanel2, new GridBagConstraints(1, 0, 1, 1, 1.5, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 10, 0, 0), 0, 22));
        jPanel2.add(jScrollPane1, BorderLayout.CENTER);
        jPanel5.add(jPanel3, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        jPanel3.add(jScrollPane2, BorderLayout.CENTER);
        jPanel3.add(jPanel6, BorderLayout.EAST);
        jPanel6.add(addSensorButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
        jPanel6.add(editSensorButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
        jPanel6.add(deleteSensorButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 0, 4), 0, 0));
        jPanel6.add(addDcpPmsButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 4, 5, 4), 0, 0));
		if (AddDecodingScriptsPanel)
        jPanel5.add(decodingScriptsPanel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.5
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        decodingScriptsPanel.add(jScrollPane3, BorderLayout.CENTER);
        decodingScriptsPanel.add(jPanel7, BorderLayout.EAST);
        jPanel7.add(addScriptButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
        jPanel7.add(editScriptButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
        jPanel7.add(deleteScriptButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
            ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(2, 4, 2, 4), 0, 0));
        jScrollPane3.getViewport().add(decodingScriptTable, null);
        jScrollPane2.getViewport().add(configSensorTable, null);
        jScrollPane1.getViewport().add(descriptionArea, null);
    }

	/**
	  Called when the Equipment Model Select button is pressed.
	  @param e ignored.
	*/
    void equipmentModelSelectButton_actionPerformed(ActionEvent e)
	{
		EquipmentModelSelectDialog dlg = new EquipmentModelSelectDialog();
		dlg.setSelection(theConfig.equipmentModel);
		launchDialog(dlg);
		if (!dlg.cancelled())
		{
			EquipmentModel mod = dlg.getSelectedEquipmentModel();
			theConfig.equipmentModel = mod;
			//theConfig.equipmentId =
			//	mod == null ? Constants.undefinedId : mod.equipmentId;
			equipmentModelField.setText(mod == null ? "" : mod.name);
		}
    }

	/**
	  Called when the Add Sensor button is pressed.
	  @param e ignored.
	*/
    void addSensorButton_actionPerformed(ActionEvent e)
	{
		ConfigSensor cs = new ConfigSensor(theConfig,
			configSensorTableModel.highestSensorNumber() + 1);
		theConfig.addSensor(cs);
		validateDecodingScriptSensors();
		ConfigSensorEditDialog dlg = new ConfigSensorEditDialog(cs, true);

		// This is a modal dialog box; this call doesn't return until the
		// box goes away:
		launchDialog(dlg);

		configSensorTableModel.fireTableDataChanged();
    }

	/**
	  Called when the EditSensor button is pressed.
	*/
    void editSensorButtonPressed()
	{
		int r = configSensorTable.getSelectedRow();
		ConfigSensor cs;
		if (r == -1 ||
		    (cs = configSensorTableModel.getObjectAt(r)) == null)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("ConfigEditPanel.selectSensorEditMsg"));
			return;
		}

		ConfigSensorEditDialog dlg = new ConfigSensorEditDialog(cs);
		launchDialog(dlg);
		configSensorTableModel.fireTableDataChanged();
    }

	/**
	  Called when the Delete Sensor button is pressed.
	  @param e ignored.
	*/
    void deleteSensorButton_actionPerformed(ActionEvent e)
	{
    	//Allow multiple deletes
		int nrows = configSensorTable.getSelectedRowCount();
		if (nrows == 0)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("ConfigEditPanel.selectSensorDelMsg"));
			return;
		}
		int[] rows = configSensorTable.getSelectedRows();
		ConfigSensor cs[] = new ConfigSensor[nrows];
		for (int x = 0; x < nrows; x++)
			cs[x] = configSensorTableModel.getObjectAt(rows[x]);
		
//		int r = configSensorTable.getSelectedRow();
//		ConfigSensor cs;
//		if (r == -1 ||
//		    (cs = configSensorTableModel.getObjectAt(r)) == null)
//		{
//			TopFrame.instance().showError(
//				dbeditLabels.getString("ConfigEditPanel.selectSensorDelMsg"));
//			return;
//		}

		int rsp = JOptionPane.YES_OPTION;
		if (!DecodesDbEditor.turnOffPopUps) //if true just remove do not ask
		{
			rsp = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("ConfigEditPanel.confirmDelSensorMsg"),
				dbeditLabels.getString("ConfigEditPanel.confirmDeleteTitle"),
				JOptionPane.YES_NO_OPTION);
		}
		if (rsp == JOptionPane.YES_OPTION)
		{
			for (int i = 0; i < nrows; i++)
			{
				if (cs[i] != null)
				{
					configSensorTableModel.remove(cs[i]);					
				}
			}
		    configSensorTableModel.fireTableDataChanged();
			validateDecodingScriptSensors();
		}
    }

	/**
	  Called when the Add Script button is pressed.
	  @param e ignored.
	*/
    void addScriptButton_actionPerformed(ActionEvent e)
	{
		DecodesScript ds = new DecodesScript(theConfig, "");
		ds.scriptType = Constants.scriptTypeDecodes;
		for(Iterator it = theConfig.getSensors(); it.hasNext(); )
		{
			ConfigSensor cs = (ConfigSensor)it.next();
			ScriptSensor ss = new ScriptSensor(ds, cs.sensorNumber);
			ss.rawConverter = new UnitConverterDb("raw", "raw");
			ss.rawConverter.algorithm = Constants.eucvt_none;
			ds.addScriptSensor(ss);
			//ds.scriptSensors.add(ss);
		}
		
		if (dsEditDlg == null)
		{
			dsEditDlg = new DecodingScriptEditDialog(ds, this);
			launchDialog(dsEditDlg);
		}
		else
		{
			dsEditDlg.setDecodesScript(ds);
			dsEditDlg.setVisible(true);
		}
		decodingScriptTableModel.fireTableDataChanged();
   }

	/**
	  Called when the Edit Script button is pressed.
	*/
    void editScriptButtonPressed()
	{
		validateDecodingScriptSensors();
			
		int r = 
			decodingScriptTable.getRowCount() == 1 ? 0 :
			decodingScriptTable.getSelectedRow();

		DecodesScript ds;
		if (r == -1 ||
		    (ds = decodingScriptTableModel.getObjectAt(r)) == null)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("ConfigEditPanel.selectScriptEditMsg"));
			return;
		}
//		if (!ds.scriptType.equalsIgnoreCase(Constants.scriptTypeDecodes))
//		{
//			TopFrame.instance().showError(
//				"Only standard DECODES scripts can be edited.");
//			return;
//		}
		
		if (dsEditDlg == null)
		{
			dsEditDlg = new DecodingScriptEditDialog(ds, this);
			launchDialog(dsEditDlg);
		}
		else
		{
			dsEditDlg.setDecodesScript(ds);
//			try {
//				dsEditDlg.jbInit();
//				dsEditDlg.pack();
//			} catch (Exception e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			dsEditDlg.setVisible(true);
		}
		decodingScriptTableModel.fireTableDataChanged();
    }

	/**
	  Called when the Delete Script button is pressed.
	  @param e ignored.
	*/
    void deleteScriptButton_actionPerformed(ActionEvent e)
	{
		int r = decodingScriptTable.getSelectedRow();
		DecodesScript ds;
		if (r == -1 ||
		    (ds = decodingScriptTableModel.getObjectAt(r)) == null)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("ConfigEditPanel.selectScriptDelMsg"));
			return;
		}

		int rsp = JOptionPane.YES_OPTION;
		if (!DecodesDbEditor.turnOffPopUps) //if true just remove do not ask
		{
			rsp = JOptionPane.showConfirmDialog(this,
				dbeditLabels.getString("ConfigEditPanel.confirmDelScriptMsg"),
				dbeditLabels.getString("ConfigEditPanel.confirmDeleteTitle"),
				JOptionPane.YES_NO_OPTION);
		}
		if (rsp == JOptionPane.YES_OPTION)
		{
			decodingScriptTableModel.remove(ds);
		    decodingScriptTableModel.fireTableDataChanged();
		}
    }

	/**
	 * From ChangeTracker interface.
	 * @return true if changes have been made to this
	 * screen since the last time it was saved.
	 */
	public boolean hasChanged()
	{
		getDataFromFields();
		return !origConfig.equals(theConfig);
	}

	/**
	 * From ChangeTracker interface, save the changes back to the database 
	 * & reset the hasChanged flag.
	 * @return true if object was successfully saved.
	 */
	public boolean saveChanges()
	{
		validateDecodingScriptSensors();
		getDataFromFields();

		// Write the changes out to the database.
		try
		{
Logger.instance().debug1("ConfigEditPanel.saveChanges - writing config.");
			theConfig.write();
		}
		catch(DatabaseException e)
		{
			parent.showError(e.toString());
			return false;
		}

		// Replace origConfig in ConfigList with the modified config.
		Database.getDb().platformConfigList.remove(origConfig);
		Database.getDb().platformConfigList.add(theConfig);

		// Replace origConfig in every platform using this config.
		for(Iterator it = Database.getDb().platformList.iterator();
			it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			if (theConfig.configName.equalsIgnoreCase(p.getConfigName()))
//			if (p.platformConfig == origConfig)
			{
				try
				{
					if (!p.isComplete())
{Logger.instance().debug1("ConfigEditPanel.saveChanges - reading platform.");
						p.read();
}
						p.setConfig(theConfig);
{Logger.instance().debug1("ConfigEditPanel.saveChanges - writing platform.");
					p.write();
}
				}
				catch(DatabaseException e)
				{
					TopFrame.instance().showError(
						LoadResourceBundle.sprintf(
							dbeditLabels.getString(
								"ConfigEditPanel.cannotWritePlatform"),
							p.makeFileName(), e.toString()));
				}
			}
		}
Logger.instance().debug1("ConfigEditPanel.saveChanges - writing platform list.");

		try { Database.getDb().platformList.write(); }
		catch(DatabaseException e)
		{
			TopFrame.instance().showError(
				dbeditLabels.getString("ConfigEditPanel.cannotWriteList")
				+ e.toString());
		}

		// Config List Panel keeps its own vector, replace old with new:
		if (parent != null)
		{
			ConfigsListPanel clp = parent.getConfigsListPanel();
			clp.replace(origConfig, theConfig);
		}

		// Make a new copy in case user wants to keep editing.
		origConfig = theConfig;
		theConfig = origConfig.copy();
		setTopObject(origConfig);
		configSensorTableModel.setConfig(theConfig);
		decodingScriptTableModel.setConfig(theConfig);
		return true;
	}

	/** @see EntityOpsController */
	public String getEntityName() { return "PlatformConfig"; }

	/** @see EntityOpsController */
	public void commitEntity()
	{
		if (!saveChanges())
			return;
	}

	/** @see EntityOpsController */
	public void closeEntity()
	{
		if (hasChanged())
		{
			int r = JOptionPane.showConfirmDialog(this, 
				genericLabels.getString("saveChanges"));
			if (r == JOptionPane.CANCEL_OPTION)
				return;
			else if (r == JOptionPane.YES_OPTION)
			{
				if (!saveChanges())
					return;
			}
			else if (r == JOptionPane.NO_OPTION)
				;
		}
		if (parent != null)
		{
			DbEditorTabbedPane configsTabbedPane = 
				parent.getConfigsTabbedPane();
			configsTabbedPane.remove(this);
		}
	}

	/**
	 * Called from File - CloseAll to close this tab, abandoning any changes.
	 */
	public void forceClose()
	{
		DbEditorTabbedPane configsTabbedPane = parent.getConfigsTabbedPane();
		configsTabbedPane.remove(this);
	}

	/** @see EntityOpsController */
	public void help()
	{
	}


	/**
	  Called when the config sensors have changed.  Makes sure that there is
	  exactly one ScriptSensor for each config sensor, and no extras.
	*/
	public void validateDecodingScriptSensors()
	{
		int nsensors = theConfig.getNumSensors();
		int sensnums[] = new int[nsensors];

		for(Iterator dsit = theConfig.decodesScripts.iterator(); dsit.hasNext(); )
		{
			DecodesScript ds = (DecodesScript)dsit.next();

			// build an array of all valid sensor numbers.
			int i = 0;
			for(Iterator csit = theConfig.getSensors(); csit.hasNext(); )
			{
				ConfigSensor cs = (ConfigSensor)csit.next();
				sensnums[i++] = cs.sensorNumber;
			}

			for(Iterator ssit = ds.scriptSensors.iterator(); ssit.hasNext(); )
			{
				// Make sure each script sensor still refers to a config sensor.
				ScriptSensor ss = (ScriptSensor)ssit.next();
				for(i=0; i<nsensors; i++)
					if (ss.sensorNumber == sensnums[i])
					{
						sensnums[i] = -1;
						break;
					}
				if (i == nsensors) // fell through - invalid sensor number.
					ssit.remove();
			}

			// Add script sensors for each unrepresented config sensor.
			for(i=0; i < nsensors; i++)
				if (sensnums[i] != -1)
				{
					ScriptSensor ss = new ScriptSensor(ds, sensnums[i]);
					ss.rawConverter = new UnitConverterDb("raw", "raw");
					ss.rawConverter.algorithm = Constants.eucvt_none;
					ds.addScriptSensor(ss);
				}
		}
	}

	private ConfigSensor findSensorByCode(String codeValue)
	{
		for(Iterator<ConfigSensor> csit = theConfig.getSensors(); csit.hasNext();)
		{
			ConfigSensor cs = csit.next();
			for(Iterator<DataType> dtit = cs.getDataTypes(); dtit.hasNext(); )
			{
				DataType dt = dtit.next();
				if (dt.getCode().equalsIgnoreCase(codeValue))
					return cs;
			}
		}
		return null;
	}
	
	private ConfigSensor findSensorByName(String sensorName)
	{
		for(Iterator<ConfigSensor> csit = theConfig.getSensors(); csit.hasNext();)
		{
			ConfigSensor cs = csit.next();
			if (cs.sensorName.equalsIgnoreCase(sensorName))
				return cs;
		}
		return null;
	}

	/**
	  Called when the Add Performance Measurements button is pressed.
	  Adds the canned GOES DCP parameters used by USGS.
	  @param e ignored.
	*/
    void addDcpPmsButton_actionPerformed(ActionEvent e)
	{
    	PMSelectDialog dlg = new PMSelectDialog(parent);
    	parent.launchDialog(dlg);
    	if (dlg.isCancelled())
    		return;
    	ArrayList<String> pmNames = dlg.getResults();
    	int sensorNum = configSensorTableModel.highestSensorNumber() + 1;
    	for(String n : pmNames)
    	{
    		ConfigSensor cs = this.findSensorByName(n);
    		if (cs != null)
    		{
    			parent.showError("There is already a sensor named '" + n + "' -- cannot add.");
    			continue;
    		}
    		
    		cs = new ConfigSensor(theConfig, sensorNum++);
    		cs.sensorName = n;
			cs.recordingMode = Constants.recordingModeVariable;
    		if (n.equalsIgnoreCase(GoesPMParser.DCP_ADDRESS))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-DCPAddress"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YA"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.MESSAGE_LENGTH))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Langth-Message"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YL"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.SIGNAL_STRENGTH))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Power-Signal"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YS"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.FAILURE_CODE))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Failure"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YF"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.FREQ_OFFSET))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Freq-Offset"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YO"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.MOD_INDEX))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Ratio-ModIndex"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YI"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.CHANNEL))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Channel"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YC"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.SPACECRAFT))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Code-Spacecraft"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YP"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.BAUD))
    		{
           		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Freq-Baud"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YB"));
    		}
    		else if (n.equalsIgnoreCase(GoesPMParser.DCP_MSG_FLAGS))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Binary-Flags"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YG"));
    		}
    		else if (n.equalsIgnoreCase(IridiumPMParser.LATITUDE))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-Latitude"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YA"));
    		}
    		else if (n.equalsIgnoreCase(IridiumPMParser.LONGITUDE))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-Longitude"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YO"));
    		}
    		else if (n.equalsIgnoreCase(IridiumPMParser.CEP_RADIUS))
    		{
        		cs.addDataType(DataType.getDataType(Constants.datatype_CWMS, "Rotation-CEPRadius"));
        		cs.addDataType(DataType.getDataType(Constants.datatype_SHEF, "YR"));
    		}
    		theConfig.addSensor(cs);
    	}
    	
//    	int sensorNum = configSensorTableModel.highestSensorNumber() + 1;
//    	if (sensorNum < 101)
//    		sensorNum = 101;
//		ConfigSensor cs = findSensorByCode("72114");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72114"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72114"));
//			cs.sensorName = "EIRP(DBM)";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72113");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72113"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72113"));
//			cs.sensorName = "MOD(db)";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72115");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72115"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72115"));
//			cs.sensorName = "FREQ(hz)";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72112");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72112"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72112"));
//			cs.sensorName = "(S+N)/N";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72111");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72111"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72111"));
//			cs.sensorName = "ER #";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("68229");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "68229"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "68229"));
//			cs.sensorName = "SOURCE";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72117");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72117"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72117"));
//			cs.sensorName = "TIME OFF";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
//
//		cs = findSensorByCode("72116");
//		if (cs == null)
//		{
//			cs = new ConfigSensor(theConfig, sensorNum++);
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_EPA, "72116"));
//			cs.addDataType(
//				DataType.getDataType(Constants.datatype_USGS, "72116"));
//			cs.sensorName = "BAD CHRS";
//			cs.recordingMode = Constants.recordingModeVariable;
//			cs.getProperties().setProperty("StatisticsCode", "00011");
//			theConfig.addSensor(cs);
//		}
		validateDecodingScriptSensors();
		configSensorTableModel.fireTableDataChanged();
    }

	/** Disables all fields -- used when called with null config. */
	private void disableFields()
	{
	    descriptionArea.setEnabled(false);
	    editSensorButton.setEnabled(false);
	    addSensorButton.setEnabled(false);
	    deleteSensorButton.setEnabled(false);
	    configSensorTable.setEnabled(false);
	    addScriptButton.setEnabled(false);
	    deleteScriptButton.setEnabled(false);
	    decodingScriptTable.setEnabled(false);
	    editScriptButton.setEnabled(false);
	    nameField.setEnabled(false);
	    equipmentModelField.setEnabled(false);
	    numPlatformsField.setEnabled(false);
	    addDcpPmsButton.setEnabled(false);
    	equipmentModelSelectButton.setEnabled(false);
	}

	/** Enables all fields. */
	private void enableFields()
	{
	    descriptionArea.setEnabled(true);
	    editSensorButton.setEnabled(true);
	    addSensorButton.setEnabled(true);
	    deleteSensorButton.setEnabled(true);
	    configSensorTable.setEnabled(true);
	    addScriptButton.setEnabled(true);
	    deleteScriptButton.setEnabled(true);
	    decodingScriptTable.setEnabled(true);
	    editScriptButton.setEnabled(true);
	    nameField.setEnabled(true);
	    equipmentModelField.setEnabled(true);
	    numPlatformsField.setEnabled(true);
	    addDcpPmsButton.setEnabled(true);
    	equipmentModelSelectButton.setEnabled(true);
	}

	public ArrayList<String> getTmSelectList()
	{
		return tmSelectList;
	}

	public void setTmSelectList(ArrayList<String> tmSelectList)
	{
		this.tmSelectList = tmSelectList;
	}

	public PlatformConfig getOrigConfig()
	{
		return origConfig;
	}

}


/**
This class provides the interface between the JTable and the list of
ConfigSensors stored in the PlatformConfig object.
*/
class ConfigSensorTableModel extends AbstractTableModel
{
	static String columnNames[] =
	{ 
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.sensorTableCol0"),
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.sensorTableCol1"),
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.sensorTableCol2"),
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.sensorTableCol3"),
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.sensorTableCol4"),
		ConfigEditPanel.genericLabels.getString("properties")
	};

	private PlatformConfig theConfig;

	/** 
	  Construct table model for specified config. 
	  @param pc The configuration
	*/
	public ConfigSensorTableModel(PlatformConfig pc)
	{
		setConfig(pc);
	}

	/** 
	  Sets configuration 
	  @param pc The configuration
	*/
	void setConfig(PlatformConfig pc)
	{
		theConfig = pc;
		fillValues();
	}

	public void fillValues()
	{
		fireTableDataChanged();
	}

	public int getRowCount()
	{
		int r = theConfig == null ? 0 : theConfig.getNumSensors();
		return r;
	}

	public int getColumnCount() { return columnNames.length; }

	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	ConfigSensor getObjectAt(int r)
	{
		if (r < 0 || r >= getRowCount())
			return null;

		for(Iterator it = theConfig.getSensors(); it.hasNext();)
		{
			ConfigSensor cs = (ConfigSensor)it.next();
			if (r-- == 0)
				return cs;
		}
		return (ConfigSensor)null;
	}

	public Object getValueAt(int r, int c)
	{
		ConfigSensor cs = getObjectAt(r);
		if (cs == null)
			return "";

		String ret="";
		switch(c)
		{
		case 0: ret = "" + cs.sensorNumber; break;
		case 1: ret = cs.sensorName; break;
		case 2:
		{
			
			DataType dt = cs.getDataType(
				DecodesSettings.instance().dataTypeStdPreference);
			if (dt == null)
				dt = cs.getDataType(null);
			ret = dt == null ? "" : dt.toString();
			break;
		}
		case 3:
			ret = cs.recordingMode == Constants.recordingModeFixed ? "Fixed" :
				cs.recordingMode == Constants.recordingModeVariable ? "Variable" :
				"Undefined";
			break;
		case 4:
			if (cs.recordingMode == Constants.recordingModeFixed)
				ret = TimeOfDay.seconds2hhmmss(cs.timeOfFirstSample) + " "
					+ TimeOfDay.durationString(cs.recordingInterval);
			else
				ret = "N/A";
			break;
		case 5:
			ret = PropertiesUtil.props2string(cs.getProperties());
			break;
		}
		return ret;
	}

	void add(ConfigSensor ob)
	{
		theConfig.addSensor(ob);
		fireTableDataChanged();
	}

	void remove(ConfigSensor ob)
	{
		theConfig.removeSensor(ob);
		fireTableDataChanged();
	}

	int highestSensorNumber()
	{
		int ret = 0;
		for(Iterator<ConfigSensor> it = theConfig.getSensors(); it.hasNext();)
		{
			ConfigSensor cs = it.next();
			if (cs.sensorNumber > ret)
				ret = cs.sensorNumber;
		}
		return ret;
	}
}


/**
Table model for table of decoding scripts in a configuration.
*/
class DecodingScriptTableModel extends AbstractTableModel
{
	private PlatformConfig theConfig;

	static String columnNames[] =
	{ 
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.scriptTableCol0"),
		ConfigEditPanel.dbeditLabels.getString("ConfigEditPanel.scriptTableCol1")
	};

	/** Constructs table model for specified config. */
	public DecodingScriptTableModel(PlatformConfig pc)
	{
		setConfig(pc);
	}

	/** Sets the configuration */
	void setConfig(PlatformConfig pc)
	{
		theConfig = pc;
		fillValues();
	}

	/** Fills the table with values from the configuration. */

	public void fillValues()
	{
		fireTableDataChanged();
	}

	/** @returns number of rows */
	public int getRowCount()
	{
		return theConfig == null ? 0 : theConfig.decodesScripts.size();
	}

	/** @returns number of columns */
	public int getColumnCount() { return columnNames.length; }

	/** @returns specified column name */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/** 
	  Gets decoding script at specified row 
	  @param r the row
	*/
	DecodesScript getObjectAt(int r)
	{
		if (r < 0 || r >= theConfig.decodesScripts.size())
			return null;
		return (DecodesScript)theConfig.decodesScripts.elementAt(r);
	}

	/** 
	  Gets object (String) at specified row/column. 
	  @param r the row
	  @param c the column
	*/
	public Object getValueAt(int r, int c)
	{
		DecodesScript ds = getObjectAt(r);
		switch(c)
		{
		case 0: return ds.scriptName;
		case 1: return ds.scriptType;
		default: return "";
		}
	}

	/** 
	  Adds a decoding script to the model 
	  @param ob the DecodesScript
	*/
	void add(DecodesScript ob)
	{
		theConfig.decodesScripts.add(ob);
		fireTableDataChanged();
	}

	/** 
	  Removes a decoding script from the model 
	  @param ob the DecodesScript
	*/
	void remove(DecodesScript ob)
	{
		theConfig.decodesScripts.remove(ob);
		fireTableDataChanged();
	}
}
