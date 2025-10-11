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
package lrgs.rtstat;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.gui.GuiDialog;
import ilex.util.TextUtil;
import lrgs.lrgsmain.LrgsConfig;

@SuppressWarnings("serial")
public class LritCfgPanel extends JPanel implements LrgsConfigPanel
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private LrgsConfig conf = null;
	private JCheckBox enableCheck = null;
	private JTextField hostField = new JTextField();
	private JTextField portField = new JTextField();
	private JTextField syncPatternField = new JTextField();
	private JTextField srcField = new JTextField();
	private JTextField timeoutField = new JTextField();
	private JTextField maxAgeSecField = new JTextField();
	private GuiDialog parent = null;
	private JTextField minHourlyField = new JTextField(9);

	public LritCfgPanel(GuiDialog parent)
	{
		jbinit();
		this.parent = parent;
	}

	@Override
	public String getLabel() { return "HRIT-DAMSNT"; }

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

		add(new JLabel(RtStat.getLabels().getString("LritPanel.hostname")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(hostField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 0), 80, 0));

		add(new JLabel(RtStat.getLabels().getString("LritPanel.port")),
			new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(portField,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 0), 30, 0));

		add(new JLabel(RtStat.getLabels().getString("LritPanel.syncpatt")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(syncPatternField,
			new GridBagConstraints(1, 3, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 30), 0, 0));

		add(new JLabel(RtStat.getLabels().getString("LritPanel.srccode")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(srcField,
			new GridBagConstraints(1, 4, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 30), 0, 0));

		add(new JLabel(RtStat.getGenericLabels().getString("timeout")),
			new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(timeoutField,
			new GridBagConstraints(1, 5, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 50, 0));

		add(new JLabel(RtStat.getLabels().getString("LritPanel.maxage")),
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		add(maxAgeSecField,
			new GridBagConstraints(1, 6, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 50, 0));

		add(new JLabel(RtStat.getLabels().getString("minHourly")),
			new GridBagConstraints(0, 7, 1, 1, 0.0, 0.5,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));
		add(minHourlyField,
			new GridBagConstraints(1, 7, 1, 1, 0.5, 0.5,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 0, 0));
	}

	@Override
	public void fillFields(LrgsConfig conf)
	{
		enableCheck.setSelected(conf.enableLritRecv);
		hostField.setText(conf.lritHostName);
		portField.setText("" + conf.lritPort);
		syncPatternField.setText(conf.lritDamsNtStartPattern);
		srcField.setText(conf.lritSrcCode);
		timeoutField.setText("" + conf.lritTimeout);
		maxAgeSecField.setText("" + conf.lritMaxMsgAgeSec);
		minHourlyField.setText(
			conf.lritMinHourly > 0 ? ("" + conf.lritMinHourly) : "");
		this.conf = conf;
	}

	public boolean hasChanged()
	{
		if (conf == null)
			return false;

		int minHourly = getMinHourly();

		boolean ret = enableCheck.isSelected() != conf.enableLritRecv
		 || !TextUtil.strEqual(hostField.getText().trim(), conf.lritHostName)
		 || !TextUtil.strEqual(portField.getText().trim(), ""+conf.lritPort)
		 || !TextUtil.strEqual(syncPatternField.getText().trim(), conf.lritDamsNtStartPattern)
		 || !TextUtil.strEqual(srcField.getText().trim(), conf.lritSrcCode)
		 || !TextUtil.strEqual(timeoutField.getText().trim(), ""+conf.lritTimeout)
		 || !TextUtil.strEqual(maxAgeSecField.getText().trim(), ""+conf.lritMaxMsgAgeSec)
		 || minHourly != conf.lritMinHourly
		;
		return ret;
	}

	public void saveChanges()
	{
		if (conf == null)
			return;
		conf.enableLritRecv = enableCheck.isSelected();
		conf.lritHostName = hostField.getText().trim();
		if (portField.getText().trim().length() > 0)
		{
			try
			{
				conf.lritPort = Integer.parseInt(portField.getText().trim());
				if (conf.lritPort <= 0) throw new Exception ("Negative port number");
			}
			catch(Exception ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Invalid LRIT DAMS-NT Port {} -- " +
						"must be a positive integer. Using default of 17010",
						portField.getText());
			}
		}
		conf.lritDamsNtStartPattern = syncPatternField.getText().trim();
		conf.lritSrcCode = srcField.getText().trim();
		try { conf.lritTimeout = Integer.parseInt(timeoutField.getText().trim()); }
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Invalid lrit timeout '{}' -- set to default of 120 seconds", timeoutField.getText());
			conf.lritTimeout = 120;
		}
		try { conf.lritMaxMsgAgeSec = Integer.parseInt(maxAgeSecField.getText().trim()); }
		catch(Exception ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Invalid lrit max age (seconds) '{}' -- set to default of 7200 seconds",
			   		maxAgeSecField.getText());
			conf.lritTimeout = 7200;
		}
		conf.lritMinHourly = getMinHourly();
	}

	private int getMinHourly()
	{
		String s = minHourlyField.getText().trim();
		if (s.length() == 0)
			return 0;
		try
		{
			return Integer.parseInt(s);
		}
		catch(NumberFormatException ex)
		{
			log.atWarn().setCause(ex).log("EDL Minimum Hourly field must be an integer.");
			return 0;
		}
	}


}
