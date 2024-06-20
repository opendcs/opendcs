package org.opendcs.database;

import java.util.Iterator;

import decodes.sql.DbKey;
import opendcs.dao.CachableDbObject;
import opendcs.dao.DaoBase;

public interface DbObjectCache<DBT extends CachableDbObject> {

    /**
     * Place an object in the cache.
     * @param dbObj the object to cache
     */
    void put(DBT dbObj);

    /**
     * Removes an object from the cache.
     *
     * @param key DbKey of the object to be removed.
     */
    void remove(DbKey key);

    /**
     * Retrieve an object by its surrogate database key
     * @param key the surrogate database key
     * @return the object
     */
    DBT getByKey(DbKey key);

    /**
     * Used by caches like in ComputationDAO that must also check a last modify
     * time in the database to determine if a cached object needs to be reloaded.
     * Before returning an object from the cache, the dao's check method is consulted.
     * @param key The key of the object
     * @param daoBase the DAO
     * @return the object or null if cached object doesn't exist or is not OK.
     */
    DBT getByKey(DbKey key, DaoBase daoBase);

    /**
     * Retrieve an object by its unique name
     * @param uniqueName the unique name
     * @return the object
     */
    DBT getByUniqueName(String uniqueName);

    public default CacheKey<DBT> getKey(DbKey key, String uniqueName)
    {
        return new CacheKey<>(key, uniqueName);
    }

    public default CacheKey<DBT> getKey(DbKey key)
    {
        return new CacheKey<>(key, null);
    }

    public default CacheKey<DBT> getKey(String uniqueName)
    {
        return new CacheKey<>(null, uniqueName);
    }

    /**
     * Used by caches like in ComputationDAO that must also check a last modify
     * time in the database to determine if a cached object needs to be reloaded.
     * Before returning an object from the cache, the dao's check method is consulted.
     * @param uniqueName The unique name of the object
     * @param daoBase the DAO
     * @return the object or null if cached object doesn't exist or is not OK.
     */
    DBT getByUniqueName(String uniqueName, DaoBase daoBase);

    int size();

    /**
     * Searches the cache for an object that is 'equal to' the passed comparator.
     * This method does a linear search of the objects in the cache, so it is not
     * necessary that the Comparable be consistent in a sorting sense.
     * @param cmp The comparator
     * @return a matching object, or null if non is found.
     */
    DBT search(Comparable<DBT> cmp);

    /**
     * @return iterator into the list of cached values.
     */
    Iterator<DBT> iterator();

    /**
     * Completely clear the cache.
     */
    void clear();

    void setMaxAge(long maxAge);
    
}