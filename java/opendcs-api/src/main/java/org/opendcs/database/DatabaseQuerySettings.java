package org.opendcs.database;

import org.opendcs.settings.api.OpenDcsSettings;

/**
 * For settings that are static per implementation but are difficult to determine automatically.
 * Implementations should implement this class an overwrite as appropriate.
 *
 * Additional properties should be added to this class with a default method
 * provided.
 */
public interface DatabaseQuerySettings extends OpenDcsSettings
{
    DatabaseQuerySettings DEFAULT_SETTINGS = new DatabaseQuerySettings()
    {
        /* just use the defaults */
    };

    /**
     * Whether date columns in the database are stored a SQL Integer values or SQL Date types.
     */
    default boolean numericDate()
    {
        return true; // Reference implementations currently use numeric columns instead of date columns for time.
    }
}
