/*
*  $Id$
*/
package decodes.decoder;

import java.util.Vector;
import java.util.ArrayList;
import ilex.util.ArrayUtil;
import ilex.util.Logger;
import ilex.util.TextUtil;
import decodes.datasource.RawMessage;
import decodes.util.DecodesSettings;
import decodes.db.FormatStatement;


/**
DataOperations maintains the context as the decoder steps through the
message. As well as holding the position within the RawMessage, it provides
primitive operations for checking, scanning, parsing, etc., which are used
by the script operations.
*/
public class DataOperations 
{
	private RawMessage rawMsg;
	private byte[] data;      // buffer contained within rawMsg
	private int bytePos;      // byte position within data.
	private int curLine; // Line number for debug messages
	private int startLineNum; // Beinning line number of data
	private int relBytePos; // byte position within line
	private Vector<Integer> begLineBytePos; // byte position at beginning of line
	private static final byte NL = (byte)'\n';
	private static final byte CR = (byte)'\n';
	private DecodesSettings settings = null;
	public byte[] getDataBuffer() { return data; }
	
	// This data structure is used to detect endless loops.
	class FormatPosition 
	{
		FormatStatement fs;
		int pos;
		FormatPosition(FormatStatement fs, int pos)
		{
			this.fs = fs;
			this.pos = pos;
		}
	};
	private ArrayList<FormatPosition> formatPositions = 
		new ArrayList<FormatPosition>();

	/**
	  Constructor.
	  @param rawMsg contains the data to initialize the decoding context.
	*/
	public DataOperations(RawMessage rawMsg)
	{
		this.rawMsg = rawMsg;
		this.data = rawMsg.getMessageData();
		bytePos = 0;
		startLineNum = rawMsg.getStartLineNum();
		curLine = startLineNum;
		begLineBytePos = new Vector<Integer>();
		begLineBytePos.add(new Integer(0));
		settings = DecodesSettings.instance();
if (data.length >=2)
Logger.instance().debug1("DataOperations instantiated with length=" + data.length
+ ", last 2 chars = '" + (char)data[data.length-2] + "' '" + (char)data[data.length-1]
+ "'");
	}

	/** @return the RawMessage reference. */
	public RawMessage getRawMessage()
	{
		return rawMsg;
	}

	/**
	  Skip (move) cursor n characters. N may be positive or negative.

	  @param n number of characters
	  @throws EndOfDataException if this would place cursor past end of data.
	  @throws ScriptException if this place curser bevfore start of data.
	*/
	void skipCharacters(int n)
		throws EndOfDataException, ScriptException
	{
		boolean forward;

		if ( n >= 0 )
			forward = true;
		else 
		{
			forward = false;
			n *= -1;
		}

		while ( n-- > 0 ) 
		{
			if ( forward )
				forwardspace();
			else 
				backspace();
		}
	}


	/**
	  Positions the cursor to n'th character on the current line.
	  If there are fewer than N characters on this line, the cursor
	  will be placed at the end of the line.

	  @param n number of characters
	  @throws EndOfDataException if this would place cursor past end of data.
	  @throws ScriptException if this place curser bevfore start of data.
	*/
	public void position(int n)
		throws EndOfDataException, ScriptException
	{
		// if cursor at EOL, back it up one.
		if (curByte() == NL)
			backspace();

		// find the start of this line.
		byte c = (byte)' ';
		while (bytePos > 0 && ( c = curByte())	!= NL)
			backspace();
		if ( c == NL)
			forwardspace();

		while (--n > 0 && (c = curByte()) != NL )
			forwardspace();
	}


	/**
	  Skips 'n' lines of data. 'n' may be positive or negative.

	  @param n number of lines 
	  @throws EndOfDataException if this would place cursor past end of data.
	  @throws ScriptException if this place curser bevfore start of data.
	*/
	void skipLines(int n)
		throws EndOfDataException, ScriptException
	{
		byte c = (byte)' ';

		boolean forward;
		if ( n >= 0 )
			forward = true;
		else {
			forward = false;
			n *= -1;
		}

		if ( forward )
		{
			while ( moreChars() && n-- > 0 )
			{
				try
				{
					// goto first char on the next line.
					while (moreChars() && (c = curByte()) != NL) 
						forwardspace();
					if (moreChars())
						forwardspace();
				}
				catch(EndOfDataException e) {} // just let moreChars check fail.
			}
		}
		else // backward
		{
			while ( bytePos > 0 && n-- > 0 )
			{
				// goto first char on previous line.
				while (bytePos > 0 && (c = curByte()) != NL) 
					backspace();
				backspace();
				while (bytePos > 0 && (c = curByte()) != NL)
					backspace();
				if ( c == NL)
					forwardspace();
			}
		}
	}

