package org.opendcs.database.model.mappers;

import org.jdbi.v3.core.mapper.RowMapper;

public abstract class PrefixRowMapper<T> implements RowMapper<T>
{
    protected final String prefix;

    protected PrefixRowMapper(String prefix)
    {
        this.prefix = (prefix == null || prefix.trim().isEmpty()) ? ""
                    : (prefix.endsWith("_") ? prefix : prefix+"_");
    }
}
