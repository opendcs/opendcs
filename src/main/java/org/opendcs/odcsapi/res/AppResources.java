package org.opendcs.odcsapi.res;

import java.io.IOException;
import java.net.ConnectException;
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

import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.appmon.ApiEventClient;
import org.opendcs.odcsapi.beans.ApiAppEvent;
import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.dao.ApiAppDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.UserToken;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiEnvExpander;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ProcWaiterCallback;
import org.opendcs.odcsapi.util.ProcWaiterThread;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Resources for editing, monitoring, stopping, and starting processes.
 */
@Path("/")
public class AppResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAppRefs(@QueryParam("token") String token)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getAppRefs");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			ArrayList<ApiAppRef> ret = dao.getAppRefs();
			Logger.getLogger(ApiConstants.loggerName).fine("Returning " + ret.size() + " apps.");
			return ApiHttpUtil.createResponse(ret);
		}
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
 	public Response getApp(
		@QueryParam("appid") Long appId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required appid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getApp id=" + appId);
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getApp(appId));
		}
	}

	@POST
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postApp(@QueryParam("token") String token, 
			ApiLoadingApp app)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post app received app " + app.getAppName() 
			+ " with ID=" + app.getAppId());
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			dao.writeApp(app);
			return ApiHttpUtil.createResponse(app);
		}
	}

	@DELETE
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deletApp(
		@QueryParam("token") String token, 
		@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE app received appId=" + appId + ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			dao.deleteApp(appId);
			return ApiHttpUtil.createResponse("appId with ID " + appId + " deleted");
		}
	}

	@GET
	@Path("appstat")
	@Produces(MediaType.APPLICATION_JSON)
 	public Response getAppStat(
		@QueryParam("token") String token
		)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		Logger.getLogger(ApiConstants.loggerName).fine("getAppStat");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAppStatus());
		}
	}


	@GET
	@Path("appevents")
	@Produces(MediaType.APPLICATION_JSON)
 	public Response getAppEvents(
		@QueryParam("token") String token, @QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getAppEvents appId=" + appId);
		
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");

		ApiEventClient cli = userToken.getEventClient(appId);
		ApiAppStatus appStat = null;
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			appStat = dao.getAppStatus(appId);
			if (appStat.getPid() == null)
			{
				if (cli != null)
				{
					cli.disconnect();
					userToken.setEventClient(appId, null);
				}
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "appid " + appId 
					+ " (" + appStat.getAppName() + ") is not running.");
			}
			else if (System.currentTimeMillis() - appStat.getHeartbeat().getTime() > 20000L)
			{
				if (cli != null)
				{
					cli.disconnect();
					userToken.setEventClient(appId, null);
				}
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "appid " + appId 
					+ " (" + appStat.getAppName() + ") is not running (stale heartbeat).");

			}
			else if (cli == null)
			{
				Integer port = appStat.getEventPort();
				if (port == null)
					return ApiHttpUtil.createResponse(new ArrayList<ApiAppEvent>());
				cli = new ApiEventClient(appId, appStat.getHostname(), port, appStat.getAppName(), appStat.getPid());
				cli.connect();
System.out.println("Connected to " + appStat.getHostname() + ":" + port);
				userToken.setEventClient(appId, cli);
			}
			else if (appStat.getPid() != null && appStat.getPid() != cli.getPid())
			{
				// This means that the app was stopped and restarted since we last checked for events.
				// Close the old client and open a new one with the correct PID.
				cli.disconnect();
				userToken.setEventClient(appId, null);
				
				Integer port = appStat.getEventPort();
				if (port == null)
					return ApiHttpUtil.createResponse(new ArrayList<ApiAppEvent>()); // app not running
				cli = new ApiEventClient(appId, appStat.getHostname(), port, appStat.getAppName(), appStat.getPid());
				cli.connect();
System.out.println("Connected to " + appStat.getHostname() + ":" + port);
				userToken.setEventClient(appId, cli);
			}
			return ApiHttpUtil.createResponse(cli.getNewEvents());
		}
		catch(ConnectException ex)
		{			
			throw new WebAppException(ErrorCodes.IO_ERROR, "Cannot connect to " + appStat.getAppName() + ".");
			// NOTE: event client added to user token ONLY if connect succeeds.
		}
		catch(IOException ex)
		{
			System.out.print("Error in getAppEvents: " + ex);
			ex.printStackTrace(System.out);
			cli.disconnect();
			userToken.setEventClient(appId, null);
			
			throw new WebAppException(ErrorCodes.IO_ERROR, "Event socket to " + appStat.getAppName() + " closed by app.");
		}
	}
	
	@POST
	@Path("appstart")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAppStart(@QueryParam("token") String token,
			@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post appstart received appId=" + appId);
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "appId parameter required for this operation.");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			// Retrieve ApiLoadingApp and ApiAppStatus
			ApiLoadingApp loadingApp = dao.getApp(appId);
			ApiAppStatus appStat = dao.getAppStatus(appId);
			
			// Error if already running and heartbeat is current
			if (appStat.getPid() != null && appStat.getHeartbeat() != null
			&& (System.currentTimeMillis() - appStat.getHeartbeat().getTime() < 20000L))
				throw new WebAppException(ErrorCodes.NOT_ALLOWED,
					"App id=" + appId + " (" + loadingApp.getAppName() + ") is already running.");
			
			// Error if no "startCmd" property
			String startCmd = ApiPropertiesUtil.getIgnoreCase(loadingApp.getProperties(), "startCmd");
			if (startCmd == null)
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
					"App id=" + appId + " (" + loadingApp.getAppName() + ") has no 'startCmd' property.");

			// ProcWaiterThread runBackground to execute command, use callback.
			ProcWaiterCallback pwcb = 
				new ProcWaiterCallback()
				{
					@Override
					public void procFinished(String procName, Object obj, int exitStatus)
					{
						ApiLoadingApp loadingApp = (ApiLoadingApp)obj;
						Logger.getLogger(ApiConstants.loggerName).info("appTermination: "
							+ "App " + loadingApp.getAppName() + " terminated with exit status"
							+ exitStatus);
					}
				
				};

			ProcWaiterThread.runBackground(ApiEnvExpander.expand(startCmd), "App:" + loadingApp.getAppName(), 
				pwcb, loadingApp);

			return ApiHttpUtil.createResponse("App with ID " + appId + " (" + loadingApp.getAppName() + ") started.");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
				"Error attempting to start appId=" + appId + ": " + ex);
		}
	}

	@POST
	@Path("appstop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postAppStop(@QueryParam("token") String token,
		@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post appstop received appId=" + appId);
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "appId parameter required for this operation.");
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			// Retrieve ApiLoadingApp and ApiAppStatus
			ApiLoadingApp loadingApp = dao.getApp(appId);
			
			ApiAppStatus appStat = dao.getAppStatus(appId);
			
			if (appStat.getPid() == null)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"appId " + appId + "(" + loadingApp.getAppName() + ") not currently running.");
			
			dao.terminateApp(appId);
			
			return ApiHttpUtil.createResponse("App with ID " + appId + " (" + loadingApp.getAppName() + ") terminated.");

		}
	}

}
