/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/11/20 18:49:25  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:46  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.11  2008/10/05 12:28:54  satin
*  Made the code "N" in the check operation case insensitive.
*
*  Revision 1.10  2008/03/25 18:39:52  satin
*  Corrected the statements that set the "case sensitive" flag.
*  (The comparison on the property variable should  have been a string
*  comparison. )
*
*  Revision 1.9  2008/02/11 14:33:26  mmaloney
*  Removed misleading comment.
*
*  Revision 1.8  2008/02/07 15:42:33  satin
*  Added code to make format labels case-sensitive if the DECODES
*  property "decodesFormatLabelMode" equals "case-sensitive".
*
*  Revision 1.7  2004/08/27 21:12:24  mjmaloney
*  javadocs
*
*  Revision 1.6  2004/02/05 22:03:34  mjmaloney
*  Remove extraneous debugs
*
*  Revision 1.5  2003/12/12 02:07:20  mjmaloney
*  EDL testing.
*
*  Revision 1.4  2002/11/24 20:07:24  mjmaloney
*  Fixed DR 86 whereby C('5', ...), the '5' was taken as repeat factor.
*
*  Revision 1.3  2001/08/19 19:33:21  mike
*  dev
*
*  Revision 1.2  2001/05/21 13:38:50  mike
*  dev
*
*  Revision 1.1  2001/05/06 22:53:03  mike
*  dev
*
*
*/
package decodes.decoder;

import ilex.util.ArgumentTokenizer;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.util.DecodesSettings;
import ilex.util.Logger;

/**
DecodesCheckCommand is a DecodesOperation that checks the
current locat
*/
class CheckOperation extends DecodesOperation
{
	private int numChars;
	private FormatStatement newFormat;
	private char checkFor;
	private String checkString;

	private static final char CHECK_FOR_SIGN = 's';
	private static final char CHECK_FOR_NUMBER = 'n';
	private static final char CHECK_FOR_STRING = '\'';
	private boolean labelIsCaseSensitive = false;

	public char getType() { return 'c'; }

	/**
	  Constructor.
	  @param args string inside the parens
	  @param ds the script.
	*/
	CheckOperation(String args, DecodesScript ds)
		throws ScriptFormatException
	{
		super(1);  // By definition, only 1 repetition.

		numChars = 1;
		checkString = null;
		
		DecodesSettings settings = DecodesSettings.instance();
		if ( settings.decodesFormatLabelMode.equals("case-sensitive" ) )
			labelIsCaseSensitive = true;
		
		ArgumentTokenizer tokenizer = new ArgumentTokenizer(args, !labelIsCaseSensitive);

		String what = tokenizer.getNextToken();
		if (what == null || what.length() == 0)
			throw new ScriptFormatException("No target in check operation");
		boolean quoted = tokenizer.wasQuoted();

		String label = tokenizer.getNextToken();
		if (label == null)
			throw new ScriptFormatException("No label in check operation");
		else {
			if ( !labelIsCaseSensitive )
				label = label.toLowerCase();
		}
		newFormat = ds.getFormatStatement(label);
		if (newFormat == null)
			throw new ScriptFormatException("No such format statement '"
				 + label + "'");

		if (quoted)
		{
			checkFor = CHECK_FOR_STRING;
			checkString = what;
		}
		else if (what.toLowerCase().charAt(0) == 's')
		{
			checkFor = CHECK_FOR_SIGN;
		}
		else if (what.toLowerCase().charAt(0) == 'n')
		{
			checkFor = CHECK_FOR_NUMBER;
			numChars = 1;
		}
		else if (Character.isDigit(what.charAt(0)))
		{
			int n = 1;
			while(n < what.length() && Character.isDigit(what.charAt(n)))
				n++;
			numChars = Integer.parseInt(what.substring(0, n));
			if (n >= what.length() || what.toLowerCase().charAt(n) != 'n')
				throw new ScriptFormatException(
					"Illegal target '" + what + "' in check operation");
			checkFor = CHECK_FOR_NUMBER;
		}
		else
			throw new ScriptFormatException(
				"Illegal target '" + what + "' in check operation");
	}


	/**
	  Executes the check.
	  @param dd DataOperations containing message context
	  @param msg DecodedMessage in which to store results
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		int pos = dd.getBytePos();
		boolean found = false;
		switch(checkFor) 
		{
		case CHECK_FOR_NUMBER:
			found = dd.checkNum(numChars);
			Logger.instance().debug3("check(number), pos=" + pos + ", result=" + found);
			break;
		case CHECK_FOR_SIGN:
			found = dd.checkSign();
			Logger.instance().debug3("check(sign), pos=" + pos + ", result=" + found);
			break;
		case CHECK_FOR_STRING:
			found = dd.checkString(checkString);
			Logger.instance().debug3("check('" + checkString + "'), pos=" + pos + ",result=" + found);
			break;
		}

		if ( !found ) 
			throw new SwitchFormatException(newFormat);
	}
}
