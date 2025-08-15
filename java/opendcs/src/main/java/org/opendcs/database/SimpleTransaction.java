package org.opendcs.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

public final class SimpleTransaction implements DataTransaction
{
    private final Connection conn;
    private final Handle jdbiHandle;

    @SuppressWarnings("java:S2095") // Close is manged by the closing of this object.
    public SimpleTransaction(Connection conn, Jdbi jdbi)
    {

        if (jdbi != null)
        {
            this.jdbiHandle = jdbi.open().begin();
            this.conn = jdbiHandle.getConnection();
        }
        else
        {
            this.conn = conn;
            this.jdbiHandle = null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> connection(Class<T> connectionType) throws OpenDcsDataException {
        if (Connection.class.equals(connectionType) && conn != null)
        {
            return Optional.of((T)conn);
        }
        else if (Handle.class.equals(connectionType) && conn != null)
        {
            return Optional.of((T)jdbiHandle);
        }
        return Optional.empty();

    }

    @Override
    public void commit() throws OpenDcsDataException
    {
        try
        {
            if (jdbiHandle != null)
            {
                jdbiHandle.commit();
            }
            else
            {
                conn.commit();
            }

        }
        catch (JdbiException | SQLException ex)
        {
            throw new OpenDcsDataException("Unable to commit transaction.", ex);
        }
    }

    @Override
    public void rollback() throws OpenDcsDataException
    {
        try
        {
            if (jdbiHandle != null)
            {
                jdbiHandle.rollback();
            }
            else
            {
                conn.rollback();
            }
        }
        catch (JdbiException | SQLException ex)
        {
            throw new OpenDcsDataException("Unable to rollback transaction.", ex);
        }
    }

    @Override
    public void close() throws OpenDcsDataException
    {
        try
        {
            commit();
            if (jdbiHandle != null)
            {
                jdbiHandle.close();
            }
            else
            {
                conn.close();
            }
        }
        catch (JdbiException | SQLException ex)
        {
            throw new OpenDcsDataException("Closing connection failed.", ex);
        }
    }
}