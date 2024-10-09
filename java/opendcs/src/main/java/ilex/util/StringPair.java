/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:10  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/30 14:50:31  mjmaloney
*  Javadocs
*
*  Revision 1.4  2001/03/20 22:56:18  mike
*  Must override equals() method so StringPair is usable in HashMap key.
*
*  Revision 1.3  2001/01/08 15:38:44  mike
*  dev
*
*  Revision 1.2  2000/12/18 02:59:30  mike
*  dev
*
*  Revision 1.1  2000/12/15 23:27:56  mike
*  created StringPair
*
*
*/
package ilex.util;

/**
Simple class that stores a pair of strings.
Similar to the C++ STL  pair<string>
*/
public class StringPair
{
	/** The first string */
	public String first;
	/** The second string */
	public String second;

	/**
	* Constructor.
	* @param f the first string.
	* @param s the second string.
	*/
	public StringPair( String f, String s )
	{
		first = f;
		second = s;
	}

	/**
	* @return hash code incorporating both strings
	*/
	public int hashCode( )
	{
		int ret = (first.hashCode() << 4) + second.hashCode();
		return ret;
	}

	/**
	* @param obj the other StringPair
	* @return true if constituent strings match in both objects.
	*/
	public boolean equals( Object obj )
	{
		if (!(obj instanceof StringPair))
			return false;
		StringPair rhs = (StringPair)obj;
		return first.equals(rhs.first) && second.equals(rhs.second);
	}
}

