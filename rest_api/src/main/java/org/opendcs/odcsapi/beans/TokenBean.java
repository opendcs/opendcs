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
