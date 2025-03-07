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

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
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
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getNetlistRefs(@QueryParam("tmtype") String tmtype)
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
			ret.add(anlr);
		}
		return ret;
	}

	@GET
	@Path("netlist")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response getNetList(@QueryParam("netlistid") Long netlistId)
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response  postNetlist(ApiNetList netList)
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
		ret.lastModifyTime = netList.getLastModifyTime();
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
	@RolesAllowed({ApiConstants.ODCS_API_ADMIN, ApiConstants.ODCS_API_USER})
	public Response deleteNetlist(@QueryParam("netlistid") Long netlistId)
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
	@RolesAllowed({ApiConstants.ODCS_API_GUEST})
	public Response cnvtANL(String nldata)
			throws WebAppException
	{
		ApiNetList ret = new ApiNetList();

		LineNumberReader rdr = new LineNumberReader(new StringReader(nldata));
		String ln = null;
		try
		{
			while( (ln = rdr.readLine()) != null)
			{
				ln = ln.trim();
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
					"NL File Parsing Failed on line " + rdr.getLineNumber() + ": " + ex
							+ (ln == null ? "" : (" -- " + ln));
			throw new WebAppException(HttpServletResponse.SC_NOT_ACCEPTABLE, msg);
		}

		try { rdr.close(); } catch (Exception ignored)
		{
			// Ignored
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