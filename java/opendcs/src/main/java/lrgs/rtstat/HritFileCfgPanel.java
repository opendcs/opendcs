/**
 * $Id$
 * 
 * Copyright U.S. Government 2019
 */
package lrgs.rtstat;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import decodes.gui.GuiDialog;
import ilex.util.Logger;
import ilex.util.TextUtil;
import lrgs.lrgsmain.HritFileInterface;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.rtstat.LrgsConfigPanel;
import lrgs.rtstat.RtStat;

@SuppressWarnings("serial")
public class HritFileCfgPanel 
	extends JPanel
	implements LrgsConfigPanel
{
	private LrgsConfig conf = null;
	private JCheckBox enableCheck = null;
	private JTextField inputDirField = new JTextField();
	private JTextField filenamePrefixField = new JTextField();
	private JTextField filenameSuffixField = new JTextField();
	private JTextField srcField = new JTextField();
	private JTextField timeoutField = new JTextField();
	private JTextField maxAgeSecField = new JTextField();
	private JTextField doneDirField = new JTextField(9);
	private JCheckBox ccsdsHeaderCheck = new JCheckBox("CCSDS Header Present");
	
	private GuiDialog parent = null;

	public HritFileCfgPanel(GuiDialog parent)
	{
		jbinit();
		this.parent = parent;
	}

	@Override
	public String getLabel() { return "HRIT-File"; }
	
	private void jbinit()
	{
		this.setLayout(new GridBagLayout());
		
		setBorder(
			BorderFactory.createTitledBorder(
				"LRIT DAMS-NT " + RtStat.getGenericLabels().getString("parameters")));

		enableCheck = new JCheckBox(RtStat.getGenericLabels().getString("enable"));
		add(enableCheck,
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 15), 0, 0));
		
		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.inputDir")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(inputDirField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 30), 80, 0));

		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.prefix")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		
		add(filenamePrefixField,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 0), 40, 0));
		
		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.suffix")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		
		add(filenameSuffixField,
			new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 40, 0));

		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.srcCode")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(srcField,
			new GridBagConstraints(1, 4, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 40, 0));
		
		add(new JLabel(RtStat.getGenericLabels().getString("timeout")),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(timeoutField,
			new GridBagConstraints(1, 5, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 40, 0));
		
		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.fileMaxAge")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		add(maxAgeSecField,
			new GridBagConstraints(1, 6, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 40, 0));
		
		add(ccsdsHeaderCheck,
			new GridBagConstraints(1, 7, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 40, 0));

		add(new JLabel(RtStat.getLabels().getString("HritFilePanel.doneDir")),
			new GridBagConstraints(0, 8, 1, 1, 0.0, 0.5,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		add(doneDirField,
			new GridBagConstraints(1, 8, 1, 1, 0.5, 0.5,
				GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 30), 0, 0));
	}

	@Override
	public void fillFields(LrgsConfig conf)
	{
		enableCheck.setSelected(conf.hritFileEnabled);
		inputDirField.setText(conf.hritInputDir == null ? "" : conf.hritInputDir);
		filenamePrefixField.setText(conf.hritFilePrefix == null ? "" : conf.hritFilePrefix);
		filenameSuffixField.setText(conf.hritFileSuffix == null ? "" : conf.hritFileSuffix);
		srcField.setText(conf.hritSourceCode);
		timeoutField.setText("" + conf.hritTimeoutSec);
		maxAgeSecField.setText("" + conf.hritFileMaxAgeSec);
		doneDirField.setText(conf.hritDoneDir == null ? "" : conf.hritDoneDir);
		ccsdsHeaderCheck.setSelected(conf.lritHeaderType == lrgs.lrgsmain.HritFileInterface.FILE_HEADER_DOMAIN6);
		
		this.conf = conf;
	}
	
	public boolean hasChanged()
	{
		if (conf == null)
			return false;
		
		return enableCheck.isSelected() != conf.hritFileEnabled
		 || !TextUtil.strEqual(inputDirField.getText().trim(), conf.hritInputDir)
		 || !TextUtil.strEqualNE(filenamePrefixField.getText().trim(), conf.hritInputDir)
		 || !TextUtil.strEqualNE(filenameSuffixField.getText().trim(), conf.hritFileSuffix)
		 || !TextUtil.strEqual(srcField.getText().trim(), conf.lritSrcCode)
		 || !TextUtil.strEqual(timeoutField.getText().trim(), ""+conf.lritTimeout)
		 || !TextUtil.strEqual(maxAgeSecField.getText().trim(), ""+conf.lritMaxMsgAgeSec)
		 || !TextUtil.strEqualNE(doneDirField.getText().trim(), conf.hritDoneDir)
		 || ccsdsHeaderCheck.isSelected() !=
		 		(conf.lritHeaderType == HritFileInterface.FILE_HEADER_DOMAIN6)
		;
	}
	
	public void saveChanges()
	{
		if (conf == null)
			return;
		conf.hritFileEnabled = enableCheck.isSelected();
		conf.hritInputDir = inputDirField.getText().trim();
		conf.hritFilePrefix = filenamePrefixField.getText().trim();
		if (conf.hritFilePrefix.length() == 0)
			conf.hritFilePrefix = null;
		conf.hritFileSuffix = filenameSuffixField.getText().trim();
		if (conf.hritFileSuffix.length() == 0)
			conf.hritFileSuffix = null;
		conf.hritSourceCode = srcField.getText().trim();

		try { conf.hritTimeoutSec = Integer.parseInt(timeoutField.getText().trim()); }
		catch(Exception ex)
		{
			Logger.instance().warning("Invalid HRIT timeout '" + timeoutField.getText()
				+ "' -- set to default of 120 seconds");
			conf.hritTimeoutSec = 120;
		}
		try { conf.hritFileMaxAgeSec = Integer.parseInt(maxAgeSecField.getText().trim()); }
		catch(Exception ex)
		{
			Logger.instance().warning("Invalid File max age (seconds) '" + timeoutField.getText()
				+ "' -- set to default of 7200 seconds");
			conf.hritFileMaxAgeSec = 7200;
		}
		
		conf.hritDoneDir = doneDirField.getText().trim();
		if (conf.hritDoneDir.length() == 0)
			conf.hritDoneDir = null;
		
		conf.lritHeaderType = (ccsdsHeaderCheck.isSelected() ? HritFileInterface.FILE_HEADER_DOMAIN6 
				: HritFileInterface.FILE_HEADER_NONE);
	}
}
