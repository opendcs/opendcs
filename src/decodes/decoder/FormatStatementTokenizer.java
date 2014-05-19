/*
*	$Id$
*/
package decodes.decoder;

import ilex.util.Logger;
import decodes.db.DecodesScript;
import decodes.db.FormatStatement;

/**
FormatStatementTokenizer parses out DecodesOperations from a format statement.
*/
public class FormatStatementTokenizer
{
	private String fmtStatementText;
//	String origString;
	private DecodesScript myScript;
	private int offset = 0;
	private char quoteChar = (char)0;
	private boolean escaped = false;
	private int lastTokenStart = 0;
	private int wholeStatementOffset = 0;
	private FormatStatement formatStatement = null;

	/**
	  Constructor.
	  @param  s the complete format statement.
	  @param  ds the DecodesScript that this belongs to
	*/
	public FormatStatementTokenizer(String fmtStatement, DecodesScript ds,
		FormatStatement formatStatement)
	{
		this.fmtStatementText = fmtStatement;
		this.formatStatement = formatStatement;
		myScript = ds;
	}
	
	/**
	 * return new non-whitespace char in the string
	 * @return (char)0 when end of format statement is reached.
	 */
	private char nextChar()
	{
		if (offset < fmtStatementText.length())
			return fmtStatementText.charAt(offset++);
		return (char)0;
	}

	/**
	  Parses and returns the next decodes operation from the format statement.
	  @return DecodesOperation or null if end-of-statement is reached.
	  @throws ScriptFormatException if a syntax error is encountered.
	*/
	public DecodesOperation getOperation()
		throws ScriptFormatException
	{
		char c;
		String args = null;
		
		// Skip over whitespace or delimiters (',' or '\n')
		while((c = nextChar()) != (char)0 
		   && (c == ',' || Character.isWhitespace(c)));
		if (c == (char)0)
			return null;

		lastTokenStart = offset-1;

		// Operations that position the curser 
		int sign = 1;
		if (c == '-')
		{
			sign = -1;
			c = nextChar();
		}
		if (c == (char)0)
			throw new ScriptFormatException("Sign with no repetitions.");

		// Optional number of repetitions preceding the op code
		int repetitions = 0;
		boolean repsProvided = false;
		while (Character.isDigit(c))
		{
			repetitions = (repetitions * 10) + ((int)c - (int)'0');
			c = nextChar();
			repsProvided = true;
		}
		if (!repsProvided)
			repetitions = 1;
		repetitions *= sign;
		
		// Optional whitespace after repetitions.
		while(Character.isWhitespace(c))
			c = nextChar();
		if (c == (char)0)
			throw new ScriptFormatException("Repetition factor without operator");

		// Current char is now the beginning of the operator
		StringBuilder opName = new StringBuilder();
		opName.append(c);
		if (Character.isLetter(c))
			while(Character.isLetterOrDigit(c = nextChar()) || c == '_')
				opName.append(Character.toLowerCase(c));
		String op = opName.toString().toLowerCase();

		// Optional whitespace after operator string.
		// Note don't include \n which may be inserted when concatenating adjacent
		// lines with same label. We want to treat \n as a delimiter.
		while(c == ' ')
			c = nextChar();
		
		// Character op code or function name followed by open paren
		DecodesOperation ret = null;
		if (Character.isLetter(op.charAt(0)) && c == '(')
		{
			args = findArgToRightParen(op);
			c = ')';
			if (op.equals("c"))
				ret = new CheckOperation(args, myScript);
			else if (op.equals("s"))
				ret = new ScanOperation(args, myScript);
			else if (op.equals("f"))
				ret = new FieldOperation(repetitions, args, myScript);
			else if (op.equals("t"))
				ret = new TimeTruncateOperation(args);
			else if (op.equals("w"))
				ret = new WhiteSpaceSkipOperation();
			else
			{
				DecodesFunctionOperation func = FunctionList.lookup(op);
				if (func != null)
				{
					func.setRepetitions(repetitions);
					func.setArguments(args);
					ret = func;
				}
				else
					throw new ScriptFormatException("Function '" + op + "' unknown.");
			}
		}
		else if (op.equals("x"))
			ret = new SkipCharactersOperation(repetitions);
		else if (op.equals("p"))
			ret = new PositionOperation(repetitions);
		else if (op.equals(">"))
		{
			// Goto statement: >label
			StringBuilder argsb = new StringBuilder();
			boolean leading = true;
			while(Character.isLetterOrDigit(c = nextChar()) || c == '_' ||
				Character.isWhitespace(c))
			{
				if (Character.isWhitespace(c))
				{
					if (leading)
						continue; // ignore spaces between > and label
					else break;
				}
				else leading = false;
				argsb.append(c);
			}
			return new FormatSelectOperation(argsb.toString(), myScript);
		}
		else if (op.equals("#")) // Rest of this statement is a comment.
			return null;
		else if (op.equals("/"))
			ret = new SkipLinesOperation(repetitions);
		else if (op.equals("\\"))
			ret = new SkipLinesOperation(-repetitions);
		else if (op.equals("w"))
			ret = new WhiteSpaceSkipOperation();
		// Decodes operation groups
		else if (op.equals("("))
		{
			int argsStart = offset; // This is now the local offset to start of args
			args = findArgToRightParen(op);
			c = ')';
			ret = new DecodesOperationGroup(repetitions, args, myScript, 
				wholeStatementOffset + argsStart, formatStatement);
		}
		else
		{
			throw new ScriptFormatException(
				"Unknown operator '" + op + "', in '" + fmtStatementText 
				+ "', position=" + lastTokenStart);
		}

		// After the above, the last char processed is one of:
		// - delimiter between this op & the next
		// - right paren after op or function argument
		if (c != ',' && c != (char)0) // not a delimiter or end of line
		{
//			if ((c = nextChar()) != ',' && c != (char)0)
//				pushback();
		}
		
		int tokenStart = wholeStatementOffset+lastTokenStart;
		int tokenEnd = wholeStatementOffset+offset;
		if (c == ',')
			tokenEnd--; // Don't include comma delimiter in token

//		Logger.instance().debug3("FST returning op "
//			+ ret.getClass().getName() + " repetitions=" + ret.repetitions
//			+ " args='" + args + "' start=" + tokenStart
//			+ ", end=" + tokenEnd);
		ret.setTokenPosition(new TokenPosition(tokenStart, tokenEnd));
		ret.setFormatStatement(formatStatement);
		
		return ret;
	}

