/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2003/08/06 23:29:24  mjmaloney
*  dev
*
*/
package lritdcs;

import ilex.util.FailureException;

public class StatusInvalidException extends FailureException
{
	public StatusInvalidException(String msg)
	{
		// Note FailureException ctor will issue log message.
		super(msg);
	}

}
