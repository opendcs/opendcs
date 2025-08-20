/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/11 21:40:59  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:07  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:57  mjmaloney
*  Created.
*
*/
package decodes.comp;

import decodes.comp.ComputationException;

/**
Thrown when table bounds exceeded and can't interpolate.
*/
public class TableBoundsException extends ComputationException
{
	/**
	  Constructs new exception.
	* @param msg the message
	*/
	public TableBoundsException( String msg )
	{
		super(msg);
	}
}
