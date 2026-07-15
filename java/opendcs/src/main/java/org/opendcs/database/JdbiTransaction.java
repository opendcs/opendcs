package org.opendcs.database;

import java.sql.Connection;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleListener;
import org.jdbi.v3.core.transaction.TransactionException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.TransactionContext;

import opendcs.util.sql.WrappedConnection;

public final class JdbiTransaction implements DataTransaction
{
    final Handle jdbiHandle;
    final TransactionContext context;

    public JdbiTransaction(Handle handle, TransactionContext context)
    {
        this.context = context;
        this.jdbiHandle = handle;
        if (!this.jdbiHandle.isInTransaction())
        {
            this.jdbiHandle.begin();
        }
    }

    @SuppressWarnings("unchecked") // types are checked before any operations happen.
    @Override
    public <T extends AutoCloseable> Optional<T> connection(Class<T> connectionType) throws OpenDcsDataException
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
        if (jdbiHandle.isInTransaction())
        {
            try
            {
                jdbiHandle.commit();
            }
            catch (TransactionException ex)
            {
                try
                {
                    this.rollback();
                }
                catch (Exception rbEx)
                {
                    ex.addSuppressed(rbEx);
                }
                throw new OpenDcsDataException("Unable to commit transaction.", ex);
            }
        }
    }

    @Override
    public void rollback() throws OpenDcsDataException
    {
        if (jdbiHandle.isInTransaction())
        {
            try
            {
                jdbiHandle.rollback();
            }
            catch (TransactionException ex)
            {
                throw new OpenDcsDataException("Unable to rollback transaction.", ex);
            }
        }
    }

    @Override
    public void close() throws OpenDcsDataException
    {
        commit();
        jdbiHandle.close();
    }

    @Override
    public TransactionContext getContext()
    {
        return this.context;
    }
}
