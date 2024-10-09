/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2005/01/03 18:51:33  mjmaloney
*  Added javadocs.
*
*  Revision 1.1  2004/04/29 01:10:20  mjmaloney
*  Created.
*
*/
package decodes.routmon;

/**
Thrown when a routing-spec status file could not be opened or parsed.
*/
public class BadStatusFile extends Exception
{
	/**
	 * Constructor.
	 * @param msg the error message, should contain file name and reason.
	 */
	public BadStatusFile(String msg)
	{
		super(msg);
	}
}
