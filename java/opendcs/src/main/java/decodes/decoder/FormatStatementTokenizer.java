/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.decoder;

import decodes.db.DecodesScript;
import decodes.db.FormatStatement;

/**
FormatStatementTokenizer parses out DecodesOperations from a format statement.
*/
public class FormatStatementTokenizer
{
	private String fmtStatementText;
	private DecodesScript myScript;
	private int offset = 0;
	private int lastTokenStart = 0;
	private int wholeStatementOffset = 0;
	private FormatStatement formatStatement = null;

	/**
	  Constructor.
	  @param  fmtStatement the complete format statement.
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
			throw new ScriptFormatException("Sign with no repetitions.", this.fmtStatementText,
											"-", lastTokenStart);

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
			throw new ScriptFormatException("Repetition factor without operator",
											fmtStatementText,"" + repetitions,lastTokenStart);

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
				DecodesFunction func = FunctionList.lookup(op);
				if (func != null)
				{
					func.setRepetitions(repetitions);
					func.setArguments(args, myScript);
					ret = func;
				}
				else
					throw new ScriptFormatException("Function '" + op + "' unknown.",
												    fmtStatementText,op,lastTokenStart);
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
			// MJM 20141210 labels may contain a hyphen, e.g. 'shef-chek'.
			while(Character.isLetterOrDigit(c = nextChar()) || c == '_' || c == '-' ||
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
				+ "', position=" + lastTokenStart,
				fmtStatementText, op, lastTokenStart);
		}

		int tokenStart = wholeStatementOffset+lastTokenStart;
		int tokenEnd = wholeStatementOffset+offset;
		if (c == ',')
			tokenEnd--; // Don't include comma delimiter in token

		ret.setTokenPosition(new TokenPosition(tokenStart, tokenEnd));
		ret.setFormatStatement(formatStatement);
		
		return ret;
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
