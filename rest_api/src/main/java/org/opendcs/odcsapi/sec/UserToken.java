package org.opendcs.odcsapi.sec;

import java.util.HashMap;

import org.opendcs.odcsapi.appmon.ApiEventClient;
import org.opendcs.odcsapi.beans.ApiSearchCrit;
import org.opendcs.odcsapi.lrgsclient.ApiLddsClient;

/**
 * Immutable token for storing a token string for a limited amount of time.
 */
public class UserToken
{
	private String token = "";
	private String username = "";
	private long lastUsed = 0L;
	private boolean isManager = false;
	private ApiSearchCrit searchCrit = null;
	private ApiLddsClient lddsClient = null;
	private HashMap<Long, ApiEventClient> eventClients = new HashMap<Long, ApiEventClient>();
	private Long lastDacqEventId = null;

	public UserToken(String token, String username)
	{
		super();
		this.token = token;
		this.username = username;
		touch();
	}

	public String getToken()
	{
		return token;
	}

	public String getUsername()
	{
		return username;
	}

	public long getLastUsed()
	{
		return lastUsed;
	}

	public void touch()
	{
		this.lastUsed = System.currentTimeMillis();
	}

	public boolean isManager()
	{
		return isManager;
	}

	public void setManager(boolean isManager)
	{
		this.isManager = isManager;
	}

	public ApiSearchCrit getSearchCrit()
	{
		return searchCrit;
	}

	public void setSearchCrit(ApiSearchCrit searchCrit)
	{
		this.searchCrit = searchCrit;
	}

	public ApiLddsClient getLddsClient()
	{
		return lddsClient;
	}

	public void setLddsClient(ApiLddsClient lddsClient)
	{
		this.lddsClient = lddsClient;
	}
	
	public ApiEventClient getEventClient(long appId)
	{
		return eventClients.get(appId);
	}
	
	public void setEventClient(Long appId, ApiEventClient evc)
	{
		if (evc == null)
			eventClients.remove(appId);
		else
			eventClients.put(appId, evc);
	}
	
	public HashMap<Long,ApiEventClient> getEventClients()
	{
		return eventClients;
	}

	public Long getLastDacqEventId()
	{
		return lastDacqEventId;
	}

	public void setLastDacqEventId(Long lastDacqEventId)
	{
		this.lastDacqEventId = lastDacqEventId;
	}
}
