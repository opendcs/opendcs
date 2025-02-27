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
import ilex.util.Logger;
import opendcs.dai.DacqEventDAI;
import opendcs.dai.IntervalDAI;
import opendcs.dai.ScheduleEntryDAI;
import opendcs.opentsdb.Interval;
import org.opendcs.odcsapi.beans.ApiDacqEvent;
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
	public Response getRouting(@QueryParam("routingid") Long routingId)
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
	public Response deleteRouting(@QueryParam("routingid") Long routingId)
			throws DbException, WebAppException
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
					.entity("RoutingSpec with ID " + routingId + " deleted").build();
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
	public Response getSchedule(@QueryParam("scheduleid") Long scheduleId)
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
	public Response deleteSchedule(@QueryParam("scheduleid") Long scheduleId)
			throws DbException, WebAppException
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
	public Response getRoutingExecStatus(@QueryParam("scheduleentryid") Long scheduleEntryId)
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
	public Response getDacqEvents(@QueryParam("appid") Long appId, @QueryParam("routingexecid") Long routingExecId,
			@QueryParam("platformid") Long platformId, @QueryParam("backlog") String backlog)
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
