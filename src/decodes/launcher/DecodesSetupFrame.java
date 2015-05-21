/*
 * $Id$
 *
 * $Log$
 * Revision 1.2  2014/05/22 12:15:21  mmaloney
 * Call Launcher Frame's setupSaved after saving config so it can adjust if necessary.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.2  2013/03/28 17:29:09  mmaloney
 * Refactoring for user-customizable decodes properties.
 *
 * Revision 1.1  2012/05/21 21:20:56  mmaloney
 * Generic DECODES Properties Frame
 *
 * This is open-source software written by Sutron Corporation under
 * contract to the federal government. Anyone is free to copy and use this
 * source code for any purpos, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * Except for specific contractual terms between Sutron and the federal 
 * government, this source code is provided completely without warranty.

 */
package decodes.launcher;

import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import java.util.Properties;
import java.util.ResourceBundle;
import java.io.*;

import ilex.util.EnvExpander;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import decodes.gui.TopFrame;
import decodes.launcher.DecodesPropsPanel;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

@SuppressWarnings("serial")
public class DecodesSetupFrame 
	extends TopFrame
{
	private static ResourceBundle labels = getLabels();
	private static ResourceBundle genericLabels = getGenericLabels();
	private JPanel jPanel3 = new JPanel();
	private FlowLayout flowLayout2 = new FlowLayout();
	private JPanel southButtonPanel = new JPanel();
	private JButton saveDecodesPropsButton = new JButton();
	private JButton abandonDecodesPropsButton = new JButton();
	private DecodesPropsPanel decodesPropsPanel;
	private LauncherFrame launcherFrame = null;
	
	private static DecodesSetupFrame _lastInstance = null;
	public static DecodesSetupFrame lastInstance() { return _lastInstance; }

	public DecodesSetupFrame(LauncherFrame launcherFrame)
	{
		this.launcherFrame = launcherFrame;
		exitOnClose = false;
		try
		{
			jbInit();
			setTitle(labels.getString("DecodesPropsPanel.title"));
			// ? this.setSize(new Dimension(770, 600));//650, 600
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener(new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					if (getExitOnClose())
						System.exit(0);
				}
			});
			pack();
			populateDecodesPropsTab();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		trackChanges("DecodesSetupFrame");
		_lastInstance = this;
	}

	public void cleanupBeforeExit()
	{
	}

	public static void main(String[] args)
	{
		CmdLineArgs cmdLineArgs = new CmdLineArgs();
		cmdLineArgs.parseArgs(args);
		labels = getLabels();
		genericLabels = getGenericLabels();
		DecodesSetupFrame setupFrame = new DecodesSetupFrame(null);
		setupFrame.setExitOnClose(true);

		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = setupFrame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		setupFrame.setLocation((screenSize.width - frameSize.width) / 2,
				(screenSize.height - frameSize.height) / 2);
		setupFrame.setVisible(true);
	}

	public static ResourceBundle getLabels()
	{
		if (labels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			// Return the main label descriptions for ToolKit Setup App
			labels = LoadResourceBundle.getLabelDescriptions(
					"decodes/resources/launcherframe", settings.language);
			if (labels == null)
				System.err.println("Cannot get labels!!!");
		}
		return labels;
	}

	public static ResourceBundle getGenericLabels()
	{
		if (genericLabels == null)
		{
			DecodesSettings settings = DecodesSettings.instance();
			// Load the generic properties file - includes labels that are used
			// in multiple screens
			genericLabels = LoadResourceBundle.getLabelDescriptions(
					"decodes/resources/generic", settings.language);
			if (genericLabels == null)
				System.err.println("Cannot get genericLabels!!!");
		}
		return genericLabels;
	}

	private void jbInit() throws Exception
	{
		this.getContentPane().setLayout(new BorderLayout());
		
		jPanel3.setLayout(flowLayout2);
		flowLayout2.setHgap(40);

		
		saveDecodesPropsButton.setText(
			labels.getString("DecodesPropsPanel.saveChanges"));
		saveDecodesPropsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					saveChangesPressed();
				}
			});
		abandonDecodesPropsButton.setText(
			labels.getString("DecodesPropsPanel.abandonChanges"));
		abandonDecodesPropsButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					abandonChangesPressed();
				}
			});
		southButtonPanel.add(saveDecodesPropsButton, null);
		southButtonPanel.add(abandonDecodesPropsButton, null);
		for(JButton jb : ResourceFactory.instance().additionalSetupButtons())
			southButtonPanel.add(jb, null);
		this.getContentPane().add(southButtonPanel, BorderLayout.SOUTH);

		decodesPropsPanel = new DecodesPropsPanel(this, labels, genericLabels);
		this.getContentPane().add(decodesPropsPanel, BorderLayout.CENTER);
	}

	private void saveChangesPressed()
	{
		DecodesSettings settings = DecodesSettings.instance();
		decodesPropsPanel.saveToSettings(settings);
		Properties props = new Properties();
		
		settings.saveToProps(props);
		
		// For owner, DCSTOOL_USERDIR == DCSTOOL_HOME
		String propFile = settings.isToolkitOwner() ?
			EnvExpander.expand("$DCSTOOL_HOME/decodes.properties")
			: EnvExpander.expand("$DCSTOOL_USERDIR/user.properties");
		try
		{
			FileOutputStream fos = new FileOutputStream(propFile);
			props.store(fos, "OPENDCS Toolkit Settings");
			fos.close();
		}
		catch (IOException ex)
		{
			Logger.instance().failure(
				"Cannot save DECODES Properties File '" + propFile + "': "
				+ ex);
		}
		if (launcherFrame != null)
			launcherFrame.setupSaved();
	}

	private void abandonChangesPressed()
	{
		populateDecodesPropsTab();
	}

	void populateDecodesPropsTab()
	{
		String propFile = 
			EnvExpander.expand("$DECODES_INSTALL_DIR/decodes.properties");
		DecodesSettings settings = DecodesSettings.instance();
		Properties props = new Properties();
		try
		{
			FileInputStream fis = new FileInputStream(propFile);
			props.load(fis);
			fis.close();
		}
		catch (IOException ex)
		{
			Logger.instance().failure(
				"Cannot open DECODES Properties File '" + propFile + "': " + ex);
		}
		settings.loadFromProperties(props);
		
		if (!settings.isToolkitOwner())
		{
			props.clear();
			propFile = EnvExpander.expand("$DCSTOOL_USERDIR/user.properties");
			try
			{
				FileInputStream fis = new FileInputStream(propFile);
				props.load(fis);
				fis.close();
				settings.loadFromUserProperties(props);
			}
			catch (IOException ex)
			{
				Logger.instance().debug1(
					"No User-Specific Properties File '" + propFile + "': " + ex);
			}
		}
		
		decodesPropsPanel.loadFromSettings(settings);
	}

}
