/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation.gui;

import ilex.gui.DateCalendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.IntervalDAI;
import decodes.cwms.validation.AbsCheck;
import decodes.cwms.validation.ConstCheck;
import decodes.cwms.validation.DurCheckPeriod;
import decodes.cwms.validation.RocPerHourCheck;
import decodes.cwms.validation.ScreeningCriteria;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;

public class SeasonCheckPanel extends JPanel
{
	private JTextField rejHiValue = new JTextField(7);
	private JTextField quesHiValue = new JTextField(7);
	private JTextField quesLoValue = new JTextField(7);
	private JTextField rejLoValue = new JTextField(7);
	private JTextField rejHiROC = new JTextField(7);
	private JTextField quesHiROC = new JTextField(7);
	private JTextField quesLoROC = new JTextField(7);
	private JTextField rejLoROC = new JTextField(7);
	private JComboBox rejDurCombo = null;
	private JTextField rejConstTolerance = new JTextField(7);
	private JTextField rejMinConst = new JTextField(7);
	private JTextField rejNmissConst = new JTextField(7);
	private JComboBox quesDurCombo = null;
	private JTextField quesConstTolerance = new JTextField(7);
	private JTextField quesMinConst = new JTextField(7);
	private JTextField quesNmissConst = new JTextField(7);
	private SortingListTable durMagTable = null;
	private DurMagTableModel durMagModel = null;
	private ScreeningEditFrame frame = null;
	private NumberFormat numFmt = NumberFormat.getNumberInstance();
	private ScreeningCriteria season = null;
	private ScreeningEditTab parent = null;
	private String[] durationArray = null;
	
	double vrh, vqh, vql, vrl; // value reject/question high/low 
	double rrh, rqh, rql, rrl; // rate of change reject/question high/low
	double crt, crm, cqt, cqm;
	int crnummiss, cqnummiss;

	public SeasonCheckPanel(ScreeningEditFrame frame, ScreeningEditTab parent)
	{
		this.frame = frame;
		this.parent = parent;
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(4);
		guiInit();
	}
	
