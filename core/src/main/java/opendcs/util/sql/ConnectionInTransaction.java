package opendcs.util.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a connection and prevets the auto commit state from getting changed.
 */
public class ConnectionInTransaction extends WrappedConnection
{
    public ConnectionInTransaction(Connection realConnection)
    {
        super(realConnection);
    }

    public void setAutoCommit(boolean state) throws SQLException
    {
        throw new SQLException("This connection is in a specific transaction. The auto commit state cannot be changed.");
    }    
}
