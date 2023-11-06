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

import org.opendcs.odcsapi.beans.ApiConfigScript;
import org.opendcs.odcsapi.beans.ApiConfigScriptSensor;
import org.opendcs.odcsapi.beans.ApiPlatformConfig;
import org.opendcs.odcsapi.dao.ApiConfigDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class ConfigResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("configrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("geConfigRefs");
		try (DbInterface dbi = new DbInterface();
			ApiConfigDAO configDAO = new ApiConfigDAO(dbi))
		{
			return ApiHttpUtil.createResponse(configDAO.getConfigRefs());
		}
	}

	@GET
	@Path("config")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfig(
		@QueryParam("configid") Long configId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (configId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required configid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getConfig id=" + configId);
		try (DbInterface dbi = new DbInterface();
			ApiConfigDAO configDAO = new ApiConfigDAO(dbi))
		{
			return ApiHttpUtil.createResponse(configDAO.getConfig(configId));
		}
	}

	@POST
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postConfig(@QueryParam("token") String token, 
			ApiPlatformConfig config)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post config received config " + config.getName() 
			+ " with ID=" + config.getConfigId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("POST config script sensors: ");
		for(ApiConfigScript acs : config.getScripts())
		{
			Logger.getLogger(ApiConstants.loggerName).fine("\tscript " + acs.getName());
			for(ApiConfigScriptSensor acss : acs.getScriptSensors())
			{
				Logger.getLogger(ApiConstants.loggerName).fine("\t\t" + acss.prettyPrint());
			}
		}
		try (DbInterface dbi = new DbInterface();
			ApiConfigDAO configDAO = new ApiConfigDAO(dbi))
		{
			configDAO.writeConfig(config);
			return ApiHttpUtil.createResponse(config);
		}
	}
	
	@DELETE
	@Path("config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteConfig(
		@QueryParam("token") String token, 
		@QueryParam("configid") Long configId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE config received configid=" + configId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiConfigDAO cfgDao = new ApiConfigDAO(dbi))
		{
			if (cfgDao.numPlatformsUsing(configId) > 0)
				return ApiHttpUtil.createResponse(" Cannot delete config with ID " + configId + " because it is used by one or more platforms.", ErrorCodes.NOT_ALLOWED);
				
			cfgDao.deleteConfig(configId);
			return ApiHttpUtil.createResponse("Config with ID " + configId + " deleted");
		}
	}


}
