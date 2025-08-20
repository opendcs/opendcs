/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2010/02/06 17:32:01  mjmaloney
*  If no name defined, getName() returns the abbreviation.
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.13  2007/08/07 11:52:54  mmaloney
*  dev
*
*  Revision 1.12  2007/08/01 17:31:53  mmaloney
*  dev
*
*  Revision 1.11  2007/07/23 13:13:32  mmaloney
*  dev
*
*  Revision 1.10  2004/08/26 13:29:21  mjmaloney
*  Added javadocs
*
*  Revision 1.9  2002/09/22 18:39:54  mjmaloney
*  SQL Dev.
*
*  Revision 1.8  2002/06/15 23:32:52  chris
*  Added javadoc
*
*  Revision 1.7  2001/11/23 21:18:22  mike
*  dev
*
*  Revision 1.6  2001/09/14 21:18:15  mike
*  dev
*
*  Revision 1.5  2001/06/30 13:37:21  mike
*  dev
*
*  Revision 1.4  2001/03/23 20:22:53  mike
*  Collection classes are no longer static monostate. Access them through
*  the current database (Database.getDb().collectionName)
*
*  Revision 1.3  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*  Revision 1.2  2001/01/12 15:38:20  mike
*  dev
*
*  Revision 1.1  2000/12/29 02:42:52  mike
*  Created.
*
*
*/

package decodes.db;

import ilex.util.TextUtil;

/**
 * This class encapsulates information about an engineering unit.
 * Every EngineeringUnit has one abbreviation which is unique among all
 * the EngineeringUnits.  Also, each one can have zero or more names.
 * If an EngineeringUnit has any names, the last one set will be the
 * "primary" name, and all others will be non-primary names.
 */
public class EngineeringUnit
{
	/**
	 * Used by database when reading lists from SQL.
	 * @param abbr
	 * @param name
	 * @param family
	 * @param measures
	 */
	public EngineeringUnit(String abbr, String name, String family,
		String measures)
	{
		super();
		this.abbr = abbr;
		this.name = name;
		this.family = family;
		this.measures = measures;
	}

	/** the abbreviation */
	public String abbr;

	/**
	 * the full name
	 */
	private String name;
	/** the family (eg English, Metric) */
	public String family;

	/** the physical quantity measured (eg length, force) */
	public String measures;

	boolean cnvtSearched;  // Used during search for composite conversion.

	/**
	 * Private constructor.
	 * Users must call the static getEngineeringUnit method to
	 * instantiate a new EngineeringUnit object. 
	 * This enforces the constraint that the abbreviation must be unique.
	 * @param abbr the abbreviation
	 */
	private EngineeringUnit(String abbr)
	{
		  this.abbr = abbr;
		  name = null;
		  family = null;
		  measures = null;
		  cnvtSearched = false;
	}

	/**
	 * Get an existing EngineeringUnit by abbreviation, or, if none exists,
	 * create a new one.
	 * @param abbr The abbreviation
	 * @return EngineeringUnit with this abbr.
	 */
	public static synchronized EngineeringUnit getEngineeringUnit(String abbr)
	{
		if (abbr == null)
			return null;
		Database db = Database.getDb();
		
		EngineeringUnit ret = null;
		if (db != null)
			ret = db.engineeringUnitList.get(abbr);
		if (ret != null)
			return ret;

		ret = new EngineeringUnit(abbr);
		if (db != null)
			db.engineeringUnitList.add(ret);
		return ret;
	}

	/**
	 * Get the primary name.
	 * @return full name
	 */
	public String getName() 
	{
		if (name != null)
			return name;
		return abbr;
	}

	/**
	 * Set the full name.  
	 * If this object already had a primary name, then that name is no
	 * longer primary.  This implies that when setting a list of names 
	 * for an EngineeringUnit, you must set the primary name last.
	 * @param nm the name
	 */
	public void setName(String nm) 
	{
		name = nm; 
		Database db = Database.getDb();
		if (db != null)
			db.engineeringUnitList.addName(this);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(abbr);
		if (name != null)
		{
			sb.append('(');
			sb.append(name);
			sb.append(')');
		}
		return sb.toString();
	}

	public boolean equals(Object ob)
	{
		if (!(ob instanceof EngineeringUnit))
			return false;
		if (ob == this)
			return true;

		EngineeringUnit eu = (EngineeringUnit)ob;
		return
			TextUtil.strEqual(abbr, eu.abbr)
		 && TextUtil.strEqual(name, eu.name)
		 && TextUtil.strEqual(family, eu.family)
		 && TextUtil.strEqual(measures, eu.measures);
	}

	/** @return the abbreviation. */
	public String getAbbr() { return abbr; }
	
	/** @return the family */
	public String getFamily() { return family; }

	/** 
	 * Sets the family 
	 * @param family one of 'metric', 'english', or 'standard'.
	*/
	public void setFamily(String family) { this.family = family; }

	/** @return measures */
	public String getMeasures() { return measures; }

	/** 
	 * Sets measures
	 * @param measures specifies the physical attribute being measured,
	 *  e.g. volume, length
	 */
	public void setMeasures(String measures) { this.measures = measures; }
}

