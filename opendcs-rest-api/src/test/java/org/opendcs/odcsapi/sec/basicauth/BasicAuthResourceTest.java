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

import java.util.Base64;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
final class BasicAuthResourceTest extends JerseyTest
{
	@Mock
	private HttpServletRequest mockRequest;

	@BeforeEach
	void setup()
	{
		DbInterface.isOpenTsdb = true;
		DbInterface.isCwms = false;
		DbInterface.isHdb = false;
	}

	@AfterEach
	void reset()
	{
		DbInterface.isOpenTsdb = true;
		DbInterface.isCwms = false;
		DbInterface.isHdb = false;
	}

	@Override
	protected Application configure()
	{
		return new ResourceConfig(BasicAuthResource.class)
				.register(new AbstractBinder()
				{
					@Override
					protected void configure()
					{
						bind(mockRequest).to(HttpServletRequest.class);
					}
				});
	}

	@Test
	void testCredentialsBodyBadFormat()
	{
		Credentials credentials = new Credentials();
		credentials.setUsername("user");
		try(Response response = target("/credentials").request()
				.post(Entity.entity(credentials, MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing password");
		}
		credentials.setPassword("");
		try(Response response = target("/credentials").request()
				.post(Entity.entity(credentials, MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing password");
		}
		credentials.setUsername(null);
		credentials.setPassword("password");
		try(Response response = target("/credentials").request()
				.post(Entity.entity(credentials, MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing username");
		}
		credentials.setUsername("");
		try(Response response = target("/credentials").request()
				.post(Entity.entity(credentials, MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing username");
		}
	}

	@Test
	void testCredentialsHeaderBadFormat()
	{
		String credentials = Base64.getEncoder().encodeToString("user:password".getBytes());
		try(Response response = target("/credentials").request().header("Authorization", credentials)
				.post(Entity.entity("", MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing 'Basic ' prefix");
		}
		credentials = Base64.getEncoder().encodeToString("userpassword".getBytes());
		try(Response response = target("/credentials").request().header("Authorization", "Basic " + credentials)
				.post(Entity.entity("", MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing colon");
		}
		credentials = Base64.getEncoder().encodeToString("user:".getBytes());
		try(Response response = target("/credentials").request().header("Authorization", "Basic " + credentials)
				.post(Entity.entity("", MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing password");
		}
		credentials = Base64.getEncoder().encodeToString(":password".getBytes());
		try(Response response = target("/credentials").request().header("Authorization", "Basic " + credentials)
				.post(Entity.entity("", MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus(), "Check should return 400 due to missing username");
		}
	}

	@Test
	void testCredentialsNotOpenTsdb()
	{
		DbInterface.isOpenTsdb = false;
		DbInterface.isCwms = true;
		String credentials = Base64.getEncoder().encodeToString("user:password".getBytes());
		try(Response response = target("/credentials").request().header("Authorization", "Basic " + credentials)
				.post(Entity.entity("", MediaType.APPLICATION_JSON)))
		{
			assertEquals(HttpServletResponse.SC_NOT_IMPLEMENTED, response.getStatus(), "Check should return 400 due to missing 'Basic ' prefix");
		}
	}
}
