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

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.tsdb.TimeSeriesDb;
import opendcs.opentsdb.OpenTsdb;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;
import org.opendcs.odcsapi.sec.basicauth.OpenTsdbAuthorizationDAO;
import org.opendcs.odcsapi.sec.cwms.CwmsAuthorizationDAO;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

public abstract class AuthorizationCheck
{

	/**
	 * Authorizes the current session returning the SecurityContext that will check user roles.
	 *
	 * @param requestContext     context for the current session.
	 * @param httpServletRequest context for the current request.
	 */
	public abstract SecurityContext authorize(ContainerRequestContext requestContext,
			HttpServletRequest httpServletRequest, ServletContext servletContext);

	public abstract boolean supports(String type, ContainerRequestContext requestContext, ServletContext servletContext);


	protected final ApiAuthorizationDAI getAuthDao(ServletContext servletContext)
	{
		DataSource dataSource = (DataSource) servletContext.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		OpenDcsDatabase db = OpenDcsDatabaseFactory.createDb(dataSource);
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
}
