/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.2  2017/05/18 12:29:00  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.1  2017/05/17 20:36:56  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.Logger;
import ilex.util.StringPair;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class AlarmDefDialog extends GuiDialog
{
	private JTextField patternField = new JTextField();
	@SuppressWarnings("rawtypes")
	private JComboBox priorityCombo = null;
	private boolean ok;
	AlarmEditPanel parentPanel = null;
	private String priorities[] = { Logger.priorityName[Logger.E_INFORMATION],
		Logger.priorityName[Logger.E_WARNING], Logger.priorityName[Logger.E_FAILURE],
		Logger.priorityName[Logger.E_FATAL] };


	public AlarmDefDialog(AlarmEditPanel parentPanel)
	{
		super(parentPanel.parentFrame, 
			parentPanel.parentFrame.eventmonLabels.getString("alarmDef"), true);
		this.parentPanel = parentPanel;
		guiInit();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void guiInit()
	{
		ResourceBundle labels = parentPanel.parentFrame.eventmonLabels;
		
		JPanel centerPanel = new JPanel(new GridBagLayout());
		centerPanel.add(new JLabel(labels.getString("priority") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		priorityCombo = new JComboBox(priorities);
		centerPanel.add(priorityCombo,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		centerPanel.add(new JLabel(labels.getString("pattern") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(patternField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 0, 0));
			
		ok = false;
		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());
		contpane.add(centerPanel, BorderLayout.CENTER);

		// South will contain 'OK' and 'Cancel' buttons.
		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 7, 7));

		JButton okButton = new JButton(parentPanel.parentFrame.genericLabels.getString("OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent av)
			{
				doOK();
			}
		});
		south.add(okButton);

		JButton cancelButton = new JButton(parentPanel.parentFrame.genericLabels.getString("cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent av)
			{
				doCancel();
			}
		});
		south.add(cancelButton);
		contpane.add(south, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(okButton);
		pack();
	}

	/** Called when OK pressed. */
	private void doOK()
	{
		ok = true;
		closeDlg();
	}

	/** Called when cancel pressed. */
	private void doCancel()
	{
		ok = false;
		closeDlg();
	}

	/** Closes the dialog */
	private void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	 * @return true if OK was pressed, false if Cancel was pressed.
	 */
	public boolean isOK()
	{
		return ok;
	}

	public String getPriority()
	{
		return priorities[priorityCombo.getSelectedIndex()];
	}

	public String getPattern()
	{
		String s = patternField.getText().trim();
		if (s.length() == 0)
			return null;
		return s;
	}
	
	public void setData(StringPair sp)
	{
		for(int idx = 0; idx < priorities.length; idx++)
		{
			if (sp.first.equalsIgnoreCase(priorities[idx]))
			{
				priorityCombo.setSelectedIndex(idx);
				break;
			}
		}
		patternField.setText(sp.second);
		ok = false;
	}
}
