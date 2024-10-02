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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import com.google.auto.service.AutoService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import org.opendcs.odcsapi.dao.ApiAuthorizationDAI;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.sec.OpenDcsApiRoles;
import org.opendcs.odcsapi.sec.OpenDcsPrincipal;
import org.opendcs.odcsapi.sec.OpenDcsSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(AuthorizationCheck.class)
public final class OidcAuthCheck implements AuthorizationCheck
{

	private static final Logger LOGGER = LoggerFactory.getLogger(OidcAuthCheck.class);
	static final String AUTHORIZATION_HEADER = "Authorization";
	static final String BEARER_PREFIX = "Bearer ";
	private final JWKSource<SecurityContext> keySource;

	public OidcAuthCheck()
	{
		this.keySource = setupJwkSource();
	}

	//Used by unit tests
	OidcAuthCheck(JWKSource<SecurityContext> keySource)
	{
		this.keySource = keySource;
	}

	private static JWKSource<SecurityContext> setupJwkSource()
	{
		JWKSource<SecurityContext> keySource = null;
		String property = "opendcs.rest.api.authorization.jwt.jwkset.url";
		try
		{
			String jwkSetUrl = DbInterface.decodesProperties.getProperty(property);
			if(jwkSetUrl == null)
			{
				LOGGER.atWarn().log("Property: " + property + " not set. OpenID Authorization is disabled.");
			}
			else
			{
				keySource = JWKSourceBuilder
						.create(new URL(jwkSetUrl))
						.retrying(true)
						.build();
			}
		}
		catch(MalformedURLException e)
		{
			LOGGER.atWarn().setCause(e).log("Property: " + property + " is invalid. OpenID Authorization is disabled.");
		}
		return keySource;
	}

	@Override
	public OpenDcsSecurityContext authorize(ContainerRequestContext requestContext, HttpServletRequest httpServletRequest)
	{
		String authorizationHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
		try
		{
			String token = authorizationHeader.substring(BEARER_PREFIX.length());
			JWTClaimsSet claimsSet = JwtVerifier.getInstance().getClaimsSet(keySource, token);
			String username = claimsSet.getStringClaim("preferred_username");
			OpenDcsPrincipal principal = createPrincipalFromSubject(username);
			return new OpenDcsSecurityContext(principal, httpServletRequest.isSecure(), BEARER_PREFIX);
		}
		catch(ParseException | JOSEException | BadJOSEException e)
		{
			LOGGER.warn("Token processing error: ", e);
			throw new NotAuthorizedException("Invalid JWT.");
		}
	}

	private static OpenDcsPrincipal createPrincipalFromSubject(String subject)
	{
		try(DbInterface dbInterface = new DbInterface();
			ApiAuthorizationDAI authorizationDao = dbInterface.getDao(ApiAuthorizationDAI.class))
		{
			Set<OpenDcsApiRoles> roles = authorizationDao.getRoles(subject);
			return new OpenDcsPrincipal(subject, roles);
		}
		catch(Exception e)
		{
			throw new ServerErrorException("Error accessing database to determine user roles",
					Response.Status.INTERNAL_SERVER_ERROR, e);
		}
	}

	@Override
	public boolean supports(String type, ContainerRequestContext requestContext)
	{
		String authorizationHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
		return keySource != null && "openid".equals(type) && authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX);
	}
}
