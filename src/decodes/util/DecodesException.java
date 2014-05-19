/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:07  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/27 20:50:29  mjmaloney
*  javadocs
*
*  Revision 1.3  2001/01/03 02:54:55  mike
*  dev
*
*  Revision 1.2  2001/01/02 14:54:41  mike
*  No longer extends IlexException
*
*  Revision 1.1  2000/12/21 14:31:28  mike
*  Created.
*
*
*/
package decodes.util;

/**
Base class for all exceptions thrown by DECODES
*/
public class DecodesException extends Exception
{
	/** 
	  Constructor.
	  @param msg the explanation
	*/
	public DecodesException(String msg)
	{
		super(msg);
	}
}


