/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/30 14:50:36  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/11/16 02:36:24  mike
*  dev
*
*
*/
package ilex.var;

/**
Base class for all variable-related exceptions.
*/
public class VariableException extends Exception
{
	/**
	* Constructor
	* @param msg the message
	*/
	public VariableException( String msg )
	{
		super(msg);
	}
}

