package org.opendcs.database.impl.cwms.jdbi.mapper.exception;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

/**
 * CWMS always runs on Oracle, so the same standard Oracle integrity-constraint
 * error codes handled for the generic Oracle-backed OpenDCS implementation
 * (see {@code OpenDcsExceptionHandler} in opendcs-implementation) apply here too.
 *
 * CWMS's own PL/SQL packages also raise user-defined error codes (via
 * raise_application_error, conventionally in the -20000..-20999 range) for
 * business-rule constraints -- e.g. a location still referenced elsewhere --
 * that aren't backed by a plain database foreign key. None of those are
 * enumerated yet: they need to be identified against the CWMS schema/package
 * source (and verified against a real database) before being added here,
 * rather than guessed at.
 *
 * This handler is registered after the generic {@code OpenDcsConstraintSqlExceptionHandler},
 * so it runs first; anything it doesn't recognize falls through to that handler
 * and finally the base handler.
 */
public final class CwmsExceptionMapper implements SqlExceptionHandler
{
    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        throwForOracleErrorCode(ex.getErrorCode(), ex);
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
