package org.opendcs.database.model;

import java.time.ZonedDateTime;
import decodes.sql.DbKey;

/**
 * Defined Access Role.
 */
public class Role
{
    public final DbKey id;
    public final String name;
    public final String description;
    public final ZonedDateTime updatedAt;


    public Role(DbKey id, String name, String description, ZonedDateTime updatedAt)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
