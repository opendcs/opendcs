/*
*  $Id$
*/
package lrgs.ldds;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Date;
import java.util.NoSuchElementException;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import ilex.util.Logger;
import ilex.util.EnvExpander;
import ilex.util.PasswordFile;
import ilex.util.PasswordFileEntry;
import ilex.util.TextUtil;
import lrgs.common.*;
import lrgs.apiadmin.AuthenticatorString;
import lrgs.db.DbPasswordFile;
import lrgs.db.DdsConnectionStats;
import lrgs.db.LrgsDatabase;
import lrgs.db.LrgsDatabaseThread;
import lrgs.ddsserver.DdsServer;
import lrgs.lrgsmain.LrgsConfig;

/**
This class handles the authenticated hello message on the server side.
*/
public class CmdAuthHello extends LddsCommand
{
	/** The user name */
	String username;

	/** time string supplied by client */
	String timestr;

	/** authenticator supplied by client */
	String authenticator;

	/** Unchangable password file name on the server machine. */
	public static final String LRGS_PASSWORD_FILE_NAME = 
		"$LRGSHOME/.lrgs.passwd";

	/** Unchangable local password file name on the server machine. */
	public static final String LRGS_LOCAL_PASSWORD_FILE_NAME = 
		"$LRGSHOME/.lrgs.passwd.local";

	/** Alternate directory for the password file. */
	public static final String ALT_LRGS_PASSWORD_FILE_NAME = "~/.lrgs.passwd";

	/** Maximum allowable clock difference: 20 min. */
	public static final long MAX_CLOCK_DIFF = (20L * 60L * 1000L);

	private String ddsVersion = null;

	private SimpleDateFormat goesDateFormat;

	/**
	  Create a new 'AuthHello' request with the specified user name.
	  @param data body of the request from client.
	*/
	public CmdAuthHello(byte data[])
	{
		String args = new String(data, 0, 
			(data[data.length-1] == (byte)0) ? data.length-1 : data.length);

		goesDateFormat = new SimpleDateFormat("yyDDDHHmmss");
		TimeZone jtz = TimeZone.getTimeZone("UTC");
		goesDateFormat.setTimeZone(jtz);

		StringTokenizer st = new StringTokenizer(args);

		username = null;
		timestr = null;
		authenticator = null;

		try
		{
			username = st.nextToken();
			timestr = st.nextToken();
			authenticator = st.nextToken();
			if (st.hasMoreTokens())
				ddsVersion = st.nextToken();

			Logger.instance().debug1(DdsServer.module+" Received "
				+ toString()
				+ (ddsVersion==null ? "" : (" dds="+ddsVersion)));
		}
		catch(NoSuchElementException e)
		{
			Logger.instance().warning(
				"Invalid arguments in AuthHello message '" + args + "'");
		}
	}

	/** @return "CmdAuthHello"; */
	public String cmdType()
	{
		return "CmdAuthHello";
	}

