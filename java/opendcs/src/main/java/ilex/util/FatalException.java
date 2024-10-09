/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 15:44:00  mjmaloney
*  Removed import statements for classes within ilex.util.
*
*  Revision 1.3  2004/08/30 14:50:26  mjmaloney
*  Javadocs
*
*  Revision 1.2  2003/07/30 20:30:23  mjmaloney
*  dev
*
*  Revision 1.1  2003/05/12 12:36:46  mjmaloney
*  Added FailureException & FatalException.
*  All three IlexException sub-classes modified to log messages.
*
*  Revision 1.1  2001/01/24 02:17:14  mike
*  Added ErrorException and WarningException for File parsers.
*
*/
package ilex.util;

/**
* FatalException is a non-recoverable application error. It most likely
* means that the application should terminate.
*/
public class FatalException extends IlexException
{
	/**
	* Constructor.
	* @param msg the message.
	*/
	public FatalException( String msg )
	{
		super("Fatal: " + msg);
		Logger.instance().log(Logger.E_FATAL, msg);
	}
}