	/**
	  Removes all spaces that are not inside a quoted string. This saves
	  the tokenizer of checking for them.
	  @param s the input string
	  @return the collapsed string
	*/
	private String removeSpaces(String s)
	{
		StringBuffer sb = new StringBuffer();
		boolean instring = false;

		for ( int i = 0; i < s.length(); i++ ) 
		{
			if ( s.charAt(i) == '\'') 
				instring = !instring;
			if (instring || !Character.isWhitespace(s.charAt(i)))
				sb.append(s.charAt(i));
		}
		return(sb.toString());
	}


	/**
	 * Gets the arguments within parens after an opcode or function name.
	 * Handled quoted parens and nested parens.
	 * @return arguments string up to first non quoted right paren
	 */
	private String findArgToRightParen(String op)
		throws ScriptFormatException
	{
		int parens = 1; // assume we already have 1 left paren
		char quoted = (char)0;
		StringBuilder argsb = new StringBuilder();
		
		char c = (char)0;
		while((c = nextChar()) != (char)0)
		{
			if (c == ')' && quoted == (char)0)
			{
				if (--parens == 0)
					break;
			}
			
			argsb.append(c);
		
			if (quoted == (char)0) // not inside quotation
			{
				if (c == '\'' || c == '"')
					quoted = c;
				else if (c == '(')
					++parens;
			}
			else // we are inside quotations
			{
				if (c == quoted)
					quoted = (char)0;
			}
		}
		if (parens == 0)
			return argsb.toString();
		else
			throw new ScriptFormatException("Operator '" + op 
				+ "' mismatched parens in arg'" + argsb.toString() + "'");
	}

	public void setWholeStatementOffset(int wholeStatementOffset)
	{
		this.wholeStatementOffset = wholeStatementOffset;
	}
	
	
}
