package opendcs.util.functional;

import decodes.db.DatabaseException;

/**
 * Function interval to allow for checked exceptions.
 */
@FunctionalInterface
public interface ThrowingConsumer<T,E extends Exception>
{
    public void accept(T value) throws E, DatabaseException;
}