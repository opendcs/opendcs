/**
 * $Id$
 * 
 * $Log$
 * Revision 1.1  2011/02/07 23:37:19  mmaloney
 * Implement DbmsSpecificFunctions class and concrete subclass for PG and Oracle.
 *
 */
package decodes.tsdb;

/**
 * This class performs DBMS-specific functions.
 * In general we try to stay to a common subset of SQL that works
 * on Postgres, Oracle, Ingres, and MS SQL Server. This class 
 * encapsulates the few places where this is not possible.
 */
public abstract class DbmsSpecificFunctions
{
	TimeSeriesDb theDb;
	
	/** The time-series database object constructs this as a helper-class. */
	public DbmsSpecificFunctions(TimeSeriesDb theDb)
	{
		this.theDb = theDb;
	}
	
	public abstract void setSessionTimeZone(String tzName)
		throws DbIoException;

}
