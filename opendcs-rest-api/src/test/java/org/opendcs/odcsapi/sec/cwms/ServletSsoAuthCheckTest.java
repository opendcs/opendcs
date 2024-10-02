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

package org.opendcs.odcsapi.sec.cwms;

import java.security.Principal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.opendcs.odcsapi.sec.cwms.ServletSsoAuthCheck.SESSION_COOKIE_NAME;

final class ServletSsoAuthCheckTest
{

	private void setupDataSource() throws Exception
	{
		Connection mockConn = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		when(mockConn.getMetaData()).thenReturn(metadata);
		when(metadata.getDatabaseProductName()).thenReturn("oracle");
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(mockConn);
		DbInterface.setDataSource(dataSource);
		when(DriverManager.getConnection(any(), any(), any())).thenReturn(mockConn);
		PreparedStatement mockStatement = mock(PreparedStatement.class);
		when(mockConn.prepareStatement(any())).thenReturn(mockStatement);
		ResultSet mockRs = mock(ResultSet.class);
		when(mockStatement.executeQuery()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true, false);
		when(mockRs.getString(1)).thenReturn("CCP Proc");
	}

	@Test
	void testServlvetSsoAuthCheck() throws Exception
	{
		try(MockedStatic<DriverManager> ignored = mockStatic(DriverManager.class))
		{
			setupDataSource();
			DbInterface.isCwms = true;
			HttpServletRequest requestMock = mock(HttpServletRequest.class);
			ContainerRequestContext contextMock = mock(ContainerRequestContext.class);
			SecurityContext originalSecurityContext = mock(SecurityContext.class);
			when(contextMock.getSecurityContext()).thenReturn(originalSecurityContext);
			Principal principal = mock(Principal.class);
			when(originalSecurityContext.getUserPrincipal()).thenReturn(principal);
			when(principal.getName()).thenReturn("TEST");
			when(contextMock.getCookies()).thenReturn(Collections.singletonMap(SESSION_COOKIE_NAME, new Cookie(SESSION_COOKIE_NAME, "TEST")));
			ServletSsoAuthCheck servletSsoAuthCheck = new ServletSsoAuthCheck();
			OpenDcsSecurityContext securityContext = servletSsoAuthCheck.authorize(contextMock, requestMock);
			assertTrue(securityContext.isUserInRole(OpenDcsApiRoles.ODCS_API_USER.getRole()), "User should be authorized for USER role");
		}
	}
}
