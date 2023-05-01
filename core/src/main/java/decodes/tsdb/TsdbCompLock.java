/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*/
package decodes.tsdb;

import ilex.util.Logger;
import ilex.util.ServerLock;

import java.net.InetAddress;
import java.util.Date;

import decodes.sql.DbKey;

/**
 * This object represents an application lock in the database.
 */
public class TsdbCompLock
{
	/** Application ID being locked.  */
	private DbKey appId;
	
	/** Process ID that locked the application ID.  */
	private int pid;
	
	/** The host name where the application is running.  */
	private String host;
	
	/**
	 * An application must update its 'heartbeat' time in the database
	 * at least once per minute. Otherwise, we assume it's dead.
	 */
	private Date heartbeat;

	private String status;

	/** Locks with heartbeats more than this old are considered stale. */
	public static final long LockStaleMS = 15000L;
	
	/** 
	 * For XML databases, a TsdbCompLock is mapped to a ServerLock binary file.
	 * Store the ServerLock object so that setHeartBeat and setStatus methods will
	 * be delegated to the ServerLock.
	 */
	private ServerLock serverLock = null;
	
	/**
	 * The name of the loading app in the CompAppInfo record.
	 */
	private String appName = null;


	/**
	 * Constructor.
	 * @param appId the application ID.
	 * @param pid the PID on the host name.
	 * @param host the host name where the app is running.
	 * @param heartbeat stored in database & used to detect dead apps.
	 */
	public TsdbCompLock(DbKey appId, int pid, String host, Date heartbeat,
		String status)
	{
		this.appId = appId;
		this.pid = pid;
		this.host = host;
		this.heartbeat = heartbeat;
		this.status = status;
	}

	/**
	 * @return application ID being locked.
	 */
	public DbKey getAppId() { return appId; };
	
	/**
	 * Sets application ID being locked.
	 * @param x the App ID.
	 */
	public void setAppId(DbKey x) { appId = x; };
	
	/**
	 * @return Process ID being locked.
	 */
	public int getPID() { return pid; };
	
	/**
	 * Sets Process ID being locked.
	 * @param x the process ID.
	 */
	public void setPID(int x) { pid = x; };
	
	/**
	 * @return host for this lock.
	 */
	public String getHost() { return host; };
	
	/**
	 * Sets host for this lock.
	 * @param x the host.
	 */
	public void setHost(String x) { host = x; };
	
	/**
	 * @return heartbeat.
	 */
	public Date getHeartbeat() { return heartbeat; };
	
	/**
	 * Sets heartbeat.
	 * @param x the heartbeat.
	 */
	public void setHeartbeat(Date x) { heartbeat = x; };
	
	public String toString()
	{
		return "appId=" + appId + ", pid=" + pid
			+ ", host=" + host + ", heartbeat=" + heartbeat
			+ ", status=" + status + ", name=" + appName;
	}

	public void setStatus(String status)
	{
		this.status = status;
		if (serverLock != null)
			serverLock.setAppStatus(status);
	}

	public String getStatus() { return status; }
	
	/**
	 * A lock is stale if it has no heartbeat or if the hearbeat is older
	 * than LockStaleMS.
	 * @return true if this lock is to be considered stale.
	 */
	public boolean isStale()
	{
		if (heartbeat == null)
			return true;
		return System.currentTimeMillis() - heartbeat.getTime() > LockStaleMS;
	}

	public ServerLock getServerLock()
	{
		return serverLock;
	}

	public void setServerLock(ServerLock serverLock)
	{
		this.serverLock = serverLock;
	}

	public String getAppName()
	{
		return appName;
	}

	public void setAppName(String appName)
	{
		this.appName = appName;
	}
	
	/**
	 * @return true if it can be determined that the process is running on this host.
	 */
	public boolean isRunningLocally()
	{
		if (host == null || host.trim().length() == 0)
			return true;
		try
		{
			InetAddress localHost = InetAddress.getLocalHost();
			if (localHost == null)
				return true;
			InetAddress ih = InetAddress.getByName(host);
			if (localHost.equals(ih))
				return true;
		}
		catch (Exception ex)
		{
			Logger.instance().warning("isRunningLocally() cannot check inet address: " + ex);
		}
		return false;
	}
}
