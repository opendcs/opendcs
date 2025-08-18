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
package decodes.cwms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.db.DatabaseException;
import decodes.db.Platform;
import decodes.db.PlatformConfig;
import decodes.db.PlatformList;
import decodes.sql.ConfigListIO;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.EquipmentModelListIO;
import decodes.sql.PlatformListIO;

public class CwmsPlatformListIO extends PlatformListIO
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	public CwmsPlatformListIO(CwmsSqlDatabaseIO dbio, ConfigListIO configListIO,
		EquipmentModelListIO emlIO)
	{
		super(dbio, configListIO, emlIO);
	}

	public void writePlatform(Platform p)
		throws SQLException, DatabaseException
	{
		if (p.getSite() == null)
			throw new DatabaseException("Cannot save CWMS Platform without Site association.");
		if (p.getConfig() == null)
			throw new DatabaseException("Cannot save CWMS Platform without Config association.");
		super.writePlatform(p);
	}

	public void read(PlatformList platformList)
		throws SQLException, DatabaseException
	{
		log.debug("Reading CWMS PlatformList...");

		_pList = platformList;

		CwmsSqlDatabaseIO cwmsDbIo = (CwmsSqlDatabaseIO)getDbio();

		

		// When we read platform list, have to joine it with config so that
		// we implicitely get the predicate to filter on db_office_code.
		String q = "SELECT a.ID, a.Agency, a.IsProduction, " +
			 "a.SiteId, a.ConfigId, a.Description, a.LastModifyTime, " +
			 "a.Expiration, a.platformDesignator " +
			 "FROM Platform a, PlatformConfig b " +
			 "WHERE a.ConfigId = b.id";

		log.trace("Executing '{}'", q);
		try(Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(q);)
		{
			while (rs.next())
			{
				DbKey platformId = DbKey.createDbKey(rs, 1);

				// MJM 20041027 Check to see if this ID is already in the
				// cached platform list and ignore if so. That way, I can
				// periodically refresh the platform list to get any newly
				// created platforms after the start of the routing spec.
				// Refreshing will not affect previously read/used platforms.
				Platform p = _pList.getById(platformId);
				if (p != null)
					continue;

				p = new Platform(platformId);
				_pList.add(p);

				p.agency = rs.getString(2);

				DbKey siteId = DbKey.createDbKey(rs, 4);
				if (!rs.wasNull()) {
					p.setSite(p.getDatabase().siteList.getSiteById(siteId));
				}

				DbKey configId = DbKey.createDbKey(rs, 5);
				if (!rs.wasNull())
				{
					PlatformConfig pc =
						platformList.getDatabase().platformConfigList.getById(
							configId);
					if (pc == null)
					{
						log.warn("Platform({}) references config({})," +
								 " which is not in list, will attempt read...",
								 platformId, configId);
						try { pc = _configListIO.getConfig(configId); }
						catch(Exception ex)
						{
							log.atWarn().setCause(ex).log("Error reading config({})", configId);
						}
					}
					if (pc != null)
						p.setConfigName(pc.configName);
					p.setConfig(pc);
				}

				String desc = rs.getString(6);
				if (!rs.wasNull())
					p.setDescription(desc);

				p.lastModifyTime = getTimeStamp(rs, 7, null);

				p.expiration = getTimeStamp(rs, 8, p.expiration);

				if (getDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_7)
					p.setPlatformDesignator(rs.getString(9));

			}
		}
		readAllTransportMedia(platformList);
	}
}
