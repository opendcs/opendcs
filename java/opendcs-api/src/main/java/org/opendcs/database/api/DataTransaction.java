package org.opendcs.database.api;

import java.util.Optional;

import javax.management.openmbean.OpenDataException;

/**
 * Instance of this class are used to hold appropriate connections
 * to external systems. Most commonly will be SQL connections for the OpenDCS information.
 * Implementations are free to provide any type required; however, generic implementations
 * will assume SQL based operations.
 *
 * Implementations of this interface *MUST* hold and provide connections to datasources
 * in such a way that all operations are a valid single transaction. For JDBC connection this
 * means instances hold a single {@link java.sql.Connection} or @{link org.jdbi.Handle} for operations.
 *
 * Instances of this class are *NOT* thread safe and should not be shared between threads.
 *
 * SQL Based connections *MUST* be a valid SQL Transaction e.g. setAutoCommit(false).
 * For any additional connection types, transaction support is the responsibility of the implementation.
 *
 */
public interface DataTransaction extends AutoCloseable {

    /**
     * Retrieve a connection of a given type, such as java.sql.Connection or
     * an HTTP client.
     * @param <T> Requested connection type. Implementations should implement support
     *            for the connections they use.
     * @param connectionType
     * @return optional with the instance of the connection type, if available.
     * @throws OpenDcsDataException any issues with the connection, if implementations check validity.
     */
    <T> Optional<T> connection(Class<T> connectionType) throws OpenDcsDataException;

    /**
     * Finalize transaction state across all connections.
     *
     * On successful commit, implementations *MUST* provide a new valid transaction.
     *
     * @throws OpenDcsDataException any issues with finalizing the transaction.
     */
    void commit() throws OpenDcsDataException;

    /**
     * Reset data sources to known state.
     * Transaction *MUST* return to the valid initial state.
     */
    void rollback() throws OpenDcsDataException;

    /**
     * Remove because downstream things need to be responsible for closing any actually connections and such.
     */
    @Override
    void close() throws OpenDcsDataException;
}
