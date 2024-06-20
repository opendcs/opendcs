/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.opendcs.database;

import java.util.Objects;

import decodes.sql.DbKey;

/**
 * A composite key type that can be used by some cache implementations
 * to allow storing by the full key,uniqueName set but searching by either.
 * 
 * NOTE: This does not work for HashMap as the hash function could never
 * output the correctly value in both cases. (Well, not without some work anyways.)
 */
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

    /**
     * Loose equality. If only one set needs to match of the other set has a null value.
     */
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

    /**
     * Get the hashCode of the complete data set.
     * A CacheKey that contains only one of key or uniqueName
     * will not map to the same hash.
     * 
     * ... Unless someone figures that out.
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(key, uniqueName);
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
