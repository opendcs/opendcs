/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2004/08/31 16:31:19  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/05/04 21:16:12  mike
*  dev
*
*/
package decodes.decoder;

/**
This is thrown when the decoder reaches the end of the raw data.
*/
public class EndOfDataException extends DecoderException
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
	public EndOfDataException(String msg)
	{
		super(msg);
	}
}

