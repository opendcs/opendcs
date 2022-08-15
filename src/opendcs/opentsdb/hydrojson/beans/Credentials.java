package opendcs.opentsdb.hydrojson.beans;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Credentials
{
	private String username = null;
	private String password = null;
	
	public Credentials() {}
	
	public String getUsername()
	{
		return username;
	}
	public void setUsername(String username)
	{
		this.username = username;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	
}
