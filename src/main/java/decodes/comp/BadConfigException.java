/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2004/08/11 21:40:56  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:04  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:51  mjmaloney
*  Created.
*
*/
/**
* @(#) BadConfigException.java
*/
package decodes.comp;

import decodes.comp.ComputationException;

/**
* Thrown when ComputationProcessor could not initialize because of
* a bad or missing configuration file.
*/
public class BadConfigException extends ComputationException
{
	/**
	  Construct new BadConfigException.
	  @param msg the message
	*/
	public BadConfigException(String msg)
	{
		super(msg);
	}
}
