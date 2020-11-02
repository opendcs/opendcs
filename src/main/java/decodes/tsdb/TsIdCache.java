/**
 * $Id$
 * 
 * $Log$
 * Revision 1.8  2013/07/24 13:43:38  mmaloney
 * Removed debugs.
 *
 * Revision 1.7  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 * Revision 1.6  2012/12/17 16:28:35  mmaloney
 * neverRemove must be static.
 *
 * Revision 1.5  2012/12/14 16:53:56  mmaloney
 * neverRemove must be static.
 *
 * Revision 1.4  2012/12/14 15:52:58  mmaloney
 * add neverRemove option. Needed by CpCompDependsUpdater
 *
 * Revision 1.3  2012/07/09 19:02:11  mmaloney
 * First cut of new daemon to update CP_COMP_DEPENDS.
 *
 * Revision 1.2  2012/07/05 18:27:04  mmaloney
 * tsKey is stored as a long.
 *
 * Revision 1.1  2012/06/18 15:15:39  mmaloney
 * Moved TS ID cache to base class.
 *
 * 
 * This is open-source software written by Cove Software LLC under
 * contract to the federal government. You are free to copy and use this
 * source code for your own purposes, except that no part of the information
 * contained in this file may be claimed to be proprietary.
 *
 * This source code is provided completely without warranty.
 * 
 * $Log$
 * Revision 1.8  2013/07/24 13:43:38  mmaloney
 * Removed debugs.
 *
 * Revision 1.7  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.tsdb;

import ilex.util.Logger;

import java.util.Collection;
import java.util.HashMap;

import decodes.sql.DbKey;

/**
 * Caches Time Series Identifiers for limited amount of time.
 */
public class TsIdCache
{
	private HashMap<DbKey, TimeSeriesIdentifier> key2tsid
		= new HashMap<DbKey, TimeSeriesIdentifier>();
	private HashMap<String, TimeSeriesIdentifier> str2tsid
		= new HashMap<String, TimeSeriesIdentifier>();
	
	public static final long cacheTimeLimit = 45 * 60000L; // 45 min
	public static boolean neverRemove = false;
	
	public TsIdCache()
	{
	}
	
	public void clear()
	{
		key2tsid.clear();
		str2tsid.clear();
	}
	
	public TimeSeriesIdentifier get(DbKey ts_code) 
	{
		TimeSeriesIdentifier ret = key2tsid.get(ts_code);
		long now = System.currentTimeMillis();
		if (!neverRemove && ret != null && now - ret.getReadTime() > cacheTimeLimit)
		{
			remove(ret);
			ret = null;
		}
		return ret;
	}

	public TimeSeriesIdentifier get(String uniqueStr) 
	{
		TimeSeriesIdentifier ret = str2tsid.get(uniqueStr.toUpperCase());
		long now = System.currentTimeMillis();
		if (!neverRemove && ret != null && now - ret.getReadTime() > cacheTimeLimit)
		{
			remove(ret);
			ret = null;
		}
		return ret;
	}
	
	public void remove(TimeSeriesIdentifier tsid)
	{
		key2tsid.remove(tsid.getKey());
		str2tsid.remove(tsid.getUniqueString().toUpperCase());
	}
	
	public void add(TimeSeriesIdentifier tsid)
	{
		tsid.setReadTime(System.currentTimeMillis());
		key2tsid.put(tsid.getKey(), tsid);
		str2tsid.put(tsid.getUniqueString().toUpperCase(), tsid);
	}
	
	public int size()
	{
		return key2tsid.size();
	}
	
	/**
	 * Return a collection of identifiers in the list.
	 * The caller must not modify the returned collection.
	 * Doing so will result in corruption of the cache.
	 * @return
	 */
	public Collection<TimeSeriesIdentifier> getListNoModify()
	{
		return key2tsid.values();
	}

}
