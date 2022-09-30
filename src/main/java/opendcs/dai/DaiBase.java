package opendcs.dai;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;

import decodes.tsdb.DbIoException;
import opendcs.util.functional.BatchStatementConsumer;
import opendcs.util.functional.ConnectionConsumer;
import opendcs.util.functional.ResultSetConsumer;
import opendcs.util.functional.ResultSetFunction;
import opendcs.util.functional.StatementConsumer;
import opendcs.util.functional.ThrowingSupplier;

public interface DaiBase
	extends AutoCloseable
{
	public ResultSet doQuery(String q)
		throws DbIoException;
	
	public ResultSet doQuery2(String q) 
		throws DbIoException;

	public int doModify(String q)
		throws DbIoException;

	public void close();
	
	public void setManualConnection(Connection conn);


	public void withConnection(ConnectionConsumer consumer) throws SQLException;

	/**
	 * Prepare a statement and let caller deal with setting parameters and calling the execution
	 *
	 * @see DaoBase#withConnection(ConnectionConsumer) for thread safety
	 * @param statement SQL statement
	 * @param consumer Function that will handle the operations. @see opendcs.util.functional.StatementConsumer
	 */
	public void withStatement( String statement, StatementConsumer consumer,Object... parameters) throws SQLException;

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
	public void doQuery(String query, ResultSetConsumer consumer, Object... parameters) throws SQLException;

	/**
	 * perform an update or insert operations with given parameters.
	 * @param query SQL query passed directly to prepareStatement
	 * @param args variables to bind on the statement in order.
	 * @return number of rows affected
	 * @throws SQLException anything goes wrong talking to the database
	 */
	public int doModify(String query, Object... args) throws SQLException;

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
	public <R> R getSingleResult(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException;

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
	public <R,E extends Exception> R getSingleResultOr(String query, ResultSetFunction<R> onValidRs,ThrowingSupplier<R,E> onNoResult, Object... parameters ) throws SQLException, E;

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
	public  <R> List<R> getResults(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException;	

	/**
	 * Same as getResults except null values are removed from the
	 * generated list.
	 * @param query SQL query with ? for bind vars.
	 * @param consumer Function that Takes a ResultSet and returns an instance of R
	 * @param parameters arg list of query inputs
	 * @returns Object of type R determined by the caller.
	 * @throws SQLException any goes during during the creation, execution, or processing of the query.
	 */
	public <R> List<R> getResultsIgnoringNull(String query, ResultSetFunction<R> consumer, Object... parameters ) throws SQLException;

	/**
	 * Helper to wrap batch calls for various insert/delete operations
	 * @param query the query with bind vars
	 * @param batchSize how many items to add to each batch
	 * @param consumer operation that sets the appropriate columns
	 * @param items list of items to insert
	 * @throws SQLException
	 */
	public <R> void doBatch(String query, int batchSize, BatchStatementConsumer<R> consumer, Iterable<R> items) throws SQLException;
}
