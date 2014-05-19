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
*  Revision 1.2  2004/08/30 14:50:33  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/11/16 21:45:21  mike
*  dev
*
*/
package ilex.var;

import ilex.var.VariableException;

/**
* This exception is thrown when an bad index is passed to an array
* operation. For example, a negative number or a number past the end
* of the array.
*/
public class BadIndexException extends VariableException
{
	/**
	* Constructor.
	* @param msg the message
	*/
	public BadIndexException( String msg )
	{
		super(msg);
	}
}

