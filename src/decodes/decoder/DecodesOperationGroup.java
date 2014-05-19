/*
*	$Id$
*
*	$State$
*
*	$Log$
*	Revision 1.1  2008/04/04 18:21:01  cvs
*	Added legacy code to repository
*	
*	Revision 1.3  2004/08/31 16:31:19  mjmaloney
*	javadoc
*	
*	Revision 1.2  2001/08/13 01:30:00  mike
*	dev
*	
*	Revision 1.1  2001/05/06 22:53:03  mike
*	dev
*	
*
*/
package decodes.decoder;

import java.util.ArrayList;
import java.util.Vector;
import java.util.Enumeration;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;

/**
DecodesOperationGroup is a DecodesOperation that consists of
executing a list of DecodesOperations.
Each script will have a top-dog that is a DecodesOperationGroup.
*/
public class DecodesOperationGroup extends DecodesOperation
{
	private ArrayList<DecodesOperation> ops;

	public char getType() { return 'g'; }

	/**
	  Constructor.
	  @param repetitions number of times to execute this group
	  @param args string parsed to create operations under this group.
	  @param script the DecodesScript that this DOG belongs to
	*/
	public DecodesOperationGroup(int repetitions, String args, DecodesScript script,
		int wholeStatementOffset, FormatStatement formatStatement)
		throws ScriptFormatException
	{
		super(repetitions);

		ops = new ArrayList<DecodesOperation>();

		// 'args' is a string that must be parsed to create the operations
		// under this group.
		FormatStatementTokenizer tokenizer = new FormatStatementTokenizer(
			args, script, formatStatement);
		tokenizer.setWholeStatementOffset(wholeStatementOffset);
		DecodesOperation dop;
		while((dop = tokenizer.getOperation()) != null)
			ops.add(dop);
	}

	/**
	  Executes the dog.
	  @param dd the DataOperations providing the raw message context
	  @param msg the DecodedMessage into which decoded values are placed
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		for(int i = 0; i < repetitions; i++)
		{
			for(DecodesOperation dop : ops)
				dop.execute(dd, msg);
		}
	}
}
