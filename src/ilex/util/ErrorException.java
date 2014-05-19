/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/30 15:43:59  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.2  2004/08/30 14:50:26  mjmaloney
*  Javadocs
*
*  Revision 1.1  2001/01/24 02:17:14  mike
*  Added ErrorException and WarningException for File parsers.
*
*/
package ilex.util;

/**
* ErrorException is a non-recoverable exception. It means that the requested
* operation was not completed successfully.
*/
public class ErrorException extends IlexException
{
	/**
	* Constructor.
	* @param msg the message.
	*/
	public ErrorException( String msg )
	{
		super("Error: " + msg);
	}
}

