/*
*  $Id$
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
*  Revision 1.1  2003/05/12 12:36:46  mjmaloney
*  Added FailureException & FatalException.
*  All three IlexException sub-classes modified to log messages.
*
*/
package ilex.util;

/**
* FailureException indicates a failed application-level operation.
* Throwing will cause a E_FAILURE log event.
*/
public class FailureException extends IlexException
{
	/**
	* Constructor.
	* @param msg the message.
	*/
	public FailureException( String msg )
	{
		super("Failure: " + msg);
		Logger.instance().log(Logger.E_FAILURE, msg);
	}
}

