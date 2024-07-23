package opendcs.util.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps a connection and prevents the auto commit state from getting changed.
 */
public class ConnectionInTransaction extends WrappedConnection
{
    public ConnectionInTransaction(Connection realConnection)
    {
        super(realConnection);
    }

    /**
     * So long as the actual state isn't changing, this can be a noop.
     * But only the original creator of the ConnectionInTransaction should
     * be able to alter the commit state.
     * @param state desired state of transaction auto commits.
     */
    public void setAutoCommit(boolean state) throws SQLException
    {
        boolean existing = super.getAutoCommit();
        if (existing != state)
        {
            throw new SQLException("This connection is in a specific transaction. The auto commit state cannot be changed.");
        }
    }
}
