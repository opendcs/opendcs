/**
 * $Id$
 * 
 * $Log$
 * Revision 1.4  2017/01/24 15:38:08  mmaloney
 * CWMS-10060 added support for DecodesSettings.tsidFetchSize
 *
 * Revision 1.3  2014/07/03 12:53:41  mmaloney
 * debug improvements.
 *
 * Revision 1.2  2014/07/03 12:46:41  mmaloney
 * Make 'module' protected so that subclasses can change it.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other 
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import ilex.util.Logger;
import opendcs.dai.DaiBase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;
import decodes.util.DecodesException;

/**
 * Base class for Data Access Objects within OpenDCS.
 * This class can also be instantiated directly for executing miscellaneous
 * queries and modify statements.
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class DaoBase
	implements DaiBase
{
	protected DatabaseConnectionOwner db = null;
	private Statement queryStmt1 = null;
	private ResultSet queryResults1 = null;
	private Statement queryStmt2 = null;
	private ResultSet queryResults2 = null;
	private int fetchSize = 0;

	protected String module;
	
	/**
	 * Constructor
	 * @param tsdb the database
	 * @param module the name of the module for log messages
	 */
	public DaoBase(DatabaseConnectionOwner tsdb, String module)
	{
		this.db = tsdb;
		this.module = module;
	}
	
	/**
	 * Users should close the DAO after using.
	 */
	public void close()
	{
		if (queryStmt1 != null)
			try { queryStmt1.close(); } catch(Exception ex) {}
		if (queryStmt2 != null)
			try { queryStmt2.close(); } catch(Exception ex) {}
		queryStmt1 = queryStmt2 = null;
	}
	
	public void finalize()
	{
		close();
	}
	
	/**
	 * Does a SQL query with the default static statement & returns the
	 * result set.
	 * Warning: this method is not thread and nested-loop safe.
	 * If you need to do nested queries, you must create a separate
	 * statement and do the inside query yourself. Likewise, if called
	 * from multiple threads, an external synchronization mechanism is
	 * needed.
	 * @param q the query
	 * @return the result set
	 */
	public ResultSet doQuery(String q)
		throws DbIoException
	{
		if (queryResults1 != null)
		{
			try { queryResults1.close(); }
			catch(Exception ex) {}
			queryResults1 = null;
		}
		try
		{
			if (queryStmt1 == null)
				queryStmt1 = db.getConnection().createStatement();
			if (fetchSize > 0)
				queryStmt1.setFetchSize(fetchSize);
			debug3("Query1 '" + q + "'");
			
//if (this instanceof decodes.cwms.CwmsTimeSeriesDAO
// || this instanceof decodes.cwms.CwmsSiteDAO)
//debug1("Fetch size=" + queryStmt1.getFetchSize());

			return queryResults1 = queryStmt1.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}
	
	/** An extra do-query for inside-loop queries. */
	public ResultSet doQuery2(String q) throws DbIoException
	{
		if (queryResults2 != null)
		{
			try { queryResults2.close(); }
			catch(Exception ex) {}
			queryResults2 = null;
		}
		try
		{
			if (queryStmt2 == null)
				queryStmt2 = db.getConnection().createStatement();
			debug3("Query2 '" + q + "'");
			return queryResults2 = queryStmt2.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbIoException(msg);
		}
	}



	/**
	* Executes an UPDATE or INSERT query.
	* Thread safe: internally synchronized on the modify-statement.
	* @param q the query string
	* @throws DatabaseException  if the update fails.
	* @return number of records modified in the database
	*/
	public int doModify(String q)
		throws DbIoException
	{
		Statement modStmt = null;
		try
		{
			modStmt = db.getConnection().createStatement();
			debug3("Executing statement '" + q + "'");
			return modStmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			throw new DbIoException(msg);
		}
		finally
		{
			if (modStmt != null)
			{
				try { modStmt.close(); }
				catch(Exception ex) {}
				modStmt = null;
			}
		}
	}

	public void debug1(String msg)
	{
		Logger.instance().debug1(module + " " + msg);
	}
	public void debug2(String msg)
	{
		Logger.instance().debug2(module + " " + msg);
	}
	public void debug3(String msg)
	{
		Logger.instance().debug3(module + " " + msg);
	}
	public void info(String msg)
	{
		Logger.instance().info(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.instance().warning(module + " " + msg);
	}
	public void failure(String msg)
	{
		Logger.instance().failure(module + " " + msg);
	}
	public void fatal(String msg)
	{
		Logger.instance().fatal(module + " " + msg);
	}

	/**
	 * Format a string for a SQL statement
	 * @param arg the string
	 * @return the formatted string
	 */
	protected String sqlString(String arg)
	{
		if (arg == null)
			return "NULL";
		
		String a = "";
		int from = 0;
		int to;
		while ( (to = arg.indexOf('\'', from)) != -1 ) {
			a += arg.substring(from, to) + "''";
			from = to + 1;
		}
		a += arg.substring(from);

		return "'" + a + "'";
	}

	public DbKey getKey(String tableName)
		throws DbIoException
	{
		try { return db.getKeyGenerator().getKey(tableName, db.getConnection()); }
		catch(DecodesException ex)
		{
			throw new DbIoException(ex.getMessage());
		}
	}
	
	/**
	 * Format a double precision float for a sql statement.
	 * The special value Constants.undefinedDouble (a huge number) will
	 * be printed as NULL.
	 * @param d the double value
	 * @return The string for a sql statement
	 */
	public String sqlDouble(double d)
	{
		if (d == Constants.undefinedDouble) return "NULL";
		return Double.toString(d);
	}
	
	public String sqlBoolean(boolean b)
	{
		return db.sqlBoolean(b);
	}

	/**
	 * This method is used by the caches of some DAOs where a further check is
	 * needed to determine whether the cached object is OK before returning it.
	 * Typically the check involves a last modify time stored in the database.
	 * The default implementation here always returns true.
	 * @param ob The cached object
	 * @return true if the object is OK. False if it needs to be reloaded.
	 */
	public boolean checkCachedObjectOK(CachableDbObject ob)
	{
		return true;
	}

	public int getFetchSize()
	{
		return fetchSize;
	}

	public void setFetchSize(int fetchSize)
	{
		this.fetchSize = fetchSize;
	}
}
