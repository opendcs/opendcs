/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2011/09/10 10:23:34  mmaloney
*  added getFloat method
*
*  Revision 1.3  2009/10/30 15:31:25  mjmaloney
*  Added toHexAsciiString -- useful for debugging output.
*
*  Revision 1.2  2008/09/05 12:53:53  mjmaloney
*  LRGS 7 dev
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.11  2004/08/30 14:50:24  mjmaloney
*  Javadocs
*
*  Revision 1.10  2003/12/20 00:32:50  mjmaloney
*  Implemented TimeoutInputStream.
*
*  Revision 1.9  2003/08/19 15:51:37  mjmaloney
*  dev
*
*  Revision 1.8  2003/05/16 20:12:38  mjmaloney
*  Added EnvExpander. This is preferrable to ShellExpander because
*  it is platform independent.
*
*  Revision 1.7  2003/04/09 15:16:11  mjmaloney
*  dev.
*
*  Revision 1.6  2003/03/27 21:17:55  mjmaloney
*  drgs dev
*
*  Revision 1.5  2001/04/12 12:26:18  mike
*  test checkin from boss
*
*  Revision 1.4  2000/03/12 22:41:34  mike
*  Added PasswordFile & PasswordFileEntry classes.
*
*  Revision 1.3  1999/11/16 14:46:32  mike
*  Added getCString function - retrieve a null-terminated C string from a byte
*  array.
*
*  Revision 1.2  1999/10/26 12:44:27  mike
*  Fixed sign extension problems for getting integers from bytes.
*
*  Revision 1.1  1999/10/20 21:10:04  mike
*  Initial implementation
*/
package ilex.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
* This class contains several static methods for manipulating arrays of
* bytes.
*/
public class ByteUtil
{
	/**
	* Convert 4 bytes from the passed array to an integer. Start at the
	* specified offset. Assume big-endian encoding.
	* @param b the byte array
	* @param offset the offset
	* @return integer constructed from bytes
	*/
	public static final int getInt4_BigEndian( byte[] b, int offset )
	{
		return 
			    ((int)b[offset+3] & 0xff)
			 | (((int)b[offset+2] & 0xff) << 8) 
			 | (((int)b[offset+1] & 0xff) << 16) 
			 | (((int)b[offset+0] & 0xff) << 24);
	}

	public static final long getInt8_BigEndian(byte[] b, int offset)
	{
		return 
		    ((long)b[offset+7] & 0xff)
		 | (((long)b[offset+6] & 0xff) << 8) 
		 | (((long)b[offset+5] & 0xff) << 16) 
		 | (((long)b[offset+4] & 0xff) << 24)
	     | (((long)b[offset+3] & 0xff) << 32)
	     | (((long)b[offset+2] & 0xff) << 40) 
	     | (((long)b[offset+1] & 0xff) << 48) 
	     | (((long)b[offset+0] & 0xff) << 56);
	}
	
	/**
	* Convert 4 bytes from the passed array to an integer. Start at the
	* specified offset. Assume little-endian encoding.
	* @param b the byte array
	* @param offset the offset
	* @return integer constructed from bytes
	*/
	public static final int getInt4_LittleEndian( byte[] b, int offset )
	{
		return 
			   ((int)b[offset+0] & 0xff)
			| (((int)b[offset+1] & 0xff) << 8)
			| (((int)b[offset+2] & 0xff) << 16) 
			| (((int)b[offset+3] & 0xff) << 24);
	}

	/**
	* Convert 2 bytes from the passed array to an integer. Start at the
	* specified offset. Assume little-endian encoding.
	* @param b the byte array
	* @param offset the offset
	* @return integer constructed from bytes
	*/
	public static final int getInt2_LittleEndian( byte[] b, int offset )
	{
		return 
			   ((int)b[offset+0] & 0xff)
			| (((int)b[offset+1] & 0xff) << 8);
	}

	/**
	* Convert 2 bytes from the passed array to an integer. Start at the
	* specified offset. Assume big-endian encoding.
	* @param b the byte array
	* @param offset the offset
	* @return integer constructed from bytes
	*/
	public static final int getInt2_BigEndian( byte[] b, int offset )
	{
		return 
			   ((int)b[offset+1] & 0xff)
			| (((int)b[offset+0] & 0xff) << 8);
	}

	/**
	* Encode a 4-byte integer and place it in a byte array at the
	* specified offset. Assume big-endian encoding.
	* @param i the integer
	* @param b the byte array
	* @param offset at which to start
	*/
	public static final void putInt4_BigEndian( int i, byte[] b, int offset )
	{
		b[offset  ] = (byte)((i>>24) & 0xff);
		b[offset+1] = (byte)((i>>16) & 0xff);
		b[offset+2] = (byte)((i>>8 ) & 0xff);
		b[offset+3] = (byte)(i       & 0xff);
	}

