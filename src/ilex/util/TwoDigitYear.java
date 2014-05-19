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
*  Revision 1.2  2004/08/30 14:50:32  mjmaloney
*  Javadocs
*
*  Revision 1.1  1999/09/23 12:33:41  mike
*  Initial implementation
*
*
*/
package ilex.util;

/**
* This class contains routines for handling two digit year values. All
* functions that have to process two digit years should do so through this
* class. This will ensure it gets done properly and consistently.
* The class assumes that values lower than a cutoff (default = 70) are
* in the 2000 century. Values equal or higher than the cutoff are assumed
* to be in the 1900 centry.
*/
public class TwoDigitYear
{
	/**
	* >= cutoff: assume 1900
	* <  cutoff: assume 2000
	*/
	public static int cutoff = 70;

	/**
	* Pass 2 digit year, return 4 digit year.
	* @param yy the integer year
	* @return the complete 4-digit year
	*/
	public static final int getYear( int yy )
	{
		if (yy < cutoff)
			return 2000 + yy;
		else
			return 1900 + yy;
	}

	/**
	* Pass 2 digit year, return 4 digit year.
	* @param yy String year
	* @return integer 4-digit year
	*/
	public static final int getYear( String yy )
	{
		int i = Integer.parseInt(yy);
		return getYear(i);
	}
}
