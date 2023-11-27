package org.opendcs.implementations.xml;

import org.opendcs.authentication.AuthSourceService;

import decodes.db.DatabaseConnectException;
import decodes.db.DatabaseException;
import decodes.sql.KeyGenerator;
import decodes.sql.SqlDatabaseIO;
import decodes.util.DecodesSettings;
import ilex.util.AuthException;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;

import javax.sql.DataSource;

public class XmlSqlDatabaseIO extends SqlDatabaseIO
{
    private DataSource ds;


	public XmlSqlDatabaseIO(String location)
	{
		super();
		keyGenerator = null;
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

		String url = EnvExpander.expand("jdbc:calcite:model=$DCSTOOL_HOME/schema/xml/model.json?dir="+sqlDbLocation);
		// Load the JDBC Driver Class
		String driverClass = "org.apache.calcite.jdbc.Driver";
		try
		{
			Logger.instance().debug3("initializing driver class '" + driverClass + "'");
			Class.forName(driverClass);
            try (Connection c = DriverManager.getConnection(url);)
			{
				determineVersion(c);
				setDBDatetimeFormat(c);
				postConnectInit();
			}
		}
		catch (Exception ex)
		{
			String msg = "Cannot load JDBC driver class '" + driverClass + "': " + ex;
			Logger.instance().fatal(msg);
			throw new DatabaseConnectException(msg);
		}
	}
}
