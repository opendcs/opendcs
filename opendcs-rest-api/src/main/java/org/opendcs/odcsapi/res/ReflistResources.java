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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.DbEnum;
import decodes.db.EnumList;
import decodes.db.EnumValue;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
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
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getRefLists(@QueryParam("name") String listNames) throws DbException, WebAppException
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
		catch(DatabaseException e)
		{
			throw new DbException("Unable to retrieve reference lists", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postRefList(ApiRefList reflist) throws DbException
	{
		try (EnumDAI dai = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbEnum dbEnum = mapToEnum(reflist);
			dai.writeEnum(dbEnum);

			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(dbEnum))
					.build();
		}
		catch( DatabaseException | DbIoException e)
		{
			throw new DbException("Unable to write reference list", e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteRefList(@QueryParam("reflistid") Long reflistId) throws DbException, WebAppException
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
		catch(DbIoException e)
		{
			throw new DbException("Unable to delete reference list", e);
		}
	}

	@GET
	@Path("seasons")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
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
		catch(DbIoException e)
		{
			throw new DbException("Unable to retrieve seasons", e);
		}
	}

	static ArrayList<ApiSeason> mapSeasons(DbEnum seasons)
	{
		ArrayList<ApiSeason> ret = new ArrayList<>();
		for (EnumValue val : seasons.values())
		{
			ApiSeason as = new ApiSeason();
			as.setName(val.getFullName());
			as.setAbbr(val.getValue());
			String[] startEndTZ = val.getEditClassName().split(" ");
			as.setStart(startEndTZ[0]);
			as.setEnd(startEndTZ[1]);
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
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getSeason(@QueryParam("abbr") String abbr)
			throws DbException, WebAppException
	{
		try (EnumDAI dai  = getLegacyTimeseriesDB().makeEnumDAO())
		{
			DbKey seasonId = dai.getEnumId(SEASON_ENUM);
			EnumValue seasonVal = dai.getEnumValue(seasonId, abbr);

			return Response.status(HttpServletResponse.SC_OK).entity(map(seasonVal)).build();
		}
		catch(DbIoException e)
		{
			if (e.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException("Unable to retrieve season: matching season not found", e);
			}
			throw new DbException(String.format("Unable to retrieve season with abbreviation: %s", abbr), e);
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response postSeason(@QueryParam("fromabbr") String fromAbbr, ApiSeason season)
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
			if (fromAbbr != null)
			{
				dai.deleteEnumValue(dbSeasonId, fromAbbr);
			}
			EnumValue dbSeason = map(season, dbEnum);
			dai.writeEnumValue(dbSeasonId, dbSeason, fromAbbr, season.getSortNumber());
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(dbSeason))
					.build();
		}
		catch(DbIoException e)
		{
			throw new DbException(String.format("Unable to write season with abbreviation: %s", season.getAbbr()), e);
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

	static DbEnum map(ApiSeason season, String fromAbbr)
	{
		DbEnum ret = new DbEnum(season.getName());
		ret.setDefault(fromAbbr);
		return ret;
	}

	@DELETE
	@Path("season")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteSeason(@QueryParam("abbr") String abbr)
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
		catch(DbIoException e)
		{
			throw new DbException(String.format("Unable to delete season with abbreviation: %s", abbr), e);
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