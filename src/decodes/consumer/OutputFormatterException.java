/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/24 21:01:37  mjmaloney
*  added javadocs
*
*  Revision 1.1  2001/09/09 17:39:42  mike
*  Created.
*
*/
package decodes.consumer;

import decodes.util.DecodesException;

/**
Thrown by concrete output formatters.
*/
public class OutputFormatterException extends DecodesException
{
	/** 
	  Constructs new exception.
	  @param msg the explanation
	*/
	public OutputFormatterException(String msg)
	{
		super(msg);
	}
}

