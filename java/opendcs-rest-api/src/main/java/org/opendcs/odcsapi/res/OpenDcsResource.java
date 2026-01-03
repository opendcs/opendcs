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

package org.opendcs.odcsapi.res;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletContext;
import javax.sql.DataSource;

import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.tsdb.TimeSeriesDb;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;
import static org.opendcs.odcsapi.util.ApiConstants.ORGANIZATION_HEADER;

@OpenAPIDefinition(
		info = @Info(
				title = "OpenDCS - Swagger",
				description = "OpenDCS Rest API is web application that provides access to the OpenDCS database using JSON (Java Script Object Notation). " +
						"Source and documentation may be found here: [Github Documentation](https://github.com/opendcs/rest_api)",
				version = "0.0.3"
		)
)
public class OpenDcsResource
{
	private static final String UNSUPPORTED_OPERATION_MESSAGE = "Endpoint is unsupported by the OpenDCS REST API.";

	@HeaderParam(ORGANIZATION_HEADER)
	@Parameter(description = "Organization ID for the request", required = true)
	protected String organizationId;

	@Context
	protected ContainerRequestContext request;

	@Context
	protected ServletContext context;

	@Resource
	protected ExecutorService executor = Executors.newCachedThreadPool();

	protected final synchronized OpenDcsDatabase createDb()
	{
		DataSource dataSource = getDataSource();
		return OpenDcsDatabaseFactory.createDb(dataSource, organizationId);
	}

	protected final DataSource getDataSource()
	{
		DataSource dataSource = (DataSource) context.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		if(dataSource == null)
		{
			throw new IllegalStateException("DataSource not found in ServletContext.");
		}
		return dataSource;
	}

	protected final DatabaseIO getLegacyDatabase()
	{
		return createDb().getLegacyDatabase(Database.class)
				.map(db -> 
				{
					// if you hate this line... good. but it can't go away until we've made all the new DAOs.
					// or someone goes through and makes sure Database is always injected. (NOTE: The DAOs will be easier.)
					Database.setDb(db);
					return db.getDbIo();
				})
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}

	protected TimeSeriesDb getLegacyTimeseriesDB()
	{
		getLegacyDatabase(); // make sure Database.setDb is called because way too many things use it.
		return createDb().getLegacyDatabase(TimeSeriesDb.class)
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}
}
