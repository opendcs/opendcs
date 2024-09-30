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

import java.util.Properties;
import java.util.logging.Logger;

import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.dao.ApiTsDAO;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.opendcs_dep.PropSpecHelper;
import org.opendcs.odcsapi.opendcs_dep.TestDecoder;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.hydrojson.DbInterface;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class OdcsapiResource
{
	@Context HttpHeaders httpHeaders;
	
	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getTsdbProperties() throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getTsdbProperties");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getTsdbProperties());
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postTsdbProperties(Properties props) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post tsdb_properties");
		try (DbInterface dbi = new DbInterface();
			ApiTsDAO dao = new ApiTsDAO(dbi))
		{
			dao.setTsdbProperties(props);;
			return ApiHttpUtil.createResponse(props);
		}
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getPropSpecs(@QueryParam("class") String className)
		throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).info("getPropSpecs class='" + className + "'");
		
		if (className == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required class argument.");
		
		return ApiHttpUtil.createResponse(PropSpecHelper.getPropSpecs(className));
	}

	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postDecode(@QueryParam("script") String scriptName, DecodeRequest request)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("decode message");
		try (DbInterface dbi = new DbInterface())
		{
			return ApiHttpUtil.createResponse(TestDecoder.decodeMessage(request.getRawmsg(), request.getConfig(), 
				scriptName, dbi));
		}
	}
}
