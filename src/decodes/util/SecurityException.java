/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/27 20:50:30  mjmaloney
*  javadocs
*
*  Revision 1.1  2001/04/06 10:44:40  mike
*  dev
*
*/
package decodes.util;

/**
This is thrown by methods that check access privileges within Decodes.
These include LRGS interfaces (which must connect to an LRGS with a
valid user name), and SQL databases.
*/
public class SecurityException extends DecodesException
{
	/**
	  Constructor
	  @param msg the message
	*/
	public SecurityException(String msg)
	{
		super(msg);
	}
}

