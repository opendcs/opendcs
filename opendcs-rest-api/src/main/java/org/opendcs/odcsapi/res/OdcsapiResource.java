/*
 *  Copyright 2024 OpenDCS Consortium and its Contributors
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

import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletResponse;
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

import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.odcsapi.beans.DecodeRequest;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.opendcs_dep.PropSpecHelper;
import org.opendcs.odcsapi.opendcs_dep.TestDecoder;
import org.opendcs.odcsapi.sec.AuthorizationCheck;


@Path("/")
public class OdcsapiResource extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("tsdb_properties")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getTsdbProperties() throws DbException
	{
		try
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();
			tsdb.readTsdbProperties(tsdb.getConnection());

			Properties props = new Properties();
			for (Object keyObj : Collections.list(tsdb.getPropertyNames()))
			{
				String key = (String) keyObj;
				props.setProperty(key, tsdb.getProperty(key));
			}
			return Response.status(HttpServletResponse.SC_OK).entity(props).build();
		}
		catch(SQLException e)
		{
			throw new DbException("Error reading timeseries properties", e);
		}
	}

	@POST
	@Path("tsdb_properties")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postTsdbProperties(Properties props) throws DbException
	{
		try
		{
			TimeSeriesDb tsdb = getLegacyTimeseriesDB();
			tsdb.writeTsdbProperties(props);
		}
		catch (DbIoException e)
		{
			throw new DbException("Error writing timeseries properties", e);
		}
		return Response.status(HttpServletResponse.SC_OK).entity(props).build();
	}

	@GET
	@Path("propspecs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getPropSpecs(@QueryParam("class") String className)
			throws WebAppException
	{
		if (className == null)
		{
			throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST, "Missing required class argument.");
		}

		return Response.status(HttpServletResponse.SC_OK).entity(PropSpecHelper.getPropSpecs(className)).build();
	}

	@POST
	@Path("decode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postDecode(@QueryParam("script") String scriptName, DecodeRequest request)
			throws WebAppException, DbException
	{
		if (scriptName == null)
		{
			throw new WebAppException(HttpServletResponse.SC_BAD_REQUEST, "Missing required script argument.");
		}
		OpenDcsDatabase db = createDb();

		return Response.status(HttpServletResponse.SC_OK).entity(TestDecoder.decodeMessage(request.getRawmsg(),
				request.getConfig(), scriptName, db)).build();
	}
}