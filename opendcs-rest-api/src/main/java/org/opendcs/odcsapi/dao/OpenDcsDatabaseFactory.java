/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import decodes.cwms.CwmsDatabaseProvider;
import decodes.db.DatabaseException;
import decodes.sql.OracleSequenceKeyGenerator;
import decodes.util.DecodesSettings;
import opendcs.opentsdb.OpenTsdbProvider;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.spi.database.DatabaseProvider;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

public final class OpenDcsDatabaseFactory
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	private static OpenDcsDatabase database;

	private OpenDcsDatabaseFactory()
	{
		throw new AssertionError("Utility class");
	}

	public static synchronized OpenDcsDatabase createDb(DataSource dataSource)
	{
		if(database != null)
		{
			return database;
		}
		try
		{
			if(dataSource == null)
			{
				throw new IllegalStateException("No data source defined in context.xml");
			}
			database = DatabaseService.getDatabaseFor(dataSource);
		}
		catch(DatabaseException ex)
		{
			//Temporary workaround until database_properties table is implemented in the schema
			log.atWarn().setCause(ex).log("Temporary solution forcing OpenTSDB");
			DecodesSettings decodesSettings = new DecodesSettings();
			DecodesSettings.instance().writeCwmsLocations = true;
			decodesSettings.CwmsOfficeId = DbInterface.decodesProperties.getProperty("CwmsOfficeId");
			try(Connection connection = dataSource.getConnection())
			{
				DatabaseProvider databaseProvider;
				String databaseProductName = connection.getMetaData().getDatabaseProductName();
				if(databaseProductName.toLowerCase().startsWith("oracle"))
				{
					databaseProvider = new CwmsDatabaseProvider();
					decodesSettings.sqlKeyGenerator = OracleSequenceKeyGenerator.class.getName();
				}
				else
				{
					databaseProvider = new OpenTsdbProvider();
				}
				database = databaseProvider.createDatabase(dataSource, decodesSettings);
			}
			catch(DatabaseException | SQLException ex2)
			{
				ex2.addSuppressed(ex);
				throw new IllegalStateException("Error connecting to the database via JNDI", ex2);
			}
		}
		return database;
	}
}
