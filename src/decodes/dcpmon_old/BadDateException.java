/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2014/06/02 14:28:48  mmaloney
*  rc5 includes initial refactory for dcpmon
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2007/12/04 14:28:33  mmaloney
*  added code to download channels from url
*
*  Revision 1.2  2004/09/23 13:41:53  mjmaloney
*  javadoc clean-up
*
*  Revision 1.1  2004/02/20 16:12:09  mjmaloney
*  Implementation of DCP Monitor Server
*
*/
package decodes.dcpmon_old;

import ilex.util.WarningException;

/**
Thrown when date is unparsable. 
*/
public class BadDateException extends WarningException
{
	/**
	  Constructor.
	  @param msg the message.
	*/
	public BadDateException(String msg)
	{
		super(msg);
	}
}
