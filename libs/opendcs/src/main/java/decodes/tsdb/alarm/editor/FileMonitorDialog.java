/**
 * $Id$
 * 
 * Copyright 2017 Cove Software, LLC. All rights reserved.
 * 
 * $Log$
 * Revision 1.4  2018/03/23 20:12:20  mmaloney
 * Added 'Enabled' flag for process and file monitors.
 *
 * Revision 1.3  2017/10/04 17:25:26  mmaloney
 * Fix AEP Bugs
 *
 * Revision 1.2  2017/05/18 12:29:00  mmaloney
 * Code cleanup. Remove System.out debugs.
 *
 * Revision 1.1  2017/05/17 20:36:56  mmaloney
 * First working version.
 *
 */
package decodes.tsdb.alarm.editor;

import ilex.util.TextUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Calendar;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.gui.GuiDialog;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.alarm.FileMonitor;

@SuppressWarnings("serial")
public class FileMonitorDialog extends GuiDialog
{
	private JTextField pathField = new JTextField(25);
	@SuppressWarnings("rawtypes")
	private JComboBox priorityCombo = null;
	AlarmEditPanel parentPanel = null;
	private String priorities[] = { Logger.priorityName[Logger.E_INFORMATION],
		Logger.priorityName[Logger.E_WARNING], Logger.priorityName[Logger.E_FAILURE],
		Logger.priorityName[Logger.E_FATAL] };
	private JTextField maxFilesField = new JTextField();
	private JTextField maxFilesHintField = new JTextField();
	private JTextField maxSizeField = new JTextField();
	private JTextField maxSizeHintField = new JTextField();
	private JTextField maxAgeField = new JTextField();
	private JTextField maxAgeHintField = new JTextField();
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JComboBox maxAgeUnitsCombo = new JComboBox(
		new String[] { "Seconds", "Minutes", "Hours", "Days" });
	private JCheckBox alarmOnDeleteCheck = new JCheckBox();
	private JTextField alarmOnDeleteHintField = new JTextField();
	static private JFileChooser jFileChooser = new JFileChooser();
    static
    {
        jFileChooser.setCurrentDirectory(new File(EnvExpander.expand("$DCSTOOL_USERDIR")));
        jFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    }
    private JCheckBox enabledCheck = new JCheckBox("Enabled?");
    boolean changed = false;
    private FileMonitor theFM = null;

