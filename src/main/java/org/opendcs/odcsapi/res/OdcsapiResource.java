package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.beans.TokenBean;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.dao.ApiTsDAO;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.opendcs_dep.PropSpecHelper;
import org.opendcs.odcsapi.opendcs_dep.TestDecoder;
import org.opendcs.odcsapi.sec.Credentials;
import org.opendcs.odcsapi.sec.UserToken;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class OdcsapiResource
{
	public static final String defaultDateFmt = "%Y-%m-%dT%H:%M:%S";
	@Context HttpHeaders httpHeaders;
	
	public OdcsapiResource()
	{
	}
	
	@GET
	@Path("check")
	@Produces(MediaType.APPLICATION_JSON)
	public Response checkToken(@QueryParam("token") String token)
		throws WebAppException
	{
		if (DbInterface.getTokenManager().checkToken(httpHeaders, token))
			return ApiHttpUtil.createResponse("Token Valid");
		else
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, 
				"Missing required token.");
	}
	
	@POST
	@Path("credentials")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCredentials(Credentials credentials)
		throws WebAppException, DbException
	{
		String u = credentials.getUsername();
		String p = credentials.getPassword();
		if (u == null || u.trim().length() == 0 || p == null || p.trim().length() == 0)
		{
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, 
				"Neither username nor password may be null.");
		}
		for(int i=0; i<u.length(); i++)
		{
			char c = u.charAt(i);
			if (!Character.isLetterOrDigit(c) && c != '_' && c != '.')
				throw new WebAppException(ErrorCodes.AUTH_FAILED, 
					"Username may only contain alphanumeric, underscore, or period.");
		}
		for(int i=0; i<p.length(); i++)
		{
			char c = p.charAt(i);
			if (Character.isWhitespace(c) || c == '\'')
				throw new WebAppException(ErrorCodes.AUTH_FAILED,
					"Password may not contain whitespace or quote.");
		}
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface())
		{
			UserToken userToken = DbInterface.getTokenManager().makeToken(credentials, dbi, httpHeaders);
			TokenBean ret = new TokenBean();
			ret.setUsername(userToken.getUsername());
			ret.setToken(userToken.getToken());
			ret.setLastUsed(userToken.getLastUsed());
			return ApiHttpUtil.createResponse(ret);
		}
	}
	
	@GET
	@Path("credentials")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postCredentials()
		throws WebAppException, DbException
	{
		System.out.println("credentials with no args.");
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface())
		{
			UserToken userToken = DbInterface.getTokenManager().makeToken(null, dbi, httpHeaders);
			TokenBean ret = new TokenBean();
			ret.setUsername(userToken.getUsername());
			ret.setToken(userToken.getToken());
			ret.setLastUsed(userToken.getLastUsed());

			//HttpHeaders.AUTHORIZATION
			
			// Place the new token in the return JSON and in the header.
			//return Response.status(HttpServletResponse.SC_OK).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken.getToken())
			//	.entity(ret).build();
			String[] tokenHeader = new String[]{HttpHeaders.AUTHORIZATION, "Bearer " + userToken.getToken()};
			ArrayList<String[]> hdrs = new ArrayList<String[]>();
			hdrs.add(tokenHeader);
			
			return ApiHttpUtil.createResponseWithHeaders(ret, HttpServletResponse.SC_OK, hdrs); 
		}
	}

	
	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTsdbProperties(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getTsdbProperties");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsdbProperties());
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postTsdbProperties(@QueryParam("token") String token, 
		Properties props)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post tsdb_properties");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			dao.setTsdbProperties(props);;
			return ApiHttpUtil.createResponse(props);
		}
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getPropSpecs(@QueryParam("token") String token,
		@QueryParam("class") String className)
		throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).info("getPropSpecs class='" + className + "'");

		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (className == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required class argument.");
		
		return ApiHttpUtil.createResponse(PropSpecHelper.getPropSpecs(className));
	}

	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postDecode(@QueryParam("token") String token,
		@QueryParam("script") String scriptName, DecodeRequest request)
		throws WebAppException, DbException
	{
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("decode message");
		
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		try (DbInterface dbi = new DbInterface())
		{
			return ApiHttpUtil.createResponse(TestDecoder.decodeMessage(request.getRawmsg(), request.getConfig(), 
				scriptName, dbi));
		}
	}

	

}
