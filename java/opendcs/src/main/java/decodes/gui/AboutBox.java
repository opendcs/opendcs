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
package decodes.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.*;

import org.opendcs.gui.GuiHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;

import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import lrgs.gui.LrgsBuild;

/**
 * Canned About dialog.
 */
@SuppressWarnings("serial")
public class AboutBox extends JDialog implements ActionListener
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private JButton okButton = new JButton();

	public AboutBox(Frame parent, String appAbbr, String appName)
	{
		super(parent);

		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			guiInit(appAbbr, appName);
		}
		catch (Exception ex)
		{
			GuiHelpers.logGuiComponentInit(log, ex);
		}
		pack();
	}

	/**
	 * Component initialization
	 */
	private void guiInit(String appAbbr, String appName)
	{
		DecodesSettings settings = DecodesSettings.instance();
		ResourceBundle genericLabels = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic", settings.language);

		String label = appAbbr + " " + DecodesVersion.getAbbr()
			+ " " + DecodesVersion.getVersion();
		this.setTitle(genericLabels.getString("about") + " " + label);
		setResizable(false);

		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel centerPanel = new JPanel(new BorderLayout());
		JPanel imagePanel = new JPanel(new FlowLayout());
		JPanel labelsPanel = new JPanel(new GridLayout(5, 1));
		JPanel buttonPanel = new JPanel(new FlowLayout());

		// The image on the left side of the center panel
		centerPanel.add(imagePanel, BorderLayout.WEST);
		imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JLabel imageLabel = new JLabel(new ImageIcon(
			EnvExpander.expand("$DECODES_INSTALL_DIR/icons/setup64x64.gif"),
			"OPENDCS Icon"));
		imagePanel.add(imageLabel, null);

		// The labels on the right side of the center panel
		centerPanel.add(labelsPanel, BorderLayout.CENTER);
		labelsPanel.setBorder(BorderFactory.createEmptyBorder(10, 60, 10, 10));
		JLabel abbrLabel = new JLabel(label);
		abbrLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(abbrLabel);

		JLabel lrgsNameLabel = new JLabel(appName);
		lrgsNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(lrgsNameLabel);

		JLabel buildDateLabel = new JLabel("Build: " + LrgsBuild.buildDate);
		buildDateLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(buildDateLabel);

		JLabel ossLabel = new JLabel("Supported by");
		ossLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(ossLabel);

		JLabel coveLabel = new JLabel("OpenDCS Consortium (https://github.com/opendcs/opendcs)");
		coveLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(coveLabel);

		// The buttons panel with the OK button on the bottom
		okButton.setText(genericLabels.getString("OK"));
		okButton.addActionListener(this);
		this.getContentPane().add(mainPanel, null);
		buttonPanel.add(okButton, null);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		mainPanel.add(centerPanel, BorderLayout.NORTH);
	}

	/**
	 * Overridden so we can exit when window is closed
	 */
	protected void processWindowEvent(WindowEvent e)
	{
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			cancel();
		}
		super.processWindowEvent(e);
	}

	/**
	 * Close the dialog
	 */
	void cancel()
	{
		dispose();
	}

	/**
	 * Close the dialog on a button event
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == okButton)
		{
			cancel();
		}
	}
}
