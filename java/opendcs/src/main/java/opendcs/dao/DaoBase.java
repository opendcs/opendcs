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
import opendcs.util.functional.ConnectionConsumer;
import opendcs.util.functional.DaoConsumer;
import opendcs.util.functional.ResultSetConsumer;
import opendcs.util.functional.ResultSetFunction;
import opendcs.util.functional.StatementConsumer;
import opendcs.util.functional.ThrowingFunction;
import opendcs.util.sql.ConnectionInTransaction;
import opendcs.util.sql.WrappedConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.opendcs.utils.sql.SqlSettings;

import decodes.db.Constants;
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
        // Code intentionally duplicated so this form can be used
        // within DaoHelper for transactions.
        this.conSetManually = true;
        this.myCon = con;
    }

    /**
     * When used within the transaction block of another Dao allow this to assume the same connection.
     *
     * NOTE: DAOs calling this should be created and discarded within the transaction block.
     *
     * @param other DAO with the in-transaction connection.
     * @throws IllegalStateException if the other Dao does not already have a connection open this operation is not valid.
     */
    public void  inTransactionOf(DaoBase other) throws IllegalStateException
    {
        if (other.myCon == null)
        {
            throw new IllegalStateException("Provided DAO does not currently have a valid connection that would be in a transaction.");
        }
        this.setManualConnection(other.myCon);
    }

    /**
     * Assert what connection will be used for this DAOs operations.
     * Caller is responsible for cleaning up the Connection object.
     */
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
        {
            try { queryStmt1.close(); } catch(Exception ex) {}
        }
        if (queryStmt2 != null)
        {
            try { queryStmt2.close(); } catch(Exception ex) {}
        }
        queryStmt1 = queryStmt2 = null;

        // for pooling: return the connection (if there is one) back to the pool.
        if (myCon != null && !conSetManually)
        {
            db.freeConnection(myCon);
        }
        myCon = null;
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
            // If the connection is closed or invalid so are these objects.
            this.queryStmt1 = null;
            this.queryStmt2 = null;
            this.queryResults1 = null;
            this.queryResults2 = null;
            try
            {
                myCon = db.getConnection();
            }
            catch (SQLException ex)
            {
                throw new RuntimeException("Unable to get connection.", ex);
            }
        }


        return new WrappedConnection(myCon, c -> {}, SqlSettings.TRACE_CONNECTIONS);
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
     * Does a SQL query with the default static statement &amp; returns the
     * result set.
     * Warning: this method is not thread and nested-loop safe.
     * If you need to do nested queries, you must create a separate
     * statement and do the inside query yourself. Likewise, if called
     * from multiple threads, an external synchronization mechanism is
     * needed.
     * @param q the query
     * @return the result set
     * @deprecated Do not use for use code
     */
    @Deprecated
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
            throw new DbIoException(msg);
        }
    }

    /**
     *
     *  An extra do-query for inside-loop queries.
     * @Deprecated do not use for new code
     */
    @Deprecated
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
            String msg = "SQL Error in query '" + q + "': " + ex;
            warning(msg);
            throw new DbIoException(msg);
        }
    }



    /**
    * Executes an UPDATE or INSERT query.
    *
    * Thread safety: No gaurantees. Uses one statement but may share connection depending on OpenDCS implementation.
    *
    * @param q the query string
    * @throws DbIoException  if the update fails.
    * @return number of records modified in the database
    */
    public int doModify(String q)
        throws DbIoException
    {
        try(Connection conn = getConnection();
            Statement modStmt = conn.createStatement();)
        {
            debug3("Executing statement '" + q + "'");
            return modStmt.executeUpdate(q);
        }
        catch(SQLException ex)
        {
            String msg = "SQL Error in modify query '" + q + "': " + ex;
            throw new DbIoException(msg);
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
        try { return db.getKeyGenerator().getKey(tableName, getConnection()); }
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

    /**
     * Provides connection to consumer, closing/return the connection when done unless.
     * Connection was manually set. As would happen with a transaction.
     *
     * NOTE: Thread safe IF the TimeseriesDB implementation supports connection pooling AND
     *  you aren't calling in parallel from an "inTransaction" handler.
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
            if(conn != null && !this.conSetManually)
            {
                conn.close();
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
    public void withStatement( String statement, StatementConsumer consumer, Object... parameters) throws SQLException
    {
        withConnection((conn) -> {
            try (PreparedStatement stmt = conn.prepareStatement(statement);)
            {
                if (fetchSize > 0)
                {
                    stmt.setFetchSize(fetchSize);
                }
                bind(stmt, parameters);
                consumer.accept(stmt);
            }
        });

    }

    private void bind(PreparedStatement stmt, Object... parameters) throws SQLException
    {
        int index=1;

        for( Object param: parameters)
        {
            if (param instanceof Integer)
            {
                stmt.setInt(index,(Integer)param);
            }
            else if (param instanceof Boolean)
            {
                Boolean v = (Boolean)param;
                String value = "";
                // There is a db.sqlBoolean but it tries to
                // be helpful and wraps the return in ' ' which
                // we don't need or want here.
                // A callback to the "TimeseriesDatabase" class
                // is probably the best solution for the data that various
                // so they can take responsibility. That's also roughly
                // how JDBI will work. That'll be handled as part of
                // a separate PR.
                if (db.isOracle())
                {
                    value = v ? "Y" : "N";
                }
                else
                {
                    value = v ? "TRUE" : "FALSE";
                }
                stmt.setString(index, value);
            }
            else if (param instanceof String)
            {
                stmt.setString(index,(String)param);
            }
            else if (param instanceof Character)
            {
                stmt.setString(index, ((Character)param).toString());
            }
            else if (param instanceof DbKey)
            {
                DbKey key = (DbKey)param;
                if (DbKey.isNull(key))
                {
                    stmt.setNull(index,Types.NULL);
                }
                else
                {
                    stmt.setLong(index,key.getValue());
                }
            }
            else if (param instanceof Date)
            {
                if (this.db.isOpenTSDB())
                {
                    stmt.setLong(index,((Date)param).getTime());
                }
                else
                {
                    stmt.setDate(index,new java.sql.Date(((Date)param).getTime()));
                }
            }
            else if (param == null)
            {
                stmt.setNull(index,Types.NULL);
            }
            else
            {
                try
                {
                    stmt.setObject(index,param);
                }
                catch (SQLException ex)
                {
                    String msg = String.format(
                            "Attempting to set parameter of type '%'"
                        + ". Please open an issue on the project page with this error message.",
                        param.getClass().getName());
                    throw new SQLException(msg, ex);
                }
            }
            index++;
        }
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
     * Perform a repeated SQL query with the given list of values and a bindingFunction
     * @param <ValueType> Object we are binding to the query
     * @param query The query to use repeatedly
     * @param bindingFunction A function that takes the ValueType and creates a Object[] list in bind variable order
     * @param values List of values that will be used.
     * @param batchSize How many elements of the list to execute for each batch.
     * @throws SQLException
     */
    public <ValueType> void doModifyBatch(String query, ThrowingFunction<ValueType, Object[] , SQLException> bindingFunction, Collection<ValueType> values, int batchSize) throws SQLException
    {
        withStatement(query, (stmt) -> {
            int count = 0;
            for (ValueType v: values)
            {
                bind(stmt, bindingFunction.accept(v));
                stmt.addBatch();
                if(++count % batchSize == 0)
                {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }
            if (count % batchSize != 0)
            {
                stmt.executeBatch();
            }
        });
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
     * @throws SQLException if anything goes bad during the creation, execution, or processing of the query. Or if more than one result is returned
     */
    public <R> R getSingleResult(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException
    {
        return getSingleResultOr(query, consumer, null, parameters);
    }

    /**
     * Given a query string and bind variables execute the query.
     * The provided function should process the single valid result set and return an object R.
     *
     * The query should return a single result.
     *
     * @param query SQL query with ? for bind vars.
     * @param consumer Function that Takes a ResultSet and returns an instance of R.
     * @param defaultValue An instance of type R to return if nothing is found in the database.
     * @param parameters arg list of query inputs
     * @returns Object of type R determined by the caller. If no results in the database, defaultValue is returned,
     *             other wise the result of the consumer is returned.
     * @throws SQLException any goes during during the creation, execution, or processing of the query. Or if more than one result is returned.
     */
    public <R> R getSingleResultOr(String query, ResultSetFunction<R> consumer, R defaultValue, Object... parameters ) throws SQLException
    {
        final ArrayList<R> result = new ArrayList<>();
        withStatement(query,(stmt)->{
            try(ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    result.add(consumer.accept(rs));
                    if (rs.next())
                    {
                        throw new SQLException(String.format("Query '%s' returned more than one row",query));
                    }
                }
            }
        },parameters);
        if (result.isEmpty())
        {
            return defaultValue;
        }
        else
        {
            return result.get(0);
        }
    }

    /**
     * A query that may return more than one result; but we only care about the first one.
     *
     * @param <R> The return type
     * @param query query that may return more than 1 result
     * @param consumer function to take the ResultSet and process it into a Object of type R (or null)
     * @param parameters variables to bind into the query.
     * @return value provided by the consumer or null
     * @throws SQLException
     */
    public <R> R getFirstResult(String query, ResultSetFunction<R> consumer, Object... parameters) throws SQLException
    {
        final ArrayList<R> result = new ArrayList<>();
        withStatement(query,(stmt)->{
            try(ResultSet rs = stmt.executeQuery())
            {
                if (rs.next())
                {
                    result.add(consumer.accept(rs));
                    return;
                }
            }
        },parameters);
        if (result.size() == 1)
        {
            return result.get(0);
        }
        else
        {
            return null;
        }
    }

    /**
     * A query that may return more than one result; but we only care about the first one.
     *
     * @param <R> The return type
     * @param query query that may return more than 1 result
     * @param consumer function to take the ResultSet and process it into a Object of type R (or null)
     * @param defaultvalue value to provide if no query results
     * @param parameters variables to bind into the query.
     * @return The value from the query, or the defaultValue if the query returns nothing
     * @throws SQLException
     */
    public <R> R getFirstResultOr(String query, ResultSetFunction<R> consumer, R defaultValue, Object... parameters) throws SQLException
    {
        R value = getFirstResult(query, consumer, parameters);
        if (value == null)
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    /**
     * Given a query string and bind variables execute the query.
     * The provided function should process the single valid result set and return an object R.
     * Each return will be added to a list and returned, including null.
     *
     * @param query SQL query with ? for bind vars.
     * @param consumer Function that Takes a ResultSet and returns an instance of R
     * @param parameters arg list of query inputs
     * @returns Object of type R determined by the caller.
     * @throws SQLException any goes during during the creation, execution, or processing of the query.
     */
    public <R> List<R> getResults(String query, ResultSetFunction<R> consumer, Object... parameters) throws SQLException
    {
        return getResults(query, consumer, false, parameters);
    }

    /**
     * Given a query string and bind variables execute the query.
     * The provided function should process the single valid result set and return an object R.
     * Each return will be added to a list and returned. Null values returned will not be added to the list.
     *
     * @param query SQL query with ? for bind vars.
     * @param consumer Function that Takes a ResultSet and returns an instance of R
     * @param parameters arg list of query inputs
     * @returns Object of type R determined by the caller.
     * @throws SQLException any goes during during the creation, execution, or processing of the query.
     */
    public <R> List<R> getResultsIgnoringNull(String query, ResultSetFunction<R> consumer, Object... parameters) throws SQLException
    {
        return getResults(query, consumer, true, parameters);
    }

    private <R> List<R>  getResults(String query, ResultSetFunction<R> consumer, boolean ignoreNull, Object... parameters) throws SQLException
    {
        final ArrayList<R> result = new ArrayList<>();
        withStatement(query,(stmt)->{
            try(ResultSet rs = stmt.executeQuery())
            {
                while(rs.next())
                {
                    R tmp = consumer.accept(rs);
                    if (tmp != null || ignoreNull == false)
                    {
                        result.add(tmp);
                    }
                }
            }
        },parameters);
        return result;
    }

    /**
     * Run a set of queries with a specific connection in a transaction.
     * Use the presented dao for all operations.
     *
     * The presented Dao is a {@link DaoHelper} which will be very picky
     * about which SQL functions can be called.
     *
     * If any component attempts to alter the auto commit state of the connection during
     * the transaction an exception will be thrown.
     *
     * @param consumer given a new DAO that is manually set to a JDBC transaction.
     * @throws SQLException
     */
    public void inTransaction(DaoConsumer consumer) throws Exception
    {
        Connection c = getConnection();
        boolean autoCommit = c.getAutoCommit();
        c.setAutoCommit(false);
        try (DaoBase dao = new DaoHelper(this.db,"transaction",new ConnectionInTransaction(c));)
        {
            consumer.accept(dao);
            c.commit();
        }
        catch (Exception ex)
        {
            c.rollback();
            throw ex;
        }
        finally
        {
            c.setAutoCommit(autoCommit);
            c.close();
        }
    }
}
