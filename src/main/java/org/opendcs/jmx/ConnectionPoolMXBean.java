package org.opendcs.jmx;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import opendcs.util.sql.WrappedConnection;

public interface ConnectionPoolMXBean {
    public int getConnectionsOut();
    public int getConnectionsAvailable();
    public String getThreadName();
    public int getGetConnCalled();
    public int getFreeConnCalled();
    public int getUnknownReturned();
    public TabularData getConnectionsList() throws OpenDataException;    
}
