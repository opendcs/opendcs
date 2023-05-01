/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:02  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 16:31:22  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/05/05 23:53:51  mike
*  dev
*
*/
package decodes.decoder;

/**
This exception indicates a syntax error in a format statement.
*/
public class ScriptFormatException extends DecoderException
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
	public ScriptFormatException(String msg)
	{
		super(msg);
	}
}
