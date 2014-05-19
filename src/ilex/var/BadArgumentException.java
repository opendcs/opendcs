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
*  Revision 1.2  2004/08/30 14:50:32  mjmaloney
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
* This exception is thrown when a method cannot be performed because
* the passed argument is not of the correct type and cannot be converted.
* For example, calling setValue("abc") on a numeric Variable.
*/
public class BadArgumentException extends VariableException
{
	/**
	* Constructor.
	* @param msg the message
	*/
	public BadArgumentException( String msg )
	{
		super(msg);
	}
}

