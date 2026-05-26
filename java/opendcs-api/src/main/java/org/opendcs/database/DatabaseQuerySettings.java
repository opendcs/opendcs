package org.opendcs.database;

import org.opendcs.settings.api.OpenDcsSettings;

public interface DatabaseQuerySettings extends OpenDcsSettings
{
    default boolean numericDate()
    {
        return true; // Reference implementations currently use numeric columns instead of date columns for time.
     
    }
}
