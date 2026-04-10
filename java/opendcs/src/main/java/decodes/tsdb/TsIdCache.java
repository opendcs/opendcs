/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb;

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