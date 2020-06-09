/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.4  2011/01/27 19:06:38  mmaloney
*  removed debug
*
*  Revision 1.3  2009/09/12 13:31:35  mjmaloney
*  USGS Merge
*
*  Revision 1.2  2009/05/04 16:43:22  mjmaloney
*  dev
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.23  2005/03/15 16:11:26  mjmaloney
*  Modify 'Enum' for Java 5 compat.
*
*  Revision 1.22  2004/08/27 18:41:30  mjmaloney
*  Platwiz work
*
*  Revision 1.21  2004/06/28 18:08:58  mjmaloney
*  Bug fix in data presentation.
*
*  Revision 1.20  2004/04/27 17:15:53  mjmaloney
*  Update to data presentations.
*  Added time zone to transport medium.
*
*  Revision 1.19  2003/11/22 00:34:46  mjmaloney
*  equals() function must take maxDecimals into account.
*
*  Revision 1.18  2003/11/15 19:36:18  mjmaloney
*  maxDecimals moved from RR to data presentation.
*  Implemented new algorithm for rounding rules.
*
*  Revision 1.17  2002/10/31 18:53:29  mjmaloney
*  Added noIdCopy() functions used by dbedit.
*
*  Revision 1.16  2002/09/30 18:54:33  mjmaloney
*  SQL dev.
*
*  Revision 1.15  2002/09/19 17:18:01  mjmaloney
*  SQL dev.
*
*  Revision 1.14  2002/08/26 05:02:59  chris
*  This file has been superceded by the .pjava version.
*
*  Revision 1.1  2002/08/26 04:53:45  chris
*  Major SQL Database I/O development.
*
*  Revision 1.13  2002/06/08 19:22:08  mjmaloney
*  Added defaultNumberFormat with maximum fractional decimals to 3.
*
*  Revision 1.12  2002/04/18 02:05:19  mike
*  Fixed bug in presentation group with negative numbers.
*
*  Revision 1.11  2001/11/09 14:35:11  mike
*  dev
*
*  Revision 1.10  2001/11/07 21:37:59  mike
*  dev
*
*  Revision 1.9  2001/10/02 15:28:53  mike
*  Implemented default DataPresentation
*
*  Revision 1.8  2001/09/27 18:18:55  mike
*  Finished rounding rules & eu conversions.
*
*  Revision 1.7  2001/09/27 00:57:23  mike
*  Work on presentation elements.
*
*  Revision 1.6  2001/09/25 12:58:15  mike
*  PresentationGroup prepareForExec() written.
*
*  Revision 1.5  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.4  2001/04/12 12:30:10  mike
*  dev
*
*  Revision 1.3  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.2  2001/03/17 17:44:27  mike
*  created
*
*  Revision 1.1  2001/03/16 19:53:10  mike
*  Implemented XML parsers for routing specs
*
*/
package decodes.db;

import java.text.NumberFormat;

import ilex.util.TextUtil;

/**
* This encapsulates a DataPresentation.  Each DataPresentation object
* specifies how to present the data of a particular DataType.  Each
* DataPresentation is a member of a PresentationGroup.  Incoming data is
* identified by its DataType and, optionally by the EquipmentModel from
* which it originated.  Once identified, the DataPresentation entity
* specifies what EngineeringUnits to convert the data to, and what
* RoundingRules to apply.
*/
public class DataPresentation extends IdDatabaseObject
{
	// Note: _id stored in IdDatabaseObject superclass

	/** EU abbreviation to convert values to. */
	private String unitsAbbr = null;

	/** Reference to this object's parent PresentationGroup.  */
	private PresentationGroup myGroup = null;

	/** Determines data type that this DP will act upon. */
	private DataType dataType = null;

	/**
	  The maximum number of decimal places, overrides all rouding rules.
	  Initially set to Integer.MAX_VALUE, meaning unlimited.
	*/
	private int maxDecimals = Integer.MAX_VALUE;
	
	/**
	 * DECODES DB V10 allows max & min limits set by datatype in a presentation entry.
	 */
	private double maxValue = Constants.undefinedDouble;

	/**
	 * DECODES DB V10 allows max & min limits set by datatype in a presentation entry.
	 */
	private double minValue = Constants.undefinedDouble;
	
	/// Log base e of 10, used for computing log base 10 of values.
	private static final double LOGe10 = Math.log(10.0);

	private static NumberFormat numberFormat;
	private static NumberFormat defaultNumberFormat;
	static
	{
		numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setGroupingUsed(false);
		defaultNumberFormat = NumberFormat.getNumberInstance();
		defaultNumberFormat.setMaximumFractionDigits(3);
		defaultNumberFormat.setGroupingUsed(false);
	}

	/**
	* Constructor.
	*/
	public DataPresentation()
	{
		super();  // Sets _id to Constants.undefinedId;
	}

	/**
	Constructs an element within a specific group.
	  @param group the group
	*/
	public DataPresentation(PresentationGroup group)
	{
		this();
		setGroup(group);
	}

	/**
	* This overrides the DatabaseObject's method
	@return "DataPresentation".
	*/
	public String getObjectType() {
		return "DataPresentation";
	}

