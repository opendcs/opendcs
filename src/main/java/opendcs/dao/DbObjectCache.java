/**
 * $Id$
 * 
 * $Log$
 * Revision 1.4  2015/07/17 13:18:38  mmaloney
 * Don't allow blank unique name in cache.
 *
 * Revision 1.3  2015/04/14 18:24:42  mmaloney
 * Improved comments.
 *
 * Revision 1.2  2014/07/03 12:53:40  mmaloney
 * debug improvements.
 *
 * 
 * This software was written by Cove Software, LLC ("COVE") under contract
 * to the United States Government. No warranty is provided or implied other 
 * than specific contractual terms between COVE and the U.S. Government.
 *
 * Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
 */
package opendcs.dao;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import ilex.util.Logger;
import decodes.sql.DbKey;

/**
 * Implements a cache for DAOs
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 * @param <DBT> type type of database object to be cached
 * @param <HasUniqueName> 
 */
public class DbObjectCache<DBT extends CachableDbObject>
{
	private long maxAge = 3600000L; // # ms. If older than this, object is removed from cache.
	private boolean nameIsCaseSensitive = false;
	public boolean testMode = false;
	
	class ObjWrapper
	{
		DBT obj = null;
		long timeLoaded = 0L;
		
		ObjWrapper(DBT obj)
		{
			this.obj = obj;
			timeLoaded = System.currentTimeMillis();
		}
	}
	
	private HashMap<DbKey, ObjWrapper> keyObjMap = new HashMap<DbKey, ObjWrapper>();
	private HashMap<String, ObjWrapper> nameObjMap = new HashMap<String, ObjWrapper>();
	
	/**
	 * Constructor.
	 * @param maxAge number of milliseconds. Objects older than this are removed from cache on retrieval.
	 * @param nameIsCaseSensitive set to false to ignore case on the unique names
	 */
	public DbObjectCache(long maxAge, boolean nameIsCaseSensitive)
	{
		this.maxAge = maxAge;
		this.nameIsCaseSensitive = nameIsCaseSensitive;
	}
	
	/**
	 * Place an object in the cache.
	 * @param dbObj the object to cache
	 */
	public void put(DBT dbObj)
	{
		String un = dbObj.getUniqueName();
		if (un == null || un.trim().length() == 0)
			return;
//if (testMode) Logger.instance().info("Adding key=" + dbObj.getKey() + " name=" + dbObj.getUniqueName() +
//" to map with " + keyObjMap.size() + " elements.");
		ObjWrapper ow = new ObjWrapper(dbObj);
		keyObjMap.put(dbObj.getKey(), ow);
		
		nameObjMap.put(nameIsCaseSensitive ? un : un.toUpperCase(), ow);
	}
	
	/**
	 * Removes an object from the cache
	 * @param dbObj the object to remove.
	 */
	public void remove(DbKey key)
	{
		ObjWrapper ow = keyObjMap.get(key);
		if (ow == null)
			return;
		nameObjMap.remove(
			nameIsCaseSensitive ? ow.obj.getUniqueName() : ow.obj.getUniqueName().toUpperCase());
		keyObjMap.remove(key);
	}
	
	/**
	 * Retrieve an object by its surrogate database key
	 * @param key the surrogate database key
	 * @return the object
	 */
	public DBT getByKey(DbKey key)
	{
		ObjWrapper ow = keyObjMap.get(key);

		if (ow == null)
		{
//if (testMode) Logger.instance().info("getByKey(" + key + ") object not in map.");
			return null;
		}
//if (testMode) Logger.instance().info("getByKey(" + key + ") object in map with timeLoaded = " + ow.timeLoaded); 
		long now = System.currentTimeMillis();
		if (now - ow.timeLoaded > maxAge)
		{
//if (testMode) Logger.instance().info("getByKey(" + key + ") maxAge " + maxAge + " exceeded age="
//+ (now - ow.timeLoaded));
			remove(key);
			return null;
		}
		return ow.obj;
	}
	
	/**
	 * Used by caches like in ComputationDAO that must also check a last modify
	 * time in the database to determine if a cached object needs to be reloaded.
	 * Before returning an object from the cache, the dao's check method is consulted.
	 * @param key The key of the object
	 * @param daoBase the DAO
	 * @return the object or null if cached object doesn't exist or is not OK.
	 */
	public DBT getByKey(DbKey key, DaoBase daoBase)
	{
		DBT ret = getByKey(key);
		if (ret != null && !daoBase.checkCachedObjectOK(ret))
		{
			remove(key);
			ret = null;
		}
		return ret;
	}

	/**
	 * Retrieve an object by its unique name
	 * @param uniqueName the unique name
	 * @return the object
	 */
	public DBT getByUniqueName(String uniqueName)
	{
		ObjWrapper ow = nameObjMap.get(
			nameIsCaseSensitive ? uniqueName : uniqueName.toUpperCase());
		if (ow == null)
			return null;
		if (System.currentTimeMillis() - ow.timeLoaded > maxAge)
		{
			remove(ow.obj.getKey());
			return null;
		}
		return ow.obj;
	}
	
	/**
	 * Used by caches like in ComputationDAO that must also check a last modify
	 * time in the database to determine if a cached object needs to be reloaded.
	 * Before returning an object from the cache, the dao's check method is consulted.
	 * @param uniqueName The unique name of the object
	 * @param daoBase the DAO
	 * @return the object or null if cached object doesn't exist or is not OK.
	 */
	public DBT getByUniqueName(String uniqueName, DaoBase daoBase)
	{
		DBT ret = getByUniqueName(uniqueName);
		if (ret != null && !daoBase.checkCachedObjectOK(ret))
		{
			remove(ret.getKey());
			ret = null;
		}
		return ret;
	}

	public int size()
	{
		return keyObjMap.size();
	}

	/**
	 * Searches the cache for an object that is 'equal to' the passed comparator.
	 * This method does a linear search of the objects in the cache, so it is not
	 * necessary that the Comparable be consistent in a sorting sense.
	 * @param cmp The comparator
	 * @return a matching object, or null if non is found.
	 */
	public DBT search(Comparable<DBT> cmp)
	{
		for(ObjWrapper ow : keyObjMap.values())
			if (cmp.compareTo(ow.obj) == 0)
				return ow.obj;
		return null;
	}
	
	public class CacheIterator implements Iterator<DBT>
	{
		Iterator<ObjWrapper> wrapperIterator;
		
		CacheIterator(Iterator<ObjWrapper> wrapperIterator)
		{
			this.wrapperIterator = wrapperIterator;
		}
		
		@Override
		public boolean hasNext()
		{
			return wrapperIterator.hasNext();
		}

		@Override
		public DBT next()
		{
			return wrapperIterator.next().obj;
		}

		@Override
		public void remove()
		{
			wrapperIterator.remove();
		}
	}
	
	/**
	 * @return iterator into the list of cached values.
	 */
	public CacheIterator iterator()
	{
		return new CacheIterator(keyObjMap.values().iterator());
	}
	
	/**
	 * Completely clear the cache.
	 */
	public void clear()
	{
		keyObjMap.clear();
		nameObjMap.clear();
	}

	public void setMaxAge(long maxAge)
	{
		this.maxAge = maxAge;
	}
}
