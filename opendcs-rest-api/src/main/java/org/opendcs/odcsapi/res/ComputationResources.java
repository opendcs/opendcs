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

import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.dao.ApiComputationDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class ComputationResources
{
	@Context HttpHeaders httpHeaders;
	
	@GET
	@Path("computationrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getComputationRefs(@QueryParam("site") String site,
		@QueryParam("algorithm") String algorithm,
		@QueryParam("datatype") String datatype,
		@QueryParam("group") String group,
		@QueryParam("process") String process,
		@QueryParam("enabled") Boolean enabled,
		@QueryParam("interval") String interval)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getComputationRefs");
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getComputationRefs(site, algorithm, datatype, group,
					process, enabled, interval));
		}
	}

	@GET
	@Path("computation")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getComputation(@QueryParam("computationid") Long compId)
		throws WebAppException, DbException
	{
		if (compId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required computationid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getComputation computationid=" + compId);

		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.getComputation(compId));
		}
	}

	
	@POST
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postComputation(ApiComputation comp)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post comp received comp " + comp.getName()
			+ " with ID=" + comp.getComputationId());
		
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.writeComputation(comp));
		}
	}

	@DELETE
	@Path("computation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteComputation(@QueryParam("computationid") Long computationId) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE computation received computationId=" + computationId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiComputationDAO dao = new ApiComputationDAO(dbi))
		{
			dao.deleteComputation(computationId);
			return ApiHttpUtil.createResponse("Computation with ID " + computationId + " deleted");

		}
	}
}

