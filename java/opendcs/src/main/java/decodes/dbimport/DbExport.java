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

import java.io.IOException;
import java.util.Vector;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.LoadingAppDAI;
import opendcs.dai.ScheduleEntryDAI;

import ilex.cmdline.*;

import decodes.tsdb.DbIoException;
import decodes.util.*;
import decodes.db.*;
import decodes.xml.TopLevelParser;

/**
Exports the entire database to an XML file.
Writes output to stdout.
*/
public class DbExport
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false, "util.log");

	static
	{
	}


	/**
	Usage: [java] decodes.import.DbExport <options>
	Options:
	<ul>
	  <li>-i         Export from 'installed' database (default is edit db)</li>
	  <li>-l logfile Specify log file. Default="./util.log"</li>
	  <li>-insecure  include usernames/passwords in export. Default=false </li>
	</ul>
	@param args command line arguments
	*/
	public static void main(String args[])
		throws IOException, DecodesException
	{


		// Parse command line arguments.

		BooleanToken insecureArg = new BooleanToken("insecure", "include passwords and username in output",
													"", TokenOptions.optSwitch, false);
		cmdLineArgs.addToken(insecureArg);
		cmdLineArgs.parseArgs(args);

		boolean insecure = insecureArg.getValue();

		log.info("DbExport Starting ({}) =====================", DecodesVersion.startupTag());

		DecodesSettings settings = DecodesSettings.instance();

		// Construct the database and the interface specified by properties.
		Database db = new decodes.db.Database();
		Database.setDb(db);
		DatabaseIO dbio;

		dbio = DatabaseIO.makeDatabaseIO(settings.editDatabaseTypeCode, settings.editDatabaseLocation);

		// Standard Database Initialization for all Apps:
		Site.explicitList = false; // YES Sites automatically added to SiteList
		db.setDbIo(dbio);

		// Initialize standard collections:
		db.enumList.read();
		db.dataTypeSet.read();
		db.engineeringUnitList.read();
		db.siteList.read();
		db.equipmentModelList.read();
		db.platformConfigList.read();
		db.platformList.read();
		db.networkListList.read();
		db.dataSourceList.read();
		db.presentationGroupList.read();
		db.routingSpecList.read();

		try (LoadingAppDAI loadingAppDAO = dbio.makeLoadingAppDAO())
		{
			db.loadingAppList.addAll(loadingAppDAO.listComputationApps(false));
		}
		catch(DbIoException ex)
		{
			log.atWarn().setCause(ex).log("Cannot list loading apps.");
			db.loadingAppList.clear();
		}

		ScheduleEntryDAI scheduleEntryDAO = dbio.makeScheduleEntryDAO();
		if (scheduleEntryDAO == null)
			log.debug("Cannot export schedule entries. Not supported on this database.");
		else
		{
			try
			{
				db.schedEntryList.addAll(scheduleEntryDAO.listScheduleEntries(null));
			}
			catch(DbIoException ex)
			{
				log.atWarn().setCause(ex).log("Cannot list schedule entries.");
				db.schedEntryList.clear();
			}
			finally
			{
				scheduleEntryDAO.close();
			}
		}

		Vector<Platform> platforms = db.platformList.getPlatformVector();

		// Completely read all platform data from the database
		for(Platform p : platforms)
		{
			try { p.read(); }
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Error reading platform '{}'", p.makeFileName());
			}
		}

		// Likewise, completely read all presentation groups
		Vector<PresentationGroup> pgs = db.presentationGroupList.getVector();
		for(PresentationGroup pg : pgs)
		{
			try { pg.read(); }
			catch(Exception ex)
			{
				log.atError().setCause(ex).log("Error reading presentation group '{}'",  pg.getDisplayName());
			}
		}

		TopLevelParser.write(System.out, db, insecure);
	}
}
