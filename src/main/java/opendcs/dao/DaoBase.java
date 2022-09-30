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
import opendcs.util.functional.BatchStatementConsumer;
import opendcs.util.functional.ConnectionConsumer;
import opendcs.util.functional.ResultSetConsumer;
import opendcs.util.functional.ResultSetFunction;
import opendcs.util.functional.StatementConsumer;
import opendcs.util.functional.ThrowingSupplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.tsdb.DbIoException;

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
	protected Connection myCon = null;
	private boolean conSetManually = false;

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
	 * Constructor for subordinate DAOs.
	 * The parent DAO shares its connection with the
	 * subordinate after creation. Then the subordinate close() will not free
	 * the connection. For connection pooling systems this can reduce number of
	 * open connections.
	 * @param tsdb
	 * @param module
	 */
	public DaoBase(DatabaseConnectionOwner tsdb, String module, Connection con)
	{
		this.db = tsdb;
		this.module = module;
		setManualConnection(con);
	}
	
	public void setManualConnection(Connection con)
	{
		this.myCon = con;
		conSetManually = true;
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
		
		// for pooling: return the connection (if there is one) back to the pool.
		if (myCon != null && !conSetManually)
			db.freeConnection(myCon);
		myCon = null;
	}


	/**
	 * So that local functions can follow the usual workflow
	 * @param conn Connection given from this Dao instance's getConnection
	 */
	private void closeConnection(Connection conn)
	{
		if( myCon != null && !conSetManually)
		{
			db.freeConnection(conn);
			myCon = null;
		}
	}
	
	public void finalize()
	{
		close();
	}
	
	protected Connection getConnection()
	{
		// local getConnection() method that saves the connection locally
		if (myCon == null || connectionClosed() )
		{
			Logger.instance().debug3("DaoBase -> resetting connection");
			// If the connection is closed or invalid so are these objects.
			this.queryStmt1 = null;
			this.queryStmt2 = null;
			this.queryResults1 = null;
			this.queryResults2 = null;
			myCon = db.getConnection();
		}
			
			
		return myCon;
	}

	/**
	 * 
	 * @return connection state (open -> false, closed -> true)
	 */
	private boolean connectionClosed()
	{
		try{
			return myCon.isClosed();
		} catch( SQLException err){
			// There is no compelling reason here to distinguish between a failed and closed connection.						
			return true;	
		}
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
			if (queryStmt1 == null || statementInvalid(queryStmt1))
				queryStmt1 = getConnection().createStatement();
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
			throw new DbIoException(msg,ex);
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
			if (queryStmt2 == null|| statementInvalid(queryStmt2))
				queryStmt2 = getConnection().createStatement();
			debug3("Query2 '" + q + "'");
			return queryResults2 = queryStmt2.executeQuery(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in query '" + q + "'";
			warning(msg);
			throw new DbIoException(msg,ex);
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
			modStmt = getConnection().createStatement();
			debug3("Executing statement '" + q + "'");
			return modStmt.executeUpdate(q);
		}
		catch(SQLException ex)
		{
			String msg = "SQL Error in modify query '" + q + "': " + ex;
			throw new DbIoException(msg,ex);
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
		final DbKey ret[] = new DbKey[1];
		ret[0] = Constants.undefinedId;
		try
		{
			withConnection( conn -> {
				try
				{
					ret[0] = db.getKeyGenerator().getKey(tableName, getConnection());
				}
				catch(DatabaseException ex)
				{
					throw new SQLException("failed to generate key",ex);
				}
			});
			return ret[0];
		}
		catch(SQLException ex)
		{
			throw new DbIoException("Failed to get key for table",ex);
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

	/**
	 * Provides connection to consumer, closing/return the connection when done.
	 *
	 * NOTE: Thread safe IF the TimeseriesDB implementation supports connection pooling.
	 * @param consumer @see opendcs.util.functional.ConnectionConsumer
	 * @throws SQLException
	 */
	public void withConnection(ConnectionConsumer consumer) throws SQLException
	{
		Connection conn = null;
		try
		{
			conn = this.getConnection();
			consumer.accept(conn);
		}
		finally
		{
			if( conn != null)
			{
				this.closeConnection(conn);
			}
		}
	}

	/**
	 * Prepare a statement and let caller deal with setting parameters and calling the execution
	 *
	 * @see DaoBase#withConnection(ConnectionConsumer) for thread safety
	 * @param statement SQL statement
	 * @param consumer Function that will handle the operations. @see opendcs.util.functional.StatementConsumer
	 */
	public void withStatement( String statement, StatementConsumer consumer,Object... parameters) throws SQLException
	{
		withConnection((conn) -> {
			try (PreparedStatement stmt = conn.prepareStatement(statement);)
			{
				if (fetchSize > 0)
				{
					stmt.setFetchSize(fetchSize);;
				}
				int index=1;
				for( Object param: parameters)
				{
					if (param instanceof Integer)
					{
						stmt.setInt(index,(Integer)param);
					}
					else if (param instanceof String)
					{
						setStmtString(stmt, index, (String)param);
						stmt.setString(index,(String)param);
					}
					else if (param instanceof NullableParameter<?>){
						NullableParameter<?> p = (NullableParameter<?>)param;
						if (p.type().equals(String.class))
						{
							setStmtString(stmt, index, (String)p.get());
						}
						else if (p.type().equals(Date.class))
						{
							setStmtDate(stmt, index,(Date)p.get());
						}
						else
						{
							throw new SQLException(String.format("Unable to save type %s, for parameter at index '%d'"
															     + " for query '%s'",p.type().getName(),index,statement));
						}
					}
					else if (param instanceof DbKey)
					{
						DbKey key = (DbKey)param;
						if( key.isNull() )
						{
							stmt.setNull(index, java.sql.Types.NUMERIC);
						}
						else
						{
							stmt.setLong(index,key.getValue());
						}
					}
					else if( param instanceof Date)
					{
						setStmtDate(stmt,index,(Date)param);
					}
					else
					{
						stmt.setObject(index,param);
					}
					index++;
				}

				consumer.accept(stmt);
			}
		});

	}	

	/**
	 * Prepare and run a statement with the given parameters.
	 * Each element of the result set is passed to the consumer
	 *
	 * System will make best effort to automatically convert datatypes.
	 * @see DaoBase#withConnection(ConnectionConsumer) for thread safety
	 * @param query SQL Query string with ? for bind variables
	 * @param consumer User provided function to use the result set. @see opendcs.util.functional.ResultSetConsumer
	 * @param parameters Variables for the query
	 * @throws SQLException
	 */
	public void doQuery(String query, ResultSetConsumer consumer, Object... parameters) throws SQLException
	{
		withStatement(query, (stmt) -> {
			try (ResultSet rs = stmt.executeQuery();)
			{
				while(rs.next())
				{
					consumer.accept(rs);
				}
			}
		},parameters);
	}

	/**
	 * perform an update or insert operations with given parameters.
	 * @param query SQL query passed directly to prepareStatement
	 * @param args variables to bind on the statement in order.
	 * @return number of rows affected
	 * @throws SQLException anything goes wrong talking to the database
	 */
	public int doModify(String query, Object... args) throws SQLException
	{
		int result[] = new int[1];
		result[0] = 0;
		withStatement(query, (stmt) -> {
			result[0] = stmt.executeUpdate();
		},args);
		return result[0];
	}

	/**
	 * Given a query string and bind variables execute the query.
	 * The provided function should process the single valid result set and return an object R.
	 *
	 * The query should return a single result.
	 *
	 * @param query SQL query with ? for bind vars.
	 * @param consumer Function that Takes a ResultSet and returns an instance of R
	 * @param parameters arg list of query inputs
	 * @returns Object of type R determined by the caller.
	 * @throws SQLException any goes during during the creation, execution, or processing of the query. Or if more than one result is returned
	 */
	public <R> R getSingleResult(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException
	{
		return getSingleResultOr(query,consumer, () -> null,parameters);
	}

		/**
	 * Given a query string and bind variables execute the query.
	 * The provided function should process the single valid result set and return an object R.
	 *
	 * The query should return a single result.
	 *
	 * @param query SQL query with ? for bind vars.
	 * @param onValidRs Function that Takes a ResultSet and returns an instance of R
	 * @param onNoResult Function that returns desired value on no result
	 * @param parameters arg list of query inputs
	 * @returns Object of type R determined by the caller.
	 * @throws SQLException any goes during during the creation, execution, or processing of the query. Or if more than one result is returned
	 */
	public <R,E extends Exception> R getSingleResultOr(String query, ResultSetFunction<R> onValidRs,ThrowingSupplier<R,E> onNoResult, Object... parameters ) throws SQLException, E
	{
		final ArrayList<R> result = new ArrayList<>();
		withStatement(query,(stmt)->{
			try(ResultSet rs = stmt.executeQuery())
			{
				if (rs.next())
				{
					result.add(onValidRs.accept(rs));
					if (rs.next())
					{
						throw new SQLException(String.format("Query '%s' returned more than one row",query));
					}
				}
			}
		},parameters);
		if( result.isEmpty() )
		{
			return onNoResult.get();
		}
		else
		{
			return result.get(0);
		}
	}


	/**
	 * Given a query string and bind variables execute the query.
	 * The provided function should process the single valid result set and return an object R.
	 * Each return will be added to a list and returned.
	 * @param query SQL query with ? for bind vars.
	 * @param consumer Function that Takes a ResultSet and returns an instance of R
	 * @param parameters arg list of query inputs
	 * @returns Object of type R determined by the caller.
	 * @throws SQLException any goes during during the creation, execution, or processing of the query.
	 */
	public <R> List<R> getResults(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException
	{
		final ArrayList<R> result = new ArrayList<>();
		withStatement(query,(stmt)->{
			try(ResultSet rs = stmt.executeQuery())
			{
				while(rs.next())
				{
					result.add(consumer.accept(rs));
				}
			}
		},parameters);
		return result;
	}

	/**
	 * Same as getResults except null values are removed from the
	 * generated list.
	 * @param query SQL query with ? for bind vars.
	 * @param consumer Function that Takes a ResultSet and returns an instance of R
	 * @param parameters arg list of query inputs
	 * @returns Object of type R determined by the caller.
	 * @throws SQLException any goes during during the creation, execution, or processing of the query.
	 */
	public <R> List<R> getResultsIgnoringNull(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException
	{
		return getResults(query, consumer, parameters)
				.stream()
				.filter(r -> r != null)
				.collect(Collectors.toList());
	}

	/**
	 * Helper to wrap batch calls for various insert/delete operations
	 * @param query the query with bind vars
	 * @param batchSize how many items to add to each batch
	 * @param consumer operation that sets the appropriate columns
	 * @param items list of items to insert
	 * @throws SQLException
	 */
	public <R> void doBatch(String query, int batchSize, BatchStatementConsumer<R> consumer, Iterable<R> items) throws SQLException
	{
		withStatement(query, stmt -> {
			int count = 0;
			for(R item: items)
			{
				stmt.clearParameters();
				consumer.accept(stmt,item);
				stmt.addBatch();
				count++;
				if( count % batchSize == 0)
				{
					stmt.executeBatch();
				}
			}
			if( count % batchSize != 0)
			{
				stmt.executeBatch();
			}
		});
	}

	/**
	 * Wrapper for possibly null text values
	 * @param stmt
	 * @param index
	 * @param value
	 * @throws SQLException
	 */
	private void setStmtString(PreparedStatement stmt, int index, String value)
		throws SQLException
	{
		stmt.setString(index,value);
	}

	/**
	 * Wrapper for handling possibly Null Date values
	 * @param stmt PrepareStatement
	 * @param index 
	 * @param value java.util.Date value, can be null
	 */
	private void setStmtDate(PreparedStatement stmt, int index, Date value)
		throws SQLException
	{
		if( value == null )
		{
			stmt.setNull(index,
							db.isOpenTSDB()
							? java.sql.Types.NUMERIC
							: java.sql.Types.DATE
							);
		}
		else if( db.isOpenTSDB() )
		{
			stmt.setLong(index,value.getTime());
		}
		else
		{
			stmt.setDate(index,new java.sql.Date(value.getTime()));
		}
	}

	/**
	 * Generate the requisite number of ?, for a sql insert statement 
	 * given the length of the provided list
	 * Will correctly leave of the last , for last value
	 * @param list used to get the count of objects we need the bind placeolder for.
	 * @return String with a series of "?[,?]""
	 */
	public String valueBinds(Collection<? extends Object> list)
	{
		StringBuilder sb = new StringBuilder();
		for( int i = 0; i < list.size(); i++)
		{
			sb.append("?");
			if (i < (list.size() - 1))
			{
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * Helper to allow for null values so the preparedstatement code and match types correctly.
	 */
	public static class NullableParameter<T extends Object> {
		private T value;
		private Class<T> clazz;

		private NullableParameter(T value, Class<T> clazz)
		{
			this.value = value;
			this.clazz = clazz;
		}

		/**
		 * Creates a wrapped value for the database that may or may not be null.
		 * saves the datatype separately in cases of null values
		 * @param <T> Actual data type
		 * @param value value that can be null
		 * @param clazz the Class represenation for type T
		 * @return NullableParameter that can be passed as a DaoBase "parameter"
		 */
		public static <T> NullableParameter<T> of(T value, Class<T> clazz)
		{
			return new NullableParameter<T>(value,clazz);
		}

		/**
		 * 
		 * @return the wrapped object
		 */
		public T get()
		{
			return this.value;
		}
		
		/**
		 * 
		 * @return the type class
		 */
		public Class<T> type()
		{
			return clazz;
		}
	}
}
