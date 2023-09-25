package org.opendcs.odcsapi.res;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

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

import org.opendcs.odcsapi.beans.ApiDacqEvent;
import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiRoutingExecStatus;
import org.opendcs.odcsapi.beans.ApiRoutingRef;
import org.opendcs.odcsapi.beans.ApiRoutingStatus;
import org.opendcs.odcsapi.beans.ApiScheduleEntry;
import org.opendcs.odcsapi.beans.ApiScheduleEntryRef;
import org.opendcs.odcsapi.dao.ApiRoutingDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.UserToken;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;

@Path("/")
public class RoutingResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("routingrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRoutingRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response getRouting(
		@QueryParam("routingid") Long routingId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response postRouting(@QueryParam("token") String token, 
		ApiRouting routing)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post routing received routing " + routing.getName() 
			+ " with ID=" + routing.getRoutingId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
	public Response deleteRouting(
		@QueryParam("token") String token, 
		@QueryParam("routingid") Long routingId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE routing received routingId=" + routingId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
	public Response getScheduleRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
 	public Response getSchedule(
		@QueryParam("scheduleid") Long scheduleId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response postSchedule(@QueryParam("token") String token, 
		ApiScheduleEntry schedule)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post schedule received sched " + schedule.getName() 
			+ " with ID=" + schedule.getSchedEntryId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
	public Response deleteSchedule(
		@QueryParam("token") String token, 
		@QueryParam("scheduleid") Long scheduleId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE schedule received scheduleId=" + scheduleId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
	public Response getRoutingStats(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response getRoutingExecStatus(@QueryParam("token") String token,
		@QueryParam("scheduleentryid") Long scheduleEntryId)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response getDacqEvents(@QueryParam("token") String token,
		@QueryParam("appid") Long appId, @QueryParam("routingexecid") Long routingExecId,
		@QueryParam("platformid") Long platformId, @QueryParam("backlog") String backlog)
		throws WebAppException, DbException
	{
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getDacqEvents");
		try (DbInterface dbi = new DbInterface();
			ApiRoutingDAO dao = new ApiRoutingDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getDacqEvents(appId, routingExecId, platformId, backlog, userToken));
		}
	}
}
