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

package org.opendcs.odcsapi.sec.basicauth;

import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import com.google.auto.service.AutoService;
import decodes.tsdb.TimeSeriesDb;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.dao.OpenDcsDatabaseFactory;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;

import static org.opendcs.odcsapi.res.DataSourceContextCreator.DATA_SOURCE_ATTRIBUTE_KEY;

@AutoService(AuthorizationCheck.class)
public final class BasicAuthCheck extends AuthorizationCheck
{

	@Override
	public OpenDcsSecurityContext authorize(ContainerRequestContext requestContext, HttpServletRequest httpServletRequest, ServletContext servletContext)
	{
		HttpSession session = httpServletRequest.getSession(false);
		if(session == null)
		{
			throw new NotAuthorizedException("User has not yet established a session.");
		}
		Object attribute = session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE);
		if(!(attribute instanceof OpenDcsPrincipal))
		{
			throw new NotAuthorizedException("User has not established an authenticated session.");
		}
		String username = ((OpenDcsPrincipal) attribute).getName();
		Set<OpenDcsApiRoles> roles = getUserRoles(username, servletContext);
		OpenDcsPrincipal principal = new OpenDcsPrincipal(username, roles);
		return new OpenDcsSecurityContext(principal,
				httpServletRequest.isSecure(), SecurityContext.BASIC_AUTH);
	}

	@Override
	public boolean supports(String type, ContainerRequestContext ignored, HttpServletRequest servletContext)
	{
		DataSource dataSource = (DataSource) servletContext.getAttribute(DATA_SOURCE_ATTRIBUTE_KEY);
		OpenDcsDatabase db = OpenDcsDatabaseFactory.createDb(dataSource);
		TimeSeriesDb timeSeriesDb = db.getLegacyDatabase(TimeSeriesDb.class)
				.orElseThrow(() -> new UnsupportedOperationException("Endpoint is unsupported by the OpenDCS REST API."));
		return "basic".equals(type) && timeSeriesDb.isOpenTSDB();
	}

	private Set<OpenDcsApiRoles> getUserRoles(String username, ServletContext servletContext)
	{
		try(ApiAuthorizationDAI dao = getAuthDao(servletContext))
		{
			return dao.getRoles(username);
		}
		catch(Exception e)
		{
			throw new IllegalStateException("Unable to query the database for user authorization", e);
		}
	}
}
