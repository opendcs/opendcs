/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

import javax.annotation.security.RolesAllowed;
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

import org.opendcs.odcsapi.beans.ApiRefList;
import org.opendcs.odcsapi.beans.ApiSeason;
import org.opendcs.odcsapi.dao.ApiRefListDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiHttpUtil;

/**
 * HTTP Resources relating to reference lists and seasons
 * @author mmaloney
 *
 */
@Path("/")
public class ReflistResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("reflists")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getRefLists(@QueryParam("name") String listNames) throws DbException
	{
		
		try (DbInterface dbi = new DbInterface(); 
			ApiRefListDAO dao = new ApiRefListDAO(dbi))
		{
			HashMap<String, ApiRefList> ret = dao.getAllRefLists();
			ArrayList<String> searches = getSearchTerms(listNames);
			if (!searches.isEmpty())
			{
				ArrayList<String> toRm = new ArrayList<>();
			nextName:
				for(String rlname : ret.keySet())
				{
					for(String term : searches)
						if (rlname.equalsIgnoreCase(term))
							continue nextName;
					toRm.add(rlname);
				}
				for(String rm : toRm)
					ret.remove(rm);
			}
					
			return ApiHttpUtil.createResponse(ret);
		}
	}
	
	@POST
	@Path("reflist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postRefList(ApiRefList reflist) throws DbException
	{
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO reflistDAO = new ApiRefListDAO(dbi))
		{
			return ApiHttpUtil.createResponse(reflistDAO.writeRefList(reflist));
		}
	}

	@DELETE
	@Path("reflist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleReflist(@QueryParam("reflistid") Long reflistId) throws DbException
	{
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO dao = new ApiRefListDAO(dbi))
		{
			dao.deleteRefList(reflistId);
			return ApiHttpUtil.createResponse("reflist with ID " + reflistId + " deleted");
		}
	}

	@GET
	@Path("seasons")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getSeasons() throws WebAppException, DbException
	{
		try (DbInterface dbi = new DbInterface(); 
			ApiRefListDAO dao = new ApiRefListDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getAllSeasons());
		}
	}

	@GET
	@Path("season")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getSeason(@QueryParam("abbr") String abbr)
		throws WebAppException, DbException
	{
		try (DbInterface dbi = new DbInterface(); 
			ApiRefListDAO dao = new ApiRefListDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getSeason(abbr));
		}
	}

	@POST
	@Path("season")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postSeason(@QueryParam("fromabbr") String fromAbbr, ApiSeason season)
		throws WebAppException, DbException
	{
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO reflistDAO = new ApiRefListDAO(dbi))
		{
			if (fromAbbr != null)
				reflistDAO.deleteSeason(fromAbbr);
			reflistDAO.writeSeason(season);
			return ApiHttpUtil.createResponse(String.format("The season (%s) has been saved successfully", season.getAbbr()));
		}
	}

	@DELETE
	@Path("season")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteSeason(@QueryParam("abbr") String abbr)
		throws WebAppException, DbException
	{
		if (abbr == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Provide 'abbr' argument to delete a season.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiRefListDAO reflistDAO = new ApiRefListDAO(dbi))
		{
			reflistDAO.deleteSeason(abbr);
			return ApiHttpUtil.createResponse("Deleted season + " + abbr);
		}
	}

	
	/**
	 * Passed a list like ("one", "two", "three"), return the quoted strings as an array list.
	 * Parans may also be square brackets.
	 * Parens may be omitted. E.g. one,two,three
	 * Terms are optionally enclosed in quotes.
	 * @param theArg
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
					if (!escaped && theArg.charAt(end) == '\\')
						escaped = true;
					else
						escaped = false;
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
					if (!escaped && theArg.charAt(end) == '\\')
						escaped = true;
					else
						escaped = false;
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
					System.err.println("Parse error in argument '" + theArg + "' at position " + start);
					return ret;
				}
			}
		}
		
		return ret;
	}

	
	/**
	 * Pass string that has been trimmed to start just AFTER the open bracket.
	 * @param arg
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
				level++;
			else if (c == ']' || c == ')')
			{
				if (--level == 0)
					return idx;
			}
		}
		return idx;
	}	

}
