package org.opendcs.data.lrgs;

import java.util.Map;

import org.opendcs.data.TracksChanges;
import org.opendcs.database.DatabaseKey;

public interface LrgsDataInterface extends TracksChanges
{
    DatabaseKey getId();

    String getName();

    LrgsDataInterfaceType getType();
    
    /**
     * Mapped JSON Configuration from or for database.
     * Implementations that expand their configuration
     * need to make sure that this returns the current
     * configuration for storage to the database.
     * @return
     */
    Map<String, Object> getConfig();
}
