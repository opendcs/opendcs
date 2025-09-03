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
package decodes.hdb;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.OracleSequenceKeyGenerator;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

/**
Implements the KeyGenerator interface by using SQL sequences using the
ORACLE syntax.
This class is the same as the vanilla SequenceKeyGenerator, except
that it uses the oracle attribute syntax, rather than the postgres
function call syntax.
This implementation will work with Version 5 or Version 6 database.
*/
public class OracleSequenceHDBGenerator extends OracleSequenceKeyGenerator
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
	public static String module = "OracleSequenceHDBGenerator";

	/** Default constructor. */
	public OracleSequenceHDBGenerator()
	{
	}

	/**
	  Initializes the the key-generator for the passed database connection.
	  @param conn the JDBC Database Connection object
	*/
	public void init(Connection conn)
	{
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
		if (tableName.equalsIgnoreCase("HDB_LOADING_APPLICATION"))
		{
			String q = "select max(loading_application_id) from hdb_loading_application";
			try(Statement stmt = conn.createStatement();
			    ResultSet rs = stmt.executeQuery(q))
			{
				if ( !rs.next())
				{
					String msg = module + " query '" + q + "' returned no values.";
					log.warn(msg);
					throw new DatabaseException(msg);
				}
				long max = rs.getLong(1);
		
				return DbKey.createDbKey(max+1);
			}
			catch(SQLException ex)
			{
				String err = "SQL Error executing '" + q + "'";
				throw new DatabaseException(err,ex);
			}
		}
		else
			return super.getKey(tableName, conn);
	}
	
	
}

