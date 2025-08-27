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
package decodes.dbimport;

import java.io.File;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;

import decodes.util.*;
import decodes.db.*;

/**
This program writes an LRGS-style network list file from a DECODES
NetworkList database object.
*/
public class LrgsNetlist
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		DecodesSettings settings = DecodesSettings.instance();

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
		db.siteList.read();
		db.platformList.read();
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
					log.error("No such network list '{}' -- skipped.", s);
					continue;
				}

				nl.prepareForExec();
				File output = new File(nl.name + ".nl");
				//Check added to verify that legacyNetworkList is not null
				if (nl.legacyNetworkList != null)
				{
					System.out.println("Saving file '" + output.getName() + "'");
					nl.legacyNetworkList.saveFile(output);	
				}
			}
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Could not save network list '{}'", s);
			}
		}
	}
}

