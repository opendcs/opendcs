/*
*  $Id: ApiTextUtil.java,v 1.2 2022/12/06 13:45:51 mmaloney Exp $
*
*  $Source: /home/cvs/repo/odcsapi/src/main/java/org/opendcs/odcsapi/util/ApiTextUtil.java,v $
*
*  $State: Exp $
*
*  $Log: ApiTextUtil.java,v $
*  Revision 1.2  2022/12/06 13:45:51  mmaloney
*  Refactor to stop using ilex.util.Logger and start using java.util.logging.
*
*  Revision 1.1  2022/11/29 15:05:13  mmaloney
*  First cut of refactored DAOs and beans to remove dependency on opendcs.jar
*
*  Revision 1.5  2020/01/31 19:42:07  mmaloney
*  Added intEqual method to compare Integer objects allowing for null.
*
*  Revision 1.4  2019/12/11 14:31:52  mmaloney
*  Added splitQuoted method.
*
*  Revision 1.3  2019/06/10 19:35:05  mmaloney
*  Added dateEqual method.
*
*  Revision 1.2  2019/03/28 13:00:11  mmaloney
*  Added strEqualNE - consider null string the same as blank string.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2012/11/12 19:14:05  mmaloney
*  CWMS uses 't' to mean true.
*
*  Revision 1.3  2011/01/17 16:35:23  mmaloney
*  Added getFirstLine method.
*
*  Revision 1.2  2009/10/08 17:15:52  mjmaloney
*  fixed comment
*
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.20  2007/09/29 21:58:41  mmaloney
*  dev
*
*  Revision 1.19  2007/05/25 14:07:13  mmaloney
*  dev
*
*  Revision 1.18  2006/12/23 18:16:05  mmaloney
*  dev
*
*  Revision 1.17  2004/08/30 14:50:32  mjmaloney
*  Javadocs
*
*  Revision 1.16  2004/06/21 13:31:53  mjmaloney
*  Added scanAssign method.
*
*  Revision 1.15  2004/05/21 18:28:04  mjmaloney
*  Added startsWithIgnoreCase method.
*
*  Revision 1.14  2004/04/02 18:58:17  mjmaloney
*  Created.
*
*  Revision 1.13  2003/12/15 15:21:14  mjmaloney
*  Improvements to support LRGS Config Editor & EDL files.
*
*  Revision 1.12  2003/11/15 20:36:43  mjmaloney
*  Added compareIgnoreCase method that tolerates null arguments.
*
*  Revision 1.11  2003/09/02 14:37:28  mjmaloney
*  Added TeeLogger. Added more control on msg format to Logger.
*  Added TextUtil.fixedLengthFields method.
*
*  Revision 1.10  2002/10/29 00:57:13  mjmaloney
*  Added right/left justify functions to TextUtil.
*
*  Revision 1.9  2002/08/29 05:59:56  chris
*  Added the split() method; also added some tests.
*
*  Revision 1.8  2001/11/09 14:35:22  mike
*  dev
*
*  Revision 1.7  2001/10/05 17:49:59  mike
*  Added HumanReadableFormatter
*
*  Revision 1.6  2001/03/19 03:11:57  mike
*  *** empty log message ***
*
*  Revision 1.5  2000/12/31 14:14:20  mike
*  Added containsNoWhiteSpace method.
*
*  Revision 1.4  2000/12/29 02:50:05  mike
*  dev
*
*  Revision 1.3  2000/12/27 22:03:54  mike
*  Added isAllWhiteSpace
*
*  Revision 1.2  2000/12/24 02:41:07  mike
*  dev
*
*  Revision 1.1  2000/01/07 23:04:51  mike
*  Created
*
*
*/
package org.opendcs.odcsapi.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.*;

