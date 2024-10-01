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
*  Revision 1.2  2004/08/30 14:50:35  mjmaloney
*  Javadocs
*
*  Revision 1.1  2000/11/16 02:36:24  mike
*  dev
*
*
*/
package ilex.var;

import ilex.var.VariableException;

/**
* This exception is thrown when a method cannot succeed because
* a required conversion cannot be made.
* For example, calling getFloatValue() on a String Variable containing "ABC".
*/
public class NoConversionException extends VariableException
{
	/**
	* Constructor
	* @param msg the message.
	*/
	public NoConversionException( String msg )
	{
		super(msg);
	}
}

