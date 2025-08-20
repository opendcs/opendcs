/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2009/03/24 18:32:47  mjmaloney
*  wordChars should also include underscore.
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/12/12 18:50:42  mjmaloney
*  bug fix.
*
*  Revision 1.4  2004/08/30 14:50:24  mjmaloney
*  Javadocs
*
*  Revision 1.3  2003/12/15 15:21:14  mjmaloney
*  Improvements to support LRGS Config Editor & EDL files.
*
*  Revision 1.2  2001/05/30 23:52:50  mike
*  dev
*
*  Revision 1.1  2001/05/06 17:48:11  mike
*  Created ArgumentTokenizer
*
*
*/
package ilex.util;

import java.io.StreamTokenizer;
import java.io.StringReader;

/**
This class can tokenize a string containing a set of arguments that may
be quoted.
*/
public class ArgumentTokenizer extends StreamTokenizer
{
	/**
	* Constructor
	* @param args the string containing the arguments
	* @param lowerCaseMode turns on lower case mode
	* @see StreamTokenizer
	*/
	public ArgumentTokenizer( String args, boolean lowerCaseMode )
	{
		super(new StringReader(args));

		slashSlashComments(false);
		slashStarComments(false);
		eolIsSignificant(false);
		lowerCaseMode(lowerCaseMode);
		quoteChar('\'');
		quoteChar('"');
		whitespaceChars((int)',', (int)',');
		ordinaryChars((int)'0', (int)'9');
		ordinaryChar((int)'_');
		wordChars((int)'0', (int)'9');
		wordChars((int)'_', (int)'_');
	}

	/**
	* @return the next token or null if no more.
	*/
	public String getNextToken( )
	{
		int t;
		try { t = nextToken(); }
		catch(Exception e) { return null; }
		if (t == TT_EOF)
			return null;
		else if (t == TT_NUMBER)
			return "" + nval;
		else
			return sval;
	}

	/**
	* @return true if the last token returned was a quoted string.
	*/
	public boolean wasQuoted( )
	{
		return ttype == '\'' || ttype == '"';
	}

	/**
	* Test main.
	* @param args the first arg is parsed and each token printed.
	*/
	public static void main( String[] args )
	{
//		ArgumentTokenizer at = new ArgumentTokenizer(args[0], true);
//		ArgumentTokenizer at = new ArgumentTokenizer(
//			"'one,two,three', 2, 3, \"double quoted string\""
//			+",partially'quoted'", true);
/*
		ArgumentTokenizer at = new ArgumentTokenizer(
			"' (00045)',ERROR", true);
*/
		System.out.println("Tokenizing '" + args[0] + "'");
		ArgumentTokenizer at = new ArgumentTokenizer(args[0], true);
		String token;
		for(int i=0; (token = at.getNextToken()) != null; i++)
			System.out.println("" + i + ": '" + token + "' (quoted="
				+ at.wasQuoted() + ")");

	}
}


