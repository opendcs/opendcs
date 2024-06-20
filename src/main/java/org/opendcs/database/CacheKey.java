package org.opendcs.database;

import decodes.sql.DbKey;
import java.util.Objects;

public class CacheKey<DBT>
{
    final private DbKey key;
    final private String uniqueName;

    public CacheKey(DbKey key, String uniqueName)
    {
        if (key == null)
        {
            this.key = DbKey.NullKey;
        }
        else
        {
            this.key = key;
        }
        this.uniqueName = uniqueName;
    }

    @Override
    public boolean equals(Object rhs)
    {
        if (rhs instanceof CacheKey<?>)
        {
            CacheKey<?> other = (CacheKey<?>)rhs;
            if (this.key.equals(other.key))
            {
                return true;
            }
            else
            {
                return this.uniqueName.equals(other.uniqueName);
            }
        }
        else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(key, uniqueName);
    }
}
