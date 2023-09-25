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

import org.opendcs.odcsapi.beans.ApiAlgorithm;
import org.opendcs.odcsapi.beans.ApiAlgorithmRef;
import org.opendcs.odcsapi.dao.ApiAlgorithmDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;

@Path("/")
public class AlgorithmResrouces
{
	@Context HttpHeaders httpHeaders;
	
	@GET
	@Path("algorithmrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlgorithmRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getAlgorithmRefs");
		try (DbInterface dbi = new DbInterface();
			ApiAlgorithmDAO dao = new ApiAlgorithmDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAlgorithmRefs());
		}
	}

	@GET
	@Path("algorithm")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAlgorithm(
		@QueryParam("algorithmid") Long algoId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (algoId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required algorithmid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getAlgorithm algorithmid=" + algoId);

		try (DbInterface dbi = new DbInterface();
			ApiAlgorithmDAO dao = new ApiAlgorithmDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAlgorithm(algoId));
		}
	}

	
	@POST
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAlgorithm(@QueryParam("token") String token, 
		ApiAlgorithm algo)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post algo received algo " + algo.getName()
			+ " with ID=" + algo.getAlgorithmId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiAlgorithmDAO dao = new ApiAlgorithmDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.writeAlgorithm(algo));
		}
		catch(WebAppException e)
		{
			throw e;
		}
	}

	@DELETE
	@Path("algorithm")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletAlgorithm(
		@QueryParam("token") String token, 
		@QueryParam("algorithmid") Long algorithmId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE algorithm received algorithmId=" + algorithmId + ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiAlgorithmDAO dao = new ApiAlgorithmDAO(dbi))
		{
			dao.deleteAlgorithm(algorithmId);
			return ApiHttpUtil.createResponse("Algorithm with ID " + algorithmId + " deleted");
		}
	}
}
