package opendcs.opentsdb.hydrojson.beans;

import java.util.Date;
import java.util.Properties;

import decodes.sql.DbKey;

public class LoadingApp
{
	private long appId = DbKey.NullKey.getValue();
	private String appName = null;
	private String appType = null;
	private String comment = null;
	private Date lastModified = null;
	private boolean manualEditingApp = false;
	private Properties properties = new Properties();
	
	public long getAppId()
	{
		return appId;
	}
	public void setAppId(long appId)
	{
		this.appId = appId;
	}
	public String getAppName()
	{
		return appName;
	}
	public void setAppName(String appName)
	{
		this.appName = appName;
	}
	public String getAppType()
	{
		return appType;
	}
	public void setAppType(String appType)
	{
		this.appType = appType;
	}
	public String getComment()
	{
		return comment;
	}
	public void setComment(String comment)
	{
		this.comment = comment;
	}
	public Date getLastModified()
	{
		return lastModified;
	}
	public void setLastModified(Date lastModified)
	{
		this.lastModified = lastModified;
	}
	public boolean isManualEditingApp()
	{
		return manualEditingApp;
	}
	public void setManualEditingApp(boolean manualEditingApp)
	{
		this.manualEditingApp = manualEditingApp;
	}
	public Properties getProperties()
	{
		return properties;
	}
	public void setProperties(Properties properties)
	{
		this.properties = properties;
	}
}
