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

import lrgs.common.ArchiveException;
import lrgs.common.LrgsErrorCode;

/**
This is an abstract base-class for all administrative commands.
It ensures that the check for administrative privilege is done in
a unified, correct manner.
*/
public abstract class CmdAdminCmd extends LddsCommand
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
			log.warn("User {} attempted admin command when not authenticated -- rejected.", ldds.getName());
			throw new ArchiveException("Admin commands require authentication!",
				LrgsErrorCode.DDDSAUTHFAILED, false);
		}
		if (!ldds.user.isAdmin)
		{
			String msg = "User " + ldds.getName() +
						 " attempted admin command without admin permission -- rejected.";
			log.warn(msg);
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
