/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:16  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2005/12/30 19:40:59  mmaloney
*  dev
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

import ilex.util.FatalException;
import ilex.util.Logger;

public class InitFailedException extends FatalException
{
	public InitFailedException(String msg)
	{
		super(msg);
		Logger.instance().fatal("LRIT:" + Constants.EVT_INIT_FAILED
			+ "- " + msg);
		// Note FatalException ctor will issue log message.
	}

}
