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

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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

import org.opendcs.odcsapi.beans.ApiDataSource;
import org.opendcs.odcsapi.beans.ApiDataSourceRef;
import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiRawMessage;
import org.opendcs.odcsapi.beans.ApiRawMessageBlock;
import org.opendcs.odcsapi.beans.ApiSearchCrit;
import org.opendcs.odcsapi.dao.ApiDataSourceDAO;
import org.opendcs.odcsapi.dao.ApiNetlistDAO;
import org.opendcs.odcsapi.dao.ApiPlatformDAO;
import org.opendcs.odcsapi.dao.ApiTsDAO;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.lrgsclient.ApiLddsClient;
import org.opendcs.odcsapi.lrgsclient.ClientConnectionCache;
import org.opendcs.odcsapi.lrgsclient.DdsProtocolError;
import org.opendcs.odcsapi.lrgsclient.DdsServerError;
import org.opendcs.odcsapi.lrgsclient.LrgsErrorCode;
import org.opendcs.odcsapi.sec.AuthorizationCheck;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;

/**
 * Resources for interacting with an LRGS for DCP messages and status.
 */
@Path("/")
public class LrgsResources
{
	@Context private HttpServletRequest request;
	@Context private HttpHeaders httpHeaders;

	@POST
	@Path("searchcrit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public Response postSearchCrit(ApiSearchCrit searchcrit)
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post searchcrit");


		HttpSession session = request.getSession(true);
		// If session already contains an LddsClient, close and delete it.
		// I.e., if a message retrieval is already in progress, the new searchcrit
		// cancels it. A new client will have to be started on the next GET messages.
		ClientConnectionCache.getInstance().removeApiLddsClient(session.getId());
		String searchCritSessionAttribute = ApiSearchCrit.ATTRIBUTE;
		session.setAttribute(searchCritSessionAttribute, searchcrit);

		return Response.status(HttpServletResponse.SC_OK).entity(
			"Searchcrit cached for current session.").build();
	}
	
	@GET
	@Path("searchcrit")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public ApiSearchCrit getSearchCrit() throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getSearchCrit");

