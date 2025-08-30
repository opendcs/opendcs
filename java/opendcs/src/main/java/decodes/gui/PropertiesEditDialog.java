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
package decodes.gui;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.gui.properties.PropertiesEditPanelController;
import decodes.util.DecodesSettings;
import decodes.util.PropertiesOwner;

import java.util.Properties;
import java.util.ResourceBundle;
import java.awt.event.*;


/**
Dialog wrapper for a properties edit panel.
@see PropertiesEditPanelController
*/
public class PropertiesEditDialog extends JDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static ResourceBundle genericLabels = null;
    JPanel panel1 = new JPanel();
    JPanel jPanel1 = new JPanel();
    JLabel jLabel1 = new JLabel();
    JTextField entityNameField = new JTextField();
    BorderLayout borderLayout1 = new BorderLayout();
    FlowLayout flowLayout1 = new FlowLayout();
    PropertiesEditPanel propertiesEditPanel;
    JPanel jPanel2 = new JPanel();
    JButton okButton = new JButton();
    JButton cancelButton = new JButton();
    FlowLayout flowLayout2 = new FlowLayout();

	String entityName;
	private boolean okPressed = false;

	/**
	  Constructor.
	  @param entityName name of object that owns the properties.
	  @param properties the properties set to edit.
	*/
    public PropertiesEditDialog(String entityName, Properties properties)
	{
        super(GuiApp.topFrame, "Properties for " + entityName, true);
        genericLabels = getGenericLabels();

        this.setTitle(LoadResourceBundle.sprintf(
        		genericLabels.getString("PropertiesEditDialog.title"),
        		entityName));

		this.entityName = entityName;
        propertiesEditPanel = PropertiesEditPanel.from(properties);
		propertiesEditPanel.setOwnerDialog(this);

		try
		{
            jbInit();
            pack();
			entityNameField.requestFocus();
        }
        catch (Exception ex)
		{
            GuiHelpers.logGuiComponentInit(log, ex);
        }
    }

	public void setPropertiesOwner(PropertiesOwner propertiesOwner)
	{
		propertiesEditPanel.getModel().setPropertiesOwner(propertiesOwner);
	}

	/**
	 * @return resource bundle containing generic labels for the selected
	 * language.
	 */
	public static ResourceBundle getGenericLabels()
	{
		if (genericLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic", settings.language);
		}
		return genericLabels;
	}

	/** Initializes GUI components. */
    void jbInit() throws Exception {
        panel1.setLayout(borderLayout1);
        jPanel1.setLayout(flowLayout1);
        jLabel1.setText(
        	genericLabels.getString("PropertiesEditDialog.propFor"));
        entityNameField.setPreferredSize(new Dimension(120, 21));
        entityNameField.setEditable(false);
        entityNameField.setText(entityName);
        okButton.setText(
        genericLabels.getString("PropertiesEditDialog.OK"));
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
        jPanel2.setLayout(flowLayout2);
        flowLayout2.setHgap(35);
        flowLayout2.setVgap(10);
        panel1.setMinimumSize(new Dimension(300, 120));
        getContentPane().add(panel1);
        panel1.add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jLabel1, null);
        jPanel1.add(entityNameField, null);
        panel1.add(propertiesEditPanel, BorderLayout.CENTER);
        panel1.add(jPanel2, BorderLayout.SOUTH);
        jPanel2.add(okButton, null);
        jPanel2.add(cancelButton, null);
    }

	/**
	  Called when OK button pressed.
	  Saves the changes and closes the dialog.
	  param e ignored.
	*/
    void okButton_actionPerformed(ActionEvent e)
	{
		propertiesEditPanel.getModel().saveChanges();
		closeDlg();
		okPressed = true;
    }

	/** Closes the dialog.  */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  Called when cancel button pressed.
	  Closes the dialog without save.
	  param e ignored.
	*/
    void cancelButton_actionPerformed(ActionEvent e)
	{
		closeDlg();
    }

	public boolean isOkPressed()
	{
		return okPressed;
	}

}
