/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:11  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2005/07/08 18:42:53  mjmaloney
*  Created.
*
*/
package lrgs.common;

/**
Thrown when configuration file cannot be successfully parsed.
*/
public class BadConfigException extends Exception
{
	/**
	  Construct the exception.
	  @param s the message
	*/
	public BadConfigException(String s)
	{
		super(s);
	}
}
