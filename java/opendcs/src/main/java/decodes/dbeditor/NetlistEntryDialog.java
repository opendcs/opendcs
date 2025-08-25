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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.db.NetworkListEntry;
import decodes.gui.GuiDialog;

/**
 * Edit dialog for a network list entry.
 * @author mmaloney Mike Maloney, Cove Software LLC
 */
@SuppressWarnings("serial")
public class NetlistEntryDialog extends GuiDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
	static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
	private NetworkListEntry editOb = null;
	private JTextField idField = new JTextField();
	private JTextField nameField = new JTextField();
	private JTextField descField = new JTextField();
	private boolean _okPressed = false;

	public NetlistEntryDialog(NetworkListEntry entry)
	{
		super(DecodesDbEditor.getTheFrame(), "Network List Entry", true);
		try
		{
			guiInit();
			pack();
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
		this.editOb = entry;
		fillFields(editOb);
	}

	private void fillFields(NetworkListEntry editOb2)
	{
		idField.setText(editOb2.transportId);
		nameField.setText(editOb2.getPlatformName() != null ? editOb2.getPlatformName() : "");
		descField.setText(editOb2.getDescription() != null ? editOb2.getDescription() : "");
	}

	private void guiInit()
	{
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
			new JLabel(dbeditLabels.getString("NetlistEntryDialog.MediumId")),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(idField, 
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(5, 0, 5, 10), 80, 0));

		paramPanel.add(
			new JLabel(dbeditLabels.getString("NetlistEntryDialog.PlatformName")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(nameField, 
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 10), 80, 0));

		paramPanel.add(
			new JLabel(dbeditLabels.getString("NetlistEntryDialog.BriefDescription")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(5, 10, 5, 2), 0, 0));
		paramPanel.add(descField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 10), 200, 0));
	}

	protected void cancelPressed()
	{
		closeDlg();
	}

	protected void okPressed()
	{
		if (getDataFromFields())
		{
			closeDlg();
			_okPressed = true;
		}
	}
	
	public boolean wasOkPressed() { return _okPressed; }
	
	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}
	
	private boolean getDataFromFields()
	{
		String id = idField.getText().trim();
		String name = nameField.getText().trim();
		String desc = descField.getText().trim();
		
		if (id.length() == 0)
		{
			showError(dbeditLabels.getString("NetlistEntryDialog.BlankMediumId"));
			return false;
		}
		editOb.transportId = id;
		editOb.setPlatformName(name.length() == 0 ? null : name);
		editOb.setDescription(desc.length() == 0 ? null : desc);
		return true;
	}
}
