package decodes.cwms;

import java.sql.Connection;

import decodes.sql.DbKey;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;

/**
 * Bean returned by CwmsTimeSeriesDb.getDbConnection().
 */
public class CwmsConnectionInfo
{
	private Connection connection = null;
	private DbKey dbOfficeCode = DbKey.NullKey;
	private ConnectionLoginInfo loginInfo = null;
	private String dbOfficePrivilege = null;

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

	public void setLoginInfo(ConnectionLoginInfo loginInfo)
	{
		this.loginInfo = loginInfo;
	}

	public ConnectionLoginInfo getLoginInfo()
	{
		return loginInfo;
	}

	public String getDbOfficePrivilege()
	{
		return dbOfficePrivilege;
	}

	public void setDbOfficePrivilege(String dbOfficePrivilege)
	{
		this.dbOfficePrivilege = dbOfficePrivilege;
	}

}
