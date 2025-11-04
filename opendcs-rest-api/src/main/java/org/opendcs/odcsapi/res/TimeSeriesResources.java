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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import decodes.cwms.CwmsTsId;
import decodes.hdb.HdbTsId;
import decodes.sql.DbKey;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.IntervalCodes;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import ilex.var.TimedVariable;
import opendcs.dai.IntervalDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import opendcs.opentsdb.Interval;
import org.opendcs.odcsapi.beans.ApiDataType;
import ilex.util.IDateFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.opendcs.odcsapi.beans.ApiInterval;
import org.opendcs.odcsapi.beans.ApiSiteRef;
import org.opendcs.odcsapi.beans.ApiTimeSeriesData;
import org.opendcs.odcsapi.beans.ApiTimeSeriesIdentifier;
import org.opendcs.odcsapi.beans.ApiTimeSeriesSpec;
import org.opendcs.odcsapi.beans.ApiTimeSeriesValue;
import org.opendcs.odcsapi.beans.ApiTsGroup;
import org.opendcs.odcsapi.beans.ApiTsGroupRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * HTTP resources relating to Time Series data and descriptors
 * @author mmaloney
 *
 */
@Path("/")
public final class TimeSeriesResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@Tag(name = "Time Series Methods")
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The tsrefs method returns a list of time series defined in the database.",
			description = "You have the option to filter out inactive time series by passing 'active=true' argument.  \n"
					+ "Examples:  \n\n    http://localhost:8080/odcsapi/tsrefs\n    "
					+ "http://localhost:8080/odcsapi/tsrefs?active=true\n\n\n"
					+ "This returns an array of Time Series Identifiers. The numeric Key of a time series identifier "
					+ "may be used in subsequent calls to get the complete specification for the time series "
					+ "(GET tsspec) or to retrieve time series data (GET tsdata). The format of the returned "
					+ "data is as follows:  \n```\n[\n  {\n    \"uniqueString\": \"OKVI4.Stage.Inst.15Minutes.0.raw\","
					+ "\n    \"key\": 1,\n    \"description\": null,\n    \"storageUnits\": \"ft\",\n    "
					+ "\"active\": true\n  },\n  {\n    \"uniqueString\": \"OKVI4.Stage.Ave.1Day.1Day.CO\",\n    "
					+ "\"key\": 2,\n    \"description\": null,\n    \"storageUnits\": \"ft\",\n    "
					+ "\"active\": true\n  },\n  {\n    \"uniqueString\": \"OKVI4.Stage.Ave.1Day.1Day.CC\",\n    "
					+ "\"key\": 4,\n    \"description\": null,\n    \"storageUnits\": \"ft\",\n    "
					+ "\"active\": true\n  },\n. . .\n]\n```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
								array = @ArraySchema(schema = @Schema(implementation = ApiTimeSeriesIdentifier.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
			},
			tags = {"Time Series Methods"}
	)
	public Response getTimeSeriesRefs(@Parameter(description = "Include only active time series", required = true,
			schema = @Schema(implementation = Boolean.class, example = "true"))
		@QueryParam("active") Boolean activeOnly)
			throws DbException
	{
		boolean filterActive = activeOnly != null && activeOnly;

		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		try (TimeSeriesDAI dai = tsdb.makeTimeSeriesDAO())
		{
			List<ApiTimeSeriesIdentifier> tsIds = idMap(dai.listTimeSeries());
			List<ApiTimeSeriesIdentifier> returnList = new ArrayList<>();
			for (ApiTimeSeriesIdentifier tsId : tsIds)
			{
				if (!filterActive || tsId.isActive())
				{
					returnList.add(tsId);
				}
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(returnList)
					.build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series", ex);
		}
	}

	static List<ApiTimeSeriesIdentifier> idMap(List<TimeSeriesIdentifier> identifiers)
	{
		List<ApiTimeSeriesIdentifier> ret = new ArrayList<>();
		for(TimeSeriesIdentifier id : identifiers)
		{
			if (id instanceof CwmsTsId)
			{
				CwmsTsId ctsid = (CwmsTsId)id;
				ApiTimeSeriesIdentifier apiId = new ApiTimeSeriesIdentifier();
				if (id.getKey() != null)
				{
					apiId.setKey(id.getKey().getValue());
				}
				else
				{
					apiId.setKey(DbKey.NullKey.getValue());
				}
				apiId.setActive(ctsid.isActive());
				apiId.setDescription(id.getDescription());
				apiId.setStorageUnits(id.getStorageUnits());
				apiId.setUniqueString(id.getUniqueString());
				ret.add(apiId);
			}
			else
			{
				ApiTimeSeriesIdentifier apiId = new ApiTimeSeriesIdentifier();
				if (id.getKey() != null)
				{
					apiId.setKey(id.getKey().getValue());
				}
				else
				{
					apiId.setKey(DbKey.NullKey.getValue());
				}
				apiId.setDescription(id.getDescription());
				apiId.setStorageUnits(id.getStorageUnits());
				apiId.setUniqueString(id.getUniqueString());
				ret.add(apiId);
			}
		}
		return ret;
	}

	@GET
	@Path("tsspec")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The tsspec method returns a complete specification for a time series "
					+ "identified by the 'key' parameter.",
			description = "Example: \n\n    http://localhost:8080/odcsapi/tsspec?key=532",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved time series specification",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
								schema = @Schema(implementation = ApiTimeSeriesSpec.class))),
					@ApiResponse(responseCode = "400", description = "Missing required tskey parameter"),
					@ApiResponse(responseCode = "404", description = "Time series not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods"}
	)
	public Response getTimeSeriesSpec(@Parameter(description = "Numeric key identifying the time series.",
			required = true, example = "532", schema = @Schema(implementation = Long.class))
		@QueryParam("key") Long tsKey)
			throws WebAppException, DbException
	{
		if (tsKey == null)
		{
			throw new MissingParameterException("Missing required tskey parameter.");
		}

		try (TimeSeriesDAI dai = getLegacyTimeseriesDB().makeTimeSeriesDAO())
		{
			TimeSeriesIdentifier identifier = dai.getTimeSeriesIdentifier(DbKey.createDbKey(tsKey));
			ApiTimeSeriesSpec spec = specMap(identifier);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(spec).build();
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException(String.format("Time series with key: %d not found", tsKey), ex);
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series spec", ex);
		}
	}

	static ApiTimeSeriesSpec specMap(TimeSeriesIdentifier id)
	{
		ApiTimeSeriesSpec ret = new ApiTimeSeriesSpec();
		ApiTimeSeriesIdentifier tsId = map(id);
		ret.setTsid(tsId);
		if (id instanceof CwmsTsId)
		{
			CwmsTsId ctsid = (CwmsTsId)id;
			ret.setActive(ctsid.isActive());
			ret.setInterval(ctsid.getInterval());
			ret.setDuration(ctsid.getDuration());
			ret.setVersion((ctsid).getVersion());
			if (ctsid.getDataTypeId() != null)
			{
				ret.setDatatypeId(ctsid.getDataTypeId());
			}
			else
			{
				ret.setDatatypeId(DbKey.NullKey);
			}
			if (ctsid.getSite() != null && ctsid.getSite().getId() != null)
			{
				ret.setSiteId(ctsid.getSite().getId());
			}
			else
			{
				ret.setSiteId(DbKey.NullKey);
			}
			if (ctsid.getSubLoc() != null)
			{
				ret.setLocation(ctsid.getBaseLoc() + "-" + ctsid.getSubLoc());
			}
			else
			{
				ret.setLocation(ctsid.getBaseLoc());
			}
		}
		else if (id instanceof HdbTsId)
		{
			HdbTsId htsid = (HdbTsId)id;
			ret.setInterval(htsid.getInterval());
			if (htsid.getDataTypeId() != null)
			{
				ret.setDatatypeId(htsid.getDataTypeId());
			}
			else
			{
				ret.setDatatypeId(DbKey.NullKey);
			}
			if (htsid.getSite() != null && htsid.getSite().getId() != null)
			{
				ret.setSiteId(htsid.getSite().getId());
			}
			else
			{
				ret.setSiteId(DbKey.NullKey);
			}
		}
		ret.setDatatypeId(id.getDataTypeId());
		if (ret.getLocation() == null || ret.getLocation().isEmpty())
		{
			ret.setLocation(id.getSiteName());
		}
		return ret;
	}

	@GET
	@Path("tsdata")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The tsdata method returns data for a time series over a specified time range.",
			description = "The method takes 3 arguments:\n"
					+ "* **tkey (required)** – the numeric key identifying the time series. "
					+ "It is contained within a Time Series Identifier described above.\n"
					+ "* **tstart** – Optionally specifies the start of the time range for retrieval. "
					+ "If omitted, the oldest data in the database is returned. See below for time format.\n"
					+ "* **tend** – Optionally specifies the end of the time range for retrieval. "
					+ "If omitted, the newest data in the database is returned. See below for time format.  \n\n"
					+ "The since and until arguments may have any of the following formats:\n"
					+ "*\t**now-1day**\tThe word 'now' minus an increment times a unit. "
					+ "Examples: now-1day, now-5hours, now-1week, etc.\n"
					+ "*\t**now**\tThe current time that the web service call was made.\n"
					+ "*\t**YYYY/DDD/HH:MM:SS**\tA complete Julian Year, Day-of-Year, and Time\n"
					+ "*\t**YYYY/DDD/HH:MM**\tSeconds omitted means zero.\n"
					+ "*\t**DDD/HH:MM:SS**\tAssume current year\n*\t**DDD/HH:MM**\t\n"
					+ "*\t**HH:MM:SS**\tAssume current day\n*\t**HH:MM**  \n\n"
					+ "Examples:  \n```http://localhost:8080/odcsapi/tsdata?key=12```",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved time series data",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiTimeSeriesData.class))),
					@ApiResponse(responseCode = "400", description = "Invalid input parameters"),
					@ApiResponse(responseCode = "404", description = "Time series not found"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"Time Series Methods"}
	)
	public Response getTimeSeriesData(@Parameter(description = "Timeseries key", required = true,
				schema = @Schema(implementation = Long.class), example = "532")
		@QueryParam("key") Long tsKey,
			@Parameter(description = "Start time of the time range", schema = @Schema(implementation = String.class))
		@QueryParam("start") String start,
			@Parameter(description = "End time of the time range", schema = @Schema(implementation = String.class))
		@QueryParam("end") String end)
			throws WebAppException, DbException
	{
		if (tsKey == null)
		{
			throw new MissingParameterException("Missing required tskey parameter.");
		}

		Date dStart = null;
		Date dEnd = null;
		if (start != null)
		{
			try
			{
				dStart = IDateFormat.parse(start);
			}
			catch(IllegalArgumentException ex)
			{
				throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST,
						"Invalid start time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.", ex);
			}
		}
		if (end != null)
		{
			try
			{
				dEnd = IDateFormat.parse(end);
			}
			catch (IllegalArgumentException ex)
			{
				throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST,
						"Invalid end time. Use [[[CC]YY]/DDD]/HH:MM[:SS] or relative time.", ex);
			}
		}

		TimeSeriesDb tsdb = getLegacyTimeseriesDB();
		try (TimeSeriesDAI dai = tsdb.makeTimeSeriesDAO())
		{
			TimeSeriesIdentifier tsId = dai.getTimeSeriesIdentifier(DbKey.createDbKey(tsKey));
			CTimeSeries cts = tsdb.makeTimeSeries(tsId);
			dai.fillTimeSeries(cts, dStart, dEnd);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(dataMap(cts, dStart, dEnd)).build();
		}
		catch (NoSuchObjectException ex)
		{
			throw new DatabaseItemNotFoundException("Time series with key=" + tsKey + " not found", ex);
		}
		catch (DbIoException | BadTimeSeriesException ex)
		{
			throw new DbException("Unable to retrieve time series data", ex);
		}
	}

	static ApiTimeSeriesData dataMap(CTimeSeries cts, Date start, Date end)
	{
		ApiTimeSeriesData ret = new ApiTimeSeriesData();
		ret.setTsid(map(cts.getTimeSeriesIdentifier()));
		ret.setValues(map(cts, start, end));
		return ret;
	}

	static List<ApiTimeSeriesValue> map(CTimeSeries cts, Date start, Date end)
	{
		List<ApiTimeSeriesValue> ret = new ArrayList<>();
		Date current = start;
		TimedVariable tv = cts.findWithin(current, 5);
		if (tv != null && !tv.getTime().before(current))
		{
			current = processSample(tv, current, end, ret);
		}

		while (current.before(end) || current.equals(end))
		{
			TimedVariable value = cts.findNext(current);

			if (value == null)
			{
				break;
			}
			current = processSample(value, current, end, ret);
		}
		return ret;
	}

	static Date processSample(TimedVariable value, Date current, Date end, List<ApiTimeSeriesValue> ret)
	{
		double val = Double.parseDouble(value.valueString());
		ApiTimeSeriesValue apiValue = new ApiTimeSeriesValue(value.getTime(), val, value.getFlags());
		ret.add(apiValue);
		if (current.equals(end))
		{
			return Date.from(end.toInstant().plusSeconds(1));
		}
		else
		{
			return value.getTime();
		}
	}

	static ApiTimeSeriesIdentifier map(TimeSeriesIdentifier tsid)
	{
		if (tsid instanceof CwmsTsId)
		{
			CwmsTsId cTsId = (CwmsTsId)tsid;
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(cTsId.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(cTsId.getUniqueString());
			ret.setDescription(cTsId.getDescription());
			ret.setStorageUnits(cTsId.getStorageUnits());
			ret.setActive(cTsId.isActive());
			return ret;
		}
		else
		{
			// Active flag is not set here because it is not part of the TimeSeriesIdentifier
			ApiTimeSeriesIdentifier ret = new ApiTimeSeriesIdentifier();
			if(tsid.getKey() != null)
			{
				ret.setKey(tsid.getKey().getValue());
			}
			else
			{
				ret.setKey(DbKey.NullKey.getValue());
			}
			ret.setUniqueString(tsid.getUniqueString());
			ret.setDescription(tsid.getDescription());
			ret.setStorageUnits(tsid.getStorageUnits());
			return ret;
		}
	}

	@GET
	@Path("intervals")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "Time Series Methods - Interval Methods", description = "Time Intervals are stored in the database "
			+ "for OpenTSDB. They are hardcoded for CWMS and HDB.")
	@Operation(
			summary = "Returns a list of time intervals defined in the database.",
			description = "Example: \n\n    http://localhost:8080/odcsapi/intervals\n\n"
					+ "An array of data structures representing all known time intervals will be returned as shown below.\n"
					+ "```\n[\n  {\n    \"intervalId\": 1,\n    \"name\": \"irregular\",\n    \"calConstant\": \"minute\","
					+ "\n    \"calMultilier\": 0\n  },\n  {\n    \"intervalId\": 2,\n    \"name\": \"2Minutes\","
					+ "\n    \"calConstant\": \"minute\",\n    \"calMultilier\": 2\n  },\n. . .\n]\n```\n\n"
					+ "For each interval the system stores a numeric ID, a name, a Java Calendar Constant "
					+ "(one of second, minute, hour, day, week, month, year), and a multiplier for the constant.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved intervals",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiInterval.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods - Interval Methods"}
	)
	public Response getIntervals()
			throws DbException
	{
		try (IntervalDAI dai = getLegacyTimeseriesDB().makeIntervalDAO())
		{
			dai.loadAllIntervals();
			List<ApiInterval> intervals = new ArrayList<>();
			for (String code : dai.getValidIntervalCodes())
			{
				Interval intV = IntervalCodes.getInterval(code);
				intervals.add(map(intV));
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(intervals).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve intervals", ex);
		}
	}

	@POST
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Operation(
			summary = "Create a new, or update an existing Time Interval",
			description = "Example URL for POST:  \n\n    "
					+ "http://localhost:8080/odcsapi/interval\n\n\n"
					+ "The POST data should contain a single time interval record as described "
					+ "above for the 'intervals' list.   \n\n"
					+ "As with other POST methods, to create a new record, omit the numeric ID.  \n\n"
					+ "To update an existing record, include the 'intervalId'.  \n\n"
					+ "For example, to create a interval 'fortnight', the data could be:\n  "
					+ "```\n  {\n    \"name\": \"fortnight\",\n    \"calConstant\": \"day\",\n    "
					+ "\"calMultilier\": 14\n  }\n  ```\n\nThe returned data structure will be the "
					+ "same as the data passed, except that if this is a new interval the "
					+ "intervalId member will be added.",
			requestBody = @RequestBody(
					description = "Engineering Unit Conversion",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiInterval.class))),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully stored interval",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiInterval.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods - Interval Methods"}
	)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	public Response postInterval(ApiInterval intv)
			throws DbException
	{
		try (IntervalDAI dai = getLegacyTimeseriesDB().makeIntervalDAO())
		{
			Interval interval = map(intv);
			dai.writeInterval(interval);
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(interval)).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to store interval", ex);
		}
	}

	static Interval map(ApiInterval intv)
	{
		Interval ret = new Interval(intv.getName());
		if (intv.getIntervalId() != null)
		{
			ret.setKey(DbKey.createDbKey(intv.getIntervalId()));
		}
		else
		{
			ret.setKey(DbKey.NullKey);
		}
		ret.setCalConstant(str2const(intv.getCalConstant()));
		ret.setCalMultiplier(intv.getCalMultilier());
		return ret;
	}

	static ApiInterval map(Interval intv)
	{
		ApiInterval ret = new ApiInterval();
		if (intv.getKey() != null)
		{
			ret.setIntervalId(intv.getKey().getValue());
		}
		else
		{
			ret.setIntervalId(DbKey.NullKey.getValue());
		}

		ret.setName(intv.getName());
		ret.setCalConstant(IntervalCodes.getCalConstName(intv.getCalConstant()));
		ret.setCalMultilier(intv.getCalMultiplier());
		return ret;
	}

	static int str2const(String s)
	{
		if (s.isEmpty())
			return -1;
		s = s.toUpperCase();
		if (s.charAt(0) == 'H')
			return Calendar.HOUR_OF_DAY;
		else if (s.charAt(0) == 'D')
			return Calendar.DAY_OF_MONTH;
		else if (s.charAt(0) == 'W')
			return Calendar.WEEK_OF_YEAR;
		else if (s.charAt(0) == 'Y')
			return Calendar.YEAR;
		else if (s.startsWith("MI"))
			return Calendar.MINUTE;
		else if (s.startsWith("MO"))
			return Calendar.MONTH;
		else return -1;
	}

	@DELETE
	@Path("interval")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete an existing Time Interval record.",
			description = "Example URL for DELETE:  \n\n    "
					+ "http://localhost:8080/odcsapi/interval?intervalid=1459\n\n\n"
					+ "This deletes the Time Interval with ID 1459.  \n\n"
					+ "**Use care with this method**. The system needs to know about all of the 'interval' "
					+ "and 'duration' specifiers used for time series IDs. \n\n"
					+ "Deletion of intervals is currently unsupported!",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted the interval"),
					@ApiResponse(responseCode = "400", description = "Invalid parameters"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods - Interval Methods"}
	)
	public Response deleteInterval(@Parameter(description = "ID of the interval to delete.", required = true,
				schema = @Schema(implementation = Long.class), example = "1459")
		@QueryParam("intvid") Long intvId)
			throws MissingParameterException
	{
		if (intvId == null)
		{
			throw new MissingParameterException("Missing required intvid parameter.");
		}
		throw new UnsupportedOperationException("Deletion of intervals is not implemented");
	}

	@GET
	@Path("tsgrouprefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "Time Series Methods - Groups", description = "Time Series Groups are used to define a "
			+ "set of time series identifiers")
	@Operation(
			summary = "Provide a list of all groups defined in the database.",
			description = "Time Series Groups are used to define a set of time series identifiers. "
					+ "Groups can contain:\n  \n*  Explicit list of time series identifiers  \n"
					+ "*  A list of attributes to flexibly define a set of time series identifiers, "
					+ "E.g. All time series at a particular with interval '30minutes'.  \n"
					+ "*  A list of sub-groups that can be included, excluded, "
					+ "or intersected with the group being defined.\n  \n"
					+ "***\n  \nExample URL:  \n\n    http://localhost:8080/odcsapi/tsgrouprefs\n\n"
					+ "The returned list has the following structure:\n  \n```\n  [\n    {\n      "
					+ "\"groupId\": 8,\n      \"groupName\": \"topgroup\",\n      \"groupType\": \"basin\",\n      "
					+ "\"description\": \"\"\n    },\n    {\n      \"groupId\": 7,\n      "
					+ "\"groupName\": \"subgroup-x\",\n      \"groupType\": \"data type\",\n      "
					+ "\"description\": \"testing for OPENDCS-15 issue\"\n    },\n    {\n      "
					+ "\"groupId\": 2,\n      \"groupName\": \"regtest_017\",\n      "
					+ "\"groupType\": \"data-type\",\n      \"description\": \"Group for regression test 017\"\n    },"
					+ "\n    {\n      \"groupId\": 3,\n      \"groupName\": \"stageRate1Var\",\n      "
					+ "\"groupType\": \"basin\",\n      \"description\": \"Collection of TS IDs with stage "
					+ "to flow ratings\"\n    }\n  ]\n\n```\n\n  ",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved time series group references",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiTsGroupRef.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods - Groups"}
	)
	public Response getTsGroupRefs () throws DbException
	{
		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			return Response.status(HttpServletResponse.SC_OK)
					.entity(mapRef(dai.getTsGroupList(null))).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series group references", ex);
		}
	}

	static ArrayList<ApiTsGroupRef> mapRef(ArrayList<TsGroup> groups)
	{
		ArrayList<ApiTsGroupRef> ret = new ArrayList<>();
		for(TsGroup group : groups)
		{
			ApiTsGroupRef ref = new ApiTsGroupRef();
			if (group.getGroupId() != null)
			{
				ref.setGroupId(group.getGroupId().getValue());
			}
			else
			{
				ref.setGroupId(DbKey.NullKey.getValue());
			}
			ref.setGroupName(group.getGroupName());
			ref.setDescription(group.getDescription());
			ref.setGroupType(group.getGroupType());
			ret.add(ref);
		}
		return ret;
	}

	@GET
	@Path("tsgroup")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Provide a complete definition of a single group.",
			description = "Example URL:  \n\n    http://localhost:8080/odcsapi-0-7/tsgroup?groupid=9\n\n"
					+ "The returned list has the following structure:  \n  \n```\n  {\n    \"groupId\": 9,\n    "
					+ "\"groupName\": \"junk\",\n    \"groupType\": \"basin\",\n    \"description\": \"\",\n    "
					+ "\"tsIds\": [\n      {\n        \"uniqueString\": \"OKVI4.Stage.Inst.15Minutes.0.raw\",\n        "
					+ "\"key\": 1,\n        \"description\": null,\n        \"storageUnits\": \"ft\",\n        "
					+ "\"active\": true\n      },\n      {\n        \"uniqueString\": \"OKVI4.Stage.Ave.1Day.1Day.CO\","
					+ "\n        \"key\": 2,\n        \"description\": null,\n        \"storageUnits\": \"ft\",\n        "
					+ "\"active\": true\n      }\n    ],\n    \"includeGroups\": [\n      {\n        \"groupId\": 1,"
					+ "\n        \"groupName\": \"MROI4-ROWI4-HG\",\n        \"groupType\": \"basin\",\n        "
					+ "\"description\": \"This is a group for the MROI4-ROWI4-HG Regression Test\"\n      }\n    ],"
					+ "\n    \"excludeGroups\": [\n      {\n        \"groupId\": 2,\n        "
					+ "\"groupName\": \"regtest_017\",\n        \"groupType\": \"data-type\",\n        "
					+ "\"description\": \"Group for regression test 017\"\n      }\n    ],\n    "
					+ "\"intersectGroups\": [\n      {\n        \"groupId\": 7,\n        "
					+ "\"groupName\": \"subgroup-x\",\n        \"groupType\": \"data type\",\n        "
					+ "\"description\": \"testing for OPENDCS-15 issue\"\n      }\n    ],\n    "
					+ "\"groupAttrs\": [\n      \"BaseLocation=TESTSITE2\",\n      \"BaseParam=ELEV\",\n      "
					+ "\"BaseVersion=DCP\",\n      \"Duration=0\",\n      \"Interval=1Hour\",\n      "
					+ "\"ParamType=Inst\",\n      \"SubLocation=Spillway2-Gate1\",\n      \"SubParam=PZ1B\",\n      "
					+ "\"SubVersion=Raw\",\n      \"Version=DCP-Raw\"\n    ],\n    \"groupSites\": [\n      "
					+ "{\n        \"siteId\": 2,\n        \"sitenames\": {\n          "
					+ "\"CWMS\": \"ROWI4\",\n          \"USGS\": \"05449500\"\n        },\n        "
					+ "\"publicName\": \"IOWA RIVER NEAR ROWAN\",\n        "
					+ "\"description\": \"IOWA RIVER NEAR ROWAN 4NW\"\n      }\n    ],\n    "
					+ "\"groupDataTypes\": [\n      {\n        \"id\": 224,\n        "
					+ "\"standard\": \"CWMS\",\n        \"code\": \"ELEV-PZ2A\",\n        "
					+ "\"displayName\": \"CWMS:ELEV-PZ2A\"\n      }\n    ]\n  }\n\n```\n  \n"
					+ "**Notes**:  \n*  **tsIds** is a list of explicit time series identifiers "
					+ "that are considered part of the group.  \n*  **includedGroups** is a list of "
					+ "subgroups to be included in this group.  \n*  **excludedGroups** is a list of subgroups. "
					+ "The TSIDs in the subgroup will be excluded from this group.  \n"
					+ "*  **intersectedGroups** is a list of subgroups to be intersected with this group. "
					+ "Only TSIDs in both groups are considered part of this group.  \n"
					+ "*  **groupSites** is a list of Site records. TSIDs in these Sites are "
					+ "considered members of this group.  \n*  **groupDataTypes** is a list of fully-specified "
					+ "data types (a.k.a. 'Param' in CWMS and OpenTSDB databases). TSIDs with a matching data "
					+ "type will be included in the group.  \n*  **groupAttrs** is a list of attributes "
					+ "that are used to define the group. These are presented in 'name=value' pairs where "
					+ "the name is one of the following:  \n\n    *  **BaseLocation** – only the first "
					+ "part of Site (Location) before first hyphen  \n    *  **SubLocation** – only trailing "
					+ "part of Site after first hyphen.  \n    *  **BaseParam** – only first part of data "
					+ "type (Param) before first hyphen  \n    *  **SubParam** – only trailing part of data "
					+ "type (Param) after first hyphen\n    *  **ParamType**  \n    *  **Interval**\n    "
					+ "*  **Duration**  \n    *  **Version**  \n    *  **BaseVersion**\n    *  **SubVersion**",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved time series group details",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiTsGroup.class))),
					@ApiResponse(responseCode = "400", description = "Invalid or missing group ID"),
					@ApiResponse(responseCode = "404", description = "Time series group not found"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"Time Series Methods - Groups"}
	)
	public Response getTsGroup (@Parameter(description = "Requested group id", required = true,
			schema = @Schema(implementation = Long.class), example = "9")
		@QueryParam("groupid") Long groupId)
			throws WebAppException, DbException
	{
		if (groupId == null)
		{
			throw new MissingParameterException("Missing required groupid parameter.");
		}
		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			TsGroup group = dai.getTsGroupById(DbKey.createDbKey(groupId));
			if (group == null)
			{
				throw new DatabaseItemNotFoundException("Time series group with ID=" + groupId + " not found");
			}
			return Response.status(HttpServletResponse.SC_OK)
					.entity(map(group)).build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to retrieve time series group by ID", ex);
		}
	}

	static ApiTsGroup map(TsGroup group)
	{
		if (group == null)
		{
			return null;
		}
		ApiTsGroup ret = new ApiTsGroup();
		ret.setGroupName(group.getGroupName());
		ret.setDescription(group.getDescription());
		ret.setGroupType(group.getGroupType());
		if (group.getGroupId() != null)
		{
			ret.setGroupId(group.getGroupId().getValue());
		}
		else
		{
			ret.setGroupId(DbKey.NullKey.getValue());
		}
		ret.getIntersectGroups().addAll(mapRef(group.getIntersectedGroups()));
		List<ApiTimeSeriesIdentifier> tsids = new ArrayList<>();
		for (TimeSeriesIdentifier tsid : group.getTsMemberList())
		{
			tsids.add(map(tsid));
		}
		ret.getTsIds().addAll(tsids);
		List<ApiSiteRef> sites = new ArrayList<>();
		for (int i = 0; i < group.getSiteNameList().size(); i++)
		{
			ApiSiteRef site = new ApiSiteRef();
			site.setSiteId(group.getSiteIdList().get(i).getValue());
			site.setPublicName(group.getSiteNameList().get(i));
			sites.add(site);
		}
		ret.getGroupSites().addAll(sites);
		List<ApiDataType> dts = new ArrayList<>();
		for (DbKey dtid : group.getDataTypeIdList())
		{
			ApiDataType dt = new ApiDataType();
			dt.setId(dtid.getValue());
			dts.add(dt);
		}
		ret.getGroupDataTypes().addAll(dts);
		ret.getIncludeGroups().addAll(mapRef(group.getIncludedSubGroups()));
		ret.getExcludeGroups().addAll(mapRef(group.getExcludedSubGroups()));
		return ret;
	}

	@POST
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create a new, or update an existing time series group",
			description = "Example URL for POST:  \n\n    "
					+ "http://localhost:8080/odcsapi/tsgroup\n\n"
					+ "The POST data is as described above for GET tsgroup",
			requestBody = @RequestBody(
					description = "Time Series Group",
					required = true,
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiTsGroup.class),
					examples = {
							@ExampleObject(name = "Basic", value = ResourceExamples.TsGroupExamples.BASIC),
							@ExampleObject(name = "Verbose", value = ResourceExamples.TsGroupExamples.VERBOSE)
					})
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created the time series group",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiTsGroup.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"Time Series Methods - Groups"}
	)
	public Response postTsGroup (ApiTsGroup grp) throws DbException
	{
			try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
			{
				TsGroup group = map(grp);
				dai.writeTsGroup(group);
				return Response.status(HttpServletResponse.SC_OK)
						.entity(map(group)).build();
			}
			catch (DbIoException | BadTimeSeriesException ex)
			{
				throw new DbException("Unable to store time series group", ex);
			}
	}

	static TsGroup map(ApiTsGroup grp) throws BadTimeSeriesException
	{
		TsGroup ret = new TsGroup();
		ret.setDescription(grp.getDescription());
		ret.setGroupName(grp.getGroupName());
		ret.setGroupType(grp.getGroupType());
		ret.setIntersectedGroups(map(grp.getIntersectGroups()));
		if (grp.getGroupId() != null)
		{
			ret.setGroupId(DbKey.createDbKey(grp.getGroupId()));
		}
		else
		{
			ret.setGroupId(DbKey.NullKey);
		}
		for (ApiTimeSeriesIdentifier ident : grp.getTsIds())
		{
			ret.addTsMember(map(ident));
		}
		for (ApiSiteRef site : grp.getGroupSites())
		{
			ret.addSiteId(DbKey.createDbKey(site.getSiteId()));
			ret.addSiteName(site.getPublicName());
		}
		for (ApiDataType dt : grp.getGroupDataTypes())
		{
			ret.addDataTypeId(DbKey.createDbKey(dt.getId()));
		}
		for (ApiTsGroupRef include : grp.getIncludeGroups())
		{
			TsGroup inc = new TsGroup();
			inc.setGroupName(include.getGroupName());
			inc.setDescription(include.getDescription());
			inc.setGroupId(DbKey.createDbKey(include.getGroupId()));
			inc.setGroupType(include.getGroupType());

			ret.addSubGroup(inc,'A');
		}
		for (ApiTsGroupRef exclude : grp.getExcludeGroups())
		{
			TsGroup exc = new TsGroup();
			exc.setGroupName(exclude.getGroupName());
			exc.setDescription(exclude.getDescription());
			exc.setGroupId(DbKey.createDbKey(exclude.getGroupId()));
			exc.setGroupType(exclude.getGroupType());

			ret.addSubGroup(exc,'S');
		}
		return ret;
	}

	static TimeSeriesIdentifier map(ApiTimeSeriesIdentifier tsid) throws BadTimeSeriesException
	{
		TimeSeriesIdentifier ret = new CwmsTsId();
		if (tsid.getKey() != null)
		{
			ret.setKey(DbKey.createDbKey(tsid.getKey()));
		}
		else
		{
			ret.setKey(DbKey.NullKey);
		}
		ret.setUniqueString(tsid.getUniqueString());
		ret.setDescription(tsid.getDescription());
		ret.setStorageUnits(tsid.getStorageUnits());
		return ret;
	}

	static ArrayList<TsGroup> map(List<ApiTsGroupRef> groupRefs)
	{
		ArrayList<TsGroup> ret = new ArrayList<>();
		for(ApiTsGroupRef ref : groupRefs)
		{
			TsGroup group = new TsGroup();
			group.setGroupName(ref.getGroupName());
			group.setDescription(ref.getDescription());
			group.setGroupType(ref.getGroupType());
			if (ref.getGroupId() != null)
			{
				group.setGroupId(DbKey.createDbKey(ref.getGroupId()));
			}
			else
			{
				group.setGroupId(DbKey.NullKey);
			}
			ret.add(group);
		}
		return ret;
	}

	@DELETE
	@Path("tsgroup")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Time Series Group",
			description = "Example URL for DELETE:  \n\n    "
					+ "http://localhost:8080/odcsapi/delete\n\n"
					+ "This example deletes the Time series group with ID 9.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted time series group"),
					@ApiResponse(responseCode = "400", description = "Missing or invalid group ID"),
					@ApiResponse(responseCode = "500", description = "Database error occurred")
			},
			tags = {"Time Series Methods - Groups"}
	)
	public Response deleteTsGroup (@Parameter(description = "Group Id to delete", required = true, example = "9",
			schema = @Schema(implementation = Long.class))
		@QueryParam("groupid") Long groupId)
			throws WebAppException, DbException
	{
		if (groupId == null)
		{
			throw new MissingParameterException("Missing required groupid parameter.");
		}

		try (TsGroupDAI dai = getLegacyTimeseriesDB().makeTsGroupDAO())
		{
			dai.deleteTsGroup(DbKey.createDbKey(groupId));
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("tsgroup with ID=" + groupId + " deleted").build();
		}
		catch (DbIoException ex)
		{
			throw new DbException("Unable to delete time series group", ex);
		}
	}
}
