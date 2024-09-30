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

import java.sql.SQLException;
import java.util.logging.Logger;

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

import org.opendcs.odcsapi.beans.ApiUnit;
import org.opendcs.odcsapi.beans.ApiUnitConverter;
import org.opendcs.odcsapi.dao.ApiUnitDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

/**
 * HTTP Resources relating to DataTypes, Engineering Units, and Conversions
 * @author mmaloney
 *
 */
@Path("/")
public class DatatypeUnitResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datatypelist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getDataTypeList(@QueryParam("standard") String std) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getDataTypeList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getDataTypeList(std));
		}
	}


	@GET
	@Path("unitlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getUnitList() throws DbException
	{
		
		Logger.getLogger(ApiConstants.loggerName).fine("getUnitList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getUnitList());
		}
	}
	
	@POST
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postEU(@QueryParam("fromabbr") String fromabbr, ApiUnit eu)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postEU abbr=" + eu.getAbbr()
			+ ", fromabbr=" + fromabbr);
		
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.writeEU(eu, fromabbr);
			return ApiHttpUtil.createResponse("{\"message\": \"The Engineering Unit was Saved successfully.\"}");
		}
	}

	@DELETE
	@Path("eu")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteEU(@QueryParam("abbr") String abbr) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE EU abbr=" + abbr);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.deleteEU(abbr);
			return ApiHttpUtil.createResponse("EU with abbr " + abbr + " deleted");
		}
	}

	@GET
	@Path("euconvlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getUnitConvList() throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getUnitConvList");
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getConverterList(false));
		}
	}
	
	@POST
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postEUConv(ApiUnitConverter euc) throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("postEUConv from=" 
			+ euc.getFromAbbr() + ", to=" + euc.getToAbbr());
		
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.writeEUConv(euc);
			return ApiHttpUtil.createResponse("EUConv Saved");
		}
	}

	@DELETE
	@Path("euconv")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteEUConv(@QueryParam("euconvid") Long id) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE EUConverter id=" + id);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiUnitDAO dao = new ApiUnitDAO(dbi))
		{
			dao.deleteEUConv(id);
			return ApiHttpUtil.createResponse("EUConv with id=" + id + " deleted");
		}
	}
}
