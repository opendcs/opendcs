/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2011/06/16 13:30:43  mmaloney
*  fix
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.5  2004/08/27 12:23:09  mjmaloney
*  Added javadocs
*
*  Revision 1.4  2002/09/08 19:43:22  mjmaloney
*  Updates for 5.2
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
5th order polynomial engineering unit converter.
*/
public class Poly5Converter extends UnitConverter
{
	/** coefficients */
	private double coeff[];

	/**
	  Constructor.
	  @param from the EU we're converting from
	  @param to the EU we're converting to
	*/
	public Poly5Converter(EngineeringUnit from, EngineeringUnit to)
	{
		super(from, to);
		coeff = new double[6];
		for(int i=0; i<6; i++)
			coeff[i] = 0.0;
	}

	/**
	  Sets the coeffients.
	  @param coeff the coefficients.
	*/
	public void setCoefficients(double[] coeff)
	{
		for(int i=0; i<6; i++)
			if (coeff[i] != Constants.undefinedDouble)
				this.coeff[i] = coeff[i];
	}

	/**
	  Does the conversion and returns the result.
	  @param x independent X value
	  @return dependent Y value
	*/
	public double convert(double x)
	{
		double ret = coeff[5];  // x^0
		double Xm = x;
		ret += (Xm * coeff[4]);  // x^1
		Xm *= x;
		ret += (Xm * coeff[3]);  // x^2
		Xm *= x;
		ret += (Xm * coeff[2]);  // x^3
		Xm *= x;
		ret += (Xm * coeff[1]);  // x^4
		Xm *= x;
		ret += (Xm * coeff[0]);  // x^5

		return ret;
	}

	/** @return the weight relative to a linear conversion (guess). */
	public double getWeight()
	{
		return 3.0;
	}
}
