/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 14:50:25  mjmaloney
*  Javadocs
*
*  Revision 1.2  2002/05/18 20:02:09  mjmaloney
*  Added prompt to PasswordFileEditor, this is now an option on CmdLineProcessor.
*
*  Revision 1.1  1999/09/16 14:52:43  mike
*  9/16/1999
*
*
*/

package ilex.util;

import java.util.Vector;

/**
Works with CmdLine and CmdLineProcessor to create applications that
parse command-line input
This class holds the list of CmdLine objects.
*/
public class CmdLineList extends Vector
{
	/**
	* Adds a CmdLine to the list
	* @param o the CmdLine.
	* @return true
	*/
	public boolean add( Object o ) throws IllegalArgumentException
	{
		if (!(o instanceof CmdLine))
			throw new IllegalArgumentException(
				"CmdLineList can only hold CmdLine objects");
		return super.add(o);
	}

	/**
	* Subclass and override unrecognizedCmd if you want to handle
	* unrecognized keywords in a special way. The default behavior
	* is to print a message to System.err
	* @param tokens the tokens seen on the command line
	*/
	public void unrecognizedCmd( String[] tokens )
	{
		if (tokens != null)
			System.err.println("Unrecognized cmd '" + tokens[0] + "' (type 'help' for list)");
		else
			System.err.println("Empty command received");
	}
}
