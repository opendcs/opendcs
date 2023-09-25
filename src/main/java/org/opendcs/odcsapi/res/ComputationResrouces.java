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

import org.opendcs.odcsapi.beans.ApiCompTestRequest;
import org.opendcs.odcsapi.beans.ApiCompTestResults;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiComputationRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.dao.ApiComputationDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.opendcs_dep.CompRunner;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import jakarta.servlet.http.HttpServletResponse;

@Path("/")
public class ComputationResrouces
{
	@Context HttpHeaders httpHeaders;
	
	@GET
	@Path("computationrefs")
	@Produces(MediaType.APPLICATION_JSON)
 	//public ArrayList<ApiComputationRef> getComputationRefs(@QueryParam("token") String token,
	public Response getComputationRefs(@QueryParam("token") String token,
		@QueryParam("site") String site,
		@QueryParam("algorithm") String algorithm,
		@QueryParam("datatype") String datatype,
		@QueryParam("group") String group,
		@QueryParam("process") String process,
		@QueryParam("enabled") Boolean enabled,
		@QueryParam("interval") String interval)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getComputationRefs");
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			//return Response.ok(dao.getComputationRefs(site, algorithm, datatype, group,
			//	process, enabled, interval)).header("X-Content-Type-Options", "nosniff").build();
			return ApiHttpUtil.createResponse(dao.getComputationRefs(site, algorithm, datatype, group,
					process, enabled, interval));
		}
	}

	@GET
	@Path("computation")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getComputation(
		@QueryParam("computationid") Long compId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (compId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required computationid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getComputation computationid=" + compId);

		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getComputation(compId));
		}
	}

	
	@POST
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postComputation(@QueryParam("token") String token, 
		ApiComputation comp)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post comp received comp " + comp.getName()
			+ " with ID=" + comp.getComputationId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.writeComputation(comp));
		}
		catch(WebAppException e)
		{
			throw e;
		}
	}

	@DELETE
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteComputation(
		@QueryParam("token") String token, 
		@QueryParam("computationid") Long computationId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE computation received computationId=" + computationId + ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			dao.deleteComputation(computationId);
			//return Response.status(HttpServletResponse.SC_OK).entity(
			//		"Computation with ID " + computationId + " deleted").build();
			return ApiHttpUtil.createResponse("Computation with ID " + computationId + " deleted");

		}
	}
	/*
	@POST
	@Path("resolvecomp")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response resolveComp(@QueryParam("token") String token, 
		ApiComputation comp)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("resolve comp " + comp.getName());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface())
		{
			CompRunner compRunner = new CompRunner();
			return ApiHttpUtil.createResponse(compRunner.resolveCompInputs(comp, dbi));
		}
		catch(WebAppException e)
		{
			throw e;
		}
	}
	*/
	/*
	@POST
	@Path("comptest")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response testComp(@QueryParam("token") String token, 
		ApiCompTestRequest req)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("comp test " + req.getComputation().getName());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface())
		{
			CompRunner compRunner = new CompRunner();
			return ApiHttpUtil.createResponse(compRunner.testComp(req, dbi));
		}
	}
	*/
}

