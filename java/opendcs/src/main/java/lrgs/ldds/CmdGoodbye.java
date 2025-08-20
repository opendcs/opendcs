/*
*  $Id$
*/

package lrgs.ldds;

import ilex.util.Logger;

/**
This command signals the end of the session.
*/
public class CmdGoodbye extends LddsCommand
{
	/**
	  Executes the command.
	  The server will hangup.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds)
	{
		Logger.instance().debug1(
			"Executing GOODBYE for " + ldds.getClientName());

		try
		{
			LddsMessage msg = new LddsMessage(LddsMessage.IdGoodbye, "");
			ldds.send(msg);
			Thread.sleep(100); 
		}
		catch (Exception ex) 
		{
		}
		ldds.disconnect();
		return 0;
	}

	public String cmdType()
	{
		return "CmdGoodbye";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return LddsMessage.IdGoodbye; }
}
