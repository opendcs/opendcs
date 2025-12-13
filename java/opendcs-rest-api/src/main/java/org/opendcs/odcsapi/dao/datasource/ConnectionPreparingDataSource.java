package org.opendcs.odcsapi.dao.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public class ConnectionPreparingDataSource extends DelegatingDataSource
{

	private ConnectionPreparer preparer;

	public ConnectionPreparingDataSource(ConnectionPreparer preparer, DataSource targetDataSource)
	{
		super(targetDataSource);
		this.preparer = preparer;
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		Connection connection = getDelegate().getConnection();

		try
		{
			return getPreparer().prepare(connection);
		}
		catch(RuntimeException e)
		{
			try
			{
				// If there was some problem preparing the connection
				// we close the connection in order to return it to
				// the pool it probably came from.
				connection.close();
			}
			catch(SQLException ex)
			{
				e.addSuppressed(ex);
			}
			throw e;
		}
	}

	/**
	 * @return the preparer
	 */
	public ConnectionPreparer getPreparer()
	{
		return preparer;
	}

	/**
	 * @param preparer the preparer to set
	 */
	public void setPreparer(ConnectionPreparer preparer)
	{
		this.preparer = preparer;
	}


}
