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

import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiUnit;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.dao.ApiUnitDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP Resources relating to DataTypes, Engineering Units, and Conversions
 * @author mmaloney
 *
 */
@Path("/")
public class DatatypeUnitResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datatypelist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDataTypeList(@QueryParam("token") String token,
		@QueryParam("standard") String std)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getDataTypeList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getDataTypeList(std));
		}
	}


	@GET
	@Path("unitlist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUnitList(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getUnitList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getUnitList());
		}
	}
	
	@POST
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postEU(@QueryParam("token") String token, 
		@QueryParam("fromabbr") String fromabbr, ApiUnit eu)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postEU abbr=" + eu.getAbbr()
			+ ", fromabbr=" + fromabbr);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.writeEU(eu, fromabbr);
			return ApiHttpUtil.createResponse("{\"message\": \"The Engineering Unit was Saved successfully.\"}");
		}
	}

	@DELETE
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteEU(
		@QueryParam("token") String token, 
		@QueryParam("abbr") String abbr)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE EU abbr=" + abbr);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.deleteEU(abbr);
			return ApiHttpUtil.createResponse("EU with abbr " + abbr + " deleted");
		}
	}

	@GET
	@Path("euconvlist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUnitConvList(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getUnitConvList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getConverterList(false));
		}
	}
	
	@POST
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postEUConv(@QueryParam("token") String token, 
		ApiUnitConverter euc)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postEUConv from=" 
			+ euc.getFromAbbr() + ", to=" + euc.getToAbbr());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.writeEUConv(euc);
			return ApiHttpUtil.createResponse("EUConv Saved");
		}
	}

	@DELETE
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteEUConv(
		@QueryParam("token") String token, 
		@QueryParam("euconvid") Long id)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE EUConverter id=" + id);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.deleteEUConv(id);
			return ApiHttpUtil.createResponse("EUConv with id=" + id + " deleted");
		}
	}
}
