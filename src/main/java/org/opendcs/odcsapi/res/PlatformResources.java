package org.opendcs.odcsapi.res;

import java.sql.SQLException;
import java.util.ArrayList;
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

import org.opendcs.odcsapi.beans.ApiPlatform;
import org.opendcs.odcsapi.beans.ApiPlatformRef;
import org.opendcs.odcsapi.beans.ApiPlatformStatus;
import org.opendcs.odcsapi.dao.ApiPlatformDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;

@Path("/")
public class PlatformResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("platformrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response gePlatformRefs(
		@QueryParam("token") String token,
		@QueryParam("tmtype") String tmtype
		)
		throws WebAppException, DbException, SQLException
	{
		System.out.println("platformrefs");
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		HashMap<String, ApiPlatformRef> ret = new HashMap<String, ApiPlatformRef>();
		
		Logger.getLogger(ApiConstants.loggerName).fine("getPlatforms, tmtype=" + tmtype);
		try (DbInterface dbi = new DbInterface();
			ApiPlatformDAO platformDAO = new ApiPlatformDAO(dbi))
		{
			ArrayList<ApiPlatformRef> platSpecs = platformDAO.getPlatformRefs(tmtype);
			for(ApiPlatformRef ps : platSpecs)
				ret.put(ps.getName(), ps);
		}
		
		Logger.getLogger(ApiConstants.loggerName).fine("getPlatforms returning " + ret.size() + " objects.");
		
		return ApiHttpUtil.createResponse(ret);
	}
	
	@GET
	@Path("platform")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPlatform(
		@QueryParam("platformid") Long platformId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (platformId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required platformid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getPlatform id=" + platformId);
		try (DbInterface dbi = new DbInterface();
			ApiPlatformDAO dao = new ApiPlatformDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.readPlatform(platformId));
		}
	}
	
	@POST
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postPlatform(@QueryParam("token") String token, 
		ApiPlatform platform)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"post platform received platformId=" + platform.getPlatformId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiPlatformDAO dao = new ApiPlatformDAO(dbi))
		{
			dao.writePlatform(platform);
			return ApiHttpUtil.createResponse(platform);
		}
	}
	
	@DELETE
	@Path("platform")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletePlatform(
		@QueryParam("token") String token, 
		@QueryParam("platformid") Long platformId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE platform received id=" + platformId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiPlatformDAO platDao = new ApiPlatformDAO(dbi))
		{
			platDao.deletePlatform(platformId);
			return ApiHttpUtil.createResponse("Platform with ID " + platformId + " deleted");
		}
	}
	
	@GET
	@Path("platformstat")
	@Produces(MediaType.APPLICATION_JSON)
	public Response gePlatformStats(
		@QueryParam("token") String token,
		@QueryParam("netlistid") Long netlistId
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("gePlatformStats, netlistid=" + netlistId);
		try (DbInterface dbi = new DbInterface();
			ApiPlatformDAO platformDAO = new ApiPlatformDAO(dbi))
		{
			return ApiHttpUtil.createResponse(platformDAO.getPlatformStatus(netlistId));
		}
	}

}
