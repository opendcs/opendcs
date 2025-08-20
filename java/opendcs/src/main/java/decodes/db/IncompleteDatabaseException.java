/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/26 13:29:23  mjmaloney
*  Added javadocs
*
*  Revision 1.2  2001/04/06 10:44:36  mike
*  dev
*
*  Revision 1.1  2001/01/20 02:54:00  mike
*  dev
*
*/
package decodes.db;

/**
Thrown when needed database records are missing.
*/
public class IncompleteDatabaseException extends DatabaseException
{
	/** 
	  constructor.
	  @param msg the message.
 	*/
	public IncompleteDatabaseException(String msg)
	{
		super(msg);
	}
}

