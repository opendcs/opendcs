/*
*  $Id$
*
*  Open Source Software
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.2  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.1  2008/04/04 18:21:00  cvs
*  Added legacy code to repository
*
*  Revision 1.23  2007/11/20 14:27:35  mmaloney
*  dev
*
*  Revision 1.22  2007/09/11 15:21:18  mmaloney
*  dev
*
*  Revision 1.21  2004/08/27 12:23:12  mjmaloney
*  Added javadocs
*
*  Revision 1.20  2004/04/12 17:52:41  mjmaloney
*  Added delete method, required for new reference list editor.
*
*  Revision 1.19  2003/03/06 18:17:12  mjmaloney
*  Fixed DR 115 - Don't add raw converters to exec list. They are not general
*  purpose converters and cannot be used for composite conversions.
*
*  Revision 1.18  2002/10/06 14:23:57  mjmaloney
*  SQL Development.
*
*  Revision 1.17  2002/09/30 18:54:34  mjmaloney
*  SQL dev.
*
*  Revision 1.16  2002/09/22 18:39:54  mjmaloney
*  SQL Dev.
*
*  Revision 1.15  2002/07/15 21:58:06  chris
*  Added the size() and sizeDb() methods.
*
*  Revision 1.14  2001/11/24 18:29:10  mike
*  First working DbImport!
*
*  Revision 1.13  2001/09/27 18:18:55  mike
*  Finished rounding rules & eu conversions.
*
*  Revision 1.12  2001/09/27 00:57:24  mike
*  Work on presentation elements.
*
*  Revision 1.11  2001/08/12 17:36:54  mike
*  Slight architecture change for unit converters. The UnitConverterDb objects
*  are now full-fledged DatabaseObjects and not derived from UnitConverter.
*  This necessitated changes to DB parsing code and prepareForExec code.
*
*  Revision 1.10  2001/04/21 20:19:23  mike
*  Added read & write methods to all DatabaseObjects
*
*  Revision 1.9  2001/04/12 12:30:52  mike
*  dev
*
*  Revision 1.8  2001/04/02 00:42:33  mike
*  DatabaseObject is now an abstract base-class.
*
*  Revision 1.7  2001/03/23 20:09:25  mike
*  Collection classes are no longer monostate static collections.
*
*  Revision 1.6  2001/01/13 17:22:46  mike
*  Added parsers for EngineeringUnits
*
*  Revision 1.5  2001/01/13 14:59:33  mike
*  Implemented EU Conversions
*
*  Revision 1.4  2001/01/13 01:50:27  mike
*  dev
*
*  Revision 1.3  2001/01/12 21:56:25  mike
*  Renamed UnitConverterBase to UnitConverter
*
*  Revision 1.2  2001/01/12 21:53:38  mike
*  Renamed UnitConverter to UnitConverterDb
*
*  Revision 1.1  2001/01/12 15:38:20  mike
*  dev
*
*/
package decodes.db;

import java.util.HashMap;
import java.util.Iterator;

import decodes.sql.DbKey;
import ilex.util.StringPair;
import ilex.util.Logger;

/**
Maintains the set of engineering unit conversions.
*/
public class UnitConverterSet extends DatabaseObject
{
	/** Executable unit converters */
	private HashMap<StringPair, UnitConverter> execUnitConverters;

	/** database records for unit converters */
	private HashMap<StringPair, UnitConverterDb> dbUnitConverters
		= new HashMap<StringPair, UnitConverterDb>();

	/** Cross reference of all UCs (even raw's) to SQL ID. */
	private IdRecordList ucIdList;

	/** Default constructor. */
	public UnitConverterSet()
	{
		execUnitConverters = null;
		ucIdList = new IdRecordList("UnitConverter");
	}

	/** @return "UnitConverterSet" */
	public String getObjectType() { return "UnitConverterSet"; }

	/**
	  * Gets an executable converter to get from one engineering unit to
	  * another. If one was not defined in the database, this method attempts
	  * to build one by stringing together other converters.
	  *
	  * For example, cm-&gt;in might string together cm-&gt;m, m-&gt;ft, ft-&gt;in
	  *
	  * @param from the from EU
	  * @param to the to EU
	  * @return executable converter, or null if no match and can't build one.
	*/
	public UnitConverter get(EngineeringUnit from, EngineeringUnit to)
	{
		if (!isPrepared())
			prepareForExec();

		String fromAbbr = from.abbr.toUpperCase();
		String toAbbr = to.abbr.toUpperCase();

		// If a match already exists in the exec-set, just return it.
		StringPair sp = new StringPair(fromAbbr, toAbbr);

		Logger.instance().debug3("Looking for a converter from '" + fromAbbr
			+ "' to '" + toAbbr + "'");

		Object obj = execUnitConverters.get(sp);
		if (obj != null)
		{
			UnitConverter uc = (UnitConverter)obj;
			Logger.instance().log(Logger.E_DEBUG2,
				"Found unit converter from '" + uc.getFromAbbr()
				+ "' to '" + uc.getToAbbr()+ "'");
			return (UnitConverter)obj;
		}

		Logger.instance().log(Logger.E_DEBUG2,
			"No converter from '" + fromAbbr
			+ "' to '" + toAbbr + "': Attempting to derive one...");

		// Else -- try to derive a composite converter.
		UnitConverter comp = CompositeConverter.build(from, to);
		if (comp != null)
		{
			addExecConverter(comp);
			return comp;
		}

		Logger.instance().debug1(
			"Cannot find converter for '" + from + "' to '" + to + "'");
		return null;
	}

	/**
	  Finds database record given database ID.
	  @param id the ID
	  @return the UnitConverterDb database record or null if no match
	*/
	public UnitConverterDb getById(DbKey id)
	{
		return (UnitConverterDb)ucIdList.get(id);
	}

