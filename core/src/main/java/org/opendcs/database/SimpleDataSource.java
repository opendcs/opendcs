package org.opendcs.database;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * This datasource is used for instances were high-performance
 * is not a concern such as the command line or GUI Database migration
 * tools. It should not be used for anything else.
 */
public class SimpleDataSource implements DataSource
{
    final String url;
    final String username;
    final String password;
    PrintWriter pw = null;

    public SimpleDataSource(String jdbcUrl, String username, String password)
    {
        this.url = jdbcUrl;
        this.username = username;
        this.password = password;
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
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException
    {
        return DriverManager.getConnection(url, username, password);
    }
}
