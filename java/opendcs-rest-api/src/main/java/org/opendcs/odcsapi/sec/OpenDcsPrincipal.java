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

import java.io.Serializable;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

public final class OpenDcsPrincipal implements Principal, Serializable
{
	public static final String USER_PRINCIPAL_SESSION_ATTRIBUTE = "opendcs-user-principal";
	private static final long serialVersionUID = -2116796045388257540L;
	private final String name;
	private final Set<OpenDcsApiRoles> roles;

	public OpenDcsPrincipal(String name, Set<OpenDcsApiRoles> roles)
	{
		this.name = name;
		this.roles = Collections.unmodifiableSet(roles);
	}

	@Override
	public String getName()
	{
		return name;
	}

	public Set<OpenDcsApiRoles> getRoles()
	{
		return roles;
	}
}
