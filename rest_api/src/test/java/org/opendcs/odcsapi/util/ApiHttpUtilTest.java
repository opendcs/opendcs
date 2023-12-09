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

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiHttpUtilTest {
	@Test
	void testCreateResponse() {
		String testMessage = "Test Message";
		int testStatus = Response.Status.OK.getStatusCode();
		try(Response response = ApiHttpUtil.createResponse(testMessage, testStatus)) {
			assertEquals(testStatus, response.getStatus());
			assertEquals(testMessage, response.getEntity());
			assertEquals("max-age=63072000", response.getHeaderString("Strict-Transport-Security"));
			assertEquals("true", response.getHeaderString("Access-Control-Allow-Credentials"));
			assertEquals("Content-Type", response.getHeaderString("Access-Control-Allow-Headers"));
			assertEquals("application/json", response.getHeaderString("Content-Type"));
			assertEquals("nosniff", response.getHeaderString("X-Content-Type-Options"));
		}
	}
}