	/**
	  Attach to LRGS as the specified user.
	  Throw IllegalArgumentException if the username is unknown or if
	  user is already connected and multiple connections are disallowed.
	  @param ldds the server thread object
	  @throws UnknownUserException if the user is not known.
	  @throws AuthFailedException if authentication or authorization fails.
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		Logger.instance().debug1(DdsServer.module +
			" AuthHello execute for client " + ldds.getClientName());

		if (username == null || timestr == null || authenticator == null)
		{
			Logger.instance().warning(DdsServer.module +
				" Received AuthHello missing required args from client " 
				+ ldds.getClientName());
			throw new UnknownUserException(
				"AuthHello missing required arguments", true);
		}

		long mt = 0;
		try
		{
			Date d = goesDateFormat.parse(timestr);
			mt = d.getTime();
		}
		catch(ParseException pe)
		{
			Logger.instance().warning(DdsServer.module +
				" Received AuthHello with invalid time format from client " 
				+ ldds.getClientName());
			throw new LddsRequestException(toString()
				+ ", invalid time format '" + timestr 
				+ "', must be YYDDDHHMMSS", 
				LrgsErrorCode.DDDSAUTHFAILED, true);
		}

		// Verify that time is within 20 minutes.
		long diff = System.currentTimeMillis() - mt;
		if (diff < 0) 
			diff = -diff;
		if (diff > MAX_CLOCK_DIFF)
		{
			Logger.instance().warning(DdsServer.module +
				" Received AuthHello with clockdiff = " + (diff/1000L)
				+ " seconds from client " + ldds.getClientName());
			throw new LddsRequestException(toString()
				+ ", time stamp difference too large ("
				+ (diff/1000L) + " seconds)",
				LrgsErrorCode.DDDSAUTHFAILED, true);
		}

		PasswordFileEntry pfe;
		try { pfe = getPasswordFileEntry(username); }
		catch(UnknownUserException ex)
		{
			Logger.instance().warning(DdsServer.module +
				" Received AuthHello with bad username '" + username
				+ "' from client " + ldds.getClientName());
			ldds.myStats.setSuccessCode(ldds.myStats.SC_BAD_USERNAME);
			ldds.statLogger.incrBadUsernames();
			ex.setHangup(true);
			throw ex;
		}

		// Make sure this user has the 'dds' role defined.
		if (!pfe.isRoleAssigned("dds"))
		{
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_NO_DDS_PERM);
			String msg = "Rejecting connection from user '" + username 
				+ "' -- does not have permission to use DDS";
			Logger.instance().warning(DdsServer.module + " " + msg);
			throw new AuthFailedException(msg);
		}
		
		// LddsUser ctor will throw UnknownUserException if no sandbox dir.
		boolean isLocal = TextUtil.str2boolean(pfe.getProperty("local"));
		LrgsConfig cfg = LrgsConfig.instance();
		String userRoot = isLocal ? cfg.ddsUserRootDirLocal : cfg.ddsUserRootDir;
		LddsUser user = new LddsUser(username, userRoot);
		if (ddsVersion != null)
			user.setClientDdsVersion(ddsVersion);

		// Construct an authenticator & compare to the one passed.
		AuthenticatorString authstr = null;
		int timet;
		String constructed = "";
		String algo = AuthenticatorString.ALGO_SHA256;
		try
		{
			timet = (int)(mt/1000);
			authstr = new AuthenticatorString(timet, pfe, algo);
			constructed = authstr.getString();
			if (!constructed.equals(authenticator))
			{
				// No match with SHA-256, Try again with SHA.
				authstr = new AuthenticatorString(timet, pfe, algo = AuthenticatorString.ALGO_SHA);
				constructed = authstr.getString();
			}
		}
		catch (java.security.NoSuchAlgorithmException nsae)
		{
			throw new LddsRequestException(algo + " not supported in JVE",
				LrgsErrorCode.DDDSINTERNAL, true);
		}

		if (!constructed.equals(authenticator))
		{
			// Didn't match either SHA256 or SHA.
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_BAD_PASSWORD);
			ldds.myStats.setUserName(username);
			ldds.statLogger.incrBadPasswords();
			String msg = DdsServer.module + 
				" Rejecting connection from user '" + username 
				+ "' -- bad password used from clent " + ldds.getClientName();
			Logger.instance().warning(msg);
			
			// If this is the 3rd consecutive bad password, suspend account for 30 sec.
			if (LrgsConfig.instance().getPasswordChecker() != null)
			{
				LrgsDatabaseThread ldt = LrgsDatabaseThread.instance();
				if (ldt != null)
				{
					LrgsDatabase lrgsDb = ldt.getLrgsDb();
					if (lrgsDb != null)
					{
						if (lrgsDb.getNumConsecutiveBadPasswords(username) == 4)
						{
							Logger.instance().warning(
								"User '" + username + "' -- 5 consecutive bad passwords. "
								+ "Account will be suspended for 5 minutes.");
							user.suspendUntil(new Date(System.currentTimeMillis() + 5*60000L));
						}
					}
				}
			}
			throw new LddsRequestException("Bad password", LrgsErrorCode.DDDSAUTHFAILED, true);
		}
		
		// If I get to here, it means the constructed authenticator matched what the user sent.
		if (algo == AuthenticatorString.ALGO_SHA && LrgsConfig.instance().reqStrongEncryption)
		{
			// The match was with SHA, but this server requires SHA256.
			if (ldds.secondAuthAttempt)
			{
				ldds.myStats.setSuccessCode(DdsConnectionStats.SC_BAD_PASSWORD);
				ldds.myStats.setUserName(username);
				ldds.statLogger.incrBadPasswords();
				String msg = DdsServer.module + 
					" Rejecting connection from user '" + username 
					+ "' -- bad password used from clent " + ldds.getClientName();
				Logger.instance().warning(msg);
				throw new LddsRequestException("Server requires SHA-256.", LrgsErrorCode.DDDSAUTHFAILED, true);
			}
			else // Allow them to try again with SHA-256
			{
				String msg = DdsServer.module + 
					" Received SHA auth from user '" + username 
					+ "' -- but this server requires SHA-256. " + ldds.getClientName();
				Logger.instance().debug1(msg);
				ldds.secondAuthAttempt = true;
				throw new LddsRequestException("Server requires SHA-256.", LrgsErrorCode.DSTRONGREQUIRED, false);
			}
		}

		// If I get to here, the user is authenticated, wither with SHA-256 or SHA.
		
		// Some users are restricted by IP Address
		checkValidIpAddress(pfe, ldds, username);

		user.isAuthenticated = true;
		user.setLocal(isLocal);
		try
		{
			// Session key will use the same algorithm as selected above.
			user.setSessionKey(AuthenticatorString.makeAuthenticator(
				authstr.getString().getBytes(), pfe.getShaPassword(), timet, algo));
			user.isAdmin = pfe.isRoleAssigned("admin");
			if (!isLocal && cfg.localAdminOnly)
				user.isAdmin = false;
		}
		catch (java.security.NoSuchAlgorithmException nsae)
		{
			// Can't happen if we got this far!
		}
		
		// Callback to thread to attach to LRGS as this user.
		ldds.attachLrgs(user);

		// OpenDCS 6.2 NOAA enhancements for suspending an account.
		if (user.isSuspended())
		{
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_ACCOUNT_SUSPENDED);
			throw new LddsRequestException("Account suspended.", LrgsErrorCode.DDDSAUTHFAILED, true);
		}

		String x = pfe.getProperty("disableBackLinkSearch");
		if (x == null)
			x = pfe.getProperty("forceAscending");
		if (x != null)
			user.setDisableBackLinkSearch(TextUtil.str2boolean(x));

		getDcpLimit(pfe, ldds);

		// Echo AuthHELLO with username and proto version as an acknowledgement.
		LddsMessage msg = new LddsMessage(LddsMessage.IdAuthHello, 
			username + " " + timestr + " " + DdsVersion.DdsVersionNum);
		ldds.send(msg);

		if (user.isAdmin)
			Logger.instance().info(DdsServer.module + " ADMIN authenticated connection from "
				+ ldds.getClientName() + " (algo=" + algo + ")");
		else
			Logger.instance().debug1(DdsServer.module + " " + ldds.getClientName() 
				+ " - Successfully authenticated (algo=" + algo + ")!");
		return 0;
	}

	public static void main(String args[])
		throws Exception
	{
		PasswordFileEntry pfe = getPasswordFileEntry(args[0]);
		if (pfe == null)
			System.out.println("No such user");
		else
			System.out.println(pfe.toString());
	}
	/**
	 * Read the password file and extract the entry for the named user.
	 * @param user the user to search for.
	 * @return the PasswordFileEntry for the named user
	 * @throws UnknownUserException if the user cannot be found.
	 */
	public static synchronized PasswordFileEntry getPasswordFileEntry(String user)
		throws UnknownUserException
	{
		LrgsDatabase lrgsDb = LrgsDatabaseThread.instance().getLrgsDb();
		if (lrgsDb != null)
		{
			DbPasswordFile dpf = new DbPasswordFile(null, lrgsDb);
			return dpf.readSingle(user);
		}
		
		// Read the password file entry for this user.
		String fileNames[] = { LRGS_LOCAL_PASSWORD_FILE_NAME,
			LRGS_PASSWORD_FILE_NAME, ALT_LRGS_PASSWORD_FILE_NAME };
		PasswordFile pf;
		for(String fn : fileNames)
		{
			String pfname = EnvExpander.expand(fn);
			try
			{
				pf = new PasswordFile(new File(pfname));
				pf.read();
				Logger.instance().debug1(DdsServer.module 
					+ " Read main password file at " + pfname);
				PasswordFileEntry pfe = pf.getEntryByName(user);
				if (pfe != null)
				{
					if (fn.equals(LRGS_LOCAL_PASSWORD_FILE_NAME))
					{
						pfe.setProperty("local", "true");
						pfe.setLocal(true);
					}
					return pfe;
				}
				Logger.instance().debug1(DdsServer.module
					+ " No such user '" + user + "' in file '" + pfname
					+ "'");
			}
			catch(IOException ioe)
			{
				Logger.instance().debug1("Cannot read password file '" 
					+ pfname + "'");
			}
		}
		throw new UnknownUserException("No such user '" + user + "'");
	}

