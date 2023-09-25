package org.opendcs.odcsapi.res;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

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

import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiNetlistRef;
import org.opendcs.odcsapi.dao.ApiNetlistDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;

import jakarta.servlet.http.HttpServletResponse;


@Path("/")
public class NetlistResources
{
	@Context HttpHeaders httpHeaders;

	@GET
	@Path("netlistrefs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetlistRefs(
		@QueryParam("token") String token,
		@QueryParam("tmtype") String tmtype
		)
		throws WebAppException, DbException, SQLException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);

		Logger.getLogger(ApiConstants.loggerName).fine("getNetlistRefs tmtype=" + tmtype);
		try (
			DbInterface dbi = new DbInterface(); 
			ApiNetlistDAO netlistDAO = new ApiNetlistDAO(dbi))
		{
			return ApiHttpUtil.createResponse(netlistDAO.readNetlistRefs(tmtype));
		}
	}

	@GET
	@Path("netlist")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getNetList(
		@QueryParam("netlistid") Long netlistId,
		@QueryParam("token") String token
		)
		throws WebAppException, DbException
	{
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		if (netlistId == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required netlistid parameter.");
		
		Logger.getLogger(ApiConstants.loggerName).fine("getNetList netlistid=" + netlistId);
		
		try (
			DbInterface dbi = new DbInterface(); 
			ApiNetlistDAO netlistDAO = new ApiNetlistDAO(dbi))
		{
			ApiNetList ret = netlistDAO.readNetworkList(netlistId);
			if (ret == null)
				throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
					"No such network list with id=" + netlistId + ".");
			return ApiHttpUtil.createResponse(ret);
		}
	}

	@POST
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response  postNetlist(@QueryParam("token") String token, ApiNetList netList)
		throws WebAppException, DbException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"post netlist received netlist " + netList.getName() 
			+ " with tm type " + netList.getTransportMediumType() + " containing "
			+ netList.getItems().size() + " TMs, token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiNetlistDAO netlistDAO = new ApiNetlistDAO(dbi))
		{
			netlistDAO.writeNetlist(netList);
			return ApiHttpUtil.createResponse(netList);
		}
	}

	@DELETE
	@Path("netlist")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteNetlist(
		@QueryParam("token") String token, 
		@QueryParam("netlistid") Long netlistId)
		throws WebAppException, DbException
	{
		Logger.getLogger(ApiConstants.loggerName).fine(
			"DELETE netlist received netlistid=" + netlistId
			+ ", token=" + token);
		
		if (!DbInterface.getTokenManager().checkToken(httpHeaders, token))
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// Use username and password to attempt to connect to the database
		try (DbInterface dbi = new DbInterface();
			ApiNetlistDAO netlistDAO = new ApiNetlistDAO(dbi))
		{
			String errmsg = netlistDAO.netlistUsedByRs(netlistId);
			if (errmsg != null)
				return ApiHttpUtil.createResponse(" Cannot delete network list with ID " + netlistId 
						+ " because it is used by the following routing specs: "
						+ errmsg, ErrorCodes.NOT_ALLOWED);

			netlistDAO.deleteNetlist(netlistId);
			return ApiHttpUtil.createResponse("ID " + netlistId + " deleted");
		}
	}

	@POST
	@Path("cnvtnl")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cnvtNL(
		@QueryParam("token") String token,
		String nldata
		)
		throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("cnvtnl nldata='" + nldata + "'");
		
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
		ApiNetList ret = new ApiNetList();
		
   		LineNumberReader rdr = new LineNumberReader(new StringReader(nldata));
   	   	String ln = null;
   	   	try
   	   	{
	   	   	while( (ln = rdr.readLine()) != null)
	   	   	{
	   	   		ln = ln.trim();
				if (ln.length() <= 0
	   			 || ln.charAt(0) == '#' || ln.charAt(0) == ':') // skip comment lines.
	   				continue;
	   			else
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
	
		try { rdr.close(); } catch (Exception e) {}

		return ApiHttpUtil.createResponse(ret);
	}


}
