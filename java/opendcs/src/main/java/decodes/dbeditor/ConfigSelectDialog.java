/*
 * Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package decodes.dbeditor;

import java.awt.*;
import javax.swing.*;
import java.util.ResourceBundle;
import javax.swing.border.*;

import decodes.db.PlatformConfig;
import org.slf4j.LoggerFactory;

/** Dialog for selecting a configuration from a list. */
public class ConfigSelectDialog extends JDialog
{
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigSelectDialog.class);
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    JPanel panel1 = new JPanel();
    JPanel jPanel1 = new JPanel();
    FlowLayout flowLayout1 = new FlowLayout();
    JButton selectButton = new JButton();
    JButton cancelButton = new JButton();

	ConfigSelectController myController;
    BorderLayout borderLayout1 = new BorderLayout();
    JPanel jPanel2 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    TitledBorder titledBorder1;
    Border border1;
    ConfigSelectPanel configSelectPanel = new ConfigSelectPanel(this::selectButtonPressed);
	PlatformConfig config;

	/** 
	  Construct new dialog.
	  @param ctl the owner of this dialog to receive a call-back when a 
	  selection has been made.
	*/
    public ConfigSelectDialog(JFrame parent, ConfigSelectController ctl)
	{
        super(parent, dbeditLabels.getString("ConfigSelectDialog.title"), true);
		myController = ctl;
		allInit();
 	}

	private void allInit()
	{
		config = null;
        try {
            jbInit();
			getRootPane().setDefaultButton(selectButton);
            pack();
        }
        catch(Exception ex) {
            log.atInfo().log("Error in allInit() ",ex);
        }
    }

	/** Initializes GUI components */
    void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(
			new Color(153, 153, 153),2),
			dbeditLabels.getString("ConfigSelectDialog.title"));
        border1 = BorderFactory.createCompoundBorder(titledBorder1,BorderFactory.createEmptyBorder(5,5,5,5));
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        selectButton.setText(genericLabels.getString("select"));
        selectButton.addActionListener(e -> selectButtonPressed());
        cancelButton.setText(genericLabels.getString("cancel"));
        cancelButton.addActionListener(e -> cancel());
        flowLayout1.setHgap(35);
        flowLayout1.setVgap(10);
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(border1);
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(selectButton, null);
        jPanel1.add(cancelButton, null);
        panel1.add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(configSelectPanel, BorderLayout.NORTH);
    }

	/**
	  Called when the Select button is pressed.
	*/
    void selectButtonPressed()
	{
		config = configSelectPanel.getSelection();
		if (myController != null)
			myController.selectConfig(config);
		closeDlg();
    }

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when Cancel the button is pressed.
	*/
    void cancel()
	{
		config = null;
		closeDlg();
    }

	/** Returns the selected configuration, or null if none selected. */
	public PlatformConfig getSelectedConfig()
	{
		return config;
	}

	/** Sets current selection. */
	public void setSelection(String name)
	{
		if (name == null)
			configSelectPanel.clearSelection();
		else
			configSelectPanel.setSelection(name);
	}
}
