package org.opendcs.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import decodes.util.DecodesSettings.DbTypes;
import opendcs.dao.CachableDbObject;
import opendcs.dao.DaoBase;

public class SimpleCacheTest {
    
    @Test
    void test_cache_key() throws Exception
    {
        DbObjectCache<TestDbObject> testCache = new SimpleCache<>();
        final String searchString = "two";

        final TestDbObject object1 = new TestDbObject(DbKey.createDbKey(1), "Object1", "one");
        final TestDbObject object2 = new TestDbObject(DbKey.createDbKey(2), "Object2", searchString);

        testCache.put(object1);
        testCache.put(object2);

        assertEquals(2, testCache.size());

        final TestDbObject result1Key = testCache.getByKey(object1.getKey());
        assertNotNull(result1Key, "Unable to lookup object by key.");
        assertEquals(object1, result1Key, "Correct object not retrieved.");

        final TestDbObject result1Unique = testCache.getByUniqueName(object1.getUniqueName());
        assertNotNull(result1Unique, "Unable to lookup object by uniqueName.");
        assertEquals(object1, result1Unique, "Correct object not retrieved.");
        
        final TestDbObject resultSearch = testCache.search(inCache -> inCache.value.compareTo(searchString));
        assertNotNull(resultSearch, "Search returned no results.");
        assertEquals(object2, resultSearch, "Correct object not retrieved.");

        testCache.clear();
        assertEquals(0, testCache.size(), "Cache did not clear correctly.");
    }

    public static class TestDbObject implements CachableDbObject
    {
        final private DbKey key;
        final private String uniqueName;
        final private String value;

        public TestDbObject(DbKey key, String uniqueName, String value)
        {
            this.key = key;
            this.uniqueName = uniqueName;
            this.value = value;
        }

        @Override
        public DbKey getKey()
        {
            return key;
        }

        @Override
        public String getUniqueName()
        {
            return uniqueName;
        }

        public String getValue()
        {
            return value;
        }
        
    }

    public static class SimpleCache<DBT extends CachableDbObject> implements DbObjectCache<DBT>
    {
        Map<CacheKey<DBT>,DBT> cache = new TreeMap<>();

        @Override
        public void put(DBT dbObj)
        {
            CacheKey<DBT> cacheKey = getKey(dbObj.getKey(), dbObj.getUniqueName());
            cache.put(cacheKey, dbObj);

        }

        @Override
        public void remove(DbKey key)
        {
            CacheKey<DBT> cacheKey = getKey(key);
            cache.remove(cacheKey);
        }

        @Override
        public DBT getByKey(DbKey key) 
        {
            CacheKey<DBT> cacheKey = getKey(key);
            return cache.get(cacheKey);
        }

        @Override
        public DBT getByKey(DbKey key, DaoBase daoBase) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getByKey'");
        }

        @Override
        public DBT getByUniqueName(String uniqueName)
        {
            CacheKey<DBT> cacheKey = getKey(uniqueName);
            return cache.get(cacheKey);
        }

        @Override
        public DBT getByUniqueName(String uniqueName, DaoBase daoBase) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getByUniqueName'");
        }

        @Override
        public int size()
        {
            return cache.size();
        }

        @Override
        public DBT search(Comparable<DBT> cmp)
        {
            for (DBT obj: cache.values())
            {
                if (cmp.compareTo(obj) == 0)
                {
                    return obj;
                }
            }
            return null;
        }

        @Override
        public Iterator<DBT> iterator()
        {
            return cache.values().iterator();
        }

        @Override
        public void clear()
        {
            cache.clear();
        }

        @Override
        public void setMaxAge(long maxAge)
        {
        }
    }

}