	/**
	* Encode a 4-byte integer and place it in a byte array at the
	* specified offset. Assume little-endian encoding.
	* @param i the integer
	* @param b the byte array
	* @param offset at which to start
	*/
	public static final void putInt4_LittleEndian( int i, byte[] b, int offset )
	{
		b[offset+3] = (byte)((i>>24) & 0xff);
		b[offset+2] = (byte)((i>>16) & 0xff);
		b[offset+1] = (byte)((i>>8 ) & 0xff);
		b[offset  ] = (byte)(i       & 0xff);
	}

	/**
	* Encode a 2-byte integer and place it in a byte array at the
	* specified offset. Assume little-endian encoding.
	* @param i the integer
	* @param b the byte array
	* @param offset at which to start
	*/
	public static final void putInt2_LittleEndian( int i, byte[] b, int offset )
	{
		b[offset  ] = (byte)(i       & 0xff);
		b[offset+1] = (byte)((i>>8 ) & 0xff);
	}

	/**
	* Pull a null terminated C-style string out of a byte array.
	* @param b byte array
	* @param offset offset at which to start
	* @return String
	*/
	public static final String getCString( byte[] b, int offset )
	{
		StringBuffer ret = new StringBuffer();
		for(int i = offset; i < b.length && b[i] != (byte)0; i++)
			ret.append((char)b[i]);
		return new String(ret);
	}
	
	/**
	 * Encode a string into the buffer as a C-style null-terminated array of
	 * bytes.
	 * @param s the string to encode
	 * @param b the byte buffer to encode it into
	 * @param offset the starting point within the buffer
	 * @param padLength The length of the area within the buffer. The string
	 * will be padded to the end of the field with null bytes.
	 */
	public static final void putCString(String s, byte[] b, int offset, int padLength)
	{
		if (s == null)
			s = "";
		int strlen = s.length();
		if (strlen >= padLength)
			strlen = padLength-1; // Leave at least 1 char for a null byte
		for (int i=0; i<strlen; i++)
			b[offset+i] = (byte)s.charAt(i);
		for(int i=strlen; i<padLength; i++)
			b[offset + i] = (byte)0;
	}

	/**
	* Converts a byte array to a hex character string. 
	* Each byte will be represented by 2 hex characters.
	* @param b the byte array
	* @return hex string
	*/
	public static final String toHexString( byte[] b )
	{
		return toHexString(b, 0, b.length);
	}

