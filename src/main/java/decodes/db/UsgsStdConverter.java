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
*  Revision 1.5  2004/08/27 12:23:13  mjmaloney
*  Added javadocs
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
USGS 4-coefficient equation engineering unit converter.
Implements the equation:
	Y = A * (B + X)^^C + D
*/
public class UsgsStdConverter extends UnitConverter
{
	private double a, b, c, d;

	/**
	  Constructor
	  @param from the EU we're converting FROM
	  @param to the EU we're converting TO
	*/
	public UsgsStdConverter(EngineeringUnit from, EngineeringUnit to)
	{
		super(from, to);
		a=1.0;
		b=0.0;
		c=1.0;
		d=0.0;
	}

	/**
	  Sets coefficients from array.
	  The first for values in the array correspond to A, B, C, D.
	  @param coeff the coefficients.
	*/
	public void setCoefficients(double[] coeff)
	{
		if (coeff[0] != Constants.undefinedDouble)
			a = coeff[0];
		if (coeff[1] != Constants.undefinedDouble)
			b = coeff[1];
		if (coeff[2] != Constants.undefinedDouble)
			c = coeff[2];
		if (coeff[3] != Constants.undefinedDouble)
			d = coeff[3];

		if (a == Constants.undefinedDouble)
			a = 1.0;
		if (b == Constants.undefinedDouble)
			b = 0.0;
		if (c == Constants.undefinedDouble)
			c = 1.0;
		if (d == Constants.undefinedDouble)
			d = 0.0;
	}

	/**
	  Performs the conversions.
	  @return the result.
	*/
	public double convert(double x)
	{
		return a * Math.pow(b + x, c) + d;
	}

	/**
	  @return weight compared to linear
	*/
	public double getWeight()
	{
		return 3.0; // guess
	}
	
	public String toString()
	{
		return "USGS Converter a=" + a + ", b=" + b + ", c=" + c + ", d=" + d;
	}
}
