package org.opendcs.database.impl.opendcs.jdbi.plugins;

import java.sql.Connection;
import java.sql.SQLException;

import org.jdbi.v3.core.spi.JdbiPlugin;

/**
 * Simple plugin to handle forcing the autocommit state on Jdbi connections
 * so the transaction management is left up to the {@see org.opendcs.database.api.DataTransaction}
 * implementation.
 * 
 * This intentionally doesn't have an automatic service annotation or META-INF
 * entry to prevent unintended usage.
 */
public class ConnectionAutoCommitOff implements JdbiPlugin
{
    @Override    
    public Connection customizeConnection(Connection conn) throws SQLException
    {
        conn.setAutoCommit(false);
        return conn;
    }
}