/**
* This class contains a set of static methods that supplement the methods
* in the java.text package.
*/
public class ApiTextUtil
{
	/**
	* Skip white space in the string starting at the specified parse
	* position. Leave 'pp' updated to the first non-whitespace character.
	* @param s the input string
	* @param pp the ParsePosition object
	* @return true if a non-whitespace character is found, false if the
	* end of the string is reached.
	*/
	public static boolean skipWhitespace( String s, ParsePosition pp )
	{
		int i = pp.getIndex();
		for(; i < s.length() && Character.isWhitespace(s.charAt(i)); i++);

		pp.setIndex(i);
		return i < s.length() ? true : false;
	}

	/**
	* Returns true if the passed string is empty or contains only white space.
	* @param s the input string
	* @return true if string is all whitespace
	*/
	public static boolean isAllWhitespace( String s )
	{
		for(int i = 0; i < s.length(); i++)
			if (!Character.isWhitespace(s.charAt(i)))
				return false;
		return true;
	}

	/**
	* Returns true if the passed string is non-blank and contains no white-
	* space.
	* This is appropriate for strings used as identifiers (e.g. filenames,
	* variable-names etc.)
	* @param s the input string
	* @return true if string has no whitespace
	*/
	public static boolean containsNoWhitespace( String s )
	{
		int len = s.length();
		if (len == 0)
			return false;  // empty string

		for(int i = 0; i < len; i++)
			if (Character.isWhitespace(s.charAt(i)))
				return false;

		return true;
	}

	/**
	* Collapses contiguouse whitespace in the input string into a single
	* space character in the output. This is useful for formatting paragraphs
	* that were read from a file that may contain extraneous newlines and
	* indentation, for example a long descripting field that was indented
	* in an XML file.
	* @param in the input string
	* @return string with all whitespace collapsed to single space
	*/
	public static String collapseWhitespace( String in )
	{
		String inTrimm = in.trim();
		StringBuffer sb = new StringBuffer(inTrimm.length());
		boolean ws = false;

		for(int i=0; i<inTrimm.length(); i++)
			if (!Character.isWhitespace(inTrimm.charAt(i)))
			{
				sb.append(inTrimm.charAt(i));
				ws = false;
			}
			else // This is a WS character
			{
				if (ws) // last char was also WS
					continue;
				else
				{
					sb.append(' ');
					ws = true;
				}
			}
		return sb.toString();
	}

	/**
	* Returns a string that is centered in a new string with the given width.
	* The return value is padded with blanks on both ends to the specified width.
	* @param in the input string
	* @param width width
	* @return centered string
	*/
	public static String strcenter( String in, int width )
	{
		int inlen = in.length();
		if (inlen >= width)
			return in;

		int lpad = (width - inlen) / 2;
		int rpad = width - inlen - lpad;

		StringBuffer sb = new StringBuffer();
		for(int i=0; i<lpad; i++)
			sb.append(' ');
		sb.append(in);
		for(int i=0; i<rpad; i++)
			sb.append(' ');
		return sb.toString();
	}

