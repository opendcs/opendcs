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

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public final class SessionResource
{

	@Context
	HttpServletRequest request;

	@GET
	@Path("check")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response checkSessionAuthorization()
	{
		//Security filters will ensure this method is only accessible via an authenticated client
		return ApiHttpUtil.createResponse("Token Valid");
	}

	@DELETE
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
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
