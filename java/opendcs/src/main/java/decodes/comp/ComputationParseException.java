/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/11 21:40:57  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:05  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:53  mjmaloney
*  Created.
*
*/
package decodes.comp;

import decodes.comp.ComputationException;

/**
 * Thrown when parsing could not succeed for data necessary to
 * execute a computation. E.g., a bad RDB Rating File.
 */
public class ComputationParseException extends ComputationException
{
	/** Construct new object. */
	public ComputationParseException( String msg )
	{
		super(msg);
	}
}
