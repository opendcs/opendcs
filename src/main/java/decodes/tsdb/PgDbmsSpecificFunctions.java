/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2011/02/07 23:37:19  mmaloney
 * Implement DbmsSpecificFunctions class and concrete subclass for PG and Oracle.
 *
 */
package decodes.tsdb;

import opendcs.dai.DaiBase;
import opendcs.dao.DaoBase;

/**
 * Implements DBMS-specific functions for Postgres
 * @author mmaloney
 */
public class PgDbmsSpecificFunctions extends DbmsSpecificFunctions
{

	public PgDbmsSpecificFunctions(TimeSeriesDb theDb)
	{
		super(theDb);
	}

	@Override
	public void setSessionTimeZone(String tzName) 
		throws DbIoException
	{
		String q = "SET time zone " + theDb.sqlString(tzName);
		DaiBase dao = new DaoBase(theDb, "PgDbmsSpecificFunctions");
		try { dao.doModify(q); }
		finally { dao.close(); }
	}
}
