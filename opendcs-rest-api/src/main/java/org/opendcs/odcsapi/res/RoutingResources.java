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

import ilex.util.Logger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

import decodes.db.DataSource;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.db.RoutingStatus;
import decodes.db.ScheduleEntry;
import decodes.db.ScheduleEntryStatus;
import decodes.db.ValueNotFoundException;
import decodes.polling.DacqEvent;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.opentsdb.Interval;
import org.opendcs.odcsapi.beans.ApiDacqEvent;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Schema;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.opendcs.odcsapi.beans.ApiRouting;
import org.opendcs.odcsapi.beans.ApiRoutingExecStatus;
import org.opendcs.odcsapi.beans.ApiRoutingRef;
import org.opendcs.odcsapi.beans.ApiRoutingStatus;
import org.opendcs.odcsapi.beans.ApiScheduleEntry;
import org.opendcs.odcsapi.beans.ApiScheduleEntryRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

@Path("/")
public final class RoutingResources extends OpenDcsResource
{
	private static final String LAST_DACQ_ATTRIBUTE = "last-dacq-event-id";

	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@GET
	@Path("routingrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "Get Routing References",
			description = "Retrieves a list of all routing references.",
			tags = {"REST - DECODES Routing Spec Records"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiRoutingRef.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getRoutingRefs() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			RoutingSpecList rsList = new RoutingSpecList();
			dbIo.readRoutingSpecList(rsList);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(rsList)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to retrieve routing reference list", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiRoutingRef> map(RoutingSpecList rsList)
	{
		List<ApiRoutingRef> refs = new ArrayList<>();
		rsList.getList().forEach(rs -> {
			ApiRoutingRef ref = new ApiRoutingRef();
			if (rs.getId() != null)
			{
				ref.setRoutingId(rs.getId().getValue());
			}
			else
			{
				ref.setRoutingId(DbKey.NullKey.getValue());
			}
			ref.setName(rs.getName());
			ref.setDestination(rs.consumerArg);
			if (rs.dataSource != null)
			{
				ref.setDataSourceName(rs.dataSource.getName());
			}
			ref.setLastModified(rs.lastModifyTime);
			refs.add(ref);
		});
		return refs;
	}

	@GET
	@Path("routing")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "This method returns a JSON representation of a single routing spec",
			description = "Example: \n\n    http://localhost:8080/odcsapi/routing?routingid=20",
			tags = {"REST - DECODES Routing Spec Records"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved routing spec",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiRouting.class))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid routing ID parameter"),
					@ApiResponse(responseCode = "404", description = "Requested routing spec not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getRouting(@Parameter(description = "Routing Spec ID", required = true, example = "20",
			schema = @Schema(implementation = Long.class))
		@QueryParam("routingid") Long routingId)
			throws WebAppException, DbException
	{

		if (routingId == null)
		{
			throw new MissingParameterException("Missing required routingid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			RoutingSpec spec = new RoutingSpec();
			spec.setId(DbKey.createDbKey(routingId));
			dbIo.readRoutingSpec(spec);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(spec)).build();
		}
		catch(DatabaseException e)
		{
			if (e.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException("RoutingSpec with ID " + routingId + " not found");
			}
			throw new DbException("Unable to retrieve routing spec by ID", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiRouting map(RoutingSpec spec) {
		ApiRouting routing = new ApiRouting();
		if (spec.getId() != null)
		{
			routing.setRoutingId(spec.getId().getValue());
		}
		else
		{
			routing.setRoutingId(DbKey.NullKey.getValue());
		}
		routing.setName(spec.getName());
		routing.setLastModified(spec.lastModifyTime);
		if (spec.outputTimeZoneAbbr != null)
		{
			routing.setOutputTZ(spec.outputTimeZoneAbbr);
		}
		routing.setNetlistNames(new ArrayList<>(spec.networkListNames));
		routing.setOutputFormat(spec.outputFormat);
		routing.setEnableEquations(spec.enableEquations);
		if (spec.dataSource != null)
		{
			routing.setDataSourceId(spec.dataSource.getId().getValue());
			routing.setDataSourceName(spec.dataSource.getName());
		}
		routing.setSince(spec.sinceTime);
		routing.setUntil(spec.untilTime);
		routing.setProperties(spec.getProperties());
		routing.setPresGroupName(spec.presentationGroupName);
		routing.setProduction(spec.isProduction);
		routing.setDestinationArg(spec.consumerArg);
		routing.setDestinationType(spec.consumerType);
		return routing;
	}

	@POST
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Create or Overwrite Existing Routing Spec",
			description = "The POST routing method takes a single DECODES Routing Spec in JSON format, "
					+ "as described above for GET.  \n\nFor creating a new record, "
					+ "leave routingId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the routingId that was previously returned. "
					+ "The routing spec in the database is replaced with the one sent.",

			tags = {"REST - DECODES Routing Spec Records"},
			requestBody = @RequestBody(
					description = "Decodes Routing Spec Object",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiRouting.class),
						examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.RoutingExamples.BASIC),
							@ExampleObject(name = "New", value = ResourceExamples.RoutingExamples.NEW),
							@ExampleObject(name = "Update", value = ResourceExamples.RoutingExamples.UPDATE)
					})
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created or updated the routing",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiRouting.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response postRouting(ApiRouting routing)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			RoutingSpec spec = map(routing);
			dbIo.writeRoutingSpec(spec);
			return Response.status(HttpServletResponse.SC_CREATED).entity(map(spec)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to store routing spec", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static RoutingSpec map(ApiRouting routing) throws DbException
	{
		try
		{
			RoutingSpec spec = new RoutingSpec();
			if (routing.getRoutingId() != null)
			{
				spec.setId(DbKey.createDbKey(routing.getRoutingId()));
			}
			else
			{
				spec.setId(DbKey.NullKey);
			}
			spec.setName(routing.getName());
			spec.usePerformanceMeasurements = false;
			spec.lastModifyTime = routing.getLastModified();
			if (routing.getOutputTZ() != null)
			{
				spec.outputTimeZoneAbbr = routing.getOutputTZ();
				spec.outputTimeZone = TimeZone.getTimeZone(ZoneId.of(routing.getOutputTZ()));
			}
			routing.getNetlistNames().forEach(spec::addNetworkListName);
			spec.outputFormat = routing.getOutputFormat();
			spec.enableEquations = routing.isEnableEquations();
			spec.presentationGroupName = routing.getPresGroupName();
			spec.isProduction = routing.isProduction();
			spec.consumerArg = routing.getDestinationArg();
			spec.consumerType = routing.getDestinationType();
			if (routing.getSince() != null)
			{
				spec.sinceTime = routing.getSince();
			}
			if (routing.getUntil() != null)
			{
				spec.untilTime = routing.getUntil();
			}
			spec.setProperties(routing.getProperties());
			if (routing.getDataSourceId() != null)
			{
				DataSource dataSource = new DataSource();
				if (routing.getDataSourceId() != null)
				{
					dataSource.setId(DbKey.createDbKey(routing.getDataSourceId()));
				}
				else
				{
					dataSource.setId(DbKey.NullKey);
				}
				dataSource.setName(routing.getDataSourceName());
				spec.dataSource = dataSource;
			}
			spec.networkListNames = new Vector<>(routing.getNetlistNames());
			return spec;
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to map routing spec", e);
		}

	}

	@DELETE
	@Path("routing")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Delete Existing Decodes Routing Spec",
			description = "Required argument routingid must be passed in the URL.",
			tags = {"REST - DECODES Routing Spec Records"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted routing"),
					@ApiResponse(responseCode = "400", description = "Missing required routingid parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response deleteRouting(@Parameter(description = "Routing Spec ID", required = true, example = "20",
			schema = @Schema(implementation = Long.class))
		@QueryParam("routingid") Long routingId)
			throws WebAppException, DbException
	{
		if (routingId == null)
		{
			throw new MissingParameterException("Missing required routingid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			RoutingSpec spec = new RoutingSpec();
			spec.setId(DbKey.createDbKey(routingId));
			dbIo.deleteRoutingSpec(spec);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity(String.format("RoutingSpec with ID: %d deleted", routingId)).build();
		}
		catch(DatabaseException e)
		{
			throw new DbException("Unable to delete routing spec", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	@GET
	@Path("schedulerefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "Retrieve all schedule references",
			description = "Example:  \n\n    http://localhost:8080/odcsapi/schedulerefs\n\n"
					+ "The returned structure is:\n  \n**Note**: in the third entry below that appName may be omitted. "
					+ "In the database, a schedule entry may not (yet) be assigned to an application.\n```\n[\n  {\n    "
					+ "\"appName\": \"RoutingScheduler\",\n    \"enabled\": false,\n    "
					+ "\"lastModified\": \"2020-12-15T17:52:13.934Z[UTC]\",\n    \"name\": \"goes1\",\n    "
					+ "\"routingSpecName\": \"goes1\",\n    \"schedEntryId\": 9\n  },\n  {\n    "
					+ "\"appName\": \"RoutingScheduler\",\n    \"enabled\": false,\n    "
					+ "\"lastModified\": \"2020-12-15T17:53:06.043Z[UTC]\",\n    \"name\": \"goes2\",\n    "
					+ "\"routingSpecName\": \"goes2\",\n    \"schedEntryId\": 10\n  },\n  {\n    "
					+ "\"enabled\": false,\n    \"lastModified\": \"2022-03-23T13:54:09.188Z[UTC]\",\n    "
					+ "\"name\": \"no_app_assigned\",\n    \"routingSpecName\": \"polltest\",\n    "
					+ "\"schedEntryId\": 17\n  },\n  {\n    \"appName\": \"RoutingScheduler\",\n    "
					+ "\"enabled\": false,\n    \"lastModified\": \"2022-01-14T14:45:01.336Z[UTC]\",\n    "
					+ "\"name\": \"junk\",\n    \"routingSpecName\": \"polltest\",\n    "
					+ "\"schedEntryId\": 14\n  }\n]\n```",
			tags = {"REST - Schedule Entry Methods"},
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiScheduleEntryRef.class)))
					),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getScheduleRefs()
			throws DbException
	{
		try (ScheduleEntryDAI dai = getLegacyDatabase().makeScheduleEntryDAO())
		{
			List<ScheduleEntry> entries = dai.listScheduleEntries(null);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(entryMap(entries)).build();
		}
		catch(DbIoException e)
		{
			throw new DbException("Unable to retrieve schedule entry ref list", e);
		}
	}

	static List<ApiScheduleEntryRef> entryMap(List<ScheduleEntry> entries)
	{
		List<ApiScheduleEntryRef> refs = new ArrayList<>();
		entries.forEach(entry -> {
			ApiScheduleEntryRef ref = new ApiScheduleEntryRef();
			if (entry.getId() != null)
			{
				ref.setSchedEntryId(entry.getId().getValue());
			}
			else
			{
				ref.setSchedEntryId(DbKey.NullKey.getValue());
			}
			ref.setEnabled(entry.isEnabled());
			ref.setAppName(entry.getLoadingAppName());
			ref.setName(entry.getName());
			ref.setLastModified(entry.getLastModified());
			ref.setRoutingSpecName(entry.getRoutingSpecName());
			refs.add(ref);
		});
		return refs;
	}

	@GET
	@Path("schedule")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "This method returns a JSON representation of a single schedule entry",
			description = "Fetches a specific schedule object based on the provided schedule ID.\n\n"
					+ "Example: \n\n    http://localhost:8080/odcsapi/schedule?scheduleid=21",
			tags = {"REST - Schedule Entry Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiScheduleEntry.class))
					),
					@ApiResponse(responseCode = "400", description = "Missing or invalid schedule ID parameter"),
					@ApiResponse(responseCode = "404", description = "Requested schedule entry not found"),
					@ApiResponse(responseCode = "500", description = "Default error sample response")
			}
	)
	public Response getSchedule(@Parameter(description = "Schedule ID", required = true,
			schema = @Schema(implementation = Long.class, example = "21"))
		@QueryParam("scheduleid") Long scheduleId)
			throws WebAppException, DbException
	{
		if (scheduleId == null)
		{
			throw new MissingParameterException("Missing required scheduleid parameter.");
		}

		try (ScheduleEntryDAI dai = getLegacyDatabase().makeScheduleEntryDAO())
		{
			ScheduleEntry entry = dai.readScheduleEntry(DbKey.createDbKey(scheduleId));
			if (entry == null)
			{
				throw new DatabaseItemNotFoundException("ScheduleEntry with ID " + scheduleId + " not found");
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(entry))
					.build();
		}
		catch(DbIoException e)
		{
			throw new DbException("Unable to retrieve schedule entry by ID", e);
		}
	}

	@POST
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Create or Overwrite Existing Schedule",
			description = "The POST schedule method takes a single DECODES Schedule Entry "
					+ "in JSON format, as described above for GET.\n\n"
					+ "For creating a new record, leave schedEntryId out of the passed data structure.\n\n"
					+ "For overwriting an existing one, provide the schedEntryId that was previously returned. "
					+ "The routing spec in the database is replaced with the one sent.",
			tags = {"REST - Schedule Entry Methods"},
			requestBody = @RequestBody(
					description = "Schedule Object",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiScheduleEntry.class),
					examples = {
						@ExampleObject(name = "Basic", value = ResourceExamples.ScheduleExamples.BASIC),
						@ExampleObject(name = "New", value = ResourceExamples.ScheduleExamples.NEW),
						@ExampleObject(name = "Update", value = ResourceExamples.ScheduleExamples.UPDATE)
					})
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created or updated the schedule",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiScheduleEntry.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response postSchedule(ApiScheduleEntry schedule)
			throws DbException
	{
		try (ScheduleEntryDAI dai = getLegacyDatabase().makeScheduleEntryDAO())
		{
			ScheduleEntry entry = map(schedule);
			dai.writeScheduleEntry(entry);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(entry))
					.build();
		}
		catch (DbIoException e)
		{
			throw new DbException("Unable to store schedule entry", e);
		}
	}

	static ScheduleEntry map(ApiScheduleEntry schedule) throws DbException
	{
		try
		{
			ScheduleEntry entry = new ScheduleEntry(schedule.getName());
			if (schedule.getSchedEntryId() != null)
			{
				entry.setId(DbKey.createDbKey(schedule.getSchedEntryId()));
			}
			else
			{
				entry.setId(DbKey.NullKey);
			}
			entry.setStartTime(schedule.getStartTime());
			entry.setTimezone(schedule.getTimeZone());
			if (schedule.getAppId() != null)
			{
				entry.setLoadingAppId(DbKey.createDbKey(schedule.getAppId()));
				entry.setLoadingAppName(schedule.getAppName());
			}
			if (schedule.getRoutingSpecId() != null)
			{
				entry.setRoutingSpecId(DbKey.createDbKey(schedule.getRoutingSpecId()));
				entry.setRoutingSpecName(schedule.getRoutingSpecName());
			}
			entry.setRunInterval(schedule.getRunInterval());
			entry.setLastModified(schedule.getLastModified());
			return entry;
		}
		catch (DatabaseException e)
		{
			throw new DbException("Unable to map schedule entry", e);
		}
	}

	static ApiScheduleEntry map(ScheduleEntry entry)
	{
		ApiScheduleEntry schedule = new ApiScheduleEntry();
		if (entry.getId() != null)
		{
			schedule.setSchedEntryId(entry.getId().getValue());
		}
		else
		{
			schedule.setSchedEntryId(DbKey.NullKey.getValue());
		}
		schedule.setName(entry.getName());
		schedule.setStartTime(entry.getStartTime());
		schedule.setTimeZone(entry.getTimezone());
		schedule.setEnabled(entry.isEnabled());
		if (entry.getLoadingAppId() != null)
		{
			schedule.setAppId(entry.getLoadingAppId().getValue());
			schedule.setAppName(entry.getLoadingAppName());
		}
		if (entry.getRoutingSpecId() != null)
		{
			schedule.setRoutingSpecId(entry.getRoutingSpecId().getValue());
			schedule.setRoutingSpecName(entry.getRoutingSpecName());
		}
		schedule.setRunInterval(entry.getRunInterval());
		schedule.setLastModified(entry.getLastModified());
		return schedule;
	}

	@DELETE
	@Path("schedule")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Delete Existing Schedule",
			description = "Required argument scheduleid must be passed in the URL.",
			tags = {"REST - Schedule Entry Methods"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted schedule"),
					@ApiResponse(responseCode = "400", description = "Invalid schedule ID provided"),
					@ApiResponse(responseCode = "500", description = "Default error sample response")
			}
	)
	public Response deleteSchedule(@Parameter(description = "Schedule ID", required = true,
			schema = @Schema(implementation = Long.class, example = "21"))
		@QueryParam("scheduleid") Long scheduleId)
			throws WebAppException, DbException
	{
		if (scheduleId == null)
		{
			throw new MissingParameterException("missing required scheduleid argument.");
		}
		try (ScheduleEntryDAI dai = getLegacyDatabase().makeScheduleEntryDAO())
		{
			ScheduleEntry entry = new ScheduleEntry(DbKey.createDbKey(scheduleId));
			dai.deleteScheduleEntry(entry);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("Schedule entry with ID " + scheduleId + " deleted").build();
		}
		catch (DbIoException e)
		{
			throw new DbException(String.format("Unable to delete schedule entry by ID: %s", scheduleId), e);
		}
	}


	@GET
	@Path("routingstatus")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Tag(name = "OpenDCS Process Monitor and Control (Routing)", description = "The following methods allow a user to "
			+ "view the status of all routing specs and to start/stop them.")
	@Operation(
			summary =  "This method allows a developer to implement a web version of the OpenDCS Routing Monitor screen.",
			description = "Sample URL:\n  \n    http://localhost:8080/odcsapi/routingstatus\n  \n"
					+ "The returned data structure is shown below. Note the following:\n  \n"
					+ "* All routing specs are contained in the list regardless of whether they have a "
					+ "schedule entry assigned. No schedule entry is indicated by scheduleEntryId = null.\n  \n"
					+ "* Routing specs with a suffix of '-manual' and the 'manual' attribute set to true indicate "
					+ "that the routing spec was run throught the 'rs' command. Otherwise they were run by a "
					+ "Routing Scheduler from a schedule entry.\n  \n* A routing spec may be run either way. "
					+ "Note that the entry for 'test' and 'test-manual' are the same routing spec. "
					+ "'test' was run from the routing scheduler with scheduleEntryId=43, and "
					+ "'test-manual' was run from the command line 'rs'.\n  \n```\n  [\n    {\n      "
					+ "\"routingSpecId\": 44,\n      \"name\": \"rs-MROI4-ROWI4\",\n      "
					+ "\"scheduleEntryId\": null,\n      \"appId\": null,\n      \"appName\": null,\n      "
					+ "\"runInterval\": null,\n      \"lastActivity\": null,\n      \"lastMsgTime\": null,\n      "
					+ "\"numMessages\": 0,\n      \"numErrors\": 0,\n      \"enabled\": false,\n      "
					+ "\"manual\": false\n    },\n    {\n      \"routingSpecId\": 58,\n      \"name\": \"test\",\n      "
					+ "\"scheduleEntryId\": 43,\n      \"appId\": 26,\n      \"appName\": \"RoutingScheduler\",\n      "
					+ "\"runInterval\": \"5 minute\",\n      \"lastActivity\": \"2023-05-31T18:56:54.364Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2023-05-31T18:56:53.099Z[UTC]\",\n      \"numMessages\": 3362,\n      "
					+ "\"numErrors\": 3362,\n      \"enabled\": true,\n      \"manual\": false\n    },\n    {\n      "
					+ "\"routingSpecId\": 58,\n      \"name\": \"test-manual\",\n      \"scheduleEntryId\": 40,\n      "
					+ "\"appId\": 0,\n      \"appName\": null,\n      \"runInterval\": null,\n      "
					+ "\"lastActivity\": \"2023-05-31T18:37:02.490Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2023-05-31T18:37:02.458Z[UTC]\",\n      \"numMessages\": 5700,\n      "
					+ "\"numErrors\": 5699,\n      \"enabled\": true,\n      \"manual\": true\n    },\n    {\n      "
					+ "\"routingSpecId\": 59,\n      \"name\": \"goes1-manual\",\n      \"scheduleEntryId\": 39,\n      "
					+ "\"appId\": 0,\n      \"appName\": null,\n      \"runInterval\": null,\n      "
					+ "\"lastActivity\": \"2022-12-01T22:19:06.024Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2022-12-01T22:19:05.939Z[UTC]\",\n      \"numMessages\": 9,\n      "
					+ "\"numErrors\": 0,\n      \"enabled\": true,\n      \"manual\": true\n    },\n    {\n      "
					+ "\"routingSpecId\": 63,\n      \"name\": \"periodic-10-min\",\n      "
					+ "\"scheduleEntryId\": 38,\n      \"appId\": 26,\n      \"appName\": \"RoutingScheduler\",\n      "
					+ "\"runInterval\": \"10 minute\",\n      \"lastActivity\": \"2023-05-22T13:28:17.631Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2023-05-22T13:28:12.825Z[UTC]\",\n      \"numMessages\": 8,\n      "
					+ "\"numErrors\": 0,\n      \"enabled\": true,\n      \"manual\": false\n    },\n    {\n      "
					+ "\"routingSpecId\": 63,\n      \"name\": \"periodic-10-minute-manual\",\n      "
					+ "\"scheduleEntryId\": 37,\n      \"appId\": 0,\n      \"appName\": null,\n      "
					+ "\"runInterval\": null,\n      \"lastActivity\": \"2022-12-01T20:37:50.811Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2022-12-01T20:37:50.710Z[UTC]\",\n      \"numMessages\": 6,\n      "
					+ "\"numErrors\": 0,\n      \"enabled\": true,\n      \"manual\": true\n    },\n    {\n      "
					+ "\"routingSpecId\": 65,\n      \"name\": \"last-5-min\",\n      \"scheduleEntryId\": null,\n      "
					+ "\"appId\": null,\n      \"appName\": null,\n      \"runInterval\": null,\n      "
					+ "\"lastActivity\": null,\n      \"lastMsgTime\": null,\n      \"numMessages\": 0,\n      "
					+ "\"numErrors\": 0,\n      \"enabled\": false,\n      \"manual\": false\n    },\n    {\n      "
					+ "\"routingSpecId\": 65,\n      \"name\": \"last-5-min-manual\",\n      "
					+ "\"scheduleEntryId\": 42,\n      \"appId\": 0,\n      \"appName\": null,\n      "
					+ "\"runInterval\": null,\n      \"lastActivity\": \"2023-05-31T18:49:49.453Z[UTC]\",\n      "
					+ "\"lastMsgTime\": \"2023-05-31T18:49:47.902Z[UTC]\",\n      \"numMessages\": 3179,\n      "
					+ "\"numErrors\": 0,\n      \"enabled\": true,\n      \"manual\": true\n    }\n  ]\n\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved routing statistics",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiRoutingStatus.class)))),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"OpenDCS Process Monitor and Control (Routing)"}
	)
	public Response getRoutingStats()
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(dbIo.readRoutingSpecStatus())).build();
		}
		catch (DatabaseException e)
		{
			throw new DbException("Unable to retrieve routing status", e);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiRoutingStatus> map(List<RoutingStatus> entries)
	{
		List<ApiRoutingStatus> refs = new ArrayList<>();
		for (RoutingStatus entry : entries)
		{
			ApiRoutingStatus status = new ApiRoutingStatus();
			status.setEnabled(entry.isEnabled());
			status.setName(entry.getName());
			if (entry.getRoutingSpecId() != null)
			{
				status.setRoutingSpecId(entry.getRoutingSpecId().getValue());
			}
			else
			{
				status.setRoutingSpecId(DbKey.NullKey.getValue());
			}
			status.setLastActivity(entry.getLastActivityTime());
			status.setRunInterval(entry.getRunInterval());
			if (entry.getAppId() != null)
			{
				status.setAppId(entry.getAppId().getValue());
			}
			status.setAppName(entry.getAppName());
			status.setManual(entry.isManual());
			status.setLastMsgTime(entry.getLastMessageTime());
			status.setNumErrors(entry.getNumDecodesErrors());
			status.setNumMessages(entry.getNumMessages());
			if (entry.getScheduleEntryId() != null)
			{
				status.setScheduleEntryId(entry.getScheduleEntryId().getValue());
			}
			else
			{
				status.setScheduleEntryId(DbKey.NullKey.getValue());
			}
			refs.add(status);
		}

		return refs;
	}

	@GET
	@Path("routingexecstatus")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "The GET routingexecstatus method returns all of the executions for the specified schedule entry",
			description = "Sample URL\n  \n      http://localhost:8080/odcsapi/routingexecstatus?scheduleentryid=38\n  \n"
					+ "Note the 'GET routingstatus' method returns a list of routing specs showing a unique "
					+ "scheduleEntryId for each entry. There may be more than one entry for each routing spec because:\n    \n"
					+ "* The same routing spec may be run in multiple schedule entries.\n  \n* A 'manual' routing spec "
					+ "(i.e. run with the 'rs' command) will appear as a separate schedule entry with the 'manual' "
					+ "Boolean set to true.\n  \nThus this method, GET routingexecstatus, takes a scheduleentryid "
					+ "as its argument. It returns all of the executions for the specified schedule entry. "
					+ "The returned data structure appears as follows:\n  \n```\n  [\n    {\n      "
					+ "\"routingExecId\": 568,\n      \"scheduleEntryId\": 38,\n      \"routingSpecId\": 63,\n      "
					+ "\"runStart\": \"2023-06-01T17:20:00.516Z[UTC]\",\n      "
					+ "\"runStop\": \"2023-06-01T17:20:00.526Z[UTC]\",\n      \"numMessages\": 0,\n      "
					+ "\"numErrors\": 0,\n      \"numPlatforms\": 0,\n      \"lastMsgTime\": null,\n      "
					+ "\"lastActivity\": \"2023-06-01T17:20:00.527Z[UTC]\",\n      \"runStatus\": \"ERR-OutputInit\",\n      "
					+ "\"hostname\": \"mmaloney3.local\",\n      \"lastInput\": null,\n      \"lastOutput\": null\n    "
					+ "},\n    {\n      \"routingExecId\": 565,\n      \"scheduleEntryId\": 38,\n      "
					+ "\"routingSpecId\": 63,\n      \"runStart\": \"2023-06-01T17:10:00.841Z[UTC]\",\n      "
					+ "\"runStop\": \"2023-06-01T17:10:00.855Z[UTC]\",\n      \"numMessages\": 0,\n      "
					+ "\"numErrors\": 0,\n      \"numPlatforms\": 0,\n      \"lastMsgTime\": null,\n      "
					+ "\"lastActivity\": \"2023-06-01T17:10:00.855Z[UTC]\",\n      "
					+ "\"runStatus\": \"ERR-OutputInit\",\n      \"hostname\": \"mmaloney3.local\",\n      "
					+ "\"lastInput\": null,\n      \"lastOutput\": null\n    },\n    {\n      "
					+ "\"routingExecId\": 562,\n      \"scheduleEntryId\": 38,\n      \"routingSpecId\": 63,\n      "
					+ "\"runStart\": \"2023-06-01T17:00:00.259Z[UTC]\",\n      "
					+ "\"runStop\": \"2023-06-01T17:00:00.269Z[UTC]\",\n      \"numMessages\": 0,\n      "
					+ "\"numErrors\": 0,\n      \"numPlatforms\": 0,\n      \"lastMsgTime\": null,\n      "
					+ "\"lastActivity\": \"2023-06-01T17:00:00.270Z[UTC]\",\n      "
					+ "\"runStatus\": \"ERR-OutputInit\",\n      \"hostname\": \"mmaloney3.local\",\n      "
					+ "\"lastInput\": null,\n      \"lastOutput\": null\n    }\n  ]\n```\n  \n"
					+ "The entries are sorted in descending order by the runStart time. "
					+ "'runStop' may be null if the execution was halted abnormally or if it is still running. "
					+ "If any messages were processed, the num Messages/Errors/Platforms will be non-zero.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved routing execution status",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiRoutingExecStatus.class)))),
					@ApiResponse(responseCode = "400", description = "Missing or invalid schedule entry ID parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"OpenDCS Process Monitor and Control (Routing)"}
	)
	public Response getRoutingExecStatus(@Parameter(description = "Schedule entry identifier", required = true,
			schema = @Schema(implementation = Long.class), example = "38")
		@QueryParam("scheduleentryid") Long scheduleEntryId)
			throws WebAppException, DbException
	{
		if (scheduleEntryId == null)
		{
			throw new MissingParameterException("Missing required scheduleentryid argument.");
		}

		try (ScheduleEntryDAI dai = getLegacyDatabase().makeScheduleEntryDAO())
		{
			ScheduleEntry entry = new ScheduleEntry(DbKey.createDbKey(scheduleEntryId));
			return Response.status(HttpServletResponse.SC_OK)
					.entity(statusMap(dai.readScheduleStatus(entry))).build();
		}
		catch (DbIoException e)
		{
			throw new DbException("Unable to retrieve routing exec status", e);
		}
	}

	static ArrayList<ApiRoutingExecStatus> statusMap(ArrayList<ScheduleEntryStatus> statuses)
	{
		// Note: Routing spec id is not mapped here!
		ArrayList<ApiRoutingExecStatus> execStatuses = new ArrayList<>();
		for (ScheduleEntryStatus status : statuses)
		{
			ApiRoutingExecStatus execStatus = new ApiRoutingExecStatus();
			if (status.getScheduleEntryId() != null)
			{
				execStatus.setScheduleEntryId(status.getScheduleEntryId().getValue());
			}
			else
			{
				execStatus.setScheduleEntryId(DbKey.NullKey.getValue());
			}
			execStatus.setHostname(status.getHostname());
			execStatus.setNumErrors(status.getNumDecodesErrors());
			execStatus.setRunStatus(status.getRunStatus());
			execStatus.setRunStop(status.getRunStop());
			execStatus.setLastMsgTime(status.getLastMessageTime());
			execStatus.setRunStart(status.getRunStart());
			execStatus.setNumPlatforms(status.getNumPlatforms());
			execStatus.setNumMessages(status.getNumMessages());
			execStatus.setLastActivity(status.getLastModified());
			if (status.getId() != null)
			{
				execStatus.setRoutingExecId(status.getId().getValue());
			}
			else
			{
				execStatus.setRoutingExecId(DbKey.NullKey.getValue());
			}
			execStatuses.add(execStatus);
		}
		return execStatuses;
	}

	@GET
	@Path("dacqevents")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Returns data acquisition events stored in the DACQ_EVENT database table",
			description = "Sample URL:\n  \n    "
					+ "http://localhost:8080/odcsapi/dacqevents?appid=26\n  \n"
					+ "The 'GET dacqevents' method returns events stored in the DACQ_EVENT database table. "
					+ "These are events having to do with data acquisition (DACQ) events. "
					+ "That can be associated with …\n  \n"
					+ "* An execution of a routing spec (pass argument routingexecid)\n  \n"
					+ "* An application (pass argument appid)\n  \n"
					+ "*  A specific platform (pass argument platformid)\n  \n"
					+ "It may contain any of the following additional argument. "
					+ "Each argument refines a filter that determines which events are to be returned:\n  \n"
					+ "*  **appid** (*long integer*): only return events generated by a specific app.\n  \n"
					+ "*  **routingexecid** (*long integer*): only return events generated during a specific "
					+ "execution of a routing spec. (The 'GET routingexecstatus' method will return a list of "
					+ "executions, each with a unique ID.)\n  \n*  **platformid** (*long integer*): only return "
					+ "events generated during the processing of a specific platform.\n  \n"
					+ "*  **backlog** (*string*): either the word 'last' or one of the valid interval names "
					+ "returned in GET intervals (see section 3.4.1). Only events generated since the specified "
					+ "interval are returned. The word 'last' means only return events generated since the last "
					+ "'GET dacqevents' call within this session. It can be used to approximate a real-time stream. \n  \n"
					+ "The returned data looks like this:\n  \n```\n  [\n    {\n      \"eventId\": 181646,\n      "
					+ "\"routingExecId\": 607,\n      \"platformId\": null,\n      "
					+ "\"eventTime\": \"2023-06-08T19:21:15.255Z[UTC]\",\n      "
					+ "\"priority\": \"INFO\",\n      \"appId\": 26,\n      \"appName\": \"RoutingScheduler\",\n      "
					+ "\"subsystem\": null,\n      \"msgRecvTime\": null,\n      "
					+ "\"eventText\": \"RoutingSpec(test) Connected to DDS server at www.covesw.com:-1, "
					+ "username='covetest'\"\n    },\n    {\n      \"eventId\": 181647,\n      "
					+ "\"routingExecId\": 606,\n      \"platformId\": null,\n      "
					+ "\"eventTime\": \"2023-06-08T19:21:15.281Z[UTC]\",\n      \"priority\": \"INFO\",\n      "
					+ "\"appId\": 26,\n      \"appName\": \"RoutingScheduler\",\n      \"subsystem\": null,\n      "
					+ "\"msgRecvTime\": null,\n      \"eventText\": \"RoutingSpec(periodic-10-minute) "
					+ "Connected to DDS server at www.covesw.com:-1, username='covetest'\"\n    },\n    "
					+ "{\n      \"eventId\": 181648,\n      \"routingExecId\": 607,\n      \"platformId\": null,\n      "
					+ "\"eventTime\": \"2023-06-08T19:21:15.284Z[UTC]\",\n      \"priority\": \"INFO\",\n      "
					+ "\"appId\": 26,\n      \"appName\": \"RoutingScheduler\",\n      \"subsystem\": null,\n      "
					+ "\"msgRecvTime\": null,\n      \"eventText\": \"RoutingSpec(test) Purging old DACQ_EVENTs "
					+ "before Sat Jun 03 15:21:15 EDT 2023\"\n    }\n  ]\n\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved data acquisition events",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiDacqEvent.class)))),
					@ApiResponse(responseCode = "400", description = "Invalid input parameters"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"OpenDCS Process Monitor and Control (Routing)"}
	)
	public Response getDacqEvents(@Parameter(description = "Only return events generated by a specific app.", example = "26",
			schema = @Schema(implementation = Long.class))
		@QueryParam("appid") Long appId,
			@Parameter(description = "Only return events generated during a specific execution of a routing spec. " +
					"(The 'GET routingexecstatus' method will return a list of executions, each with a unique ID.)",
					example = "64", schema = @Schema(implementation = Long.class))
		@QueryParam("routingexecid") Long routingExecId,
			@Parameter(description = "Only return events generated during the processing of a specific platform.",
					example = "45", schema = @Schema(implementation = Long.class))
		@QueryParam("platformid") Long platformId,
			@Parameter(description = "Either the word 'last' or one of the valid interval names returned in " +
					"GET intervals (see section 3.4.1). Only events generated since the specified interval " +
					"are returned. The word 'last' means only return events generated since the last " +
					"'GET dacqevents' call within this session. It can be used to approximate a real-time stream.",
					example = "15Minutes", schema = @Schema(implementation = String.class))
		@QueryParam("backlog") String backlog)
			throws DbException, MissingParameterException
	{
		if (appId == null || routingExecId == null || platformId == null)
		{
			StringBuilder parameter = new StringBuilder();
			if (appId == null)
			{
				parameter.append("appid");
			}
			if (routingExecId == null)
			{
				if (parameter.length() > 0)
				{
					parameter.append(", ");
				}
				parameter.append("routingexecid");
			}
			if (platformId == null)
			{
				if (parameter.length() > 0)
				{
					parameter.append(", ");
				}
				parameter.append("platformid");
			}
			throw new MissingParameterException(String.format("Missing required parameter(s): %s", parameter));
		}

		try (DacqEventDAI dai = getLegacyTimeseriesDB().makeDacqEventDAO())
		{
			HttpSession session = request.getSession(true);
			ArrayList<DacqEvent> events = new ArrayList<>();
			Map<String, Object> backlogMap = handleBacklog(backlog, session);
			boolean backLogValid = (boolean) backlogMap.get("backLogValid");
			Long dacqEventId = (Long) backlogMap.get("dacqEventId");
			Long timeInMillis = (Long) backlogMap.get("timeInMillis");
			dai.readEvents(events, DbKey.createDbKey(appId), DbKey.createDbKey(routingExecId),
					DbKey.createDbKey(platformId), backLogValid, dacqEventId, timeInMillis);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(events.stream().map(RoutingResources::map).collect(Collectors.toList())).build();
		}
		catch (DbIoException e)
		{
			throw new DbException("Unable to retrieve dacq events", e);
		}
	}

	Map<String, Object> handleBacklog(String backlog, HttpSession session) throws DbIoException
	{
		Map<String, Object> backlogMap = new HashMap<>();
		Object lastDacqEventId = session.getAttribute(LAST_DACQ_ATTRIBUTE);
		boolean backLogValid = false;
		Long dacqEventId = null;
		Long timeInMillis = null;
		if (backlog != null && !backlog.trim().isEmpty())
		{
			backLogValid = true;
			if (backlog.equalsIgnoreCase("last"))
			{
				if (lastDacqEventId != null)
				{
					dacqEventId = (Long) lastDacqEventId;
				}
			}
			else
			{
				try (IntervalDAI intDai = getLegacyTimeseriesDB().makeIntervalDAO())
				{
					intDai.loadAllIntervals();
					String[] intervalCodes = intDai.getValidIntervalCodes();
					for (String intervalCode : intervalCodes)
					{
						Interval intV = IntervalCodes.getInterval(intervalCode);
						if (intV != null && backlog.equalsIgnoreCase(intV.getName()))
						{
							Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
							cal.setTimeInMillis(System.currentTimeMillis());
							int calConstant = intV.getCalConstant();
							if (calConstant != -1)
							{
								cal.add(calConstant, -intV.getCalMultiplier());
								timeInMillis = cal.getTimeInMillis();
								session.removeAttribute(LAST_DACQ_ATTRIBUTE);
							}
							break;
						}
					}
				}
			}
		}
		backlogMap.put("backLogValid", backLogValid);
		backlogMap.put("dacqEventId", dacqEventId);
		backlogMap.put("timeInMillis", timeInMillis);
		return backlogMap;
	}

	static ApiDacqEvent map(DacqEvent event)
	{
		// The app name is not present in the Toolkit DTO, so it won't be mapped here.
		ApiDacqEvent apiEvent = new ApiDacqEvent();
		apiEvent.setEventId(event.getDacqEventId().getValue());
		apiEvent.setAppId(event.getAppId().getValue());
		apiEvent.setPlatformId(event.getPlatformId().getValue());
		apiEvent.setRoutingExecId(event.getScheduleEntryStatusId().getValue());
		apiEvent.setPriority(Logger.priorityName[event.getEventPriority()]);
		apiEvent.setEventTime(event.getEventTime());
		apiEvent.setEventText(event.getEventText());
		apiEvent.setSubsystem(event.getSubsystem());
		apiEvent.setMsgRecvTime(event.getMsgRecvTime());
		return apiEvent;
	}
}
