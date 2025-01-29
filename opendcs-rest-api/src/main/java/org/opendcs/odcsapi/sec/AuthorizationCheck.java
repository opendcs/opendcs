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

package org.opendcs.odcsapi.sec;

import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import decodes.cwms.CwmsDatabaseProvider;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.db.DatabaseException;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import opendcs.opentsdb.OpenTsdb;
import opendcs.opentsdb.OpenTsdbProvider;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.sec.basicauth.OpenTsdbAuthorizationDAO;
import org.opendcs.odcsapi.sec.cwms.CwmsAuthorizationDAO;
import org.opendcs.spi.database.DatabaseProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

public abstract class AuthorizationCheck
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationCheck.class);

	/**
	 * Authorizes the current session returning the SecurityContext that will check user roles.
	 *
	 * @param requestContext     context for the current session.
	 * @param httpServletRequest context for the current request.
	 */
	public abstract SecurityContext authorize(ContainerRequestContext requestContext,
			HttpServletRequest httpServletRequest, ServletContext servletContext);

	public abstract boolean supports(String type, ContainerRequestContext requestContext);


	protected final ApiAuthorizationDAI getAuthDao(ServletContext servletContext)
	{
		OpenDcsDatabase db = createDb(servletContext);
		TimeSeriesDb timeSeriesDb = db.getLegacyDatabase(TimeSeriesDb.class)
				.orElseThrow(() -> new UnsupportedOperationException("Endpoint is unsupported by the OpenDCS REST API."));
		//Need to figure out a better way to extend the toolkit API to be able to add dao's within the REST API
		if(timeSeriesDb instanceof CwmsTimeSeriesDb)
		{
			return new CwmsAuthorizationDAO(timeSeriesDb);
		}
		else if(timeSeriesDb instanceof OpenTsdb)
		{
			return new OpenTsdbAuthorizationDAO(timeSeriesDb);
		}
		throw new UnsupportedOperationException("Endpoint is unsupported by the OpenDCS REST API.");
	}

	private OpenDcsDatabase createDb(ServletContext servletContext)
	{
		DataSource dataSource = (DataSource) servletContext.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
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
			decodesSettings.CwmsOfficeId = System.getProperty("DB_OFFICE");
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