	/**
	* @return the number of executable UnitConverters in this set.
	*/
	public int size() {
		if (execUnitConverters == null) return 0;
		return execUnitConverters.size();
	}

	/**
	* @return the number of UnitConveterDbs in this set.
	*/
	public int sizeDb() {
		return dbUnitConverters.size();
	}

	/**
	  Adds a database-defined unit converter.
	  Do not call this methods for raw converters, only for converters
	  from one standard EU to another.
	  @param ucdb the database UC record
	*/
	public void addDbConverter(UnitConverterDb ucdb)
	{
		// HashMap does not contain raw converters, only real Unit conversions.
		if (!ucdb.toAbbr.equalsIgnoreCase("raw"))
		{
			StringPair sp = new StringPair(ucdb.fromAbbr.toUpperCase(),
				ucdb.toAbbr.toUpperCase());
			dbUnitConverters.put(sp, ucdb);
		}
		ucIdList.add(ucdb);
	}

	/**
	  Gets a database record for the specified from and to.
	  @param from the from EU
	  @param to the to EU
	  @return ucdb or null if no match
	*/
	public UnitConverterDb getDb(EngineeringUnit from, EngineeringUnit to)
	{
		return getDb(from.abbr, to.abbr);
	}

	/**
	  Gets a database record for the specified from and to.
	  @param fromAbbr the from EU abbreviation
	  @param toAbbr the to EU abbreviation
	  @return ucdb or null if no match
	*/
	public UnitConverterDb getDb(String fromAbbr, String toAbbr)
	{
		fromAbbr = fromAbbr.toUpperCase();
		toAbbr = toAbbr.toUpperCase();
		StringPair sp = new StringPair(fromAbbr, toAbbr);
		return dbUnitConverters.get(sp);
	}

	/**
	  Removes a database record for the specified from and to.
	  @param fromAbbr the from EU abbreviation
	  @param toAbbr the to EU abbreviation
	*/
	public void removeDbConverter(String fromAbbr, String toAbbr)
	{
		fromAbbr = fromAbbr.toUpperCase();
		toAbbr = toAbbr.toUpperCase();
		StringPair sp = new StringPair(fromAbbr, toAbbr);
		dbUnitConverters.remove(sp);
	}

	/**
	  Adds an executable unit converter.
	  Do not call this methods for raw converters, only for converters
	  from one standard EU to another.
	  @param uc the executable converter
	*/
	public void addExecConverter(UnitConverter uc)
	{
		StringPair sp = new StringPair(uc.getFrom().abbr.toUpperCase(),
			uc.getTo().abbr.toUpperCase());

		// If there's an existing converter with a lower weight, just keep it.
		UnitConverter tmp = (UnitConverter)execUnitConverters.get(sp);
		if (tmp != null && tmp.getWeight() < uc.getWeight())
			return;

		// Otherwise add/replace the new one into the set.
		execUnitConverters.put(sp, uc);
	}

	/**
	  @return in iterator into the set of known database-defined converters.
	*/
	public Iterator<UnitConverterDb> iteratorDb()
	{
		return dbUnitConverters.values().iterator();
	}

	/**
	  Some executable converters may be derived dynamically (i.e. not 
	  stored in database).
	  @return in interator into the set of known executable converters.
	*/
	public Iterator<UnitConverter> iteratorExec()
	{
		return execUnitConverters.values().iterator();
	}

	/**
	  From DatabaseObject, creates executable concrete sub-classes for
	  all unit-converters defined in the database.
	*/
	public void prepareForExec()
	{
		Logger.instance().log(Logger.E_DEBUG1, "Preparing unit converters");

		execUnitConverters = new HashMap<StringPair, UnitConverter>();

		// Look for unprepared match in DB converters & prepare it.
		for(Iterator<UnitConverterDb> it = iteratorDb(); it.hasNext(); )
		{
			UnitConverterDb dbuc = it.next();

			try
			{
				dbuc.prepareForExec();
				addExecConverter(dbuc.execConverter);

				// MJM 20020307 - Don't add raw converters to the exec list.
				// Note - the SQL IO classes add them to the DB list.
				if (dbuc.fromAbbr.trim().equalsIgnoreCase("raw"))
					continue;

				// If it's linear add the inverse as well.
				if (dbuc.execConverter instanceof LinearConverter)
				{
					LinearConverter luc = (LinearConverter)dbuc.execConverter;
					addExecConverter(luc.makeInverse());
				}
				else if (dbuc.execConverter instanceof NullConverter)
				{
					NullConverter luc = (NullConverter)dbuc.execConverter;
					addExecConverter(luc.makeInverse());
				}

			}
			catch(InvalidDatabaseException e)
			{
				Logger.instance().log(Logger.E_WARNING,
					"Cannot prepare converter for '" + dbuc.fromAbbr
					+ "' to '" + dbuc.toAbbr + "': " + e);
				// Prepare rest of set...
			}
			catch(NoConversionException e)
			{
				// Happens when linear converter is not convertable
				// because the slope is 0. ... ignore & keep going.
			}
		}
	}

	/**
	  From DatabaseObject
	*/
	public boolean isPrepared()
	{
		return execUnitConverters != null;
	}

	/**
	  From DatabaseObject
	*/
	public void validate()
	{
	}

	/** Reads the set from the database */
	public void read()
		throws DatabaseException
	{
		myDatabase.getDbIo().readUnitConverterSet(this);
	}

	/** Writes the set to the database */
	public void write()
		throws DatabaseException
	{
		throw new DatabaseException("UnitConverterSet.write() not implemented");
	}
	
	public void clear()
	{
		dbUnitConverters.clear();
		ucIdList.clear();
	}
}
