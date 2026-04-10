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
import java.sql.ResultSet;
import java.sql.SQLException;
import decodes.db.DatabaseException;

/**
Implements the KeyGenerator interface by using SQL sequences.
<p>
A Sequence for each table-name that requires keys must exist in the 
database. In most cases, the name of the sequence is the table name
plus "IdSeq". For backward compatibility. Specifically, the mapping
of table-name to sequence name is as follows:
<p>
<ul>
 <li>Site                 SiteIdSeq</li>
 <li>EquipmentModel       EquipmentIdSeq</li>
 <li>Enum                 EnumIdSeq</li>
 <li>DataType             DataTypeIdSeq</li>
 <li>Platform             PlatformIdSeq</li>
 <li>PlatformConfig       PlatformConfigIdSeq</li>
 <li>DecodesScript        DecodesScriptIdSeq</li>
 <li>RoutingSpec          RoutingSpecIdSeq</li>
 <li>DataSource           DataSourceIdSeq</li>
 <li>NetworkList          NetworkListIdSeq</li>
 <li>PresentationGroup    PresentationGroupIdSeq</li>
 <li>DataPresentation     DataPresentationIdSeq</li>
 <li>UnitConverter        UnitConverterIdSeq</li>
</ul>
<p>
This implementation will work with Version 5 or Version 6 database.
*/
public class SequenceKeyGenerator implements KeyGenerator
{
//	Connection conn;
	private String sequenceSuffix = "IdSeq";

	/** Default constructor. */
	public SequenceKeyGenerator()
	{
      /* do nothing */
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
			seqname = tableName + sequenceSuffix;
		String q = "SELECT nextval('" + seqname + "')";

		try (Statement stmt =  conn.createStatement();
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
			String err = "SQL Error executing '" + q + "': " + ex;
			throw new DatabaseException(err, ex);
		}
	}

	public void setSequenceSuffix(String sequenceSuffix)
	{
		this.sequenceSuffix = sequenceSuffix;
	}

	@Override
	public void reset(String tableName, Connection conn)
		throws DatabaseException
	{
		String seqname = tableName + sequenceSuffix;
		String q = "alter sequence " + seqname + " restart with 1";
		try (Statement stmt = conn.createStatement())
		{
			stmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String err = "SQL Error executing '" + q + "': " + ex;
			throw new DatabaseException(err);
		}
	}
}

