/*
*  $Id$
*
*  $Log$
*  Revision 1.4  2016/09/29 18:54:35  mmaloney
*  CWMS-8979 Allow Database Process Record to override decodes.properties and
*  user.properties setting. Command line arg -Dsettings=appName, where appName is the
*  name of a process record. Properties assigned to the app will override the file(s).
*
*  Revision 1.3  2016/01/27 21:37:09  mmaloney
*  schedule_entry_status and dacq_event have their own sequences.
*
*  Revision 1.2  2015/12/02 21:13:12  mmaloney
*  Overload new reset() method to do nothing. CWMS sequence is never reset.
*
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.5  2013/04/11 21:01:42  mmaloney
*  dev
*
*  Revision 1.4  2013/04/04 19:24:56  mmaloney
*  CWMS connection stuff for both DECODES and TSDB.
*
*  Revision 1.3  2013/03/27 18:42:29  mmaloney
*  CWMS 2.2 Mods
*
*  Revision 1.2  2013/03/21 18:27:40  mmaloney
*  DbKey Implementation
*
*  Revision 1.1  2011/09/20 15:56:50  mmaloney
*  created - gang to finish.
*
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
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
import ilex.util.Logger;

/**
Implements the KeyGenerator interface by using SQL sequences using the
ORACLE syntax.
This class is the same as the vanilla SequenceKeyGenerator, except
that it uses the oracle attribute syntax, rather than the postgres
function call syntax.
This implementation will work with Version 5 or Version 6 database.
*/
public class CwmsSequenceKeyGenerator
	implements KeyGenerator
{
	private int cwmsSchemaVersion = CwmsTimeSeriesDb.CWMS_V_2_2;
	private int decodesDatabaseVersion = 0;
	
	/** Default constructor. */
	public CwmsSequenceKeyGenerator(int v, int decodesDatabaseVersion)
	{
		cwmsSchemaVersion = v;
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
		String seqname = cwmsSchemaVersion <= CwmsTimeSeriesDb.CWMS_V_2_2
			? "ccp.ccp_seq" : "cwms_20.cwms_seq";
		
		// DB 13 is DECODES 6.2 or later. Use table-specific sequences for
		// high-volume records so we don't overrun CWMS_SEQ.
		if (decodesDatabaseVersion >= DecodesDatabaseVersion.DECODES_DB_13
		 && (tableName.equalsIgnoreCase("SCHEDULE_ENTRY_STATUS")
			 || tableName.equalsIgnoreCase("DACQ_EVENT")))
			seqname = tableName + "IdSeq";
		
		if (tableName.equalsIgnoreCase("CP_DEPENDS_NOTIFY"))
			seqname = tableName + "IdSeq";
		
		String q = "SELECT " + seqname + ".nextval from dual";

		try
		{
			Statement stmt = conn.createStatement();
	
			ResultSet rs = stmt.executeQuery(q);
			if (rs == null || !rs.next())
			{
				String err = "Cannot read sequence with '" + q 
					+ "': " + (rs == null ? "Null Return" : "Empty Return");
				Logger.instance().log(Logger.E_FAILURE, err);
				throw new DatabaseException(err);
			}
	
			DbKey ret = DbKey.createDbKey(rs, 1);
			stmt.close();
			return ret;
		}
		catch(SQLException ex)
		{
			String err = "SQL Error executing '" + q + "': " + ex;
			Logger.instance().log(Logger.E_FAILURE, err);
			throw new DatabaseException(err);
		}
	}

	@Override
	public void reset(String tableName, Connection conn) throws DatabaseException
	{
		// Never reset the one-and-only cwms sequence
	}
	
}

