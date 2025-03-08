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

package org.opendcs.odcsapi.util;

/**
 * Various constants used by the API.
 */
public final class ApiConstants
{
	public static final String ODCS_API_GUEST = "ODCS_API_GUEST";
	public static final String ODCS_API_USER = "ODCS_API_USER";
	public static final String ODCS_API_ADMIN = "ODCS_API_ADMIN";

	private ApiConstants()
	{
		throw new AssertionError("Utility class");
	}
}
