/*
*  $Id$
*/
package lrgs.ldds;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import ilex.util.AuthException;
import ilex.util.ByteUtil;
import ilex.util.DesEncrypter;
import ilex.util.FileUtil;
import ilex.util.Logger;
import ilex.util.EnvExpander;
import ilex.util.PasswordFile;
import ilex.util.PasswordFileEntry;
import ilex.util.PropertiesUtil;
import ilex.util.TextUtil;

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;

import lrgs.lrgsmain.LrgsConfig;

/**
This class handles the 'User' command on the LRGS server.
*/
public class CmdUser extends LddsCommand
{
	/** The sub-command */
	String subcmd;

	/** The user name */
	String username;

	/** encrypted authenticator for 'set' subcmd */
	String encAuth;

	/** roles for 'set' subcmd */
	String roles;

	/** Property Assignments string */
	String propAssigns;

	/**
	  Create a new 'User' request with the specified user name.
	  Supported formats:
		set name [authenticator] [roles] [properties]
			- Must either have authenticated connection as user 'name',
			  or be logged in as a user with admin permissions on this system.
			- 'properties' is comma-separated list of name=value pairs.
			- 'roles' is comma-separated list of words.
			- Respond OK or Error
		list
			- Must be logged in as a user with admin permissions on this system.
			- Respond with nl-separated list of user names & permissions
		rm name
			- Must be logged in as a user with admin permissions on this system.
			- Respond OK or Error
	  @param data body of the request from client.
	*/
	public CmdUser(byte data[])
	{
		String args = new String(data, 0, 
			(data[data.length-1] == (byte)0) ? data.length-1 : data.length);

		StringTokenizer st = new StringTokenizer(args);
		subcmd = st.hasMoreTokens() ? st.nextToken() : null;
		username = st.hasMoreTokens() ? st.nextToken() : null;
		encAuth = st.hasMoreTokens() ? st.nextToken() : null;
		roles = st.hasMoreTokens() ? st.nextToken() : null;
		propAssigns = st.hasMoreTokens() ? st.nextToken() : null;
//System.out.println("CmdUser subcmd='" + subcmd + "', user='" + username
//+ "' encAuth='" + encAuth + "' roles='" + roles + ", props='" + propAssigns+ "'");
	}

	/** @return "CmdUser"; */
	public String cmdType()
	{
		return "CmdUser";
	}

