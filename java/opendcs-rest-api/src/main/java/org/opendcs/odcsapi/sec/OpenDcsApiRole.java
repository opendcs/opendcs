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

import org.opendcs.database.model.Role;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * record instead of enum so that additional roles could be created/registered.
 * OpenDcsApiRoles
 * @param role
 * @param description
 */
public record OpenDcsApiRole(String role, String description)
{
	public static final OpenDcsApiRole ODCS_API_GUEST =
		new OpenDcsApiRole(ApiConstants.ODCS_API_GUEST, "Unauthenticated users");
	public static final OpenDcsApiRole ODCS_API_USER =
		new OpenDcsApiRole(ApiConstants.ODCS_API_USER, "Able to perform general configuration operations.");
	public static final OpenDcsApiRole ODCS_API_ADMIN =
		new OpenDcsApiRole(ApiConstants.ODCS_API_ADMIN, "Able to perform administrative actions");
	public static final OpenDcsApiRole ODCS_API_REGISTERED =
		new OpenDcsApiRole(ApiConstants.ODCS_API_REGISTERED,
			"""
		Registered users that may or may not have any actual roles yet.
		Allows access to session and user profile.
		""".stripIndent().trim());

	/**
	 * Either return one of the constants if the name matches or just create a new 
	 * instance with the given information.
	 * @return OpenDcsApiRoles
	 */
	public static OpenDcsApiRole valueOf(Role databaseRole)
	{
		return switch(databaseRole.name())
		{
			case ApiConstants.ODCS_API_GUEST -> ODCS_API_GUEST;
			case ApiConstants.ODCS_API_ADMIN -> ODCS_API_ADMIN;
			case ApiConstants.ODCS_API_USER -> ODCS_API_USER;
			case ApiConstants.ODCS_API_REGISTERED -> ODCS_API_REGISTERED;
			default -> new OpenDcsApiRole(databaseRole.name(), databaseRole.description());
		};
	}
}