	/**
	  Returns true if the current character equals the passed character.
	  @param c the character to check for.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean checkChar(byte c)
		throws EndOfDataException
	{
		return ( c == curByte() ); 
	}

	/**
	  @return true if the current character is a + or - sign.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean checkSign()
		throws EndOfDataException
	{
		return ( curByte() == '+' || curByte() == '-' ); 
	}	

	/**
	  Returns true if string matches the current data.
	  Leaves position within data unchanged.
	  @param s the string to check for
	  @return true if match
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean checkString(String s)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		savePosition();

		byte[] bytes = s.getBytes();
		int n = bytes.length;

		int i=0;
		try
		{
			for(i=0; i<n && bytes[i] == curByte(); i++)
				forwardspace();
		}
		catch(EndOfDataException e)
		{
			i = 0;  // force false return
		}
			
		restorePosition();
		return i == n;  // successfully fell through loop without mismatch?
	}


	/**
	  Returns true if the next 'n' characters contain digits.
	  Leaves position within data unchanged.
	  @param n number of characters
	  @return true if digit found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean checkNum(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		savePosition();

		int i;
		for(i=0; i<n && moreChars(); i++)
		{
			try
			{
				char c = (char)curByte();
				if ( !( Character.isDigit(c) || c == '.'
				        || c == '+' || c == '-') )
					break;
				forwardspace(); 
			}
			catch(EndOfDataException e) { break; }
		}

		restorePosition();
		return i == n;
	}

	/**
	  Returns true if the next 'n' characters contain pseudo binary values.
	  Leaves position within data unchanged.
	  @param n number of characters
	  @return true if pseudo binary value found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean checkPseudoBinary(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		savePosition();

		int i;
		for(i=0; i<n && moreChars(); i++)
		{
			try
			{
				int intPseudo = (int)curByte();						
				if(!(intPseudo==47) & !(intPseudo >=63) &(intPseudo <=127))				
					break;
				forwardspace(); 
			}
			catch(EndOfDataException e) { break; }
		}

		restorePosition();		
		return i == n;
	}
	
	/**
	  Returns true if the passed character 'c' is present in the next n 
	  bytes of data.
	  Leaves the cursor where the scan operation completed or failed.
	  @param n number of characters
	  @param c the byte to check for.
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanChar(int n, byte c) 
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
			return checkChar(c);

		while ( moreChars() && n-- > 0 )
		{
			if (!settings.scanPastEOL && data[bytePos] == NL)
				break; 
			try
			{
				if (c == curByte())
					return true;
				else
					forwardspace();
			}
			catch(EndOfDataException e) { return false; }
		}
		return false;
	}

	/**
	  Scans 'n' characters for a digit or sign-character, returns true if found,
	  false otherwise.
	  The cursor where the sign-or-digit was found or at the end of the 
	  n-character scan if not.
	  @param n number of characters
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanNum(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
			return checkNum(1) || checkSign();

		while( moreChars() && n-- > 0)
		{
			if (!settings.scanPastEOL && data[bytePos] == NL )
				break;
			if (checkNum(1) || checkSign())
				return true;
			else
			{
				try { forwardspace(); }
				catch(EndOfDataException e) { return false; }
			}
		}
		return false;
	}

	/**
	  Scans 'n' characters for a + or - sign-character, returns true if found,
	  false otherwise.
	  The cursor is left where the sign was found or at the end of the 
	  n-character scan if not.
	  @param n number of characters
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanSign(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
			return checkSign();

		while( moreChars() && n-- > 0)
		{
			if (!settings.scanPastEOL && data[bytePos] == NL )
				break;
			if (checkSign())
				return true;
			else
			{
				try { forwardspace(); }
				catch(EndOfDataException e) { return false; }
			}
		}
		return false;
	}

	/**
	  Scans 'n' characters for an alphabetic character.
	  @param n number of characters
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanAlpha(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
		{
			return Character.isLetter((char)curByte());
		}

		while( moreChars() && n-- > 0)
		{
			if ( !settings.scanPastEOL && data[bytePos] == NL )
				break;
			try
			{
				if (Character.isLetter((char)curByte()))
					return true;
				else
					forwardspace();
			}
			catch(EndOfDataException e) { return false; }
		}

		return false;
	}

	
	/**
	  Scans 'n' characters for an pseudo binary character.
	  @param n number of characters
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanPseudoBinary(int n)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
			return checkPseudoBinary(1);

		while( moreChars() && n-- > 0)
		{
			if (!settings.scanPastEOL && data[bytePos] == NL )
				break;
			if (checkPseudoBinary(1))
				return true;
			else
			{
				try { forwardspace(); }
				catch(EndOfDataException e) { return false; }
			}
		}
		return false;
	}
	
	/**
	  Scans 'n' characters for the specified string & returns true if found.
	  The cursor is left at the beginning of the string (if found) or at
	  the end of the scan (if not).
	  @param n number of characters
	  @param s the string to scan for
	  @return true if found.
	  @throws EndOfDataException if current position is passed end of message.
	*/
	boolean scanString(int n, String s)
		throws EndOfDataException
	{
		checkPosition(); // Will throw if we're already past end of data.

		if (n<0)
			return false;

		if (n == 0) // special case: means scan 1 char & leave cursor unchanged
			return checkString(s);

		while( moreChars() && n-- > 0)
		{
			if (!settings.scanPastEOL && data[bytePos] == NL )
				break;
			if (checkString(s))
				return true;
			else
			{
				try { forwardspace(); }
				catch(EndOfDataException e) { return false; }
			}
		}
	
		return false;
	}

//	/**
//	  Returns a 'field' of data. The field starts at the current byte
//	  and continues for specified number of characters or until the
//	  specified delimiter is found. Set 'delimiter' to (byte)0xff to
//	  mean un-delimited.
//	  Fields always implicitely end when the end-of-line is encountered.
//	  @param length number of bytes in field length
//	  @param delimiter or 0xff if there is none.
//	  @return byte array containing the field data
//	  @deprecated Use the version with a string delimiter
//	*/
//	byte[] getField(int length, byte delimiter)
//		throws EndOfDataException
//	{
//		byte b[] = new byte[length];
//		int i;
//		boolean signDelim = (delimiter==(byte)'+' || delimiter==(byte)'-');
//
//		for(i=0; i < length; i++ ) 
//		{
//			if ( moreChars() ) 
//			{
// 				byte c = curByte();
//
// 				if (c == NL                      // EOL
//				 || (delimiter != 0xff           // There IS a delimiter
//				  && ((!signDelim && c == delimiter)
//				   || (signDelim && i>0 && (c==(byte)'+' || c==(byte)'-')))))
//				{
//					break;
//				}
// 				//if ( c != NL && (delimiter == 0xff || c != delimiter))
//				else
//				{
//					b[i] = c;
//					forwardspace();
//				}
//				//else  // end of field reached!
//				//	break;
//			}
//		}
//		return i==length ? b : ArrayUtil.getField(b, 0, i);
//	}

