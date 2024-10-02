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

import javax.servlet.http.HttpServletResponse;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.odcsapi.fixtures.EmbeddedTomcatExtension;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ExtendWith(EmbeddedTomcatExtension.class)
@Tag("integration")
final class BasicAuthIT
{

	private SessionFilter sessionFilter;

	@BeforeEach
	void setup()
	{
		sessionFilter = new SessionFilter();
	}

	@Test
	void testBasicAuthFlow()
	{
		DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.type", "basic");
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept("application/json")
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_UNAUTHORIZED))
		;
		Credentials credentials = new Credentials();
		credentials.setUsername(System.getProperty("opendcs.db.username"));
		credentials.setPassword(System.getProperty("opendcs.db.password"));
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept("application/json")
			.contentType("application/json")
			.body(credentials)
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.post("credentials")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
			.body("message", equalTo("Authentication Successful."))
		;

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept("application/json")
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;

	}
}
