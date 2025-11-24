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

import java.util.Collections;
import java.util.ServiceLoader;
import javax.annotation.Priority;
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.slf4j.Logger;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

@Provider
@Priority(Priorities.AUTHORIZATION)
public final class SecurityFilter implements ContainerRequestFilter
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();

	@Context
	private ResourceInfo resourceInfo;
	@Context
	private HttpHeaders httpHeaders;
	@Context
	private HttpServletRequest httpServletRequest;
	@Context
	private ServletContext servletContext;

	@Override
	public void filter(ContainerRequestContext requestContext)
	{

		// TODO: instead of create session here check and then see if a stateless method is used
		HttpSession session = httpServletRequest.getSession(true);
		Object sessionPrincipal = session.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE);
		if (sessionPrincipal != null)
		{
			OpenDcsPrincipal principal = (OpenDcsPrincipal) sessionPrincipal;
			requestContext.setSecurityContext(
				new OpenDcsSecurityContext(principal, httpServletRequest.isSecure(), SecurityContext.BASIC_AUTH));
		}
		else
		{
			setupGuestContext(requestContext);
		}
	
		
		verifyRoles(requestContext);
	}

	private void setupGuestContext(ContainerRequestContext requestContext)
	{
		if(log.isTraceEnabled())
		{
			log.trace("Public endpoint identified: {}", resourceInfo.getResourceMethod().toGenericString());
		}
		OpenDcsPrincipal principal = new OpenDcsPrincipal("guest", Collections.singleton(OpenDcsApiRoles.ODCS_API_GUEST));
		requestContext.setSecurityContext(new OpenDcsSecurityContext(principal,
				httpServletRequest.isSecure(), ""));
		
	}

	private void verifyRoles(ContainerRequestContext requestContext)
	{
		SecurityContext securityContext = requestContext.getSecurityContext();
		RolesAllowed annotation = resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class);
		String endpoint = requestContext.getMethod() + " " + requestContext.getUriInfo().getPath();
		if (!resourceInfo.getResourceClass().equals(OpenApiResource.class))
		{
			if(annotation == null)
			{
				throw new InternalServerErrorException("Endpoint " + endpoint + " does not have the @RolesAllowed annotation");
			}
			String[] value = annotation.value();
			for(String role : value)
			{
				if(securityContext.isUserInRole(role))
				{
					return;
				}
			}
			if ("guest".equals(securityContext.getUserPrincipal().getName()))
			{
				throw new NotAuthorizedException("WWW-Authenticate");
			}
			else
			{
				throw new ForbiddenException("User does not have the correct roles for endpoint: " + endpoint);
			}
		}
	}
}
