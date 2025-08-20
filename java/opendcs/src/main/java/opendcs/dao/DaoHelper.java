package opendcs.dao;

import java.sql.Connection;
import java.sql.ResultSet;

import decodes.tsdb.DbIoException;

/**
 * A simple Version of a dao used when only a specific connection instance
 * should be used AND only the new handlers.
 *
 * It is used to constrain connection usage inside a transaction.
 * NOTE: This class is extremely unfriendly to misuse and will
 * throw exception when incorrect calls are made.
 */
public class DaoHelper extends DaoBase
{
    public DaoHelper(DatabaseConnectionOwner tsdb, String module, Connection con)
    {
        super(tsdb, module, con);
    }

    /**
     * @deprecated Use the "(String query, Object ...parameters)" version.
     *             You can do (query,new Object[0]) when there aren't parameters.
     *             DO NOT use that to bypass the use of bind variables.
     */
    @Deprecated
    @Override
    public ResultSet doQuery(String q) throws DbIoException
    {
        throw new DbIoException("Use the new handlers when using this class");
    }

    /**
     * @deprecated Use the "(String query, Object ...parameters)" version.
     *             You can do (query,new Object[0]) when there aren't parameters.
     *             DO NOT use that to bypass the use of bind variables.
     */
    @Deprecated
    public ResultSet doQuery2(String q) throws DbIoException
    {
        throw new DbIoException("Use the new handlers when using this class");
    }

    /**
     * @deprecated Use the "(String query, Object ...parameters)" version.
     *             You can do (query,new Object[0]) when there aren't parameters.
     *             DO NOT use that to bypass the use of bind variables.
     */
    @Deprecated
    public int doModify(String q) throws DbIoException
    {
        throw new DbIoException("Use the new handlers when using this class");
    }

    /**
     * Connection should be set in constructor and never changed.
     */
    @Override
    @Deprecated
    public void setManualConnection(Connection conn)
    {
        throw new RuntimeException("A call to setManual connection was made on "
                                + "a helper class were such use is not intended.");
    }

    @Override
    public Connection getConnection()
    {
        /** bypass DaoBase get connection as we know it MUST be valid. */
        return this.myCon;
    }
}
