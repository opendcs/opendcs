package lrgs.rtstat;

import ilex.util.LoadResourceBundle;

import java.util.ResourceBundle;

import javax.swing.*;

import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
Main class for the Real Time Status Applications.
*/
public class RtStat
{
	private final static Logger log = LoggerFactory.getLogger(RtStat.class);
	private static ResourceBundle labels = null;
	private static ResourceBundle genericLabels = null;
	
	boolean packFrame = false;

	RtStatCmdLineArgs cmdLineArgs = new RtStatCmdLineArgs();
	RtStatFrame frame = null;

	//Construct the application
	public RtStat(String args[])
		throws Exception
	{
		cmdLineArgs.parseArgs(args);
		getMyLabelDescriptions();
		ResourceFactory.instance().initDbResources();
		frame = new RtStatFrame(
			cmdLineArgs.getScanPeriod(), 
			cmdLineArgs.getIconFile(),
			cmdLineArgs.getHeaderFile());

		final String hostname = cmdLineArgs.getHostName();
		if (hostname != null)
		{
			final String username = cmdLineArgs.getUserName();
			
			Thread delay = new Thread()
			{
				public void run()
				{
					try
					{
						sleep(3000L);
					} catch (InterruptedException e)
					{
					log.error("InterruptedException ",e);
					}
					SwingUtilities.invokeLater(
						new Runnable()
						{
							public void run()
							{
								frame.setHost(hostname);
								if (username != null)
									frame.userField.setText(username.trim());
								frame.connectButton_actionPerformed(null);
							}
						});
				}
			};
			delay.start();

		}
		if (packFrame)
		{
			frame.pack();
		}
		else
		{
			frame.validate();
		}
	}
	
	public RtStatFrame getFrame() { return frame; }
	
	public static void getMyLabelDescriptions()
	{
		DecodesSettings settings = DecodesSettings.instance();
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				settings.language);
		//Return the main label descriptions for RStat App
		labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/rtstat",
				settings.language);
	}
	
	public static ResourceBundle getLabels() 
	{
		if (labels == null)
			getMyLabelDescriptions();
		return labels;
	}

	public static ResourceBundle getGenericLabels() 
	{
		if (genericLabels == null)
			getMyLabelDescriptions();
		return genericLabels;
	}
	
	//Main method
	public static void main(String[] args)
		throws Exception
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			log.error("Error starting RtStat ",e);
		}
		RtStat rtStat = new RtStat(args);
		rtStat.frame.setVisible(true);
	}
}
