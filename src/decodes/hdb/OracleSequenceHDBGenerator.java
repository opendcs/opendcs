/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of this source
*  code may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.4  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*  Revision 1.3  2009/01/03 20:41:58  mjmaloney
*  Key gen must now be passed a connection.
*
*  Revision 1.2  2008/12/23 22:29:46  mbogner
*  mod to fix compile error
*
*  Revision 1.1  2008/12/23 22:26:42  mbogner
*  create new sequence generator specifically for HDB only  M Bogner
*
*  Revision 1.1  2008/04/04 18:21:04  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2007/12/11 01:05:19  mmaloney
*  javadoc cleanup
*
*  Revision 1.3  2007/11/20 14:27:35  mmaloney
*  dev
*
*  Revision 1.2  2006/05/03 21:01:01  mmaloney
*  dev
*
*  Revision 1.1  2005/06/09 20:45:44  mjmaloney
*  Modifications for Oracle compatibility.
*
*/
package decodes.hdb;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.OracleSequenceKeyGenerator;

import ilex.util.Logger;

/**
Implements the KeyGenerator interface by using SQL sequences using the
ORACLE syntax.
This class is the same as the vanilla SequenceKeyGenerator, except
that it uses the oracle attribute syntax, rather than the postgres
function call syntax.
This implementation will work with Version 5 or Version 6 database.
*/
public class OracleSequenceHDBGenerator
	extends OracleSequenceKeyGenerator
{

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
			return DbKey.createDbKey(0);
		else
			return super.getKey(tableName, conn);
	}
}

