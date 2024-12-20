/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

package org.opendcs.odcsapi.res;

import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Context;

import decodes.cwms.CwmsDatabaseProvider;
import decodes.db.DatabaseException;
import decodes.util.DecodesSettings;
import opendcs.opentsdb.OpenTsdbProvider;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.spi.database.DatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

class OpenDcsResource
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDcsResource.class);

	@Context
	private ServletContext context;

	final <T extends OpenDcsDao> T getDao(Class<T> daoClass)
	{
		return createDb().getDao(daoClass)
				.orElseThrow(() -> new UnsupportedOperationException("Endpoint is unsupported by the OpenDCS REST API."));
	}

	final OpenDcsDatabase createDb()
	{
		DataSource dataSource = (DataSource) context.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		try
		{
			if(dataSource == null)
			{
				throw new IllegalStateException("No data source defined in context.xml");
			}
			return DatabaseService.getDatabaseFor(dataSource);
		}
		catch(DatabaseException e)
		{
			//Temporary workaround until database_properties table is implemented in the schema
			LOGGER.atWarn().setCause(e).log("Temporary solution forcing OpenTSDB");
			DecodesSettings decodesSettings = new DecodesSettings();
			try(Connection connection = dataSource.getConnection())
			{
				DatabaseProvider databaseProvider;
				String databaseProductName = connection.getMetaData().getDatabaseProductName();
				if(databaseProductName.toLowerCase().startsWith("oracle"))
				{
					databaseProvider = new CwmsDatabaseProvider();
				}
				else
				{
					databaseProvider = new OpenTsdbProvider();
				}
				return databaseProvider.createDatabase(dataSource, decodesSettings);
			}
			catch(DatabaseException | SQLException ex)
			{
				throw new IllegalStateException("Error connecting to the database via JNDI", ex);
			}
		}
	}
}
