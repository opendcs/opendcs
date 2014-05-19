package decodes.syncgui;

import javax.swing.UIManager;
import javax.swing.JOptionPane;
import java.awt.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import java.util.Properties;

import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.util.EnvExpander;
import ilex.cmdline.*;

public class SyncGui 
{
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
		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height) {
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width) {
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
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
		Logger.setLogger(new StderrLogger("RoutingSpecThread"));
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
				Logger.instance().info("No file '" + hubHomeFile + "': " + ex);
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
					Logger.instance().warning("Can't write '" 
						+ hubHomeFile + "': " + ex);
				}
			}
		}
		SyncConfig.instance().setHubHome(hubHomeUrl);

		String urlstr = SyncConfig.instance().getHubHome() + "/hub.conf";
		try
		{
			URL url = new URL(urlstr);
			InputStream fis = url.openStream();
			SyncConfig.instance().readConfig(fis);
			fis.close();
		}
		catch(MalformedURLException ex)
		{
			System.err.println("Malformed URL: " + urlstr);
			System.exit(1);
		}
		catch(IOException ex)
		{
			System.err.println("Can't open configuration file at " + urlstr);
			System.exit(1);
		}

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		SyncGui syncGUI = new SyncGui();
		syncGUI.run();
	}
}
