package org.opendcs.utils.sql;

/**
 * This classes is to old developer or advanced configuration settings
 * Set as properties as opposed to intended user facing settings.
 */
public class SqlSettings
{
    /**
     * Determines whether or not to maintain and print track traces of 
     * various java.sql.Connection operations. Primarily @see opendcs.util.sql.WrappedConnection
     * to find instances of createStatement and print stacktraces showing the location.
     *
     * @since 7.0.11 (cwms.connection.pool.trace to be removed)
     */
    public static final boolean TRACE_CONNECTIONS = 
        Boolean.parseBoolean(
            System.getProperty("opendcs.connection.poo..trace",
                System.getProperty("cwms.connection.pool.trace", "false")
                )
        );    
}
