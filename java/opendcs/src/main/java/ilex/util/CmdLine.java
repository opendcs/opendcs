/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2014/06/27 20:34:40  mmaloney
*  Added usage() method.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:25  mjmaloney
*  Javadocs
*
*  Revision 1.4  2000/03/13 15:35:01  mike
*  PasswordFileEditor complete.
*
*  Revision 1.3  1999/11/11 16:19:03  mike
*  Add support for help message for each command.
*
*  Revision 1.2  1999/09/16 16:25:21  mike
*  created
*
*  Revision 1.1  1999/09/16 14:52:43  mike
*  9/16/1999
*
*
*/

package ilex.util;

import java.io.EOFException;
import java.io.IOException;

/**
Works with CmdLineList and CmdLineProcessor to create applications that
parse command-line input
This abstract class is sub-classed for each command keyword.
*/
public abstract class CmdLine
{
	/** The command keyword, i.e. the first word of the command */
	public String keyword;

	/** A help message displayed on syntax error. */
	public String helpmsg;

	/**
	* Constructor.
	* @param kw the keyword
	*/
	protected CmdLine( String kw )
	{
		keyword = kw;
		helpmsg = "";
	}

	/**
	* Constructor.
	* @param kw the keyword
	* @param help the help message
	*/
	protected CmdLine( String kw, String help )
	{
		this(kw);
		helpmsg = help;
	}

	/**
	* Called when the processor matches this keyword.
	* Subclass should override to execute the command.
	* @param tokens all command line words: token[0]==keyword
	* @throws EOFException to cause processor to halt
	*/
	public abstract void execute( String[] tokens )
		throws IOException, EOFException;

	/**
	* Convenience method for sub-classes: call this with the required
	* number of tokens as an initial syntax check.
	* @param min minimum number of tokens
	* @param tokens Tokens actually seen
	* @return true if OK to proceed with execute
	*/
	public boolean requireTokens( int min, String[] tokens )
	{
		if (tokens.length >= min)
			return true;

		usage();
		return false;
	}
	
	public void usage()
	{
		System.out.println("Usage: " + keyword + " " + helpmsg);
	}
}
