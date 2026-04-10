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
package decodes.tsdb;

import java.util.HashMap;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import lrgs.gui.DecodesInterface;
import decodes.db.Database;
import decodes.db.Platform;
import decodes.sql.SqlDatabaseIO;

/**
 * This is primarily for large DCP Monitor databases.
 * It will remove any platform that ...
 * - Has no transport media
 * - Has no config
 * - Is expired (i.e. has an expiration date)
 * - Has the same site/designator combination as another platform
 *   (Note, which platform is kept is indeterminate).
 */
public class CleanupPlatforms extends TsdbAppTemplate
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public CleanupPlatforms()
	{
		super("util.log");
		DecodesInterface.silent = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		SqlDatabaseIO dbio = (SqlDatabaseIO)Database.getDb().getDbIo();

		HashMap<String, Platform> platnames = new HashMap<String, Platform>();
		for(Iterator<Platform> pit = Database.getDb().platformList.iterator(); pit.hasNext(); )
		{
			Platform p = pit.next();
			String platname = p.makeFileName();
			if (p.transportMedia.size() == 0)
			{
				log.info("Removing platform {} '{}' from agency '{}' because it has no transport media.",
						 p.getKey(), platname, p.getAgency());
				dbio.deletePlatform(p);
				continue;
			}
			if (p.getConfig() == null)
			{
				log.info("Removing platform {} '{}' from agency '{}' because it has no configuration.",
						 p.getKey(), platname, p.getAgency());
				dbio.deletePlatform(p);
				continue;
			}
			if (p.expiration != null)
			{
					log.info("Removing platform {} '{}' from agency '{}' it has an expiration date of {}",
							 p.getKey(), platname, p.getAgency(), p.expiration);
					dbio.deletePlatform(p);
					continue;
			}
			Platform existingPlat = platnames.get(platname);
			if (existingPlat != null)
			{
				log.info("Removing platform {} '{}' from agency '{}' because there is " +
						 "already a platform with that name. The existing platform has ID={}  and  agency '{}'",
						 p.getKey(), platname, p.getAgency(), existingPlat.getId(), existingPlat.getAgency());
				dbio.deletePlatform(p);
			}

			// Fell through -- we're keeping this one.
			platnames.put(platname, p);
		}
	}

	public static void main(String[] args)
		throws Exception
	{
		CleanupPlatforms to = new CleanupPlatforms();
		to.execute(args);
	}

}