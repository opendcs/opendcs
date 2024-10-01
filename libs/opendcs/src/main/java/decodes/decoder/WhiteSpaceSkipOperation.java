/*
*	$Id$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:02  cvs
*	Added legacy code to repository
*	
*	Revision 1.3  2007/12/11 01:05:18  mmaloney
*	javadoc cleanup
*	
*	Revision 1.2  2004/08/31 16:31:24  mjmaloney
*	javadoc
*	
*	Revision 1.1  2004/01/05 01:49:02  mjmaloney
*	Debug.
*	
*/
package decodes.decoder;

/**
WhiteSpaceSkipOperation skips any amount of white space.
*/
public class WhiteSpaceSkipOperation extends DecodesOperation 
{
	/**
	  Constructor.
	*/
	public WhiteSpaceSkipOperation()
	{
		super(1);
	}

	/** @return type code for this operation. */
	public char getType() { return 'W'; }

	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		dd.skipWhiteSpace();
	}
}

