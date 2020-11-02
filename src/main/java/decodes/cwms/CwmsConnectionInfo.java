package decodes.cwms;

import java.sql.Connection;

import decodes.sql.DbKey;

/**
 * Bean returned by CwmsTimeSeriesDb.getDbConnection().
 */
public class CwmsConnectionInfo
{
	private Connection connection = null;
	private DbKey dbOfficeCode = DbKey.NullKey;

	public CwmsConnectionInfo()
	{
	}

	public Connection getConnection()
	{
		return connection;
	}

	public void setConnection(Connection connection)
	{
		this.connection = connection;
	}

	public DbKey getDbOfficeCode()
	{
		return dbOfficeCode;
	}

	public void setDbOfficeCode(DbKey dbOfficeCode)
	{
		this.dbOfficeCode = dbOfficeCode;
	}

}
