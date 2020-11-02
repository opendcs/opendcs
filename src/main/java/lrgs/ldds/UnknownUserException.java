/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2005/03/07 21:33:53  mjmaloney
*  dev
*
*  Revision 1.1  2005/01/05 19:21:09  mjmaloney
*  Bug fixes & updates.
*
*/
package lrgs.ldds;

import ilex.util.WarningException;
import lrgs.common.LrgsErrorCode;

/**
  Thrown when any DDS command is attempted and the user has not successfully
  authenticated with either a Hello or AuthHello message. Also thrown by
  the Hello commands if the user name is not recognized.
*/
public class UnknownUserException extends LddsRequestException
{
	/**
	  Constructor takes a String message.
	  @param msg the message
	*/
	public UnknownUserException(String msg, boolean hangup)
	{
		super(msg, LrgsErrorCode.DINVALIDUSER, hangup);
	}

	/**
	  Constructor takes a String message.
	  @param msg the message
	*/
	public UnknownUserException(String msg)
	{
		this(msg, false);
	}
}

