/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/07/08 18:40:47  mjmaloney
*  Added AuthException -- do not use IllegalArgumentException.
*
*/
package ilex.util;

/**
* This exception is used for various types of authentication errors.
*/
public class AuthException extends Exception
{
	/**
	* Constructor
	* @param msg the message
	*/
	public AuthException( String msg )
	{
		super(msg);
	}

	/**
	 * Constructor with cause
	 * @param msg the message
	 * @param cause root cause
	 */
	public AuthException(String msg, Throwable cause)
	{
		super(msg,cause);
	}
}
