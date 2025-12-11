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

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.opendcs.odcsapi.hydrojson.DbInterface;

final class JwtVerifier
{

	private static JwtVerifier instance = new JwtVerifier();

	private JwtVerifier()
	{
		//access through singleton
	}

	static JwtVerifier getInstance()
	{
		return instance;
	}

	//Used for testing only. Can't access without mockito due to it being a final class
	static void setInstance(JwtVerifier instance)
	{
		JwtVerifier.instance = instance;
	}

	JWTClaimsSet getClaimsSet(JWKSource<SecurityContext> keySource, String accessToken)
			throws BadJOSEException, ParseException, JOSEException
	{
		// Nimbus API documentation taken from:
		// https://connect2id.com/products/nimbus-jose-jwt/examples/validating-jwt-access-tokens
		ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
		// Set the required "typ" header "at+jwt" for access tokens
		jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));

		// The expected JWS algorithm of the access tokens (agreed out-of-band)
		JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
		JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, keySource);
		jwtProcessor.setJWSKeySelector(keySelector);
		String issuer = DbInterface.decodesProperties.getProperty("opendcs.rest.api.authorization.jwt.issuer.url");
		jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
				new JWTClaimsSet.Builder().issuer(issuer).build(),
				new HashSet<>(Arrays.asList(
						JWTClaimNames.SUBJECT,
						JWTClaimNames.ISSUED_AT,
						JWTClaimNames.EXPIRATION_TIME,
						JWTClaimNames.JWT_ID))));
		return jwtProcessor.process(accessToken, null);
	}
}
