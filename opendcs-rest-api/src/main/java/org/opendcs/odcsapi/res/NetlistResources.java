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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;
import decodes.db.RoutingSpec;
import decodes.db.RoutingSpecList;
import decodes.sql.DbKey;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiNetlistRef;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.DatabaseItemNotFoundException;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.util.ApiConstants;


@Path("/")
public final class NetlistResources extends OpenDcsResource
{
	@Context HttpHeaders httpHeaders;


	@GET
	@Path("netlistrefs")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The GET netlistrefs method returns references to network lists",
			description = "The GET netlistrefs method is intended to populate a pick list of network lists and"
					+ " does not contain all of the list elements.\n\nExamples:\n\n"
					+ "    http://localhost:8080/odcsapi/netlistrefs\n\n"
					+ "    http://localhost:8080/odcsapi/netlistrefs?tmtype=goes\n\nWith no arguments,"
					+ " a list of network lists in the database is returned. The format is as follows:"
					+ "\n```\n[\n  {\n    \"lastModifyTime\": \"2020-08-22T14:36:55.705Z[UTC]\",\n"
					+ "    \"name\": \"BFD-BMD\",\n    \"netlistId\": 1,\n    \"numPlatforms\": 3,\n"
					+ "    \"siteNameTypePref\": \"nwshb5\",\n    \"transportMediumType\": \"goes\"\n  },"
					+ "\n  {\n    \"lastModifyTime\": \"2020-12-15T17:51:04.194Z[UTC]\",\n"
					+ "    \"name\": \"goes2\",\n    \"netlistId\": 6,\n    \"numPlatforms\": 3,\n"
					+ "    \"siteNameTypePref\": \"nwshb5\",\n    \"transportMediumType\": \"goes\"\n  }\n]"
					+ "\n```\n\n**Note** that the list contents (i.e., the references to the platforms in the list)"
					+ " is not included. Rather a count of platforms in the list is given."
					+ " The 'netlistId' element is a unique key to be used for retrieving entire lists.",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									array = @ArraySchema(schema = @Schema(implementation = ApiNetlistRef.class)))),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Network Lists"}
	)
	public Response getNetlistRefs(@Parameter(description = "The transport medium type to filter by",
			schema = @Schema(implementation = String.class))
		@QueryParam("tmtype") String tmtype)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			NetworkListList nlList = new NetworkListList();
			if (tmtype == null)
			{
				dbIo.readNetworkListList(nlList);
			}
			else
			{
				dbIo.readNetworkListList(nlList, getSingleWord(tmtype).toLowerCase());
			}
			return Response.status(HttpServletResponse.SC_OK).entity(map(nlList)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to retrieve network list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static List<ApiNetlistRef> map(NetworkListList nlList)
	{
		List<ApiNetlistRef> ret = new ArrayList<>();
		for (NetworkList nl : nlList.getList())
		{
			ApiNetlistRef anlr = new ApiNetlistRef();
			if (nl.getId() != null)
			{
				anlr.setNetlistId(nl.getId().getValue());
			}
			else
			{
				anlr.setNetlistId(DbKey.NullKey.getValue());
			}
			anlr.setName(nl.getDisplayName());
			anlr.setLastModifyTime(nl.lastModifyTime);
			anlr.setTransportMediumType(nl.transportMediumType);
			anlr.setSiteNameTypePref(nl.siteNameTypePref);
			anlr.setNumPlatforms(nl.networkListEntries.size());
			ret.add(anlr);
		}
		return ret;
	}

	@GET
	@Path("netlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "The ‘netlists’ GET method will return a specific network list in its entirety.",
			description = "Example:\n\n    http://localhost:8080/odcsapi/netlist?netlistid=1",
			responses = {
					@ApiResponse(responseCode = "200", description = "Success",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiNetList.class))),
					@ApiResponse(responseCode = "400",
							description = "The required ‘netlistid’ parameter was missing in the URL."),
					@ApiResponse(responseCode = "404", description = "Not Found"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Network Lists"}
	)
	public Response getNetList(@Parameter(description = "Unique identifier for the netlist to retrieve",
			schema = @Schema(implementation = Long.class))
		@QueryParam("netlistid") Long netlistId)
			throws WebAppException, DbException
	{
		if (netlistId == null)
		{
			throw new MissingParameterException("Missing required netlistid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			NetworkListList nlList = new NetworkListList();
			dbIo.readNetworkListList(nlList);
			NetworkList nl = nlList.getById(DbKey.createDbKey(netlistId));
 			if (nl == null || nl.networkListEntries == null || nl.networkListEntries.isEmpty())
			{
				throw new DatabaseItemNotFoundException("No such network list with id=" + netlistId + ".");
			}
			ApiNetList ret = map(nl);
			return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException(String.format("Unable to retrieve network list of ID: %s", netlistId), ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static ApiNetList map(NetworkList nl)
	{
		ApiNetList ret = new ApiNetList();
		if (nl.getId() != null)
		{
			ret.setNetlistId(nl.getId().getValue());
		}
		else
		{
			ret.setNetlistId(DbKey.NullKey.getValue());
		}
		ret.setName(nl.getDisplayName());
		ret.setLastModifyTime(nl.lastModifyTime);
		ret.setTransportMediumType(nl.transportMediumType);
		ret.setSiteNameTypePref(nl.siteNameTypePref);
		for (Map.Entry<String, NetworkListEntry> entry : nl.networkListEntries.entrySet())
		{
			ApiNetListItem anli = new ApiNetListItem();
			anli.setTransportId(entry.getValue().getTransportId());
			anli.setDescription(entry.getValue().getDescription());
			anli.setPlatformName(entry.getValue().getPlatformName());
			ret.getItems().put(anli.getTransportId().toUpperCase(), anli);
		}
		return ret;
	}

	@POST
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Tag(name = "REST - Network Lists", description = "A Network List is simply a list of Platforms.")
	@Operation(
			summary = "Create or Overwrite Existing Netlist",
			description = "The ‘netlist’ POST method takes a single network list in JSON format,"
					+ " as described for the GET method:"
					+ "\n```\n{\n  \"items\": {\n    \"14159500\": {\n      \"description\": \"\",\n      "
					+ "\"platformName\": \"CGRO\",\n      \"transportId\": \"14159500\"\n    },\n    "
					+ "\"14372300\": {\n      \"description\": \"\",\n      \"platformName\": \"AGNO\",\n      "
					+ "\"transportId\": \"14372300\"\n    }\n  },\n  \"lastModifyTime\": \"2020-10-19T18:14:14.788Z[UTC]\","
					+ "\n  \"name\": \"USGS-Sites\",\n  \"netlistId\": 4,\n  \"siteNameTypePref\": \"nwshb5\",\n  "
					+ "\"transportMediumType\": \"other\"\n}\n```\n\nFor creating a new network list, "
					+ "leave netlistId out of the passed data structure.\n\nFor overwriting an existing one, "
					+ "include the netlistId that was previously returned. The network list in the database "
					+ "is replaced with the one sent.",
			requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ApiNetList.class),
						examples = {
								@ExampleObject(name = "Basic", value = ResourceExamples.NetlistExamples.BASIC),
								@ExampleObject(name = "New", value = ResourceExamples.NetlistExamples.NEW),
								@ExampleObject(name = "Update", value = ResourceExamples.NetlistExamples.UPDATE)
						})),
			responses = {
					@ApiResponse(responseCode = "201", description = "Successfully created or updated network list",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiNetList.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request - Missing required request body"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error"),
			},
			tags = {"REST - Network Lists"}
	)
	public Response postNetlist(ApiNetList netList)
			throws DbException
	{
		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			NetworkList nlList = map(netList);
			dbIo.writeNetworkList(nlList);
			return Response.status(HttpServletResponse.SC_CREATED).entity(map(nlList)).build();
		}
		catch(DatabaseException ex)
		{
			throw new DbException("Unable to store network list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	static NetworkList map(ApiNetList netList) throws DatabaseException
	{
		NetworkList ret = new NetworkList(netList.getName());
		if (netList.getNetlistId() != null)
		{
			ret.setId(DbKey.createDbKey(netList.getNetlistId()));
		}
		else
		{
			ret.setId(DbKey.NullKey);
		}
		ret.name = netList.getName();
		ret.transportMediumType = netList.getTransportMediumType();
		ret.siteNameTypePref = netList.getSiteNameTypePref();
		ret.lastModifyTime = new Date();
		for (Map.Entry<String, ApiNetListItem> anli : netList.getItems().entrySet())
		{
			NetworkListEntry nle = new NetworkListEntry(ret, anli.getValue().getTransportId());
			nle.transportId = anli.getValue().getTransportId();
			nle.setDescription(anli.getValue().getDescription());
			nle.setPlatformName(anli.getValue().getPlatformName());
			ret.networkListEntries.put(anli.getKey().toUpperCase(), nle);
		}
		return ret;
	}

	@DELETE
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Delete Existing netlists",
			description = "Required argument netlistid must be passed.\n\n" +
					"Error 409 will be returned if network list is used by one or more " +
					"routing specs and cannot be deleted. The body of the error will be " +
					"a message containing the name of the routing specs using the referenced netlist.",
			responses = {
					@ApiResponse(responseCode = "204", description = "Successfully deleted network list"),
					@ApiResponse(responseCode = "409",
							description = "Conflict - Network list is used by one or more routing specs"),
					@ApiResponse(responseCode = "400", description = "Missing required netlistid parameter"),
					@ApiResponse(responseCode = "500", description = "Internal Server Error")
			},
			tags = {"REST - Network Lists"}
	)
	public Response deleteNetlist(@Parameter(schema = @Schema(implementation = Long.class))
		@QueryParam("netlistid") Long netlistId)
			throws DbException, WebAppException
	{
		if (netlistId == null)
		{
			throw new MissingParameterException("Missing required netlistid parameter.");
		}

		DatabaseIO dbIo = getLegacyDatabase();
		try
		{
			NetworkListList nlList = new NetworkListList();
			dbIo.readNetworkListList(nlList);
			NetworkList nl = nlList.getById(DbKey.createDbKey(netlistId));

			RoutingSpecList routingSpecList = new RoutingSpecList();
			dbIo.readRoutingSpecList(routingSpecList);

			StringBuilder errmsg = new StringBuilder();

			for (RoutingSpec spec : routingSpecList.getList())
			{
				for (NetworkList list : spec.networkLists)
				{
					if (list.getId().equals(nl.getId()))
					{
						errmsg.append((errmsg.length() > 0) ? ", " : "").append(spec.getName());
					}
				}
			}

			if (errmsg.length() > 0)
			{
				return Response.status(HttpServletResponse.SC_CONFLICT)
						.entity(" Cannot delete network list with ID " + netlistId
								+ " because it is used by the following routing specs: "
								+ errmsg).build();
			}
			if (nl == null)
			{
				nl = new NetworkList();
				nl.setId(DbKey.createDbKey(netlistId));
			}
			dbIo.deleteNetworkList(nl);
			return Response.status(HttpServletResponse.SC_NO_CONTENT).entity("ID " + netlistId + " deleted").build();
		}
		catch (DatabaseException ex)
		{
			throw new DbException("Unable to delete network list", ex);
		}
		finally
		{
			dbIo.close();
		}
	}

	@POST
	@Path("cnvtnl")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_USER, ApiConstants.ODCS_API_ADMIN})
	@Operation(
			summary = "Convert Network List File",
			description = "Parses a network list file (in text format) and converts it to an object representation.",
			requestBody = @RequestBody(required = true, content = @Content(mediaType = MediaType.TEXT_PLAIN,
					examples = {
						@ExampleObject(value = "14159500 CGRO\n14372300 AGNO\n"),
						@ExampleObject(value = "6698948: Stream_test_site platform associated with stream")
					})),
			responses = {
					@ApiResponse(responseCode = "200", description = "Network list successfully parsed",
							content = @Content(mediaType = MediaType.APPLICATION_JSON,
									schema = @Schema(implementation = ApiNetList.class))),
					@ApiResponse(responseCode = "406", description = "File parsing error or invalid format"),
					@ApiResponse(responseCode = "500", description = "Server error occurred")
			},
			tags = {"REST - Network Lists"}
	)
	public Response cnvtANL(String nldata)
			throws WebAppException
	{
		ApiNetList ret = new ApiNetList();


		String ln = null;
		int lineNumber = 0;
		try(LineNumberReader rdr = new LineNumberReader(new StringReader(nldata)))
		{
			while( (ln = rdr.readLine()) != null)
			{
				ln = ln.trim();
				lineNumber = rdr.getLineNumber();
				if (!(ln.isEmpty()
						|| ln.charAt(0) == '#' || ln.charAt(0) == ':')) // skip comment lines.
				{
					ApiNetListItem anli = ApiNetListItem.fromString(ln);
					ret.getItems().put(anli.getTransportId(), anli);
				}
			}
		}
		catch(Exception ex)
		{
			String msg =
					"NL File Parsing Failed on line " + lineNumber + ": " + ex
							+ (ln == null ? "" : (" -- " + ln));
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, msg, ex);
		}

		return Response.status(HttpServletResponse.SC_OK).entity(ret).build();
	}

	// Helper method to extract the first word from a string.
	public static String getSingleWord(String arg)
	{
		String special = "(){}[]'\"|,";

		StringBuilder sb = new StringBuilder(arg.trim());
		for(int idx = 0; idx < sb.length(); idx++)
		{
			char c = sb.charAt(idx);
			if (Character.isWhitespace(c) || special.indexOf(c) >= 0)
				return idx == 0 ? "" : sb.substring(0, idx);
		}
		return sb.toString();
	}

}
