package org.opendcs.database.api;

import java.util.Optional;

import org.opendcs.settings.api.ProvidesOpenDcsSettings;

public interface TransactionContext extends ProvidesOpenDcsSettings
{
    /**
     * KeyGenerator alternative to auto generated keys.
     * @return
     */
    <T extends Generator> Optional<T> getGenerator(Class<T> generatorClass);

    /**
     * Which database engine is this
     * @return
     */
    DatabaseEngine getDatabaseEngine();

    /* settings retrieval defined by ProvidesOpenDcsSettings interface */
}
