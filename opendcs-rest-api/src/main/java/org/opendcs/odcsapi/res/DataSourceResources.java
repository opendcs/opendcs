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
import java.util.List;
import java.util.Vector;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import decodes.db.DataSource;
import decodes.db.DataSourceList;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.db.ValueNotFoundException;
import decodes.sql.DbKey;
import org.opendcs.odcsapi.beans.ApiDataSource;
import org.opendcs.odcsapi.beans.ApiDataSourceGroupMember;
import org.opendcs.odcsapi.beans.ApiDataSourceRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;

import static ilex.util.PropertiesUtil.props2string;
import static ilex.util.PropertiesUtil.string2props;


@Path("/")
public class DataSourceResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("datasourcerefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "This method returns a JSON list of DECODES Data Source records suitable for displaying in a table or pick-list",
			description = "Example: \n\n`http://localhost:8080/odcsapi/datasourcerefs`\n\n" +
					"The returned structure contains only the high-level descriptive information about each data source.\n\n" +
					"The arguments (properties) are represented by a string with a comma delimiter. " +
					"Passwords within the string are replaced with four asterisks.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Successfully retrieved data source references",
						content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							array = @ArraySchema(schema = @Schema(implementation = ApiDataSourceRef.class))
					)),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Data Source Records"}
	)
	public Response getDataSourceRefs() throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			DataSourceList dsl = new DataSourceList();
			dbIo.readDataSourceList(dsl);
			return Response.status(HttpServletResponse.SC_OK).entity(map(dsl)).build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error reading data source list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ArrayList<ApiDataSourceRef> map(DataSourceList dsl)
	{
		ArrayList<ApiDataSourceRef> ret = new ArrayList<>();
		for(DataSource ds : dsl.getList())
		{
			ApiDataSourceRef adr = new ApiDataSourceRef();
			if (ds.getId() != null)
			{
				adr.setDataSourceId(ds.getId().getValue());
			}
			else
			{
				adr.setDataSourceId(DbKey.NullKey.getValue());
			}
			adr.setName(ds.getName());
			adr.setType(ds.dataSourceType);
			adr.setUsedBy(ds.numUsedBy);
			if (ds.getArguments() != null)
			{
				adr.setArguments(props2string(ds.getArguments()));
			}
			else
			{
				adr.setArguments(ds.getDataSourceArg());
			}
			ret.add(adr);
		}
		return ret;
	}

	@GET
	@Path("datasource")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	@Operation(
			summary = "The GET datasource method returns a single DECODES data source with all of its detail.",
			description = "The integer argument datasourceid is required.\n Example: " +
					"\n\n\t http://localhost:8080/odcsapi/datasource?datasourceid=10 \n",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
						content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiDataSource.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required datasourceid parameter"),
					@ApiResponse(responseCode = "404", description = "Not Found - No such data source"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Data Source Records"}
	)
	public Response getDataSource(@Parameter(required = true, schema = @Schema(type = "long"))
				@QueryParam("datasourceid") Long dataSourceId)
		throws WebAppException, DbException
	{
		String notFound = "No such DECODES data source with id=";
		if (dataSourceId == null)
		{
			throw new MissingParameterException("Missing required datasourceid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			DataSource ds = new DataSource(DbKey.createDbKey(dataSourceId));
			dbIo.readDataSource(ds);

			if (ds.getName() == null)
			{
				throw new DatabaseItemNotFoundException(notFound + dataSourceId + ".");
			}
			ApiDataSource ret = map(ds);
			return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
		}
		catch (ValueNotFoundException ex)
		{
			throw new DatabaseItemNotFoundException(notFound + dataSourceId + ".", ex);
		}
		catch (DatabaseException ex)
		{
			if (ex.getCause() instanceof ValueNotFoundException)
			{
				throw new DatabaseItemNotFoundException(notFound + dataSourceId + ".");
			}
			throw new DbException("Error reading data source", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiDataSource map(DataSource ds)
	{
		if (ds == null)
		{
			return null;
		}
		ApiDataSource ads = new ApiDataSource();
		if (ds.getId() != null)
		{
			ads.setDataSourceId(ds.getId().getValue());
		}
		else
		{
			ads.setDataSourceId(DbKey.NullKey.getValue());
		}
		ads.setName(ds.getName());
		ads.setType(ds.dataSourceType);
		if (ds.getArguments() != null)
		{
			ads.setProps(ds.getArguments());
		}
		else
		{
			ads.setProps(string2props(ds.getDataSourceArg()));
		}
		ads.setGroupMembers(map(ds.groupMembers));
		ads.setUsedBy(ds.numUsedBy);
		return ads;
	}

	static List<ApiDataSourceGroupMember> map(Vector<DataSource> groupMembers)
	{
		if (groupMembers == null)
		{
			return new ArrayList<>();
		}
		List<ApiDataSourceGroupMember> ret = new ArrayList<>();
		for(DataSource ds : groupMembers)
		{
			ApiDataSourceGroupMember ads = new ApiDataSourceGroupMember();
			if (ds.getId() != null)
			{
				ads.setDataSourceId(ds.getId().getValue());
			}
			else
			{
				ads.setDataSourceId(DbKey.NullKey.getValue());
			}
			ads.setDataSourceName(ds.getName());
			ret.add(ads);
		}
		return ret;
	}

	@POST
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Create or Overwrite Existing Data Source",
			description = "The POST datasource method takes a single datasource in JSON format, as described for the GET method." +
					"\n\nFor creating a new data source, leave datasourceId out of the passed data structure." +
					"\n\nFor overwriting an existing one, include the datasourceId that was previously returned by GET." +
					" The data source in the database is replaced with the one sent.",
			requestBody = @RequestBody(
					description = "Data Source",
					required = true,
					content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiDataSource.class),
							examples = {
									@ExampleObject(name = "Basic", value = ResourceExamples.DataSourceExamples.BASIC),
									@ExampleObject(name = "New", value = ResourceExamples.DataSourceExamples.NEW),
									@ExampleObject(name = "Update", value = ResourceExamples.DataSourceExamples.UPDATE)
							}
					)
			),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created data source", content = @Content(
							mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiDataSource.class)
					)),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required data source object"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Data Source Records"}
	)
	public Response postDatasource(ApiDataSource datasource) throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			DataSource source = map(datasource);
			dbIo.writeDataSource(source);
			return Response.status(HttpServletResponse.SC_CREATED)
					.entity(map(source))
					.build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error writing data source", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static DataSource map(ApiDataSource ads) throws DatabaseException
	{
		DataSource ds = new DataSource();
		if (ads.getDataSourceId() != null)
		{
			ds.setId(DbKey.createDbKey(ads.getDataSourceId()));
		}
		else
		{
			ds.setId(DbKey.NullKey);
		}
		ds.setName(ads.getName());
		ds.dataSourceType = ads.getType();
		ds.arguments = ads.getProps();
		if (ads.getProps() != null)
		{
			ds.setDataSourceArg(props2string(ads.getProps()));
		}
		ds.numUsedBy = ads.getUsedBy();
		ds.groupMembers = map(ads.getGroupMembers());
		return ds;
	}

	static Vector<DataSource> map(List<ApiDataSourceGroupMember> groupMembers)
	{
		Vector<DataSource> ret = new Vector<>();
		if (groupMembers == null)
		{
			return ret;
		}
		for(ApiDataSourceGroupMember ads : groupMembers)
		{
			DataSource ds = new DataSource(DbKey.createDbKey(ads.getDataSourceId()));
			ds.setName(ads.getDataSourceName());
			ret.add(ds);
		}
		return ret;
	}

	@DELETE
	@Path("datasource")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	@Operation(
			summary = "Delete Existing Data Source",
			description = "Required argument datasourceid must be passed. " +
					"\n\nError 405 will be returned if datasource is used by one or more routing specs and cannot" +
					" be deleted. The body of the error will be a message containing the name of the" +
					" routing specs using the referenced datasource.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted data source"),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required datasourceid parameter"),
					@ApiResponse(responseCode = "404", description = "Not Found - No such data source"),
					@ApiResponse(responseCode = "405", description = "Method Not Allowed - Currently in use by routing specs"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - DECODES Data Source Records"}
	)
	public Response deleteDatasource(@Parameter(required = true, schema = @Schema(type = "long"))
				@QueryParam("datasourceid") Long datasourceId)
			throws DbException, WebAppException
	{
		if (datasourceId == null)
		{
			throw new MissingParameterException("Missing required datasourceid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			DataSource ds = new DataSource(DbKey.createDbKey(datasourceId));
			dbIo.readDataSource(ds);
			if (ds.getName() == null)
			{
				throw new DatabaseItemNotFoundException(String.format("No such data source with ID: %s", datasourceId));
			}

			if (ds.numUsedBy > 0)
			{
				return Response.status(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
						.entity(" Cannot delete datasource with ID " + datasourceId
								+ " because it is used by the following number of routing specs: "
								+ ds.numUsedBy).build();
			}

			dbIo.deleteDataSource(ds);
			return Response.status(HttpServletResponse.SC_NO_CONTENT)
					.entity("Datasource with ID " + datasourceId + " deleted").build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Error deleting data source", ex);
		}
		finally
		{
			dbIo.close();
		}
	}
}
