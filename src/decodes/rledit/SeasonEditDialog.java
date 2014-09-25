package decodes.rledit;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.dbeditor.TimeZoneSelector;
import decodes.decoder.FieldParseException;
import decodes.decoder.Season;
import decodes.gui.GuiDialog;

@SuppressWarnings("serial")
public class SeasonEditDialog extends GuiDialog
{
	private static ResourceBundle genericLabels = 
		RefListEditor.getGenericLabels();
	private static ResourceBundle labels = RefListEditor.getLabels();
	
	private JTextField abbrField = new JTextField();
	private JTextField nameField = new JTextField();
	private JTextField startField = new JTextField();
	private JTextField endField = new JTextField();
	private TimeZoneSelector timeZoneCombo = new TimeZoneSelector();
	private Season editSeason = null;
	private boolean okPressed = false;
 
	public SeasonEditDialog(JFrame parent)
	{
		super(parent, labels.getString("SeasonDialog.title"), true);
		guiInit();
		pack();
	}
	
	private void guiInit()
	{
		JPanel fieldPanel = new JPanel(new GridBagLayout());
		
		fieldPanel.add(new JLabel(labels.getString("SeasonsTab.abbr") + ":"),
			new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(10, 10, 2, 1), 0, 0));
		fieldPanel.add(abbrField,
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(10, 0, 2, 5), 80, 0));
		fieldPanel.add(new JLabel(labels.getString("SeasonDialog.abbrHelp")),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(10, 0, 2, 10), 0, 0));

		fieldPanel.add(new JLabel(labels.getString("SeasonsTab.name") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		fieldPanel.add(nameField,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 80, 0));
		fieldPanel.add(new JLabel(labels.getString("SeasonDialog.nameHelp")),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		
		fieldPanel.add(new JLabel(labels.getString("SeasonsTab.start") + ":"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		fieldPanel.add(startField,
			new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 80, 0));
		fieldPanel.add(new JLabel(labels.getString("SeasonDialog.startHelp")),
			new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
		
		fieldPanel.add(new JLabel(labels.getString("SeasonsTab.end") + ":"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 1), 0, 0));
		fieldPanel.add(endField,
			new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 
				new Insets(2, 0, 2, 5), 80, 0));
		fieldPanel.add(new JLabel(labels.getString("SeasonDialog.endHelp")),
			new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 2, 10), 0, 0));
	
		fieldPanel.add(new JLabel(labels.getString("SeasonsTab.tz") + ":"),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 5, 1), 0, 0));
		fieldPanel.add(timeZoneCombo,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 5, 5), 0, 0));
		fieldPanel.add(new JLabel(labels.getString("SeasonDialog.tzHelp")),
			new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, 
				new Insets(2, 0, 5, 10), 0, 0));

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 35, 10));
		JButton okButton = new JButton("  " + genericLabels.getString("OK") + "  ");
		okButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton(genericLabels.getString("cancel"));
		cancelButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					cancelPressed();
				}
			});
		okButton.setPreferredSize(cancelButton.getPreferredSize());
		buttonPanel.add(cancelButton);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(fieldPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(mainPanel);

	}
	
	protected void cancelPressed()
	{
		closeDlg();
	}

	protected void okPressed()
	{
		String abbr = abbrField.getText().trim();
		if (abbr.length() == 0)
		{
			showError(labels.getString("SeasonDialog.abbrRequired"));
			return;
		}
		if (abbr.indexOf(" ") != -1)
		{
			showError(labels.getString("SeasonDialog.abbrNoSpace"));
			return;
		}
		String name = nameField.getText().trim();
		String start = null;
		try { start = Season.formatDateTime(startField.getText().trim(), "start"); }
		catch (FieldParseException ex)
		{
			showError(ex.getMessage());
			return;
		}
		String end = null;
		try { end = Season.formatDateTime(endField.getText().trim(), "end"); }
		catch (FieldParseException ex)
		{
			showError(ex.getMessage());
			return;
		}
		
		editSeason.setAbbr(abbr);
		editSeason.setName(name);
		editSeason.setStart(start);
		editSeason.setEnd(end);
		editSeason.setTz((String)timeZoneCombo.getSelectedItem());
		okPressed = true;
		closeDlg();
	}

	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	public void fillValues(Season season)
	{
		editSeason = season;
		abbrField.setText(season.getAbbr());
		nameField.setText(season.getName());
		startField.setText(season.getStart());
		endField.setText(season.getEnd());
		String tz = season.getTz();
		if (tz == null || tz.length() == 0)
			timeZoneCombo.setSelectedIndex(0);
		else
			timeZoneCombo.setSelectedItem(tz);
	}

	public boolean isOkPressed()
	{
		return okPressed;
	}
	
	

}
