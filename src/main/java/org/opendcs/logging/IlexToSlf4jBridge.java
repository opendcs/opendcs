package org.opendcs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class IlexToSlf4jBridge extends ilex.util.Logger
{
    private Logger realLogger = null;

    public IlexToSlf4jBridge(String procName)
    {
        super(procName, E_DEBUG3); // The bridge will capture everything.
        realLogger = LoggerFactory.getLogger(procName);
    }

    @Override
    public void close()
    {
        // nothing to do.
    }

    @Override
    public void doLog(int priority, String text)
    {
        Level level = ilexLevelToSLF4J(priority);
        realLogger.makeLoggingEventBuilder(level)
                  .setMessage(text)
                  .log();
    }

    private Level ilexLevelToSLF4J(int priority)
    {
        switch(priority)
        {
            case ilex.util.Logger.E_WARNING: return Level.WARN;
            case ilex.util.Logger.E_DEBUG1: return Level.DEBUG;
            case ilex.util.Logger.E_DEBUG2: // intentional pass through
            case ilex.util.Logger.E_DEBUG3: return Level.TRACE;
            case ilex.util.Logger.E_FAILURE: return Level.ERROR;
            case ilex.util.Logger.E_FATAL: return Level.ERROR;
            case ilex.util.Logger.E_INFORMATION: ;// intentional pass through (includes DEFAULT MIN)
            default: return Level.INFO;
        }
    }
}
