package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.UUID;

public class Role
{
    final UUID id;
    final String name;
    final String description;
    final ZonedDateTime updatedAt;


    public Role(UUID id, String name, String description, ZonedDateTime updatedAt)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
