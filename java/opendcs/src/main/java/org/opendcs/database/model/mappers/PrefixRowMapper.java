package org.opendcs.database.model.mappers;

import org.jdbi.v3.core.mapper.RowMapper;

/**
 * Helper class for row mappers to take a prefix.
 * If the provided prefix does not start end with and underscore (_)
 * one will be added.
 */
public abstract class PrefixRowMapper<T> implements RowMapper<T>
{
    protected final String prefix;

    protected PrefixRowMapper(String prefix)
    {
        if (prefix == null || prefix.trim().isEmpty())
        {
            this.prefix = "";
        }
        else
        {
            this.prefix = addUnderscoreIfMissing(prefix);
        }
    }

    public static String addUnderscoreIfMissing(String prefix)
    {
        return prefix.endsWith("_") ? prefix : (prefix + "_");
    }
}
