package org.opendcs.database;

import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.openmbean.OpenDataException;
import javax.sql.DataSource;

import org.opendcs.jmx.ConnectionPoolMXBean;
import org.opendcs.jmx.WrappedConnectionMBean;
import org.opendcs.utils.sql.SqlSettings;
import org.slf4j.LoggerFactory;

import opendcs.util.sql.WrappedConnection;

/**
 * This datasource is used for instances were high-performance
 * is not a concern such as the command line or GUI Database migration
 * tools. It should not be used for anything else.
 */
public class SimpleDataSource implements DataSource, ConnectionPoolMXBean
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SimpleDataSource.class);
    final String url;
    final Properties properties;
    PrintWriter pw = null;
    private Connection c = null;
    private Set<WrappedConnection> connections = Collections.synchronizedSet(new HashSet<>());
    private AtomicInteger requests = new AtomicInteger(0);
    private AtomicInteger freed = new AtomicInteger(0);

    public SimpleDataSource(String jdbcUrl, String username, String password)
    {
        this.url = jdbcUrl;
        this.properties = new Properties();
        this.properties.setProperty("username", username);
        this.properties.setProperty("user", username);
        this.properties.setProperty("password", password);
        setupJmx();
    }

    public SimpleDataSource(String jdbcUrl, Properties properties)
    {
        this.url = jdbcUrl;
        this.properties = properties;
        if (properties.getProperty("username") != null)
        {
            this.properties.setProperty("user", properties.getProperty("username"));
        }
        setupJmx();
    }

    private void setupJmx()
    {
        try
		{
            String name = String.format("SimpleDataSource(%s/%s)",url.replace("?","\\?"),properties.getProperty("username", properties.getProperty("user", "<no user>")));
			ManagementFactory.getPlatformMBeanServer()
							 .registerMBean(this, new ObjectName("org.opendcs:type=ConnectionPool,name=\""+name+"\",hashCode=" + this.hashCode()));
		}
		catch(JMException ex)
		{
            log.warn("Unable to register tracking bean.",ex);
		}
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException
    {
        this.pw = out;
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException
    {
    }

    @Override
    public int getLoginTimeout() throws SQLException
    {
        return -1;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        final WrappedConnection wc = new WrappedConnection(
            DriverManager.getConnection(url, properties),
            c ->
            {
                c.getRealConnection().close();
                connections.remove(c);
                freed.getAndIncrement();
                log.trace("connections requeste/freed {}/{}", requests.get(),freed.get());
            },
            SqlSettings.TRACE_CONNECTIONS);
        requests.getAndIncrement();
        log.trace("connections requeste/freed {}/{}", requests.get(),freed.get());
        connections.add(wc);
        return wc;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        final WrappedConnection wc = new WrappedConnection(
            DriverManager.getConnection(url, username, password),
            c ->
            {
                c.getRealConnection().close();
                connections.remove(c);
                freed.getAndIncrement();
                log.trace("connections requeste/freed {}/{}", requests.get(),freed.get());
            },
            SqlSettings.TRACE_CONNECTIONS
        );
        requests.getAndIncrement();
        log.trace("connections requeste/freed {}/{}", requests.get(),freed.get());
        connections.add(wc);
        return wc;
    }

    @Override
    public int getConnectionsOut()
    {
        return connections.size();
    }

    @Override
    public int getConnectionsAvailable()
    {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getThreadName()
    {
        return Thread.currentThread().getName();
    }

    @Override
    public int getGetConnCalled()
    {
        return requests.get();
    }

    @Override
    public int getFreeConnCalled()
    {
        return freed.get();
    }

    @Override
    public int getUnknownReturned()
    {
        return -1;
    }

    @Override
    public int getConnectionsClosedDuringGet()
    {
        return -1;
    }

    @Override
    public WrappedConnectionMBean[] getConnectionsList() throws OpenDataException
    {
        return this.connections.toArray(new WrappedConnection[0]);
    }
}
