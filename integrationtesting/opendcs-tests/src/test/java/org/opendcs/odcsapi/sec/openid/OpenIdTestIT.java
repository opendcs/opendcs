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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.extensions.auth.KeyCloakExtension;
import org.opendcs.fixtures.helpers.Constants;
import org.opendcs.odcsapi.res.it.BaseApiIT;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@EnableIfTsDb({"OpenDCS-Postgres"})
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
				var user = umDao.getUsers(tx, -1, -1).stream().filter(u -> "test_user".equals(u.email)).findFirst().orElseThrow();
				var userBuilder = new UserBuilder(user).withIdentityMapping(new IdentityProviderMapping(idpOut, "45ee99c4-3dc8-444d-81d8-2c669a148bff"));
				umDao.updateUser(tx, user.id, userBuilder.build());
				providerInitialized = true;
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


	@Test
	void test_opendcs_auth_code_plus_pkce_flow() throws Exception
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

		var loginSession = doAuthCodePlusPkceLogin(initialSession);
		
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


	private Cookie doAuthCodeLogin(Cookie initialSession)
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


	/**
	 * Handle the flow of AuthCode+PKCE. Note that manually do several OIDC interaction steps here.
	 * That is expected, the API will receive a token directly while the client would handle the login service interaction.
	 * So we need to get the AccessToken JWT and then pass it in to the appropriate api handler to get a session.
	 * 
	 * Additional we will use a localhost redirect in this test, the UI would instead redirect to a specific handler
	 * page that would take care of token acquisition and final "login".
	 * 
	 * @param initialSession
	 * @return
	 */
	private Cookie doAuthCodePlusPkceLogin(Cookie initialSession) throws Exception
	{
		byte[] verifierBytes = new byte[40];
		SecureRandom.getInstanceStrong().nextBytes(verifierBytes);
		Base64.Encoder b64encoder = Base64.getUrlEncoder().withoutPadding();
		final String verifier = b64encoder.encodeToString(verifierBytes);
		final String originalState = UUID.randomUUID().toString();

		MessageDigest md = MessageDigest.getInstance("SHA-256");
		final String challenge = b64encoder.encodeToString(md.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
		

		final String redirectUri = "http://localhost:5000"; // we never directly call this URI so the port here doesn't matter.

		var loginSessionPage = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.queryParam("grant_type", "code")
			.queryParam("client_id", "opendcs")
			.queryParam("scope", "openid profile email")
			.queryParam("response_type", "code")
			.queryParam("code_challenge_method", "S256")
			.queryParam("code_challenge", challenge)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("state", originalState)
		.when()
			.get(KeyCloakExtension.getCodeUrl())
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
			.assertThat()
			.statusCode(Response.Status.OK.getStatusCode())
			.extract()

		;
		
		var authUrl = loginSessionPage.htmlPath().getString("**.find { it.@id == 'kc-form-login'}.@action");
		

		var location = given()
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
			.extract().header("Location")
		;

		log.info("Recevied {}", location);
		final var responseUri = URI.create(location);
		var queryParams = QueryParameters.parse(responseUri.getRawQuery());

		var accessToken = given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.formParam("client_id", "opendcs")
			.formParam("grant_type","authorization_code")
			.formParam("code_verifier", verifier)
			.formParam("scopes", "openid profile email")
			.formParam("redirect_uri", redirectUri)
			.formParam("state", queryParams.get("state").get(0))
			.formParam("session_state", queryParams.get("session_state").get(0))
			.formParam("code", queryParams.get("code").get(0))
			.formParam("response_mode", "fragment")
			.formParam("response_type", "id_token token")
		.when()
			.post(KeyCloakExtension.getTokenUrl())
		.then()
			.assertThat()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.extract()
			.jsonPath()
			.getString("access_token")
			;

		return given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.header("Authorization", "Bearer " + accessToken)
			.cookie(initialSession)
		.when()
			.post("/login_jwt")
		.then()
			.assertThat()
			.log().ifValidationFails(LogDetail.ALL, true)
			.statusCode(is(Response.Status.OK.getStatusCode()))
			.cookie(Constants.JSESSIONID)
		.extract()
		.detailedCookie(Constants.JSESSIONID)
		;
	}

	public static final class Result
	{
        public final String code;
        public final String state;
        public final String sessionState;

        public final String error;
        public final String errorDescription;

        private Result(String code, String state, String sessionState, String error, String errorDescription)
		{
            this.code = code;
            this.state = state;
            this.sessionState = sessionState;
            this.error = error;
            this.errorDescription = errorDescription;
        }

        public static Result success(String code, String state, String sessionState)
		{
            return new Result(code, state, sessionState, null, null);
        }

        public static Result failure(String error, String errorDescription)
		{
            return new Result(null, null, null, error, errorDescription);
        }
		
    }

		/**
	 * Helper class to support creation and query of URL query parameters
	 */
	public static final class QueryParameters
	{	
		private Map<String, List<String>> parameters = new HashMap<>();

		private QueryParameters()
		{

		}

		/**
		 * Retrieve Parameter list
		 * @param parameter
		 * @return List of parameters. If no values are set for the parameter an empty list is returned.
		 */
		public List<String> get(String parameter)
		{
			return parameters.computeIfAbsent(parameter, p -> new ArrayList<>());
		}

		/**
		 * Add an additional value to the parameter
		 * @param parameter
		 * @param value
		 * @return this instance to allow for fluent syntax operations
		 */
		public QueryParameters add(String parameter, String value)
		{
			List<String> values = parameters.computeIfAbsent(parameter, p -> new ArrayList<>());
			values.add(value);
			return this;
		}

		/**
		 * Set given parameter to only the given value. Any previously stored values are lost.
		 * @param parameter
		 * @param value
		 * @return this instance to allow for fluent syntax operations
		 */
		public QueryParameters set(String parameter, String value)
		{
			List<String> values = new ArrayList<>();
			values.add(value);
			parameters.put(parameter, values);
			return this;
		}

		/**
		 * URL encoded query parameters suitable for appending to query. Does not include the initial ?
		 * @return
		 */
		public String encode()
		{
			final ArrayList<String> sets = new ArrayList<>();
			parameters.forEach((k,v) ->
			{
				ArrayList<String> pairs = new ArrayList<>();
				for (String value: v)
				{
					pairs.add(String.format("%s=%s",
											URLEncoder.encode(k, StandardCharsets.UTF_8),
											URLEncoder.encode(value, StandardCharsets.UTF_8)));
				}
				sets.add(String.join("&", pairs));
			});

			return String.join("&", sets);
		}

		/**
		 * Given a URL query string, create a QueryParameters object for additional processing.
		 * @param query
		 * @return
		 */
		public static QueryParameters parse(String query)
		{
			QueryParameters parameters = new QueryParameters();
			for (String pair: query.split("&"))
			{
				String[] kv = pair.split("=", 2); // parameters are *always* seperated by &, but may have embedded =
				if (kv[0].trim().isEmpty())
				{
					continue;
				}
				String parameter = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
				String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : null;
				parameters.parameters.computeIfAbsent(parameter, p -> new ArrayList<>()).add(value);
			}
			return parameters;
		}

		/**
		 * Create a new QueryParameters object.
		 * @return
		 */
		public static QueryParameters empty()
		{
			return parse("");
		}
	}
}
