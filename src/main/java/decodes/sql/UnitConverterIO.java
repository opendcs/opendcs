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
import java.util.Iterator;

import ilex.util.Logger;
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

		Statement stmt = null;
		try
		{
			stmt = createStatement();
			String q =
				"SELECT " + columns + " FROM UnitConverter WHERE fromUnitsAbbr != 'raw'";

			debug3("Executing '" + q + "'");

			ResultSet rs = stmt.executeQuery(q);

			while (rs != null && rs.next())
			{
				UnitConverterDb ucdb = rs2Uc(rs);
				ucs.addDbConverter(ucdb);
			}
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (stmt != null)
				try {stmt.close();} catch(Exception ex) {}
		}
	}

	/**
	 * In clause should be complete a complete list of IDs, containing surrounding parens.
	 * @param inClause
	 * @return
	 */
	public ArrayList<UnitConverterDb> readUCsIn(String inClause)
		throws DatabaseException
	{
		ArrayList<UnitConverterDb> ret = new  ArrayList<UnitConverterDb>();
		Logger.instance().debug1("Reading UnitConversions in " + inClause);

		Statement stmt = null;
		try
		{
			stmt = createStatement();
			String q = "SELECT " + columns + " FROM UnitConverter WHERE id in " + inClause;

			debug3("Executing '" + q + "'");

			ResultSet rs = stmt.executeQuery(q);

			while (rs != null && rs.next())
			{
				UnitConverterDb ucdb = rs2Uc(rs);
				ret.add(ucdb);
			}

			return ret;
		}
		catch (SQLException e)
		{
			throw new DatabaseException(e.toString());
		}
		finally
		{
			if (stmt != null)
				try {stmt.close();} catch(Exception ex) {}
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
						uc2write.forceSetId(inDb.getId());
						q = "update UnitConverter set "
							+ "algorithm=" + sqlString(uc2write.algorithm) + ", "
							+ "a=" + sqlOptDouble(uc2write.coefficients[0]) + ", "
							+ "b=" + sqlOptDouble(uc2write.coefficients[1]) + ", "
							+ "c=" + sqlOptDouble(uc2write.coefficients[2]) + ", "
							+ "d=" + sqlOptDouble(uc2write.coefficients[3]) + ", "
							+ "e=" + sqlOptDouble(uc2write.coefficients[4]) + ", "
							+ "f=" + sqlOptDouble(uc2write.coefficients[5])
							+ " where fromUnitsAbbr=" + sqlString(inDb.fromAbbr)
							+ " and toUnitsAbbr=" + sqlString(inDb.toAbbr);
						executeUpdate(q);
					}
					dbUcs.removeDbConverter(inDb.fromAbbr, inDb.toAbbr);
				}
				else // this is a new converter
				{
					DbKey id = getKey("UnitConverter");
					uc2write.forceSetId(id);
					q = "insert into UnitConverter("
						+ "id, fromUnitsAbbr, toUnitsAbbr, "
						+ "algorithm, a, b, c, d, e, f) values( "
						+ id + ", " + sqlString(uc2write.fromAbbr)
						+ ", " + sqlString(uc2write.toAbbr)
						+ ", " + sqlString(uc2write.algorithm)
						+ ", " + sqlOptDouble(uc2write.coefficients[0])
						+ ", " + sqlOptDouble(uc2write.coefficients[1])
						+ ", " + sqlOptDouble(uc2write.coefficients[2])
						+ ", " + sqlOptDouble(uc2write.coefficients[3])
						+ ", " + sqlOptDouble(uc2write.coefficients[4])
						+ ", " + sqlOptDouble(uc2write.coefficients[5])
						+ ")";
					executeUpdate(q);
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
			try
			{
				UnitConverterDb inDb = it.next();
				q = "delete from UnitConverter "
					+ " where fromUnitsAbbr=" + sqlString(inDb.fromAbbr)
					+ " and toUnitsAbbr=" + sqlString(inDb.toAbbr);
				executeUpdate(q);
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
		DbKey id = ucdb.getId();

		String coeffStr = "";
		for (int i = 0; i < UnitConverterDb.MAX_COEFFICIENTS; ++i) {
			coeffStr += sqlOptDouble(ucdb.coefficients[i]);
			if (i < UnitConverterDb.MAX_COEFFICIENTS - 1) {
				coeffStr += ", ";
			}
		}

		if (ucdb.fromAbbr == null || ucdb.fromAbbr.trim().length() == 0
		 || ucdb.toAbbr == null || ucdb.toAbbr.trim().length() == 0)
		{
			warning(context
				+ " Unit Converter -- neither from nor to may be null: from='"
				+ ucdb.fromAbbr + "' to='" + ucdb.toAbbr + "'");
			return;
		}

		String q = "INSERT INTO " +
				"UnitConverter(ID, FROMUNITSABBR, TOUNITSABBR, ALGORITHM, A, B, C, D, E, F)" +
				" VALUES (" +
					 id + ", " +
					 sqlReqString(ucdb.fromAbbr) + ", " +
					 sqlReqString(ucdb.toAbbr) + ", " +
					 sqlReqString(ucdb.algorithm) + ", " +
					 coeffStr +
				   ")";

		executeUpdate(q);
	}

	public void setContext(String context)
	{
		this.context = context;
	}
}
