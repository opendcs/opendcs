package org.opendcs.data.lrgs;

import java.time.ZonedDateTime;

import org.opendcs.data.TracksChanges;
import org.opendcs.database.DatabaseKey;

/**
 * NOTE: it is possible we should just use DbEnum for this since we're starting to intergrate
 * the systems. For now will leave as is, may change as the PR itself expands into database
 * operations.
 * LrgsDataSourceType
 * @param id Identifier for this entry
 * @param name Friendly name of this database type
 * @param serviceProvider Service provider to lookup and create an instance
 */
public final record LrgsDataInterfaceType(DatabaseKey id, String name, String serviceProvider,
                                       ZonedDateTime createdAt, ZonedDateTime updatedAt)
    implements TracksChanges
{
}