		HttpSession session = request.getSession(false);
		if(session == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT,
					"No searchcrit is currently stored.");
		String sessionAttribute = ApiSearchCrit.ATTRIBUTE;
		ApiSearchCrit searchcrit = (ApiSearchCrit) session.getAttribute(sessionAttribute);
		if (searchcrit == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
				"No searchcrit is currently stored.");
		return searchcrit;
	}

	@GET
	@Path("messages")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public ApiRawMessageBlock getMessages() throws WebAppException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getMessages");

		HttpSession session = request.getSession(true);
		String sessionAttribute = ApiSearchCrit.ATTRIBUTE;
		ApiSearchCrit searchcrit = (ApiSearchCrit) session.getAttribute(sessionAttribute);
		if (searchcrit == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
				"POST searchcrit required prior to GET messages.");
		
		ApiDataSource dataSource = null;
		ClientConnectionCache clientConnectionCache = ClientConnectionCache.getInstance();
		ApiLddsClient client = clientConnectionCache.getApiLddsClient(session.getId())
				.orElse(null);
		
		String action = "connecting";
		// See if there is already an ApiLddsClient object in the userToken.
		// If so, skip the stuff below where I connect & send netlists & searchcrit.
		// Just skip to getting the next message block
		if (client != null)
		{
			// This is a new retrieval. Create client, send netlists & searchcrit.
		
			try (DbInterface dbi = new DbInterface();
				ApiPlatformDAO platformDao = new ApiPlatformDAO(dbi);
				ApiNetlistDAO nlDao = new ApiNetlistDAO(dbi))
			{
				dataSource = getApiDataSource(dbi, null);
				String host = ApiPropertiesUtil.getIgnoreCase(dataSource.getProps(),"host");
				if (host == null)
					host = dataSource.getName();
				int port = 16003;
				String s = ApiPropertiesUtil.getIgnoreCase(dataSource.getProps(),"port");
				if (s != null)
				{
					try { port = Integer.parseInt(s.trim()); }
					catch(NumberFormatException ex)
					{
						Logger.getLogger(ApiConstants.loggerName).warning("getMessages bad port property "
							+ s + " in data source " + dataSource.getName() + " -- ignored.");
						port = 16003;
					}
				}
				
				client = new ApiLddsClient(host, port);
				client.connect();
				clientConnectionCache.setApiLddsClient(client, session.getId());
				
				String username = dataSource.getProps().getProperty("username");
				String password = dataSource.getProps().getProperty("password");
				if (password == null)
					client.sendHello(username);
				else
					client.sendAuthHello(username, password);
				
				for(String nlname : searchcrit.getNetlistNames())
				{
					Long nlId = nlDao.getNetlistId(nlname);
					if (nlId != null)
					{
						action = "sending netlist " + nlname + ", id=" + nlId;
						ApiNetList nl = nlDao.readNetworkList(nlId);
						client.sendNetList(nl);
					}
				}
				
				action = "sending searchcrit";
				client.sendSearchCrit(searchcrit, platformDao);
			}
			catch (DbException ex)
			{
				clientConnectionCache.removeApiLddsClient(session.getId());
				throw new WebAppException(ErrorCodes.DATABASE_ERROR,
						"There was an error getting messages from the LRGS client: ", ex);
			}
			catch (UnknownHostException ex)
			{
				clientConnectionCache.removeApiLddsClient(session.getId());
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "Cannot connect to LRGS data source "
					+ dataSource.getName() + ": " + ex);
			}
			catch (IOException ex)
			{
				clientConnectionCache.removeApiLddsClient(session.getId());
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
					+ dataSource.getName() + ": " + ex);
			}
			catch (DdsProtocolError ex)
			{
				clientConnectionCache.removeApiLddsClient(session.getId());
				String em = "Error while " + action + ": " + ex;
				Logger.getLogger(ApiConstants.loggerName).warning("getMessages " + em);
				throw new WebAppException(ErrorCodes.IO_ERROR, em);
			}
			catch (DdsServerError ex)
			{
				clientConnectionCache.removeApiLddsClient(session.getId());
				String em = "Error while " + action + ": " + ex;
				Logger.getLogger(ApiConstants.loggerName).warning("getMessages " + em);
				throw new WebAppException(ErrorCodes.IO_ERROR, em);
			}
		}
		// ELSE use the existing client object already initialized.
		
		try
		{
			action = "getting message block";
			return client.getMsgBlockExt(60);
		}
		catch (IOException ex)
		{
			clientConnectionCache.removeApiLddsClient(session.getId());
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
				+ dataSource.getName() + ": " + ex);
		}
		catch (DdsProtocolError ex)
		{
			clientConnectionCache.removeApiLddsClient(session.getId());
			throw new WebAppException(ErrorCodes.IO_ERROR, "Error while " + action + ": " + ex);
		}
		catch (DdsServerError ex)
		{
			if (ex.Derrno == LrgsErrorCode.DUNTIL)
			{
				// The retrieval is now finished. Close the client
				clientConnectionCache.removeApiLddsClient(session.getId());
				
				ApiRawMessageBlock ret = new ApiRawMessageBlock();
				ret.setMoreToFollow(false);
				return ret;
			}
			// Any other server error returns an error.
			clientConnectionCache.removeApiLddsClient(session.getId());
			throw new WebAppException(ErrorCodes.IO_ERROR, "Error while " + action + ": " + ex);
		}
	}
	
	/**
	 * 
	 * @param dbi
	 * @return
	 * @throws DbException
	 * @throws SQLException 
	 */
	private static ApiDataSource getApiDataSource(DbInterface dbi, String dsName)
		throws DbException, WebAppException, SQLException
	{
		try(ApiDataSourceDAO dsDao = new ApiDataSourceDAO(dbi);
			ApiTsDAO tsDao = new ApiTsDAO(dbi))
		{
			ApiDataSource dataSource = null;
			Properties tsdbProps = tsDao.getTsdbProperties();
			if (dsName == null)
				dsName = tsdbProps.getProperty("api.datasource");
			if (dsName != null)
			{
				Long dsId = dsDao.getDataSourceId(dsName);
				if (dsId == null)
					Logger.getLogger(ApiConstants.loggerName).warning(
						"TSDB property api.datasource references non-existant data source '"
						+ dsName + "' -- will try other LRGS.");
				else
					dataSource = dsDao.readDataSource(dsId);
			}
			if (dataSource == null)
			{
				// No api.datasource specified, or it doesn't exist. Try the first LRGS
				// datasource in the list.
			
				ArrayList<ApiDataSourceRef> dataSourceRefs = dsDao.readDataSourceRefs();
				for(ApiDataSourceRef dsr : dataSourceRefs)
					if (dsr.getType().toLowerCase().equals("lrgs"))
					{
						dataSource = dsDao.readDataSource(dsr.getDataSourceId());
						break;
					}
			}
			if (dataSource == null)
				throw new WebAppException(ErrorCodes.BAD_CONFIG,
					"No usable LRGS datasource: "
					+ "Create one, then define 'api.datasource' in TSDB properties.");
			return dataSource;
		}
	}
		
	@GET
	@Path("message")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_ADMIN, AuthorizationCheck.ODCS_API_USER})
	public ApiRawMessage getMessage(@QueryParam("tmid") String tmid, @QueryParam("tmtype") String tmtype)
		throws WebAppException, SQLException
	{
		if (tmid == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required tmid argument.");
		
		// Create and save searchcrit for tmid for last 8 hours
		ApiSearchCrit searchcrit = new ApiSearchCrit();
		searchcrit.getPlatformIds().add(tmid);
		searchcrit.setSince("now - 12 hours");
		searchcrit.setUntil("now");
		postSearchCrit(searchcrit);
		
		// Get a message block and return the first (most recent) message in it.
		ApiRawMessageBlock mb = getMessages();
		if (mb.getMessages().isEmpty())
		{
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No message for '"
					+ tmid + "' in last 12 hours.");
		}
		
		// This method gets a SINGLE message, so we're finished with client now.
		HttpSession session = request.getSession(false);
		if(session != null)
		{
			ClientConnectionCache.getInstance().removeApiLddsClient(session.getId());
		}
		return mb.getMessages().get(0);		
	}
	
	@GET
	@Path("lrgsstatus")
	@Produces(MediaType.APPLICATION_JSON)
	@RolesAllowed({AuthorizationCheck.ODCS_API_GUEST})
	public Response getLrgsStatus(@QueryParam("source") String source)
		throws WebAppException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getLrgsStatus");
		ApiDataSource dataSource = null;
		ApiLddsClient client = null;
		
		String action = "connecting";
		
		try (DbInterface dbi = new DbInterface())
		{
			dataSource = getApiDataSource(dbi, source);
			String host = ApiPropertiesUtil.getIgnoreCase(dataSource.getProps(),"host");
			if (host == null)
				host = dataSource.getName();
			int port = 16003;
			String s = ApiPropertiesUtil.getIgnoreCase(dataSource.getProps(),"port");
			if (s != null)
			{
				try { port = Integer.parseInt(s.trim()); }
				catch(NumberFormatException ex)
				{
					Logger.getLogger(ApiConstants.loggerName).warning("getMessages bad port property "
						+ s + " in data source " + dataSource.getName() + " -- ignored.");
					port = 16003;
				}
			}
			
			client = new ApiLddsClient(host, port);
			client.connect();
			
			String username = dataSource.getProps().getProperty("username");
			String password = dataSource.getProps().getProperty("password");
			if (password == null)
				client.sendHello(username);
			else
				client.sendAuthHello(username, password);
			
			action = "getting LRGS status";
			return ApiHttpUtil.createResponse(client.getLrgsStatus());
		}
		catch (DbException ex)
		{
			throw new WebAppException(ErrorCodes.DATABASE_ERROR,
					"There was an error connecting to the decodes database", ex);
		}
		catch (UnknownHostException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "Cannot connect to LRGS data source "
				+ dataSource.getName() + ": ", ex);
		}
		catch (IOException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
				+ dataSource.getName() + ": ", ex);
		}
		catch (DdsProtocolError | DdsServerError ex)
		{
			String em = "Error while " + action + ": ";
			throw new WebAppException(ErrorCodes.IO_ERROR, em, ex);
		}
		finally
		{
			if (client != null)
				client.disconnect();
		}
	}
}
