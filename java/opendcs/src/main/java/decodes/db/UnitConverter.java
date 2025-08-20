/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.9  2004/08/27 12:23:12  mjmaloney
*  Added javadocs
*
*  Revision 1.8  2003/03/06 18:17:12  mjmaloney
*  Fixed DR 115 - Don't add raw converters to exec list. They are not general
*  purpose converters and cannot be used for composite conversions.
*
*  Revision 1.7  2002/08/26 04:53:46  chris
*  Major SQL Database I/O development.
*
*  Revision 1.6  2001/09/27 00:57:24  mike
*  Work on presentation elements.
*
*  Revision 1.5  2001/06/25 18:15:37  mike
*  First working version of EmitImport
*
*  Revision 1.4  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*/

package decodes.db;

import decodes.util.DecodesException;

/**
 * This is the common base class for all unit converters.
 * <p>
 *   The decoder will use the execClassName defined in the database to
 *   instantiate unit converters dynamically.
 * </p>
 */
public abstract class UnitConverter
{
	/** The EU that this converts FROM */
	protected EngineeringUnit from;

	/** The EU that this converts TO */
	protected EngineeringUnit to;


  	/** 
	  Constructor.  
	  @param from the EU that this converts from
	  @param to the EU that this converts to
	*/
	public UnitConverter(EngineeringUnit from, EngineeringUnit to)
	{
		this.from = from;
		this.to = to;
	}

  	/**
   	  Converts the input according to the type of conversion and the
   	  specified coefficients.
	  @param value the value to convert
   	*/
	public abstract double convert(double value)
		throws DecodesException;

  	/**
	  Sets the coefficients for this conversion.
	  The database defines up to six coefficients which are passed here
	  as an array of doubles. Not all coefficients are needed by all
	  converters.
	  @param coeff the coefficients
	*/
	public abstract void setCoefficients(double[] coeff);

  	/**
   	  Weights are used to select among multiple converters in cases where
   	  there is more than one way to get from one EU to another. The
   	  converter with the lowest weight will be selected.
   	  @return the weight of this conversion relative to a linear conversion.
   	*/
	public abstract double getWeight();

  	/**
   	* @return the 'from' EU.
   	*/
	public EngineeringUnit getFrom()
	{
		return from;
	}

  	/**
   	* @return the 'to' EU.
   	*/
	public EngineeringUnit getTo()
	{
		return to;
	}

  	/**
   	* @return the 'from' abbreviation.
   	*/
	public String getFromAbbr()
	{
		return (from == null) ? "unknown" : from.abbr;
	}

  	/**
   	* @return the 'to' abbreviation.
   	*/
	public String getToAbbr()
	{
		return (to == null) ? "unknown" : to.abbr;
	}

  	/**
   	  Set the "to" units.
	  @param to the EU
   	*/
	public void setTo(EngineeringUnit to)
	{
		this.to = to;
	}
}