	/**
	* Splits a string into a series of lines with a width less than or
	* equal to the specified value.
	* @param line the input string
	* @param width maximum length of a line
	* @return line with embedded newlines
	*/
	public static String[] splitLine( String line, int width )
	{
		int curpos = 0;
		int end = line.length();
		if (end < width)
			return new String[]{ line };

		Vector vec = new Vector();
		while(curpos < end)
		{
			int remaining = end - curpos;
			int n = remaining;
			if (remaining > width)
			{
				// Have to split the line. Find last whitespace before width.
				int j = curpos + width;
				while(j>curpos && !Character.isWhitespace(line.charAt(j)))
					j--;
				if (j>curpos)
					n = j - curpos;
				else // No WS before width, find first WS after width.
				{
					j = curpos + width;
					while(j < end && !Character.isWhitespace(line.charAt(j)))
						j++;
					n = j - curpos;
				}
			}
			vec.add(line.substring(curpos, curpos + n));
			curpos += n;
			while(curpos < end && Character.isWhitespace(line.charAt(curpos)))
				curpos++;
		}
		String ret[] = new String[vec.size()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = (String)vec.elementAt(i);
		return ret;
	}

  /**
	* Split a String into pieces using c as a delimiter.
	* If the character c appears n times in s, this will return n+1
	* Strings in the array.  Note that that means this will always
	* return at least one String, even if s is empty.
	* Examples:
	* <pre>  split("a:b:c", ':')  yields  "a", "b", "c"
	* split("a:b:", ':')  yields  "a", "b", ""
	* split(":b:c", ':')  yields  "", "b", "c"
	* split("a::c", ':')  yields  "a", "", "c"
	* split("", ':')      yields  ""</pre>
	* @param s the input string
	* @param c the delimiter
	* @return results
	*/
	public static String[] split( String s, char c )
    {
        Vector v = new Vector();
        int i = 0;
        int next;
        while ((next = s.indexOf(c, i)) != -1) {
            v.add(s.substring(i, next));
            i = next + 1;
        }
        v.add(s.substring(i));

        return (String[]) v.toArray(new String[0]);
    }

  /**
	* Compares Strings.  Either can be null.
	* If both are null, equality is true.
	* @param s1 string 1
	* @param s2 string 2
	* @return true if equal
	*/
	public static boolean strEqual( String s1, String s2 )
	{
        // let's use logic!
        return !(s1==null ^ s2==null) && (s1==null || s1.equals(s2));

      /*
		if (s1 == null)
		{
			if (s2 == null)
				return true;
			else
				return false;
		}
		else // s1 != null
		{
			if (s2 == null)
				return false;
			else
				return s1.equals(s2);
		}
      */
	}
	
	public static boolean dateEqual(Date d1, Date d2)
	{
		if (d1 == null)
		{
			if (d2 == null)
				return true;
			else
				return false;
		}
		else // d1 != null
		{
			if (d2 == null)
				return false;
			else
				return d1.equals(d2);
		}
 	}
	
	public static boolean intEqual(Integer i1, Integer i2)
	{
		if (i1 == null)
		{
			if (i2 == null)
				return true;
			else
				return false;
		}
		else // i1 != null
		{
			if (i2 == null)
				return false;
			else
				return i1.equals(i2);
		}
 	}

	/**
	 * Compare strings, but consider null the same as an empty string.
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean strEqualNE(String s1, String s2)
	{
		if (s1 == null) s1 = "";
		if (s2 == null) s2 = "";
		return strEqual(s1, s2);
	}

	/**
	* Compares strings for equality, allowing either string to be null.
	* Returns true if both arguments are null, or neither is null and they
	* match, ignoring case.
	* @param s1 string 1
	* @param s2 string 2
	* @return true if equal
	*/
	public static boolean strEqualIgnoreCase( String s1, String s2 )
	{
        // let's use logic!
        return !(s1==null ^ s2==null) &&
               (s1==null || s1.equalsIgnoreCase(s2));

      /*
		if (s1 == null)
		{
			if (s2 == null)
				return true;
			else
				return false;
		}
		else // s1 != null
		{
			if (s2 == null)
				return false;
			else
				return s1.equalsIgnoreCase(s2);
		}
	  */
    }


	/**
	* Compares strings, allowing either string to be null.
	* Returns true if both arguments are null, or neither is null and they
	* match, ignoring case. A null string is considered greater than a
	* non-null string.
	* @param s1 string 1
	* @param s2 string 2
	* @return 0 if equal
	*/
	public static int strCompareIgnoreCase( String s1, String s2 )
	{
		if (s1 == null)
		{
			if (s2 == null)
				return 0;
			else
				return 1;
		}
		else // s1 != null
		{
			if (s2 == null)
				return -1;
			else
				return s1.compareToIgnoreCase(s2);
		}
    }

	/**
	* Returns true if the first string starts with a copy of the second
	* string, without regard to case.
	* @param s the input string
	* @param v the string to check for
	* @return true if the first string starts with a copy of the second
	*/
	public static boolean startsWithIgnoreCase( String s, String v )
	{
		return s.length() >= v.length()
		 && s.substring(0, v.length()).equalsIgnoreCase(v);
	}

	/**
	* Returns true if the first string ends with a copy of the second
	* string, without regard to case.
	* @param s the input string
	* @param v the string to check for
	* @return true if the first string starts with a copy of the second
	*/
	public static boolean endsWithIgnoreCase( String s, String v )
	{
		int len = s.length();
		int vlen = v.length();
		return len >= vlen
		 && s.substring(len - vlen).equalsIgnoreCase(v);
	}

	/**
	* Sets the length of the string by padding with blanks on the right.
	* If the passed string's length is greater than len, it is truncated.
	* The return value is guaranteed to be a string of exactly 'len' characters.
	* @param s the input string
	* @param len the desired length
	* @return padded string
	*/
	public static String setLengthLeftJustify( String s, int len )
	{
		if (s == null)
			s = "";
		StringBuffer sb = new StringBuffer(len);
		int x;
		for(x=0; x<s.length() && x<len; x++)
			sb.append(s.charAt(x));
		while (x++ < len)
			sb.append(' ');
		return sb.toString();
	}

	/**
	* Sets the length of the string by padding with blanks on the left.
	* If the passed string's length is greater than len, it is truncated.
	* The return value is guaranteed to be a string of exactly 'len' characters.
	* @param s the input string
	* @param len the desired length
	* @return padded string
	*/
	public static String setLengthRightJustify( String s, int len )
	{
		if (s == null)
			s = "";
		else if (s.length() > len)
			s = s.substring(0, len);

		StringBuffer sb = new StringBuffer(len);
		int x = len - s.length();
		for(int i=0; i<x; i++)
			sb.append(' ');

		sb.append(s);

		return sb.toString();
	}

	/**
	* Returns true if string is the word "true", "on", or "yes".
	* Otherwise, returns false.
	* @param s the input string
	* @return true if string is the word "true", "on", or "yes".
	*/
	public static boolean str2boolean( String s )
	{
		if (s == null)
			return false;
		s = s.trim().toLowerCase();
		return s.startsWith("t") || s.startsWith("y")
			|| s.equalsIgnoreCase("on");
	}

	/**
	* Splits a line of text int fixed-width fields and returns an array
	* of string fields.
	* Pass and array of field widths.
	* Returned array may be less than the # of widths if the passed line
	* is too short.
	* The final field returned may be less than the specified width.
	* Example: line="AAAABBCCCDDDDDEEEEEEEE" widths=4,2,3,5,8,
	* Return strings: AAAA, BB, CCC, DDDDD, EEEEEEEE
	* @param line the input string
	* @param widths widths of each field
	* @return extracted fields
	*/
	public static String[] getFixedFields( String line, int[] widths )
	{
		int curpos=0;
		int linelen = line.length();
		int fieldnum = 0;
		String ret[] = new String[widths.length];
		while(curpos < linelen && fieldnum < widths.length)
		{
			int w = widths[fieldnum];
			ret[fieldnum] = curpos + w >= linelen ? line.substring(curpos) :
				line.substring(curpos, curpos+w);

			curpos += w;
			fieldnum++;
		}
		if (fieldnum < widths.length)
		{
			String newret[] = new String[fieldnum];
			for(int i=0; i<fieldnum; i++)
				newret[i] = ret[i];
			ret = newret;
		}
		return ret;
	}

	/**
	* Expands a string by putting spaces before every capital letter.
	* @param s the input string
	* @return the expanded string.
	*/
	public static String capsExpand( String s )
	{
		StringBuffer ret = new StringBuffer();
		for(int i=0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			if (i != 0 && Character.isUpperCase(c))
				ret.append(' ');
			ret.append(c);
		}
		return ret.toString();
	}

	/**
	* Collapses a string by removing all white space.
	* @param s the input string
	* @return the collapsed string
	*/
	public static String removeAllSpace( String s )
	{
		StringBuffer ret = new StringBuffer();
		for(int i=0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			if (!Character.isWhitespace(c))
				ret.append(c);
		}
		return ret.toString();
	}

	/**
	* Scans line for an assignment of the form label[ws]=[ws]value.
	* <ul>
	* <li>The supplied label can be anywhere in the string.</li>
	* <li>whitespace is optional on either side of equals sign.</li>
	* <li>Value may be a single whitespace delimited word, or enclosed
	* in matching double or single quotes.</li>
	* <li>Minimum length specifies the minimum length of the value and
	* may also be used to skip over known whitespace in the string.</li>
	* <li>If the ignoreCase argument is true, label will be matched
	* regardless of case</li>
	* </ul>
	* @param line the input string
	* @param label the label
	* @param minLength the minimum length of the value
	* @param ignoreCase true if you want to ignore case of label
	* @return assigned value if found, null if not.
	*/
	public static String scanAssign( String line, String label, int minLength, boolean ignoreCase )
	{
		String tln = ignoreCase ? line.toLowerCase() : line;
		String tlb = ignoreCase ? label.toLowerCase() : label;
		int length = tln.length();

		// Find the label
		int idx = tln.indexOf(tlb);
		if (idx == -1)
			return null;

		// Skip past label and any whitespace before equals sign.
		for(idx += label.length();
		    idx < length && Character.isWhitespace(tln.charAt(idx));
			idx++);
		if (idx >= length)
			return null;

		if (tln.charAt(idx) != '=')
			return null;

		// Skip past '=' and any whitespace before value.
		for(++idx; idx < length
		        && Character.isWhitespace(line.charAt(idx)); idx++);
		if (idx >= length)
			return null;
		
		if (line.charAt(idx) == '"')
		{
			++idx;
			int edx = line.indexOf('"', idx);
			if (edx == -1)
				return line.substring(idx);
			else
				return line.substring(idx, edx);
		}
		else if (line.charAt(idx) == '\'')
		{
			++idx;
			int edx = line.indexOf('\'', idx);
			if (edx == -1)
				return line.substring(idx);
			else
				return line.substring(idx, edx);
		}

		// Value goes to first white space or end of line
		int start = idx;
		idx = start + minLength;
		if (idx >= length)
			idx = length;
		for(; idx < length
		   && !Character.isWhitespace(line.charAt(idx)); idx++);

		return line.substring(start, idx);
	}

	/**
	 * Collapses the passed string to something suitable for use as a
	 * variable name. Whitespace and special characters are removed. If
	 * it starts with a digit, an underscore is prefixed.
	 */
	public static String collapse2Name(String txt)
	{
		StringBuilder sb = new StringBuilder(removeAllSpace(txt));
		if (sb.length() == 0)
			return "_null";
		if (Character.isDigit(sb.charAt(0)))
			sb.insert(0, '_');
		for(int i=0; i<sb.length(); i++)
		{
			char c = sb.charAt(i);
			if (!Character.isLetterOrDigit(c))
				sb.setCharAt(i, '_');
		}
		return sb.toString();
	}

	/**
	 * @return true if passed string is a hex number.
	 */
	public static boolean isHexString(String s)
	{
		int len = s.length();
		for(int i=0; i<len; i++)
			if (!ApiByteUtil.isHexChar((byte)s.charAt(i)))
				return false;
		return true;
	}

  /**
	* This provides some tests of some of the above functions.
	* @param args
	*/
	public static void main( String[] args )
	{
		test_Assign();
		//test_FixField();
        //test_splitLine();
        //test_split();
        //test_strEqual();
    }

	public static void test_Assign( )
	{
		String inp = "#// abcdeFOO=BAR\n";
		System.out.println("ScanAssignment '" + inp + "' results in '" +
			scanAssign(inp, "FOo", 1, true) + "'");

		inp = "#// abcdeFOO=\"string in doublequotes\"";
		System.out.println("ScanAssignment '" + inp + "' results in '" +
			scanAssign(inp, "FOo", 1, true) + "'");

		inp = "#// abcdeFOO = 'string in singlequotes'";
		System.out.println("ScanAssignment '" + inp + "' results in '" +
			scanAssign(inp, "FOo", 1, true) + "'");
	}

	public static void test_FixField( )
	{
		String orig = "AAAABBCCCDDDDDDDDEF";
		String fields[] = getFixedFields(orig, new int[]{4, 2, 3, 8, 1, 2});
		System.out.println("orig '" + orig + "'");
		for(int i=0; i<fields.length; i++)
			System.out.println("field[" + i + "] '" + fields[i] + "'");
	}

  /**
	* Test the splitLine() function above.
	*/
	public static void test_splitLine( )
    {
        System.out.println("test_splitLine:\n");

		String tests[] = { "This is a short line.",
			"This is a much longer line but it can be easily broken along white space into much shorter lines for easy outputting.",
			"Thisisamuchlongerlinebutwhichcan'tbeeasilybrokenalongwhitespaceintomuchshorterlinesforeasyoutputting.",
			"Thisisamuchlongerlinebutwhichcan'tbeeasilybrokenalong whitespaceintomuchshorterlinesforeasyoutputting."
		};

		for(int i=0; i<tests.length; i++)
		{
			System.out.println("ORIGINAL: " + tests[i]);
			String split[] = splitLine(tests[i], 40);
			for(int j=0; j<split.length; j++)
				System.out.println("  " + j + ": " + split[j]);
		}
	}

  /**
	* Test the split() function.
	*/
	public static void test_split( )
    {
        System.out.println("\n\ntest_split:");

        String tests[] = {
            "a:b:c", "a:b:", ":b:c", "a::c", ""
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] r = split(tests[i], ':');

            System.out.print("\nResult of splitting '" + tests[i] + "' is ");
            for (int j = 0; j < r.length; ++j) {
                System.out.print("'" + r[j] + "' ");
            }
        }
    }

