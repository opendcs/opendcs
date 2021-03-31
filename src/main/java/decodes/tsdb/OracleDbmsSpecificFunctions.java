/**
 * $Id$
 * 
 * $Log$
 * Revision 1.2  2011/08/06 04:09:51  gchen
 * Set up the date and timestamp format for Oracle DB.
 * Check not to use double slashes (\\) for Oracle DB.
 *
 * Revision 1.1  2011/02/07 23:37:19  mmaloney
 * Implement DbmsSpecificFunctions class and concrete subclass for PG and Oracle.
 *
 */
package decodes.tsdb;

import decodes.util.DecodesSettings;
import opendcs.dai.DaiBase;
import opendcs.dao.DaoBase;

/**
 * Implements DBMS-specific functions for Postgres
 * @author mmaloney
 */
public class OracleDbmsSpecificFunctions extends DbmsSpecificFunctions
{

	public OracleDbmsSpecificFunctions(TimeSeriesDb theDb)
	{
		super(theDb);
	}

	@Override
	public void setSessionTimeZone(String tzName) 
		throws DbIoException
	{
		String q = null;

		DaiBase dao = new DaoBase(theDb, "PgDbmsSpecificFunctions");
		try
		{
			q = "ALTER SESSION SET time_zone = "
				+ theDb.sqlString(DecodesSettings.instance().sqlTimeZone);
			theDb.debug3("Setting time zone with '" + q + "'");
			dao.doModify(q);
			
			theDb.debug3("Setting date/timestamp format");
			q = "ALTER SESSION SET nls_date_format = 'yyyy-mm-dd hh24:mi:ss'";
			dao.doModify(q);
			q = "ALTER SESSION SET nls_timestamp_format = 'yyyy-mm-dd hh24:mi:ss'";
			dao.doModify(q);
		}
		finally
		{
			dao.close();
		}
	}
	
}
