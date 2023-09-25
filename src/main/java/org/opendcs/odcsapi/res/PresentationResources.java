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

import org.opendcs.odcsapi.beans.ApiPresentationGroup;
import org.opendcs.odcsapi.beans.ApiPresentationRef;
import org.opendcs.odcsapi.dao.ApiPresentationDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;

@Path("/")
public class PresentationResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("presentationrefs")
	@Produces(MediaType.APPLICATION_JSON)
 	public Response getPresentationRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response getPresentation(
		@QueryParam("groupid") Long groupId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
	public Response postPresentation(@QueryParam("token") String token, 
			ApiPresentationGroup presGrp)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post presentation received presentation " + presGrp.getName() 
			+ " with ID=" + presGrp.getGroupId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
	public Response deletePresentation(
		@QueryParam("token") String token, 
		@QueryParam("groupid") Long groupId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE presentation received groupid=" + groupId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
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
