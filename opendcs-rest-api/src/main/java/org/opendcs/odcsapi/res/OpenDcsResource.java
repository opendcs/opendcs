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

import jakarta.servlet.ServletContext;
import javax.sql.DataSource;
import jakarta.ws.rs.core.Context;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.tsdb.TimeSeriesDb;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

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

	@Context
	protected ServletContext context;

	protected final synchronized OpenDcsDatabase createDb()
	{
		DataSource dataSource = (DataSource) context.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		return OpenDcsDatabaseFactory.createDb(dataSource);
	}

	protected DatabaseIO getLegacyDatabase()
	{
		return createDb().getLegacyDatabase(Database.class)
				.map(Database::getDbIo)
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}

	protected TimeSeriesDb getLegacyTimeseriesDB()
	{
		return createDb().getLegacyDatabase(TimeSeriesDb.class)
				.orElseThrow(() -> new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE));
	}
}
