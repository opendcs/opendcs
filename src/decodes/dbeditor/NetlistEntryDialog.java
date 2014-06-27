package decodes.dbeditor;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

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
public class NetlistEntryDialog 
	extends GuiDialog
{
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
			ex.printStackTrace();
		}
		this.editOb = entry;
		fillFields(editOb);
	}

	private void fillFields(NetworkListEntry editOb2)
	{
		idField.setText(editOb2.transportId);
		nameField.setText(editOb2.platformName != null ? editOb2.platformName : "");
		descField.setText(editOb2.description != null ? editOb2.description : "");
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
		editOb.platformName = name.length() == 0 ? null : name;
		editOb.description = desc.length() == 0 ? null : desc;
		return true;
	}
}
