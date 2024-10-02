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

import org.opendcs.odcsapi.beans.ApiDataSource;
import org.opendcs.odcsapi.dao.ApiDataSourceDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

@Path("/")
public class DataSourceResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datasourcerefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getDataSourceRefs() throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getDataSourceRefs");
		try (DbInterface dbi = new DbInterface();
			ApiDataSourceDAO dao = new ApiDataSourceDAO(dbi))
		{
			return ApiHttpUtil.createResponse(dao.readDataSourceRefs());
		}
	}

	@GET
	@Path("datasource")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response geDataSource(@QueryParam("datasourceid") Long dataSourceId)
		throws WebAppException, DbException, SQLException
	{
		if (dataSourceId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, 
				"Missing required datasourceid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getDataSource id=" + dataSourceId);
		try (DbInterface dbi = new DbInterface();
			ApiDataSourceDAO dao = new ApiDataSourceDAO(dbi))
		{
			ApiDataSource ret = dao.readDataSource(dataSourceId);
			if (ret == null)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such DECODES data source with id=" + dataSourceId + ".");
			return ApiHttpUtil.createResponse(ret);
		}
	}

	@POST
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postDatasource(ApiDataSource datasource) throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"post datasource received datasource " + datasource.getName() 
			+ " with ID=" + datasource.getDataSourceId());
		
		try (DbInterface dbi = new DbInterface();
			ApiDataSourceDAO dsDao = new ApiDataSourceDAO(dbi))
		{
			dsDao.writedDataSource(datasource);
			return ApiHttpUtil.createResponse(datasource);
		}
	}

	@DELETE
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response deleteDatasource(@QueryParam("datasourceid") Long datasourceId) throws DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE datasource received datasourceid=" + datasourceId);
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiDataSourceDAO dsDao = new ApiDataSourceDAO(dbi))
		{
			String errmsg = dsDao.datasourceUsedByRs(datasourceId);
			if (errmsg != null)
				return ApiHttpUtil.createResponse(" Cannot delete datasource with ID " + datasourceId 
						+ " because it is used by the following routing specs: "
						+ errmsg, ErrorCodes.NOT_ALLOWED);

			dsDao.deleteDatasource(datasourceId);
			return ApiHttpUtil.createResponse("Datasource with ID " + datasourceId + " deleted");
		}
	}


}
