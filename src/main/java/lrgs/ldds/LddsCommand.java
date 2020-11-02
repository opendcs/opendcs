/*
*  $Id$
*/
package lrgs.ldds;

import java.io.OutputStream;
import java.io.IOException;

import ilex.util.ArrayUtil;

import lrgs.common.*;

/**
Base class for all server request commands.
*/
public abstract class LddsCommand
{
	/**
	 * @return the command type as a string.
	 */
	public abstract String cmdType();

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	  @throws ArchiveException on request processing error.
	  @throws IOException if error returning response to user (hangup).
	*/
	public abstract int execute(LddsThread ldds)
		throws ArchiveException, IOException;

	/**
	 * @return one of the valid DDS command codes.
	 */
	public abstract char getCommandCode();
}

