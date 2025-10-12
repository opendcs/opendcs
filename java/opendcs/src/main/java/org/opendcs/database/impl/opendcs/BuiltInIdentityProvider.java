package org.opendcs.database.impl.opendcs;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.opendcs.database.model.IdentityProvider;

import decodes.sql.DbKey;

public class BuiltInIdentityProvider implements IdentityProvider
{
    public static final String TYPE = "BuiltIn";
    private final DbKey id;
    private final String name;
    private final ZonedDateTime updatedAt;

    public BuiltInIdentityProvider(DbKey id, String name, ZonedDateTime updateAt, Map<String, Object> configMap)
    {
        this.id = id;
        this.name = name;
        this.updatedAt = updateAt;
        // then process config
    }
    

    @Override
    public DbKey getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    @Override
    public Map<String, Object> configToMap()
    {
        return new HashMap<>();
    }
    
}
