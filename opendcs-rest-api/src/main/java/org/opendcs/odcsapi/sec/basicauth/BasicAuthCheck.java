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

package org.opendcs.odcsapi.sec.basicauth;

import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import com.google.auto.service.AutoService;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;

@AutoService(AuthorizationCheck.class)
public final class BasicAuthCheck implements AuthorizationCheck
{

	@Override
	public OpenDcsSecurityContext authorize(ContainerRequestContext requestContext, HttpServletRequest httpServletRequest)
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
		Set<OpenDcsApiRoles> roles = getUserRoles(username);
		OpenDcsPrincipal principal = new OpenDcsPrincipal(username, roles);
		return new OpenDcsSecurityContext(principal,
				httpServletRequest.isSecure(), SecurityContext.BASIC_AUTH);
	}

	@Override
	public boolean supports(String type, ContainerRequestContext ignored)
	{
		return "basic".equals(type) && DbInterface.isOpenTsdb;
	}

	static Set<OpenDcsApiRoles> getUserRoles(String username)
	{
		try(DbInterface dbi = new DbInterface();
			ApiAuthorizationDAI dao = dbi.getDao(ApiAuthorizationDAI.class))
		{
			return dao.getRoles(username);
		}
		catch(Exception e)
		{
			throw new IllegalStateException("Unable to query the database for user authorization", e);
		}
	}
}
