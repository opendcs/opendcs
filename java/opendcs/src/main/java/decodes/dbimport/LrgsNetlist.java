/*
*  $Id$
*/
package decodes.dbimport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Iterator;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.StderrLogger;
import ilex.cmdline.*;

import decodes.util.*;
import decodes.db.*;

/**
This program writes an LRGS-style network list file from a DECODES
NetworkList database object.
*/
public class LrgsNetlist
{
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
	static StringToken netlistArg = new StringToken("n", "Network-List",
		"", TokenOptions.optArgument|TokenOptions.optMultiple, "");
	static StringToken dbLocArg = new StringToken("E", 
		"Explicit Database Location", "", TokenOptions.optSwitch, "");

	static
	{
		dbLocArg.setType("dirname");
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(netlistArg);
	}


	/**
	Usage: [java] decodes.import.LrgsNetlist options netlist1 ...
	<p>
	Options:
	<ul>
	  <li>-e               Export from editable database</li>
	</ul>
	*/
	public static void main(String args[])
		throws IOException, DecodesException
	{
		Logger.setLogger(new StderrLogger("LrgsNetlist"));

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		DecodesSettings settings = DecodesSettings.instance();
		Logger lg = Logger.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio;

		String dbloc = dbLocArg.getValue();
		if (dbloc.length() > 0)
		{
			dbio = DatabaseIO.makeDatabaseIO(settings.DB_XML, dbloc);
		}
		else
			dbio = DatabaseIO.makeDatabaseIO(
				settings.editDatabaseTypeCode, settings.editDatabaseLocation);
		
		// Standard Database Initialization for all Apps:
		Site.explicitList = false; // YES Sites automatically added to SiteList
		db.setDbIo(dbio);

		// Initialize standard collections:
		db.enumList.read();
//		db.dataTypeSet.read(); // dont need this?
//		db.engineeringUnitList.read(); // dont need this?
		db.siteList.read();
		db.platformList.read(); // dont need this?
//		db.platformConfigList.read(); // dont need this?
//		db.equipmentModelList.read(); // dont need this?
		db.networkListList.read();

		for(int i = 0; i < netlistArg.NumberOfValues(); i++)
		{
			String s = netlistArg.getValue(i);
			if (s.length() == 0)
				continue;

			try
			{
				NetworkList nl = db.networkListList.find(s);
				if (nl == null)
				{
					System.err.println(
						"No such network list '" + s + "' -- skipped.");
					continue;
				}

				nl.prepareForExec();
				File output = new File(nl.name + ".nl");
				//Check added to verify that legacyNetworkList is not null
				if (nl.legacyNetworkList != null)
				{
					System.out.println("Saving file '" + 
												output.getName() + "'");
					nl.legacyNetworkList.saveFile(output);	
				}
			}
			catch(Exception e)
			{
				System.err.println("Could not save network list '" + s 
					+ "': " + e);
			}
		}
	}
}

