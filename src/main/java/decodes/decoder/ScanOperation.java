/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.6  2010/09/13 19:30:36  mmaloney
*  Scan should always jump to the specified label if the scan fails, even if the scan ran out of data.
*
*  Revision 1.5  2009/09/30 13:52:15  shweta
*  *** empty log message ***
*
*  Revision 1.4  2009/09/29 21:16:11  shweta
*  MOdified code  to scan pseudo binary characters.
*
*  Revision 1.3  2009/03/24 18:38:40  mjmaloney
*  Fix debug message
*
*  Revision 1.2  2008/11/20 18:49:25  mjmaloney
*  merge from usgs mods
*
*  Revision 1.1  2008/11/15 01:03:50  mmaloney
*  Moved from separate trees to common parent
*
*  Revision 1.11  2008/08/21 18:38:34  sedreyer
*  In displays, replaced references to the absolute byte position within a file
*  to a line number and character offset from the beginning of line.
*
*  Revision 1.10  2008/03/25 18:39:53  satin
*  Corrected the statements that set the "case sensitive" flag.
*  (The comparison on the property variable should  have been a string
*  comparison. )
*
*  Revision 1.9  2008/02/11 14:33:27  mmaloney
*  Removed misleading comment.
*
*  Revision 1.8  2008/02/07 15:42:41  satin
*  Added code to make format labels case-sensitive if the DECODES
*  property "decodesFormatLabelMode" equals "case-sensitive".
*
*  Revision 1.7  2007/12/11 01:05:18  mmaloney
*  javadoc cleanup
*
*  Revision 1.6  2004/08/31 16:31:22  mjmaloney
*  javadoc
*
*  Revision 1.5  2004/04/08 19:50:09  satin
*  Added code to generate an EndOfData exception when a skipline
*  operation goes past the end of data.
*
*  Revision 1.4  2003/02/01 19:05:18  mjmaloney
*  Improved debug messages on field ops.
*
*  Revision 1.3  2002/09/30 18:54:34  mjmaloney
*  SQL dev.
*
*  Revision 1.2  2001/05/21 13:38:50  mike
*  dev
*
*  Revision 1.1  2001/05/06 23:04:02  mike
*  dev
*
*
*/
package decodes.decoder;

import ilex.util.ArgumentTokenizer;
import ilex.util.Logger;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;
import decodes.util.DecodesSettings;

/**
DecodesScanCommand is a DecodesOperation that scans data for a pattern.
*/
public class ScanOperation extends DecodesOperation 
{
	private int numChars;
	private FormatStatement newFormat;
	private char scanFor;
	private String scanString;
	private char scanChar;

	private static final char SCAN_FOR_SIGN = 's';
	private static final char SCAN_FOR_NUMBER = 'n';
	private static final char SCAN_FOR_STRING = '\'';
	private static final char SCAN_FOR_CHAR = 'c';
	private static final char SCAN_FOR_LETTER = 'a';
	private static final char SCAN_FOR_PSEUDOBINAY = 'p';
	// Format Label mode
	private boolean labelIsCaseSensitive = false;

	public char getType() { return 's'; }

	/**
	  Constructor.
	  @param  args complete string inside the parens
	  @param  ds the DecodesScript that this operation belongs to
	  @throws ScriptFormatException if syntax error detected in arguments
	*/
	public ScanOperation(String args, DecodesScript ds)
		throws ScriptFormatException
	{
		super(1);  // By definition, only 1 repetition.

		DecodesSettings settings = DecodesSettings.instance();
		if ( settings.decodesFormatLabelMode.equals("case-sensitive" ) )
			labelIsCaseSensitive = true;	
		numChars = 1;
		scanString = null;
		scanChar = (char)0;

		ArgumentTokenizer tokenizer = new ArgumentTokenizer(args, !labelIsCaseSensitive);

		String nstr = tokenizer.getNextToken();
		if (nstr == null)
			throw new ScriptFormatException("Scan operation with no 'n'");
		try { numChars = Integer.parseInt(nstr); }
		catch(NumberFormatException e)
		{
			throw new ScriptFormatException(
				"Exception integer number-of-chars in scan operation.");
		}

		String what = tokenizer.getNextToken();
		if (what == null)
			throw new ScriptFormatException("No target in scan operation");
		boolean quoted = tokenizer.wasQuoted();

		String label = tokenizer.getNextToken();
		if (label == null)
			throw new ScriptFormatException("No label in check operation");
		newFormat = ds.getFormatStatement(label);
		if (newFormat == null)
			throw new ScriptFormatException("No such format statement '"
				 + label + "'");

		if (quoted)
		{
			if (what.length() == 1)
			{
				scanFor = SCAN_FOR_CHAR;
				scanChar = what.charAt(0);
			}
			else
			{
				scanFor = SCAN_FOR_STRING;
				scanString = what;
			}
		}
		else if (what.toLowerCase().charAt(0) == 's')
		{
			scanFor = SCAN_FOR_SIGN;
		}
		else if (what.toLowerCase().charAt(0) == 'n')
		{
			scanFor = SCAN_FOR_NUMBER;
		}
		else if (what.toLowerCase().charAt(0) == 'a')
		{
			scanFor = SCAN_FOR_LETTER;
		}
		else if (what.toLowerCase().charAt(0) == 'p')
		{
			scanFor = SCAN_FOR_PSEUDOBINAY;
		}
		else
			throw new ScriptFormatException("Illegal target in scan operation");
	}


	/**
	  Executes this operation using the context provided.
	  @param dd holds the raw data and context.
	  @param msg store decoded values here.
	  @throws DecoderException or subclass if error detected.
	*/
	public void execute(DataOperations dd, DecodedMessage msg) 
		throws DecoderException
	{
		boolean found = false;
		int n;

		for(n=0; !found  && n < repetitions; n++ ) 
	    	switch(scanFor) 
			{
			case SCAN_FOR_NUMBER:
				found = dd.scanNum(numChars);
				break;

			case SCAN_FOR_STRING:
				Logger.instance().log(Logger.E_DEBUG3, "Scanning "
					+ numChars + " bytes for '" + scanString + "' on line " 
					+ dd.getCurrentLine() + " at position "
					+ dd.getRelBytePos());
				found = dd.scanString(numChars, scanString);
				break;

			case SCAN_FOR_CHAR:
				Logger.instance().log(Logger.E_DEBUG3, "Scanning "
					+ numChars + " bytes for '" + scanChar + "' on line " 
					+ dd.getCurrentLine() + " at position "
					+ dd.getRelBytePos());
				found=dd.scanChar(numChars, (byte)scanChar);
				break;

			case SCAN_FOR_SIGN:
				found=dd.scanSign(numChars);
				break;

			case SCAN_FOR_LETTER:
				found=dd.scanAlpha(numChars);
				break;
				
			case SCAN_FOR_PSEUDOBINAY:				
				Logger.instance().log(Logger.E_DEBUG3, "Scanning data for pseudobinary characters");						
				found=dd.scanPseudoBinary(numChars);				
				break;
	    	}
		if (!found)
		{
// MJM - We don't want to do the following test. I might put:
// try_limits    s(100,'LIMITS=',try_data)
// try_data      0p, .... do something else.
// meaning that I want to scan entire message for the string 'LIMITS='.
// If it fails, I want to go to try_data which calls 0p to re-process the msg in
// a different way.
//			if (!dd.moreChars())
//				throw new EndOfDataException(
//					"End of data during scan operation");
	        throw new SwitchFormatException(newFormat);
		}
	}
}
