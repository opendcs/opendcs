package org.opendcs.database.logging;

import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogCreator;

/**
 * Allow us to pipe the logging through the current
 * ilex logger.
 *
 * TODO: can be removed after initial migration to slf4j
 * May be useful to tweak current concept to allow
 * GUI to present errors directly.
 */
public class MigrationLogCreator implements LogCreator
{

    @Override
    public Log createLogger(Class<?> loggerName)
    {
        return new MigrationLog(loggerName);
    }
}
