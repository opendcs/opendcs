/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2009/05/04 16:43:22  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.6  2004/08/27 12:23:10  mjmaloney
*  Added javadocs
*
*  Revision 1.5  2003/11/15 19:36:18  mjmaloney
*  maxDecimals moved from RR to data presentation.
*  Implemented new algorithm for rounding rules.
*
*  Revision 1.4  2002/08/26 04:53:46  chris
*  Major SQL Database I/O development.
*
*  Revision 1.3  2001/11/07 21:37:59  mike
*  dev
*
*  Revision 1.2  2001/09/25 12:58:15  mike
*  PresentationGroup prepareForExec() written.
*
*  Revision 1.1  2001/03/16 19:53:10  mike
*  Implemented XML parsers for routing specs
*
*/

package decodes.db;

import java.util.Vector;
import decodes.decoder.*;

/**
A rule for rounding data values within a range.
*/
public class RoundingRule
	implements Comparable
{
	/**
	  The upper limit for which this rounding rule applies.  It is possible
	  that there is no upper limit, in which case this has the value
	  Constants.undefinedDouble.
	*/
    private double upperLimit = Constants.undefinedDouble;

	/**
	  The number of significant digits when this RoundingRule is in affect.
	*/
	public int sigDigits = 0;

	public String toString() 
	{
		return "upperLimit=" + getUpperLimit() + ", sigDigits=" + sigDigits;
	}
	
	/**
	  This is a reference to the DataPresentation to which this
	  RoundingRule belongs.
	*/
	private DataPresentation parent;

	/** Constructor. */
	public RoundingRule()
	{
		parent = null;
	}

	/**
	Construct with a reference to this new object's parent.
	  @param parent the owning DP
	*/
	public RoundingRule(DataPresentation parent)
	{
		this();
		this.parent = parent;
	}

	/**
	  This compares two RoundingRules, and facilitates sorting by the
	  upper limit value.
	  @param o the object to compare this to.
	  @return -1, 0, or 1
	*/
	public int compareTo(Object o)
		throws ClassCastException
	{
		RoundingRule rhs = (RoundingRule) o;
		double r = getUpperLimit() - rhs.getUpperLimit();
		return r < 0.0 ? -1 : r > 0.0 ? 1 : 0;
	}

	/**
	  This returns true if two RoundingRules can be considered equal.
	  Two RoundingRules are equal if each of their members are equal.
	*/
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof RoundingRule))
			return false;
		RoundingRule rr = (RoundingRule)rhs;
		if (this == rr)
			return true;
		if (compareTo(rr) != 0)
			return false;
		if (sigDigits != rr.sigDigits)
			return false;
		return true;
	}

	/**
	  Get a reference to the DataPresentation to which this belongs.
	*/
    public DataPresentation getParent() 
	{
        return parent;
    }

	/**
	 * @return the upperLimit
	 */
	public double getUpperLimit()
	{
		return upperLimit;
	}

	/**
	 * @param upperLimit the upperLimit to set
	 */
	public void setUpperLimit(double upperLimit)
	{
		this.upperLimit = upperLimit;
	}
}
