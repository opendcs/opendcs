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
package decodes.syncgui;

import javax.swing.UIManager;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import javax.swing.JOptionPane;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;

import decodes.util.CmdLineArgs;
import ilex.gui.WindowUtility;
import ilex.util.EnvExpander;
import ilex.cmdline.*;

public class SyncGui
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	boolean packFrame = false;

	SyncGuiFrame frame;

	//Construct the application
	public SyncGui()
	{
		frame = SyncGuiFrame.instance();
		//Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, e.g. from their layout
		if (packFrame) {
			frame.pack();
		}
		else {
			frame.validate();
		}
		WindowUtility.center(frame);
	}

	public void run()
	{
		frame.setVisible(true);
		frame.treePanel.expandFirstRow();
	}

	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "syncgui.log");
	static StringToken hubHomeArg = new StringToken("h", "Hub-Home", "",
		TokenOptions.optSwitch, "");
	static { cmdLineArgs.addToken(hubHomeArg); }

	//Main method
	public static void main(String[] args)
		throws Exception
	{
		cmdLineArgs.parseArgs(args);

		String hubHomeUrl = hubHomeArg.getValue().trim();
		if (hubHomeUrl.length() == 0)
		{
			File hubHomeFile = new File(
				EnvExpander.expand("$DECODES_INSTALL_DIR/hubhome.txt"));
			try
			{
				BufferedReader reader =
					new BufferedReader(new FileReader(hubHomeFile));
				hubHomeUrl = reader.readLine();
				reader.close();
				if (hubHomeUrl == null)
					hubHomeUrl = "";
				else
					hubHomeUrl = hubHomeUrl.trim();
			}
			catch(IOException ex)
			{
				log.atInfo().setCause(ex).log("No file '{}'", hubHomeFile);
				hubHomeUrl = "";
			}

			hubHomeUrl = JOptionPane.showInputDialog(null,
				"Enter the URL to the hub's home directory: ", hubHomeUrl);
			if (hubHomeUrl == null || hubHomeUrl.length() == 0)
				System.exit(1);
			else
			{
				try
				{
					BufferedWriter writer =
						new BufferedWriter(new FileWriter(hubHomeFile));
					writer.write(hubHomeUrl);
					writer.newLine();
					writer.close();
				}
				catch(IOException ex)
				{
					log.atWarn().setCause(ex).log("Can't write '{}'", hubHomeFile);
				}
			}
		}
		SyncConfig.instance().setHubHome(hubHomeUrl);

		String urlstr = SyncConfig.instance().getHubHome() + "/hub.conf";
		try
		{
			URL url = new URL(urlstr);
			try (InputStream fis = url.openStream())
			{
				SyncConfig.instance().readConfig(fis);
			}
		}
		catch(MalformedURLException ex)
		{
			log.atError().setCause(ex).log("Malformed URL: {}", urlstr);
			System.exit(1);
		}
		catch(IOException ex)
		{
			log.atError().setCause(ex).log("Can't open configuration file at {}", urlstr);
			System.exit(1);
		}

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			log.atError().setCause(ex).log("Unable set set look and feel.");
		}
		SyncGui syncGUI = new SyncGui();
		syncGUI.run();
	}
}
