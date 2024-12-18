package org.opendcs.database.api;


import java.util.Optional;

import org.opendcs.settings.api.OpenDcsSettings;

public interface OpenDcsDatabase
{
    /**
     * Retrieve an instance of the given legacy database type.
     * @param <T> Type of Legacy Database. `decodes.sql.Database` or `TimeSeriesDb` or one of its derivatives.
     * @param legacyDatabaseType class reference to the desired database type.
     * @return Optional&lt;T&gt; that contains the Instance, or empty if not available.
     */
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

    <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass);
}
