package org.opendcs.database.api;

import java.util.Optional;

import org.opendcs.settings.api.OpenDcsSettings;

public interface TransactionContext
{
 
    /**
     * KeyGenerator if auto generated keys aren't used.
     * @return
     */
    <T extends Generator> Optional<T> getGenerator(Class<T> generatorClass);

    /**
     * Retrieve Settings of a given type. All implementations must provide "DecodesSettings".
     * Implementations may determine if a given set of settings are immutable at runtime.
     * @param <T> Type of settings. Currently implemented is "DecodesSettings"
     * @param settingsClass type of settings to get
     * @return Optional&lt;T&gt; Settings if available, otherwise empty.
     */
    <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass);

    /**
     * Which database engine is this
     * @return
     */
    DatabaseEngine getDatabase();
}
