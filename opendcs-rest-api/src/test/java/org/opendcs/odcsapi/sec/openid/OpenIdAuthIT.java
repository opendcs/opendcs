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

package org.opendcs.odcsapi.sec.openid;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.session.SessionFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opendcs.odcsapi.fixtures.EmbeddedTomcatExtension;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(EmbeddedTomcatExtension.class)
@Tag("integration")
final class OpenIdAuthIT
{

	private JwtVerifier original;
	private SessionFilter sessionFilter;

	@BeforeEach
	void setupMock() throws Exception
	{
		sessionFilter = new SessionFilter();
		original = JwtVerifier.getInstance();
		//Can remove if we get a testcontainers call going with a keycloak/authelia config
		JwtVerifier verifier = mock(JwtVerifier.class);
		JWTClaimsSet mock = mock(JWTClaimsSet.class);
		when(mock.getStringClaim("preferred_username")).thenReturn(System.getProperty("opendcs.db.username"));
		when(verifier.getClaimsSet(any(), any())).thenReturn(mock);
		JwtVerifier.setInstance(verifier);
	}

	@AfterEach
	void cleanupMock()
	{
		JwtVerifier.setInstance(original);
	}

	@Test
	void testOpenIdAuthFlow() throws Exception
	{
		DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.type", "openid");
		//Initial session should be unauthorized
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

		//Check while passing in JWT
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept("application/json")
			.filter(sessionFilter)
			.header("Authorization", "Bearer " + jwt())
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.get("check")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_OK))
		;

		//Session should be cached even without JWT
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

		//Logout and clear session
		given()
			.log().ifValidationFails(LogDetail.ALL, true)
			.accept("application/json")
			.filter(sessionFilter)
		.when()
			.redirects().follow(true)
			.redirects().max(3)
			.delete("logout")
		.then()
			.log().ifValidationFails(LogDetail.ALL, true)
		.assertThat()
			.statusCode(is(HttpServletResponse.SC_NO_CONTENT))
		;

		//Session should now be cleared
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
	}

	private static String jwt() throws Exception
	{
		DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.jwkset.url",
				"http://localhost:8080/realms/cwms/protocol/openid-connect/certs");
		DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.issuer.url",
				"http://localhost:8080/realms/cwms");

		RSAKey rsaJWK = getRsaKey();
		return createJwt(rsaJWK);
	}

	private static String createJwt(RSAKey rsaJWK) throws Exception
	{
		JWSSigner signer = new RSASSASigner(rsaJWK);
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer("http://localhost:8080/realms/cwms")
				.subject("123456-7890-1234-5678-901234567890")
				.claim("scp", "read write")
				.claim("cid", "client-id-123")
				.claim("preferred_username", System.getProperty("opendcs.db.username"))
				.jwtID(UUID.randomUUID().toString())
				.issueTime(Date.from(Instant.now()))
				.expirationTime(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
				.build();
		SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(JOSEObjectType.JWT)
				.keyID(rsaJWK.getKeyID()).build(),
				claimsSet);
		signedJWT.sign(signer);
		return  signedJWT.serialize();
	}

	private static RSAKey getRsaKey() throws NoSuchAlgorithmException
	{
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
				.privateKey(privateKey)
				.keyID(UUID.randomUUID().toString())
				.build();
	}
}
