/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/11 21:40:56  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:05  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:52  mjmaloney
*  Created.
*
*/
package decodes.comp;

import decodes.util.DecodesException;

/** Base class for exceptions thrown by computation code. */
public class ComputationException extends DecodesException
{
	/** 
	  Construct new object. 
	  @param msg the message
	*/
	public ComputationException( String msg )
	{
		super(msg);
	}
}
