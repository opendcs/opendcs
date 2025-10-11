package org.opendcs.database.model;

import java.time.ZonedDateTime;
import decodes.sql.DbKey;

/**
 * Defined Access Role.
 */
public class Role
{
    final DbKey id;
    final String name;
    final String description;
    final ZonedDateTime updatedAt;


    public Role(DbKey id, String name, String description, ZonedDateTime updatedAt)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
