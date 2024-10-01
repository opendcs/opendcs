/*
*  $Id$
*/
package lrgs.ldds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

import ilex.util.Logger;
import ilex.util.EnvExpander;


/**
Holds the remote user's name and the sandbox directory.
*/
public class LddsUser
{
	/** This user's name */
	String name;

	/** The sandbox directory assigned to this user */
	File directory;

	/** True if connection was authenticated */
	public boolean isAuthenticated;

	/** If authenticated, this is a one-time session key for admin functions. */
	private byte[] sessionKey;

	/** True if authenticated and this user has admin priviledges */
	public boolean isAdmin;

	/** DCP Limit for real-time stream, -1 means no limit */
	public int dcpLimit = -1;

	/** Holds the DDS version number of the client - used to maintain compat. */
	private int clientDdsVersionNum = DdsVersion.version_7; // Default to 7 if unknown.
	private String clientDdsVersion = "" + clientDdsVersionNum;
	

	/** True if this user has disabled backward linked-list search */
	private boolean disableBackLinkSearch = false;
	
	/** True if this user should only be sent good quality messages */
	private boolean goodOnly = false;
	
	/** True if this is a local user in .lrgs.passwd.local */
	private boolean local = false;

	/**
	  Constructor.
	  @param name the remote username.
	  @param userRoot the userRoot directory.
	*/
	public LddsUser(String name, String userRoot) 
		throws UnknownUserException
	{
		this.name = name;

		// First check for a sub-directory under Ldds Root:
		String dirpath = EnvExpander.expand(userRoot + 
				File.separatorChar + name);
		directory = new File(dirpath);
		if (!directory.isDirectory())
		{
			Logger.instance().log(Logger.E_DEBUG1,
				"Cannot access directory for user " + name
				+ ", path='" + dirpath + "'");
			throw new UnknownUserException("No such LRGS User '" + name + "'",
				true);
		}
		isAuthenticated = false;
		sessionKey = null;
		isAdmin = false;
	}

	public File getDirectory() { return directory; }

	/**
	 * If the user authenticated, then we will have a one-time key that can
	 * be used for message encryption.
	 * @return the sessionKey or null if this is an unauthenticated user.
	 */
	public byte[] getSessionKey()
	{
		return sessionKey;
	}

	/**
	 * Sets the session key after a successful authentication.
	 * @param sk the one-time session key.
	 */
	public void setSessionKey(byte[] sk)
	{
		sessionKey = sk;
	}

	/** @return the DDS version number of the client. */
	public String getClientDdsVersion() { return clientDdsVersion; }

	/** Sets the DDS version number of the client. */
	public void setClientDdsVersion(String v)
	{
		int i = 0;
		for(; i<v.length(); i++)
			if (!Character.isDigit(v.charAt(i)))
				break;
		if (i > 0)
			clientDdsVersionNum = Integer.parseInt(v.substring(0, i));
		clientDdsVersion = v;
	}
	
	public int getClientDdsVersionNum()
	{
		return clientDdsVersionNum;
	}

	/** Sets the flag to enable/disable backward linked-list searching. */
	public void setDisableBackLinkSearch(boolean tf)
	{
		disableBackLinkSearch = tf;
	}

	/** @return true if this user has disabled backward linked-list searching.*/
	public boolean getDisableBackLinkSearch()
	{
		return disableBackLinkSearch;
	}

	public String getName() { return name; }

	/**
     * @return the _isLocal
     */
    public boolean isLocal()
    {
    	return local;
    }

	/**
     * @param local the _isLocal to set
     */
    public void setLocal(boolean local)
    {
    	this.local = local;
    }
    
    
	/**
	 * If the account is suspended, return the time it is suspended to. Return
	 * null if not suspended.
	 * 
	 * @return null if not suspended, or date/time suspension ends if it is.
	 */
	public Date getSuspendTo()
	{
		File suspendFile = new File(directory, ".suspended");
		DataInputStream dis = null;
//Logger.instance().info("LddsUser.getSuspendTo: file=" + suspendFile.getPath());
		try
		{
			dis = new DataInputStream(new FileInputStream(suspendFile));
			Date suspendDate = new Date(dis.readLong());
//Logger.instance().info("LddsUser.getSuspendTo: suspendDate=" + suspendDate);
			return suspendDate;
		}
		catch (FileNotFoundException ex)
		{
//Logger.instance().info("LddsUser.getSuspendTo -- no file");
			return null;
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Error reading '" + suspendFile.getPath() + "': " + ex);
			suspendFile.delete();
			return null;
		}
		finally
		{
			if (dis != null)
				try { dis.close(); }
				catch (Exception ex) {}
		}
	}
	
	public boolean isSuspended()
	{
		Date d = getSuspendTo();
		if (d == null)
			return false;
		if (d.after(new Date()))
			return true;
		File suspendFile = new File(directory, ".suspended");
		suspendFile.delete();
		return false;
	}
	
	/** Convenient value for permanent suspension. This is Nov 20 12:46:40 2286 */
    public static final Date permSuspendTime = new Date(10000000000000L);

    /**
     * Suspended the user account until the passed time.
     * If passed null then the account is unsuspended.
     * @param d the suspension time or null to unsuspend.
     */
	public void suspendUntil(Date d)
	{
		File suspendFile = new File(directory, ".suspended");
		if (d == null)
		{
			suspendFile.delete();
			return;
		}
		DataOutputStream dos = null;
		try
		{
			dos = new DataOutputStream(new FileOutputStream(suspendFile));
			dos.writeLong(d.getTime());
		}
		catch (Exception ex)
		{
			Logger.instance().warning("Error writing '" + suspendFile.getPath() + "': " + ex);
		}
		finally
		{
			if (dos != null)
				try { dos.close(); }
				catch (Exception ex) {}
		}
	}

	public boolean isGoodOnly()
	{
		return goodOnly;
	}

	public void setGoodOnly(boolean goodOnly)
	{
//Logger.instance().info("User '" + name + "', setting goodOnly=" + goodOnly);
		this.goodOnly = goodOnly;
	}
}
