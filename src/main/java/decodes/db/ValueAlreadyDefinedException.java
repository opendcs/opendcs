/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/27 12:23:13  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2001/04/06 10:44:36  mike
*  dev
*
*  Revision 1.1  2000/12/21 14:31:27  mike
*  Created.
*
*
*/
package decodes.db;

/**
Exception thrown on uniqueness violation.
*/
public class ValueAlreadyDefinedException extends decodes.util.DecodesException
{
	/**
	  Construct with message
	  @param msg the message
	*/
	public ValueAlreadyDefinedException(String msg)
	{
		super(msg);
	}
}

