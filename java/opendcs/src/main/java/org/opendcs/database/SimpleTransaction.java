package org.opendcs.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

public final class SimpleTransaction implements DataTransaction
{
    private final Connection conn;

    public SimpleTransaction(Connection conn)
    {
        this.conn = conn;
    }

    @Override
    public <T> Optional<T> connection(Class<T> connectionType) throws OpenDcsDataException {
        if (Connection.class.equals(connectionType))
        {
            return Optional.of((T)conn);
        }
        return Optional.empty();
        
    }

    @Override
    public void commit() throws OpenDcsDataException
    {
        try 
        {
            conn.commit();
        }
        catch (SQLException ex) 
        {
            throw new OpenDcsDataException("Unable to commit transaction.", ex);
        }
    }

    @Override
    public void rollback() throws OpenDcsDataException
    {
        try
        {
            conn.rollback();
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Unable to rollback transaction.", ex);
        }
    }

    @Override
    public void close() throws OpenDcsDataException
    {
        try
        {
            conn.close();
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Closing connection failed.", ex);
        }
    }
}