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

import java.io.IOException;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

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
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		log.debug("Received {}, data.length={}{}",
				  toString(), data.length, (ddsVersion==null ? "" : (" dds="+ddsVersion)));
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
		log.debug("executing HELLO username='{}' for {}", username , ldds.getClientName());

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

		ldds.myStats.setUserName(username);

		// Check to see if user is connecting from a valid IP address.
		PasswordFileEntry pfe = null;
		try
		{
			log.debug("reading PFE for user {}@{}", username, ldds.getClientName());
			pfe = CmdAuthHello.getPasswordFileEntry(username);
			CmdAuthHello.checkValidIpAddress(pfe, ldds, username);
			log.debug("got password file entry for user {}@{}", username, ldds.getClientName());
		}
		catch(UnknownUserException ex)
		{
			// No PW file or no entry for this user -- means unrestricted.
			log.atDebug()
			   .setCause(ex)
			   .log("no password file entry for user {}@{}", username, ldds.getClientName());
			pfe = null;
		}

		// Creating LddsUser might throw UnknownUserException, if so, it will
		// be done with the hangup flag=true, causing disconnection.
		log.debug("Creating user data structure for user {}@{}", username, ldds.getClientName());

		String userRoot = LrgsConfig.instance().ddsUserRootDirLocal;
		LddsUser user = null;
		try
		{
			user = new LddsUser(username, userRoot);
		}
		catch(UnknownUserException ex)
		{
			log.atDebug().setCause(ex).log("No shared user dir for {}", username);
			userRoot = LrgsConfig.instance().ddsUserRootDir;
			try { user = new LddsUser(username, userRoot); }
			catch(UnknownUserException ex2)
			{
				ldds.myStats.setSuccessCode(DdsConnectionStats.SC_BAD_USERNAME);
				// If this throws, allow it to propagate.
				ex2.addSuppressed(ex);
				throw ex2;
			}
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
			x = pfe.getProperty("goodOnly");
			if (x != null)
				user.setGoodOnly(TextUtil.str2boolean(x));
		}

		// Callback to thread to attach to LRGS as this user.
		log.debug("Attaching slot for {}@{}", username, ldds.getClientName());
		ldds.attachLrgs(user);

		if (user.isSuspended())
		{
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_ACCOUNT_SUSPENDED);
			throw new LddsRequestException("Account suspended.", LrgsErrorCode.DDDSAUTHFAILED, true);
		}

		if (pfe != null)
		{
			CmdAuthHello.getDcpLimit(pfe, ldds);
		}
		log.debug("Attached for user {}@{}", username, ldds.getClientName());

		// Echo HELLO with username and proto version as an acknowledgement.
		LddsMessage msg = new LddsMessage(LddsMessage.IdHello,
			username + " " + DdsVersion.DdsVersionNum);
		ldds.send(msg);
		log.debug("Sent HELLO Acceptance to {}", ldds.getClientName());

		return 0;
	}

	public String toString()
	{
		return "HELLO from '" + username + "'";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdHello; }
}
