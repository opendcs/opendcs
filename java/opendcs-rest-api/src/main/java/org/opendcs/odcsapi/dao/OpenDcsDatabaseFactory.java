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

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import decodes.db.DatabaseException;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.datasource.ConnectionPreparer;
import org.opendcs.odcsapi.dao.datasource.ConnectionPreparingDataSource;
import org.opendcs.odcsapi.dao.datasource.DelegatingConnectionPreparer;
import org.opendcs.odcsapi.dao.cwms.SessionOfficePreparer;

public final class OpenDcsDatabaseFactory
{

	/**
	 * The plan going forward is to add the organization as to a database context mechanism
	 * Right now the office id is set statefully in too many places to allow for reuse
	 * of the OpenDcsDatabase instance.
	 */
	private static final Map<String, OpenDcsDatabase> dbCache = new HashMap<>();

	private OpenDcsDatabaseFactory()
	{
		throw new AssertionError("Utility class");
	}

	public static synchronized OpenDcsDatabase createDb(DataSource dataSource, String organization)
	{
		return dbCache.computeIfAbsent(organization, o -> newDatabase(dataSource, organization));
	}

	private static OpenDcsDatabase newDatabase(DataSource dataSource, String organization)
	{
		if(dataSource == null)
		{
			throw new IllegalStateException("No data source defined in context.xml");
		}
		try
		{
			List<ConnectionPreparer> preparers = List.of(new SessionOfficePreparer(organization));
			DataSource wrappedDataSource = new ConnectionPreparingDataSource(
					new DelegatingConnectionPreparer(preparers), dataSource);
			Properties properties = new Properties();
			if(organization != null)
			{
				properties.put("CwmsOfficeId", organization);
			}
			return DatabaseService.getDatabaseFor(wrappedDataSource, properties);
		}
		catch(DatabaseException ex)
		{
			Throwable cause = ex.getCause();
			if(cause instanceof SQLException sqlException && sqlException.getErrorCode() == 28113)
			{
				throw new IllegalArgumentException("Error establishing organization id for request.", ex);
			}
			throw new IllegalStateException("Error establishing database instance through data source.", ex);
		}
	}
}
