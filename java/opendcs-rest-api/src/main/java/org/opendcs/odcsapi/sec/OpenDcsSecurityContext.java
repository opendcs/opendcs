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

package org.opendcs.odcsapi.sec;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map.Entry;

import jakarta.ws.rs.core.SecurityContext;

public final class OpenDcsSecurityContext implements SecurityContext
{
	private final boolean secure;
	private final OpenDcsPrincipal principal;
	private final String scheme;

	public OpenDcsSecurityContext(OpenDcsPrincipal principal, boolean secure,
			String scheme)
	{
		this.principal = principal;
		this.secure = secure;
		this.scheme = scheme;
	}

	@Override
	public Principal getUserPrincipal()
	{
		return principal;
	}

	/**
	 * More robust version below should be used.
	 */
	@Override
	public boolean isUserInRole(String orgAndRole)
	{
		return false;
	}

	public boolean isUserInRole(String org, String role)
	{
		var roles = principal.getRoles()
							 .entrySet()
							 .stream()
							 .filter(s -> s.getKey().name().equals(org))
							 .map(Entry::getValue)
							 .findFirst()
							 .orElseGet(() -> new ArrayList<>());
		return roles.stream()
					.anyMatch(e ->
					{
						final String r = e.getRole();
						return r.equals(role);
					});
	}


	@Override
	public boolean isSecure()
	{
		return secure;
	}

	@Override
	public String getAuthenticationScheme()
	{
		return scheme;
	}
}
