package opendcs.dai;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

import decodes.tsdb.DbIoException;
import opendcs.dao.DaoBase;
import opendcs.dao.DaoHelper;
import opendcs.util.functional.DaoConsumer;

public interface DaiBase extends AutoCloseable
{
    /**
     * 
     * @param q
     * @return
     * @throws DbIoException
     * @deprecated This function should not be as is don't not handled the user of bind variables
     */
    @Deprecated
	public ResultSet doQuery(String q) throws DbIoException;
	
    /**
     * 
     * @param q
     * @return
     * @throws DbIoException
     * @deprecated This function should not be as is don't not handled the user of bind variables
     */
    @Deprecated
	public ResultSet doQuery2(String q) throws DbIoException;

    /**
     * 
     * @param q
     * @return
     * @throws DbIoException
     * @deprecated This function should not be as is don't not handled the user of bind variables
     */
    @Deprecated
	public int doModify(String q) throws DbIoException;

	public void close();
	
	public void setManualConnection(Connection conn);

    /**
     * This method should never be called from outside the DAO instance.
     *
     * @deprecated This function exists to support migration to a stateless model. Once
     *             all usages of the DAOs pass in the transaction this will be removed.
     */
    @Deprecated
    default DataTransaction getTransaction() throws OpenDcsDataException
    {
        return null;
    }

	/**
     * When used within the transaction block of another Dao allow this to assume the same connection.
     * @param other
     * @throws IllegalStateException if the other Dao does not already have a connection open this operation is not valid.
     */
    public void  inTransactionOf(DaoBase other) throws IllegalStateException;

	/**
     * Run a set of queryies with a specific connection in a transaction.
     * Use the presented dao for all operations.
     *
     * The presented Dao is a {@link DaoHelper} which will be very picky
     * about which SQL functions can be called.
     *
     * @param consumer given a new DAO that is manually set to a JDBC transaction.
     * @throws SQLException
     */
    public void inTransaction(DaoConsumer consumer) throws Exception;
}
