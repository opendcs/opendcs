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

package org.opendcs.odcsapi.sec;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;

import org.opendcs.odcsapi.appmon.ApiEventClient;
import org.opendcs.odcsapi.beans.ApiAppStatus;
import org.opendcs.odcsapi.dao.ApiAppDAO;
import org.opendcs.odcsapi.dao.ApiDaoBase;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.ErrorCodes;
import org.opendcs.odcsapi.errorhandling.WebAppException;
import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.lrgsclient.ApiLddsClient;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.odcsapi.util.Base64;

public class TokenManager
{
	private static String module = "TokenManager";
	private ArrayList<UserToken> activeTokens = new ArrayList<UserToken>();
	private boolean secureMode = false;
	private SecureRandom rand = new SecureRandom();
	private static final long TOKEN_MAX_AGE = 1000L * 3600 * 3; // A session is only valid for 3 hrs.

	public TokenManager(boolean secureMode)
	{
		// Set secureMode from info in the dbi, which is set from cmdline args or props file.
		this.secureMode = secureMode;
	}
	
	public synchronized UserToken makeToken(Credentials creds, DbInterface dbi, HttpHeaders hdrs)
		throws WebAppException
	{
		// If creds not provided in POST body, attempt to get from http header.
		if (creds == null)
		{
			String authString = hdrs.getHeaderString(HttpHeaders.AUTHORIZATION);
			if (authString != null && authString.length() > 0)
			{
				String authHdrs[] = authString.split(",");
				for(int idx = 0; idx < authHdrs.length; idx++)
				{
					String x = authHdrs[idx].trim();
					Logger.getLogger(ApiConstants.loggerName).info(module + ".makeToken authHdrs[" + idx + "] = " + x);
					if (x.startsWith("Basic"))
					{
						int sp = x.indexOf(' ');
						if (sp > 0)
						{
							x = x.substring(sp).trim();
							String up = new String(Base64.decodeBase64(x.getBytes()));
							String ups[] = up.split(":");
							if (ups == null || ups.length < 2 || ups[0] == null || ups[1] == null)
								throw new WebAppException(ErrorCodes.AUTH_FAILED, "Credentials not provided.");
							creds = new Credentials();
							creds.setUsername(ups[0]);
							creds.setPassword(ups[1]);
							Logger.getLogger(ApiConstants.loggerName).info(module 
								+ ".checkToken found tokstr in header.");
						}
					}
				}
			}
		}
		
		// get URL from the dbi
		Connection poolCon = dbi.getConnection();
		Connection userCon = null;

		try (ApiDaoBase daoBase = new ApiDaoBase(dbi, module))
		{
			// The only way to verify that user/pw is valid is to attempt to establish a connection:
			DatabaseMetaData metaData = poolCon.getMetaData();
			String url = metaData.getURL();
			
			// Mmake a new db connection using passed credentials
			// This validates the username & password.
			// This will throw SQLException if user/pw is not valid.
			userCon = DriverManager.getConnection(url, creds.getUsername(), creds.getPassword());
			
			if (!DbInterface.isOracle)
			{
				// Now verify that user has appropriate privilege. This only works on Postgress currently:
				String q = "select pm.roleid, pr.rolname from pg_auth_members pm, pg_roles pr "
					+ "where pm.member = (select oid from pg_roles where rolname = '" + creds.getUsername() + "') "
					+ "and pm.roleid = pr.oid";
				ResultSet rs = daoBase.doQuery(q);
				boolean hasPerm = false;
				while(rs.next() && !hasPerm)
				{
					int roleid = rs.getInt(1);
					String role = rs.getString(2);
					Logger.getLogger(ApiConstants.loggerName).info("User '" + creds.getUsername() 
						+ "' has role " + roleid + "=" + role);
					if (role.equalsIgnoreCase("OTSDB_ADMIN") || role.equalsIgnoreCase("OTSDB_MGR"))
						hasPerm = true;
				}
				
				if (!hasPerm)
					throw new WebAppException(ErrorCodes.AUTH_FAILED, 
						"User does not have OTSDB_ADMIN or OTSDB_MGR privilege - Not Authorized.");
			}
		}
		catch (Exception e)
		{
			Logger.getLogger(ApiConstants.loggerName).warning("isUserValid - Authentication failed: " + e);
			throw new WebAppException(ErrorCodes.AUTH_FAILED, "DB connection failed with passed credentials."); 
		}
		finally
		{
			if (userCon != null)
				try { userCon.close(); } catch(Exception ex) {}
		}

		// Make a UserToken and token string. Place in Hashmap & return string.
		String tokstr = Long.toHexString(rand.nextLong());
		UserToken userTok = new UserToken(tokstr, creds.getUsername());
		activeTokens.add(userTok);
		Logger.getLogger(ApiConstants.loggerName).fine("Added new token for user '" 
			+ creds.getUsername() + "'=" + tokstr);
		
		return userTok;
	}
	
