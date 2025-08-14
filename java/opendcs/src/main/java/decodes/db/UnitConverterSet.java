/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.db;

import java.util.HashMap;
import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.sql.DbKey;
import ilex.util.StringPair;

/**
Maintains the set of engineering unit conversions.
*/
public class UnitConverterSet extends DatabaseObject
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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

		log.trace("Looking for a converter from '{}' to '{}'", fromAbbr, toAbbr);

		Object obj = execUnitConverters.get(sp);
		if (obj != null)
		{
			UnitConverter uc = (UnitConverter)obj;
			log.trace("Found unit converter from '{}' to '{}'", uc.getFromAbbr(), uc.getToAbbr());
			return (UnitConverter)obj;
		}

		log.debug("No converter from '{}'' to '{}' : Attempting to derive one...", fromAbbr, toAbbr);

		// Else -- try to derive a composite converter.
		UnitConverter comp = CompositeConverter.build(from, to);
		if (comp != null)
		{
			addExecConverter(comp);
			return comp;
		}

		log.warn("Cannot find converter for '{}' to '{}'", from, to);
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
		log.debug("Preparing unit converters");

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
			catch(InvalidDatabaseException ex)
			{
				log.atWarn()
				   .setCause(ex)
				   .log("Cannot prepare converter for '{}' to '{}'.", dbuc.fromAbbr, dbuc.toAbbr);
				// Prepare rest of set...
			}
			catch(NoConversionException ex)
			{
				log.atTrace()
				   .setCause(ex)
				   .log("No conversion exception for '{}' to '{}'." +
						"If this is from the LinearConverter::makeInverse" +
						" it may be reasonable to ignore. Consider if an inverse is possible.",
						dbuc.fromAbbr, dbuc.toAbbr);
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
