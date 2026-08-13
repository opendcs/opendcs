package org.opendcs.data;

import org.opendcs.database.DatabaseKey;

public interface Organization
{
    /**
     * In database identifier for this key
     * @return
     */
    DatabaseKey getId();

    /**
     * Short name of this Organization
     * @return
     */
    String getName();

    /**
     * Full display name of this organization. 
     * Default returns the result of {@see getName}
     * @return
     */
    default String getDisplayName()
    {
        return getName();
    }

    Organization getReportsToOffice();
}
