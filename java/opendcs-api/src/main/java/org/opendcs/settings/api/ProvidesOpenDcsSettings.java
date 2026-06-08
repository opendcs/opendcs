package org.opendcs.settings.api;

import java.util.Optional;

public interface ProvidesOpenDcsSettings
{
    /**
     * Retrieve Settings of a given type. All implementations must provide {@see DecodesSettings}
     * and {@see DatabaseQuerySettings}. Other types of settings are optional.
     *
     * Implementations may determine if a given set of settings are immutable at runtime.
     *
     * @param <T> Type of settings. Currently implemented is "DecodesSettings"
     * @param settingsClass type of settings to get
     * @return Optional&lt;T&gt; Settings if available, otherwise empty.
     */
    public <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass);
}
