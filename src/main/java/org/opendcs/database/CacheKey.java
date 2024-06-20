package org.opendcs.database;

import decodes.sql.DbKey;
import java.util.Objects;

public class CacheKey<DBT> implements Comparable<CacheKey<DBT>>
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
        return key.hashCode();
    }

    @Override
    public int compareTo(CacheKey<DBT> o)
    {
        if (!DbKey.isNull(key))
        {
            return key.compareTo(o.key);
        }
        else if (uniqueName != null)
        {
            return uniqueName.compareTo(o.uniqueName);
        }
        else
        {
            return -1;
        }
    }
}
