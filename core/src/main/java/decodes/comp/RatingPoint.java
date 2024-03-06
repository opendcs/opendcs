/*
*  $Id$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:20:59  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2007/06/15 18:44:26  mmaloney
*  Synchronize with USBR.
*
*  Revision 1.4  2004/08/24 14:31:29  mjmaloney
*  Added javadocs
*
*  Revision 1.3  2004/08/11 21:40:58  mjmaloney
*  Improved javadocs
*
*  Revision 1.2  2004/06/24 18:36:06  mjmaloney
*  Preliminary working version.
*
*  Revision 1.1  2004/06/24 14:29:55  mjmaloney
*  Created.
*
*/
package decodes.comp;

/**
 * Holds a single double precision point-pair.
 */
public class RatingPoint implements Comparable<RatingPoint>
{
	/**
	 * The dependent variable
	 */
	public double dep;
	
	/**
	 * The independent variable
	 */
	public double indep;
	
	/**
	 * Each point is allowed to have a status string.
	 */
	public String status;

	/**
	 * Constructs a rating point with i=independent and d=dependent variables.
	* @param i independent value
	* @param d dependent value
	 */
	public RatingPoint( double i, double d )
	{
		dep = d;
		indep = i;
		status = null;
	}

	/**
	  From Comparable interface, used to sort the LookupTable
	  @param o the other point
	  @return 0 if equal, 1 or -1 if not.
	*/
	public int compareTo (RatingPoint rhs)
	{
		if (indep < rhs.indep)
			return -1;
		else if (indep > rhs.indep)
			return 1;
		else 
			return 0;
	}

	@Override
	public String toString()
	{
		return String.format("%f -> %f", indep, dep);
	}
}
