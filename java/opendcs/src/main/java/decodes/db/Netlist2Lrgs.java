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

package decodes.db;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import ilex.cmdline.*;
import ilex.util.StderrLogger;
import decodes.util.*;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import org.xml.sax.SAXException;

/**
 * This is a utility to convert DECODES-style network lists into LRGS (or DRS)
 * formatted network lists.
 */
public class Netlist2Lrgs
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Default constructor. */
    public Netlist2Lrgs()
    {
    }

    static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");

	static StringToken nlArgs = new StringToken("", "Netlist-Names", "",
		TokenOptions.optArgument|TokenOptions.optMultiple
		|TokenOptions.optRequired, "");
	static StringToken dbLocArg = new StringToken("E",
		"Explicit Database Location", "", TokenOptions.optSwitch, "");
	static BooleanToken useEditArg = new BooleanToken("e", "Use Edit DB", "",
		TokenOptions.optSwitch, false);

	static
	{
		cmdLineArgs.addToken(nlArgs);
		cmdLineArgs.addToken(dbLocArg);
		cmdLineArgs.addToken(useEditArg);
	}

	/**
	 * Usage: [java] decodes.db.Netlist2Lrgs [network-list list...]
	  @param args the arguments
	 */
	public static void main(String args[])
		throws IOException, DecodesException,
		       SAXException, ParserConfigurationException
	{
		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio = null;
		String dbloc = dbLocArg.getValue();
		if (dbloc.length() > 0)
		{
			dbio = DatabaseIO.makeDatabaseIO(settings.DB_XML, dbloc);
		}
		else
		{
			dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
				settings.editDatabaseLocation);
		}
		db.setDbIo(dbio);

		Site.explicitList = false; // YES Sites automatically added to SiteList

		// Initialize standard collections:
		db.enumList.read();

        db.networkListList.read();

		for(int i = 0; i < nlArgs.NumberOfValues(); i++)
		{
            String nlname = nlArgs.getValue();
            NetworkList nl = db.networkListList.find(nlname);
            if (nl == null)
            {
                log.error("No such network list {}", nlname);
                break;
            }
            nl.read();
            nl.prepareForExec();
            if (nl.legacyNetworkList != null)
            	System.out.println(nl.legacyNetworkList.toFileString());
		}
	}
}
