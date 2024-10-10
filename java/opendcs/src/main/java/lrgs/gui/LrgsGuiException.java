/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:13  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 21:08:39  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/02/17 18:41:56  mike
*  Modified to use Directory rather than Naming Service.
*
*/
package lrgs.gui;

/**
Exceptions thrown from LRGS GUI components.
*/
public class LrgsGuiException extends Exception
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
    public LrgsGuiException(String msg)
	{
		super(msg);
    }
}