	/**
	 * Checks to see if the user is connecting from a valid IP address if a
	 * restriction is defined for this user.
	 * @throws AuthFailedException if invalid
	 */
	public static void checkValidIpAddress(PasswordFileEntry pfe, 
		LddsThread ldds, String username)
		throws AuthFailedException
	{
		if (pfe == null)
			return; // No restrictions if no entry in password file.

		// Check to see if user is connecting from a valid IP address.
		String addrList = pfe.getProperty("ipaddr");
		if (addrList == null)
			return;

		String sockIpAddr = ldds.getSocket().getInetAddress().getHostAddress();

		StringTokenizer st = new StringTokenizer(addrList, ";");
		boolean matchFound = false;
		while(st.hasMoreTokens())
		{
			String allowedIpAddr = st.nextToken();
			int idx = allowedIpAddr.indexOf('*');
			if (idx != -1)
				allowedIpAddr = allowedIpAddr.substring(0, idx);
			if (sockIpAddr.startsWith(allowedIpAddr))
			{
				matchFound = true;
				break;
			}
		}
		if (!matchFound)
		{
			ldds.myStats.setSuccessCode(DdsConnectionStats.SC_BAD_IP_ADDR);
			String msg = DdsServer.module + " User '" + username +
				"' Attempt to connect from disallowed IP Address ("
				+ sockIpAddr + ")";
			Logger.instance().warning(msg);
			throw new AuthFailedException(msg);
		}
	}

	public static void getDcpLimit(PasswordFileEntry pfe, LddsThread ldds)
	{
		String dcpLimitStr;
		if (pfe != null
		 && (dcpLimitStr = pfe.getProperty("maxdcps")) != null)
		{
			try { ldds.user.dcpLimit = Integer.parseInt(dcpLimitStr); }
			catch(NumberFormatException ex)
			{
				Logger.instance().warning("User '" + ldds.getUserName()
					+ "' has invalid maxdcps property in the password file."
					+ " -- no limit will be imposed.");
				ldds.user.dcpLimit = -1;
			}
		}
		else
			ldds.user.dcpLimit = -1;
	}

	/** @return a string representation for log messages. */
	public String toString()
	{
		return "AuthHello from '" + username + "'";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdAuthHello; }
}
