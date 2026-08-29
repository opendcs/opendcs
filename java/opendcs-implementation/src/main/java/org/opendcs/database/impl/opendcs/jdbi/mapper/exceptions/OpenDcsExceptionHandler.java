package org.opendcs.database.impl.opendcs.jdbi.mapper.exceptions;

import java.sql.SQLException;

import org.jdbi.v3.core.statement.SqlExceptionHandler;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;

public final class OpenDcsExceptionHandler implements SqlExceptionHandler
{

    @Override
    public void handle(SQLException ex, StatementContext ctx)
    {
        if (ex.getMessage().contains("violates foreign key"))
        {
            throw new RelatedDataConstraintException(ex);
        }
    }
}
