/*
*  $Id$
*/
package ilex.util;

import java.util.Date;

/**
Stores a pair of Date objects, commonly used to represent the beginning
and end of a time period.
*/
public class DatePair
{
	/** The first Date */
	public Date first;
	/** The second Date */
	public Date second;

	/**
	* Constructor.
	* @param f the first Date.
	* @param s the second Date.
	*/
	public DatePair( Date f, Date s )
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
	* @return true if constituent Dates match in both objects.
	*/
	public boolean equals( Object obj )
	{
		if (!(obj instanceof DatePair))
			return false;
		DatePair rhs = (DatePair)obj;
		return first.equals(rhs.first) && second.equals(rhs.second);
	}
}

