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

package org.opendcs.odcsapi.sec.openid;

import java.net.URI;
import jakarta.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.extensions.auth.KeyCloakExtension;
import org.opendcs.fixtures.helpers.Constants;
import org.opendcs.odcsapi.res.it.BaseApiIT;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.isNotNull;


final class OpenIdTestIT extends BaseApiIT
{
		

	/**
	 * Auth Code is to get sent to Auth Provider, and return through redirect URI to a "callback function"
	 * This requires a client secret to be setup.
	 */
	@Test
	void test_opendcs_auth_code_flow() throws Exception
	{
		//Initial session should be unauthorized
		var initialSession = 
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.UNAUTHORIZED.getStatusCode()))
			.cookie(Constants.JSESSIONID)
			.extract()
			.detailedCookie(Constants.JSESSIONID)
		;

		final String redirectUri = RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath + "/oidc-callback";

		var loginSessionPage = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.contentType(ContentType.URLENC)
				.formParam("client_id","opendcs")
				.formParam("scope","openid profile email")
				.formParam("response_type","code")
				.formParam("redirect_uri", redirectUri)
			.cookie(initialSession)
		.when()		
			.redirects().follow(true)
			.redirects().max(6)
			.post(URI.create(KeyCloakExtension.getCodeUrl()))
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
		;
		// yank the action URL out of the page 
		var authUrl = loginSessionPage.body().htmlPath().getString("**.find { it.@id == 'kc-form-login'}.@action");

		// manually do the login POST. This *should* redirect us to our oidc-callback
		var callbackUrl = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.contentType(ContentType.URLENC)
			.formParam("username", "test_user")
			.formParam("password", "test_password")
			.cookie(initialSession)
			.cookies(loginSessionPage.detailedCookies())
		.when()
			.redirects().follow(false)
			.post(URI.create(authUrl))
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.FOUND.getStatusCode()))
			.extract()
			.header("Location")
			;

		var loginSession = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.cookie(initialSession)
		.when()
			.get(callbackUrl)
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
			.cookie(Constants.JSESSIONID)
			.extract()
			.detailedCookie(Constants.JSESSIONID)
		;

		assertNotEquals(initialSession.getValue(), loginSession.getValue(), "Session Cookie was not changed after successful login.");

		// Session should now change as the callback endpiont should have been called.
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.cookie(loginSession)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.ok()))
		;

		//Logout and clear session
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.cookie(loginSession)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("logout")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.NO_CONTENT.getStatusCode()))
		;

		//Session should now be cleared
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept(MediaType.APPLICATION_JSON)
			.cookie(loginSession)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(Response.Status.UNAUTHORIZED.getStatusCode()))
		;
	}

}
