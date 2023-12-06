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
