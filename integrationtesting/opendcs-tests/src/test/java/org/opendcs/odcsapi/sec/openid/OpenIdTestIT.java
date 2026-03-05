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
import java.util.HashMap;
import java.util.UUID;

import jakarta.ws.rs.core.MediaType;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendcs.authentication.identityprovider.impl.oidc.OidcIdentityProvider;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.extensions.auth.KeyCloakExtension;
import org.opendcs.fixtures.helpers.Constants;
import org.opendcs.odcsapi.res.it.BaseApiIT;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


final class OpenIdTestIT extends BaseApiIT
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	static boolean providerInitialized = false;

	@ConfiguredField
	private OpenDcsDatabase db;

	@BeforeEach
	void setup_provider() throws Exception
	{
		if (!providerInitialized)
		{
			var umDao = db.getDao(UserManagementDao.class).orElseThrow();
			try (var tx = db.newTransaction())
			{
				var config = new HashMap<String, Object>();
				config.put("issuer", KeyCloakExtension.getIssuer());
				config.put("wellKnown", KeyCloakExtension.getOidcWellKnown());
				config.put("clientId", "opendcs");
				config.put("clientSecret", "todo");
				final String redirectUri = RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath + "/oidc-callback";
				config.put("redirectUri", redirectUri);
				var idpIn = new OidcIdentityProvider(null, "test-oidc", null, config);
				var idpOut = umDao.addIdentityProvider(tx, idpIn);
				log.info("New OIDC IDP -> {} {}", idpOut.getId(), idpOut.getName());
				var user = umDao.getUsers(tx, -1, -1).stream().filter(u -> "test_user".equals(u.email)).findFirst().orElseThrow();
				for (var idmp: user.identityProviders)
				{
					log.info("Identity Mapping {} {}", idmp.provider.getName(), idmp.subject);
				}
				var userBuilder = new UserBuilder(user).withIdentityMapping(new IdentityProviderMapping(idpOut, "45ee99c4-3dc8-444d-81d8-2c669a148bff"));
				for (var idmp: userBuilder.build().identityProviders)
				{
					log.info("Identity Mapping {} {}", idmp.provider.getName(), idmp.subject);
				}
				umDao.updateUser(tx, user.id, userBuilder.build());
			}
		}
	}

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

		var loginSession = doAuthCodeLogin(initialSession);
		
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
			.statusCode(is(Response.Status.OK.getStatusCode()))
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


	public Cookie doAuthCodeLogin(Cookie initialSession)
	{
		final String redirectUri = RestAssured.baseURI + ":" + RestAssured.port + "/" + RestAssured.basePath + "/oidc-callback";
		
		final Cookie stateCookie = new Cookie.Builder("state", "test-oidc__" + UUID.randomUUID().toString())
											.setHttpOnly(true)
											.setSameSite("Strict")
											.build();
		var loginSessionPage = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.contentType(ContentType.URLENC)
				.formParam("client_id","opendcs")
				.formParam("scope","openid profile email")
				.formParam("response_type","code")
				.formParam("redirect_uri", redirectUri)
				.formParam("state", stateCookie.getValue())
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
			.cookie(stateCookie)
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

		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.cookie(initialSession)
			.cookie(stateCookie)
		.when()
			.get(callbackUrl)
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.cookie(Constants.JSESSIONID)
			.extract()
			.detailedCookie(Constants.JSESSIONID)
		;
	}

}