	/**
	 * Provides default isBinary = false.
	 * @see getField(int length, String delim, boolean isBinary)
	 */
	public byte[] getField(int length, String delim)
	throws EndOfDataException
	{
		return getField(length, delim, false, false);
	}
	
	/**
	  Returns a 'field' of data. The field starts at the current byte
	  and continues for specified number of characters or until one of
	  a set of specified delimiter characters is found. Pass null as
	  'delimiter' to mean un-delimited.
	  <p>
	  Fields always implicitely end when the end-of-line is encountered.
	  @param length number of bytes in field length
	  @param delim String containing delimiter characters or null
	  @return byte array containing the field data
	*/
	public byte[] getField(int length, String delim, boolean isBinary,
		boolean isString)
		throws EndOfDataException
	{
Logger.instance().debug3("getField pos=" + bytePos + ", data.length=" + data.length);
		checkPosition(); // Will throw if we're already past end of data.
		byte b[] = new byte[length];
		int i;

		for(i=0; i < length; i++ ) 
		{
			if ( moreChars() ) 
			{
 				byte c = curByte();

 				if (isBinary)  // For binary, no EOL check and no delims.
 					;
 				else if (c == CR || c == NL                  // EOL
				 || ((isString||i>0)                         // Allow empty strings only
				     && delim != null                        // Delimiter is specified.
				     && (delim.indexOf(c) != -1)))           // This is a delimiter.
				{
					break;
				}
				else if (i>0 
				 && delim != null
				 && delim.indexOf('!') >= 0 
				 && !isNumberChar(c))
				{
					break;
				}
				
				b[i] = c;
				forwardspace();
			}
			else
				break;
		}
		return i==length ? b : ArrayUtil.getField(b, 0, i);
	}

	private boolean isNumberChar(byte b)
	{
		char c = (char)b;
		return Character.isDigit(c) || c == '.' 
		 || c == '+' || c == '-'
		 || c == 'e' || c == 'E';
	}

