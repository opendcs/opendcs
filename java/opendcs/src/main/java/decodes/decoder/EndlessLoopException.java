/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2009/04/13 20:36:01  mjmaloney
*  dev
*
*  Revision 1.1  2009/03/12 15:42:33  ddschwit
*  41 transition
*
*  Revision 1.1  2009/03/10 16:43:01  satin
*  *** empty log message ***
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
public class EndlessLoopException extends DecoderException
{
	/**
	  Construct the exception.
	  @param msg the message
	*/
	public EndlessLoopException(String msg)
	{
		super(msg);
	}
}

