/**
 * $Id$
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to Alberta Environment and Sustainable Resource Development (Alberta ESRD).
 * No warranty is provided or implied other than specific contractual terms 
 * between COVE and Alberta ESRD.
 *
 * Copyright 2014 Alberta Environment and Sustainable Resource Development.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lrgs.rtstat;

import java.awt.GridBagLayout;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import java.awt.Font;
import java.awt.Color;

import javax.swing.JCheckBox;
import java.awt.GridBagConstraints;

import javax.swing.JLabel;
import javax.swing.JTextField;

import java.awt.Insets;
import java.util.ResourceBundle;

import decodes.gui.GuiDialog;
import decodes.gui.TopFrame;

import lrgs.lrgsmain.LrgsConfig;

public class EdlConfigPanel 
	extends JPanel
	implements LrgsConfigPanel
{
	private static final long serialVersionUID = 1L;
	private JCheckBox enableCheckBox = new JCheckBox();
	private JTextField ingestDirText = new JTextField();
	private JCheckBox recursiveCheckBox = new JCheckBox();
	private JTextField filenameSuffixText = new JTextField();
	private JTextField doneDirText = new JTextField();
	private GuiDialog parent = null;
	public LrgsConfig lrgsConfig = null;
	private static ResourceBundle genericLabels = RtStat.getGenericLabels();
	private static ResourceBundle rtstatLabels = RtStat.getLabels();

	public String getLabel()
	{
		return rtstatLabels.getString("LrgsConfigDialog.edlTab");
	}
	
	public EdlConfigPanel(GuiDialog parent)
	{
		super();
		this.parent = parent;
		initialize();
	}
	
	private void initialize()
	{
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createTitledBorder(null,
			"EDL " + genericLabels.getString("parameters"),
			TitledBorder.CENTER, TitledBorder.BELOW_TOP, 
			new Font("Dialog", Font.BOLD, 14), new Color(51, 51, 51)));
		
		enableCheckBox.setText(genericLabels.getString("enable"));
		add(enableCheckBox, 
			new GridBagConstraints(1, 0, 1, 1, 0.0, 0.5,
				GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 5), 0, 0));
		
		add(new JLabel(rtstatLabels.getString("EdlPanel.ingestDir")),
			new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(ingestDirText,
			new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 15), 20, 0));

		recursiveCheckBox.setText(genericLabels.getString("recursive") + "?");
		add(recursiveCheckBox, 
			new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 5), 0, 0));

		add(new JLabel(rtstatLabels.getString("EdlPanel.filenameSuffix")),
			new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(filenameSuffixText,
			new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 0, 5, 15), 20, 0));

		add(new JLabel(rtstatLabels.getString("EdlPanel.doneDir")),
			new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));
		add(doneDirText,
			new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
				new Insets(5, 0, 5, 15), 20, 0));
		add(new JLabel(" "),
			new GridBagConstraints(0, 5, 2, 1, 0.0, 0.5,
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(5, 30, 5, 3), 0, 0));

	}


	public void fillFields(LrgsConfig tmp)
	{
		enableCheckBox.setSelected(tmp.edlIngestEnable);
		ingestDirText.setText(
			tmp.edlIngestDirectory != null ? tmp.edlIngestDirectory : "");
		recursiveCheckBox.setSelected(tmp.edlIngestRecursive);
		filenameSuffixText.setText(
			tmp.edlFilenameSuffix != null ? tmp.edlFilenameSuffix : "");
		doneDirText.setText(
			tmp.edlDoneDirectory != null ? tmp.edlDoneDirectory : "");
		lrgsConfig = tmp;
	}
	
	public boolean hasChanged()
	{
		if (lrgsConfig == null)
			return false;
		if (enableCheckBox.isSelected() != lrgsConfig.edlIngestEnable
		 || recursiveCheckBox.isSelected() != lrgsConfig.edlIngestRecursive
		 || !stringSame(ingestDirText.getText(), lrgsConfig.edlIngestDirectory)
		 || !stringSame(filenameSuffixText.getText(), lrgsConfig.edlFilenameSuffix)
		 || !stringSame(doneDirText.getText(), lrgsConfig.edlDoneDirectory))
			return true;
		return false;
	}
	
	/**
	 * Compare 2 string fields. Consider empty strings the same as null.
	 * @param s1
	 * @param s2
	 * @return
	 */
	private boolean stringSame(String s1, String s2)
	{
		if (s1 == null)
			return s2 == null || s2.trim().length() == 0;
		else if (s2 == null)
			return s1.trim().length() == 0;
		else
			return s1.trim().equals(s2.trim());
	}
	
	public void saveChanges()
	{
		if(lrgsConfig == null)
			return;
		lrgsConfig.edlIngestEnable = enableCheckBox.isSelected();
		lrgsConfig.edlIngestRecursive = recursiveCheckBox.isSelected();
		lrgsConfig.edlIngestDirectory = ingestDirText.getText().trim();
		lrgsConfig.edlFilenameSuffix =
			filenameSuffixText.getText().trim().length() > 0 ? filenameSuffixText.getText() : null;
		lrgsConfig.edlDoneDirectory =
			doneDirText.getText().trim().length() > 0 ? doneDirText.getText() : null;
	}
}