package org.opendcs.odcsapi.dao.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;


/**
 * This class is a wrapper around a DataSource that delegates all calls to the
 * wrapped DataSource.  It is intended to be extended by classes that need to
 * override DataSource methods.
 */
public class DelegatingDataSource implements DataSource
{

	private DataSource delegate;

	/**
	 * Create a new DelegatingDataSource.
	 *
	 * @param delegate the target DataSource
	 */
	public DelegatingDataSource(DataSource delegate)
	{
		setDelegate(delegate);
	}


	/**
	 * Set the target DataSource that this DataSource should delegate to.
	 */
	public void setDelegate(DataSource delegate)
	{
		this.delegate = delegate;
	}

	/**
	 * Return the target DataSource that this DataSource should delegate to.
	 */

	public DataSource getDelegate()
	{
		return this.delegate;
	}


	@Override
	public Connection getConnection() throws SQLException
	{
		return getDelegate().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException
	{
		return getDelegate().getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException
	{
		return getDelegate().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException
	{
		getDelegate().setLogWriter(out);
	}

	@Override
	public int getLoginTimeout() throws SQLException
	{
		return getDelegate().getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException
	{
		getDelegate().setLoginTimeout(seconds);
	}


	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		if(iface.isInstance(this))
		{
			return (T) this;
		}
		return getDelegate().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return (iface.isInstance(this) || getDelegate().isWrapperFor(iface));
	}


	@Override
	public Logger getParentLogger()
	{
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
