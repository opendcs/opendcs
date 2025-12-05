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

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.util.ApiConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;

@Path("/")
public final class SessionResource
{

	@Context
	HttpServletRequest request;

	@GET
	@Path("check")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Check if session authentication is valid",
			description = "The ‘check’ GET method can be called with a configured session.",
			responses = {
					@ApiResponse(responseCode = "200", description = "If the session is valid," +
							" a successful response will be returned."),
					@ApiResponse(
							responseCode = "410",
							description = "If the session is not valid, HTTP 410 is returned.",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = String.class),
								examples = @ExampleObject(value = "Session Valid"))
					)
			},
			tags = {"REST - Authentication and Authorization"}
	)
	public Response checkSessionAuthorization()
	{
		//Security filters will ensure this method is only accessible via an authenticated client
		return Response.status(HttpServletResponse.SC_OK).entity(new Status("Session Valid"))
			.build();
	}

	@DELETE
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "Remove access tokens and clear the client's session.",
			description = "Session variables for the client will be cleared. The auth token will be invalidated.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Session was cleared.")
			},
			tags = {"REST - Authentication and Authorization"}
	)
	public Response logout()
	{
		HttpSession session = request.getSession(false);
		if(session != null)
		{
			session.invalidate();
		}
		return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
	}
}
