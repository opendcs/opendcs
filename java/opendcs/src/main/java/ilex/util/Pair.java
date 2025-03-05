/*
 * $Id$
 *
 * $State$
 *
 * $Log$
 * Revision 1.1  2008/04/04 18:21:10  cvs
 * Added legacy code to repository
 *
 * Revision 1.2  2004/08/30 14:50:29  mjmaloney
 * Javadocs
 *
 * Revision 1.1  2002/08/26 17:47:33  chris
 * SQL Database I/O development
 *
 */


/**
This is a simple utility class to store a pair of objects.
Similar to the pair template in the C++ STL.
*/
package ilex.util;

public class Pair<FirstType,SecondType>
{
	/** first object in the pair. */
	public FirstType first;
	/** second object in the pair. */
	public SecondType second;

    /**
	* Constructor.
	* @param f the first object
	* @param s the second object
	*/
	public Pair(FirstType f, SecondType s)
	{
		first = f;
		second = s;
	}

	/**
	 * Create a pair
	 *
	 * @param <FirstType>
	 * @param <SecondType>
	 * @param f
	 * @param s
	 * @return
	 */
	public static <FirstType,SecondType> Pair<FirstType,SecondType> of(FirstType f, SecondType s)
	{
		return new Pair<>(f,s);
	}

	/**
	* @return a hashcode incorporating the hashcodes from both constituents.
	*/
	public int hashCode( )
	{
		int ret = (first.hashCode() << 4) + second.hashCode();
		return ret;
	}

	/**
	* Return true if both components of this equal components of passed object.
	* @param obj the object to compare.
	* @return true if both components of this equal components of passed object.
	*/
	public boolean equals( Object obj )
	{
		if ((obj instanceof Pair<?,?>))
		{
			Pair<?,?> rhs = (Pair<?,?>) obj;
			return first.equals(rhs.first) && second.equals(rhs.second);
		}
		return false;
	}
}
