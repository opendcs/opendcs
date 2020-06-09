/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2008/04/19 15:06:33  cvs
*  added TW Sample
*
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.7  2007/12/05 22:44:36  mmaloney
*  dev
*
*  Revision 1.6  2004/11/10 19:26:34  mjmaloney
*  Dev
*
*  Revision 1.5  2004/08/30 14:50:24  mjmaloney
*  Javadocs
*
*  Revision 1.4  2002/11/18 18:39:25  mjmaloney
*  Fixed bug in parsing octal escape sequences.
*
*  Revision 1.3  2002/01/28 02:57:27  mike
*  dev
*
*  Revision 1.2  2000/09/08 19:02:36  mike
*  Added wrapString method.
*
*  Revision 1.1  2000/03/31 16:11:36  mike
*  Created AsciiUtil.java
*
*
*/
package ilex.util;

/**
Collection of static utility methods for manupulating ASCII strings.
*/
public class AsciiUtil
{
	public static final int SOH = 1;
	public static final int STX = 2;
	public static final int ETX = 3;
	public static final int DLE = 16;

	/**
	* Returns an array of bytes parsed from the passed string.
	* The standard UNIX escape sequences are handled and converted
	* to their binary equivalent. These include \n (newline),
	* \r (carriage return), \b (backspace), \f (form-feed), \t (tab).
	* Also handled are 3 digit octal sequences.
	* @param asciiString the string
	* @return the binary result
	*/
	public static byte[] ascii2bin( String asciiString )
	{
		byte[] ascii = asciiString.getBytes();
		byte[] bin = new byte[ascii.length];
		int n = 0;

		for(int i=0; i < ascii.length; i++)
		{
			if (ascii[i] == '\\' && i < ascii.length-1)
			{
				i++;
			    if (Character.isDigit((char)ascii[i]))
			    {
					int c = 0;
					for(int j=0; j<3 && i<ascii.length && 
						Character.isDigit((char)ascii[i]); j++, i++)
					{
						c = (c<<3) + ((int)ascii[i] - (int)'0');
					}
					bin[n++] = (byte)c;
					i--;  // Will get incremented in outer loop.
			    }
			    else
			    {
					switch((char)ascii[i])
					{
					case '\0': break;
					case '\\': bin[n++] = (byte)'\\'; break;
					case 'n': bin[n++] = (byte)'\n'; break;
					case 'r': bin[n++] = (byte)'\r'; break;
					case 'b': bin[n++] = (byte)'\b'; break;
					case 't': bin[n++] = (byte)'\t'; break;
					case 'f': bin[n++] = (byte)'\f'; break;
					default:
						bin[n++] = ascii[i];
					}
			    }
			}
			else
				bin[n++] = ascii[i];
		}

		return ArrayUtil.getField(bin, 0, n);
	}

	/**
	 * This is the inverse of ascii2bin.
	 * Return an ASCII representation of the passed bytes. Non-printable
	 * characters are converted to escape sequences.
	 * <p>
	 * Note that since there are is more than one possible escape sequence
	 * for certain binary chars, it is not necessarily true
	 * that   str.equals( bin2ascii(ascii2bin(str)) )
	 * @param bin string containing binary characters
	 * @param specialChars other characters that must be escaped.
	 * @return printable String with backslash sequences
	 */
	public static String bin2ascii( byte[] bin, String specialChars )
	{
		StringBuffer sb = new StringBuffer();
		int idx;
	
		for(idx = 0; idx < bin.length; idx++)
		{
			int c = bin[idx] & 0xff;

			if (c >= (int)' ' && c <= (int)'~' 
			 && c != (int)'\\'
			 && (specialChars == null || specialChars.indexOf(c) == -1))
				sb.append((char)c);
			else /* Use some type of escape sequence. */
			{
				sb.append('\\');
				switch((char)c)
				{
				case '\\': sb.append('\\'); break;
				case '\n': sb.append('n'); break;
				case '\r': sb.append('r'); break;
				case '\b': sb.append('b'); break;
				case '\f': sb.append('f'); break;
				case '\t': sb.append('t'); break;
				default:
					sb.append((char)( (c>>6) + (int)'0' ));
					sb.append((char)( ((c>>3)&7) + (int)'0' ));
					sb.append((char)( (c&7) + (int)'0' ));
				}
			}
		}
		return new String(sb);
	}

	/**
	 * Calls bin2ascii with no explicit special characters.
	 * @param bin string containing binary characters
	 * @return printable String with backslash sequences
	 */
	public static String bin2ascii( byte[] bin)
	{
		return bin2ascii(bin, null);
	}

	/**
	* Inserts newlines to cause a string to wrap at a specified width.
	* @param ins the input string
	* @param linelen the maximum line length
	* @return String with newlines such that no line is > max.
	*/
	public static String wrapString( String ins, int linelen )
	{
		StringBuffer sb = new StringBuffer(ins);
		int length = ins.length();

	  nextLine:
		for(int linestart = 0; linestart + linelen < length; )
		{
			for(int i = linestart + linelen - 1; i>linestart; i--)
				if (sb.charAt(i) == '\n')
					continue nextLine;
				else if (Character.isWhitespace(sb.charAt(i)))
				{
					sb.setCharAt(i, '\n');
					linestart = i+1;
					continue nextLine;
				}
			// Fell through, couldn't break this line.
			linestart += linelen;
		}
		return sb.toString();
	}

	/**
	* test main
	* @param args the arguments.
	*/
	public static void main( String[] args ) // test only
	{
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < args.length; i++)
		{
			sb.append(args[i]);
			sb.append(" ");
		}
		String s = sb.toString();
		System.out.println("wrapString Test:");
		System.out.println(wrapString(s, 15));

		System.out.println("asciibin test:");
		for(int i=0; i<args.length; i++)
		{
			byte[] bin = ascii2bin(args[i]);
			s = bin2ascii(bin);
			System.out.println("Args["+i+"]: '" + args[i] + "'");
			System.out.println("   cnvt to binary: '" + new String(bin) + "'");
			System.out.println("   back to string: '" + s + "'");
		}

	}
}
