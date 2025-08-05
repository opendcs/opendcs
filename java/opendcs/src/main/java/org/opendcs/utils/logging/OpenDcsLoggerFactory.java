package org.opendcs.utils.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface OpenDcsLoggerFactory
{
    /**
     * Get a Logger instance using calling class of this function.
     * @return
     */
    static Logger getLogger()
    {
        StackTraceElement ste[] = Thread.currentThread().getStackTrace();
        return LoggerFactory.getLogger(ste[2].getClassName());
    }

    /**
     * Get a Logger instance using the exact name provided.
     * @param logName
     * @return
     */
    static Logger getLogger(String logName)
    {
        return LoggerFactory.getLogger(logName);
    }

    /**
     * Get a Logger instance based on the given class.
     * @param clazz
     * @return
     */
    static Logger getLogger(Class<?> clazz)
    {
        return LoggerFactory.getLogger(clazz);
    }
}
