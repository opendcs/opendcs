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

import ilex.util.AuthException;
import ilex.util.LoadResourceBundle;
import lrgs.rtstat.hosts.LrgsConnection;
import lrgs.rtstat.hosts.LrgsConnectionPanel;

import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;

import decodes.util.ResourceFactory;

import org.opendcs.tls.TlsMode;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Main class for the Real Time Status Applications.
*/
public class RtStat
{
	private final static Logger log = OpenDcsLoggerFactory.getLogger();
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
				throw new AuthException("A Username and Password are now required for LRGS connections."
						 + " Please set one with -pw <password>.");
			}
			Thread delay = new Thread()
			{
				public void run()
				{
					try
					{
						sleep(3000L);
					}
					catch (InterruptedException ex)
					{
						log.atError().setCause(ex).log("InterruptedException ");
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
															null, TlsMode.NONE)
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
			RtStat rtStat = new RtStat(args);
			rtStat.frame.setVisible(true);
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Error starting RtStat.");
		}
	}
}
