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

import java.sql.Connection;
import java.sql.Statement;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;

import decodes.db.DatabaseException;

/**
Implements the KeyGenerator interface by using SQL sequences using the
ORACLE syntax.
This class is the same as the vanilla SequenceKeyGenerator, except
that it uses the oracle attribute syntax, rather than the postgres
function call syntax.
This implementation will work with Version 5 or Version 6 database.
*/
public class OracleSequenceKeyGenerator implements KeyGenerator
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();


	/** Default constructor. */
	public OracleSequenceKeyGenerator()
	{
		log.debug("OracleSequenceKeyGenerator constructor");
	}

	/**
	  Generates a database-unique key suitable for adding a new entry
	  to the table.
	  The 'tableName' argument should be the name of the table to which
	  a new record will be added.
	  @param tableName the tableName for which a key is needed
	  @return numeric key for a new row in this table
	*/
	public DbKey getKey(String tableName, Connection conn)
		throws DatabaseException
	{
		String seqname;
		if (tableName.equalsIgnoreCase("EquipmentModel"))
			seqname = "EquipmentIdSeq";
		else
			seqname = tableName.trim() + "IdSeq";
		String q = "SELECT " + seqname.trim() + ".nextval from dual";

		try (Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(q);)
		{
			if (!rs.next())
			{
				String err = "Cannot read sequence value from '" + seqname
					+ "': " + (rs == null ? "Null Return" : "Empty Return");
				throw new DatabaseException(err);
			}

			return DbKey.createDbKey(rs, 1);
		}
		catch(SQLException ex)
		{
			String err = "SQL Error executing '" + q + "'";
			throw new DatabaseException(err, ex);
		}
	}

	@Override
	public void reset(String tableName, Connection conn) throws DatabaseException
	{
		// Primitive Oracle SQL requires 4 steps:
		// 1. Get current sequence value
		// 2. Set increment to negative that amount
		// 3. Get next sequence value (which causes increment to be applied).
		// 4. Set increment back to 1.
		DbKey curval = getKey(tableName, conn);

		String seqname = tableName.trim() + "IdSeq";
		String q = "alter sequence " + seqname + " increment by -" + (curval.getValue()-2);

		try (Statement stmt = conn.createStatement();)
		{
			stmt.executeUpdate(q);
			q = "SELECT " + seqname + ".nextval from dual";
			stmt.executeUpdate(q);
			q = "alter sequence " + seqname + " increment by 1 minvalue 0";
			stmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String err = "SQL Error executing '" + q + "'";
			throw new DatabaseException(err, ex);
		}
	}
}
