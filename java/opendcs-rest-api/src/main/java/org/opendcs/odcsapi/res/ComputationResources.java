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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import decodes.cwms.CwmsTimeSeriesDAO;
import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.CompFilter;
import decodes.tsdb.ComputationExecution;
import decodes.tsdb.ConstraintException;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbCompResolver;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.ProgressListener;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
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
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import opendcs.dai.CompDependsDAI;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.beans.ApiCompResults;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiComputationRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.servlet.ExecutorPoolService;
import org.opendcs.odcsapi.util.APIStreamMapper;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.DTOMappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

@Path("/")
public final class ComputationResources extends OpenDcsResource
{
	private static final Logger log = LoggerFactory.getLogger(ComputationResources.class);

	@Context HttpHeaders httpHeaders;

	private final ExecutorPoolService executors;

	@Inject
	protected ComputationResources(ExecutorPoolService executors)
	{
		this.executors = executors;
	}

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
			List<ApiComputationRef> computationRefs = APIStreamMapper.mapList(dai.listComps(compFilter::passes),
					ApiComputationRef.class);
			return Response.ok().entity(computationRefs).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to retrieve computation references", ex);
		}
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
					.entity(DTOMappers.map(dai.getComputationById(DbKey.createDbKey(compId)))).build();
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

	@POST
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create or Overwrite Existing OpenDCS Computation",
			description = """
					The Computation POST method takes a single OpenDCS Computation Record in JSON format, \
					as described above for GET. \s

					For creating a new record, leave computationId out of the passed data structure. \s

					For overwriting an existing one, include the computationId that was previously returned. \
					The computation in the database is replaced with the one sent.""",
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
					@ApiResponse(responseCode = "400", description = "Bad Request - e.g. a constraint violation "
							+ "such as a duplicate computation name",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = Status.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = Status.class)))
			}
	)
	public Response postComputation(ApiComputation comp)
			throws DbException
	{
		try (ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO())
		{
			DbComputation dbComp = DTOMappers.map(comp);
			dai.writeComputation(dbComp);
			return Response.status(Response.Status.CREATED).entity(DTOMappers.map(dbComp)).build();
		}
		catch(DbIoException | DatabaseException ex)
		{
			throw new DbException("Unable to store computation", ex);
		}
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
			description = """
					Endpoint takes in a computation name and a list of timeseries IDs to execute a computation. \
					Optionally takes in a start and end date for a time window to use for the computation. \
					For group computations, supply the tsid parameter to run against a specific input time series. \
					If tsid is omitted for a group computation, the computation is expanded and run against \
					every time series that can currently trigger it.""",
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
			@Parameter(required = true, description = "Parameter to specify the beginning of the time range to execute the computation on.",
					schema = @Schema(implementation = Instant.class, example = "2025-10-25T12:00:00Z"))
			@QueryParam("start") String start,
			@Parameter(required = true, description = "Parameter to specify the end of the time range to execute the computation on",
					schema = @Schema(implementation = Instant.class, example = "2025-10-25T12:00:00Z"))
			@QueryParam("end") String end,
			@Parameter(description = """
					Time series key to use as the group computation input. \
					When provided the group computation is resolved against this specific time series. \
					When omitted for a group computation, the computation is expanded against every \
					time series that can currently trigger it.""",
					schema = @Schema(implementation = Long.class))
			@QueryParam("tsid") Long tsId)
			throws DbException, WebAppException
	{
		final String compStatus = "computation-status";

		if(computationId == null)
		{
			throw new MissingParameterException("Missing required computationid parameter.");
		}

		try(ComputationDAI dai = getLegacyTimeseriesDB().makeComputationDAO();
			TimeSeriesDAI tsDai = getLegacyTimeseriesDB().makeTimeSeriesDAO();
			SiteDAI siteDai = getLegacyTimeseriesDB().makeSiteDAO();
			CompDependsDAI compDependsDai = getLegacyTimeseriesDB().makeCompDependsDAO())
		{
			DbComputation comp = dai.getComputationById(DbKey.createDbKey(computationId));

			List<String> diagnostics = new ArrayList<>();
			List<DbComputation> resolvedComps =
					resolveComputations(comp, computationId, tsId, tsDai, compDependsDai, diagnostics);

			String taskID = UUID.randomUUID().toString();

			final Instant startTime = Instant.parse(start);
			final Instant endTime = Instant.parse(end);
			Date startDate = Date.from(startTime);
			Date endDate = Date.from(endTime);

			final var channel = new SseChannel(sse, eventSink, compStatus, taskID);

			List<TimeSeriesIdentifier> outputList = new ArrayList<>();
			for(DbComputation resolvedComp : resolvedComps)
			{
				outputList.addAll(processOutputTsIds(resolvedComp, tsDai, siteDai, computationId, channel));
			}

			final var contextMap = MDC.getCopyOfContextMap();
			log.trace("Starting computation");
			CompletableFuture.runAsync(() ->
			{
				channel.sendText(String.format("Running computation with ID: %s", computationId));

				// Anything the input resolution had to say -- a recovered binding, or the reason
				// this run is expected to come back empty.
				for(String diagnostic : diagnostics)
				{
					channel.sendText(diagnostic);
				}

				List<TimeSeriesIdentifier> written = List.of();
				try
				{
					if (contextMap != null)
					{
						MDC.setContextMap(contextMap);
					}
					written = executeAndPublishResult(computationId, resolvedComps, startDate, endDate, channel);
				}
				catch (RuntimeException ex)
				{
					log.error("Unexpected error in computation async task for computation ID: {}", computationId, ex);
				}
				finally
				{
					try
					{
						// Prefer the identifiers of the time series actually written by the run: those
						// carry the real database keys the caller needs to read the computed values back.
						// The parm-derived outputList is only a best-effort description of the intended
						// outputs (no keys), so fall back to it when nothing was written.
						processOutput(written.isEmpty() ? outputList : written, channel, startTime, endTime);
					}
					catch (RuntimeException ex)
					{
						log.error("Error sending computation results for computation ID: {}", computationId, ex);
					}
					try
					{
						eventSink.close();
					}
					catch(IOException ex)
					{
						log.error("Error closing SSE event sink", ex);
					}
					MDC.clear();
				}
			}, executors.getComputationExecutor());
		}
		catch(NoSuchObjectException ex)
		{
			// NoSuchObjectException is also thrown by the group-input resolution above (unknown
			// tsid, or a parm that can't be resolved against it) -- prefer that specific message
			// over a generic "computation not found" when one is available.
			String message = ex.getMessage() != null
					? ex.getMessage()
					: String.format("Computation with ID %s not found", computationId);
			throw new DatabaseItemNotFoundException(message, ex);
		}
		catch(DbIoException ex)
		{
			throw new DbException(String.format("Error retrieving computation to execute by ID: %s", computationId), ex);
		}
	}

	/**
	 * Resolves the computation(s) that should actually be executed. Non-group computations
	 * run as-is, unless their input binding is missing -- see {@link #resolveConcreteInputs}.
	 * Group computations are made concrete against either the single supplied
	 * tsid, or -- if none was supplied -- against every time series currently known to
	 * trigger the computation, mirroring the expansion the thick-client comp-run tool performs.
	 */
	private List<DbComputation> resolveComputations(DbComputation comp, Long computationId, Long tsId,
			TimeSeriesDAI tsDai, CompDependsDAI compDependsDai, List<String> diagnostics)
			throws DbIoException, NoSuchObjectException
	{
		if (!comp.hasGroupInput())
		{
			return List.of(resolveConcreteInputs(comp, computationId, tsDai, compDependsDai, diagnostics));
		}

		if (tsId != null)
		{
			TimeSeriesIdentifier inputTsid = tsDai.getTimeSeriesIdentifier(DbKey.createDbKey(tsId));
			return List.of(DbCompResolver.makeConcrete(getLegacyTimeseriesDB(), tsDai, inputTsid, comp, true));
		}

		DbCompResolver resolver = new DbCompResolver(getLegacyTimeseriesDB());
		List<DbComputation> resolvedComps = new ArrayList<>();
		for(TimeSeriesIdentifier trigger : compDependsDai.getTriggersFor(DbKey.createDbKey(computationId)))
		{
			try
			{
				DbComputation concrete = DbCompResolver.makeConcrete(getLegacyTimeseriesDB(), tsDai, trigger, comp, true);
				resolver.addToResults(resolvedComps, concrete, null);
			}
			catch(NoSuchObjectException ex)
			{
				log.warn("Skipping trigger '{}' for group computation ID: {} -- {}",
						trigger.getUniqueString(), computationId, ex.getMessage());
			}
		}
		if (resolvedComps.isEmpty())
		{
			throw new NoSuchObjectException(
					String.format("No resolvable input time series found for group computation ID: %s", computationId));
		}
		return resolvedComps;
	}

	/**
	 * A non-group computation carries its input binding in each parm's SITE_DATATYPE_ID. If that
	 * is missing the computation cannot read its inputs, and a manual run quietly produces
	 * nothing -- the automatic path never notices because it takes its inputs from the tasklist
	 * with real identifiers already attached.
	 *
	 * <p>Recover by resolving the computation against the dependency table, which lists the
	 * concrete time series that trigger it, exactly as the group path does. This repairs
	 * computations whose stored binding was lost without needing a data migration. If the
	 * dependencies cannot supply a binding either, run anyway but record why the run is
	 * expected to come back empty, so the caller is told rather than left guessing.
	 */
	private DbComputation resolveConcreteInputs(DbComputation comp, Long computationId,
			TimeSeriesDAI tsDai, CompDependsDAI compDependsDai, List<String> diagnostics)
			throws DbIoException
	{
		List<String> unresolved = unresolvedInputRoles(comp);
		if (unresolved.isEmpty())
		{
			return comp;
		}
		log.info("Computation ID {} has unbound input parm(s) {} -- attempting to resolve from "
				+ "its dependencies.", computationId, unresolved);
		for(TimeSeriesIdentifier trigger : compDependsDai.getTriggersFor(DbKey.createDbKey(computationId)))
		{
			try
			{
				DbComputation concrete = DbCompResolver.makeConcrete(getLegacyTimeseriesDB(), tsDai,
						trigger, comp, true);
				if (unresolvedInputRoles(concrete).isEmpty())
				{
					diagnostics.add(String.format(
							"Input parm(s) %s had no time series bound; resolved from dependency '%s'.",
							unresolved, trigger.getUniqueString()));
					return concrete;
				}
			}
			catch(NoSuchObjectException ex)
			{
				log.warn("Could not resolve computation ID {} against dependency '{}' -- {}",
						computationId, trigger.getUniqueString(), ex.getMessage());
			}
		}
		diagnostics.add(String.format(
				"Input parm(s) %s have no time series bound and none could be recovered from this "
				+ "computation's dependencies. Bind the parm to a time series and save the "
				+ "computation; until then it can only run from the tasklist.", unresolved));
		return comp;
	}

	/** Roles of the input parms that cannot currently be resolved to a stored time series. */
	private List<String> unresolvedInputRoles(DbComputation comp)
	{
		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		List<String> unresolved = new ArrayList<>();
		for(DbCompParm parm : comp.getParmList())
		{
			if (!parm.isInput())
			{
				continue;
			}
			try
			{
				// expandSDI also fills the parm's site and data type, which is what the executive
				// later needs; a null return means the parm has no usable binding.
				if (tsdb.expandSDI(parm) == null)
				{
					unresolved.add(parm.getRoleName());
				}
			}
			catch(DbIoException | NoSuchObjectException ex)
			{
				log.debug("Input parm '{}' does not resolve -- {}", parm.getRoleName(), ex.getMessage());
				unresolved.add(parm.getRoleName());
			}
		}
		return unresolved;
	}

	private List<TimeSeriesIdentifier> processOutputTsIds(DbComputation comp, TimeSeriesDAI tsDai, SiteDAI siteDai,
				Long computationId, SseChannel channel) throws DbIoException
	{
		List<TimeSeriesIdentifier> outputList = new ArrayList<>();
		DbKey dataTypeId = null;
		DataType dataType = null;
		for(DbCompParm parm : comp.getParmList())
		{
			if(parm.getAlgoParmType().contains("o"))
			{
				boolean isCwms = tsDai instanceof CwmsTimeSeriesDAO;
				TimeSeriesIdentifier identifier;
				if(isCwms)
				{
					identifier = new CwmsTsId();
					((CwmsTsId) identifier).setUtcOffset(parm.getDeltaT());
					((CwmsTsId) identifier).setDuration(parm.getDuration());
					((CwmsTsId) identifier).setVersion(parm.getVersion());
					identifier.setPart("paramtype", parm.getParamType());
				}
				else
				{
					identifier = new HdbTsId();
					if(parm.getSiteDataTypeId() != null && dataTypeId == null)
					{
						dataTypeId = parm.getSiteDataTypeId();
						((HdbTsId) identifier).setSdi(parm.getSiteDataTypeId());
					}
					else if(parm.getDataTypeId() == null && dataTypeId != null)
					{
						((HdbTsId) identifier).setSdi(dataTypeId);
					}
					((HdbTsId) identifier).setModelId(parm.getModelId());
					identifier.setTableSelector(parm.getTableSelector());
				}
				if(parm.getDataType() != null)
				{
					dataType = parm.getDataType();
				}
				identifier.setDataType(dataType);
				identifier.setStorageUnits(parm.getUnitsAbbr());
				identifier.setInterval(parm.getInterval());

				try
				{
					Site site = siteDai.getSiteById(parm.getSiteId());
					if(site != null)
					{
						identifier.setSiteName(site.getDisplayName());
						identifier.setSite(site);
					}
					else
					{
						String name = comp.getProperty("reservoirId");
						if(name != null && !name.isEmpty())
						{
							identifier.setSiteName(name);
						}
					}
				}
				catch(NoSuchObjectException ex)
				{
					log.error(String.format("Unable to retrieve site name for site ID: %s", parm.getSiteId()), ex);
					channel.send(channel.newEvent("ERROR")
							.mediaType(MediaType.TEXT_PLAIN_TYPE)
							.data(String.format("No site found with ID: %s for computation with ID: %s", parm.getSiteId().getValue(), computationId))
							.build());
				}
				finally
				{
					outputList.add(identifier);
				}
			}
		}
		return outputList;
	}

	/**
	 * Runs the resolved computations and persists whatever they produced.
	 *
	 * <p>{@link ComputationExecution} only computes: the output time series come back in the
	 * {@code afterComp} handler and it is the caller's responsibility to write them, exactly as
	 * {@code ComputationApp} does for the automatic (tasklist driven) path. Without this the run
	 * would appear to succeed while nothing was ever stored, and the caller would read an empty
	 * time series back.
	 *
	 * @return the identifiers of the time series that were successfully saved
	 */
	private List<TimeSeriesIdentifier> executeAndPublishResult(Long computationId, List<DbComputation> comps,
			Date startDate, Date endDate, SseChannel channel)
	{
		SseProgressListener listener = new SseProgressListener(channel);
		// Computations are executed in parallel on the shared pool, so afterComp can be called from
		// several threads at once. Only collect here -- saving happens on this thread once the batch
		// has joined, because a TimeSeriesDAI is not safe to share across threads.
		List<CTimeSeries> outputs = new CopyOnWriteArrayList<>();
		try
		{
			OpenDcsDatabase db = createDb();
			TimeSeriesDb tsDb = db.getLegacyDatabase(TimeSeriesDb.class)
					.orElseThrow(() -> new IllegalStateException("Time series database is unavailable."));
			ComputationExecution.CompResults results;
			try (ComputationExecution execution = new ComputationExecution(db, executors.getComputationExecutor()))
			{
				results = execution.execute(comps, new DataCollection(), startDate, endDate, listener, dc ->
				{
					outputs.addAll(dc.getAllTimeSeries());
					return dc;
				});
			}

			List<TimeSeriesIdentifier> written = saveOutputs(outputs, tsDb, listener);

			channel.sendText(String.format("Computation executed with %d errors", results.numErrors()));
			return written;
		}
		catch (RuntimeException ex)
		{
			log.error("Error during computation execution for computation ID: {}", computationId, ex);
			// Reported as ERROR, not as a status line: this is a hard failure and the caller has to
			// be able to tell it apart from ordinary trace output.
			channel.send(channel.newEvent("ERROR")
					.mediaType(MediaType.TEXT_PLAIN_TYPE)
					.data(String.format("Computation failed: %s", describe(ex)))
					.build());
			return List.of();
		}
	}

	/**
	 * Writes the computed output time series and reports each one on the progress stream so the
	 * caller can see what the run actually stored.
	 */
	private List<TimeSeriesIdentifier> saveOutputs(List<CTimeSeries> outputs, TimeSeriesDb tsDb,
			ProgressListener listener)
	{
		if(outputs.isEmpty())
		{
			listener.onProgress("Computation produced no output time series to save.", Level.INFO, null);
			return List.of();
		}
		// A group computation resolves into one concrete computation per trigger, and several of
		// those can share an output series -- key the results so the caller gets one column per
		// output rather than one per resolved computation.
		Map<DbKey, TimeSeriesIdentifier> written = new LinkedHashMap<>();
		try (TimeSeriesDAI timeSeriesDAO = tsDb.makeTimeSeriesDAO())
		{
			for(CTimeSeries cts : outputs)
			{
				TimeSeriesIdentifier tsid = cts.getTimeSeriesIdentifier();
				String name = tsid != null ? tsid.getUniqueString() : cts.getNameString();
				try
				{
					timeSeriesDAO.saveTimeSeries(cts);
					listener.onProgress(String.format("Saved %d values for '%s'", cts.size(), name), Level.INFO, null);
					if(tsid != null && !DbKey.isNull(tsid.getKey()))
					{
						written.putIfAbsent(tsid.getKey(), tsid);
					}
				}
				catch(DbIoException | BadTimeSeriesException ex)
				{
					listener.onProgress(String.format("Cannot save time series '%s'", name), Level.WARN, ex);
				}
			}
		}
		return new ArrayList<>(written.values());
	}

	/** Exception message plus its root cause, so a wrapped failure still says what went wrong. */
	private static String describe(Throwable ex)
	{
		StringBuilder sb = new StringBuilder(ex.getMessage() != null ? ex.getMessage() : ex.toString());
		Throwable cause = ex.getCause();
		while(cause != null)
		{
			if(cause.getMessage() != null && sb.indexOf(cause.getMessage()) < 0)
			{
				sb.append(": ").append(cause.getMessage());
			}
			cause = cause.getCause();
		}
		return sb.toString();
	}

	private void processOutput(List<TimeSeriesIdentifier> outputList, SseChannel channel,
			Instant startDate, Instant endDate)
	{
		List<ApiTimeSeriesIdentifier> ids = APIStreamMapper.mapList(outputList, ApiTimeSeriesIdentifier.class);
		ApiCompResults results = new ApiCompResults();
		results.setEndTime(endDate.toString());
		results.setStartTime(startDate.toString());
		results.setTsIds(ids);

		channel.send(channel.newEvent("Results")
				.mediaType(MediaType.APPLICATION_JSON_TYPE)
				.data(ApiCompResults.class, results)
				.build());
	}

	private static final class SseProgressListener extends ProgressListener
	{
		private final SseChannel channel;

		public SseProgressListener(SseChannel channel)
		{
			this.channel = channel;
		}

		@Override
		public void onProgress(String message, Level logLevel, Throwable cause)
		{
			logEvent(message, logLevel, cause);
			// The cause carries the only useful detail for a failure ("Cannot initialize computation
			// 'x'" says nothing on its own), so fold it into the streamed line -- the caller has no
			// access to the server log.
			String data = cause == null ? message : String.format("%s -- %s", message, describe(cause));
			channel.send(channel.newEvent(channel.eventName())
					.reconnectDelay(3000L)
					.data(data)
					.mediaType(MediaType.TEXT_PLAIN_TYPE)
					.build());
		}
	}
}
