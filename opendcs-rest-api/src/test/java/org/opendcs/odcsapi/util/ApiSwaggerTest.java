/*
 *  Copyright 2023 OpenDCS Consortium
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

package org.opendcs.odcsapi.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

//import org.junit.Test;

//import org.junit.jupiter.api.Test;
import javax.ws.rs.core.Application;
import org.mockito.*;
import org.opendcs.odcsapi.res.RestServices;
import org.opendcs.odcsapi.res.SwaggerResources;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiSwaggerTest extends JerseyTest
{
	@Override
	protected Application configure()
	{
		return new ResourceConfig(SwaggerResources.class);
	}

	@Test
	void testCalc() {
		int testVal = 5;
		assertEquals(testVal, 3+3-1);
	}

	@Test
	void testCreateResponse() throws InterruptedException {
		String url = target("swaggerui").getUri().toString();
		System.out.println("------------------------");
		System.out.println("------------------------");
		System.out.println("URL: " + url);
		System.out.println("------------------------");
		System.out.println("------------------------");
		//Thread.sleep(300000);
		Response response = target("/swaggerui").request().get();
		Assertions.assertEquals(200, response.getStatus());
	}
}