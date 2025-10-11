package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.Map;

import decodes.sql.DbKey;

/**
 * Source of information regarding user identities.
 */
public interface IdentityProvider
{
    DbKey getId();
    String getName();
    String getType();
    ZonedDateTime getUpdatedAt();
    Map<String, Object> configToMap();
}
