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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.sql.DbKey;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ComputationExecution;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ProgressListener;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.odcsapi.beans.ApiCompParm;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiComputationRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static java.util.stream.Collectors.toList;

@Path("/")
public final class ComputationResources extends OpenDcsResource
{
	private static final Logger log = LoggerFactory.getLogger(ComputationResources.class);
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("computationrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve Computation References",
			description = "Example:  \n\n    http://localhost:8080/odcsapi/computationrefs",
			tags = {"REST - Computation Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiComputationRef.class)))),
					@ApiResponse(responseCode = "404", description = "No computations found matching the filter criteria"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
		)
	public Response getComputationRefs(
			@Parameter(schema = @Schema(implementation = Long.class),
					description = "Site ID to filter on") @QueryParam("site") Long siteId,
			@Parameter(schema = @Schema(implementation = Long.class),
					description = "Algorithm ID to filter on") @QueryParam("algorithm") Long algorithmId,
			@Parameter(schema = @Schema(implementation = Long.class),
					description = "Datatype ID to filter on") @QueryParam("datatype") Long dataTypeId,
			@Parameter(schema = @Schema(implementation = Long.class),
					description = "Group ID to filter on") @QueryParam("group") Long groupId,
			@Parameter(schema = @Schema(implementation = Long.class),
					description = "Process ID to filter on") @QueryParam("process") Long processId,
			@Parameter(schema = @Schema(implementation = Boolean.class),
					description = "Whether to filter only enabled computations") @QueryParam("enabled") Boolean enabled,
			@Parameter(schema = @Schema(implementation = String.class),
					description = "Interval code to filter on") @QueryParam("interval") String interval)
			throws DbException
	{
		try (ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO())
		{
			CompFilter compFilter = new CompFilter();
			if (dataTypeId != null)
			{
				compFilter.setDataTypeId(DbKey.createDbKey(dataTypeId));
			}
			if (groupId != null)
			{
				compFilter.setGroupId(DbKey.createDbKey(groupId));
			}
			if (processId != null)
			{
				compFilter.setProcessId(DbKey.createDbKey(processId));
			}
			if (siteId != null)
			{
				compFilter.setSiteId(DbKey.createDbKey(siteId));
			}
			if (interval != null)
			{
				compFilter.setIntervalCode(interval);
			}
			List<ApiComputationRef> computationRefs = map(dai.listComps(compFilter::passes));
			return Response.ok().entity(computationRefs).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to retrieve computation references", ex);
		}
	}

	static ArrayList<ApiComputationRef> map(List<DbComputation> computations)
	{
		ArrayList<ApiComputationRef> ret = new ArrayList<>();
		for (DbComputation comp : computations)
		{
			ApiComputationRef ref = new ApiComputationRef();
			ref.setComputationId(comp.getId().getValue());
			if (comp.getAlgorithmId() != null)
			{
				ref.setAlgorithmId(comp.getAlgorithmId().getValue());
			}
			else
			{
				ref.setAlgorithmId(DbKey.NullKey.getValue());
			}
			ref.setAlgorithmName(comp.getAlgorithmName());
			ref.setName(comp.getName());
			ref.setEnabled(comp.isEnabled());
			ref.setDescription(comp.getComment());
			ref.setProcessName(comp.getApplicationName());
			if (comp.getAppId() != null)
			{
				ref.setProcessId(comp.getAppId().getValue());
			}
			else
			{
				ref.setProcessId(DbKey.NullKey.getValue());
			}
			ret.add(ref);
		}
		return ret;
	}

	@GET
	@Path("computation")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Retrieve Computation by its ID",
			description = "Example: \n\n    http://localhost:8080/odcsapi/computation?computationid=4",
			tags = {"REST - Computation Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiComputation.class))),
					@ApiResponse(responseCode = "400", description = "Missing required computationid parameter"),
					@ApiResponse(responseCode = "404", description = "Computation with the specified ID not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response getComputation(@Parameter(required = true, description = "Unique Computation ID",
			schema = @Schema(implementation = Long.class, example = "4"))
		@QueryParam("computationid") Long compId)
			throws WebAppException, DbException
	{
		if (compId == null)
		{
			throw new MissingParameterException("Missing required computationid parameter.");
		}

		try (ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO())
		{
			return Response.ok()
					.entity(map(dai.getComputationById(DbKey.createDbKey(compId)))).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException(String.format("Unable to retrieve computation by ID: %s", compId), ex);
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format("Computation with ID %s not found", compId), ex);
		}
	}

	static ApiComputation map(DbComputation comp)
	{
		ApiComputation ret = new ApiComputation();
		if (comp.getId() != null)
		{
			ret.setComputationId(comp.getId().getValue());
		}
		else
		{
			ret.setComputationId(DbKey.NullKey.getValue());
		}
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithmId(comp.getAlgorithmId().getValue());
		}
		else
		{
			ret.setAlgorithmId(DbKey.NullKey.getValue());
		}
		ret.setComment(comp.getComment());
		if (comp.getAppId() != null)
		{
			ret.setAppId(comp.getAppId().getValue());
		}
		else
		{
			ret.setAppId(DbKey.NullKey.getValue());
		}
		ret.setEnabled(comp.isEnabled());
		ret.setEffectiveEndDate(comp.getValidEnd());
		ret.setEffectiveStartDate(comp.getValidStart());
		ret.setAlgorithmName(comp.getAlgorithmName());
		ret.setApplicationName(comp.getApplicationName());
		ret.setGroupName(comp.getGroupName());
		ret.setName(comp.getName());
		ret.setLastModified(comp.getLastModified());
		if (comp.getGroupId() != null)
		{
			ret.setGroupId(comp.getGroupId().getValue());
		}
		else
		{
			ret.setGroupId(DbKey.NullKey.getValue());
		}
		ret.setProps(comp.getProperties());
		ret.setParmList(new ArrayList<>(comp.getParmList()
				.stream()
				.map(ComputationResources::map)
				.collect(toList())));
		return ret;
	}

