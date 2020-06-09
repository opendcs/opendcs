/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2005/03/07 21:33:49  mjmaloney
*  dev
*
*  Revision 1.5  2004/08/30 14:51:45  mjmaloney
*  Javadocs
*
*  Revision 1.4  2003/05/03 12:33:32  mjmaloney
*  Updates
*
*  Revision 1.3  2002/06/18 19:19:21  mjmaloney
*  Use Logger class rather than LrgsEventQueue directly.
*
*  Revision 1.2  1999/10/15 10:13:55  mike
*  Moved LddsInputStream and LddsMessage to lrgs.common package. Added import
*  statements in the 'Cmd' classes that need these.
*
*  Revision 1.1  1999/10/04 17:57:53  mike
*  First complete implementation
*
*
*/

package lrgs.ldds;

import java.io.IOException;

import ilex.util.Logger;
import lrgs.common.*;

/**
  This command simply echos back to the client
*/
public class CmdEcho extends LddsCommand
{
	LddsMessage msg;
	char cmdCode;

	/** 
	  Constructor.
	  @param msg the complete message from the client.
	*/
	public CmdEcho(LddsMessage msg, char cmdCode)
	{
		this.msg = msg;
	}

	/**
	  Executes the command.
	  @param ldds the server thread object holding connection to client.
	*/
	public int execute(LddsThread ldds) throws IOException
	{
		ldds.send(msg);
		return 0;
	}

	/** @return "CmdEcho"; */
	public String cmdType()
	{
		return "CmdEcho";
	}

	/** @return the code associated with this command. */
	public char getCommandCode() { return cmdCode; }
}