	/**
	* Converts a byte array to a hex character string. 
	* Each byte will be represented by 2 hex characters.
	* @param b the byte array
	* @param offset at which to start
	* @param n number of bytes to process
	* @return hex string
	*/
	public static final String toHexString( byte[] b, int offset, int n )
	{
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < n; i++)
		{
			int x = (int)b[offset+i] & 0xff;
			ret.append(toHexChar(x / 16));
			ret.append(toHexChar(x % 16));
		}
		return new String(ret);
	}
	
	/**
	 * Pass a byte array that might contain a mix of ascii & binary chars.
	 * ASCII chars are printed, others are shown as hex.
	 * @param b byte array
	 * @param offset offset where to start
	 * @param n num bytes to print
	 * @return the bytes as a printable string
	 */
	public static final String toHexAsciiString(byte[] b, int offset, int n )
	{
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < n; i++)
		{
			int x = (int)b[offset+i] & 0xff;
			if (x > (int)' ' && x <= (int)'~')
				ret.append("  " + (char)x);
			else
			{
				ret.append(' ');
				ret.append(toHexChar(x / 16));
				ret.append(toHexChar(x % 16));
			}
		}
		return ret.toString();
	}

	/**
	* Converts hex char representation of an int in the range 0...15.
	* @param x the int.
	* @return single hex char.
	*/
	public static final char toHexChar( int x )
	{
		x %= 16;
		return x < 10 ? (char)((int)'0' + x) : (char)((int)'A' + (x-10));
	}

	/**
	* Converts hex char to in in the range 0...15
	* @param c the hex char
	* @return the int
	*/
	public static final int fromHexChar( char c )
	{
		if (Character.isDigit(c))
			return (int)c - (int)'0';
		else switch(c)
		{
			case 'a': case 'A': return 10; 
			case 'b': case 'B': return 11;
			case 'c': case 'C': return 12;
			case 'd': case 'D': return 13;
			case 'e': case 'E': return 14;
			case 'f': case 'F': return 15;
			default: return -1;
		}
	}

	/**
	* @param c the char
	* @return true if the character is a hex char.
	*/
	public static boolean isHexChar( byte c )
	{
		int i = fromHexChar((char)c);
		return i != -1;
	}

	/**
	* Converts a hex string into a byte array.
	* If the string has an odd length, it is parsed as if a trailing zero
	* were added.
	* @param hex the hex string
	* @return the byte array
	*/
	public static final byte[] fromHexString( String hex )
	{
		int len = hex.length();

		if (len == 0)
			return null;

		if (len % 2 == 1) 	// Odd length, add a zero to the beginning.
		{
			hex = '0' + hex;
			len++;
		}

		byte ret[] = new byte[len/2];   // Each byte is 2 hex chars
		int j=0;
		for(int i = 0; i<len; i += 2)
		{
			char upper = hex.charAt(i);
			char lower = hex.charAt(i+1);
			ret[j++] = (byte)((fromHexChar(upper) << 4) + fromHexChar(lower));
		}
		return ret;
	}
	
	/**
	 * Search through a buffer for a pattern, return the index of the 
	 * beginning of the buffer to the start of the pattern, or -1 if not found.
	 * @param buf the buffer to scan
	 * @param len number of bytes in buffer to scan
	 * @param pattern the pattern to scan for
	 * @return index of start of pattern, or -1 if not found.
	 */
	public static int indexOf(byte[] buf, int len, byte[] pattern)
	{
		int idx=0;
		int stop = len - pattern.length;
		int patlen = pattern.length;
		
	  nextIdx:
		for(; idx < stop; idx++)
		{
			for(int x=0; x<patlen; x++)
				if (buf[idx+x] != pattern[x])
					continue nextIdx;
			return idx;
		}
		return -1;
	}

	private static final byte zeroDigit = (byte)'0';
	private static final byte nineDigit = (byte)'9';
	private static final byte spaceChar = (byte)' ';
	private static final byte minusChar = (byte)'-';
	private static final byte plusChar = (byte)'+';

	/**
	* Parses an int from the passed byte array (of digits).
	* Allows leading or trailing blanks.
	* The first non-blank may be a sign.
	* @param data the byte array
	* @param offset the offset at which to start
	* @param len number of digits
	* @return integer
	*/
	public static int parseInt( byte[] data, int offset, int len ) throws NumberFormatException
	{
		int sign = 1;
		int value = 0;
		boolean seenNonBlank = false;
		for(int i=0; i<len; i++)
		{
			byte c = data[offset+i];
			if (c == spaceChar)
			{
				if (!seenNonBlank)
					continue;
				else
					break;
			}
			seenNonBlank = false;

			if (c == minusChar && !seenNonBlank)
				sign = -1;
			else if (c == plusChar && !seenNonBlank)
				sign = 1;
			else if (c >= zeroDigit && c <= nineDigit)
				value = (value * 10) + (c - zeroDigit);
			else
				throw new NumberFormatException("Illegal character '"
					+ (char)c + "' in integer field.");
		}
		return value * sign;
	}

	
	/**
	* Convert 4 bytes from the passed array to an integer. Start at the
	* specified offset. Assume big-endian encoding.
	* @param b the byte array
	* @param offset the offset
	* @return integer constructed from bytes
	*/
	public static final float getFloat4( byte[] b, int offset )
	{
		DataInputStream dis = new DataInputStream(
			new ByteArrayInputStream(b, offset, 4));
		try { return dis.readFloat(); }
		catch(Exception ex) { return (float)0.0; }
	}
	
	/**
	 * Return true if two byte arrays are the same, false otherwise.
	 * Tolerates either being null. Returns true if both are null.
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static boolean equals(byte[] a1, byte[] a2)
	{
		if (a1 == null)
			return a2 == null;
		else if (a2 == null)
			return false;
		
		if (a1.length != a2.length)
			return false;
		
		for(int i=0; i<a1.length; i++)
			if (a1[i] != a2[i])
				return false;
		
		return true;
	}

//	public static final void main(String args[])
//	{
		// Tests for big/little endian stuff:
//		byte b[] = new byte[4];
//		b[0] = 0x1;
//		b[1] = 0x2;
//		b[2] = 0x3;
//		b[3] = 0x4;
//
//		int iL = getInt4_LittleEndian(b, 0);
//		int iB = getInt4_BigEndian(b, 0);
//		System.out.println("   Big Endian: " + iB);
//		System.out.println("Little Endian: " + iL);

		// Test for hex conversion stuff:
//		String test = "10112030405060708090a00A1B2C3D4E5F6";
//		System.out.println("Converting String '" + test + "' to byte array:");
//		byte b[] = fromHexString(test);
//		for(int i = 0; i < b.length; i++)
//		{
//			int x = (int)b[i] & 0xff;
//			System.out.println("byte["+i+"] = " + x +", " + 
//				Integer.toHexString(x));
//		}
//		System.out.println("Back to string '" + toHexString(b) + "'");
//	}
}

