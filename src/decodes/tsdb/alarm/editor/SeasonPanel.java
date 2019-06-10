package decodes.tsdb.alarm.editor;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Calendar;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import decodes.decoder.Season;
import decodes.tsdb.BadScreeningException;
import decodes.tsdb.IntervalIncrement;
import decodes.tsdb.alarm.AlarmLimitSet;
import opendcs.dai.IntervalDAI;

@SuppressWarnings({ "serial", "rawtypes", "unchecked" })
public class SeasonPanel extends JPanel
{
	private JTextField rejHiValue = new JTextField(7);
	private JTextField critHiValue = new JTextField(7);
	private JTextField warnHiValue = new JTextField(7);
	private JTextField warnLoValue = new JTextField(7);
	private JTextField critLoValue = new JTextField(7);
	private JTextField rejLoValue = new JTextField(7);
	
	private String[] timeIncArray = { "Second(s)", "Minute(s)", "Hour(s)", "Day(s)", "Week(s)", "Month(s)", "Year(s)" };

	private JTextField rocMultField = new JTextField(4);
	private JComboBox rocUnitCombo = new JComboBox(timeIncArray);
	private JTextField rejHiROC = new JTextField(7);
	private JTextField critHiROC = new JTextField(7);
	private JTextField warnHiROC = new JTextField(7);
	private JTextField warnLoROC = new JTextField(7);
	private JTextField critLoROC = new JTextField(7);
	private JTextField rejLoROC = new JTextField(7);
	
	private JTextField stuckDurField = new JTextField(4);
	private JComboBox stuckDurCombo = new JComboBox(timeIncArray);
	private JTextField stuckToleranceField = new JTextField(7);
	private JTextField stuckMinField = new JTextField(7);
	private JTextField stuckMaxMultField = new JTextField(7);
	private JComboBox stuckMaxDurUnitCombo = new JComboBox(timeIncArray);
	
	private JTextField missingCheckDurField = new JTextField(4);
	private JComboBox missingCheckDurCombo = new JComboBox(timeIncArray);
	private JComboBox missingTSIntervalCombo = null;
	private JTextField missingMaxField = new JTextField(4);
	
	private AlarmEditFrame parentFrame = null;
	private NumberFormat numFmt = NumberFormat.getNumberInstance();
	private AlarmLimitSet limitSet = null;
	private ScreeningEditPanel parentPanel = null;
	private String[] dbIntervalArray = null;
	
	public SeasonPanel(AlarmEditFrame frame, ScreeningEditPanel parent)
	{
		this.parentFrame = frame;
		this.parentPanel = parent;
		numFmt.setGroupingUsed(false);
		numFmt.setMaximumFractionDigits(4);
		guiInit();
	}
	
	private void guiInit()
	{
		this.setLayout(new GridBagLayout());

		IntervalDAI intervalDAO = parentFrame.getTsDb().makeIntervalDAO();
		try
		{
			dbIntervalArray = intervalDAO.getValidDurationCodes();
			missingTSIntervalCombo = new JComboBox(dbIntervalArray);
		}
		catch(Exception ex)
		{
			String msg = "Error reading durations: " + ex;
			parentFrame.showError(msg);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}
		finally
		{
			intervalDAO.close();
		}

		Color redBack = new Color(255, 205, 205);
		Color yellowBack = new Color(255, 255, 200);
		Color blueBack = new Color(0xe6, 255, 255);
		
		rejHiValue.setBackground(blueBack);
		critHiValue.setBackground(redBack);
		warnHiValue.setBackground(yellowBack);
		warnLoValue.setBackground(yellowBack);
		critLoValue.setBackground(redBack);
		rejLoValue.setBackground(blueBack);

		rejHiROC.setBackground(blueBack);
		critHiROC.setBackground(redBack);
		warnHiROC.setBackground(yellowBack);
		warnLoROC.setBackground(yellowBack);
		critLoROC.setBackground(redBack);
		rejLoROC.setBackground(blueBack);

		JPanel valueRocPanel = new JPanel(new GridBagLayout());
		this.add(valueRocPanel,
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(4, 2, 4, 2), 0, 0));

