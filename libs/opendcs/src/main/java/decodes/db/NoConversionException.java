/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/26 13:29:25  mjmaloney
*  Added javadocs
*
*  Revision 1.1  2001/01/12 15:38:20  mike
*  dev
*
*/
package decodes.db;

/**
Thrown when an EU conversion could not be made
*/
public class NoConversionException extends decodes.util.DecodesException
{
	/** constructor. 
	  @param msg the message
	*/
	public NoConversionException(String msg)
	{
		super(msg);
	}
}

