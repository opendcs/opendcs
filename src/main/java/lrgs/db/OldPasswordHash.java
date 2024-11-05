package lrgs.db;

import java.util.Date;

public class OldPasswordHash
{
	private Date setTime;
	private String pwHash;
	public OldPasswordHash(Date setTime, String pwHash)
	{
		super();
		this.setTime = setTime;
		this.pwHash = pwHash;
	}
	public Date getSetTime()
	{
		return setTime;
	}
	public String getPwHash()
	{
		return pwHash;
	}
}
