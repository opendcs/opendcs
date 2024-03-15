package org.opendcs.database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import java.util.Properties;

import javax.sql.DataSource;

import opendcs.util.sql.WrappedConnection;

/**
 * This datasource is used for instances were high-performance
 * is not a concern such as the command line or GUI Database migration
 * tools. It should not be used for anything else.
 */
public class SimpleDataSource implements DataSource
{
    final String url;
    final Properties properties;
    PrintWriter pw = null;
    private Connection c = null;

    public SimpleDataSource(String jdbcUrl, String username, String password)
    {
        this.url = jdbcUrl;
        this.properties = new Properties();
        this.properties.setProperty("username", username);
        this.properties.setProperty("user", username);
        this.properties.setProperty("password", password);
    }

    public SimpleDataSource(String jdbcUrl, Properties properties)
    {
        this.url = jdbcUrl;
        this.properties = properties;
        if (properties.getProperty("username") != null)
        {
            this.properties.setProperty("user", properties.getProperty("username"));
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
        if (c == null)
        {
            c = new WrappedConnection(DriverManager.getConnection(url, properties), c -> {}, true);
        }
        return c;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return DriverManager.getConnection(url, username, password);
    }
}
