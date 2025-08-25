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

import javax.swing.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


public class DecodingWizard
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		log.info("Decoding Wizard Starting ({}) =====================", DecodesVersion.startupTag());

		// Initialize settings from properties file
		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		try
		{
			Database db = new decodes.db.Database();
			Database.setDb(db);
			DatabaseIO dbio = null;
			dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode, settings.editDatabaseLocation);

			Platform.configSoftLink = false;
			log.info("Reading DB: ");
			db.setDbIo(dbio);
			log.info("Enum, ");
			db.enumList.read();
			log.info("DataType, ");
			db.dataTypeSet.read();
			log.info("EU, ");
			db.engineeringUnitList.read();

			// Note: ExplicitList false means everytime a Site is created,
			// it is automatically added to list. I want this because the
			// site list will then get populated when I read the platform list.
			Site.explicitList = false;
			log.info("Site, ");
			db.siteList.read();
			log.info("Plat, ");
			db.platformList.read();

			log.info("DONE.");

			db.presentationGroupList.read();
			DecodingWizard decwiz = new DecodingWizard();
			decwiz.show();
		}
		catch(DatabaseException ex)
		{
			log.atError().setCause(ex).log("Cannot initialize DECODES database.");
			System.exit(1);
		}
	}
}
