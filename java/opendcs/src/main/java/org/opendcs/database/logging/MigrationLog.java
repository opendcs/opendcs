package org.opendcs.database.logging;

import org.flywaydb.core.api.logging.Log;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;


public class MigrationLog implements Log
{
    private final Logger logger;

    public MigrationLog(Class<?> loggerName)
    {
        logger = OpenDcsLoggerFactory.getLogger(loggerName);
    }
    @Override
    public void debug(String msg)
    {
        logger.debug(msg);
    }

    @Override
    public void error(String msg)
    {
        logger.error(msg);
    }

    @Override
    public void error(String msg, Exception ex)
    {
        logger.atError().setCause(ex).log(msg);
    }

    @Override
    public void info(String msg)
    {
        logger.info(msg);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public void notice(String msg)
    {
        logger.info(msg);
    }

    @Override
    public void warn(String msg)
    {
        logger.warn(msg);
    }
}
