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

 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
*/
package decodes.cwms;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.DecodesDatabaseVersion;
import decodes.sql.KeyGenerator;

/**
Implements the KeyGenerator interface by using SQL sequences using the
ORACLE syntax.
This class is the same as the vanilla SequenceKeyGenerator, except
that it uses the oracle attribute syntax, rather than the postgres
function call syntax.
This implementation will work with Version 5 or Version 6 database.
*/
public class CwmsSequenceKeyGenerator implements KeyGenerator
{
	private int decodesDatabaseVersion = 0;
	
	/** Default constructor. */
	public CwmsSequenceKeyGenerator(int decodesDatabaseVersion)
	{
		this.decodesDatabaseVersion = decodesDatabaseVersion;
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
		String seqname = "cwms_20.cwms_seq";
		
		// DB 13 is DECODES 6.2 or later. Use table-specific sequences for
		// high-volume records so we don't overrun CWMS_SEQ.
		if (decodesDatabaseVersion >= DecodesDatabaseVersion.DECODES_DB_13
		 && (tableName.equalsIgnoreCase("SCHEDULE_ENTRY_STATUS")
			 || tableName.equalsIgnoreCase("DACQ_EVENT")))
			seqname = tableName + "IdSeq";
		
		if (tableName.equalsIgnoreCase("CP_DEPENDS_NOTIFY"))
			seqname = tableName + "IdSeq";
		
		String q = "SELECT " + seqname + ".nextval from dual";

		try(Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(q))
		{
			if (rs == null || !rs.next())
			{
				String err = "Cannot read sequence with '" + q 
					+ "': " + (rs == null ? "Null Return" : "Empty Return");
				throw new DatabaseException(err);
			}
	
			DbKey ret = DbKey.createDbKey(rs, 1);
			stmt.close();
			return ret;
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
		// Never reset the one-and-only cwms sequence
	}
	
}

