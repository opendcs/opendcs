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

package org.opendcs.odcsapi.sec.cwms;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

import com.google.auto.service.AutoService;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;

@AutoService(AuthorizationCheck.class)
public final class ServletSsoAuthCheck extends AuthorizationCheck
{
	static final String SESSION_COOKIE_NAME = "JSESSIONIDSSO";

	@Override
	public OpenDcsSecurityContext authorize(ContainerRequestContext requestContext, HttpServletRequest httpServletRequest, ServletContext servletContext)
	{
		//User Principal is set by the single sign-on (e.g. CWMS AAA) web app for the servlet container (e.g. Tomcat)
		Principal userPrincipal = requestContext.getSecurityContext().getUserPrincipal();
		if(userPrincipal == null)
		{
			throw new NotAuthorizedException("User has not established a Single Sign-On session.");
		}
		try(ApiAuthorizationDAI authorizationDao = getAuthDao(servletContext))
		{
			Set<OpenDcsApiRoles> roles = authorizationDao.getRoles(userPrincipal.getName());
			OpenDcsPrincipal openDcsPrincipal = new OpenDcsPrincipal(userPrincipal.getName(), roles);
			return new OpenDcsSecurityContext(openDcsPrincipal,
					httpServletRequest.isSecure(), SESSION_COOKIE_NAME);
		}
		catch(Exception ex)
		{
			throw new ServerErrorException("Error accessing database to determine user roles",
					Response.Status.INTERNAL_SERVER_ERROR, ex);
		}
	}

	@Override
	public boolean supports(String type, ContainerRequestContext requestContext, ServletContext ignored)
	{
		return "sso".equals(type) && hasSessionCookie(requestContext);
	}

	private boolean hasSessionCookie(ContainerRequestContext request)
	{
		Map<String, Cookie> cookies = request.getCookies();
		Cookie cookie = cookies.get(SESSION_COOKIE_NAME);
		return cookie != null;
	}
}
