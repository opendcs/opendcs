package org.opendcs.database.impl.opendcs.jdbi.plugins;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.OpenDcsDataRuntimeException;

/**
 * Minimum handler to get connection errors to something we can
 * more directly process.
 *
 * {@see org.opendcs.database.AbstractJdbiOpenDcsDatabaseWrapper} will
 * always register this handler first. Implementation can and should
 * create more detail implementations that throw appropriate specific 
 * exceptions as can be determined.
 *
 * OpenDcsBaseSqlExceptionHandler
 */
public final class OpenDcsBaseSqlExceptionHandler implements SqlExceptionHandler
{
    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        throw new OpenDcsDataRuntimeException("Error during query operation", ex);
    }   
}
