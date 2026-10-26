package org.opendcs.database;

import java.sql.Connection;
import java.sql.SQLException;

import decodes.cwms.CwmsConnectionPool;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfo;
import usace.cwms.db.dao.util.connection.ConnectionLoginInfoImpl;

/** Initializes CWMS office/security context before handing a connection to an updater. */
public class CwmsSessionDataSource extends SimpleDataSource
{
    private final String officeId;

    public CwmsSessionDataSource(String url, String username, String password, String officeId)
    {
        super(url, username, password);
        this.officeId = officeId;
    }

    @Override
    public Connection getConnection() throws SQLException
    {
        return getConnection(username, password);
    }

    @Override
    public Connection getConnection(String user, String pass) throws SQLException
    {
        Connection connection = super.getConnection(user, pass);
        try
        {
            initializeSession(connection, new ConnectionLoginInfoImpl(url, user, pass, officeId));
            return connection;
        }
        catch (SQLException | RuntimeException | Error ex)
        {
            try
            {
                connection.close();
            }
            catch (SQLException closeFailure)
            {
                ex.addSuppressed(closeFailure);
            }
            throw ex;
        }
    }

    protected void initializeSession(Connection connection, ConnectionLoginInfo loginInfo) throws SQLException
    {
        CwmsConnectionPool.initializeSession(connection, loginInfo);
    }
}
