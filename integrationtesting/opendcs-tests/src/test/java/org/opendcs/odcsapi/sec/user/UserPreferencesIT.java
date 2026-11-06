/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
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

package org.opendcs.odcsapi.sec.user;

import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.Cookie;
import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.odcsapi.res.it.BaseApiIT;
import org.opendcs.odcsapi.sec.basicauth.Credentials;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@EnableIfTsDb({"OpenDCS-Postgres", "CWMS-Oracle"})
final class UserPreferencesIT extends BaseApiIT
{
	private static final String PREFERENCES = "user/preferences";

	private SessionFilter sessionFilter;
	private Cookie session;

	@BeforeEach
	void login()
	{
		sessionFilter = new SessionFilter();
		Credentials credentials = new Credentials();
		credentials.setUsername("test_user");
		credentials.setPassword("test_password");

		session = given()
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
			.extract().detailedCookie("JSESSIONID")
		;
	}

	@AfterEach
	void removeTestPreferences()
	{
		// test_user is shared with other suites, so leave its profile as it was found.
		patchPreferences("""
				{"colorMode": null, "organization": null}""")
			.statusCode(is(Response.Status.OK.getStatusCode()));
	}

	@Test
	void test_preferences_round_trip()
	{
		patchPreferences("""
				{"colorMode": "dark", "organization": "SPK"}""")
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("colorMode", equalTo("dark"))
			.body("organization", equalTo("SPK"));

		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.cookie(session)
			.filter(sessionFilter)
		.when()
			.get(PREFERENCES)
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("colorMode", equalTo("dark"))
			.body("organization", equalTo("SPK"));

		// The session's principal must be refreshed, not just the database row.
		check()
			.body("preferences.colorMode", equalTo("dark"));

		patchPreferences("""
				{"colorMode": null}""")
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.body("$", not(hasKey("colorMode")))
			.body("organization", equalTo("SPK"));

		check()
			.body("preferences", not(hasKey("colorMode")))
			.body("preferences.organization", equalTo("SPK"));
	}

	@Test
	void test_preferences_too_large_are_rejected()
	{
		patchPreferences("{\"colorMode\": \"" + "x".repeat(4000) + "\"}")
			.statusCode(is(Response.Status.BAD_REQUEST.getStatusCode()));

		check()
			.body("preferences", not(hasKey("colorMode")));
	}

	@Test
	void test_preferences_require_a_session()
	{
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.filter(new SessionFilter())
		.when()
			.get(PREFERENCES)
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.UNAUTHORIZED.getStatusCode()));
	}

	private ValidatableResponse patchPreferences(String body)
	{
		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.contentType(MediaType.APPLICATION_JSON)
			.cookie(session)
			.filter(sessionFilter)
			.body(body)
		.when()
			.patch(PREFERENCES)
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat();
	}

	private ValidatableResponse check()
	{
		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.cookie(session)
			.filter(sessionFilter)
		.when()
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()));
	}
}
