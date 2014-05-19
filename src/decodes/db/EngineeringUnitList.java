/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.2  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.14  2007/12/11 01:05:16  mmaloney
*  javadoc cleanup
*
*  Revision 1.13  2004/08/26 13:29:21  mjmaloney
*  Added javadocs
*
*  Revision 1.12  2004/04/02 15:51:10  mjmaloney
*  Added remove function to EU list, as required for ref list editor.
*
*  Revision 1.11  2002/09/13 14:16:47  mjmaloney
*  Bug fixes.
*
*  Revision 1.10  2002/07/15 21:21:47  chris
*  Added javadoc comments only (no substantive changes).
*
*  Revision 1.9  2002/06/15 23:32:52  chris
*  Added javadoc
*
*  Revision 1.8  2001/06/30 13:37:21  mike
*  dev
*
*  Revision 1.7  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.6  2001/04/12 12:30:16  mike
*  dev
*
*  Revision 1.5  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.4  2001/03/23 20:09:25  mike
*  Collection classes are no longer monostate static collections.
*
*  Revision 1.3  2001/03/20 03:43:24  mike
*  Implement final parsers
*
*  Revision 1.2  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*  Revision 1.1  2001/01/12 15:38:20  mike
*  dev
*
*
*/
package decodes.db;

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class holds a collection of all known engineering units.
 * It provides access methods to search by abbreviation and/or by name.
 * <p>
 *   Engineering unit abbreviations and names are both case-insensitive.
 *   They are always converted to uppercase before they are stored.
 * </p>
 */
public class EngineeringUnitList extends DatabaseObject
{
	/**
	* Stores references to the EngineeringUnit objects by abbreviation.
	* The hash keys are the abbreviations of the units, in uppercase.
	* Every EngineeringUnit known is in this list exactly once.
	*/
	private HashMap<String, EngineeringUnit> knownUnits
		= new HashMap<String, EngineeringUnit>();

	/**
	* Stores references to the EngineeringUnit objects by name.
	* The hash keys are the names of the Units, in uppercase.
	* An EngineeringUnit may be in this list multiple times, with
	* different names.
	*/
	private HashMap<String, EngineeringUnit> knownUnitNames
		= new HashMap<String, EngineeringUnit>();
	
	/**
	 * Default constructor.
	 */
	public EngineeringUnitList()
	{
	}

    /**
	* @return "EngineeringUnitList".
	* This overrides the DatabaseObject::getObjectType() method.
	*/
	public String getObjectType() { return "EngineeringUnitList"; }

	/**
	* Searches the set for a match and returns it (null if not found).
	* Searches first for a matching abbreviation. If there is none,
	* search for a matching name.
	* @param abbr the abbreviation.
	* @return EngineeringUnit with this abbr
	*/
	public EngineeringUnit get(String abbr)
	{
		EngineeringUnit ret = getByAbbr(abbr);
		if (ret == null)
			ret = getByName(abbr);
		return ret;
	}

    /**
	* Get an EngineeringUnit by abbreviation.
	* This does a case-insensitive search; and returns null if not found.
	* @param abbr the abbreviation.
	* @return EngineeringUnit with this abbr
	*/
	public EngineeringUnit getByAbbr(String abbr)
	{
		return knownUnits.get(abbr.toUpperCase());
	}

    /**
	* Get an EngineeringUnit by name.
	* This does a case-insensitive search; and returns null if not found.
	* @param name the name.
	* @return EngineeringUnit with this name
	*/
	public EngineeringUnit getByName(String name)
	{
		return knownUnitNames.get(name.toUpperCase());
	}

	/**
	* Adds the passed Engineering Unit to the set.
	* This is called only from the EngineeringUnit.getEU method, hence
	* it is not public.  The getEU method guarantees that each EU
	* abbreviation is unique within a given database.
	* @param eu The engineering unit to add.
	*/
	public void add(EngineeringUnit eu)
	{
		knownUnits.put(eu.abbr.toUpperCase(), eu);
		if (eu.getName() != null)
			knownUnitNames.put(eu.getName().toUpperCase(), eu);
	}

    /**
	* Adds the EngineeringUnit to the table of objects by name.
	* This is called only by the EngineeringUnit.setName() method, hence
	* it is not public.  This EngineeringUnit is guaranteed to already
	* be in the list of abbreviations.
	* @param eu The engineering unit to add.
	*/
	void addName(EngineeringUnit eu)
	{
		knownUnitNames.put(eu.getName().toUpperCase(), eu);
	}

	/**
	Removes an engineering unit from the collection.
	* @param eu The engineering unit to remove.
	@return true if one was removed, false if not found.
	*/
	public void remove(EngineeringUnit eu)
	{
		knownUnits.remove(eu.abbr.toUpperCase());
		knownUnitNames.remove(eu.getName().toUpperCase());
	}

	/**
	* @return an iterator for this list of EngineeringUnits.
	*/
	public Iterator<EngineeringUnit> iterator()
	{
		return knownUnits.values().iterator();
	}

    /**
	* @return the number of EngineeringUnits in the list.
	*/
	public int size()
	{
		return knownUnits.size();
	}

	/**
	* From DatabaseObject
	*/
	public void prepareForExec()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

	/**
	* From DatabaseObject
	*/
	public boolean isPrepared()
	{
		return false;
	}

	/**
	* From DatabaseObject
	*/
	public void validate()
		throws IncompleteDatabaseException, InvalidDatabaseException
	{
		throw new InvalidDatabaseException("Not implemented");
	}

    /**
	* Read the list from the database.
	*/
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readEngineeringUnitList(this);
	}

    /**
	* Write the list back out to the database.
	*/
	public void write()
		throws DatabaseException
	{
		myDatabase.getDbIo().writeEngineeringUnitList(this);
	}

	public void clear()
	{
		knownUnits.clear();
		knownUnitNames.clear();
	}
}

