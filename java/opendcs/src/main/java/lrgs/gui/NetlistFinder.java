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
package lrgs.gui;

import java.io.File;
import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.InvalidDatabaseException;

/**
Contains static methods for finding network lists.
*/
public class NetlistFinder
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	* Finds a network list.
	* @returns File object representing the readable network list or null
	* if not found.
	*/
	public static File find(String name)
	{
		// The <all> and <production> lists are handled at a low-level inside
		// LddsClient, so there won't be any corresponding file.
		if (name.equalsIgnoreCase("<all>")
		 || name.equalsIgnoreCase("<production>"))
			return null;

		log.trace("Looking for network list '{}'", name);
		File f = new File(name);
		log.trace("Checking '{}'", f.getPath());
		if (f.canRead())
			return f;
		f = new File(EnvExpander.expand("$DECODES_INSTALL_DIR/netlist/"+name));
		log.trace("Checking '{}'", f.getPath());
		if (f.canRead())
		{
			return f;
		}

		String tmpdirname = EnvExpander.expand("$DCSTOOL_USERDIR/tmp");
		Database db = Database.getDb();
		if (db == null)
		{
			log.error("Unable to find netlist. Database is not available.");
			return null;
		}
		try
		{
			db.networkListList.read();
			decodes.db.NetworkList nl = db.networkListList.find(name);
			if (nl != null)
			{
				log.trace("Found DECODES netlist by that name.");

				nl.prepareForExec();
				lrgs.common.NetworkList lln = nl.legacyNetworkList;
				File tmpdir = new File(tmpdirname);
				if (!tmpdir.isDirectory())
					tmpdir.mkdirs();
				f = new File(tmpdir, name);
				log.trace("Creating '{}'", f.getPath());
				lln.saveFile(f);
				return f;
			}
		}
		catch(InvalidDatabaseException ex)
		{
			log.atWarn().setCause(ex).log("Cannot construct legacy netlist file for '{}'", name);
			return null;
		}
		catch(IOException ex)
		{
			log.atWarn()
			   .setCause(ex)
			   .log("Cannot write legacy netlist file '{}' in dir '{}'", name, tmpdirname);
			return null;
		}
		catch(DatabaseException ex)
		{
			log.atWarn().setCause(ex).log("Cannot load network lists from DECODES database.");
		}
		return null;
	}
}
