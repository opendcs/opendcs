package decodes.gui;

import ilex.util.LoadResourceBundle;

import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.*;

import decodes.util.DecodesSettings;

/**
Panel containing Add Edit Delete buttons used in several places.
*/
public class AddEditDeletePanel extends JPanel
{
	private static ResourceBundle genericLabels = null;
	private JButton addButton;
	private JButton editButton;
	private JButton deleteButton;

	public AddEditDeletePanel(ActionListener addAction, 
		ActionListener editAction, ActionListener deleteAction)
	{
		super(new GridBagLayout());
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		
		GridBagConstraints addConstraints = 
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(0, 6, 5, 6), 30, 0);

		GridBagConstraints editConstraints = 
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(0, 6, 5, 6), 0, 0);

		GridBagConstraints deleteConstraints =
			new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, 
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(0, 6, 5, 6), 0, 0);

		addButton = new JButton(genericLabels.getString("add"));
		add(addButton, addConstraints);
		setAddAction(addAction);
		editButton = new JButton(genericLabels.getString("edit"));
		add(editButton, editConstraints);
		setEditAction(editAction);
		deleteButton = new JButton(genericLabels.getString("delete"));
		add(deleteButton, deleteConstraints);
		setDeleteAction(deleteAction);
	}

	public void setAddAction(ActionListener al)
	{
		addButton.addActionListener(al);
	}

	public void setEditAction(ActionListener al)
	{
		editButton.addActionListener(al);
	}

	public void setDeleteAction(ActionListener al)
	{
		deleteButton.addActionListener(al);
	}
}
