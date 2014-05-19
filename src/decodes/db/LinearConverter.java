/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.8  2007/12/11 01:05:16  mmaloney
*  javadoc cleanup
*
*  Revision 1.7  2004/08/26 13:29:23  mjmaloney
*  Added javadocs
*
*  Revision 1.6  2001/09/27 18:18:55  mike
*  Finished rounding rules & eu conversions.
*
*  Revision 1.5  2001/09/27 00:57:23  mike
*  Work on presentation elements.
*
*  Revision 1.4  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.3  2001/01/13 17:22:46  mike
*  Added parsers for EngineeringUnits
*
*  Revision 1.2  2001/01/12 21:56:25  mike
*  Renamed UnitConverterBase to UnitConverter
*
*  Revision 1.1  2001/01/12 15:38:20  mike
*  dev
*
*
*/
package decodes.db;

/**
Linear engineering unit converter.
*/
public class LinearConverter extends UnitConverter
{
	/** the Y intercept */
	private double b;

	/** the slope */
	private double m;

	/**
	  Construct linear converter.
	  @param from the EU we are converting from
	  @param to the EU we are converting to
	*/
	public LinearConverter(EngineeringUnit from, EngineeringUnit to)
	{
		super(from, to);
		m = b = 0.0;
	}

	/**
	  Sets the coeffients. coeff[0] is m, coeff[1] is b.
	  @param coeff array of coeffients.
	*/
	public void setCoefficients(double[] coeff)
	{
		m = coeff[0];
		b = coeff[1];
		if (m == Constants.undefinedDouble)
			m = 1.0;
		if (b == Constants.undefinedDouble)
			b = 0.0;
	}

	/**
	  Does the conversion.
	  @param value the input (x) value
	  @return the output (y) value
	*/
	public double convert(double value)
	{
		double ret = m * value + b;
		return ret;
	}

	/**
	  Weights are used to select conversions when there are more than two
	  ways to get there.
	  @return 1.0, All other conversions are weighted relative to a linear
	  conversion.
	*/
	public double getWeight()
	{
		return 1.0;
	}

	/**
	  Makes a new converter that is the inverse of this linear converter.
	  @return new converter.
	  @throws NoConversionException if the slope of the line is 0.
	*/
	public LinearConverter makeInverse()
		throws NoConversionException
	{
		if (m == 0.0)
			throw new NoConversionException(
				"Cannot invert linear conversion with slope=0");
		LinearConverter ret = new LinearConverter(getTo(), getFrom());
		ret.m = 1.0/m;
		ret.b = -1.0 * b/m;
		return ret;
	}
}
