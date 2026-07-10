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

package org.opendcs.odcsapi.res;

import org.opendcs.odcsapi.beans.ApiVersion;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.utils.OpenDcsVersion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Exposes the build version and git commit hash of the running REST API so users
 * can confirm which version of the code is actually deployed, independent of the
 * version shown on GitHub.
 */
@Path("/version")
@Tag(name = "Version")
public final class VersionResource extends OpenDcsResource
{
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST, ApiConstants.ODCS_API_REGISTERED,
			ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			description = "Retrieve the build version and git commit hash of the running OpenDCS REST API.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Version retrieved successfully.")
			}
	)
	public Response getVersion()
	{
		ApiVersion version = new ApiVersion(OpenDcsVersion.VERSION, OpenDcsVersion.COMMIT_HASH);
		return Response.ok().entity(version).build();
	}
}