		// 1st row: spacers and value/roc checkboxes
		JButton changeSeasonButton = new JButton(parentFrame.eventmonLabels.getString("changeSeason"));
		changeSeasonButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					changeSeasonPressed();
				}
			});
		valueRocPanel.add(changeSeasonButton,
			new GridBagConstraints(0, 0, 1, 1, 0.4, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		
		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("changePer")),
				new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(new JLabel(""),
				new GridBagConstraints(4, 0, 1, 1, 0.4, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(1, 2, 1, 2), 0, 0));

		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("value")),
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(rocMultField);
		p.add(rocUnitCombo);
		valueRocPanel.add(p,
				new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
					GridBagConstraints.CENTER, GridBagConstraints.NONE,
					new Insets(1, 2, 1, 2), 0, 0));
		
		
		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("reject") + " " 
			+ parentFrame.eventmonLabels.getString("high") + ":"),
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(rejHiValue,
			new GridBagConstraints(2, 2, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(rejHiROC,
			new GridBagConstraints(3, 2, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		
		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("critical") + " " 
			+ parentFrame.eventmonLabels.getString("high") + ":"),
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(critHiValue,
			new GridBagConstraints(2, 3, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(critHiROC,
			new GridBagConstraints(3, 3, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));

		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("warning") + " " 
			+ parentFrame.eventmonLabels.getString("high") + ":"),
			new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(warnHiValue,
			new GridBagConstraints(2, 4, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(warnHiROC,
			new GridBagConstraints(3, 4, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));

		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("warning") + " " 
			+ parentFrame.eventmonLabels.getString("low") + ":"),
			new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(warnLoValue,
			new GridBagConstraints(2, 5, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(warnLoROC,
			new GridBagConstraints(3, 5, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));

		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("critical") + " " 
			+ parentFrame.eventmonLabels.getString("low") + ":"),
			new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(critLoValue,
			new GridBagConstraints(2, 6, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(critLoROC,
			new GridBagConstraints(3, 6, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));

		valueRocPanel.add(new JLabel(parentFrame.eventmonLabels.getString("reject") + " " 
			+ parentFrame.eventmonLabels.getString("low") + ":"),
			new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		valueRocPanel.add(rejLoValue,
			new GridBagConstraints(2, 7, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
		valueRocPanel.add(rejLoROC,
			new GridBagConstraints(3, 7, 1, 1, 0.1, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 4, 1, 4), 0, 0));
	
		//============= Stuck Sensor Panel ============
		JPanel stuckSensorPanel = new JPanel(new GridBagLayout());
		this.add(stuckSensorPanel,
			new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH,
				new Insets(1, 2, 1, 2), 0, 0));
			
		stuckSensorPanel.setBorder(new TitledBorder(parentFrame.eventmonLabels.getString("stuckSensor")));
		stuckSensorPanel.add(new JLabel(parentFrame.eventmonLabels.getString("stuckDuration")),
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		stuckSensorPanel.add(new JLabel(parentFrame.eventmonLabels.getString("tolerance")),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		stuckSensorPanel.add(new JLabel(parentFrame.eventmonLabels.getString("minToCheck")),
			new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		stuckSensorPanel.add(new JLabel(parentFrame.eventmonLabels.getString("maxGap")),
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));

		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(stuckDurField);
		p.add(stuckDurCombo);
		stuckSensorPanel.add(p,
			new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));
		stuckSensorPanel.add(stuckToleranceField,
			new GridBagConstraints(2, 1, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 2), 0, 0));
		stuckSensorPanel.add(stuckMinField,
			new GridBagConstraints(3, 1, 1, 1, 0.33, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 2), 0, 0));
		
		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(stuckMaxMultField);
		p.add(stuckMaxDurUnitCombo);
		
		stuckSensorPanel.add(p,
			new GridBagConstraints(4, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(1, 2, 1, 2), 0, 0));

		//=============== Missing Data Panel =================
		JPanel missingDataPanel = new JPanel(new GridBagLayout());
		this.add(missingDataPanel,
			new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(1, 2, 1, 2), 0, 0));

		missingDataPanel.setBorder(new TitledBorder(parentFrame.eventmonLabels.getString("missingData")));
		
		missingDataPanel.add(new JLabel(parentFrame.eventmonLabels.getString("checkEvery")+":"),
			new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 5, 1, 1), 0, 0));

		p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.add(missingCheckDurField);
		p.add(missingCheckDurCombo);
		missingDataPanel.add(p,
			new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(1, 0, 1, 2), 0, 0));
		missingDataPanel.add(new JLabel(parentFrame.eventmonLabels.getString("tsInterval")+":"),
			new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 20, 1, 1), 0, 0));
		missingDataPanel.add(missingTSIntervalCombo,
			new GridBagConstraints(3, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 0, 1, 2), 0, 0));
	
		missingDataPanel.add(new JLabel(parentFrame.eventmonLabels.getString("maxMissing")+":"),
			new GridBagConstraints(4, 0, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(1, 20, 1, 1), 0, 0));
		missingDataPanel.add(missingMaxField,
			new GridBagConstraints(5, 0, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(1, 0, 1, 2), 0, 0));
	
		rejHiValue.setToolTipText("Values >= to this limit are flagged as Rejected.");
		critHiValue.setToolTipText("Values >= to this limit are flagged as Questionable.");
		critLoValue.setToolTipText("Values <= to this limit are flagged as Questionable.");
		rejLoValue.setToolTipText("Values <= to this limit are flagged as Rejected.");
		
		rejHiROC.setToolTipText("Hourly Rates of Change >= to this limit are flagged as Rejected.");
		critHiROC.setToolTipText("Hourly Rates of Change >= to this limit are flagged as Questionable.");
		critLoROC.setToolTipText("Hourly Rates of Change <= to this limit are flagged as Questionable.");
		rejLoROC.setToolTipText("Hourly Rates of Change <= to this limit are flagged as Rejected.");
		
		stuckDurCombo.setToolTipText("Duration over which to check for constant value within tolerance.");
		stuckToleranceField.setToolTipText("Values changing less than this amount over the specified duration"
			+ " are considered constant.");
		stuckMinField.setToolTipText("Values less than this amount are treated the same as missing values.");
		
//		String nmissTT = 
//			"<html>If more than this many missing values are detected over the specified<br/>"
//			+ "duration, then don't perform the check. For Irregular interval data,<br/>"
//			+ "this is a fractional portion of the duration. Example: If Duration is<br/>"
//			+ "'4Days' and # Missing is .25, then if there is more than a 1-day gap<br/>"
//			+ "anywhere in the data, the check is not performed.</html>";
//			
//		stuckMaxNumField.setToolTipText(nmissTT);
//		quesDurCombo.setToolTipText("Duration over which to check for constant value within tolerance.");
//		quesConstTolerance.setToolTipText("Values changing less than this amount over the specified duration"
//			+ " are considered constant.");
//		quesMinConst.setToolTipText("Values less than this amount are treated the same as missing values.");
//		quesNmissConst.setToolTipText(nmissTT);
	}

	protected void changeSeasonPressed()
	{
		Season newSeason = parentPanel.selectSeason(limitSet.getSeasonName());
		if (newSeason == null || newSeason == limitSet.getSeason())
			return;
		
		if (parentPanel.getPanelFor(newSeason) != null)
		{
			parentFrame.showError("There is already a panel for '");
			return;
		}
		limitSet.setSeason(newSeason);
		parentPanel.setSeasonTabLabel(this, newSeason.getAbbr());
	}

	public void clearFields()
	{
		rejHiValue.setText("");
		critHiValue.setText("");
		warnHiValue.setText("");
		warnLoValue.setText("");
		critLoValue.setText("");
		rejLoValue.setText("");
		
		rocMultField.setText("");
		rocUnitCombo.setSelectedIndex(2);
		
		rejHiROC.setText("");
		critHiROC.setText("");
		warnHiROC.setText("");
		warnLoROC.setText("");
		critLoROC.setText("");
		rejLoROC.setText("");

		stuckDurField.setText("");
		stuckDurCombo.setSelectedIndex(2);
		stuckToleranceField.setText("0.0");
		stuckMinField.setText("");
		stuckMaxMultField.setText("");
		stuckMaxDurUnitCombo.setSelectedIndex(2);
	
		missingCheckDurField.setText("");
		missingCheckDurCombo.setSelectedIndex(2);
		missingMaxField.setText("");
	}

	public void setLimitSet(AlarmLimitSet limitSet)
	{
		clearFields();
		
		if (limitSet.getRejectHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			rejHiValue.setText(numFmt.format(limitSet.getRejectHigh()));
		if (limitSet.getCriticalHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			critHiValue.setText(numFmt.format(limitSet.getCriticalHigh()));
		if (limitSet.getWarningHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			warnHiValue.setText(numFmt.format(limitSet.getWarningHigh()));
		if (limitSet.getWarningLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			warnLoValue.setText(numFmt.format(limitSet.getWarningLow()));
		if (limitSet.getCriticalLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			critLoValue.setText(numFmt.format(limitSet.getCriticalLow()));
		if (limitSet.getRejectLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			rejLoValue.setText(numFmt.format(limitSet.getRejectLow()));
		
		if (limitSet.getRejectRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			rejHiROC.setText(numFmt.format(limitSet.getRejectRocHigh()));
		if (limitSet.getCriticalRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			critHiROC.setText(numFmt.format(limitSet.getCriticalRocHigh()));
		if (limitSet.getWarningRocHigh() != AlarmLimitSet.UNASSIGNED_LIMIT)
			warnHiROC.setText(numFmt.format(limitSet.getWarningRocHigh()));
		if (limitSet.getWarningRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			warnLoROC.setText(numFmt.format(limitSet.getWarningRocLow()));
		if (limitSet.getCriticalRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			critLoROC.setText(numFmt.format(limitSet.getCriticalRocLow()));
		if (limitSet.getRejectRocLow() != AlarmLimitSet.UNASSIGNED_LIMIT)
			rejLoROC.setText(numFmt.format(limitSet.getRejectRocLow()));

		if (limitSet.getRocInterval() != null && limitSet.getRocInterval().trim().length()>0)
			setPeriod(limitSet.getRocInterval(), rocMultField, rocUnitCombo);
		
		if (limitSet.getStuckDuration() != null && limitSet.getStuckDuration().trim().length()>0)
			setPeriod(limitSet.getStuckDuration(), stuckDurField, stuckDurCombo);
		stuckToleranceField.setText(numFmt.format(limitSet.getStuckTolerance()));
		if (limitSet.getMinToCheck() != AlarmLimitSet.UNASSIGNED_LIMIT)
			stuckMinField.setText(numFmt.format(limitSet.getMinToCheck()));
		if (limitSet.getMaxGap() != null && limitSet.getMaxGap().trim().length()>0)
			setPeriod(limitSet.getMaxGap(), stuckMaxMultField, stuckMaxDurUnitCombo);
		
		if (limitSet.getMissingPeriod() != null && limitSet.getMissingPeriod().trim().length()>0)
			setPeriod(limitSet.getMissingPeriod(), missingCheckDurField, missingCheckDurCombo);
		if (limitSet.getMissingInterval() != null && limitSet.getMissingInterval().trim().length()>0)
			missingTSIntervalCombo.setSelectedItem(limitSet.getMissingInterval());
		missingMaxField.setText(numFmt.format(limitSet.getMaxMissingValues()));
		
		this.limitSet = limitSet;
		this.limitSet.prepareForExec();
	}

	/**
	 * Expect a string of the form "N timeunits" where N is an integer and timeunits
	 * is one of values defined above in timeIncArray. Place the integer into multField
	 * and set the unitCombo.
	 * @param intv
	 * @param multField
	 * @param unitCombo
	 */
	private void setPeriod(String intv, JTextField multField, JComboBox unitCombo)
	{
		IntervalIncrement iinc = IntervalIncrement.parse(intv);
		multField.setText("" + iinc.getCount());
		int cc = iinc.getCalConstant();
		unitCombo.setSelectedIndex(
			cc == Calendar.SECOND ? 0 :
			cc == Calendar.MINUTE ? 1 :
			cc == Calendar.HOUR_OF_DAY || cc == Calendar.HOUR ? 2 :
			cc == Calendar.DAY_OF_MONTH || cc == Calendar.DAY_OF_WEEK || cc == Calendar.DAY_OF_YEAR ? 3 :
			cc == Calendar.WEEK_OF_MONTH || cc == Calendar.WEEK_OF_YEAR ? 4 :
			cc == Calendar.MONTH ? 5 :
			cc == Calendar.YEAR ? 6 :
			2); // Default is Hour
	}

	public AlarmLimitSet getLimitSet()
	{
		return limitSet;
	}

	/**
	 * Save the current field settings to the passed limit set, preserving the ID in 
	 * the original limit set.
	 * @param als the limit set to save to
	 * @throws BadScreeningException on any parse errors or inconsistencies in the fields.
	 */
	public void fieldsToLimitSet(AlarmLimitSet als)
		throws BadScreeningException
	{
		StringBuilder errors = new StringBuilder("");
		
		String s = "";
		try
		{
			s = rejHiValue.getText().trim();
			als.setRejectHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Reject High Value '" + s + "' -- must be a number\n");
		}
		
		try
		{
			s = critHiValue.getText().trim();
			als.setCriticalHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Critical High Value '" + s + "' -- must be a number\n");
		}

		try
		{
			s = warnHiValue.getText().trim();
			als.setWarningHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Warning High Value '" + s + "' -- must be a number\n");
		}

		try
		{
			s = warnLoValue.getText().trim();
			als.setWarningLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Warning Low Value '" + s + "' -- must be a number\n");
		}

		try
		{
			s = critLoValue.getText().trim();
			als.setCriticalLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Critical Low Value '" + s + "' -- must be a number\n");
		}

		try
		{
			s = rejLoValue.getText().trim();
			als.setRejectLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Reject Low Value '" + s + "' -- must be a number\n");
		}
		
		// Rate Of Change Limits ========================
		s = rocMultField.getText().trim();
		String tu = (String)rocUnitCombo.getSelectedItem();
		als.setRocInterval(s.length() == 0 ? null : (s + " " + tu.substring(0, tu.indexOf('('))));
		try
		{
			s = rejHiROC.getText().trim();
			als.setRejectRocHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Reject High Rate of Change '" + s + "' -- must be a number\n");
		}
		
		try
		{
			s = critHiROC.getText().trim();
			als.setCriticalRocHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Critical High Rate of Change '" + s + "' -- must be a number\n");
		}

		try
		{
			s = warnHiROC.getText().trim();
			als.setWarningRocHigh(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Warning High Rate of Change '" + s + "' -- must be a number\n");
		}

		try
		{
			s = warnLoROC.getText().trim();
			als.setWarningRocLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Warning Low Rate of Change '" + s + "' -- must be a number\n");
		}

		try
		{
			s = critLoROC.getText().trim();
			als.setCriticalRocLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Critical Low Rate of Change '" + s + "' -- must be a number\n");
		}

		try
		{
			s = rejLoROC.getText().trim();
			als.setRejectRocLow(s.length() == 0 ? AlarmLimitSet.UNASSIGNED_LIMIT :
				Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Reject Low Rate of Change '" + s + "' -- must be a number\n");
		}
		
		// Stuck Sensor Limits ========================
		s = stuckDurField.getText().trim();
		tu = (String)stuckDurCombo.getSelectedItem();
		als.setStuckDuration(s.length() == 0 ? null : (s + " " + tu.substring(0, tu.indexOf('('))));
		
		try
		{
			s = stuckToleranceField.getText().trim();
			als.setStuckTolerance(s.length() == 0 ? 0.0 : Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Stuck Sensor Tolerance '" + s + "' -- must be a number\n");
		}

		try
		{
			s = stuckMinField.getText().trim();
			als.setMinToCheck(s.length() == 0 ? 
				AlarmLimitSet.UNASSIGNED_LIMIT : Double.parseDouble(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad Stuck Sensor Min-to-Check '" + s 
				+ "' -- must be blank (for no minimum) or a number\n");
		}
		
		s = stuckMaxMultField.getText().trim();
		tu = (String)stuckMaxDurUnitCombo.getSelectedItem();
		als.setMaxGap(s.length() == 0 ? null : (s + " " + tu.substring(0, tu.indexOf('('))));

		// Missing Data Alarms ========================
		s = missingCheckDurField.getText().trim();
		tu = (String)missingCheckDurCombo.getSelectedItem();
		als.setMissingPeriod(s.length() == 0 ? null : (s + " " + tu.substring(0, tu.indexOf('('))));
		
		als.setMissingInterval((String)missingTSIntervalCombo.getSelectedItem());
		
		als.setLimitSetId(limitSet.getLimitSetId());
		
		try
		{
			s = missingMaxField.getText().trim();
			als.setMaxMissingValues(s.length() == 0 ? 0 : Integer.parseInt(s));
		}
		catch(NumberFormatException ex)
		{
			errors.append("Bad # Max Missing Values '" + s + "' -- must be an integer\n");
		}

		if (errors.length() > 0)
		{
			errors.insert(0, "Errors found on limits for season '" 
				+ limitSet.getSeason().getAbbr() + "'");
			throw new BadScreeningException(errors.toString());
		}
	}
}
