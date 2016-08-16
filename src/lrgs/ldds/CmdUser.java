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
import lrgs.db.DbPasswordFile;
import lrgs.db.LrgsDatabase;
import lrgs.db.LrgsDatabaseThread;
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
	
	private boolean fromSetPw = false;
	
	// These are the properties that require admin privilege.
	private static final String[] adminProps = 
	{
		"maxdcps", "ipaddr", "suspended"
	};

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
		pw name base64(encrypt(password))
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
		
		propAssigns = getPropAssigns(args);
		
//System.out.println("CmdUser args='" + args + "' propAssigns='" + propAssigns + "'");
//System.out.println("CmdUser subcmd='" + subcmd + "', user='" + username
//+ "' encAuth='" + encAuth + "' roles='" + roles + ", props='" + propAssigns+ "'");
	}
	
	private String getPropAssigns(String args)
	{
		// skip past subcmd
		int idx = args.indexOf(' ');
		if (idx < 0) return null;
		
		// skip past username
		while(idx < args.length() && Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		while(idx < args.length() && !Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		
		// skip past authenticator
		while(idx < args.length() && Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		while(idx < args.length() && !Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		
		// skip past roles
		while(idx < args.length() && Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		while(idx < args.length() && !Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		
		// Skip whitespace
		while(idx < args.length() && Character.isWhitespace(args.charAt(idx))) idx++;
		if (idx >= args.length()) return null;
		// The rest of the string contains property assignments
		return args.substring(idx);


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
			else if (subcmd.equalsIgnoreCase("pw"))
				return pw(ldds);
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
		if (ldds.user == null)
			throw new UnknownUserException(
				"Authenticated Login required prior to user commands.");
		if (!ldds.user.isAuthenticated)
			throw new ArchiveException("Must be authenticated to list user info.", 
				LrgsErrorCode.DDDSAUTHFAILED, false);
		StringBuilder sb = new StringBuilder();
		if (!ldds.user.isAdmin)
		{
			// if NOT an admin, allow the request but only include THIS user's info.
			PasswordFileEntry pfe = CmdAuthHello.getPasswordFileEntry(ldds.user.name);
			if (pfe == null)
				// This shouldn't happen because we are already authenticated.
				// It could happen if user record was deleted since session start.
				throw new UnknownUserException("Cannot find user record for '" + ldds.user.name + "'.");
			else
				addUserToBuffer(pfe, sb, pfe.isLocal());
		}
		else
		{
			listDir(false, sb);
			listDir(true, sb);
		}
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
//TODO handle this
		PasswordFile pf = readPasswordFile(local);
		for(String uname : ls)
		{
			File userDir = new File(dir, uname);
			if (!userDir.isDirectory())
				continue;
			PasswordFileEntry pfe = 
				pf == null ? null : pf.getEntryByName(uname);
			if (pfe != null)
				addUserToBuffer(pfe, sb, local);
			else // Not in password file, no passwd, DDS role, no props.
				sb.append(uname + " - dds -");
			sb.append("\n");
		}
	}
	
	private void addUserToBuffer(PasswordFileEntry pfe, StringBuilder sb, boolean local)
	{
		sb.append(pfe.getUsername() + " + ");
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
		
		LrgsConfig cfg = LrgsConfig.instance();
		try
		{
			LddsUser tu = new LddsUser(pfe.getUsername(), local ? cfg.ddsUserRootDirLocal : cfg.ddsUserRootDir);
			if (tu.getSuspendTo() != null && tu.getSuspendTo().equals(LddsUser.permSuspendTime))
				pfe.setProperty("suspended", "true");
		}
		catch (UnknownUserException e)
		{
			
		}
		sb.append(pfe.getPropertiesString());
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
		
		PasswordFileEntry pfe = CmdAuthHello.getPasswordFileEntry(username);
		if (pfe != null)
		{
			pfe.getOwner().rmEntryByName(username);
			if (!(pfe.getOwner() instanceof DbPasswordFile))
				pfe.getOwner().write();
			actionsTaken++;
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
				"Not authorized to set record for user '" + username + "'!",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		
//System.out.println("set encAuth='" + encAuth + "'");
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
//System.out.println("CmdUser: propAssigns='" + propAssigns + "'");
//System.out.println(">>> reverse from userProps='" + PropertiesUtil.props2string(userProps) + "'");
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
//System.out.println("set username=" + username + ", userDir=" + userDir.getPath()
//+ ", isLocal=" + localUser + ", logged in as=" + ldds.getUserName()
//+ " (local=" + ldds.isLocal() + ")");
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
			// The presence of a password checker indicates that this server wants
			// to enforce complexity, length, etc.
			if (!fromSetPw && LrgsConfig.instance().getPasswordChecker() != null)
				throw new ArchiveException(
					"You cannot set the password from your version of the client software."
					+ " Upgrade to OpenDCS 6.2 or later to use this feature."
					+ " (Contact info@covesw.com for more information).", 
					LrgsErrorCode.DBADPASSWORD, false);
			
			// decrypt the password passed from the client.
			try
			{
				String sks = ByteUtil.toHexString(ldds.user.getSessionKey());
//System.out.println("Session Key: " + sks);
				DesEncrypter de = new DesEncrypter(sks);
//System.out.println("    set: encrypted: " + encAuth);
				newAuth = de.decrypt(encAuth); 
//System.out.println("    set: authenticator: " + newAuth);
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
//System.out.println("CmdUser.set no PFE for '" + username + "'");
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
					pfe.setChanged(true);
					pf.addEntry(pfe);
				}
				catch(AuthException ex) { /* won't happen */ }
			}

			if (newAuth != null)
			{
				pfe.setShaPassword(ByteUtil.fromHexString(newAuth));
				pfe.setChanged(true);
//System.out.println("CmdUser.set set auth to '" + newAuth + "'");
			}

			// If this is an admin, set the roles specified.
			if (ldds.user.isAdmin && roles != null && pfe != null
			 && !roles.equals("-"))
			{
				pfe.removeAllRoles();
				StringTokenizer st = new StringTokenizer(roles,",");
				while(st.hasMoreTokens())
					pfe.assignRole(st.nextToken());
				pfe.setChanged(true);
			}
			
			if (ldds.user.isAdmin && userProps != null)
			{
				boolean isSuspended = TextUtil.str2boolean(PropertiesUtil.getIgnoreCase(userProps, "suspended"));
//System.out.println("CmdUser.set() suspended=" + isSuspended);
				LddsUser user = new LddsUser(username, userRoot.getPath());
				user.suspendUntil(isSuspended ? LddsUser.permSuspendTime : null);
				PropertiesUtil.rmIgnoreCase(userProps, "suspended");
				pfe.setChanged(true);
			}

			// Set properties
			if (propAssigns != null && !propAssigns.equals("-") && pfe != null)
			{
				// Non admin means user setting his/her own record.
				// Disallow changing certain props.
				if (!ldds.user.isAdmin)
					for(int idx = 0; idx < adminProps.length; idx++)
					{
						String val = PropertiesUtil.getIgnoreCase(pfe.getProperties(), adminProps[idx]);
						if (val == null)
							PropertiesUtil.rmIgnoreCase(userProps, adminProps[idx]);
						else
							userProps.setProperty(adminProps[idx], val);
					}
//System.out.println("CmdUser.set() Setting props to '" + PropertiesUtil.props2string(userProps));					
				pfe.setProperties(userProps);
				pfe.setChanged(true);
			}

//System.out.println(">>> reverse from pfe before write='" + PropertiesUtil.props2string(pfe.getProperties()) + "'");
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
	
	private int pw(LddsThread ldds)
		throws ArchiveException, IOException
	{
		if (!ldds.user.isAdmin
		 && (username == null || !username.equals(ldds.getUserName())))
			throw new ArchiveException("Not authorized to set record for user '" + username + "'!",
				LrgsErrorCode.DDDSAUTHFAILED, false);

		
		// Format of data should be: 
		//     pw username base64(encrypt(new text password))
		// Constructor will get username and place base64(encrypt(pw)) into 'encAuth'.
		try
		{
			String sks = ByteUtil.toHexString(ldds.user.getSessionKey());
			DesEncrypter de = new DesEncrypter(sks);
			String newPw = de.decrypt(encAuth);
			
			PasswordFileEntry oldPfe = CmdAuthHello.getPasswordFileEntry(username);
			
			PasswordChecker checker = LrgsConfig.instance().getPasswordChecker();
			PasswordFileEntry newPfe = new PasswordFileEntry(username, newPw);
			String shaPw = ByteUtil.toHexString(newPfe.getShaPassword());
			if (checker != null)
				checker.checkPassword(username, newPw, shaPw);
			
			// Now run the set command with the new authenticator
			String encPw = de.encrypt(shaPw);
			encAuth = encPw;
			roles = null;
			propAssigns = null;
			if (oldPfe.isLocal())
				propAssigns = "local=true";
			fromSetPw = true;
			set(ldds);
		}
		catch(AuthException ex)
		{
			Logger.instance().warning("Cannot decrypt password: " + ex);
			throw new ArchiveException(
				"Cannot decrypt the passed passphrase.",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		}
		
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
		LrgsDatabase lrgsDb = LrgsDatabaseThread.instance().getLrgsDb();
		if (lrgsDb != null)
		{
			DbPasswordFile dpf = new DbPasswordFile(null, lrgsDb);
			try
			{
				if (!local)     // Only fill once for shared, this will get everything, including local.
					dpf.read();
			}
			catch (IOException ex)
			{
				Logger.instance().warning("Error reading DbPasswordFile: " + ex);
			}
			return dpf;
		}

		
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
