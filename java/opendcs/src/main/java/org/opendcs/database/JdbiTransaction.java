package org.opendcs.database;

import java.sql.Connection;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;

import opendcs.util.sql.WrappedConnection;

public class JdbiTransaction implements DataTransaction
{
    final Handle jdbiHandle;

    public JdbiTransaction(Handle handle)
    {
        this.jdbiHandle = handle;
        if (!this.jdbiHandle.isInTransaction())
        {
            this.jdbiHandle.begin();
        }
    }

    @SuppressWarnings("unchecked") // types are checked before any operations happen.
    @Override
    public <T> Optional<T> connection(Class<T> connectionType) throws OpenDcsDataException
    {
        if (Connection.class.isAssignableFrom(connectionType))
        {
            return (Optional<T>)Optional.of(new WrappedConnection(jdbiHandle.getConnection(), c -> {}));
        }
        else if (Handle.class.equals(connectionType))
        {
            return (Optional<T>)Optional.of(jdbiHandle);
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public void commit() throws OpenDcsDataException
    {
        jdbiHandle.commit();
    }

    @Override
    public void rollback() throws OpenDcsDataException
    {
        jdbiHandle.rollback();
    }

    @Override
    public void close() throws OpenDcsDataException
    {
        commit();
        jdbiHandle.close();
    }
    
}
