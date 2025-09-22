package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

public interface IdentityProvider
{
    UUID getId();
    String getName();
    String getType();
    ZonedDateTime getUpdatedAt();
    Map<String, Object> configToMap();
}
