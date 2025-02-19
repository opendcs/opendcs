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

import java.sql.SQLException;
import java.util.ArrayList;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
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

import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.dao.ApiAppDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources for editing, monitoring, stopping, and starting processes.
 */
@Path("/")
public final class AppResources
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AppResources.class);
	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getAppRefs() throws DbException
	{
		LOGGER.trace("Getting App Refs.");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			ArrayList<ApiAppRef> ret = dao.getAppRefs();
			LOGGER.trace("Returning {} apps.", ret.size());
			return ApiHttpUtil.createResponse(ret);
		}
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
 	public Response getApp(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required appid parameter.");
		LOGGER.debug("Getting app with id {}", appId);
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getApp(appId));
		}
	}

	@POST
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postApp(ApiLoadingApp app)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Post app received app {} with id {}", app.getAppName(), app.getAppId());
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			dao.writeApp(app);
			return ApiHttpUtil.createResponse(app);
		}
	}

	@DELETE
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deletApp(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Delete app received request to delete app with id {}", appId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			dao.deleteApp(appId);
			return ApiHttpUtil.createResponse("appId with ID " + appId + " deleted");
		}
	}

	@GET
	@Path("appstat")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
 	public Response getAppStat() throws DbException
	{
		LOGGER.debug("Getting app stats");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAppStatus());
		}
	}

}
