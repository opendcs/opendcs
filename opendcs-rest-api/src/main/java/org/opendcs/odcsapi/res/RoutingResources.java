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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiScheduleEntry;
import org.opendcs.odcsapi.dao.ApiRoutingDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class RoutingResources
{
	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("routingrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getRoutingRefs() throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getRoutingRefs");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getRoutingRefs());
		}
	}

	@GET
	@Path("routing")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getRouting(@QueryParam("routingid") Long routingId)
			throws WebAppException, DbException, SQLException
	{
		
		if (routingId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required routingid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getRouting id=" + routingId);
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getRouting(routingId));
		}
	}

	@POST
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postRouting(ApiRouting routing)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post routing received routing " + routing.getName() 
			+ " with ID=" + routing.getRoutingId());
		
		try (DbInterface dbi = new DbInterface();
				ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			dao.writeRouting(routing);
			return ApiHttpUtil.createResponse(routing);
		}
	}

	@DELETE
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteRouting(@QueryParam("routingid") Long routingId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("DELETE routing received routingId=" + routingId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			dao.deleteRouting(routingId);
			return ApiHttpUtil.createResponse("RoutingSpec with ID " + routingId + " deleted");
		}
	}

	@GET
	@Path("schedulerefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getScheduleRefs()
		throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getScheduleRefs");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getScheduleRefs());
		}
	}

	@GET
	@Path("schedule")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
 	public Response getSchedule(@QueryParam("scheduleid") Long scheduleId)
		throws WebAppException, DbException, SQLException
	{
		if (scheduleId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required scheduleid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getSchedule id=" + scheduleId);
		try (DbInterface dbi = new DbInterface();
				ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getSchedule(scheduleId));
		}
	}

	@POST
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postSchedule(ApiScheduleEntry schedule)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post schedule received sched " + schedule.getName() 
			+ " with ID=" + schedule.getSchedEntryId());
		
		try (DbInterface dbi = new DbInterface();
				ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			dao.writeSchedule(schedule);
			return ApiHttpUtil.createResponse(schedule);
		}
	}

	@DELETE
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteSchedule(@QueryParam("scheduleid") Long scheduleId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName)
				.fine("DELETE schedule received scheduleId=" + scheduleId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
				ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			dao.deleteSchedule(scheduleId);
			return ApiHttpUtil.createResponse("schedulec with ID " + scheduleId + " deleted");
		}
	}


	@GET
	@Path("routingstatus")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getRoutingStats()
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getRoutingStats");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getRsStatus());
		}
	}

	@GET
	@Path("routingexecstatus")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getRoutingExecStatus(@QueryParam("scheduleentryid") Long scheduleEntryId)
		throws WebAppException, DbException
	{
		if (scheduleEntryId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "missing required scheduleentryid argument.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getRoutingExecStatus");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getRoutingExecStatus(scheduleEntryId));
		}
	}

	@GET
	@Path("dacqevents")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response getDacqEvents(@QueryParam("appid") Long appId, @QueryParam("routingexecid") Long routingExecId,
		@QueryParam("platformid") Long platformId, @QueryParam("backlog") String backlog)
		throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getDacqEvents");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			HttpSession session = request.getSession(true);
			return ApiHttpUtil.createResponse(dao.getDacqEvents(appId, routingExecId, platformId, backlog, session));
		}
	}
}
