/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2003/07/28 19:18:24  mjmaloney
*  dev
*
*  Revision 1.1  2003/07/28 18:16:36  mjmaloney
*  Initial version.
*
*  Revision 1.1.1.1  2003/07/28 18:13:20  mjmaloney
*  LRIT-DCS Project Files
*
*/
package lritdcs;

import ilex.util.FailureException;

public class BadMessageException extends FailureException
{
	public BadMessageException(String msg)
	{
		// Note FailureException ctor will issue log message.
		super(msg);
	}

}
