package lrgs.rtstat;

import ilex.util.LoadResourceBundle;
import lrgs.rtstat.hosts.LrgsConnection;
import lrgs.rtstat.hosts.LrgsConnectionPanel;

import java.util.Locale;
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
			final String password = cmdLineArgs.getPassword();
			final int port = cmdLineArgs.getPort();
			if (username == null || password == null)
			{
				log.error("A Username and Password are now required for LRGS connections."
						 + " Please set one with -pw <password>.");
			}
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
								if (username != null)
								{
									frame.connectButton_actionPerformed(
										new LrgsConnection(hostname, port,
															username,
															LrgsConnection.encryptPassword(
																password,
																LrgsConnectionPanel.pwk),
															null)
									);
								}
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
		//Load the generic properties file - includes labels that are used
		//in multiple screens
		Locale locale = Locale.getDefault();
		LoadResourceBundle.setLocale(locale.getLanguage());
		genericLabels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/generic",
				locale.getLanguage());
		//Return the main label descriptions for RStat App
		labels = LoadResourceBundle.getLabelDescriptions(
				"decodes/resources/rtstat",
				locale.getLanguage());
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
			System.out.println("Default local: " + Locale.getDefault().toString());
		}
		catch (Exception e)
		{
			log.error("Error starting RtStat ",e);
		}
		RtStat rtStat = new RtStat(args);
		rtStat.frame.setVisible(true);
	}
}