	/**
	  Skips to the first non-whitespace char or end of message.
	*/
	public void skipWhiteSpace()
	{
		while( bytePos < data.length 
		 &&  (  data[bytePos] == (byte)' ' || data[bytePos] == (byte)'\t'
		     || data[bytePos] == (byte)'\r' || data[bytePos] == (byte)'\n'
			 || data[bytePos] == (byte)0x00AE))
			bytePos++;
	}

	/** Debug method to dump the current buffer. */
	void dump_buffer()
	{
		int i;

		for (i=0; i < data.length; i++ )
			System.out.print((char)data[i]);
		System.out.println("");
		System.out.println("size = " + data.length);
		System.out.println("pos	= " + bytePos);
		System.out.println("");
	}

	/**
	  If currently past end of data, throws EndOfDataException, else 
	  does nothing.
	*/
	private void checkPosition()
		throws EndOfDataException
	{
		if ( bytePos >= data.length ) 
			throw new EndOfDataException(
				"Attempt to read past end of data (length="+data.length+")");
	}
	
	/**
	  @return current byte, without changing the position.
	*/
	public byte curByte()
		throws EndOfDataException
	{
		checkPosition();
		return(data[bytePos]);
	}

	/**
	  Moves pointer one space forward.
	  @throws EndOfDataException if at the end of data.
	*/
	public void forwardspace()
		throws EndOfDataException
	{
		relBytePos++;
		if ( ++bytePos > data.length ) 
		{
			String msg = "Attempt to move past end of data (pos="
				+ bytePos + ", length="+data.length+")";
			Logger.instance().debug3(msg);
			throw new EndOfDataException(msg);
		}

		// Now at first char past NL? increment line.
		if (bytePos > 0 && data[bytePos-1] == NL) {
			curLine++;
			if ( curLine - startLineNum  >= begLineBytePos.size() )
				begLineBytePos.add(new Integer(bytePos));
		}
	}

	/**
	  Moves pointer one space backward
	  @throws ScriptException if already at beginning
	*/
	private void backspace()
		throws ScriptException, EndOfDataException
	{
		if ( --bytePos < 0 ) 
			throw new ScriptException("Attempt to read before start of data");

		// Backed up to EOL of previous line? decrement line.
		if (curByte() == NL)
			curLine--;
	}

	/** @return true if we are not at the end of the data. */
	public boolean moreChars()
	{
		return bytePos < data.length;
	}

	private int savedBytePos=0;
	private int savedCurLine=0;

	/** Saves current position so we can go back if the check fails. */
	public void savePosition()
	{
		savedBytePos = bytePos;
		savedCurLine = curLine;
	}

	/** Restores current position after check fails. */
	public void restorePosition()
	{
		bytePos = savedBytePos;
		curLine = savedCurLine;
	}

	/** @return current byte position within data. */
	public int getBytePos() { return bytePos; }

	/**
	 * Sets the byte position
	 * @param pos the postion
	 */
	public void setBytePos(int pos) { bytePos = pos; }

	/** @return current byte position within line. */
	public int getRelBytePos() {
		int relPos = -1;
		int dataLineNum = curLine - startLineNum;
		if ( dataLineNum == 0 ) {
			relPos = bytePos + 1;
		}
		else if ( dataLineNum > 0 && dataLineNum < begLineBytePos.size() ) {
			int begLinePos = begLineBytePos.elementAt(dataLineNum).intValue();
			relPos = bytePos - begLinePos + 1;		
		}
		return(relPos);
	}

	/** @return current line number within data. (1 == first line) */
	public int getCurrentLine() { return curLine+1; }

	/** Sets the current line number within data. (1 == first line) */
	public void setCurrentLine(int ln) { curLine = ln; }

	private void printPosition()
	{
		int lineNum = curLine+1;
		System.out. println("Line: "+lineNum+" Position: "+getRelBytePos());
	}
	
	/**
	 * Called when every format statement starts to verify that an endless loop
	 * is not occurring. This method will throw ScriptException if this format
	 * statement has executed previously at the same message position.
	 * @param fs the format statement about to execute.
	 * @throws ScriptException if an endless loop is detected.
	 */
	public void checkFormatPosition(FormatStatement fs)
		throws EndlessLoopException
	{
		int pos = getBytePos();
		for(FormatPosition fp : formatPositions)
			if (fp.fs == fs && fp.pos == pos)
				throw new EndlessLoopException("Endless loop detected format label '"
					+ fs.label + "' at position " + pos);
		formatPositions.add(new FormatPosition(fs, pos));
		if (formatPositions.size() > 50)
			formatPositions.remove(0);
	}
}

