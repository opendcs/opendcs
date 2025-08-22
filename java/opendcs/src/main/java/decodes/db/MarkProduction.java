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

import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.cmdline.*;
import ilex.util.StderrLogger;
import decodes.util.*;

/**
 * Marks the isProduction flag for all records in the database
 */
public class MarkProduction
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/** Default constructor. */
	MarkProduction()
	{
	}


	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");
	static StringToken tfArg = new StringToken("", "true/false", "", 
		TokenOptions.optArgument, "");
	static
	{
		cmdLineArgs.addToken(tfArg);
	}

	/**
	  Main method.
	  @param args the arguments
	*/
	public static void main(String args[])
		throws Exception
	{
		boolean isProduction = true;

		// Parse command line arguments.
		cmdLineArgs.parseArgs(args);

		log.info("MarkProduction Starting =====================================");
		if (tfArg.getValue().equalsIgnoreCase("false"))
			isProduction = false;

		DecodesSettings settings = DecodesSettings.instance();

		// Read the edit database into memory.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO editDbio = 
			DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode,
			settings.editDatabaseLocation);
		db.setDbIo(editDbio);
		db.read();

		// Set the production flags according to the passed argument.
		for(Iterator it = db.platformList.iterator(); it.hasNext(); )
		{
			Platform p = (Platform)it.next();
			p.isProduction = isProduction;
			p.write();
		}
		for(Iterator it = db.presentationGroupList.iterator(); it.hasNext(); )
		{
			PresentationGroup p = (PresentationGroup)it.next();
			p.isProduction = isProduction;
			p.write();
		}
		for(Iterator it = db.routingSpecList.iterator(); it.hasNext(); )
		{
			RoutingSpec p = (RoutingSpec)it.next();
			p.isProduction = isProduction;
			p.write();
		}
	}
}
