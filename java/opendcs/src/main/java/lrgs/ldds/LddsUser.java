/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.ldds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.EnvExpander;

/**
Holds the remote user's name and the sandbox directory.
*/
public class LddsUser
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.debug("Cannot access directory for user {}, path='{}", name, dirpath);
			throw new UnknownUserException("No such LRGS User '" + name + "'", true);
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

		try (DataInputStream dis = new DataInputStream(new FileInputStream(suspendFile)))
		{
			Date suspendDate = new Date(dis.readLong());
			return suspendDate;
		}
		catch (FileNotFoundException ex)
		{
			return null;
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("Error reading '{}'", suspendFile.getPath());
			suspendFile.delete();
			return null;
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

		try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(suspendFile)))
		{
			dos.writeLong(d.getTime());
		}
		catch (Exception ex)
		{
			log.atWarn().setCause(ex).log("Error writing '{}'", suspendFile.getPath());
		}
	}

	public boolean isGoodOnly()
	{
		return goodOnly;
	}

	public void setGoodOnly(boolean goodOnly)
	{
		this.goodOnly = goodOnly;
	}
}
