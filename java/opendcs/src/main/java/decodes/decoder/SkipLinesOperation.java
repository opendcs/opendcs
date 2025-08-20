/*
*	$Id$
*
*	$State$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:02  cvs
*	Added legacy code to repository
*	
*	Revision 1.5  2007/12/11 01:05:18  mmaloney
*	javadoc cleanup
*	
*	Revision 1.4  2004/08/31 16:31:23  mjmaloney
*	javadoc
*	
*	Revision 1.3  2004/04/08 19:50:09  satin
*	Added code to generate an EndOfData exception when a skipline
*	operation goes past the end of data.
*	
*	Revision 1.2  2001/05/21 13:38:50  mike
*	dev
*	
*	Revision 1.1  2001/05/06 22:53:18  mike
*	Added
*	
*
*/
package decodes.decoder;

/**
SkipLinesOperation implements the n/ operator, which skips a specified
number of lines.
*/
public class SkipLinesOperation extends DecodesOperation 
{
	/**
	  Constructor.
	  @param  repetitions number of times to repeat this operation
	*/
	public SkipLinesOperation(int repetitions)
	{
		super(repetitions);
	}

	/** @return type code for this operation. */
	public char getType() { return '/'; }

	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		dd.skipLines(repetitions);
                if (!dd.moreChars()) {
                	throw new EndOfDataException(
                         "End of data during skip lines operation");
		}
	}
}

