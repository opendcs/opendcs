/*
*  $Id$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.14  2004/08/27 12:23:12  mjmaloney
*  Added javadocs
*
*  Revision 1.13  2004/04/09 19:03:36  mjmaloney
*  Added method to retrieve coefficients as strings.
*
*  Revision 1.12  2002/09/22 18:39:54  mjmaloney
*  SQL Dev.
*
*  Revision 1.11  2002/09/08 19:43:22  mjmaloney
*  Updates for 5.2
*
*  Revision 1.10  2002/09/01 20:55:07  mjmaloney
*  dev
*
*  Revision 1.2  2002/08/28 13:11:36  chris
*  SQL database I/O development.
*
*  Revision 1.1  2002/08/26 04:53:46  chris
*  Major SQL Database I/O development.
*
*  Revision 1.8  2002/07/15 21:56:20  chris
*  Removed EnumValue algorithmEnum.
*
*  Revision 1.7  2001/11/24 18:29:10  mike
*  First working DbImport!
*
*  Revision 1.6  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.5  2001/08/12 15:50:52  mike
*  dev
*
*  Revision 1.4  2001/04/12 12:30:50  mike
*  dev
*
*  Revision 1.3  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.2  2001/01/20 02:54:00  mike
*  dev
*
*  Revision 1.1  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*/
package decodes.db;
import decodes.util.DecodesException;

/**
 * This is the class for unit converters that are defined in the database.
 * When the database is prepared for execution, this class will find or
 * construct a Executable Converter to do the actual conversion.
 */
public class UnitConverterDb extends IdDatabaseObject
{
	// _id is stored in the IdDatabaseObject superclass.

	/** Name of units to convert FROM - "raw" means convert raw sample. */
	public String fromAbbr;

	/** Name of units to convert TO. */
	public String toAbbr;

	/** Algorithm to use -- see EU Algorithm Enum. */
	public String algorithm;

	/**
	* This array stores up to MAX_COEFFICIENTS (six) coefficients for use
	* with certain
	* conversion algorithms.  These are labeled a - f.  If a coefficient
	* is not used for a particular algorithm, its value here will be
	* Constants.undefinedDouble.
	*/
	public double coefficients[];
	public static final int MAX_COEFFICIENTS = 6;

		// links
		//public EnumValue algorithmEnum;
	public UnitConverter execConverter;

	/**
	  Constructs record with given from and to abbreviations.
	  @param from the from abbreviation
	  @param to the to abbreviation
	*/
	public UnitConverterDb(String from, String to)
	{
		super(); // sets _id to Constants.undefinedId;

		fromAbbr = from;
		toAbbr = to;
		algorithm = null;
		coefficients = new double[MAX_COEFFICIENTS];
		for(int i = 0; i<MAX_COEFFICIENTS; i++)
			coefficients[i] = Constants.undefinedDouble;
		//algorithmEnum = null;
		execConverter = null;
	}

	/** @return "UnitConverterDb" */
	public String getObjectType() { return "UnitConverterDb"; }

	/**
	* Prepares this object for execution by the decoding engine.
	* Constructs an executable unit converter according to the settings
	* contained in this database object. The executable can then be retrieved
	* via getExecConverter();
	* @throws InvalidDatabaseException if information in this object
	* is invalid or inconsistent.
	*/
	public void prepareForExec()
				throws InvalidDatabaseException
	{
		EngineeringUnit from = EngineeringUnit.getEngineeringUnit(fromAbbr);
		EngineeringUnit to = EngineeringUnit.getEngineeringUnit(toAbbr);
		if (algorithm.equalsIgnoreCase(Constants.eucvt_none))
			execConverter = new NullConverter(from, to);
		else if (algorithm.equalsIgnoreCase(Constants.eucvt_linear))
			execConverter = new LinearConverter(from, to);
		else if (algorithm.equalsIgnoreCase(Constants.eucvt_usgsstd))
			execConverter = new UsgsStdConverter(from, to);
		else if (algorithm.equalsIgnoreCase(Constants.eucvt_poly5))
			execConverter = new Poly5Converter(from, to);
		else
		{
			// Possibly some user-custom converter. Try to use an enum
			// to construct the class dynamically.
			// TBD
		}
		execConverter.setCoefficients(coefficients);
	}

	/**
	* @return true if this object is ready for execution.
	*/
	public boolean isPrepared()
	{
		return execConverter != null;
	}

	/**
	* Validates this database object.
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
	}

	/**
	* Reads this object from the database.
	* Do nothing for UnitConverters, these are read inside higher-level
	* objects.
	*/
	public void read() {}

	/**
	* Writes this object back to the database.
	* Do nothing for UnitConverters, these are written inside higher-level
	* objects.
	*/
	public void write() {}

	public boolean equals(Object rhs)
	{
		if (!(rhs instanceof UnitConverterDb))
			return false;
		UnitConverterDb ruc = (UnitConverterDb)rhs;
		if (!fromAbbr.equals(ruc.fromAbbr)
		 || !toAbbr.equals(ruc.toAbbr)
		 || !algorithm.equals(ruc.algorithm))
			return false;
		for(int i = 0; i<MAX_COEFFICIENTS; i++)
			if (coefficients[i] != ruc.coefficients[i])
				return false;
		return true;
	}

	/**
	  @return String of the form  from_abbr-&gt;to_abbr
	*/
	public String toString()
	{
		return fromAbbr + "->" + toAbbr;
	}

	/**
	  @return a deep copy of this object
	*/
	public UnitConverterDb copy()
	{
		UnitConverterDb ret = new UnitConverterDb(fromAbbr, toAbbr);
		try { ret.setId(getId()); }
		catch(DatabaseException ex) {} // won't happen.

		ret.algorithm = algorithm;

		for(int i = 0; i<MAX_COEFFICIENTS; i++)
			ret.coefficients[i] = coefficients[i];

		// links
		//public EnumValue algorithmEnum;

		ret.execConverter = execConverter;

		return ret;
	}

	/** 
	  Gets specified coefficient as a string.
	  @param coefnum the coefficient number
	  @return string representation, or "" if undefined.
	*/
	public String getCoeffString(int coefnum)
	{
		if (coefnum < 0 || coefnum >= MAX_COEFFICIENTS)
			return "";
		if (coefficients[coefnum] == Constants.undefinedDouble)
			return "";
		else
			return "" + coefficients[coefnum];
	}
}
