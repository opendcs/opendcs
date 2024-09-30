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

package org.opendcs.odcsapi.sec;

import java.time.Instant;
import java.util.EnumSet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.NoInteractions;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionResourceTest extends JerseyTest
{
	private HttpSession httpSession;
	private SecurityContext securityContext;
	private HttpServletRequest mockRequest;

	@BeforeEach
	void setup()
	{
		DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.type", "basic");
	}

	@AfterEach
	void clearProperty()
	{
		DbInterface.decodesProperties.remove("opendcs.rest.api.authorization.type");
	}

	@Override
	protected Application configure()
	{
		return new ResourceConfig(SessionResource.class)
				.register(SecurityFilter.class)
				.register(new AbstractBinder()
				{
					@Override
					protected void configure()
					{
						httpSession = mock(HttpSession.class);
						AuthorizationCheck authCheck = mock(AuthorizationCheck.class);
						securityContext = mock(SecurityContext.class);
						when(authCheck.authorize(any(), any())).thenReturn(securityContext);
						mockRequest = mock(HttpServletRequest.class);
						when(mockRequest.getSession(true)).thenReturn(httpSession);
						bind(mockRequest).to(HttpServletRequest.class);
						bind(authCheck).to(AuthorizationCheck.class);
						ServletContext contextMock = mock(ServletContext.class);
						when(contextMock.getInitParameter("opendcs.rest.api.authorization.type")).thenReturn("basic");
						bind(contextMock).to(ServletContext.class);
					}
				});
	}

	@Test
	void testCheckUnauthenticated()
	{
		when(securityContext.isUserInRole(any())).thenReturn(false);
		Response response = target("/check").request().get();
		assertEquals(401, response.getStatus(), "Check should return 401 since user is unauthenticated");
	}

	@Test
	void testChecksAuthenticated()
	{
		when(securityContext.isUserInRole(any())).thenReturn(true);
		OpenDcsPrincipal principal = new OpenDcsPrincipal("unit test", EnumSet.allOf(OpenDcsApiRoles.class));
		when(httpSession.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE)).thenReturn(principal);
		when(httpSession.getAttribute(SecurityFilter.LAST_AUTHORIZATION_CHECK)).thenReturn(Instant.now());
		Response response = target("/check").request().get();
		assertEquals(200, response.getStatus(), "Check should return 401 since user is unauthenticated");
	}

	@Test
	void testLogoutUnauthenticated()
	{
		when(mockRequest.getSession(false)).thenReturn(null);
		Response response = target("/logout").request().delete();
		assertEquals(204, response.getStatus(), "Logout should return 204 even when unauthenticated");
		verify(httpSession, new NoInteractions()).invalidate();
	}

	@Test
	void testLogoutAuthenticated()
	{
		when(mockRequest.getSession(false)).thenReturn(httpSession);
		when(securityContext.isUserInRole(any())).thenReturn(true);
		OpenDcsPrincipal principal = new OpenDcsPrincipal("unit test", EnumSet.allOf(OpenDcsApiRoles.class));
		when(httpSession.getAttribute(OpenDcsPrincipal.USER_PRINCIPAL_SESSION_ATTRIBUTE)).thenReturn(principal);
		when(httpSession.getAttribute(SecurityFilter.LAST_AUTHORIZATION_CHECK)).thenReturn(Instant.now());
		Response response = target("/check").request().get();
		assertEquals(200, response.getStatus(), "Check should return 200 since user is authenticated");
		response = target("/logout").request().delete();
		assertEquals(204, response.getStatus(), "Logout should return 204");
		verify(httpSession).invalidate();
	}
}
