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

package org.opendcs.odcsapi.fixtures;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SingleSignOn;
import org.apache.catalina.authenticator.SingleSignOnEntry;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.realm.GenericPrincipal;

/**
 * Adds the JSESSIONIDSSO cookie to request and response and caches a test user for
 * mocked single sign on registration
 */
public final class TestSingleSignOn extends SingleSignOn
{
	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException
	{
		request.setUserPrincipal(new GenericPrincipal(System.getProperty("DB_USERNAME"), null, null));
		if(request.getCookies() == null || Arrays.stream(request.getCookies())
				.noneMatch(c -> c.getName().equals(Constants.SINGLE_SIGN_ON_COOKIE)))
		{
			String value = "ABCD";
			Cookie cookie = new Cookie(Constants.SINGLE_SIGN_ON_COOKIE, value);
			cookie.setSecure(true);
			cookie.setHttpOnly(true);
			cookie.setComment("OpenDCS Dev Environment Only");
			request.addCookie(cookie);
			response.addCookie(cookie);
			cache.put(value, new SingleSignOnEntry(() -> System.getProperty("DB_USERNAME"), "test",
					System.getProperty("DB_USERNAME"), System.getProperty("DB_PASSWORD")));
		}
		super.invoke(request, response);
	}

	public void wrappedRegister(String ssoId, Principal principal, String authType)
	{
		register(ssoId, principal, authType, null, null);
	}
}
