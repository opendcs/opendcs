/*
 * $Id$
 *
 * Open Source Software
 *
 * $Log$
 * Revision 1.2  2015/07/17 13:03:55  mmaloney
 * Added context to improve debug/error messages.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.3  2013/04/12 19:34:27  mmaloney
 * column mask on inserts required for VPD
 *
 * Revision 1.2  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ilex.util.Logger;
import opendcs.dao.DaoBase;
import decodes.db.DatabaseException;
import decodes.db.UnitConverterDb;
import decodes.db.UnitConverterSet;

/**
 * This class handles reading and writing the UnitConverter table
 * of the SQL database.
 */
public class UnitConverterIO extends SqlDbObjIo
{
	private String context = "";
	private String columns = "id, fromUnitsAbbr, toUnitsAbbr, algorithm, a, b, c, d, e, f";

	/**
	* Constructor.
	* @param dbio parent SqlDatabaseIO object
	*/
	public UnitConverterIO(SqlDatabaseIO dbio)
	{
		super(dbio);
	}

	/**
	* Reads in all the UnitConverters from the SQL database.
	* @param ucs the object to read
	*/
	public void read(UnitConverterSet ucs)
		throws DatabaseException
	{
		Logger.instance().debug1("Reading UnitConversions...");

		try(DaoBase dao = new DaoBase(this._dbio,this.getClass().getName());)
		{
			String q =
				"SELECT " + columns + " FROM UnitConverter WHERE fromUnitsAbbr != 'raw'";
			
			debug3("Executing '" + q + "'");

			dao.doQuery(q, rs-> {
				ucs.addDbConverter(rs2Uc(rs));
			});
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e.toString());
		}
	}

	/**
	 * Retrieve unit converions for the given unit keys
	 * @param keys
	 * @return initialized unit conversion objects. Empty list is returned on empty input
	 */
	public ArrayList<UnitConverterDb> readUCsIn(Collection<DbKey> keys)
		throws DatabaseException
	{
		ArrayList<UnitConverterDb> ret = new  ArrayList<UnitConverterDb>();
		Logger.instance().debug1("Reading UnitConversions in for " + keys.size() + " units");
		if( keys.isEmpty() )
		{
			return ret;
		}
		try(DaoBase dao = new DaoBase(this._dbio, this.getClass().getName()))
		{
			
			String q = "SELECT " + columns + " FROM UnitConverter WHERE id in (%s)";
			debug3("Executing '" + q + "'");
			ret.addAll( 
				dao.getResults(
					String.format(q,dao.valueBinds(keys)),
					rs -> rs2Uc(rs),
					keys.toArray()
				)
			);

			return ret;
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e.toString(),e);
		}
	}

	private UnitConverterDb rs2Uc(ResultSet rs)
		throws SQLException
	{
		DbKey key = DbKey.createDbKey(rs, 1);
		String from = rs.getString(2);
		String to = rs.getString(3);

		UnitConverterDb ucdb = new UnitConverterDb(from, to);
		ucdb.forceSetId(key);

		ucdb.algorithm = rs.getString(4);

		for (int i = 0; i < 6; ++i)
			ucdb.coefficients[i] = rs.getDouble(5 + i);
		return ucdb;
	}

	/**
	  Writes the UnitConverters to the SQL database.
	  @param ucs2write the object to write
	*/
	public void write(UnitConverterSet ucs2write)
		throws DatabaseException
	{
		UnitConverterSet dbUcs = new UnitConverterSet();
		read(dbUcs);

		String q = "";
		for(Iterator<UnitConverterDb> it = ucs2write.iteratorDb(); it.hasNext(); )
		{
			UnitConverterDb uc2write = it.next();
			if (uc2write.fromAbbr == null || uc2write.fromAbbr.trim().length() == 0
			 || uc2write.toAbbr == null || uc2write.toAbbr.trim().length() == 0)
			{
				warning("Unit Converter Set: Neither from nor to may be null: from='"
					+ uc2write.fromAbbr + "' to='" + uc2write.toAbbr + "'");
				continue;
			}
			UnitConverterDb inDb = dbUcs.getDb(uc2write.fromAbbr, uc2write.toAbbr);
			try
			{
				if (inDb != null)
				{
					if (!inDb.equals(uc2write))
					{
						ArrayList<Object> parameters = new ArrayList<>();
						parameters.add(uc2write.algorithm);
						for(double v: uc2write.coefficients)
						{
							parameters.add(Double.valueOf(v));
						}
						parameters.add(inDb.fromAbbr);
						parameters.add(inDb.toAbbr);
						uc2write.forceSetId(inDb.getId());
						q = "update UnitConverter set "
							+ "algorithm=?, "
							+ "a=?, "
							+ "b=?, "
							+ "c=?, "
							+ "d=?, "
							+ "e=?, "
							+ "f=?,"
							+ " where fromUnitsAbbr=" + sqlString(inDb.fromAbbr)
							+ " and toUnitsAbbr=" + sqlString(inDb.toAbbr);
						try(DaoBase dao = new DaoBase(this._dbio,this.getClass().getName()))
						{
							dao.doModify(q,parameters.toArray());
						}
					}
					dbUcs.removeDbConverter(inDb.fromAbbr, inDb.toAbbr);
				}
				else // this is a new converter
				{
					DbKey id = getKey("UnitConverter");
					uc2write.forceSetId(id);
					insert(uc2write);
				}
			}
			catch (SQLException ex)
			{
				warning("Error in query '" + q + "': " + ex.toString());
			}
		}
		// Now anything left in the db set needs to be deleted.
		for(Iterator<UnitConverterDb> it = dbUcs.iteratorDb(); it.hasNext(); )
		{
			try(DaoBase dao = new DaoBase(this._dbio,this.getClass().getName());)
			{
				UnitConverterDb inDb = it.next();
				q = "delete from UnitConverter "
					+ " where fromUnitsAbbr=?"
					+ " and toUnitsAbbr=?";
				dao.doModify(q,inDb.fromAbbr,inDb.toAbbr);
			}
			catch (SQLException ex)
			{
				warning("Error in query '" + q + "': " + ex.toString());
			}
		}
	}

	/**
	* Adds a new UnitConverterDb to the database.  The argument must
	* *not* have had its SQL database ID set.  This assigns it a new ID,
	* and then inserts it into the database.
	* @param ucdb the object to write
	*/
	public void addNew(UnitConverterDb ucdb)
		throws DatabaseException, SQLException
	{
		DbKey id = getKey("UnitConverter");
		ucdb.setId(id);
		ucdb.getDatabase().unitConverterSet.addDbConverter(ucdb);

		insert(ucdb);
	}

	/**
	* Inserts a single UnitConverterDb into the database.  The argument
	* must have already had its SQL database ID set.
	* @param ucdb the object to write
	*/
	public void insert(UnitConverterDb ucdb)
		throws DatabaseException, SQLException
	{
		if (ucdb.fromAbbr == null || ucdb.fromAbbr.trim().length() == 0
		 || ucdb.toAbbr == null || ucdb.toAbbr.trim().length() == 0)
		{
			warning(context
				+ " Unit Converter -- neither from nor to may be null: from='"
				+ ucdb.fromAbbr + "' to='" + ucdb.toAbbr + "'");
			return;
		}

		ArrayList<Object> parameters = new ArrayList<>();
		parameters.add(ucdb.getId());
		parameters.add(ucdb.fromAbbr);
		parameters.add(ucdb.toAbbr);
		parameters.add(ucdb.algorithm);
		for(double v: ucdb.coefficients)
		{
			parameters.add(Double.valueOf(v));
		}

		String q = "INSERT INTO " +
				"UnitConverter(ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)" +
				" VALUES (%s)";
		try( DaoBase dao = new DaoBase(this._dbio,this.getClass().getName()))
		{
			dao.doModify(String.format(q,dao.valueBinds(parameters)),parameters.toArray());
		}
	}

	public void setContext(String context)
	{
		this.context = context;
	}
}
