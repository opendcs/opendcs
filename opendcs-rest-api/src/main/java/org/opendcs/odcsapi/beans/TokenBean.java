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

package org.opendcs.odcsapi.beans;

public class TokenBean
{
	private String token = "";
	private String username = "";
	private long lastUsed = 0L;
	
	public String getToken()
	{
		return token;
	}
	public void setToken(String token)
	{
		this.token = token;
	}
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public long getLastUsed()
	{
		return lastUsed;
	}
	public void setLastUsed(long lastUsed)
	{
		this.lastUsed = lastUsed;
	}

}
