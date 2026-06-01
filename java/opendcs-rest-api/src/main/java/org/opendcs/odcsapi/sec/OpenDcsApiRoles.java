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

package org.opendcs.odcsapi.sec;

import org.opendcs.odcsapi.util.ApiConstants;

public enum OpenDcsApiRoles
{

	ODCS_API_GUEST(ApiConstants.ODCS_API_GUEST), // Unauthenticated users
	ODCS_API_USER(ApiConstants.ODCS_API_USER), // Authenticated users with some amount of roles
	ODCS_API_ADMIN(ApiConstants.ODCS_API_ADMIN), // Admin users
	ODCS_API_REGISTERED(ApiConstants.ODCS_API_REGISTERED) // Registered users that may or may not have any actual roles yet. Allows access to session and user profile.
	;

	private final String role;

	OpenDcsApiRoles(String role)
	{
		this.role = role;
	}

	public String getRole()
	{
		return role;
	}
}
