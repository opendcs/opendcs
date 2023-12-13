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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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

import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.dao.ApiRefListDAO;
import org.opendcs.odcsapi.dao.ApiTsDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import ilex.util.IDateFormat;

/**
 * HTTP resources relating to Time Series data and descriptors
 * @author mmaloney
 *
 */
@Path("/")
public class TimeSeriesResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTimeSeriesRefs(@QueryParam("token") String token,
		@QueryParam("active") Boolean activeOnly)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getTimeSeriesRefs");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsRefs(activeOnly != null && activeOnly));
		}
	}

	@GET
	@Path("tsspec")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTimeSeriesSpec(@QueryParam("token") String token,
		@QueryParam("key") Long tsKey)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (tsKey == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required tskey parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getTimeSeriesSpec");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getSpec(tsKey));
		}
	}

	@GET
	@Path("tsdata")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTimeSeriesData(@QueryParam("token") String token,
		@QueryParam("key") Long tsKey, @QueryParam("start") String start, 
		@QueryParam("end") String end)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getTimeSeriesData key=" + tsKey 
			+ ", start=" + start + ", end=" + end + ", token=" + token);
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (tsKey == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required tskey parameter.");
		
		Date dStart = null, dEnd = null;
		if (start != null)
			try { dStart = IDateFormat.parse(start); }
			catch(IllegalArgumentException ex)
			{
				throw new WebAppException(ErrorCodes.MISSING_ID, 
					"Invalid start time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.");
			}
		if (end != null)
		{
			try { dEnd = IDateFormat.parse(end); }
			catch(IllegalArgumentException ex)
			{
				throw new WebAppException(ErrorCodes.MISSING_ID, 
					"Invalid end time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.");
			}

		}

		Logger.getLogger(ApiConstants.loggerName).fine("getTimeSeriesData start=" + dStart + ", end=" + dEnd);

		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsData(tsKey, dStart, dEnd));
		}
	}
	
	@GET
	@Path("intervals")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIntervals(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getIntervals");
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO rlDAO = new ApiRefListDAO(dbi))
		{
			HashMap<Long,ApiInterval> imap = rlDAO.getIntervals();
			return ApiHttpUtil.createResponse(imap.values());
		}
	}

	@POST
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postInterval(@QueryParam("token") String token, ApiInterval intv)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postInterval");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO rlDAO = new ApiRefListDAO(dbi))
		{
			rlDAO.writeInterval(intv);
			return ApiHttpUtil.createResponse(intv);
		}
	}

	@DELETE
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteInterval(@QueryParam("token") String token, 
		@QueryParam("intvid") Long intvId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("deleteInterval id=" + intvId);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO rlDAO = new ApiRefListDAO(dbi))
		{
			rlDAO.deleteInterval(intvId);
			return ApiHttpUtil.createResponse("interval with ID=" + intvId + " deleted");

		}
	}

	@GET
	@Path("tsgrouprefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTsGroupRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getTsGroupRefs");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsGroupRefs());
		}
	}

	@GET
	@Path("tsgroup")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTsGroupRefs(@QueryParam("token") String token,
		@QueryParam("groupid") Long groupId)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getTsGroup");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsGroup(groupId));
		}
	}
	
	@POST
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTsGroup(@QueryParam("token") String token, ApiTsGroup grp)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postTsGroup");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			dao.writeGroup(grp);
			return ApiHttpUtil.createResponse(grp);
		}
	}

	@DELETE
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteTsGroup(@QueryParam("token") String token, 
		@QueryParam("groupid") Long groupId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("delete tsgroup id=" + groupId);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			dao.deleteGroup(groupId);
			return ApiHttpUtil.createResponse("tsgroup with ID=" + groupId + " deleted");
		}
	}
	/*
	@POST
	@Path("tsgroupeval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ArrayList<ApiTimeSeriesIdentifier> evalTsGroup(@QueryParam("token") String token, 
		ApiTsGroup grp)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("POST tsgroupeval");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		CompRunner compRunner = new CompRunner();
		return compRunner.evalGroup(grp, new DbInterface());
	}
    */
}
