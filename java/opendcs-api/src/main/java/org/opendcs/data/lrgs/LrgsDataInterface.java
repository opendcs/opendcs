package org.opendcs.data.lrgs;

import java.time.ZonedDateTime;
import java.util.Map;

import org.opendcs.data.TracksChanges;
import org.opendcs.database.DatabaseKey;

public record LrgsDataInterface(DatabaseKey id, String name, LrgsDataInterfaceType type, Map<String, Object> config,
                                ZonedDateTime createdAt, ZonedDateTime updatedAt) implements TracksChanges
{
}
