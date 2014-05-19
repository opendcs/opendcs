/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/25 19:31:13  mjmaloney
*  Added javadocs & deprecated unused code.
*
*  Revision 1.1  2001/04/06 10:44:36  mike
*  dev
*
*/
package decodes.db;

/**
Throw when database IO error occurs.
*/
public class DatabaseException extends decodes.util.DecodesException
{
	/**
	  Constructs new exception.
	  @param msg the message.
	*/
	public DatabaseException(String msg)
	{
		super(msg);
	}
}