	static ApiCompParm map(DbCompParm parm)
	{
		ApiCompParm ret = new ApiCompParm();
		if (parm.getDataType() != null)
		{
			ret.setDataType(parm.getDataType().getDisplayName());
		}
		ret.setInterval(parm.getInterval());
		if (parm.getSiteName() != null)
		{
			ret.setSiteName(parm.getSiteName().getDisplayName());
		}
		if (parm.getSiteId() != null)
		{
			ret.setSiteId(parm.getSiteId().getValue());
		}
		else
		{
			ret.setSiteId(DbKey.NullKey.getValue());
		}
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		ret.setAlgoParmType(parm.getAlgoParmType());
		ret.setAlgoRoleName(parm.getRoleName());
		ret.setDuration(parm.getDuration());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		if (parm.getDataTypeId() != null)
		{
			ret.setDataTypeId(parm.getDataTypeId().getValue());
		}
		else
		{
			ret.setDataTypeId(DbKey.NullKey.getValue());
		}
		ret.setDeltaTUnits(parm.getDeltaTUnits());
		ret.setVersion(parm.getVersion());
		ret.setModelId(parm.getModelId());
		ret.setTableSelector(parm.getTableSelector());
		ret.setParamType(parm.getParamType());
		return ret;
	}

	@POST
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing OpenDCS Computation",
			description = "The Computation POST method takes a single OpenDCS Computation Record in JSON format,"
					+ " as described above for GET.  \n\n"
					+ "For creating a new record, leave computationId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the computationId that was previously returned. "
					+ "The computation in the database is replaced with the one sent.",
			tags = {"REST - Computation Methods"},
			requestBody = @RequestBody(
					description = "Computation",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiComputation.class),
						examples = {
								@ExampleObject(name = "Basic", value = ResourceExamples.ComputationExamples.BASIC),
								@ExampleObject(name = "New", value = ResourceExamples.ComputationExamples.NEW),
								@ExampleObject(name = "Update", value = ResourceExamples.ComputationExamples.UPDATE)
						}
					)
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully stored computation",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiComputation.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response postComputation(ApiComputation comp)
			throws DbException
	{
		try (ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO())
		{
			DbComputation dbComp = map(comp);
			dai.writeComputation(dbComp);
			return Response.status(Response.Status.CREATED).entity(map(dbComp)).build();
		}
		catch(DbIoException | DatabaseException ex)
		{
			throw new DbException("Unable to store computation", ex);
		}
	}

	static DbComputation map(ApiComputation comp) throws DatabaseException
	{
		DbComputation ret;
		if (comp.getComputationId() != null)
		{
			ret = new DbComputation(DbKey.createDbKey(comp.getComputationId()), comp.getName());
		}
		else
		{
			ret = new DbComputation(DbKey.NullKey, comp.getName());
		}
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithmId(DbKey.createDbKey(comp.getAlgorithmId()));
		}
		if (comp.getAppId() != null)
		{
			ret.setAppId(DbKey.createDbKey(comp.getAppId()));
		}
		ret.setComment(comp.getComment());
		ret.setEnabled(comp.isEnabled());
		ret.setValidEnd(comp.getEffectiveEndDate());
		ret.setValidStart(comp.getEffectiveStartDate());
		ret.setAlgorithmName(comp.getAlgorithmName());
		if (comp.getAlgorithmId() != null)
		{
			ret.setAlgorithm(new DbCompAlgorithm(DbKey.createDbKey(comp.getAlgorithmId()),
					comp.getAlgorithmName(), null, comp.getComment()));
		}
		ret.setApplicationName(comp.getApplicationName());
		ret.setGroup(new TsGroup().copy(comp.getGroupName()));
		ret.setLastModified(comp.getLastModified());
		if (comp.getGroupId() != null)
		{
			ret.setGroupId(DbKey.createDbKey(comp.getGroupId()));
		}
		else
		{
			ret.setGroupId(DbKey.NullKey);
		}
		for (String prop : comp.getProps().stringPropertyNames())
		{
			ret.setProperty(prop, comp.getProps().getProperty(prop));
		}
		for (ApiCompParm parm : comp.getParmList())
		{
			ret.addParm(map(parm));
		}
		return ret;
	}

	static DbCompParm map(ApiCompParm parm) throws DatabaseException
	{
		if (parm == null)
		{
			return null;
		}
		DbCompParm ret = new DbCompParm(parm.getAlgoRoleName(),
				parm.getDataTypeId() != null ? DbKey.createDbKey(parm.getDataTypeId()) : DbKey.NullKey,
				parm.getInterval(), parm.getTableSelector(), parm.getDeltaT());
		if (parm.getDataTypeId() != null || parm.getDataType() != null)
		{
			String[] parts = parm.getDataType().split(":");
			DataType dt = new DataType(parts[0], parts[1]);
			if (parm.getDataTypeId() != null)
			{
				dt.setId(DbKey.createDbKey(parm.getDataTypeId()));
			}
			ret.setDataType(dt);
		}
		ret.setInterval(parm.getInterval());
		if (parm.getSiteId() != null)
		{
			Site site = new Site();
			site.setPublicName(parm.getSiteName());
			ret.setSite(site);
			ret.setSiteId(DbKey.createDbKey(parm.getSiteId()));
		}
		else
		{
			ret.setSiteId(DbKey.NullKey);
		}
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		ret.setAlgoParmType(parm.getAlgoParmType());
		ret.setRoleName(parm.getAlgoRoleName());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		ret.setDeltaTUnits(parm.getDeltaTUnits());
		if (parm.getModelId() != null)
		{
			ret.setModelId(parm.getModelId());
		}
		ret.setTableSelector(parm.getTableSelector());
		ret.setInterval(parm.getInterval());
		ret.setDeltaT(parm.getDeltaT());
		ret.setUnitsAbbr(parm.getUnitsAbbr());
		return ret;
	}

	@DELETE
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing OpenDCS Computation",
			description = "Required argument computationid must be passed in the URL.",
			tags = {"REST - Computation Methods"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted computation"),
					@ApiResponse(responseCode = "400", description = "Missing required computationid parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public Response deleteComputation(@Parameter(required = true, description = "Unique Computation ID",
			schema = @Schema(implementation = Long.class, example = "4"))
		@QueryParam("computationid") Long computationId)
			throws DbException, WebAppException
	{
		if (computationId == null)
		{
			throw new MissingParameterException("Missing required computationid parameter.");
		}

		try (ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO())
		{
			dai.deleteComputation(DbKey.createDbKey(computationId));
			return Response.noContent()
					.entity(String.format("Computation with ID: %d deleted", computationId)).build();
		}
		catch(DbIoException | ConstraintException ex)
		{
			throw new DbException(String.format("Unable to delete computation by ID: %s", computationId), ex);
		}
	}

	@GET
	@Path("runcomputation")
	@Produces(MediaType.SERVER_SENT_EVENTS)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Execute an Existing OpenDCS Computation",
			description = "Endpoint takes in a computation name and a list of timeseries IDs to execute a computation. "
					+ "Optionally takes in a start and end date for a time window to use for the computation",
			tags = {"REST - Computation Methods"},
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully initiated execution of computation",
							content = {@Content(mediaType = MediaType.SERVER_SENT_EVENTS)}),
					@ApiResponse(responseCode = "400", description = "Missing required computationid parameter"),
					@ApiResponse(responseCode = "404", description = "Computation with the specified ID not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			}
	)
	public void runComputation(
			@Context Sse sse,
			@Context SseEventSink eventSink,
			@Parameter(required = true, description = "Unique Computation ID",
					schema = @Schema(implementation = Long.class, example = "4"))
			@QueryParam("computationid") Long computationId,
			@Parameter(required = true, description = "List of time series IDs, comma delimited",
					schema = @Schema(implementation = String.class, example = "LOC.TS.ID,LOC2.TS.ID"))
			@QueryParam("tsids") String timeseriesIds,
			@Parameter(description = "Optional parameter to specify the beginning of the time range to execute the computation on.",
					schema = @Schema(implementation = Date.class, example = "2025-10-25T12:00:00.000Z"))
			@QueryParam("since") Date since,
			@Parameter(description = "Optional parameter to specify the end of the time range to execute the computation on",
					schema = @Schema(implementation = Date.class, example = "2025-10-25T12:00:00.000Z"))
			@QueryParam("until") Date until)
			throws DbException, WebAppException
	{
		String[] tsList = timeseriesIds.split(",");

		final String compStatus = "computation-status";

		if(computationId == null)
		{
			throw new MissingParameterException("Missing required computationid parameter.");
		}

		try(ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO();
			TimeSeriesDAI tsDai = getLegacyTimeseriesDB().makeTimeSeriesDAO())
		{
			DbComputation comp = dai.getComputationById(DbKey.createDbKey(computationId));

			List<TimeSeriesIdentifier> tsiList = new ArrayList<>();

			for(String tsId : tsList)
			{
				tsiList.add(tsDai.getTimeSeriesIdentifier(tsId));
			}

			String taskID = UUID.randomUUID().toString();

			executor.submit(() ->
			{
				OutboundSseEvent event = sse.newEventBuilder()
						.name(compStatus)
						.id(taskID)
						.mediaType(MediaType.SERVER_SENT_EVENTS_TYPE)
						.data(String.format("Running computation with ID: %s", computationId))
						.build();
				eventSink.send(event).toCompletableFuture().join();

				try
				{
					ComputationExecution execution = new ComputationExecution(createDb());
					SseProgressListener listener = new SseProgressListener(eventSink, sse, compStatus, taskID);
					ComputationExecution.CompResults results = execution.execute(comp, tsiList, since, until, listener);

					event = sse.newEventBuilder()
							.name(compStatus)
							.id(taskID)
							.mediaType(MediaType.SERVER_SENT_EVENTS_TYPE)
							.data(String.format("Computation executed with %d errors", results.numErrors()))
							.build();
					eventSink.send(event).toCompletableFuture().join();
				}
				catch(DbIoException ex)
				{
					log.error("Error while executing computation", ex);
					event = sse.newEventBuilder()
							.name(compStatus)
							.id(taskID)
							.mediaType(MediaType.SERVER_SENT_EVENTS_TYPE)
							.data(String.format("Error while executing computation: %s", comp.getApplicationName()))
							.build();
					eventSink.send(event).toCompletableFuture().join();
				}
				finally
				{
					try
					{
						eventSink.close();
					}
					catch (IOException ex)
					{
						log.error("Error closing SSE event sink", ex);
					}
				}
			});
		}
		catch(NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format("Computation with ID %s not found", computationId), ex);
		}
		catch(DbIoException ex)
		{
			throw new DbException(String.format("Error retrieving computation to execute by ID: %s", computationId), ex);
		}
	}

	private static final class SseProgressListener extends ProgressListener
	{
		private final SseEventSink eventSink;
		private final Sse sse;
		private final String name;
		private final String taskId;

		public SseProgressListener(SseEventSink eventSink, Sse sse, String name, String taskId)
		{
			this.eventSink = eventSink;
			this.sse = sse;
			this.name = name;
			this.taskId = taskId;
		}

		@Override
		public void onProgress(String message, Level logLevel, Throwable cause)
		{
			logEvent(message, logLevel, cause);
			OutboundSseEvent event = sse.newEventBuilder()
					.name(name)
					.id(taskId)
					.reconnectDelay(3000L)
					.data(message)
					.mediaType(MediaType.SERVER_SENT_EVENTS_TYPE)
					.build();
			eventSink.send(event).toCompletableFuture().join();
		}
	}
}
