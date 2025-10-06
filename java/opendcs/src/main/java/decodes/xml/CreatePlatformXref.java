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
package decodes.xml;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import org.xml.sax.SAXException;
import java.util.Iterator;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.PlatformList;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.DatabaseIO;
import ilex.util.Counter;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import decodes.sql.DbKey;
import decodes.util.*;

/**
 * Main class for utility to create the platform cross reference XML file.
 * This utility reads all platorm files in the specified database (found
 * in the platform subdirectory under the specified database directory).
 * and creates a files called "PlatformList.xml" in the same subdirectory.
 *
 * Usage: [java] decodes.xml.CreatePlatformXref databaseRoot
 *
 */
public class CreatePlatformXref
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	 * This method does the actual work. It can be called from another
	 * class or from the main.
	 * @param dbRoot
	 * @param oldDb the database
	 * @throws DatabaseException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void createPlatformXref(String dbRoot, Database oldDb)
		throws DatabaseException, SAXException, ParserConfigurationException
	{
		Database tmpDb = new Database();
		tmpDb.enumList = oldDb.enumList;
		tmpDb.engineeringUnitList = oldDb.engineeringUnitList;
		tmpDb.dataTypeSet = oldDb.dataTypeSet;
		tmpDb.unitConverterSet = oldDb.unitConverterSet;

		DecodesSettings settings = new DecodesSettings();
		settings.editDatabaseTypeCode = DecodesSettings.DB_XML;
		settings.editDatabaseLocation = dbRoot;
		XmlDatabaseIO dbio = (XmlDatabaseIO)DatabaseIO.makeDatabaseIO(settings);
		tmpDb.setDbIo(dbio);
		
		Database.setDb(tmpDb);
		try
		{
			// Delete the old cross reference file, if one exists.
			File f = new File(dbio.makePath(XmlDatabaseIO.PlatformDir,
				XmlDatabaseIO.PlatformListFile));
			log.debug("Deleting old Cross Reference File '{}'", f.getPath());
			if (f.exists())
				f.delete();

			PlatformList pl = tmpDb.platformList;
			dbio.readAllPlatforms(pl);

			// Find the highest used ID for any platform
			int highest = -1;
			for(Iterator it = tmpDb.platformList.iterator(); it.hasNext(); )
			{
				Platform p = (Platform)it.next();
	            int id = (int)p.getId().getValue();
				if (id > highest)
					highest = id;
			}

			// Set the current Platform File Counter to highest+1
			Counter idcounter = dbio.getPlatformIdCounter();
			idcounter.setNextValue(highest+1);

			// Set ID for any platforms that don't have one, & rewrite the files.
			for(Iterator it = tmpDb.platformList.iterator(); it.hasNext(); )
			{
				Platform p = (Platform)it.next();
				if (!p.idIsSet())
				{
					p.setId(DbKey.createDbKey(idcounter.getNextValue()));
					p.write();
				}
			}

			// Write out the finished list.
			pl.write();
		}
		finally
		{
			// Whatever happens, restore the old database!
			Database.setDb(oldDb);
		}
	}

	static CmdLineArgs cmdLineArgs = new CmdLineArgs(false,"util.log");

	/**
	 * @param args
	 * @throws DatabaseException
	 * @throws IOException
	 * @throws DecodesException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static void main( String[] args )
		throws DatabaseException, IOException, DecodesException, SAXException,
		ParserConfigurationException
	{

		cmdLineArgs.parseArgs(args);

		log.info("CreatePlatformXref Starting ======================");

		DecodesSettings settings = DecodesSettings.instance();

		int type = settings.editDatabaseTypeCode;
		if (type != DecodesSettings.DB_XML)
		{
			log.error("Cannot create Platform Xref for non-XML database ({})", type);
			System.exit(1);
		}

		String dbRoot = settings.editDatabaseLocation;

		Database db = new decodes.db.Database();
		Database.setDb(db);

		DatabaseIO dbio = DatabaseIO.makeDatabaseIO(DecodesSettings.DB_XML,
			dbRoot);

		// Standard Database Initialization for all Apps:
		Site.explicitList = false;
		db.setDbIo(dbio);

		// Initialize standard collections:
		db.enumList.read();
		db.dataTypeSet.read();

		createPlatformXref(dbRoot, db);
	}
}