package org.opendcs.odcsapi.res;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;

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
import org.opendcs.odcsapi.beans.ApiLrgsStatus;
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
import org.opendcs.odcsapi.lrgsclient.DdsProtocolError;
import org.opendcs.odcsapi.lrgsclient.DdsServerError;
import org.opendcs.odcsapi.lrgsclient.LrgsErrorCode;
import org.opendcs.odcsapi.sec.UserToken;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.ApiHttpUtil;
import org.opendcs.odcsapi.util.ApiPropertiesUtil;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Resources for interacting with an LRGS for DCP messages and status.
 */
@Path("/")
public class LrgsResources
{
	@Context HttpHeaders httpHeaders;

	@POST
	@Path("searchcrit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response postSearchCrit(@QueryParam("token") String token, ApiSearchCrit searchcrit)
		throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("post searchcrit");
	
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		
		// If userToken already contains an LddsClient, close and delete it.
		// I.e., if a message retrieval is already in progress, the new searchcrit
		// cancels it. I new client will have to be started on the next GET messages.
		ApiLddsClient client = userToken.getLddsClient();
		if (client != null)
		{
			client.disconnect();
			userToken.setLddsClient(null);
		}
		
		userToken.setSearchCrit(searchcrit);
		return Response.status(HttpServletResponse.SC_OK).entity(
			"Searchcrit cached for current session.").build();
	}
	
	@GET
	@Path("searchcrit")
	@Produces(MediaType.APPLICATION_JSON)
	public ApiSearchCrit getSearchCrit(@QueryParam("token") String token)
		throws WebAppException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getSearchCrit");
		
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		ApiSearchCrit searchcrit = userToken.getSearchCrit();
		if (searchcrit == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
				"No searchcrit is currently stored.");
		return searchcrit;
	}

	@GET
	@Path("messages")
	@Produces(MediaType.APPLICATION_JSON)
	public ApiRawMessageBlock getMessages(@QueryParam("token") String token)
		throws WebAppException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getMessages");
		
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken == null)
			throw new WebAppException(ErrorCodes.TOKEN_REQUIRED, 
				"Valid token is required for this operation.");
		ApiSearchCrit searchcrit = userToken.getSearchCrit();
		if (searchcrit == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, 
				"POST searchcrit required prior to GET messages.");
		
		ApiDataSource dataSource = null;
		ApiLddsClient client = userToken.getLddsClient();
		
		String action = "connecting";
		// See if there is already an ApiLddsClient object in the userToken.
		// If so, skip the stuff below where I connect & send netlists & searchcrit.
		// Just skip to getting the next message block
		if (client == null)
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
				userToken.setLddsClient(client);
				
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
			catch (DbException e)
			{
				client.disconnect();
				userToken.setLddsClient(null);
				e.printStackTrace();
				throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
			}
			catch (UnknownHostException ex)
			{
				client.disconnect();
				userToken.setLddsClient(null);
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "Cannot connect to LRGS data source "
					+ dataSource.getName() + ": " + ex);
			}
			catch (IOException ex)
			{
				client.disconnect();
				userToken.setLddsClient(null);
				throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
					+ dataSource.getName() + ": " + ex);
			}
			catch (DdsProtocolError ex)
			{
				client.disconnect();
				userToken.setLddsClient(null);
				String em = "Error while " + action + ": " + ex;
				Logger.getLogger(ApiConstants.loggerName).warning("getMessages " + em);
				throw new WebAppException(ErrorCodes.IO_ERROR, em);
			}
			catch (DdsServerError ex)
			{
				client.disconnect();
				userToken.setLddsClient(null);
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
			client.disconnect();
			userToken.setLddsClient(null);
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
				+ dataSource.getName() + ": " + ex);
		}
		catch (DdsProtocolError ex)
		{
			client.disconnect();
			userToken.setLddsClient(null);
			throw new WebAppException(ErrorCodes.IO_ERROR, "Error while " + action + ": " + ex);
		}
		catch (DdsServerError ex)
		{
			if (ex.Derrno == LrgsErrorCode.DUNTIL)
			{
				// The retrieval is now finished. Close the client
				client.disconnect();
				userToken.setLddsClient(null);
				
				ApiRawMessageBlock ret = new ApiRawMessageBlock();
				ret.setMoreToFollow(false);
				return ret;
			}
			// Any other server error returns an error.
			client.disconnect();
			userToken.setLddsClient(null);
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
	public ApiRawMessage getMessage(@QueryParam("token") String token,
		@QueryParam("tmid") String tmid, @QueryParam("tmtype") String tmtype)
		throws WebAppException, SQLException
	{
		if (tmid == null)
			throw new WebAppException(ErrorCodes.MISSING_ID, "Missing required tmid argument.");
		
		// Create and save searchcrit for tmid for last 8 hours
		ApiSearchCrit searchcrit = new ApiSearchCrit();
		searchcrit.getPlatformIds().add(tmid);
		searchcrit.setSince("now - 12 hours");
		searchcrit.setUntil("now");
		postSearchCrit(token, searchcrit);
		
		// Get a message block and return the first (most recent) message in it.
		ApiRawMessageBlock mb = getMessages(token);
		if (mb.getMessages().size() == 0)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "No message for '"
				+ tmid + "' in last 12 hours.");
		
		// This method gets a SINGLE message, so we're finished with client now.
		UserToken userToken = DbInterface.getTokenManager().getToken(httpHeaders, token);
		if (userToken != null)
		{
			ApiLddsClient client = userToken.getLddsClient();
			if (client != null)
			{
				client.disconnect();
				userToken.setLddsClient(null);
			}
		}

		return mb.getMessages().get(0);		
	}
	
	@GET
	@Path("lrgsstatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLrgsStatus(@QueryParam("token") String token,
		@QueryParam("source") String source)
		throws WebAppException, SQLException
	{
		Logger.getLogger(ApiConstants.loggerName).fine("getLrgsStatus");
		
		DbInterface.getTokenManager().checkToken(httpHeaders, token);
		
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
		catch (DbException e)
		{
			e.printStackTrace();
			throw new WebAppException(ErrorCodes.DATABASE_ERROR, "Internal exception:" + e);
		}
		catch (UnknownHostException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "Cannot connect to LRGS data source "
				+ dataSource.getName() + ": " + ex);
		}
		catch (IOException ex)
		{
			throw new WebAppException(ErrorCodes.BAD_CONFIG, "IO Error on LRGS data source "
				+ dataSource.getName() + ": " + ex);
		}
		catch (DdsProtocolError ex)
		{
			String em = "Error while " + action + ": " + ex;
			Logger.getLogger(ApiConstants.loggerName).warning("getMessages " + em);
			throw new WebAppException(ErrorCodes.IO_ERROR, em);
		}
		catch (DdsServerError ex)
		{
			String em = "Error while " + action + ": " + ex;
			Logger.getLogger(ApiConstants.loggerName).warning("getMessages " + em);
			throw new WebAppException(ErrorCodes.IO_ERROR, em);
		}
		finally
		{
			if (client != null)
				client.disconnect();
		}
	}
}
