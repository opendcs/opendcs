/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

import java.sql.SQLException;
import java.util.logging.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendcs.odcsapi.beans.ApiPresentationGroup;
import org.opendcs.odcsapi.dao.ApiPresentationDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class PresentationResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("presentationrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
 	public Response getPresentationRefs() throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getPresentationRefs");
		try (DbInterface dbi = new DbInterface();
			ApiPresentationDAO dao = new ApiPresentationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getPresentationRefs());
		}
	}

	@GET
	@Path("presentation")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getPresentation(@QueryParam("groupid") Long groupId)
		throws WebAppException, DbException, SQLException
	{
		if (groupId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required groupid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getPresentation id=" + groupId);
		try (DbInterface dbi = new DbInterface();
			ApiPresentationDAO dao = new ApiPresentationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getPresentation(groupId));
		}
	}

	@POST
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postPresentation(ApiPresentationGroup presGrp) throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("post presentation received presentation " + presGrp.getName()
						+ " with ID=" + presGrp.getGroupId());

		try (DbInterface dbi = new DbInterface();
			ApiPresentationDAO dao = new ApiPresentationDAO(dbi))
		{
			dao.writePresentation(presGrp);
			return ApiHttpUtil.createResponse(presGrp);
		}
	}

	@DELETE
	@Path("presentation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deletePresentation(@QueryParam("groupid") Long groupId) throws DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("DELETE presentation received groupid=" + groupId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiPresentationDAO dao = new ApiPresentationDAO(dbi))
		{
			String s = dao.routSpecsUsing(groupId);
			if (s != null)
				return ApiHttpUtil.createResponse("Cannot delete presentation group " + groupId 
						+ " because it is used by the following routing specs: " 
						+ s, ErrorCodes.NOT_ALLOWED);

			dao.deletePresentation(groupId);
			return ApiHttpUtil.createResponse("Presentation Group with ID " + groupId + " deleted");
		}
	}
}
