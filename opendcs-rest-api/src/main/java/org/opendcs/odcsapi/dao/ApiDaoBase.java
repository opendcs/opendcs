/*
 *  Copyright 2023 OpenDCS Consortium
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.dao;

import org.opendcs.odcsapi.hydrojson.DbInterface;
import org.opendcs.odcsapi.util.ApiConstants;

import decodes.sql.DbKey;

import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ApiDaoBase
	implements ApiDaiBase
{
	protected DbInterface dbi = null;
	protected String module;
	
	private Statement queryStmt1 = null;
	private ResultSet queryResults1 = null;
	private Statement queryStmt2 = null;
	private ResultSet queryResults2 = null;
	private int fetchSize = 0;

	private PreparedStatement prepStmnt;

	public PreparedStatement getPrepStmnt()
	{
		return this.prepStmnt;
	}

	protected void setPrepStmnt(PreparedStatement prepStmnt)
	{
		this.prepStmnt = prepStmnt;
	}
	
	public ApiDaoBase(DbInterface dbi, String module)
	{
		this.dbi = dbi;
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

	private boolean statementInvalid(Statement stmt) {
		try
		{  // similar to above, it doesn't matter why the staement is no longer valid.
			return stmt.isClosed();
		} catch( SQLException err )
		{
			return true;
		}		
	}

	/**
	 * Does a SQL query with the default static statement and returns the
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
		throws DbException
	{
		if (queryResults1 != null)
		{
			try { queryResults1.close(); }
			catch(Exception ex) {
				//EMPTY CATCH
			}
			queryResults1 = null;
		}
		try
		{
			if (queryStmt1 == null || statementInvalid(queryStmt1))
				queryStmt1 = dbi.getConnection().createStatement();
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
			throw new DbException(module, ex, msg);
		}
	}
	
	/** An extra do-query for inside-loop queries. */
	public ResultSet doQuery2(String q) 
		throws DbException
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
				queryStmt2 = dbi.getConnection().createStatement();
			debug3("Query2 '" + q + "'");
			return queryResults2 = queryStmt2.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "': " + ex;
			warning(msg);
			throw new DbException(module, ex, msg);
		}
	}

	/**
	* Executes an UPDATE or INSERT query.
	* Thread safe: internally synchronized on the modify-statement.
	* @param q the query string
	* @throws DbException  if the update fails.
	* @return number of records modified in the database
	*/
	public int doModify(String q)
		throws DbException
	{
		Statement modStmt = null;
		try
		{
			modStmt = dbi.getConnection().createStatement();
			debug3("Executing statement '" + q + "'");
			return modStmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			throw new DbException(module, ex, msg);
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
		Logger.getLogger(ApiConstants.loggerName).fine(module + " " + msg);
	}
	public void debug2(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).finer(module + " " + msg);
	}
	public void debug3(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).finest(module + " " + msg);
	}
	public void info(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).info(module + " " + msg);
	}
	public void warning(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).warning(module + " " + msg);
	}
	public void failure(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).warning(module + " " + msg);
	}
	public void fatal(String msg)
	{
		Logger.getLogger(ApiConstants.loggerName).severe(module + " " + msg);
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

	public Long getKey(DbInterface.Sequences sequenceName)
			throws DbException
	{
		try
		{
			return dbi.getKey(sequenceName);
		} catch (SQLException e)
		{
			throw new DbException(e.getMessage());
		}
	}
	
	/**
	 * Format a double precision float for a sql statement.
	 * The special value Constants.undefinedDouble (a huge number) will
	 * be printed as NULL.
	 * @param d the double value
	 * @return The string for a sql statement
	 */
	public String sqlDouble(Double d)
	{
		if (d == null) return "NULL";
		return Double.toString(d);
	}
	
	public String sqlBoolean(boolean b)
	{
		return b ? "'Y'" : "'N'";
	}

	public int getFetchSize()
	{
		return fetchSize;
	}

	public void setFetchSize(int fetchSize)
	{
		this.fetchSize = fetchSize;
	}
	
	/**
	 * Construct a PreparedStatement with the query, insert the passed parameters,
	 * execute the statement, and then call the consumer. The statement is closed
	 * before return.
	 * @param query The query statement containing substitution ?s
	 * @param consumer The consumer of the result set
	 * @param parameters variable list of parameters.
	 */
	public void doQueryV(Connection conn, String query, ResultSetConsumer consumer, Object... parameters)
		throws DbException
	{
		String qmsg = query + " with params: ";
		if (conn == null)
		{
			conn = dbi.getConnection();
		}
		try (PreparedStatement stmt = conn.prepareStatement(query);)
		{
			int index=1;
			for( Object param: parameters)
			{
				qmsg = qmsg + (param==null?"null":param.toString()) + ", ";
				if (param instanceof Integer)
					stmt.setInt(index,(Integer)param);
				else if (param instanceof Long)
					stmt.setLong(index,(Long)param);
				else if (param instanceof Double)
					stmt.setDouble(index,(Double)param);
				else if (param instanceof String)
					stmt.setString(index,(String)param);
				else
					stmt.setObject(index,param);
				index++;
			}
			ResultSet rs = stmt.executeQuery();
			consumer.accept(rs);
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query: " + qmsg + ": " + ex);
		}
	}
	
	/**
     * Construct a PreparedStatement with the query, insert the passed parameters,
     * execute the statement, and return the ResultSet. The statement can be
     * used outside of the function.
     * 
     * @param conn The connection.  If it's null, it gets set in this function.
     * @param query The query statement containing substitution ?s
     * @param parameters variable list of parameters.
     */
	public ResultSet doQueryPs(Connection conn, String query, Object... parameters)
			throws DbException, SQLException
		{
		String qmsg = query + " with params: ";
		ResultSet rs = null;
		try
		{
			if (conn == null)
			{
				conn = dbi.getConnection(); 
			}
			this.setPrepStmnt(conn.prepareStatement(query));
			int index=1;
			for( Object param: parameters)
			{
				qmsg = qmsg + (param==null?"null":param.toString()) + ", ";
				if (param instanceof Integer)
					this.prepStmnt.setInt(index,(Integer)param);
				else if (param instanceof Long)
					this.prepStmnt.setLong(index,(Long)param);
				else if (param instanceof Double)
					this.prepStmnt.setDouble(index,(Double)param);
				else if (param instanceof String)
					this.prepStmnt.setString(index,(String)param);
				else
					this.prepStmnt.setObject(index,param);
				index++;
			}
			//this.setPrepRs(this.prepStmnt.executeQuery());
			rs = this.prepStmnt.executeQuery();
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query: " + qmsg + ": " + ex);
		}
		return rs;
	}
	
	/**
	 * Lookup an ID using a prepared statement filled with the passed params.
	 * Return null if no result or the ID as a Long integer obect.
	 * @param query
	 * @param params
	 * @return
	 * @throws DbException 
	 */
	public Long lookupId(String query, Object... params) 
		throws DbException
	{
		class LongWrapper { Long id = null; }
		final LongWrapper lw = new LongWrapper();
		Connection conn = null;
		doQueryV(conn, query,
			new ResultSetConsumer()
			{
				@Override
				public void accept(ResultSet rs) throws SQLException
				{
					if (rs.next())
						lw.id = rs.getLong(1);
				}
			
			}, params);
		return lw.id;
	}
	
	/**
	 * Version of doQuery that takes a rs consumer.
	 * @param q
	 * @param consumer
	 * @throws DbException
	 */
	public void doQuery(String q, ResultSetConsumer consumer)
		throws DbException
	{
		ResultSet rs = doQuery(q);
		try { consumer.accept(rs); }
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query '" + q + "'");
		}
	}
	
	/**
	 * This method can be used for most argument validation for words to embed in a 
	 * SQL string. It removes leading/trailing whitespace. If there is embedded white
	 * space, only the first word is returned. Special characters (){}[]'"|, are removed.
	 * @param arg
	 * @return the first word of arg
	 */
	public String getSingleWord(String arg)
	{
		String special = "(){}[]'\"|,";
		
		StringBuilder sb = new StringBuilder(arg.trim());
		for(int idx = 0; idx < sb.length(); idx++)
		{
			char c = sb.charAt(idx);
			if (Character.isWhitespace(c) || special.indexOf(c) >= 0)
				return idx == 0 ? "" : sb.substring(0, idx);
		}
		return sb.toString();
	}
	
	/**
	    * Executes an UPDATE or INSERT query using a prepared statement.
	    * 
	    * @param query the query string
	    * @param parameters any number of object parameters, which are the 
	    *                   bind values of the prepared statement.
	    * @throws DbException  if the update fails.
	    * @return number of records modified in the database
	    */
	public int doModifyV(String query, Object... parameters)
		throws DbException
	{
		String qmsg = "doModifyV " + query + " with params: ";
		try (PreparedStatement stmt = dbi.getConnection().prepareStatement(query);)
		{
			int index=1;
			for( Object param: parameters)
			{
				qmsg = qmsg + (param==null ? "null" : param.toString()) + ", ";
				if (param instanceof Integer)
					stmt.setInt(index,(Integer)param);
				else if (param instanceof Long)
					stmt.setLong(index,(Long)param);
				else if (param instanceof Double)
					stmt.setDouble(index,(Double)param);
				else if (param instanceof String)
					stmt.setString(index,(String)param);
				else
					stmt.setObject(index,param);
				index++;
			}
			debug1(qmsg);
			return stmt.executeUpdate();
		}
		catch(SQLException ex)
		{
			throw new DbException(module, ex, "Error in query: " + qmsg + ": " + ex);
		}
	}
}
