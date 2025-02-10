package org.opendcs.database.api;


import java.util.Optional;

import org.opendcs.settings.api.OpenDcsSettings;

/**
 * Basic interface of an "OpenDCS Database interface.""
 */
public interface OpenDcsDatabase
{
    /**
     * Retrieve an instance of the given legacy database type.
     * @param <T> Type of Legacy Database. `decodes.sql.Database` or `TimeSeriesDb` or one of its derivatives.
     * @param legacyDatabaseType class reference to the desired database type.
     * @return Optional&lt;T&gt; that contains the Instance, or empty if not available.
     * @deprecated This is provided for transition, new implementations should return an empty optional.
     */
    @Deprecated
    <T> Optional<T> getLegacyDatabase(Class<T> legacyDatabaseType);
    
    /**
     * Retrieve DAO from the database
     * @param <T> DAO Type
     * @param dao Dao Class
     * @return A valid instance for this database, or an empty optional if the DAO is not supported
     */
    <T extends OpenDcsDao> Optional<T> getDao(Class<T> dao);

    /**
     * Start a new transaction to perform data source operations.
     * @return a valid DataTransaction containing any connections required to perform operations
     * @throws OpenDcsDataException if any issues creating the transaction.
     */
    DataTransaction newTransaction() throws OpenDcsDataException;

    /**
     * Retrieve Settings of a given type. All implementations must provide "DecodesSettings".
     * Implementations may determine if a given set of settings are immutable at runtime.
     * @param <T> Type of settings. Currently implemented is "DecodesSettings"
     * @param settingsClass type of settings to get
     * @return Optional&lt;T&gt; Settings if available, otherwise empty.
     */
    <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass);
}
