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

import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.dao.ApiSiteDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class SiteResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("siterefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response geSiteRefs()
		throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getSiteRefs");
		try (DbInterface dbi = new DbInterface();
			ApiSiteDAO dao = new ApiSiteDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getSiteRefs());
		}
	}

	@GET
	@Path("site")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response geSiteFull(@QueryParam("siteid") Long siteId)
		throws WebAppException, DbException
	{
		if (siteId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required siteid parameter.");

		Logger.getLogger(ApiConstants.loggerName).fine("getSite id=" + siteId);
		try (DbInterface dbi = new DbInterface();
			ApiSiteDAO dao = new ApiSiteDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getSite(siteId));
		}
	}

	@POST
	@Path("site")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postSite(ApiSite site)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("POST site received site id=" + site.getSiteId());
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiSiteDAO dao = new ApiSiteDAO(dbi))
		{
			dao.writeSite(site);
			String sitePubName = site.getPublicName();
			if (sitePubName == null)
			{
			    sitePubName = "";
			}
			String resp = String.format("{\"status\": 200, \"message\": \"The site (%s) has been saved successfully.\"}", sitePubName);
			return ApiHttpUtil.createResponse(resp);
		}
	}

	@DELETE
	@Path("site")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteSite(@QueryParam("siteid") Long siteId) throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("DELETE site received site id=" + siteId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiSiteDAO siteDAO = new ApiSiteDAO(dbi))
		{
			siteDAO.deleteSite(siteId);
			return ApiHttpUtil.createResponse("ID " + siteId + " deleted");
		}
	}
	

}
