/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.cwms.validation.DurCheckPeriod;
import decodes.gui.GuiDialog;

public class DurMagDialog extends GuiDialog
{
	private JComboBox durCombo = null;
	private DurCheckPeriod check = null;
	private JComboBox severityCombo = new JComboBox(new String[]{"Question", "Reject"});
	private JTextField high = new JTextField(7);
	private JTextField low = new JTextField(7);
	private boolean cancelled = false;
	

	public DurMagDialog(Frame parent, String[] durationArray, DurCheckPeriod check)
	{
		super(parent, "Dur/Mag Edit", true);
		this.check = check;
		severityCombo.setSelectedIndex(check.getFlag() == 'Q' ? 0 : 1);
		durCombo = new JComboBox(durationArray);
		durCombo.setSelectedItem(check.getDuration());
		this.setLayout(new BorderLayout());
		
		JPanel paramPanel = new JPanel(new GridBagLayout());
		this.add(paramPanel, BorderLayout.CENTER);
		paramPanel.add(new JLabel("Severity:"),
			new GridBagConstraints(0,0,1,1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(4,10, 2, 2), 0, 0));
		paramPanel.add(severityCombo,
			new GridBagConstraints(1,0,1,1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(4, 0, 2, 10), 0, 0));
	
		paramPanel.add(new JLabel("Duration:"),
			new GridBagConstraints(0, 1, 1,1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		paramPanel.add(durCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 10), 0, 0));

		paramPanel.add(new JLabel("Low:"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		paramPanel.add(low,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 2, 10), 0, 0));
		
		paramPanel.add(new JLabel("High:"),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 10, 2, 2), 0, 0));
		paramPanel.add(high,
			new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(2, 0, 4, 10), 0, 0));

		JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
		this.add(south, BorderLayout.SOUTH);
		JButton ok = new JButton("OK");
		south.add(ok);
		ok.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					okPressed();
				}
			});
		JButton cancel = new JButton("Cancel");
		south.add(cancel);
		cancel.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					cancelPressed();
				}
			});
		
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setGroupingUsed(false);
		high.setText(check.getHigh() == Double.POSITIVE_INFINITY ? "" : nf.format(check.getHigh()));
		low.setText(check.getLow() == Double.NEGATIVE_INFINITY ? "" : nf.format(check.getLow()));
		
		pack();
	}

	protected void cancelPressed()
	{
		cancelled = true;
		closeDlg();
	}

	protected void okPressed()
	{
		double hv = Double.POSITIVE_INFINITY, lv = Double.NEGATIVE_INFINITY;
		String s = high.getText().trim();
		if (s.length() > 0)
		{
			try { hv = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				showError("High value must be a number.");
				return;
			}
		}
		s = low.getText().trim();
		if (s.length() > 0)
		{
			try { lv = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				showError("Low value must be a number.");
				return;
			}
		}  
		check.setFlag(((String)severityCombo.getSelectedItem()).charAt(0));
		check.setDuration((String)durCombo.getSelectedItem());
		check.setHigh(hv);
		check.setLow(lv);
		cancelled = false;
		closeDlg();
	}
	
	/** Closes the dialog. */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	public boolean isCancelled()
	{
		return cancelled;
	}

}
