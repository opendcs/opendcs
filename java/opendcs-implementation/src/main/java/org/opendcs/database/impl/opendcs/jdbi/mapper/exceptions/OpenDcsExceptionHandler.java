package org.opendcs.database.impl.opendcs.jdbi.mapper.exceptions;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

/**
 * Implementation-specific companion to the generic
 * {@code OpenDcsConstraintSqlExceptionHandler}, which only recognizes ANSI
 * SQLStates. The generic OpenDCS backend (this module) can run against either
 * Postgres or Oracle; Postgres already sets standard SQLStates and is covered
 * by the generic handler, but Oracle's JDBC driver does not always set one, so
 * its vendor-specific error codes are checked here instead.
 *
 * This handler is registered after the generic one, so it runs first; anything
 * it doesn't recognize falls through to the generic handler and finally the
 * base handler.
 */
public final class OpenDcsExceptionHandler implements SqlExceptionHandler
{
    private final DatabaseEngine engine;

    public OpenDcsExceptionHandler(DatabaseEngine engine)
    {
        this.engine = engine;
    }

    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        if (engine == DatabaseEngine.ORACLE)
        {
            throwForOracleErrorCode(ex.getErrorCode(), ex);
        }
        // Not a code we recognize; let the next handler in the chain deal with it.
    }

    private static void throwForOracleErrorCode(int errorCode, SQLException ex)
    {
        switch (errorCode)
        {
            // ORA-00001: unique constraint violated
            case 1: throw new UniqueConstraintViolationException(ex);
            // ORA-02292: integrity constraint violated - child record found
            // ORA-02291: integrity constraint violated - parent key not found
            case 2292:
            case 2291: throw new RelatedDataConstraintException(ex);
            default: // not a constraint code we recognize
        }
    }
}
