package decodes.decwiz;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesSettings;
import decodes.util.DecodesVersion;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.TokenOptions;
import ilex.gui.WindowUtility;
import ilex.util.Logger;

import javax.swing.*;


public class DecodingWizard
{
	boolean packFrame = false;
	private DecWizFrame decWizFrame;

	/**
	 * Construct and show the application.
	 */
	public DecodingWizard()
	{
		decWizFrame = new DecWizFrame();
		decWizFrame.pack();

		WindowUtility.center(decWizFrame);
		decWizFrame.createPanels();
		decWizFrame.switchPanel(0);
	}

	public void show()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				try
				{
					decWizFrame.setVisible(true);
				}
				catch (Exception exception)
				{
					exception.printStackTrace();
				}

			}
		});
	}

	/** The command line arguments */
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "decwiz.log");
	static BooleanToken noLimitsArg = new BooleanToken("m", 
		"Do NOT apply Sensor min/max limits.", "", 
		TokenOptions.optSwitch, false);

	static
	{
		cmdLineArgs.addToken(noLimitsArg);
	}

	/**
	 * Application entry point.
	 *
	 * @param args String[]
	 */
	public static void main(String[] args)
	{
		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		Logger.instance().info(
			"Decoding Wizard Starting (" + DecodesVersion.startupTag()
			+ ") =====================");

		// Initialize settings from properties file
		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		try
		{
			Database db = new decodes.db.Database();
			Database.setDb(db);
			DatabaseIO dbio = null; 
			dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
				settings.editDatabaseLocation);

			Platform.configSoftLink = false;
			System.out.print("Reading DB: "); System.out.flush();
			db.setDbIo(dbio);
			System.out.print("Enum, "); System.out.flush();
			db.enumList.read();
			System.out.print("DataType, "); System.out.flush();
			db.dataTypeSet.read();
			System.out.print("EU, "); System.out.flush();
			db.engineeringUnitList.read();

			// Note: ExplicitList false means everytime a Site is created,
			// it is automatically added to list. I want this because the
			// site list will then get populated when I read the platform list.
			Site.explicitList = false;
			System.out.print("Site, "); System.out.flush();
			db.siteList.read();
			System.out.print("Plat, "); System.out.flush();
			db.platformList.read();
		
			System.out.println("DONE.");

			db.presentationGroupList.read();
//			db.routingSpecList.read();
//			db.networkListList.read();
		}
		catch(DatabaseException ex)
		{
			Logger.instance().fatal("Cannot initialize DECODES database: "+ex);
			System.exit(1);
		}
		DecodingWizard decwiz = new DecodingWizard();
		decwiz.show();
	}
}
