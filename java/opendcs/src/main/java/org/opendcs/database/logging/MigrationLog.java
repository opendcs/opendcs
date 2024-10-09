package org.opendcs.database.logging;

import java.io.PrintStream;

import org.flywaydb.core.api.logging.Log;

import ilex.util.Logger;

public class MigrationLog implements Log
{
    private static final Logger logger = Logger.instance();
    private final String prefix;

    public MigrationLog(Class<?> loggerName)
    {
        prefix = loggerName.getName();
    }
    @Override
    public void debug(String msg)
    {
        logger.debug1(prefix + ": " + msg);
    }

    @Override
    public void error(String msg)
    {
        logger.failure(msg);
    }

    @Override
    public void error(String msg, Exception ex)
    {
        logger.failure(prefix + ": " + msg);
        PrintStream ps = logger.getLogOutput();
        if(ps != null)
        {
            ex.printStackTrace(ps);
        }
    }

    @Override
    public void info(String msg)
    {
        logger.info(prefix + ": " + msg);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.getMinLogPriority() < Logger.E_INFORMATION;
    }

    @Override
    public void notice(String msg)
    {
        logger.info(prefix + ": " + msg);
    }

    @Override
    public void warn(String msg)
    {
        logger.warning(prefix + ": " + msg);
    }
}
