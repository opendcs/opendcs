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
*  Revision 1.1  2001/05/04 21:16:12  mike
*  dev
*
*/
package decodes.decoder;

/**
This exception indicates an error in a decoding script.
*/
public class ScriptException extends DecoderException
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
	public ScriptException(String msg)
	{
		super(msg);
	}
}

