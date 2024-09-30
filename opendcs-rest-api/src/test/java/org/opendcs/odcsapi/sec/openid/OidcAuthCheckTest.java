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
import java.security.Principal;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.opendcs.odcsapi.sec.openid.OidcAuthCheck.AUTHORIZATION_HEADER;
import static org.opendcs.odcsapi.sec.openid.OidcAuthCheck.BEARER_PREFIX;

final class OidcAuthCheckTest
{

	private void setupDataSource() throws Exception
	{
		Connection mockConn = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		when(mockConn.getMetaData()).thenReturn(metadata);
		when(metadata.getDatabaseProductName()).thenReturn("oracle");
		DataSource dataSource = mock(DataSource.class);
		when(dataSource.getConnection()).thenReturn(mockConn);
		DbInterface.setDataSource(dataSource);
		when(DriverManager.getConnection(any(), any(), any())).thenReturn(mockConn);
		PreparedStatement mockStatement = mock(PreparedStatement.class);
		when(mockConn.prepareStatement(any())).thenReturn(mockStatement);
		ResultSet mockRs = mock(ResultSet.class);
		when(mockStatement.executeQuery()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true, true, false);
		when(mockRs.getString(1)).thenReturn("John Doe", "CCP Proc");
	}

	@Test
	void testServlvetSsoAuthCheck() throws Exception
	{
		try(MockedStatic<DriverManager> ignored = mockStatic(DriverManager.class))
		{
			setupDataSource();
			DbInterface.isCwms = true;
			HttpServletRequest requestMock = mock(HttpServletRequest.class);
			ContainerRequestContext contextMock = mock(ContainerRequestContext.class);
			SecurityContext originalSecurityContext = mock(SecurityContext.class);
			when(contextMock.getSecurityContext()).thenReturn(originalSecurityContext);
			Principal principal = mock(Principal.class);
			when(originalSecurityContext.getUserPrincipal()).thenReturn(principal);
			when(principal.getName()).thenReturn("TEST");
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.jwkset.url", "http://localhost");
			DbInterface.decodesProperties.setProperty("opendcs.rest.api.authorization.jwt.issuer.url", "http://localhost");

			RSAKey rsaJWK = getRsaKey();
			String token = createJwt(rsaJWK);
			when(contextMock.getHeaderString(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + token);
			JWKSet jwkSet = new JWKSet(rsaJWK);
			JWKSource<com.nimbusds.jose.proc.SecurityContext> keySource = (jwkSelector, securityContext) ->
					jwkSet.getKeys();
			OidcAuthCheck authCheck = new OidcAuthCheck(keySource);
			OpenDcsSecurityContext securityContext = authCheck.authorize(contextMock, requestMock);
			assertTrue(securityContext.isUserInRole(OpenDcsApiRoles.ODCS_API_USER.getRole()), "User should be authorized for USER role");
		}
	}

	private String createJwt(RSAKey rsaJWK) throws Exception
	{
		JWSSigner signer = new RSASSASigner(rsaJWK);
		JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
				.issuer("http://localhost")
				.subject("John Doe")
				.claim("scp", "read write")
				.claim("cid", "client-id-123")
				.claim("preferred_username", "JohnDoe.1234567890")
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
