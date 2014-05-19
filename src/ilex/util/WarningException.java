/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 15:44:02  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.3  2004/08/30 14:50:32  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/05/12 12:36:46  mjmaloney
*  Added FailureException & FatalException.
*  All three IlexException sub-classes modified to log messages.
*
*  Revision 1.1  2001/01/24 02:17:14  mike
*  Added ErrorException and WarningException for File parsers.
*
*/
package ilex.util;

/**
* WarningException is a non-fatal recoverable exception. It means that the
* requested operation was completed successfully. It is typically used
* in a FileExceptionList for parsing.
*/
public class WarningException extends IlexException
{
	/**
	* @param msg the message.
	*/
	public WarningException( String msg )
	{
		super("Warning: " + msg);
		Logger.instance().log(Logger.E_WARNING, msg);
	}
}

