/*
*	$Id$
*
*	$State$
*
*	$Log$
*	Revision 1.2  2010/09/13 19:30:36  mmaloney
*	Scan should always jump to the specified label if the scan fails, even if the scan ran out of data.
*	
*	Revision 1.1  2008/04/04 18:21:01  cvs
*	Added legacy code to repository
*	
*	Revision 1.4  2007/12/11 01:05:17  mmaloney
*	javadoc cleanup
*	
*	Revision 1.3  2004/08/31 16:31:21  mjmaloney
*	javadoc
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

import ilex.util.Logger;

import java.io.*;
import java.util.Vector;

/**
PositionOperation implements the nP operator, which moves the character to
the nth character on the current line.
*/
public class PositionOperation extends DecodesOperation 
{
	/**
	  Constructor.
	  @param  position the desired position on the line.
	*/
	public PositionOperation(int position)
	{
		// Note 'position' stored as 'repetitions' in super class.
		super(position);
	}

	/** @return type code for this operation. */
	public char getType() { return 'P'; }

	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		Logger.instance().log(Logger.E_DEBUG3, "Positioning to " + repetitions);
		dd.position(repetitions);
	}
}