	private void guiInit()
	{
		this.setLayout(new GridBagLayout());

		IntervalDAI intervalDAO = frame.getTheDb().makeIntervalDAO();
		try
		{
			durationArray = intervalDAO.getValidDurationCodes();
			rejDurCombo = new JComboBox(durationArray);
			quesDurCombo = new JComboBox(durationArray);
		}
		catch(Exception ex)
		{
			String msg = "Error reading durations: " + ex;
			frame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			intervalDAO.close();
		}

		Color redBack = new Color(255, 205, 205);
		Color yellowBack = new Color(255, 255, 200);
		rejHiValue.setBackground(redBack);
		quesHiValue.setBackground(yellowBack);
		quesLoValue.setBackground(yellowBack);
		rejLoValue.setBackground(redBack);
		rejHiROC.setBackground(redBack);
		quesHiROC.setBackground(yellowBack);
		quesLoROC.setBackground(yellowBack);
		rejLoROC.setBackground(redBack);

		rejDurCombo.setBackground(redBack);
		rejConstTolerance.setBackground(redBack);
		rejMinConst.setBackground(redBack);
		rejNmissConst.setBackground(redBack);
		quesDurCombo.setBackground(yellowBack);
		quesConstTolerance.setBackground(yellowBack);
		quesMinConst.setBackground(yellowBack);
		quesNmissConst.setBackground(yellowBack);


		
		
		JPanel valueRocPanel = new JPanel(new GridBagLayout());
		this.add(valueRocPanel,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 2, 4, 2), 0, 0));

		// 1st row: spacers and value/roc checkboxes
		JButton changeStartButton = new JButton("Change Start");
		changeStartButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					changeStartPressed();
				}
			});
		valueRocPanel.add(changeStartButton,
			new GridBagConstraints(0, 0, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(new JLabel("Value"),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(new JLabel("Hourly Change"),
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(new JLabel(""),
			new GridBagConstraints(4, 0, 1, 1, 0.4, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		
		valueRocPanel.add(new JLabel("Reject High:"),
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(rejHiValue,
			new GridBagConstraints(2, 1, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		valueRocPanel.add(rejHiROC,
			new GridBagConstraints(3, 1, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		
		valueRocPanel.add(new JLabel("Question High:"),
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(quesHiValue,
			new GridBagConstraints(2, 2, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		valueRocPanel.add(quesHiROC,
			new GridBagConstraints(3, 2, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));

		valueRocPanel.add(new JLabel("Question Low:"),
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(quesLoValue,
			new GridBagConstraints(2, 3, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		valueRocPanel.add(quesLoROC,
			new GridBagConstraints(3, 3, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));

		valueRocPanel.add(new JLabel("Reject Low:"),
			new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		valueRocPanel.add(rejLoValue,
			new GridBagConstraints(2, 4, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
		valueRocPanel.add(rejLoROC,
			new GridBagConstraints(3, 4, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 4, 2, 4), 0, 0));
	
		JPanel constValPanel = new JPanel(new GridBagLayout());
		this.add(constValPanel,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 2, 4, 2), 0, 0));
			
		constValPanel.setBorder(new TitledBorder("Constant Value over Duration"));
		
		constValPanel.add(new JLabel("Duration"),
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(new JLabel("Tolerance"),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(new JLabel("Min to Check"),
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(new JLabel("# Missing"),
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));

		constValPanel.add(new JLabel("Reject:"),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(rejDurCombo,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(rejConstTolerance,
			new GridBagConstraints(2, 1, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(rejMinConst,
			new GridBagConstraints(3, 1, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(rejNmissConst,
			new GridBagConstraints(4, 1, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));

		constValPanel.add(new JLabel("Question:"),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(quesDurCombo,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(quesConstTolerance,
			new GridBagConstraints(2, 2, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(quesMinConst,
			new GridBagConstraints(3, 2, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));
		constValPanel.add(quesNmissConst,
			new GridBagConstraints(4, 2, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 2, 2, 2), 0, 0));

		JPanel durMagPanel = new JPanel(new GridBagLayout());
		this.add(durMagPanel,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 2, 4, 2), 0, 0));

		durMagPanel.setBorder(new TitledBorder("Duration Magnitude"));
		
		durMagModel = new DurMagTableModel();
		durMagTable = new SortingListTable(durMagModel, DurMagTableModel.columnWidths);
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport().add(durMagTable);
		durMagPanel.add(scrollPane,
			new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		
		JButton addDurMagButton = new JButton("Add Dur/Mag");
		addDurMagButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addDurMagPressed();
				}
			});
		durMagPanel.add(addDurMagButton,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));
		JButton editDurMagButton = new JButton("Edit Dur/Mag");
		editDurMagButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					editDurMagPressed();
				}
			});
		durMagPanel.add(editDurMagButton,
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));

		JButton deleteDurMagButton = new JButton("Delete Dur/Mag");
		deleteDurMagButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					deleteDurMagPressed();
				}
			});
		durMagPanel.add(deleteDurMagButton,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 1.0,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(2, 5, 2, 5), 0, 0));

		rejHiValue.setToolTipText("Values >= to this limit are flagged as Rejected.");
		quesHiValue.setToolTipText("Values >= to this limit are flagged as Questionable.");
		quesLoValue.setToolTipText("Values <= to this limit are flagged as Questionable.");
		rejLoValue.setToolTipText("Values <= to this limit are flagged as Rejected.");
		
		rejHiROC.setToolTipText("Hourly Rates of Change >= to this limit are flagged as Rejected.");
		quesHiROC.setToolTipText("Hourly Rates of Change >= to this limit are flagged as Questionable.");
		quesLoROC.setToolTipText("Hourly Rates of Change <= to this limit are flagged as Questionable.");
		rejLoROC.setToolTipText("Hourly Rates of Change <= to this limit are flagged as Rejected.");
		
		rejDurCombo.setToolTipText("Duration over which to check for constant value within tolerance.");
		rejConstTolerance.setToolTipText("Values changing less than this amount over the specified duration"
			+ " are considered constant.");
		rejMinConst.setToolTipText("Values less than this amount are treated the same as missing values.");
		
		String nmissTT = 
			"<html>If more than this many missing values are detected over the specified<br/>"
			+ "duration, then don't perform the check. For Irregular interval data,<br/>"
			+ "this is a fractional portion of the duration. Example: If Duration is<br/>"
			+ "'4Days' and # Missing is .25, then if there is more than a 1-day gap<br/>"
			+ "anywhere in the data, the check is not performed.</html>";
			
		rejNmissConst.setToolTipText(nmissTT);
		quesDurCombo.setToolTipText("Duration over which to check for constant value within tolerance.");
		quesConstTolerance.setToolTipText("Values changing less than this amount over the specified duration"
			+ " are considered constant.");
		quesMinConst.setToolTipText("Values less than this amount are treated the same as missing values.");
		quesNmissConst.setToolTipText(nmissTT);
	}

	protected void changeStartPressed()
	{
		Calendar cal = season.getSeasonStart();
		DateCalendar dc = new DateCalendar("Season Start", cal.getTime(), "MMM dd", 
			TimeZone.getDefault());
		int res = JOptionPane.showConfirmDialog(frame, dc, "Select Start Date", JOptionPane.OK_CANCEL_OPTION);
		if (res == JOptionPane.CANCEL_OPTION)
			return;
		cal.setTime(dc.getDate());
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd");
		
		parent.getSeasonsPane().setTitleAt(parent.getSeasonsPane().indexOfComponent(this), 
			sdf.format(dc.getDate()));
	}

	protected void deleteDurMagPressed()
	{
		int idx = durMagTable.getSelectedRow();
		if (idx == -1)
		{
			frame.showError("Select Dur/Mag in the table, then press Edit.");
			return;
		}
		int modelRow = durMagTable.convertRowIndexToModel(idx);
		DurCheckPeriod dcp = durMagModel.checks.get(modelRow);
		if (dcp.getHigh() != Double.POSITIVE_INFINITY || dcp.getLow() != Double.NEGATIVE_INFINITY)
		{
			int res = JOptionPane.showConfirmDialog(frame, "Confirm delete of Dur/Mag check "
				+ "with severity=" + dcp.getFlag() + " and duration=" + dcp.getDuration() + "?");
			if (res != JOptionPane.YES_OPTION)
				return;
		}
		durMagModel.remove(idx);
	}

	protected void editDurMagPressed()
	{
		int idx = durMagTable.getSelectedRow();
		if (idx == -1)
		{
			frame.showError("Select Dur/Mag in the table, then press Edit.");
			return;
		}
		int modelRow = durMagTable.convertRowIndexToModel(idx);
		DurCheckPeriod dcp = durMagModel.checks.get(modelRow);
		DurMagDialog dlg = new DurMagDialog(frame, durationArray, dcp);
		frame.launchDialog(dlg);
		if (dlg.isCancelled())
			return;
		durMagModel.update(idx);
	}

	protected void addDurMagPressed()
	{
		DurCheckPeriod dcp = new DurCheckPeriod('Q', "1Day", Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		DurMagDialog dlg = new DurMagDialog(frame, durationArray, dcp);
		frame.launchDialog(dlg);
		if (dlg.isCancelled())
			return;
		durMagModel.add(dcp);
	}
	
	public void clearFields()
	{
		rejHiValue.setText("");
		rejLoValue.setText("");
		quesHiValue.setText("");
		quesLoValue.setText("");
		rejHiROC.setText("");
		rejLoROC.setText("");
		quesHiROC.setText("");
		quesLoROC.setText("");
		rejDurCombo.setSelectedItem("1Day");
		rejConstTolerance.setText("");
		rejMinConst.setText("");
		rejNmissConst.setText("");
		quesDurCombo.setSelectedItem("1Day");
		quesConstTolerance.setText("");
		quesMinConst.setText("");
		quesNmissConst.setText("");
	}

	public void setScreeningCriteria(ScreeningCriteria season)
	{
		clearFields();
		AbsCheck ac = season.getAbsCheckFor('R');
		if (ac != null)
		{
			if (ac.getHigh() != Double.POSITIVE_INFINITY)
				rejHiValue.setText(numFmt.format(ac.getHigh()));
			if (ac.getLow() != Double.NEGATIVE_INFINITY)
				rejLoValue.setText(numFmt.format(ac.getLow()));
		}
		ac = season.getAbsCheckFor('Q');
		if (ac != null)
		{
			if (ac.getHigh() != Double.POSITIVE_INFINITY)
				quesHiValue.setText(numFmt.format(ac.getHigh()));
			if (ac.getLow() != Double.NEGATIVE_INFINITY)
				quesLoValue.setText(numFmt.format(ac.getLow()));
		}
		RocPerHourCheck rc = season.getRocCheckFor('R');
		if (rc != null)
		{
			if (rc.getRise() != Double.POSITIVE_INFINITY)
				rejHiROC.setText(numFmt.format(rc.getRise()));
			if (rc.getFall() != Double.NEGATIVE_INFINITY)
				rejLoROC.setText(numFmt.format(rc.getFall()));
		}
		rc = season.getRocCheckFor('Q');
		if (rc != null)
		{
			if (rc.getRise() != Double.POSITIVE_INFINITY)
				quesHiROC.setText(numFmt.format(rc.getRise()));
			if (rc.getFall() != Double.NEGATIVE_INFINITY)
				quesLoROC.setText(numFmt.format(rc.getFall()));
		}
		
		ConstCheck cc = season.getConstCheckFor('R');
		if (cc != null)
		{
			rejDurCombo.setSelectedItem(cc.getDuration());
			rejConstTolerance.setText(numFmt.format(cc.getTolerance()));
			rejMinConst.setText(numFmt.format(cc.getMinToCheck()));
			rejNmissConst.setText(numFmt.format(cc.getAllowedMissing()));
		}
		cc = season.getConstCheckFor('Q');
		if (cc != null)
		{
			quesDurCombo.setSelectedItem(cc.getDuration());
			quesConstTolerance.setText(numFmt.format(cc.getTolerance()));
			quesMinConst.setText(numFmt.format(cc.getMinToCheck()));
			quesNmissConst.setText(numFmt.format(cc.getAllowedMissing()));
		}
	
		durMagModel.setContents(season.getDurCheckPeriods());
		this.season = season;
	}

	public ScreeningCriteria getSeason()
	{
		return season;
	}

	/**
	 * Extract numbers, etc., from the fields and validate
	 * @return true if fields are ok. If error, show message and return false.
	 */
	public boolean validateFields()
	{
		vrh = vqh = rrh = rqh = Double.POSITIVE_INFINITY;
		vql = vrl = rql = rrl = Double.NEGATIVE_INFINITY;
		crt = crm = cqt = cqm = 0.0;
		crnummiss = cqnummiss = 0;
		
		String s = rejHiValue.getText().trim();
		if (s.length() > 0)
		{
			try { vrh = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject High Value '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesHiValue.getText().trim();
		if (s.length() > 0)
		{
			try { vqh = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question High Value '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesLoValue.getText().trim();
		if (s.length() > 0)
		{
			try { vql = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question Low Value '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = rejLoValue.getText().trim();
		if (s.length() > 0)
		{
			try { vrl = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject Low Value '" + s + "' -- must be a number.");
				return false;
			}
		}

		s = rejHiROC.getText().trim();
		if (s.length() > 0)
		{
			try { rrh = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject High Rate of Change '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesHiROC.getText().trim();
		if (s.length() > 0)
		{
			try { rqh = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question High Rate of Change '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesLoROC.getText().trim();
		if (s.length() > 0)
		{
			try { rql = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question Low Rate of Change '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = rejLoROC.getText().trim();
		if (s.length() > 0)
		{
			try { rrl = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject Low Rate of Change '" + s + "' -- must be a number.");
				return false;
			}
		}
	
		s = rejConstTolerance.getText().trim();
		if (s.length() > 0)
		{
			try { crt = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject constant-value tolerance '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = rejMinConst.getText().trim();
		if (s.length() > 0)
		{
			try { crm = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject contant-value min '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = rejNmissConst.getText().trim();
		if (s.length() > 0)
		{
			try { crnummiss = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Reject contant-value num-missing '" + s + "' -- must be an integer.");
				return false;
			}
		}

		s = quesConstTolerance.getText().trim();
		if (s.length() > 0)
		{
			try { cqt = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question constant-value tolerance '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesMinConst.getText().trim();
		if (s.length() > 0)
		{
			try { cqm = Double.parseDouble(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question contant-value min '" + s + "' -- must be a number.");
				return false;
			}
		}
		s = quesNmissConst.getText().trim();
		if (s.length() > 0)
		{
			try { cqnummiss = Integer.parseInt(s); }
			catch(NumberFormatException ex)
			{
				frame.showError("Invalid Question contant-value num-missing '" + s + "' -- must be an integer.");
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Called after all SeasonCheckPanes have successfully returned true for
	 * validate Fields. Copy the info back into the season object.
	 */
	public void saveFields()
	{
		season.clearChecks();
		if (vrh != Double.POSITIVE_INFINITY || vrl != Double.NEGATIVE_INFINITY)
			season.addAbsCheck(new AbsCheck('R', vrl, vrh));
		if (vqh != Double.POSITIVE_INFINITY || vql != Double.NEGATIVE_INFINITY)
			season.addAbsCheck(new AbsCheck('Q', vql, vqh));
		if (rrh != Double.POSITIVE_INFINITY || rrl != Double.NEGATIVE_INFINITY)
			season.addRocPerHourCheck(new RocPerHourCheck('R', rrl, rrh));
		if (rqh != Double.POSITIVE_INFINITY || rql != Double.NEGATIVE_INFINITY)
			season.addRocPerHourCheck(new RocPerHourCheck('Q', rql, rqh));

		if (crt != 0.0 || crm != 0.0 || crnummiss != 0)
			season.addConstCheck(new ConstCheck('R', (String)rejDurCombo.getSelectedItem(),
				crm, crt, crnummiss));
		if (cqt != 0.0 || cqm != 0.0 || cqnummiss != 0)
			season.addConstCheck(new ConstCheck('Q', (String)quesDurCombo.getSelectedItem(),
				cqm, cqt, cqnummiss));

		for(DurCheckPeriod p : durMagModel.checks)
			season.addDurCheckPeriod(p);
	}

}

class DurMagTableModel
	extends AbstractTableModel
	implements SortingListTableModel
{
	static String columnNames[] = { "Severity", "Duration", "Low", "High" };
	static int columnWidths[] = { 25, 35, 20, 20 };

	ArrayList<DurCheckPeriod> checks = new ArrayList<DurCheckPeriod>();
	private int sortColumn = 0;
	private NumberFormat nf = NumberFormat.getNumberInstance();
	
	DurMagTableModel()
	{
		nf.setGroupingUsed(false);
		nf.setMaximumFractionDigits(4);
	}
	
	public void remove(int idx)
	{
		checks.remove(idx);
		this.fireTableDataChanged();
	}

	public void update(int idx)
	{
		this.fireTableRowsUpdated(idx, idx);
	}

	public void add(DurCheckPeriod dcp)
	{
		checks.add(dcp);
		fireTableDataChanged();
	}

	@Override
	public int getRowCount()
	{
		return checks.size();
	}
	
	/**
	 * Fills the model with copies of the periods passed in the argument.
	 * That way, as changes are made, the original array remains unchanged.
	 * @param periods
	 */
	public void setContents(ArrayList<DurCheckPeriod> periods)
	{
		checks.clear();
		for(DurCheckPeriod p : periods)
			checks.add(new DurCheckPeriod(p.getFlag(), p.getDuration(), p.getLow(), p.getHigh()));
		sortByColumn(sortColumn);
	}
	
	@Override
	public int getColumnCount()
	{
		return columnNames.length;
	}
	
	@Override
	public String getColumnName(int c)
	{
		return columnNames[c];
	}
	
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		return getColumnValue(checks.get(rowIndex), columnIndex);
	}
	
	private String getColumnValue(DurCheckPeriod check, int columnIndex)
	{
		switch(columnIndex)
		{
		case 0: return check.getFlag() == 'Q' ? "Question" : "Reject";
		case 1: return check.getDuration();
		case 2: return check.getLow() == Double.NEGATIVE_INFINITY ? "" : nf.format(check.getLow());
		case 3: return check.getHigh() == Double.POSITIVE_INFINITY ? "" : nf.format(check.getHigh());
		}
		return ""; // won't happen
	}
	
	@Override
	public void sortByColumn(final int column)
	{
		Collections.sort(checks, 
			new Comparator<DurCheckPeriod>()
			{
				@Override
				public int compare(DurCheckPeriod o1, DurCheckPeriod o2)
				{
					return getColumnValue(o1, column).compareTo(getColumnValue(o2, column));
				}
			});
		sortColumn = column;
		this.fireTableDataChanged();
	}
	
	@Override
	public Object getRowObject(int row)
	{
		return checks.get(row);
	}

}
