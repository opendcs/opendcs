/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import decodes.sql.DbKey;
import decodes.tsdb.CompAppInfo;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbCompLock;
import opendcs.dai.LoadingAppDAI;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.opendcs.odcsapi.beans.ApiAppRef;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.beans.ApiLoadingApp;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * Resources for editing, monitoring, stopping, and starting processes.
 */
@Path("/")
public final class AppResources extends OpenDcsResource
{
	private static final String NO_APP_FOUND = "No such app with ID: %s";

	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("apprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieves a list of application references",
			description = "Example:  \n\n    http://localhost:8080/odcsapi/apprefs",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiAppRef.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Loading Application Records"}
	)
	public Response getAppRefs() throws DbException
	{
		try(LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			List<ApiAppRef> ret = dai.listComputationApps(false)
					.stream()
					.map(AppResources::map)
					.collect(Collectors.toList());
			return Response.status(HttpServletResponse.SC_OK)
					.entity(ret).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to retrieve apps", ex);
		}
	}

	static ApiAppRef map(CompAppInfo app)
	{
		ApiAppRef ret = new ApiAppRef();
		ret.setAppId(app.getAppId().getValue());
		ret.setAppName(app.getAppName());
		ret.setAppType(app.getAppType());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		return ret;
	}

	@GET
	@Path("app")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve a Single Application by its ID",
			description = "Example: \n\n    http://localhost:8080/odcsapi/app?appid=4  \n",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiLoadingApp.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required appId parameter"),
					@ApiResponse(responseCode = "404", description = "Not Found - No app found with the given ID"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Loading Application Records"}
	)
	public Response getApp(@Parameter(description = "App ID", required = true, example = "4",
			schema = @Schema(implementation = Long.class))
		@QueryParam("appid") Long appId)
			throws WebAppException, DbException
	{
		if (appId == null)
		{
			throw new MissingParameterException("Missing required appid parameter.");
		}
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			return Response.status(HttpServletResponse.SC_OK)
					.entity(mapLoading(dai.getComputationApp(DbKey.createDbKey(appId)))).build();
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format(NO_APP_FOUND, appId), ex);
		}
		catch (DbIoException ex)
		{
			throw new DbException(String.format("Unable to retrieve requested app with id: %d", appId), ex);
		}
	}

	@POST
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing App",
			description = "The App POST method takes a single DECODES Loading Application in JSON format, "
					+ "as described above for GET.  \n\n"
					+ "For creating a new record, leave appId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the appId that was previously returned. "
					+ "The app in the database is replaced with the one sent.",
			requestBody = @RequestBody(
					description = "Loading App",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiLoadingApp.class),
					examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.AppExamples.BASIC),
							@ExampleObject(name = "New", value = ResourceExamples.AppExamples.NEW),
							@ExampleObject(name = "Update", value = ResourceExamples.AppExamples.UPDATE)
					})
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully stored application",
							content = @Content(schema = @Schema(implementation = ApiLoadingApp.class),
									mediaType = MediaType.APPLICATION_JSON)),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"REST - Loading Application Records"}
	)
	public Response postApp(ApiLoadingApp app)
			throws DbException
	{
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			CompAppInfo compApp = map(app);
			dai.writeComputationApp(compApp);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(compApp))
					.build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to store app", ex);
		}
	}

	static CompAppInfo map(ApiLoadingApp app)
	{
		CompAppInfo ret = new CompAppInfo();
		if (app.getAppId() != null)
		{
			ret.setAppId(DbKey.createDbKey(app.getAppId()));
		}
		else
		{
			ret.setAppId(DbKey.NullKey);
		}
		ret.setAppName(app.getAppName());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		ret.setProperties(app.getProperties());
		ret.setManualEditApp(app.isManualEditingApp());
		String appType = app.getProperties().getProperty("appType");
		if (appType == null)
		{
			ret.setProperty("appType", app.getAppType());
		}
		return ret;
	}

	@DELETE
	@Path("app")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing Loading App",
			description = "Required argument appid must be passed in the URL.  \n\n"
					+ "This operation will fail if the loading application is currently being used by any "
					+ "computations or schedule entries, or if it is currently running and has "
					+ "an active CP_COMP_PROC_LOCK record.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted application"),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required appId parameter"),
					@ApiResponse(responseCode = "404", description = "Not Found - No app found with the given ID"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Loading Application Records"}
	)
	public Response deleteApp(@Parameter(description = "App ID", required = true, example = "4",
			schema = @Schema(implementation = Long.class))
		@QueryParam("appid") Long appId)
			throws WebAppException, DbException
	{
		if (appId == null)
		{
			throw new MissingParameterException("Missing required appid parameter.");
		}

		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			CompAppInfo app = dai.getComputationApp(DbKey.createDbKey(appId));
			if (app == null)
			{
				throw new DbException(String.format(NO_APP_FOUND, appId));
			}
			dai.deleteComputationApp(app);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("appId with ID " + appId + " deleted").build();
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format(NO_APP_FOUND, appId), ex);
		}
		catch (DbIoException | ConstraintException ex)
		{
			throw new DbException(String.format(NO_APP_FOUND, appId), ex);
		}
	}

	@GET
	@Path("appstat")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Returns an array with one element for each application",
			description = "REST - Loading Application Records describes API methods for retrieving and manipulating "
					+ "'Loading Application' records. The concept of a 'Loading App' has been generalized to include "
					+ "any application that is known by the OpenDCS software. \nApplications each have a set of properties. "
					+ "The following properties are relevant to M&C: \n\n"
					+ "•\tstartCmd – A string contains a command used to start the application on this server. "
					+ "Most of the OpenDCS apps use lock records to ensure that only a single instance can run at a time.\n\n"
					+ "•\tMonitor – A Boolean (true/false) value indicating whether this app should listen for "
					+ "'event clients.' The API can act as an event client. Event clients can connect to the app "
					+ "via a socket and pull a list of events generated by the app. This is typically used to provide "
					+ "a scrolling event window.\n\n •\tEventPort – If set, this property determines the port that this "
					+ "app will listen on for event clients. If not set (the usual case), the port is determined by "
					+ "the formula: port = (pid % 10000) + 20000\n\n"
					+ "Example:  \n\n"
					+ "`http://localhost:8080/odcsapi/appstat`\n          \n          \n"
					+ "The returned structure is an array with one element for each application returned by the "
					+ "'GET apprefs' method described in the method GET /apprefs\n\n"
					+ "If an application is currently running, the 'pid' will be the system process ID, "
					+ "and 'heartbeat' will be a valid date/time. Also 'status' will be set to some relevant string "
					+ "for that application. For example, the *compproc app* sets its status to the number of "
					+ "computation runs and errors.\n\n"
					+ "         [\n"
					+ "            {\n"
					+ "              \"appId\": 1,\n"
					+ "              \"appName\": \"decodes\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 4,\n"
					+ "              \"appName\": \"compproc\",\n"
					+ "              \"appType\": \"computationprocess\",\n"
					+ "              \"hostname\": \"mmaloney3.local\",\n"
					+ "              \"pid\": 12176,\n"
					+ "              \"heartbeat\": \"2023-05-25T16:34:18.073Z[UTC]\",\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Cmps: 0/0\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 5,\n"
					+ "              \"appName\": \"compproc_regtest\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 8,\n"
					+ "              \"appName\": \"utility\",\n"
					+ "              \"appType\": \"utility\",\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 18,\n"
					+ "              \"appName\": \"StaleDataChecker\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 19,\n"
					+ "              \"appName\": \"compedit\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 20,\n"
					+ "              \"appName\": \"corrections\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 21,\n"
					+ "              \"appName\": \"limits\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 22,\n"
					+ "              \"appName\": \"statmon\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 23,\n"
					+ "              \"appName\": \"dcpmon\",\n"
					+ "              \"appType\": null,\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 25,\n"
					+ "              \"appName\": \"CompEdit\",\n"
					+ "              \"appType\": \"gui\",\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 26,\n"
					+ "              \"appName\": \"RoutingScheduler\",\n"
					+ "              \"appType\": \"routingscheduler\",\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            },\n"
					+ "            {\n"
					+ "              \"appId\": 27,\n"
					+ "              \"appName\": \"compdepends\",\n"
					+ "              \"appType\": \"compdepends\",\n"
					+ "              \"hostname\": null,\n"
					+ "              \"pid\": null,\n"
					+ "              \"heartbeat\": null,\n"
					+ "              \"eventPort\": null,\n"
					+ "              \"status\": \"Inactive\"\n"
					+ "            }\n"
					+ "          ]\n"
					+ "\n",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved application statistics",
						content = @Content(mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ApiAppStatus.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"OpenDCS Process Monitor and Control (APP)"}
	)
	public Response getAppStat() throws DbException
	{
		List<ApiAppStatus> ret = new ArrayList<>();
		try (LoadingAppDAI dai = getLegacyDatabase().makeLoadingAppDAO())
		{
			for (TsdbCompLock lock : dai.getAllCompProcLocks())
			{
				ret.add(map(dai, lock));
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(ret).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve app status", ex);
		}
	}

	static ApiAppStatus map(LoadingAppDAI dai, TsdbCompLock lock) throws DbIoException
	{
		ApiAppStatus ret = new ApiAppStatus();
		ret.setAppId(lock.getAppId().getValue());
		ret.setAppName(lock.getAppName());
		ret.setHostname(lock.getHost());
		ret.setPid((long) lock.getPID());
		ret.setHeartbeat(lock.getHeartbeat());
		ret.setStatus(lock.getStatus());
		if (dai != null)
		{
			try {
				ApiLoadingApp app = mapLoading(dai.getComputationApp(lock.getAppId()));
				if (app.getProperties() != null && app.getProperties().getProperty("EventPort") != null)
				{
					ret.setEventPort(Integer.parseInt(app.getProperties().getProperty("EventPort")));
				}
				ret.setAppType(app.getAppType());
			}
			catch (DbIoException | NoSuchObjectException | NumberFormatException ex)
			{
				throw new DbIoException("Error mapping app status", ex);
			}
		}
		return ret;
	}

	static ApiLoadingApp mapLoading(CompAppInfo app)
	{
		ApiLoadingApp ret = new ApiLoadingApp();
		ret.setAppId(app.getAppId().getValue());
		ret.setAppName(app.getAppName());
		ret.setComment(app.getComment());
		ret.setLastModified(app.getLastModified());
		ret.setManualEditingApp(app.getManualEditApp());
		ret.setAppType(app.getAppType());
		ret.setProperties(app.getProperties());
		return ret;
	}

}
