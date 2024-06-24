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

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import decodes.sql.DbKey;

/**
 * Implements a cache for DAOs
 * @author mmaloney Mike Maloney, Cove Software LLC
 *
 */
public class ScheduledReloadDbObjectCache<DBT extends CachableDbObject> implements org.opendcs.database.DbObjectCache<DBT>
{
	private long maxAge = 3600000L; // # ms. If older than this, object is removed from cache.
	private boolean nameIsCaseSensitive = false;
	public boolean testMode = false;
	private final Consumer<ScheduledReloadDbObjectCache<DBT>> reloadFunction;
	
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
	
	private Map<DbKey, ObjWrapper> keyObjMap = new ConcurrentHashMap<DbKey, ObjWrapper>();
	private Map<String, ObjWrapper> nameObjMap = new ConcurrentHashMap<String, ObjWrapper>();
	
	
	/**
	 * Construct an cache to reloads on a schedule.
	 * Schedule is currently hard coded to 1 hour.
	 * @param maxAge max age of object to cause a refresh when accessed after this age.
	 * @param nameIsCaseSensitive
	 * @param reloadFunction callback used to reload the cache.
	 * @param executor executor service to schedule the cache reload on.
	 */
	public ScheduledReloadDbObjectCache(long maxAge, boolean nameIsCaseSensitive, Consumer<ScheduledReloadDbObjectCache<DBT>> reloadFunction, ScheduledExecutorService executor)
	{
		this.maxAge = maxAge;
		this.nameIsCaseSensitive = nameIsCaseSensitive;
		this.reloadFunction = Objects.requireNonNull(reloadFunction, "Cache Reload function must be provided.");
		Objects.requireNonNull(executor, "An executor service must be provided to schedule the task.");
		executor.scheduleAtFixedRate(() -> reloadFunction.accept(this),
									 60 /* to give the database instances time to initialize and have a connection. */,
									 TimeUnit.HOURS.toSeconds(1),
									 TimeUnit.SECONDS);
	}
	
	/**
	 * Place an object in the cache.
	 * @param dbObj the object to cache
	 */
	@Override
	public synchronized void put(DBT dbObj)
	{
		String un = dbObj.getUniqueName();
		if (un == null || un.trim().length() == 0)
			return;

		ObjWrapper ow = new ObjWrapper(dbObj);
		keyObjMap.put(dbObj.getKey(), ow);
		
		nameObjMap.put(nameIsCaseSensitive ? un : un.toUpperCase(), ow);
	}
	
	/**
	 * Removes an object from the cache.
	 *
	 * @param key DbKey of the object to be removed.
	 */
	@Override
	public synchronized void remove(DbKey key)
	{
		ObjWrapper ow = keyObjMap.get(key);
		if (ow == null)
			return;
		try
		{
			nameObjMap.remove(
				nameIsCaseSensitive ? ow.obj.getUniqueName() : ow.obj.getUniqueName().toUpperCase());
		}
		catch(NoSuchElementException ex)
		{
			// another thread already removed it.
		}
		try
		{
			keyObjMap.remove(key);
		}
		catch(NoSuchElementException ex)
		{
			// another thread already removed it.
		}
	}
	
	/**
	 * Retrieve an object by its surrogate database key
	 * @param key the surrogate database key
	 * @return the object
	 */
	@Override
	public DBT getByKey(DbKey key)
	{
		ObjWrapper ow = keyObjMap.get(key);

		if (ow == null)
		{
			return null;
		}
		long now = System.currentTimeMillis();
		if (now - ow.timeLoaded > maxAge)
		{
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
	@Override
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
	@Override
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
	@Override
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

	@Override
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
	@Override
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
	@Override
	public CacheIterator iterator()
	{
		return new CacheIterator(keyObjMap.values().iterator());
	}
	
	/**
	 * Completely clear the cache.
	 */
	@Override
	public void clear()
	{
		keyObjMap.clear();
		nameObjMap.clear();
	}

	@Override
	public void setMaxAge(long maxAge)
	{
		this.maxAge = maxAge;
	}
}