	/**
	  Attach to LRGS as the specified user.
	  Throw IllegalArgumentException if the username is unknown or if
	  user is already connected and multiple connections are disallowed.
	  @param ldds the server thread object
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (subcmd == null)
			throw new ArchiveException("Missing user subcommand!",
				LrgsErrorCode.DBADKEYWORD, false);

		if (ldds.user == null)
			throw new UnknownUserException(
				"Authenticated Login required prior to user commands.");
		if (!ldds.user.isAuthenticated)
		{
			Logger.instance().warning("User " + ldds.getName() +
		" attempted user-admin command when not authenticated -- rejected.");
			throw new ArchiveException("User commands require authentication!",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		}

		try
		{
			if (subcmd.equalsIgnoreCase("list"))
				return list(ldds);
			else if (subcmd.equalsIgnoreCase("rm"))
				return rm(ldds);
			else if (subcmd.equalsIgnoreCase("set"))
				return set(ldds);
			else
				throw new ArchiveException("Unrecognized user subcommand '" 
					+ subcmd + "'!",
                	LrgsErrorCode.DBADKEYWORD, false);
		}
		catch(ArchiveException aex) { throw aex; }
		catch(Exception ex)
		{
			System.err.println(ex);
			ex.printStackTrace();
			throw new ArchiveException("Unexpected Exception '" 
				+ subcmd + "': " + ex, LrgsErrorCode.DDDSINTERNAL, false);
		}
	}

	private int list(LddsThread ldds)
		throws ArchiveException, IOException
	{
		CmdAdminCmd.checkAdminPriviledge(ldds);

		StringBuilder sb = new StringBuilder();
		listDir(false, sb);
		listDir(true, sb);
		LddsMessage msg = new LddsMessage(LddsMessage.IdUser, sb.toString());
		ldds.send(msg);

		return 0;
	}
	
	private void listDir(boolean local, StringBuilder sb)
	{
		LrgsConfig cfg = LrgsConfig.instance();
		String dirName = local ? cfg.ddsUserRootDirLocal
			: cfg.ddsUserRootDir;
		File dir = new File(EnvExpander.expand(dirName));
		String ls[] = null;
		try { ls = dir.list(); }
		catch(Exception ex)
		{
			String msg = "Cannot list user dir '"
				+ dir.getPath() + "': " + ex.getMessage();
			if (!local)
				Logger.instance().warning(msg);
			else
				Logger.instance().debug1(msg);
			return;
		}
		if (ls == null || ls.length == 0)
		{
			String msg = "User dir '" + dir.getPath() + "' is empty.";
			if (!local)
				Logger.instance().warning(msg);
			else
				Logger.instance().debug1(msg);
			return;
		}

		PasswordFile pf = readPasswordFile(local);
		for(String uname : ls)
		{
			File userDir = new File(dir, uname);
			if (!userDir.isDirectory())
				continue;
			PasswordFileEntry pfe = 
				pf == null ? null : pf.getEntryByName(uname);
			if (pfe != null)
			{
				sb.append(uname + " + ");
				String roles[] = pfe.getRoles();
				if (roles == null || roles.length == 0)
					sb.append("-");
				else
				{
					for(int i=0; i<roles.length; i++)
					{
						sb.append(roles[i]);
						if (i < roles.length-1)
							sb.append(',');
					}
				}
				sb.append(' ');
				if (local)
					pfe.setProperty("local", "true");
				sb.append(pfe.getPropertiesString());
			}
			else // Not in password file, no passwd, DDS role, no props.
				sb.append(uname + " - dds -");
			sb.append("\n");
		}
	}

	private int rm(LddsThread ldds)
		throws ArchiveException, IOException
	{
		CmdAdminCmd.checkAdminPriviledge(ldds);
		int actionsTaken = 0;

		if (username == null)
			throw new ArchiveException("No username specified to remove!",
				LrgsErrorCode.DINVALIDUSER, false);

		String dirName = 
			EnvExpander.expand( LrgsConfig.instance().ddsUserRootDir) 
			+ File.separator + username;
		File dir = new File(dirName);
		if (dir.isDirectory())
		{
			if (!FileUtil.deleteDir(dir))
			{
				String msg = "Cannot remove dir '"
					+ dir.getPath() + "'!";
				Logger.instance().warning(msg);
			}
			else
				actionsTaken++;
		}
		
		dirName = 
			EnvExpander.expand( LrgsConfig.instance().ddsUserRootDirLocal) 
			+ File.separator + username;
		dir = new File(dirName);
		if (dir.isDirectory())
		{
			if (!FileUtil.deleteDir(dir))
			{
				String msg = "Cannot remove dir '"
					+ dir.getPath() + "'!";
				Logger.instance().warning(msg);
			}
			else
				actionsTaken++;
		}
		
		PasswordFile pf = readPasswordFile(false);
		if (pf != null)
		{
			try
			{
				if (pf.rmEntryByName(username))
					pf.write();
				actionsTaken++;
			}
			catch(IOException ioe)
			{
				String msg = "Cannot write password file '"
					+ pf.getFile().getPath() 
					+ "': " + ioe;
				Logger.instance().warning(msg);
			}
		}
		pf = readPasswordFile(true);
		if (pf != null)
		{
			try
			{
				if (pf.rmEntryByName(username))
					pf.write();
				actionsTaken++;
			}
			catch(IOException ioe)
			{
				String msg = "Cannot write password file '"
					+ pf.getFile().getPath() 
					+ "': " + ioe;
				Logger.instance().warning(msg);
			}
		}

		if (actionsTaken == 0)
		{
			throw new LddsRequestException("Could not delete user '"
				+ username + "'", LrgsErrorCode.DDDSINTERNAL, false);
		}
		String txt = "Removed DDS user '" + username + "'";
		LddsMessage msg = new LddsMessage(LddsMessage.IdUser, txt);
		ldds.send(msg);
		Logger.instance().info(txt);
		return 0;
	}

	private int set(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (!ldds.user.isAdmin
		 && (username == null || !username.equals(ldds.getUserName())))
			throw new ArchiveException(
				"Only an administrator on this LRGS can set a user!",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		
		if (username == null)
			throw new ArchiveException("No username specified to add/modify!",
				LrgsErrorCode.DINVALIDUSER, false);
		
		boolean localUser = false;
		
		// If I'm modifying my own account, set default as to local/shared.
		if (username.equals(ldds.getUserName()))
			localUser = ldds.isLocal();
		// Otherwise properties must say whether this is a local/shared user.
		Properties userProps = null;
		if (propAssigns != null && !propAssigns.equals("-"))
		{
			userProps = PropertiesUtil.string2props(propAssigns);
			String pv = userProps.getProperty("local");
			if (pv != null)
			{
				localUser = TextUtil.str2boolean(pv);
				userProps.remove("local");
			}
		}
		
		LrgsConfig cfg = LrgsConfig.instance();
		File userRoot = new File(
			EnvExpander.expand( 
				localUser ? cfg.ddsUserRootDirLocal : cfg.ddsUserRootDir));
		if (!userRoot.isDirectory())
			userRoot.mkdirs();

		File userDir = new File(userRoot, username);
System.out.println("set username=" + username + ", userDir=" + userDir.getPath()
+ ", isLocal=" + localUser + ", logged in as=" + ldds.getUserName()
+ " (local=" + ldds.isLocal() + ")");
		if (!userDir.isDirectory())
		{
			if (!ldds.user.isAdmin)
				throw new ArchiveException(
					"Not authorized make DDS User Directory '"
					+ userDir.getPath() + "'!", LrgsErrorCode.DINVALIDUSER, 
					false);

			File otherDir = new File(
				EnvExpander.expand(
					(localUser ? cfg.ddsUserRootDir : cfg.ddsUserRootDirLocal)
					+ File.separator + username));
			if (otherDir.isDirectory())
				throw new ArchiveException("Cannot make "
					+ (localUser ? "local" : "shared")
					+ " DDS User '" + username + "' because a "
					+ (localUser ? "shared" : "local")
					+ " user with that name exists.",
					LrgsErrorCode.DINVALIDUSER, false);
				
			if (!userDir.mkdir())
				throw new ArchiveException("Cannot make DDS User Directory '"
					+ userDir.getPath() + "'!", LrgsErrorCode.DINVALIDUSER, 
					false);
		}

		String newAuth = null;
		if (encAuth.equals("-"))
			; // '-' means leave password alone -- it may be already set fine.
		else
		{
			// decrypt the password passed from the client.
			try
			{
				String sks = ByteUtil.toHexString(ldds.user.getSessionKey());
//Logger.instance().info("Session Key: " + sks);
				DesEncrypter de = new DesEncrypter(sks);
//Logger.instance().info("    encrypted: " + encAuth);
				newAuth = de.decrypt(encAuth); 
//Logger.instance().info("authenticator: " + newAuth);
			}
			catch(AuthException ex)
			{
				Logger.instance().warning("Cannot decrypt password: " + ex);
				return 0;
			}
		}
//System.out.println("CmdUser set newAuth='" + newAuth + "'");

		PasswordFile pf = readPasswordFile(localUser);
		try
		{
			PasswordFileEntry pfe = pf.getEntryByName(username);
			if (pfe == null)
			{
//System.out.println("Is a new user with name '" + username + "'");
				// New user! If pw specified, create new pw file entry.
				try 
				{
					pfe = new PasswordFileEntry(username); 
					if (newAuth == null)
					{
						// No authenticator provided for a new user?
						// Set auth to all zeros -- will never match anything.
						newAuth = "0000000000000000000000000000000000000000";
					}
					
					pf.addEntry(pfe);
				}
				catch(AuthException ex) { /* won't happen */ }
			}

			if (newAuth != null)
				pfe.setShaPassword(ByteUtil.fromHexString(newAuth));

			// If this is an admin, set the roles specified.
			if (ldds.user.isAdmin && roles != null && pfe != null
			 && !roles.equals("-"))
			{
				pfe.removeAllRoles();
				StringTokenizer st = new StringTokenizer(roles,",");
				while(st.hasMoreTokens())
					pfe.assignRole(st.nextToken());
			}

			// If this is an admin, set the properties.
			if (ldds.user.isAdmin 
			 && propAssigns != null && !propAssigns.equals("-")
			 && pfe != null)
			{
				pfe.setProperties(userProps);
			}

			pf.write();
		}
		catch(IOException ioe)
		{
			throw new LddsRequestException("Cannot write password file '"
				+ pf.getFile().getPath() 
				+ "'", LrgsErrorCode.DDDSINTERNAL, true);
		}

		String txt = "Set DDS user info for '" + username + "'";
		LddsMessage msg = new LddsMessage(LddsMessage.IdUser, txt);
		ldds.send(msg);
//		Logger.instance().info(txt);
		return 0;
	}

	/** @return a string representation for log messages. */
	public String toString()
	{
		return "User from '" + username + "'";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdUser; }

	private PasswordFile readPasswordFile(boolean local)
	{
		PasswordFile pf = null;
		String pfname = local ?
			EnvExpander.expand(CmdAuthHello.LRGS_LOCAL_PASSWORD_FILE_NAME)
			: EnvExpander.expand(CmdAuthHello.LRGS_PASSWORD_FILE_NAME);
		try
		{
			pf = new PasswordFile(new File(pfname));
			pf.read();
		}
		catch(IOException ioe)
		{
			if (local)
				return pf;
			pfname = EnvExpander.expand(
				CmdAuthHello.ALT_LRGS_PASSWORD_FILE_NAME);
			try
			{
				pf = new PasswordFile(new File(pfname));
				pf.read();
			}
			catch(IOException ioe2)
			{
				Logger.instance().warning("Cannot read password file '"
					+ pfname + "': " + ioe2.getMessage());
				return pf;
			}
		}
		return pf;
	}
}
