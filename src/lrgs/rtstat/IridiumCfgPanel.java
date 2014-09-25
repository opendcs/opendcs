/**
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

import ilex.util.TextUtil;
import decodes.gui.GuiDialog;
import lrgs.iridiumsbd.IridiumSbdInterface;
import lrgs.lrgsmain.LrgsConfig;
import lrgs.rtstat.LrgsConfigPanel;
import lrgs.rtstat.RtStat;

/**
 * Configuration panel for Iridium LRGS Interface
 * @author mmaloney Mike Maloney, Cove Software, LLC
 *
 */
@SuppressWarnings("serial")
public class IridiumCfgPanel 
	extends JPanel implements LrgsConfigPanel
{
	private LrgsConfig lrgsConfig = null;
	private JTextField forwardPortNumField = new JTextField();
	private JCheckBox enableCheckBox = new JCheckBox();
	private JTextField portNumField = new JTextField();
	private JTextField captureFileField = new JTextField();

	/** Constructor */
	public IridiumCfgPanel(GuiDialog parent)
	{
		jbInit();
	}

	@Override
	public boolean hasChanged()
	{
//System.out.println("hasChanged, enabled was=" + lrgsConfig.iridiumEnabled
//+ ", checkbox=" + enableCheckBox.isSelected());
		if (lrgsConfig == null)
			return false;
		
		if (enableCheckBox.isSelected() != lrgsConfig.iridiumEnabled
		 || !TextUtil.strEqual(captureFileField.getText().trim(), 
			 	lrgsConfig.iridiumCaptureFile)
		 || getIntField(portNumField, IridiumSbdInterface.SBD_LISTEN_PORT) 
		 		!= lrgsConfig.iridiumPort)
			return true;
		return false;
	}
	
	@Override
	public void fillFields(LrgsConfig lrgsConfig)
	{
		this.lrgsConfig = lrgsConfig;
		enableCheckBox.setSelected(lrgsConfig.iridiumEnabled);
//System.out.println("fillFields, enabled="+ enableCheckBox.isSelected());
		portNumField.setText(String.valueOf(lrgsConfig.iridiumPort));
		captureFileField.setText(lrgsConfig.iridiumCaptureFile);
	}
	
	@Override
	public void saveChanges()
	{
		if (lrgsConfig == null)
			return;
		
		lrgsConfig.iridiumEnabled = enableCheckBox.isSelected();
//System.out.println("saveChanges, enabled="+ enableCheckBox.isSelected());
		lrgsConfig.iridiumPort = getIntField(portNumField,
			IridiumSbdInterface.SBD_LISTEN_PORT);
		String s = captureFileField.getText().trim();
		lrgsConfig.iridiumCaptureFile = s.length() == 0 ? null : s;
	}
	
	private void jbInit()
	{
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createTitledBorder(
			"Iridium " + RtStat.getGenericLabels().getString("parameters")));
		enableCheckBox.setText(RtStat.getGenericLabels().getString("enable"));

		add(enableCheckBox,
			new GridBagConstraints(1, 0, 1, 1, 0.5, 0.5,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 15), 0, 0));
		
		add(new JLabel(RtStat.getGenericLabels().getString("port") + ":"),
			new GridBagConstraints(0, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		add(portNumField,
			new GridBagConstraints(1, 1, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(3, 0, 3, 0), 60, 0));

		add(new JLabel(RtStat.getLabels().getString("LrgsConfigDialog.iridiumCapture")),
			new GridBagConstraints(0, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(3, 15, 3, 2), 0, 0));
		add(captureFileField,
			new GridBagConstraints(1, 2, 1, 1, 0.5, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(3, 0, 3, 15), 0, 0));
	}
	
	/**
	 * Parse an integer from a JTextField and return the value.
	 * If field is unparsable, return the supplied default value.
	 * @param field the field to parse
	 * @param defaultVal the default to return if field is unparsable
	 * @return the integer field value or the default.
	 */
	private int getIntField(JTextField field, int defaultVal)
	{
		//Get port number from text field
		try{ return Integer.parseInt(portNumField.getText().trim()); }
		catch(Exception e)
		{
			return IridiumSbdInterface.SBD_LISTEN_PORT;
		}
	}

	@Override
	public String getLabel()
	{
		return RtStat.getLabels().getString("LrgsConfigDialog.iridiumTab");
	}
}
