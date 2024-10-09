/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.3  2007/12/11 01:05:16  mmaloney
*  javadoc cleanup
*
*  Revision 1.2  2004/08/26 13:29:25  mjmaloney
*  Added javadocs
*
*  Revision 1.1  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*/
package decodes.db;

/**
Null engineering unit converter.
Used when a sensor reports raw values that are already converted to some
unit type.
Also used to 'convert' between two units which are really synonyms 
(e.g. cc to ml).
*/
public class NullConverter extends UnitConverter
{
	/**
	  Constructor.
	  @param from the EU we're converting from.
	  @param to the EU we're converting to.
	*/
	public NullConverter(EngineeringUnit from, EngineeringUnit to)
	{
		super(from, to);
	}

	/**
	  Does nothing for null converter but necessary for complete implementation.
	  @param coeff ignored
	*/
	public void setCoefficients(double[] coeff)
	{
	}

	/**
	  Returns the input directly.
	  @param value the value returned
	  @return the passed value
	*/
	public double convert(double value)
	{
		return value;
	}

	/**
	  Returns weight for this algorithm relative to a linear conversion.
	  We arbitrarily pick .5 here.
	*/
	public double getWeight()
	{
		return 0.5;
	}

	/**
	  Like linear conversion, null conversion is also invertable.
	  @return NullConverter to go from 'to' to 'from'.
	*/
	public NullConverter makeInverse()
	{
		return new NullConverter(getTo(), getFrom());
	}
}
