/*
*  $Id$
*
*  $Log$
*  Revision 1.2  2015/03/19 15:23:14  mmaloney
*  punch list
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.4  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.3  2011/05/20 17:33:23  mmaloney
*  added setSuffix method
*
*  Revision 1.2  2009/01/03 20:31:23  mjmaloney
*  Added Routing Spec thread-specific database connections for synchronization.
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/12/11 01:05:19  mmaloney
*  javadoc cleanup
*
*  Revision 1.3  2006/02/24 18:56:16  mmaloney
*  Bug fixes.
*
*  Revision 1.2  2004/09/02 12:16:04  mjmaloney
*  javadoc
*
*  Revision 1.1  2003/11/15 20:28:37  mjmaloney
*  Mods to transparently support either V5 or V6 database.
*
*/
package decodes.sql;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import decodes.db.DatabaseException;

import ilex.util.Logger;

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
public class SequenceKeyGenerator
	implements KeyGenerator
{
//	Connection conn;
	private String sequenceSuffix = "IdSeq";

	/** Default constructor. */
	public SequenceKeyGenerator()
	{
//		conn = null;
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

		Statement stmt = null;
		try
		{
			stmt = conn.createStatement();
	
			ResultSet rs = stmt.executeQuery(q);
			if (rs == null || !rs.next())
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
			throw new DatabaseException(err);
		}
		finally
		{
			if (stmt != null)
				try { stmt.close(); } catch(Exception ex) {}
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
		Statement stmt = null;
		try
		{
			stmt = conn.createStatement();
			stmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String err = "SQL Error executing '" + q + "': " + ex;
			throw new DatabaseException(err);
		}
		finally
		{
			if (stmt != null)
				try { stmt.close(); } catch(Exception ex) {}
		}
	}
}

