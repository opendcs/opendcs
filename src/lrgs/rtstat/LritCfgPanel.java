/**
 * $Id$
 * 
 * Open Source Software by Cove Software, LLC
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
import lrgs.lrgsmain.LrgsConfig;
import lrgs.rtstat.LrgsConfigPanel;
import lrgs.rtstat.RtStat;

@SuppressWarnings("serial")
public class LritCfgPanel 
	extends JPanel
	implements LrgsConfigPanel
{
	private LrgsConfig conf = null;
	private JCheckBox enableCheck = null;
	private JTextField hostField = new JTextField();
	private JTextField portField = new JTextField();
	private JTextField syncPatternField = new JTextField();
	private JTextField srcField = new JTextField();
	private JTextField timeoutField = new JTextField();
	private JTextField maxAgeSecField = new JTextField();
	private GuiDialog parent = null;

	public LritCfgPanel(GuiDialog parent)
	{
		jbinit();
		this.parent = parent;
	}

	@Override
	public String getLabel() { return "LRIT"; }
	
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
			new GridBagConstraints(0, 6, 1, 1, 0.0, 0.5,
				GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
				new Insets(3, 25, 3, 1), 0, 0));

		add(maxAgeSecField,
			new GridBagConstraints(1, 6, 1, 1, 0.5, 0.5,
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 30), 50, 0));
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
		this.conf = conf;
	}
	
	public boolean hasChanged()
	{
		if (conf == null)
			return false;
		
		boolean ret = enableCheck.isSelected() != conf.enableLritRecv
		 || !TextUtil.strEqual(hostField.getText().trim(), conf.lritHostName)
		 || !TextUtil.strEqual(portField.getText().trim(), ""+conf.lritPort)
		 || !TextUtil.strEqual(syncPatternField.getText().trim(), conf.lritDamsNtStartPattern)
		 || !TextUtil.strEqual(srcField.getText().trim(), conf.lritSrcCode)
		 || !TextUtil.strEqual(timeoutField.getText().trim(), ""+conf.lritTimeout)
		 || !TextUtil.strEqual(maxAgeSecField.getText().trim(), ""+conf.lritMaxMsgAgeSec)
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
				Logger.instance().warning("Invalid LRIT DAMS-NT Port " + portField.getText()
					+ " -- must be a positive integer. Using default of 17010");
			}
		}
		conf.lritDamsNtStartPattern = syncPatternField.getText().trim();
		conf.lritSrcCode = srcField.getText().trim();
		try { conf.lritTimeout = Integer.parseInt(timeoutField.getText().trim()); }
		catch(Exception ex)
		{
			Logger.instance().warning("Invalid lrit timeout '" + timeoutField.getText()
				+ "' -- set to default of 120 seconds");
			conf.lritTimeout = 120;
		}
		try { conf.lritMaxMsgAgeSec = Integer.parseInt(maxAgeSecField.getText().trim()); }
		catch(Exception ex)
		{
			Logger.instance().warning("Invalid lrit max age (seconds) '" + timeoutField.getText()
				+ "' -- set to default of 7200 seconds");
			conf.lritTimeout = 7200;
		}
	}

}
