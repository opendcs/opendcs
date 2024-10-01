package org.opendcs.jmx;

public interface WrappedConnectionMBean
{
    public int getRealConnectionHashCode();
    public String getConnectionOpened();
    public long getConnectionLifetimeSeconds();
    public String[] getOpenStackTrace();
    public boolean getTracingOn();
    public String getOpeningThreadName();
}