	public FileMonitorDialog(AlarmEditPanel parentPanel)
	{
		super(parentPanel.parentFrame, 
			parentPanel.parentFrame.eventmonLabels.getString("fileMonitor"), true);
		this.parentPanel = parentPanel;
		guiInit();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void guiInit()
	{
		ResourceBundle labels = parentPanel.parentFrame.eventmonLabels;
		ResourceBundle genlabels = parentPanel.parentFrame.genericLabels;

		JPanel centerPanel = new JPanel(new GridBagLayout());
		Container contpane = getContentPane();
		contpane.setLayout(new BorderLayout());
		contpane.add(centerPanel, BorderLayout.CENTER);

		// Line 0: Path: [path field] [select button]
		centerPanel.add(new JLabel(labels.getString("path") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(pathField,
			new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 0), 0, 0));
		JButton selectFileButton = new JButton(genlabels.getString("select"));
		selectFileButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent av)
			{
				selectFileButtonPressed();
			}
		});
		centerPanel.add(selectFileButton,
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 5, 2, 10), 0, 0));

		// Line 1: Priority: [priority combo] Enabled-Checkbox
		centerPanel.add(new JLabel(labels.getString("priority") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		priorityCombo = new JComboBox(priorities);
		centerPanel.add(priorityCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		centerPanel.add(enabledCheck,
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 10), 0, 0));
		
		// Line 2: Max Files: [field] (For Directories)
		centerPanel.add(new JLabel(labels.getString("maxFiles")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxFilesField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 25), 0, 0));
		centerPanel.add(new JLabel(labels.getString("maxFilesExpl")),
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 10), 0, 0));
		
		// Line 3: Hint for max Files
		centerPanel.add(new JLabel(labels.getString("maxFilesHint")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxFilesHintField,
			new GridBagConstraints(1, 3, 3, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 0, 0));


		// Line 4: Max Size: [field] (For Regular Files)
		centerPanel.add(new JLabel(labels.getString("maxSize")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxSizeField,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 25), 0, 0));
		centerPanel.add(new JLabel(labels.getString("maxSizeExpl")),
			new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 10), 0, 0));
		
		// Line 5: Hint for max size
		centerPanel.add(new JLabel(labels.getString("maxSizeHint")),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxSizeHintField,
			new GridBagConstraints(1, 5, 3, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 0, 0));
		
		// Line 6: Max Age: [int field] [units pulldown]
		centerPanel.add(new JLabel(labels.getString("maxAge")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxAgeField,
			new GridBagConstraints(1, 6, 1, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 25), 0, 0));
		centerPanel.add(maxAgeUnitsCombo,
			new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 5, 2, 10), 0, 0));

		// Line 7: Hint for max age
		centerPanel.add(new JLabel(labels.getString("maxAgeHint")),
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(maxAgeHintField,
			new GridBagConstraints(1, 7, 3, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 10), 0, 0));
		
		// Line 8: [] Alarm On Delete
		alarmOnDeleteCheck.setText(labels.getString("alarmOnDelete"));
		centerPanel.add(alarmOnDeleteCheck,
			new GridBagConstraints(1, 8, 2, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 0, 0));

		// Line 9: Hint for alarm on delete
		centerPanel.add(new JLabel(labels.getString("alarmOnDeleteHint")),
			new GridBagConstraints(0, 9, 1, 1, 0.0, 0.0, 
				GridBagConstraints.EAST, GridBagConstraints.NONE, 
				new Insets(2, 10, 2, 2), 0, 0));
		centerPanel.add(alarmOnDeleteHintField,
			new GridBagConstraints(1, 9, 3, 1, 1.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 0, 0));

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

	protected void selectFileButtonPressed()
	{
        if (jFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            pathField.setText(jFileChooser.getSelectedFile().getPath());
	}

	/** 
	 * Called when OK pressed. Copy back data to object and set 'changed' flag.
	 */
	private void doOK()
	{
		// Validation first
		String s = pathField.getText().trim();
		if (s.length() == 0)
		{
			parentPanel.parentFrame.showError(
				parentPanel.parentFrame.eventmonLabels.getString("fileMonNoPath"));
			return;
		}
		
		int maxFiles = 0;
		s = maxFilesField.getText().trim();
		if (s.length() != 0)
		{
			try { maxFiles = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				parentPanel.parentFrame.showError(
					parentPanel.parentFrame.eventmonLabels.getString("nonNumericMaxFiles"));
				return;
			}
		}
		
		long maxSize = 0;
		s = maxSizeField.getText().trim();
		if (s.length() != 0)
		{
			try { maxSize = Long.parseLong(s); }
			catch(NumberFormatException ex)
			{
				parentPanel.parentFrame.showError(
					parentPanel.parentFrame.eventmonLabels.getString("nonNumericMaxSize"));
				return;
			}
		}

		String maxAge = null;
		s = maxAgeField.getText().trim();
		if (s.length() != 0)
		{
			try
			{
				int n = Integer.parseInt(s);
				if (n != 0)
				{
					maxAge = "" + n + " " + maxAgeUnitsCombo.getSelectedItem();
				}
			}
			catch(NumberFormatException ex)
			{
				parentPanel.parentFrame.showError(
					parentPanel.parentFrame.eventmonLabels.getString("nonNumericMaxAge"));
				return;
			}
		}
		
		// Settings are valid, set object and check for changes.
		s = pathField.getText().trim();
		if (!TextUtil.strEqual(s, theFM.getPath()))
		{
			theFM.setPath(s);
			changed = true;
		}
		if (theFM.getPriority() != priorityCombo.getSelectedIndex() + Logger.E_INFORMATION)
		{
			theFM.setPriority(priorityCombo.getSelectedIndex() + Logger.E_INFORMATION);
			changed = true;
		}
		if (maxFiles != theFM.getMaxFiles())
		{
			theFM.setMaxFiles(maxFiles);
			changed = true;
		}
		s = maxFilesHintField.getText().trim();
		if (!TextUtil.strEqual(s, theFM.getMaxFilesHint()))
		{
			theFM.setMaxFilesHint(s);
			changed = true;
		}
		if (maxSize != theFM.getMaxSize())
		{
			theFM.setMaxSize(maxSize);
			changed = true;
		}
		s = maxSizeHintField.getText().trim();
		if (!TextUtil.strEqual(s, theFM.getMaxSizeHint()))
		{
			theFM.setMaxSizeHint(s);
			changed = true;
		}
		if (!TextUtil.strEqual(maxAge, theFM.getMaxLMT()))
		{
			theFM.setMaxLMT(maxAge);
			changed = true;
		}
		s = maxAgeHintField.getText().trim();
		if (!TextUtil.strEqual(s, theFM.getMaxLMTHint()))
		{
			theFM.setMaxLMTHint(s);
			changed = true;
		}
		
		if (alarmOnDeleteCheck.isSelected() != theFM.isAlarmOnDelete())
		{
			theFM.setAlarmOnDelete(alarmOnDeleteCheck.isSelected());
			changed = true;
		}
		s = alarmOnDeleteHintField.getText().trim();
		if (!TextUtil.strEqual(s, theFM.getAlarmOnDeleteHint()))
		{
			theFM.setAlarmOnDeleteHint(s);
			changed = true;
		}
		if (enabledCheck.isSelected() != theFM.isEnabled())
		{
			theFM.setEnabled(enabledCheck.isSelected());
			changed = true;
		}
		
		closeDlg();
	}

	/** Called when cancel pressed. */
	private void doCancel()
	{
		changed = false;
		closeDlg();
	}

	/** Closes the dialog */
	private void closeDlg()
	{
		theFM.makeDescription();
		setVisible(false);
		dispose();
	}

	public String getPriority()
	{
		return priorities[priorityCombo.getSelectedIndex()];
	}

	public String getPattern()
	{
		String s = pathField.getText().trim();
		if (s.length() == 0)
			return null;
		return s;
	}
	
	private String null2blank(String s)
	{
		return s == null ? "" : s;
	}
	
	public void setData(FileMonitor fm)
	{
		theFM = fm;
		pathField.setText(null2blank(fm.getPath()));
		priorityCombo.setSelectedIndex(fm.getPriority() - Logger.E_INFORMATION);
		maxFilesField.setText(fm.getMaxFiles() == 0 ? "" : ("" + fm.getMaxFiles()));
		maxFilesHintField.setText(null2blank(fm.getMaxFilesHint()));
		maxSizeField.setText(fm.getMaxSize() == 0L ? "" : ("" + fm.getMaxSize()));
		maxSizeHintField.setText(null2blank(fm.getMaxSizeHint()));

		String ma = fm.getMaxLMT();
		if (ma == null)
		{
			maxAgeField.setText("");
			maxAgeUnitsCombo.setSelectedIndex(0);
		}
		else
		{
			IntervalIncrement ii = IntervalIncrement.parse(ma);
			maxAgeField.setText("" + ii.getCount());
			//	Combo is: "Seconds", "Minutes", "Hours", "Days"
			switch(ii.getCalConstant())
			{
			case Calendar.SECOND: maxAgeUnitsCombo.setSelectedIndex(0); break;
			case Calendar.MINUTE: maxAgeUnitsCombo.setSelectedIndex(1); break;
			case Calendar.HOUR: 
			case Calendar.HOUR_OF_DAY: maxAgeUnitsCombo.setSelectedIndex(2); break;
			case Calendar.DAY_OF_MONTH:
			case Calendar.DAY_OF_YEAR: maxAgeUnitsCombo.setSelectedIndex(3); break;
			}
		}
		maxAgeHintField.setText(null2blank(fm.getMaxLMTHint()));

		alarmOnDeleteCheck.setSelected(fm.isAlarmOnDelete());
		alarmOnDeleteHintField.setText(null2blank(fm.getAlarmOnDeleteHint()));
		enabledCheck.setSelected(fm.isEnabled());
		
		changed = false;
	}

	public boolean isChanged()
	{
		return changed;
	}
}
