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
package opendcs.dao;

import ilex.util.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import opendcs.dai.DataTypeDAI;

import decodes.db.DataType;
import decodes.db.DataTypeSet;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TsdbDatabaseVersion;
import decodes.util.DecodesSettings;

/**
 * Data Access Object for writing/reading DbEnum objects to/from a SQL database
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class DataTypeDAO extends DaoBase implements DataTypeDAI
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	// Data Types are cached in DataTypeSet, so no cache here.

	// Columns to use in select
	private String columns;

	public DataTypeDAO(DatabaseConnectionOwner tsdb)
	{
		super(tsdb, "DataTypeSqlDao");
		columns = tsdb.getDecodesDatabaseVersion() < DecodesDatabaseVersion.DECODES_DB_10
			? "id, standard, code" : "id, standard, code, display_name";
	}

	@Override
	public DataType getDataType(DbKey id)
		throws DbIoException
	{
		if (id == null || id.isNull())
			return null;


		String q = "SELECT " + columns + " FROM DataType where id = " + id;

		try (ResultSet rs = doQuery(q))
		{
			if (rs.next())
			{
				DataType ret = DataType.getDataType(rs.getString(2), rs.getString(3), id);
				if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
					ret.setDisplayName(rs.getString(4));
				return ret;
			}
			else
				return null;
		}
		catch (SQLException ex)
		{
			String msg = "Error in query '" + q + "'";
			throw new DbIoException(msg, ex);
		}
	}

	@Override
	public void writeDataType(DataType dt) throws DbIoException
	{
		String q = "";
		try
		{
			DbKey id = dt.getId();
			if (id.isNull()) // New data type
			{
				id = db.getKeyGenerator().getKey("DataType", getConnection());
				dt.setId(id);

				q = "INSERT INTO DataType(" + columns + ") VALUES(" + id
					+ ", " + sqlString(dt.getStandard())
					+ ", " + sqlString(dt.getCode())
					+ (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10 ?
						(", " + sqlString(dt.getDisplayName())) : "")
					+ ")";
			}
			else // Update data type
			{
				q = "UPDATE DataType set standard = " + sqlString(dt.getStandard())
					+ ", code = " + sqlString(dt.getCode());
				if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
					q = q + ", display_name = " + sqlString(dt.getDisplayName());

				q = q + " where id = " + id;
			}
			doModify(q);
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot write data type.");
		}
	}

	@Override
	public DataType lookupDataType(String dtcode) throws DbIoException,
		NoSuchObjectException
	{
		String q = "SELECT id, standard FROM DataType WHERE upper(code) = "
				+ sqlString(dtcode.toUpperCase());
		DataType pref = null;
		DataType first = null;

		try (ResultSet rs = doQuery2(q))
		{
			while (rs.next())
			{
				DbKey id = DbKey.createDbKey(rs, 1);
				String std = rs.getString(2);
				DataType dt = DataType.getDataType(std, dtcode, id);
				if (first == null)
					first = dt;
				if (std.equalsIgnoreCase(DecodesSettings.instance().dataTypeStdPreference))
				{
					pref = dt;
					break;
				}
			}
			if (pref != null)
				return pref;
			else if (first == null)
				throw new NoSuchObjectException(
					"No data type with code '"+dtcode+"' exists. "
					+ " We suggest adding it to one of your presentation groups" +
					" in the DECODES Database Editor, Presentation tab.");
			return first;
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Unable to lookup DataType.", ex);
		}
	}

	@Override
	public void readDataTypeSet(DataTypeSet dts)
		throws DbIoException
	{
		readDataTypeSet(dts, null);
	}

	@Override
	public void readDataTypeSet(DataTypeSet dts, String standard)
		throws DbIoException
	{
		String q = "select " + columns + " from DataType";
		if (standard != null)
		{
			q = q + " where STANDARD = " + sqlString(standard);
		}
		ResultSet rs = doQuery(q);
		try
		{
			while (rs.next())
			{
				DbKey id = DbKey.createDbKey(rs, 1);
				String standardName = rs.getString(2);
				String code = rs.getString(3);
				DataType dt = new DataType(standardName, code);
				dt.forceSetId(id);
				if (db.getDecodesDatabaseVersion() >= DecodesDatabaseVersion.DECODES_DB_10)
					dt.setDisplayName(rs.getString(4));
				dts.add(dt);
			}

			ArrayList<Pair> equivs = readDtEquivalences();
			for(Pair equiv : equivs)
			{
				DbKey id0 = (DbKey)equiv.first;
				DbKey id1 = (DbKey)equiv.second;

				DataType dt0 = dts.getById(id0);
				DataType dt1 = dts.getById(id1);

				if (dt0 == null || dt1 == null)
				{
					log.warn("Bad datatype equivalence ids ({},{}) -- ignored", id0, id1);
					continue;
				}

				dt0.assertEquivalence(dt1);
			}

			if (db.isHdb())
			{
				q = "select datatype_id, datatype_common_name from hdb_datatype";
				try (ResultSet rs2 = doQuery(q))
				{
					while (rs2.next())
					{
						DataType dt = dts.getById(DbKey.createDbKey(rs2, 1));
						if (dt != null)
							dt.setDisplayName(rs2.getString(2));
					}
				}
			}
		}
		catch(SQLException ex)
		{
			String msg = "Error reading data type set.";
			throw new DbIoException(msg, ex);
		}
	}

	/**
	 * Read all equivalences and return an arraylist of Pairs where the pairs
	 * hold DbKey objects.
	 * @return
	 */
	private ArrayList<Pair> readDtEquivalences()
		throws SQLException, DbIoException
	{
		String q = "SELECT a.id0, a.id1 FROM DataTypeEquivalence a";

		if (db.isCwms() && db.getTsdbVersion() >= TsdbDatabaseVersion.VERSION_8)
			// For CWMS, the join with datatype will implicitly add the VPD predicate
			q = q + ", datatype b where a.id0 = b.id";

		ArrayList<Pair> ret = new ArrayList<Pair>();
		try (ResultSet rs = doQuery(q))
		{
			while (rs != null && rs.next())
			{
				DbKey id0 = DbKey.createDbKey(rs, 1);
				DbKey id1 = DbKey.createDbKey(rs, 2);
				ret.add(new Pair(id0, id1));
			}
		}
		return ret;
	}

	/**
	 * Given a complete data type set, modify what's currently in the database
	 * to conform to the passed list. Items in the database are updated, inserted,
	 * or deleted, as appropriate.
	 */
	public void writeDataTypeSet(DataTypeSet dts)
		throws DbIoException, SQLException
	{
		// First read what's already in the db so I know what to insert,
		// update, and delete.
		DataTypeSet dbSet = new DataTypeSet();
		readDataTypeSet(dbSet);
		ArrayList<Pair> dbEquivs = readDtEquivalences();

		// Datatypes are immutable. So only additions are allowed.
		// New datatypes will have no ID, only insert those.
		for (DataType dt : dts.values())
		{
			DataType dbdt = dbSet.get(dt.getStandard(), dt.getCode());
			if (dbdt != null)
			{
				if (!dt.getId().equals(dbdt.getId())
				 || !dt.getDisplayName().equals(dbdt.getDisplayName()))
				{
					dt.forceSetId(dbdt.getId());
					writeDataType(dt);
				}
				// Else they're already exactly the same. No need to write.
			}
			else // must be new
				writeDataType(dt);
		}


		// Now (Re)Assert equivalences.
		ArrayList<Pair> seen = new ArrayList<Pair>();

		for (DataType dt : dts.values())
		{
			for(DataType eq = dt.equivRing; eq != null && eq != dt;
				eq = eq.equivRing)
			{
				log.trace("Checking for equiv({}, {})", dt.getKey(), eq.getKey());
				boolean found = false;
				for(Pair equiv : dbEquivs)
				{
					DbKey id0 = (DbKey)equiv.first;
					DbKey id1 = (DbKey)equiv.second;
					if ((id0.equals(dt.getKey()) && id1.equals(eq.getKey()))
					 || (id1.equals(dt.getKey()) && id0.equals(eq.getKey())))
					{
						// This equivalence already exists in the DB. Remove it from the array.
						dbEquivs.remove(equiv);
						seen.add(equiv);
						found = true;
						break;
					}
				}
				if (!found) // make sure it's not a duplicate already seen
				{
					found = false;
					for(Pair equiv : seen)
					{
						DbKey id0 = (DbKey)equiv.first;
						DbKey id1 = (DbKey)equiv.second;
						if ((id0.equals(dt.getKey()) && id1.equals(eq.getKey()))
						 || (id1.equals(dt.getKey()) && id0.equals(eq.getKey())))
						{
							found = true;
							break;
						}
					}
					if (!found)
						assertEquivalence(dt, eq);
				}
			}
		}
		// Everything left now in dbEquivs has been removed from the DB.
		for(Pair equiv : dbEquivs)
		{
			DbKey id0 = (DbKey)equiv.first;
			DbKey id1 = (DbKey)equiv.second;
			doModify("DELETE FROM DataTypeEquivalence where id0 = " + id0
				+ " and id1 = " + id1);
		}
	}

	/**
	* Asserts Equivalence between two data type objects by entering a
	* row in the datatypeequivalence table.
	* @param dt1 the 1st DataType
	* @param dt2 the 2nd DataType
	*/
	private void assertEquivalence(DataType dt1, DataType dt2)
	{
		// Always make the lower number dt1. That will get rid of redundancies.
		DbKey id1 = dt1.getId();
		DbKey id2 = dt2.getId();
		if (id2.getValue() < id1.getValue())
		{
			DbKey t = id1;
			id1 = id2;
			id2 = t;
		}

		String q = "INSERT INTO datatypeequivalence VALUES (" +	id1 + ", " + id2 + ")";
		try { doModify(q); }
		catch(DbIoException ex)
		{
			String msg = ex.toString();
			if (msg.toLowerCase().contains("unique"))
				; // Do nothing. It means equivalence is already asserted.
			else
			{
				log.warn("Cannot assert equivalence '{}'", q);
			}
		}
	}
}