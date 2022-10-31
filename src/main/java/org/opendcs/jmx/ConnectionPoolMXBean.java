package org.opendcs.jmx;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import opendcs.util.sql.WrappedConnection;

public interface ConnectionPoolMXBean {
    /**
     * How many connections are being actively held for use.
     * @return
     */
    public int getConnectionsOut();
    /**
     * How many connections remain in the pool.
     * @return
     */
    public int getConnectionsAvailable();
    public String getThreadName();
    /**
     * 
     * @return How many times connections were requested.
     */
    public int getGetConnCalled();
    /**
     * 
     * @return How many times connections were returned.
     */
    public int getFreeConnCalled();
    /**
     * 
     * @return number of times a connection this pool didn't generate was returned.
     */
    public int getUnknownReturned();
    /**
     * 
     * @return A pool may sanity check connections on retrieval. Tracks the number of failures.
     */
    public int getConnectionsClosedDuringGet();
    /**
     * For any open connection return the state of each
     * @return A JMX OpenMBeans dataset that includes the lifetime of the connection and the 
     *         stack trace for where it was opened from.
     * @throws OpenDataException
     */
    public WrappedConnectionMBean[] getConnectionsList() throws OpenDataException;
}
