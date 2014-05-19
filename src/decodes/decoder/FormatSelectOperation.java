/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:01  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/12/11 01:05:17  mmaloney
*  javadoc cleanup
*
*  Revision 1.3  2004/10/21 19:15:09  mjmaloney
*  Throw ScriptException if attempting to switch to non-existant format statement.
*
*  Revision 1.2  2004/08/31 16:31:20  mjmaloney
*  javadoc
*
*  Revision 1.1  2001/05/21 13:38:50  mike
*  dev
*
*
*/
package decodes.decoder;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;

/**
FormatSelectOperation implements the n/ operator, which skips a specified
number of lines.
*/
public class FormatSelectOperation extends DecodesOperation 
{
	/** The FormatStatement to switch to */
	private FormatStatement newFormat;

	/** The label of the statement, saved in case it was null. */
	private String label;

	/**
	  Constructor.
	  @param  label the label of the new format statement.
	  @param  ds the DecodesScript that this operation belongs to
	*/
	public FormatSelectOperation(String label, DecodesScript ds)
		throws ScriptFormatException
	{
		super(1);  // By definition, only 1 repetition.
		this.label = label;
		newFormat = ds.getFormatStatement(label);
	}

	/** @return type code for this operation. */
	public char getType() { return '>'; }

	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		if (newFormat == null)
			throw new ScriptException(
				"Attempt to switch to invalid format label '" + label + "'");
		throw new SwitchFormatException(newFormat);
	}
}
