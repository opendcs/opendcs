package decodes.comp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;

/**
 * Encapsulates a single table used for looking up a dependent point
 * from an independent point.
 * Supports several types of lookup algorithms. See the INTERP constants.
 */
public class LookupTable
    implements HasLookupTable
{
    public static boolean debug = false;

    /**
     * If independent is below lowest table point and this is true,
     * Do lookup as follows:
     * INTERP_LINEAR: Extend line between 2 bottom table points & interpolate.
     * INTERP_TRUNC: Truncate to bottom table point.
     * INTERP_LOG: Sim to LINEAR, but extend logarithmic curve.
     * INTERP_ROUND: Truncate to bottom table point.
     * If this is false, throw exception on lookup.
     */
    private boolean exceedLowerBound;

    /**
     * If independent is above highest table point and this is true,
     * Do lookup as follows:
     * INTERP_LINEAR: Extend line between 2 top  table points & interpolate.
     * INTERP_TRUNC: Truncate to top table point.
     * INTERP_LOG: Sim to LINEAR, but extend logarithmic curve.
     * INTERP_ROUND: Truncate to top table point.
     * If this is false, throw exception on lookup.
     */
    private boolean exceedUpperBound;

    /**
     * Do linear interpolation when no exact match.
     */
    public static final int INTERP_LINEAR = 1;

    /**
     * Do logarithmic interpolation between independent points.
     */
    public static final int INTERP_LOG = 3;

    /**
     * Round to neares independent point, then return matching dependent.
     */
    public static final int INTERP_ROUND = 4;

    /**
     * Truncate to lower independent value in table.
     */
    public static final int INTERP_TRUNC = 2;

    /**
     * Holds one of the INTERP constants.
     */
    private int lookupType;

    /**
     * ArrayList of RatingPoint objects used for the lookup.
     */
    private ArrayList<RatingPoint> points;

    /**
     * Used (if needed) for logarithmic interpolation
    */
    private LogInterp logInterp;

    /**
    * Logarithmic interpolations require an X offset (defaults to 0 if not set.)
    */
    private double xOffset;

    /**
    * Saves last table index found. Used to detect if the same logarithmic
    * interpolator can be re-used.
    */
    private int lastTableIdx = -1;

    private boolean sorted = false;

    /**
     * Constructs a new empty RatingTable.
     */
    public LookupTable( )
    {
        exceedLowerBound = false;
        exceedUpperBound = false;
        lookupType = INTERP_LINEAR;
        points = new ArrayList<>();
        xOffset = 0.0;
    }

    /**
     * Adds a point to the table.
     * @param indep independent variable
     * @param dep dependent variable
     */
    public void add( double indep, double dep )
    {
        points.add( new RatingPoint(indep, dep) );
        sorted = false;
    }

    /**
     * Passed an independent value, returns the corresponding dependent value.
     * @param indep the independent value
     * @throws TableBoundsException if passed value is out of the table
     * range and lookup type is such that interpolation is not possible.
     * @return dependent value
     */
    public double lookup( double indep ) throws TableBoundsException
    {
        int sz = points.size();
        if (sz == 0)
        {
            throw new TableBoundsException("Attempted lookup on empty table.");
        }
        if (!sorted)
        {
            sort();
        }

        // BinarySearch finds exact match or the insertion point.
        int idx = Collections.binarySearch(points, new RatingPoint(indep, 0));

//Logger.instance().debug3("binary search returned idx=" + idx + " for indep=" + indep);
        if (idx >= 0) // exact match found.
        {
            return ((RatingPoint)points.get(idx)).dep;
        }

        idx = -(idx + 1);  // Convert negative index into insertion point.
//Logger.instance().debug3("after convert negative idx=" + idx);
        if (idx == 0 && !exceedLowerBound)
        {
            throw new TableBoundsException("Value " + indep
                + " below all table values.");
        }
        else if (idx >= sz && !exceedUpperBound)
        {
            throw new TableBoundsException("Value " + indep
                + " above all table values.");
        }

        // Insertion point is 1st value > then indep, backup 1 to get lower.
        if (idx > 0)
        {
            idx--;
        }
//Logger.instance().debug3("table point below indep is idx=" + idx);

        if (lookupType == INTERP_TRUNC)
        {
            if (idx >= sz)
            {
                idx = sz - 1;
            }
            return ((RatingPoint)points.get(idx)).dep;
        }
        else if (lookupType == INTERP_ROUND)
        {
            if (idx >= sz - 1)
            {
                return ((RatingPoint)points.get(sz - 1)).dep;
            }

            RatingPoint p0 = (RatingPoint)points.get(idx);
            RatingPoint p1 = (RatingPoint)points.get(idx+1);

            if (indep - p0.indep < p1.indep - indep)
            {
                return p0.dep;
            }
            else
            {
                return p1.dep;
            }
        }
        else if (lookupType == INTERP_LINEAR)
        {
            if (sz == 1)
            {
                throw new TableBoundsException(
                    "Cannot interpolate on table with a single value.");
            }

            RatingPoint p0, p1;
            if (idx >= sz - 1)
            {
                idx = sz - 2;
            }
            p0 = (RatingPoint)points.get(idx);
            p1 = (RatingPoint)points.get(idx + 1);
//System.out.println("Inter lower point " + idx + ": (" + p0.indep + "," + p0.dep + ")");
//System.out.println("Inter upper point " + (idx+1) + ": (" + p1.indep + "," + p1.dep + ")");

            double ifactor = (indep - p0.indep) / (p1.indep - p0.indep);
            double range = p1.dep - p0.dep;
//System.out.println("ifactor=" + ifactor + ", range=" + range);
            return p0.dep + ifactor * range;
        }
        else if (lookupType == INTERP_LOG)
        {
//Logger.instance().debug3("interp idx=" + idx + ", lastIdx=" + lastTableIdx);
            if (logInterp == null || idx != lastTableIdx)
            {
                lastTableIdx = idx;
                RatingPoint p0, p1;
                if (idx >= sz - 1)
                {
                    idx--;
                }
                p0 = (RatingPoint)points.get(idx);
                p1 = (RatingPoint)points.get(idx + 1);

                logInterp = new LogInterp(p0.indep, p0.dep, p1.indep, p1.dep, xOffset);
            }
            return logInterp.getY(indep);
        }
        else
        {
            throw new TableBoundsException("Unknown lookup algorithm (" + lookupType + ")");
        }
    }

    /**
     * Sets the flag allowing lookup of points below table range to succeed.
     * @param yn the flag
     */
    public void setExceedLowerBound( boolean yn )
    {
        exceedLowerBound = yn;
    }

    /**
     * Sets the flag allowing lookup of points above table range to succeed.
     * @param yn the flag
     */
    public void setExceedUpperBound( boolean yn )
    {
        exceedUpperBound = yn;
    }

    /**
     * Sets lookup type to one of the INTERP constants defined in this class.
     * @param t one of the constants define in this class
     */
    public void setLookupType( int t )
    {
        lookupType = t;
    }

    /**
     * Sets the X Offset used for log interpolation.
     * @param xo the x offset
    */
    public void setXOffset(double xo)
    {
        xOffset = xo;
    }

    /**
     * Sorts elements in the table. Should be called once prior
     * to any lookups.
     */
    public void sort( )
    {
        Collections.sort(points);
        sorted = true;
    }

    /** Test method to dump table to stdout */
    public void dump()
    {
        for(Iterator<RatingPoint> it = points.iterator(); it.hasNext(); )
        {
            RatingPoint p = it.next();
            System.out.println("\t" + p.indep + "\t" + p.dep);
        }
    }

    public double getMinInput()
    {
        if (!sorted)
        {
            sort();
        }
        if (points.size() == 0)
        {
            return 0.0;
        }
        RatingPoint rp = points.get(0);
        return rp.indep;
    }

    public int size() { return points.size(); }

    public double getMaxInput()
    {
        if (!sorted)
        {
            sort();
        }
        if (points.size() == 0)
        {
            return 0.0;
        }
        RatingPoint rp = points.get(points.size() - 1);
        return rp.indep;
    }

    public void clear()
    {
        points.clear();
    }

    @Override
    public void setProperty(String name, String value)
    {
    }

    @Override
    public void addPoint(double indep, double dep)
    {
        add(indep, dep);
    }

    @Override
    public void addShift(double indep, double shift)
    {
    }

    @Override
    public void setBeginTime(Date bt)
    {
    }

    @Override
    public void setEndTime(Date et)
    {
    }

    @Override
    public void clearTable()
    {
        points.clear();
    }
}
