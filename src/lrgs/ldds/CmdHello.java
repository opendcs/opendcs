/*
*  $Id$
*/
package lrgs.ldds;

import java.io.IOException;

import ilex.util.Logger;
import ilex.util.PasswordFileEntry;
import ilex.util.TextUtil;

import lrgs.common.*;
import lrgs.db.DdsConnectionStats;
import lrgs.ddsserver.DdsServer;
import lrgs.lrgsmain.LrgsConfig;

/**
This command implements the unauthenticated hello message.
*/
public class CmdHello extends LddsCommand
{
	private String username;
	private String ddsVersion = null;

	/** 
	  Create a new 'Hello' request with the specified user name.
	  @param data the message body containing the user name
	*/
	public CmdHello(byte data[])
	{
		int i;
		for(i = 0; i < data.length && data[i] != (byte)0 
			&& data[i] != (byte)' '; i++);
		username = new String(data, 0, i);
		username = username.trim();

		if (i+1 < data.length && data[i] == (byte)' '
		 && Character.isDigit((char)data[++i]))
		{
			int j=i+1;
			while(j < data.length && data[i] != (byte)0
			 	&& data[i] != (byte)' ')
				j++;
			ddsVersion = new String(data, i, j-i);
		}

		Logger.instance().debug1(DdsServer.module + " Received " 
			+ toString() + ", data.length=" + data.length
			+ (ddsVersion==null ? "" : (" dds="+ddsVersion)));
	}

	/** @return "CmdHello"; */
	public String cmdType()
	{
		return "CmdHello";
	}

	/**
	  Executes the command.
	  Attach to LRGS as the specified user.
	  @param ldds the server thread object holding connection to client.
	  @throws AuthFailedException is user is unknown, or is attempting to 
	          connect from an invalid IP address.
	*/
	public int execute(LddsThread ldds)
		throws IOException, ArchiveException
	{
		Logger.instance().debug1(DdsServer.module
			+ " executing HELLO username='" + username 
			+ "' for " + ldds.getClientName());

		if (ldds.isAuthRequired())
		{
			String s = "Attempted unauthenticated connection by user '" 
				+ username + "' rejected. This server requires authentication.";
			// AuthFailedException will always hangup on user.
			throw new AuthFailedException(s);
		}

		if (username.length() == 0)
		{
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_BAD_USERNAME);
			ldds.statLogger.incrBadUsernames();
			// AuthFailedException will always hangup on user.
			throw new AuthFailedException("Empty user name");
		}

		// Check to see if user is connecting from a valid IP address.
		PasswordFileEntry pfe = null;
		try
		{
			Logger.instance().debug1(DdsServer.module 
				+ " reading PFE for user " + username + " " + ldds.getClientName());
			pfe = CmdAuthHello.getPasswordFileEntry(username);
			CmdAuthHello.checkValidIpAddress(pfe, ldds, username);
			Logger.instance().debug1(DdsServer.module
				+ " got password file entry "
				+ "for user " + username + " " + ldds.getClientName());
		}
		catch(UnknownUserException ex)
		{
			// No PW file or no entry for this user -- means unrestricted.
			Logger.instance().debug1(DdsServer.module
				+ " no password file entry " + "for user " + username 
				+ " " + ldds.getClientName());
			pfe = null;
		}

		// Creating LddsUser might throw UnknownUserException, if so, it will
		// be done with the hangup flag=true, causing disconnection.
		Logger.instance().debug1(DdsServer.module
			+ " creating user data structure for user " + username 
			+ " " + ldds.getClientName());
		
		String userRoot = LrgsConfig.instance().ddsUserRootDirLocal;
		LddsUser user = null;
		try
		{
			user = new LddsUser(username, userRoot);
		}
		catch(UnknownUserException ex)
		{
			Logger.instance().debug1("No shared user dir for " + username);
			userRoot = LrgsConfig.instance().ddsUserRootDir;
			user = new LddsUser(username, userRoot);
			// If this throws, allow it to propegate.
		}
		if (ddsVersion != null)
			user.setClientDdsVersion(ddsVersion);
		if (pfe != null)
		{
			String x = pfe.getProperty("disableBackLinkSearch");
			if (x == null)
				x = pfe.getProperty("forceAscending");
			if (x != null)
				user.setDisableBackLinkSearch(TextUtil.str2boolean(x));
		}

		// Callback to thread to attach to LRGS as this user.
		Logger.instance().debug1(DdsServer.module
			+ " Attaching slot for " + username + " " 
			+ ldds.getClientName());
		ldds.attachLrgs(user);
		if (pfe != null)
		{
			CmdAuthHello.getDcpLimit(pfe, ldds);
		}
		Logger.instance().debug1(DdsServer.module
			+ " attached for user " + username
			+ " " + ldds.getClientName());

		// Echo HELLO with username and proto version as an acknowledgement.
		LddsMessage msg = new LddsMessage(LddsMessage.IdHello, 
			username + " " + DdsVersion.DdsVersionNum);
		ldds.send(msg);
		Logger.instance().debug1(DdsServer.module
			+ " Sent HELLO Acceptance to " + ldds.getClientName()
			+ " " + ldds.getClientName());

		return 0;
	}

	public String toString()
	{
		return "HELLO from '" + username + "'";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdHello; }
}
