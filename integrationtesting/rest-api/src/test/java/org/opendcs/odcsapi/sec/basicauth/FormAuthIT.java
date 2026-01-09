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

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.odcsapi.fixtures.DatabaseContextProvider;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@EnableIfTsDb({"OpenDCS-Postgres"})
@ExtendWith(DatabaseContextProvider.class)
final class FormAuthIT
{

	private SessionFilter sessionFilter;

	@BeforeEach
	void setup()
	{
		sessionFilter = new SessionFilter();
	}

	@TestTemplate
	void testBasicAuthFlow()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.UNAUTHORIZED.getStatusCode()))
		;
		Credentials credentials = new Credentials();
		credentials.setUsername("test_user");
		credentials.setPassword("test_password");
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.body(credentials)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("credentials")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("email", equalTo(credentials.getUsername()))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("email", equalTo(credentials.getUsername()))
		;

	}
}
