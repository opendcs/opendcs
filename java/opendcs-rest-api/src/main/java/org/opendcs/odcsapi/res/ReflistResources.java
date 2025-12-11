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
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;

import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import io.swagger.v3.oas.annotations.tags.Tag;
import opendcs.dai.EnumDAI;
import org.opendcs.odcsapi.beans.ApiRefList;
import org.opendcs.odcsapi.beans.ApiRefListItem;
import org.opendcs.odcsapi.beans.ApiSeason;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

/**
 * HTTP Resources relating to reference lists and seasons
 * @author mmaloney
 *
 */
@Path("/")
public final class ReflistResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	private static final String SEASON_ENUM = "season";

	@GET
	@Path("reflists")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Reference Lists", description = "Reference lists are used in OpenDCS to populate pulldown lists "
			+ "and extend the functionality of the software.")
	@Operation(
			summary = "The ‘reflists’ GET method will return all reference lists or a specific reference list.",
			description = "The 'name' argument may have multiple values.   For example:    \n\n"
					+ "`http://localhost:8080/odcsapi/reflists?name=scripttype,dataorder`  \n\n"
					+ "If no 'name' argument is provided, then all reference lists in the database are returned.   "
					+ "The JSON returned is an array of reference lists.  \n          \n"
					+ "The following reference lists are currently available:  \n"
					+ "* **DataSourceType** – Used in Database Editor to link a data source name to a Java class  \n"
					+ "* **PortType** – Used in the polling interface \n" + "        "
					+ "* **ScriptType** – A list of DECODES Platform Configuration Script types (reserved for future use  \n"
					+ "* **StatisticsCode** – Valid statistics codes that can be used in a time series identifier  \n"
					+ "* **SiteNameType** – Known site name types, e.g. NWSHB5 (National Weather Service Handbook 5), "
					+ "USGS (Site Number), CODWR (Colorada Dept of Water Resources).  \n"
					+ "* **DataConsumer** – Links a data consumer type name to Java Code (e.g. File, Directory, Socket)  \n"
					+ "* **Measures** – A list of physical attributes measured by an "
					+ "engineering unit (e.g. force, temperature, mass)  \n"
					+ "* **UnitConversionAlgorithm** – A list of algorithms for unit conversion  \n"
					+ "* **DataOrder** – Ascending (oldest first) or Descending (newest first) time order  \n"
					+ "* **GroupType** – Used for annotation in time series groups  \n"
					+ "* **LoggerType** – Used in the polling interface  \n"
					+ "* **LookupAlgorithm**  \n"
					+ "* **EquationScope**  \n"
					+ "* **UnitFamily** – Metric, English, or Universal  \n"
					+ "* **OutputFormat** – Links a name to Java code for formatting output data (e.g. SHEF, CSV)  \n"
					+ "* **DataTypeStandard** – Known standards for specifying data type (e.g. SHEF-PE, CWMS, HDB)  \n"
					+ "* **TransportMediumType** – A Transport Medium uniquely identifies a platform. There are several "
					+ "types: GOES ID, Iridium IMEI, Polled Identifier, etc.  \n"
					+ "* **Season** – User can create any number of seasons that start/end at specified time of year  \n"
					+ "* **EquipmentType**  \n"
					+ "* **ApplicationType** – Computation Process, Comp Depends Daemon, DECODES Routing Scheduler, etc.  \n"
					+ "* **RecordingMode**",
			responses = {
					@ApiResponse(
							responseCode = "200",
							description = "If no 'name' argument is provided, "
									+ "then all reference lists in the database are returned.\n"
									+ "            The JSON returned is an map of reference names to reference objects.",
							content = @Content(
									mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(type = "object",
										additionalProperties = Schema.AdditionalPropertiesValue.TRUE,
										properties = {
											@StringToClassMapItem(key = "string", value = ApiRefList.class)
									})
							)
					),
					@ApiResponse(responseCode = "404", description = "Matching reference lists not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response getRefLists(@Parameter(description = "List of requested Ref Lists. Comma Seperated",
			schema = @Schema(implementation = String.class, example = "scripttype,dataorder"))
		@QueryParam("name") String listNames)
			throws DbException, WebAppException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			EnumList returnedEnums = new EnumList();
			dbIo.readEnumList(returnedEnums);
			HashMap<String, ApiRefList> ret = new HashMap<>();
			for (DbEnum enumVal : returnedEnums.getEnumList())
			{
				if (enumVal.enumName.equalsIgnoreCase("season enum")
						|| enumVal.enumName.equalsIgnoreCase(SEASON_ENUM))
				{
					continue;
				}
				ApiRefList refList = new ApiRefList();
				refList.setReflistId(enumVal.getId().getValue());
				refList.setDescription(enumVal.getDescription());
				refList.setEnumName(enumVal.getUniqueName());
				refList.setDefaultValue(enumVal.getDefault());
				Map<String, ApiRefListItem> items = new HashMap<>();
				for (EnumValue val: enumVal.values())
				{
					ApiRefListItem refListItem = new ApiRefListItem();
					refListItem.setDescription(val.getDescription());
					refListItem.setValue(val.getValue());
					refListItem.setSortNumber(val.getSortNumber());
					items.put(val.getValue(), refListItem);
				}
				refList.setItems(items);
				ret.put(enumVal.enumName, refList);
			}

			ArrayList<String> searches = getSearchTerms(listNames);
			if (!searches.isEmpty())
			{
				ArrayList<String> toRm = new ArrayList<>();
				nextName:
				for(String rlname : ret.keySet())
				{
					for(String term : searches)
					{
						if(rlname.equalsIgnoreCase(term))
						{
							continue nextName;
						}
					}
					toRm.add(rlname);
				}
				for(String rm : toRm)
				{
					ret.remove(rm);
				}
				if (ret.isEmpty())
				{
					throw new DatabaseItemNotFoundException("No reference lists found matching search criteria");
				}
			}

			return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to retrieve reference lists", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	@POST
	@Path("reflist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Create New Reference List, or Overwrite Existing Reference List",
			description = "The ‘reflist’ POST method takes a single network "
					+ "list in JSON format. Note that the above GET method has a plural ‘reflists’ and returns "
					+ "an array of named reference lists. This POST method has singular ‘reflist’. "
					+ "The POST body should be a single reference list, not an array of lists.  \n\n"
					+ "For creating a new reference list, leave reflistId out of the passed data structure.  \n\n"
					+ "For overwriting an existing one, include the reflistId that was previously returned. "
					+ "The network list in the database is replaced with the one sent.",
			requestBody = @RequestBody(description = "Reference list object to post", required = true,
				content = @Content(mediaType = MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ApiRefList.class),
					examples = {
						@ExampleObject(name = "Basic", value = ResourceExamples.ReflistExamples.BASIC),
						@ExampleObject(name = "New", value = ResourceExamples.ReflistExamples.NEW),
						@ExampleObject(name = "Update", value = ResourceExamples.ReflistExamples.UPDATE)
					})),
			responses = {
					@ApiResponse(description = "Reference list created successfully", responseCode = "201",
						content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiRefList.class))),
					@ApiResponse(description = "Internal Server Error", responseCode = "500")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response postRefList(@Parameter(description = "JSON definition of new or updated reflist") ApiRefList reflist)
			throws DbException
	{
		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbEnum dbEnum = mapToEnum(reflist);
			dai.writeEnum(dbEnum);

			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(dbEnum))
					.build();
		}
		catch( DatabaseException | DbIoException ex)
		{
			throw new DbException("Unable to write reference list", ex);
		}
	}

	static DbEnum mapToEnum(ApiRefList refList) throws DatabaseException
	{
		DbEnum ev = new DbEnum(refList.getEnumName());
		ev.setDescription(refList.getDescription());
		ev.setDefault(refList.getDefaultValue());
		if (refList.getReflistId() != null)
		{
			ev.setId(DbKey.createDbKey(refList.getReflistId()));
		}
		else
		{
			ev.setId(DbKey.NullKey);
		}
		for (Map.Entry<String, ApiRefListItem> itemMap : refList.getItems().entrySet())
		{
			EnumValue val = new EnumValue(ev, itemMap.getValue().getValue());
			val.setDescription(itemMap.getValue().getDescription());
			val.setSortNumber(itemMap.getValue().getSortNumber());
			val.setExecClassName(itemMap.getValue().getExecClassName());
			val.setEditClassName(itemMap.getValue().getEditClassName());
			ev.addValue(val);
		}
		return ev;
	}

	static ApiRefList map(DbEnum dbEnum)
	{
		ApiRefList ret = new ApiRefList();
		HashMap<String, ApiRefListItem> items = new HashMap<>();
		ret.setEnumName(dbEnum.getUniqueName());
		ret.setDefaultValue(dbEnum.getDefault());
		ret.setDescription(dbEnum.getDescription());
		ret.setReflistId(dbEnum.getId().getValue());
		for (EnumValue val : dbEnum.values())
		{
			ApiRefListItem item = new ApiRefListItem();
			item.setDescription(val.getDescription());
			item.setValue(val.getValue());
			item.setExecClassName(val.getExecClassName());
			item.setEditClassName(val.getEditClassName());
			item.setSortNumber(val.getSortNumber());
			items.put(dbEnum.getUniqueName(), item);
		}

		ret.setItems(items);
		return ret;
	}

	@DELETE
	@Path("reflist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing Reference List",
			description = "Required argument reflistid must be passed.  \n\n"
					+ "Take care in deleting reference lists. "
					+ "Several modules within OpenDCS require the existence of certain lists.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Reference list deleted successfully"),
					@ApiResponse(responseCode = "400", description = "Missing Parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response deleteRefList(@Parameter(description = "Requested Reference List to Delete",
			required = true, schema = @Schema(implementation = Long.class))
		@QueryParam("reflistid") Long reflistId)
			throws DbException, WebAppException
	{
		if (reflistId == null)
		{
			throw new MissingParameterException("Provide 'reflistid' argument to delete a reference list.");
		}

		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			dai.deleteEnumList(DbKey.createDbKey(reflistId));
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("reflist with ID " + reflistId + " deleted").build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to delete reference list", ex);
		}
	}

	@GET
	@Path("seasons")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Seasons are used in various places in OpenDCS, usually to specify some type of conditional processing",
			description = "Seasons are denoted by an abbreviation, a full name, start date/time, end date/time, and an optional time zone."
					+ " Seasons are used in various places in OpenDCS, usually to specify some type of conditional processing."
					+ " For example, a platform water-level sensor may be disabled during a winter period because the river is likely to be covered in ice.\n"
					+ "* The abbreviation should be a single alpha-numeric word with no embedded spaces.\n"
					+ "* The name may contain spaces.\n"
					+ "* Start and End date/time are strings in the format MM/dd-HH:mm. They specify a date and time within a year.\n"
					+ "* If time zone is omitted, local time on the server is assumed.\n"
					+ "The GET seasons method is called with a URL like the following:",
			responses = {
					@ApiResponse(responseCode = "200", description = "A list of seasons defined on the server is returned",
						content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ApiSeason.class))
						)
					),
					@ApiResponse(responseCode = "404", description = "Season reference list not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response getSeasons() throws DbException, WebAppException
	{
		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbEnum dbEnum = dai.getEnum(SEASON_ENUM);
			if (dbEnum == null)
			{
				throw new DatabaseItemNotFoundException("Season enum not found");
			}
			return Response.status(HttpServletResponse.SC_OK).entity(mapSeasons(dbEnum)).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException("Unable to retrieve seasons", ex);
		}
	}

	static ArrayList<ApiSeason> mapSeasons(DbEnum seasons)
	{
		ArrayList<ApiSeason> ret = new ArrayList<>();
		for (EnumValue val : seasons.values())
		{
			ApiSeason as = new ApiSeason();
			as.setName(val.getDescription());
			as.setAbbr(val.getValue());
			String[] startEndTZ = val.getEditClassName().split(" ");
			as.setStart(startEndTZ[0]);
			as.setEnd(startEndTZ[1]);
			as.setSortNumber(val.getSortNumber());
			if (startEndTZ.length > 2)
			{
				as.setTz(startEndTZ[2]);
			}
			ret.add(as);
		}
		return ret;
	}

	@GET
	@Path("season")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Return a single season data structure ",
			description = "Instead of a list of seasons, the returned data is a single season data structure:  ",
			operationId = "getseason",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success", content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiSeason.class))
					),
					@ApiResponse(responseCode = "400", description = "Missing Parameter"),
					@ApiResponse(responseCode = "404", description = "Season not found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response getSeason(@Parameter(description = "Abbreviation of existing season to retrieve",
			required = true, schema = @Schema(implementation = String.class))
		@QueryParam("abbr") String abbr)
			throws DbException, WebAppException
	{
		if (abbr == null)
		{
			throw new MissingParameterException("Missing required 'abbr' argument.");
		}
		try (EnumDAI dai  = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbKey seasonId = dai.getEnumId(SEASON_ENUM);
			EnumValue seasonVal = dai.getEnumValue(seasonId, abbr);

			return Response.status(HttpServletResponse.SC_OK).entity(map(seasonVal)).build();
		}
		catch(DbIoException ex)
		{
			if (ex.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException("Unable to retrieve season: matching season not found", ex);
			}
			throw new DbException(String.format("Unable to retrieve season with abbreviation: %s", abbr), ex);
		}
	}

	static ApiSeason map(EnumValue season)
	{
		if (season == null)
		{
			return null;
		}
		ApiSeason ret = new ApiSeason();
		ret.setAbbr(season.getValue());
		ret.setName(season.getDescription());
		String[] startEndTZ = season.getEditClassName().split(" ");
		ret.setStart(startEndTZ[0]);
		ret.setEnd(startEndTZ[1]);
		if (startEndTZ.length > 2)
		{
			ret.setTz(startEndTZ[2]);
		}
		return ret;
	}

	@POST
	@Path("season")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Creates or overwrites a single season record",
			description = "It takes a data structure like the one described above for GET season.",
			requestBody = @RequestBody(
					description = "Season Object",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiSeason.class)
					)
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Season successfully created or updated",
						content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiSeason.class))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response postSeason(@Parameter(description = "Details of the new or updated season", required = true) ApiSeason season)
		throws DbException
	{
		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbEnum dbEnum = dai.getEnum(SEASON_ENUM);
			DbKey dbSeasonId;
			if (dbEnum == null)
			{
				DbEnum seasonEnum = new DbEnum(SEASON_ENUM);
				seasonEnum.setDescription("Seasons for conditional processing");
				dai.writeEnum(seasonEnum);
				dbSeasonId = seasonEnum.getId();
			}
			else
			{
				dbSeasonId = dbEnum.getId();
			}
			String fromabbr = season.getFromabbr();
			if (fromabbr != null)
			{
				dai.deleteEnumValue(dbSeasonId, fromabbr);
			}
			EnumValue dbSeason = map(season, dbEnum);
			dai.writeEnumValue(dbSeasonId, dbSeason, null, season.getSortNumber());
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(dbSeason))
					.build();
		}
		catch(DbIoException ex)
		{
			throw new DbException(String.format("Unable to write season with abbreviation: %s", season.getAbbr()), ex);
		}
	}

	static EnumValue map(ApiSeason season, DbEnum dbEnum)
	{
		EnumValue ret = new EnumValue(dbEnum, season.getAbbr());
		ret.setDescription(season.getName());
		String startEndTz = String.format("%s %s", season.getStart(), season.getEnd());
		if (season.getTz() != null)
		{
			startEndTz += " " + season.getTz();
		}
		ret.setEditClassName(startEndTz);
		ret.setSortNumber(season.getSortNumber());
		return ret;
	}

	@DELETE
	@Path("season")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing Season",
			description = "The DELETE season method requires an argument"
					+ " 'abbr' corresponding to the season abbreviation.  \n\n"
					+ "For example, to DELETE the 'autumn' season, use the following URL:\n  \n"
					+ "    http://localhost:8080/odcsapi/season?abbr=autumn",
			responses = {
					@ApiResponse(responseCode = "204", description = "Season was deleted successfully"),
					@ApiResponse(responseCode = "400", description = "Missing Parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Reference Lists"}
	)
	public Response deleteSeason(@Parameter(description = "Abbreviation of the season to delete", required = true,
			schema = @Schema(implementation = String.class), example = "autumn")
		@QueryParam("abbr") String abbr)
			throws WebAppException, DbException
	{
		if (abbr == null)
		{
			throw new MissingParameterException("Provide 'abbr' argument to delete a season.");
		}

		// Use username and password to attempt to connect to the database
		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbKey enumKey = dai.getEnumId(SEASON_ENUM);
			dai.deleteEnumValue(enumKey, abbr);
			return Response.status(HttpServletResponse.SC_NO_CONTENT).entity("Deleted season " + abbr).build();
		}
		catch(DbIoException ex)
		{
			throw new DbException(String.format("Unable to delete season with abbreviation: %s", abbr), ex);
		}
	}

	/**
	 * Passed a list like ("one", "two", "three"), return the quoted strings as an array list.
	 * Parentheses may also be square brackets.
	 * Parentheses may be omitted. E.g. one,two,three
	 * Terms are optionally enclosed in quotes.
	 * @param theArg the argument string to convert into an array
	 * @return ArrayList of search terms. Empty if empty string.
	 */
	private ArrayList<String> getSearchTerms(String theArg)
	{
		ArrayList<String> ret = new ArrayList<>();

		if (theArg == null)
			return ret;
		theArg = theArg.trim();
		if (theArg.isEmpty())
			return ret;

		if (theArg.charAt(0) == '[' || theArg.charAt(0) == '(')
		{
			theArg = theArg.substring(1);
			int idx = findCloseBracket(theArg);
			if (idx < theArg.length())
				theArg = theArg.substring(0, idx);
		}

		// Now parse into quoted strings
		int start = 0;
		while(start < theArg.length())
		{
			char c = theArg.charAt(start);
			if (c == '"')
			{
				int end = start+1;
				boolean escaped = false;
				while(end < theArg.length() &&
						(theArg.charAt(end) != '"' || escaped))
				{
					escaped = !escaped && theArg.charAt(end) == '\\';
					end++;
				}
				ret.add(theArg.substring(start+1, end).toLowerCase());
				start = end+1;
			}
			else if (Character.isLetterOrDigit(c)||c=='-'||c=='+')
			{
				int end = start+1;
				boolean escaped = false;
				while(end < theArg.length() && (theArg.charAt(end) != ',' || escaped))
				{
					escaped = !escaped && theArg.charAt(end) == '\\';
					end++;
				}
				ret.add(theArg.substring(start, end).toLowerCase());
				start = end;
			}
			else
			{
				if (c == ',' || c == ' ' || c == '\t')
					start++;
				else
				{
					return ret;
				}
			}
		}

		return ret;
	}

	/**
	 * Pass string that has been trimmed to start just AFTER the open bracket.
	 * @param arg the string to search for the close bracket
	 * @return the index of the close bracket or length of string if not found.
	 */
	private int findCloseBracket(String arg)
	{
		int level = 1;
		int idx = 0;
		for(; idx < arg.length() && level > 0; idx++)
		{
			char c = arg.charAt(idx);
			if (c == '[' || c == '(')
			{
				level++;
			}
			else if ((c == ']' || c == ')') && --level == 0)
			{
				return idx;
			}
		}
		return idx;
	}

}