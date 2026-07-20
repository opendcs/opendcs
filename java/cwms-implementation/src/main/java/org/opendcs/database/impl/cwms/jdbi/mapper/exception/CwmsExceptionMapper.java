package org.opendcs.database.impl.cwms.jdbi.mapper.exception;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;

public final class CwmsExceptionMapper implements SqlExceptionHandler
{

    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        if (ex.getMessage().contains("integrity constraint"))
        {
            throw new RelatedDataConstraintException(ex);
        }
    }
}
