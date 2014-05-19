/*
*  $Id$
*/
package lrgs.ldds;

import java.io.File;
import java.io.IOException;
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

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;

import lrgs.lrgsmain.LrgsConfig;

/**
This is an abstract base-class for all administrative commands.
It ensures that the check for administrative privilege is done in
a unified, correct manner.
*/
public abstract class CmdAdminCmd extends LddsCommand
{
	/** Constructor does nothing. */
	public CmdAdminCmd()
	{
		super();
	}

	/**
	  Checks for an authenticated user with admin priviledge and throws
	  and exception if not. Admin command should NOT overload this method.
	  Instead, they must implement 'executeAdmin(LddsThread ldds)'.
	  @param ldds the server thread object
	*/
	public int execute(LddsThread ldds)
		throws ArchiveException, IOException
	{
		checkAdminPriviledge(ldds);
		return executeAdmin(ldds);
	}

	public static final void checkAdminPriviledge(LddsThread ldds)
		throws ArchiveException
	{
		if (ldds.user == null)
			throw new UnknownUserException(
				"Authenticated Login required prior to user commands.");
		if (!ldds.user.isAuthenticated)
		{
			Logger.instance().warning("User " + ldds.getName() +
		" attempted admin command when not authenticated -- rejected.");
			throw new ArchiveException("Admin commands require authentication!",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		}
		if (!ldds.user.isAdmin)
		{
			String msg = "User " + ldds.getName() +
		" attempted admin command without admin permission -- rejected.";
			Logger.instance().warning(msg);
			throw new ArchiveException(msg, LrgsErrorCode.DDDSAUTHFAILED, false);
		}
		ldds.myStats.setAdmin_done(true);
	}

	/**
	 * Admin commands must supply this method.
	 * @param ldds the server thread object
	 */
	public abstract int executeAdmin(LddsThread ldds)
		throws ArchiveException, IOException;
}
