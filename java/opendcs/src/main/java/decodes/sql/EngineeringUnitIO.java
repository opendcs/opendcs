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
package decodes.sql;

import java.util.Iterator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import decodes.db.DatabaseException;
import decodes.db.EngineeringUnit;
import decodes.db.EngineeringUnitList;
import decodes.db.DbEnum;
import decodes.db.EnumValue;

/**
This class handles SQL IO for Engineering Units
*/
public class EngineeringUnitIO extends SqlDbObjIo
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	/**
	  Constructor.
	  @param dbio the SqlDatabaseIO to which this IO object belongs
	*/
	public EngineeringUnitIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}

	/**
	  Read the list from the EngineeringUnit table.
	  @param euList the EngineeringUnitList to populate
	*/
	public void read(EngineeringUnitList euList)
		throws DatabaseException
	{
		log.debug("Reading Engineering Units...");
		String q =
			"SELECT unitAbbr, name, family, measures " +
			"FROM EngineeringUnit";
		log.trace("Executing query '{}'", q);

		try (Statement stmt = createStatement();
			ResultSet rs = stmt.executeQuery(q);)
		{
			while (rs.next())
			{
				String abbr = rs.getString(1);
				String name = rs.getString(2);
				String family = rs.getString(3);
				String measures = rs.getString(4);
				EngineeringUnit eu = new EngineeringUnit(abbr, name, family, measures);
				euList.add(eu);
			}
		}
		catch (SQLException ex)
		{
			String msg = "Error reading EU List.";
			throw new DatabaseException(msg, ex);
		}
	}

	/**
	  Write the whole list out to the database.
	  @param euList the EngineeringUnitList to write
	*/
	public void write(EngineeringUnitList euList)
		throws DatabaseException,
				 SQLException
	{
		log.info("EngineeringUnitIO.write()");

		// First read the current list so we know what needs to be
		// updated, inserted, or deleted.
		EngineeringUnitList dbList = new EngineeringUnitList();
		read(dbList);
		log.info("EngineeringUnitIO.write() list to write has {} EUs, current database has {}",
				 euList.size(), dbList.size());

		for(Iterator<EngineeringUnit> euit = euList.iterator(); euit.hasNext(); )
		{
			EngineeringUnit eu = euit.next();
			String abbr = eu.getAbbr();
			if (abbr.length() > 24)
				abbr = abbr.substring(0, 24);
			String family = eu.getFamily();
			if (family == null)
				family = "unknown";
			else if (family.length() > 24)
				family = family.substring(0, 24);
			String measures = eu.getMeasures();
			if (measures != null && measures.length() > 24)
				measures = measures.substring(0, 24);


			EngineeringUnit dbeu = dbList.getByAbbr(eu.getAbbr());
			if (dbeu != null)
			{
				if (!dbeu.equals(eu))
				{
					String q = "update EngineeringUnit "
						+ "set name=" + sqlReqString(eu.getName())
						+ ", family=" + sqlOptString(family)
						+ ", measures=" + sqlOptString(measures)
						+ " where lower(unitabbr) = "
						+ sqlString(eu.getAbbr().toLowerCase());
					tryUpdate(q);
				}
				dbList.remove(dbeu);
			}
			else // it must be new
			{
				String q = "INSERT INTO engineeringunit(UNITABBR,NAME,FAMILY,MEASURES)" +
					" VALUES (" +
					sqlReqString(eu.abbr) + ", " +
					sqlReqString(eu.getName()) + ", " +
					sqlOptString(family) + ", " +
					sqlOptString(measures)
					+ ")";
				tryUpdate(q);
			}
		}

		// Now anything remaining in the dbList must have been deleted.
		for(Iterator<EngineeringUnit> euit = dbList.iterator(); euit.hasNext(); )
		{
			EngineeringUnit dbeu = euit.next();
			if (!dbeu.getAbbr().equalsIgnoreCase("raw"))
			{
				String q = "delete from unitconverter where " +
					"lower(fromunitsabbr) = "
					+ sqlString(dbeu.getAbbr().toLowerCase());
				log.trace("Executing '{}'", q);
				tryUpdate(q);

			}
			String q = "delete from engineeringunit where lower(unitabbr) = "
				+ sqlString(dbeu.getAbbr().toLowerCase());
			log.trace("Executing '{}'", q);
			tryUpdate(q);
		}
	}

}