  /**
	* Test the strEqual function.
	*/
	public static void test_strEqual( )
    {
        System.out.println("\n\ntest_strEqual:");

        String[] s0s = { null, "a", "A" };
        String[] s1s = { null, "a", "A" };
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                String s0 = s0s[i];
                String s1 = s1s[j];
                System.out.println("s0 == " + s0 + ", s1 == " + s1 + ", " +
                    "result is " + strEqual(s0, s1));
            }
        }
    }
	
	/** Return the first line of a string. Useful for displaying 'brief' descriptions
	 * in a single column on a GUI.
	 */
	public static String getFirstLine(String tmp)
	{
		if (tmp == null)
			return "";
		int len = tmp.length();
		int ci = len;
		if (ci > 60)
			ci = 60;
		int i = tmp.indexOf('\r');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('\n');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('.');
		if (i > 0 && i < ci)
			ci = i;
		if (ci < len)
			return tmp.substring(0,ci);
		else
			return tmp;
	}
	
	/**
	 * Splits a string into words. Strings within double quotes are a single String
	 * in the output.
	 * @param text
	 * @return
	 */
	public static String[] splitQuoted(String text)
	{
		ArrayList<String> results = new ArrayList<String>();
	    String regex = "\"([^\"]*)\"|(\\S+)";

	    Matcher m = Pattern.compile(regex).matcher(text);
	    while (m.find())
	    {
	    	String s = (m.group(1) != null) ? m.group(1) : m.group(2);
	    	results.add(s);
	    }
	    String ret[] = new String[results.size()];
	    for(int idx = 0; idx < ret.length; idx++)
	    	ret[idx] = results.get(idx);
	    return ret;
	}
}

