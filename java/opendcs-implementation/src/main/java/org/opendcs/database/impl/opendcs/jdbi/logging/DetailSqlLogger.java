package org.opendcs.database.impl.opendcs.jdbi.logging;

import org.jdbi.v3.core.statement.SqlLogger;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;

/**
 * This is a helper class that we can use or not during the development
 * of various DAOs. At any given time analyzers may see this class as unused.
 * Don't remove it.
 * DetailSqlLogger
 */
@SuppressWarnings("java:S1312") // Logger here is provided by point of usage.
public final class DetailSqlLogger implements SqlLogger
{
    private final Logger log;

    public DetailSqlLogger(Logger log)
    {
        this.log = log;
    }

    @Override
    public void logBeforeExecution(StatementContext ctx)
    {
        log.atError().log(ctx.getRawSql());
    }    
}
