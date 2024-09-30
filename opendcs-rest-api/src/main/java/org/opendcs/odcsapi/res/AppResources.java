/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
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

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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

import org.opendcs.odcsapi.appmon.ApiEventClient;
import org.opendcs.odcsapi.beans.ApiAppEvent;
import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.dao.ApiAppDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.lrgsclient.ClientConnectionCache;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiEnvExpander;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;
import org.opendcs.odcsapi.util.ProcWaiterCallback;
import org.opendcs.odcsapi.util.ProcWaiterThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resources for editing, monitoring, stopping, and starting processes.
 */
@Path("/")
public class AppResources
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AppResources.class);
	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getAppRefs() throws DbException
	{
		LOGGER.trace("Getting App Refs.");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			ArrayList<ApiAppRef> ret = dao.getAppRefs();
			LOGGER.trace("Returning {} apps.", ret.size());
			return ApiHttpUtil.createResponse(ret);
		}
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
 	public Response getApp(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required appid parameter.");
		LOGGER.debug("Getting app with id {}", appId);
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
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postApp(ApiLoadingApp app)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Post app received app {} with id {}", app.getAppName(), app.getAppId());
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
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deletApp(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Delete app received request to delete app with id {}", appId);
		
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
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
 	public Response getAppStat() throws DbException
	{
		LOGGER.debug("Getting app stats");
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAppStatus());
		}
	}


	@GET
	@Path("appevents")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
 	public Response getAppEvents(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Getting app events for app with id {}", appId);
		HttpSession session = request.getSession(true);
		ClientConnectionCache clientConnectionCache = ClientConnectionCache.getInstance();
		Optional<ApiEventClient> cli = clientConnectionCache.getApiEventClient(appId, session.getId());
		ApiAppStatus appStat = null;
		try (DbInterface dbi = new DbInterface();
			ApiAppDAO dao = new ApiAppDAO(dbi))
		{
			ApiEventClient apiEventClient = null;
			appStat = dao.getAppStatus(appId);
			if (appStat.getPid() == null)
			{
				cli.ifPresent(c -> clientConnectionCache.removeApiEventClient(c, session.getId()));
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "appid " + appId 
					+ " (" + appStat.getAppName() + ") is not running.");
			}
			else if (System.currentTimeMillis() - appStat.getHeartbeat().getTime() > 20000L)
			{
				cli.ifPresent(c -> clientConnectionCache.removeApiEventClient(c, session.getId()));
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "appid " + appId 
					+ " (" + appStat.getAppName() + ") is not running (stale heartbeat).");
			}
			else if (!cli.isPresent())
			{
				Integer port = appStat.getEventPort();
				if (port == null)
					return ApiHttpUtil.createResponse(new ArrayList<ApiAppEvent>());
				apiEventClient = new ApiEventClient(appId, appStat.getHostname(), port, appStat.getAppName(), appStat.getPid());
				apiEventClient.connect();
				LOGGER.debug("Connected to {}:{}", appStat.getHostname(), port);
				clientConnectionCache.addApiEventClient(apiEventClient, session.getId());
			}
			else if (appStat.getPid() != null && appStat.getPid() != cli.get().getPid())
			{
				// This means that the app was stopped and restarted since we last checked for events.
				// Close the old client and open a new one with the correct PID.
				cli.ifPresent(c -> clientConnectionCache.removeApiEventClient(c, session.getId()));
				
				Integer port = appStat.getEventPort();
				if (port == null)
					return ApiHttpUtil.createResponse(new ArrayList<ApiAppEvent>()); // app not running
				apiEventClient = new ApiEventClient(appId, appStat.getHostname(), port, appStat.getAppName(), appStat.getPid());
				apiEventClient.connect();
				LOGGER.debug("Connected to {}:{}", appStat.getHostname(), port);
				clientConnectionCache.addApiEventClient(apiEventClient, session.getId());
			}
			if(apiEventClient == null)
			{
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No API Event Client found or created");
			}
			return ApiHttpUtil.createResponse(apiEventClient.getNewEvents());
		}
		catch(ConnectException ex)
		{			
			throw new WebAppException(ErrorCodes.IO_ERROR,
					String.format("Cannot connect to %s.", appStat.getAppName()), ex);
			// NOTE: event client added to user token ONLY if connect succeeds.
		}
		catch(IOException ex)
		{
			cli.ifPresent(c -> clientConnectionCache.removeApiEventClient(c, session.getId()));
			throw new WebAppException(ErrorCodes.IO_ERROR,
					String.format("Event socket to %s closed by app", appStat.getAppId()), ex);
		}
	}
	
	@POST
	@Path("appstart")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postAppStart(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Post for appstart received with appId={}", appId);
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "appId parameter required for this operation.");
		
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
			ProcWaiterCallback pwcb = (procName, obj, exitStatus) ->
					{
						ApiLoadingApp loadingApp1 = (ApiLoadingApp)obj;
						LOGGER.info("App Termination: app {} was terminated with exit status {}",
								loadingApp1.getAppName(), exitStatus);
					};

			ProcWaiterThread.runBackground(ApiEnvExpander.expand(startCmd), "App:" + loadingApp.getAppName(), 
				pwcb, loadingApp);

			return ApiHttpUtil.createResponse("App with ID " + appId + " (" + loadingApp.getAppName() + ") started.");
		}
		catch (IOException ex)
		{
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
					String.format("Error attempting to start appId=%s", appId), ex);
		}
	}

	@POST
	@Path("appstop")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postAppStop(@QueryParam("appid") Long appId)
		throws WebAppException, DbException, SQLException
	{
		LOGGER.debug("Post appstop received on app with id {}", appId);
		if (appId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "appId parameter required for this operation.");
		
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
