package opendcs.opentsdb.hydrojson.util;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Immutable token for storing a token string for a limited amount of time.
 */
@XmlRootElement
public class Token
{
	private String token = "";
	private String username = "";
	private long lastUsed = 0L;

	public Token(String token, String username)
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
}