	/**
	 * Check token from header or URL. If one is found, update its lastUsed time.
	 * @param hdrs the HTTP header
	 * @param urlToken the token string from the URL, or null if not provided.
	 * @return true if token is valid, false if not.
	 * @throws WebAppException if an invalid token is provided.
	 */
	public boolean checkToken(HttpHeaders hdrs, String urlToken)
		throws WebAppException
	{
		UserToken userToken = getToken(hdrs, urlToken);
		return (userToken != null);
	}
	
	/**
	 * Performs the check operation, but also returns the UserToken being
	 * cached for the current session.
	 * @param hdrs
	 * @param urlToken
	 * @return
	 * @throws WebAppException
	 */
	public synchronized UserToken getToken(HttpHeaders hdrs, String urlToken)
		throws WebAppException
	{
		String tokstr = null;

		// try to get tokstr from header Authentication - Bearer
		String authString = hdrs.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (authString != null && authString.length() > 0)
		{
			String authHdrs[] = authString.split(",");
			for(int idx = 0; idx < authHdrs.length; idx++)
			{
				String x = authHdrs[idx].trim();
				Logger.getLogger(ApiConstants.loggerName).info(module + ".checkToken authHdrs[" + idx + "] = " + x);
				if (x.startsWith("Bearer"))
				{
					int sp = x.indexOf(' ');
					if (sp > 0)
					{
						tokstr = x.substring(sp).trim();
						Logger.getLogger(ApiConstants.loggerName).info(module 
							+ ".checkToken found tokstr in header: " + tokstr);
					}
				}
			}
		}
	
		if (tokstr == null) // i.e. not in the HTTP header.
		{
			if (secureMode)
			{
				Logger.getLogger(ApiConstants.loggerName).warning(module 
					+ ".checkToken Secure Mode requires token in HTTP Authorization Bearer header.");
				return null;
			}
			else if (urlToken == null)
			{
				Logger.getLogger(ApiConstants.loggerName).warning(module 
					+ ".checkToken Token not provided, neither in URL or HTTP Header..");
				return null;
			}
			tokstr = urlToken;
		}
		
		// Search for requested token, and cull the list of expired tokens.
		UserToken userToken = null;
		long now = System.currentTimeMillis();
		for(Iterator<UserToken> tokit = activeTokens.iterator(); tokit.hasNext(); )
		{
			UserToken t = tokit.next();
			if (now - t.getLastUsed() > TOKEN_MAX_AGE)
				tokit.remove();
			else if (tokstr.equals(t.getToken()))
				userToken = t;
		}
		if (userToken == null)
			throw new WebAppException(ErrorCodes.NO_SUCH_OBJECT, "Invalid token string provided.");
		
		userToken.touch();
		return userToken;
	}
	
	public static final long STALE_THRESHOLD_MS = 90000L;
	
	/**
	 * Called periodically from StaleClientChecker to hang up any DDS clients or event clients
	 * that have gone stale, e.g. client starts a message retrieval and never completes it, or
	 * event clients with a stale heartbeat.
	 */
	public synchronized void checkStaleConnections()
	{
		for(UserToken tok : activeTokens)
		{
			ApiLddsClient cli = tok.getLddsClient();
			if (cli != null && System.currentTimeMillis() - cli.getLastActivity() > STALE_THRESHOLD_MS)
			{
				cli.info("Hanging up due to " + (STALE_THRESHOLD_MS/1000) + " seconds of inactivity.");
				cli.disconnect();
				tok.setLddsClient(null);
				tok.setSearchCrit(null);
			}
			
			try(DbInterface dbi = new DbInterface();
				ApiAppDAO appDao = new ApiAppDAO(dbi))
			{
				ArrayList<ApiAppStatus> appStatii = appDao.getAppStatus();
				HashMap<Long,ApiEventClient> evclients = tok.getEventClients();
				for(Iterator<Map.Entry<Long,ApiEventClient>> eit = evclients.entrySet().iterator();
					eit.hasNext(); )
				{
					Map.Entry<Long,ApiEventClient> entry = eit.next();
					Long appId = entry.getKey();
					ApiEventClient evcli = entry.getValue();
					for(ApiAppStatus appStat : appStatii)
					{
						if (appId == appStat.getAppId())
						{
							if (appStat.getPid() == null || appStat.getHeartbeat() == null
							 || System.currentTimeMillis() - appStat.getHeartbeat().getTime() > 20000L)
							{
								evcli.disconnect();
								eit.remove();
							}
							break;
						}
					}
				}
			}
			catch(DbException ex)
			{
				System.out.println("Error checking event clients: " + ex);
				ex.printStackTrace(System.out);
			}
		}
	}
}