	/**
	  @return maximum number of decimal places to use, overriding any
	  rounding rules or Integer.MAX_VALUE if unlimited.
	*/
	public int getMaxDecimals() 
	{
		return maxDecimals; 
	}

	/**
	  Sets max decimal places to use, overriding any rounding rules.
	  Call with Integer.MAX_VALUE to make it unlimited.
	  @param max the maximum
	*/
	public void setMaxDecimals(int max)
	{
		if (max < 0)
			max = Integer.MAX_VALUE;
		maxDecimals = max;
	}

	/**
	* Compares two DataPresentations, and returns true if they can be
	* considered equal.  Two DataPresentations are equal iff .... (?)
	  @param rhs right-hand-side
	*/
	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof DataPresentation))
			return false;
		DataPresentation dp = (DataPresentation)rhs;
		if (this == dp)
			return true;
		if (!TextUtil.strEqualIgnoreCase(getUnitsAbbr(), dp.getUnitsAbbr()))
			return false;

		if (getDataType() != dp.getDataType())
			return false;

		if (maxDecimals != dp.maxDecimals)
			return false;

		if (maxValue != dp.maxValue)
			return false;
		if (minValue != dp.minValue)
			return false;

		return true;
	}

	/**
	  Return a deep copy of this DataPresentation, and assign it to the passed
	  group.
	  @param pg the presentation group
	  @return a deep copy of this DataPresentation
	*/
	public DataPresentation copy(PresentationGroup pg)
	{
		DataPresentation ret = new DataPresentation(pg);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.

		ret.setGroup(pg);

		ret.setUnitsAbbr(this.getUnitsAbbr());
		ret.setDataType(this.getDataType());
		ret.maxDecimals = this.maxDecimals;
		ret.setMinValue(minValue);
		ret.setMaxValue(maxValue);

		return ret;
	}

	/**
	@return this DataPresentation's parent PresentationGroup.
	*/
	public PresentationGroup getGroup() {
		return myGroup;
	}

	@Override
	public void prepareForExec()
	{
		if (maxDecimals == Integer.MAX_VALUE)
			maxDecimals = 2;
	}

	@Override
	public boolean isPrepared()
	{
		return true;
	}

	/**
	* This overrides the DatabaseObject's read() method.
	* This does nothing, since the I/O for this class is handled by
	* PresentationGroup.
	*/
	public void read()
		throws DatabaseException
	{
	}

	/**
	* This overrides the DatabaseObject's write() method.
	* This does nothing, since the I/O for this class is handled by
	* PresentationGroup.
	*/
	public void write()
		throws DatabaseException
	{
	}

	/**
	  Applies the rounding rules to the passed double value and returns the
	  String representation.
	  @param v the value
	  @return rounded String representation
	*/
	public String applyRoundingRules(double v)
	{
		if (!isPrepared())
			prepareForExec();

		String ret = "";

		ret = doRounding(v, maxDecimals, -1);
		return ret;
	}

	private String doRounding(double v, int maxDecimals, int sigDigits)
	{
		double abs = v < 0 ? -v : v;

		int fd = maxDecimals;
		if (sigDigits != -1)
		{
			int truncLog10 = (int)(Math.log(abs) / LOGe10);
			fd = sigDigits - (truncLog10 + 1);
			if (abs < 1.0)
				fd++;
			if (fd < 0)
				fd = 0;
			else if (fd > maxDecimals)
				fd = maxDecimals;
		}
		numberFormat.setMinimumFractionDigits(fd);
		numberFormat.setMaximumFractionDigits(fd);

		StringBuffer sb = new StringBuffer(numberFormat.format(v));
		int dotPosition = -1;
		for(int i=0; i<sb.length(); i++)
			if (sb.charAt(i) == '.')
			{
				dotPosition = i;
				break;
			}

		// Remove leading zeros
		if (dotPosition == -1) dotPosition = sb.length();
		dotPosition--;
		for(int i=0; i<dotPosition; i++)
			if (sb.charAt(i) == '0')
				sb.setCharAt(i, ' ');
			else
				break;

		return sb.toString();
	}

	/**
	 * @return the unitsAbbr
	 */
	public String getUnitsAbbr()
	{
		return unitsAbbr;
	}

	/**
	 * @param unitsAbbr the unitsAbbr to set
	 */
	public void setUnitsAbbr(String unitsAbbr)
	{
		this.unitsAbbr = unitsAbbr;
	}

	/**
	 * @return the dataType
	 */
	public DataType getDataType()
	{
		return dataType;
	}

	/**
	 * @param dataType the dataType to set
	 */
	public void setDataType(DataType dataType)
	{
		this.dataType = dataType;
	}

	/**
	 * @param myGroup the myGroup to set
	 */
	public void setGroup(PresentationGroup myGroup)
	{
		this.myGroup = myGroup;
	}

	public double getMaxValue()
	{
		return maxValue;
	}

	public void setMaxValue(double maxValue)
	{
		this.maxValue = maxValue;
	}

	public double getMinValue()
	{
		return minValue;
	}

	public void setMinValue(double minValue)
	{
		this.minValue = minValue;
	}
}
