/*
 * $Id$
 */
package decodes.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

import javax.swing.*;

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
	private JButton okButton = new JButton();

	public AboutBox(Frame parent, String appAbbr, String appName)
	{
		super(parent);
		
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try
		{
			guiInit(appAbbr, appName);
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
		
		JLabel ossLabel = new JLabel("Open Source Software, Supported by");
		ossLabel.setHorizontalAlignment(SwingConstants.CENTER);
		labelsPanel.add(ossLabel);

		JLabel coveLabel = new JLabel("Cove Software, LLC (www.covesw.com)");
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
