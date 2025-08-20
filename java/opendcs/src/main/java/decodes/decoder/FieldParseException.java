/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 16:31:20  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/05/21 18:01:27  mike
*  dev
*
*/
package decodes.decoder;

/**
This exception indicates an run-time error in extracting a field from
DCP data.
*/
public class FieldParseException extends DecoderException
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
	public FieldParseException(String msg)
	{
		super(msg);
	}
}

