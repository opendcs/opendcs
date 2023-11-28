package org.opendcs.implementations.xml;

import org.apache.calcite.avatica.InternalProperty;
import org.apache.calcite.jdbc.CalciteConnection;
import org.opendcs.authentication.AuthSourceService;

import decodes.db.DatabaseConnectException;
import decodes.db.DatabaseException;
import decodes.sql.KeyGenerator;
import decodes.sql.SqlDatabaseIO;
import decodes.util.DecodesSettings;
import ilex.util.AuthException;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import opendcs.util.sql.WrappedConnection;

import java.util.Properties;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.sql.DataSource;

public class XmlSqlDatabaseIO extends SqlDatabaseIO
{
    private DataSource ds;


	public XmlSqlDatabaseIO(String location) throws DatabaseException
	{
		super();
		keyGenerator = null;
		connectToDatabase(location);
	}

	@Override
	public Connection getConnection()
	{
		try
		{
			return ds.getConnection();
		}
		catch (SQLException ex)
		{
			throw new RuntimeException("Unable to retrieve connection", ex);
		}
	}

    /**
	 * Initialize the database connection using the username and password
	 * provided in the hidden DECODES auth file.
	 * @param sqlDbLocation URL (may vary for different DBs)
	 */
    @Override
	public void connectToDatabase(String sqlDbLocation)
		throws DatabaseException
	{
		// Placeholder for connecting from web where connection is from a DataSource.
		if (sqlDbLocation == null || sqlDbLocation.trim().length() == 0)
			return;
		_sqlDbLocation = sqlDbLocation;

		String url = EnvExpander.expand("jdbc:calcite:model=$DCSTOOL_HOME/schema/xml/model.json;caseSensitive=false; schema.dir="+sqlDbLocation);
		// Load the JDBC Driver Class
		String driverClass = "org.apache.calcite.jdbc.Driver";
		try
		{
			Logger.instance().debug3("initializing driver class '" + driverClass + "'");
			Class.forName(driverClass);
			final Connection c = DriverManager.getConnection(url);
			determineVersion(c);
			setDBDatetimeFormat(c);
			postConnectInit();
			ds  = new DataSource()
			{
				private Connection conn = c;
				private PrintWriter pw = null;

				@Override
				public PrintWriter getLogWriter() throws SQLException
				{
					return pw;
				}

				@Override
				public void setLogWriter(PrintWriter out) throws SQLException
				{
					pw = out;
				}

				@Override
				public void setLoginTimeout(int seconds) throws SQLException
				{
					// do nothing
				}

				@Override
				public int getLoginTimeout() throws SQLException
				{
					return 0;
				}

				@Override
				public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException
				{
					throw new SQLFeatureNotSupportedException();
				}

				@Override
				public <T> T unwrap(Class<T> iface) throws SQLException
				{
					throw new SQLException("This is not a supported operation.");
				}

				@Override
				public boolean isWrapperFor(Class<?> iface) throws SQLException
				{
					return false;
				}

				@Override
				public Connection getConnection() throws SQLException
				{
					return new WrappedConnection(conn, c -> {});
				}

				@Override
				public Connection getConnection(String username, String password) throws SQLException
				{
					return getConnection();
				}
			};
		}
		catch (Exception ex)
		{
			String msg = "Cannot load JDBC driver class '" + driverClass + "': " + ex;
			Logger.instance().fatal(msg);
			throw new DatabaseConnectException(msg, ex);
		}
	}
}
