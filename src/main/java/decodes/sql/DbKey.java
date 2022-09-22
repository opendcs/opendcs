/**
 * $Id$
 * 
 * Open source software
 * @author - Mike Maloney, Cove Software, LLC
 * 
 * $Log$
 * Revision 1.2  2014/12/11 20:28:45  mmaloney
 * Implement serializable for webapp.
 *
 * Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
 * OPENDCS 6.0 Initial Checkin
 *
 * Revision 1.1  2013/03/21 18:27:39  mmaloney
 * DbKey Implementation
 *
 */
package decodes.sql;

import ilex.util.Logger;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;


/**
 * Encapsulates a surrogate key for identifying records in a database.
 * The internal long-integer value is immutable.
 * 
 * Whenever a null result set column or key value of -1L is used in the
 * create methods, a constant NullKey is returned. Thus it is OK to
 * compare a given (key == Constants.undefinedId) to determine if a key
 * is set. This is important so that legacy comparisons will continue to work.
 * 
 * The correct way to determine if two keys are equals is: key1.equals(key2).
 * However, I am concerned that there may be legacy code out there that
 * does (key1 == key2). In order to make these cases work, keys are stored
 * internally so that two calls to createDbKey with the same key value will
 * return the same key object.
 * 
 * The method used to make this happen will work for all but extreme cases.
 * A long-running CWMS daemon process that accumulates more that 200,000 different
 * key values may require that the internal storage be occasionally trimmed.
 * Thus for these processes (key1 == key2) may be false when it should be true.
 * 
 * Recommendation: In new code, always compare keys with key1.equals(key2);
 */
@SuppressWarnings("serial")
public class DbKey
	implements Comparable<DbKey>, Serializable
{
	/** Immutable internal long integer key value */
	private long value;
	
	/** hash code computed on construction for effeciancy */
	private int _hashCode;
	
	/** Used for detecting oldest keys for removal from the hash map when necessary */
	private long createOrder;
	
	/** The one and only null key. All calls to createDbKey with -1 will return this object. */
	public static final DbKey NullKey = new DbKey(-1L);
	
	// Internal hash storage for keys.
	private static HashMap<Long, DbKey> createdKeys = new HashMap<Long, DbKey>();
	private static long createCounter = 0L;
	private static final int MaxHashSize = 200000;
	
	/** Factory method to create a key from a result set column */
	public static DbKey createDbKey(ResultSet rs, int column)
		throws SQLException
	{
		long keyValue = rs.getLong(column);
		if (rs.wasNull())
			return NullKey;

		return createDbKey(keyValue);
	}

	/**
	 * Create key using the string name of a column
	 * @param rs a valid result set
	 * @param column name of the column
	 * @return a valid DbKey object
	 * @throws SQLException column type is not Long or not convertable to long
	 */
	public static DbKey createDbKey(ResultSet rs, String column)
		throws SQLException
	{
		long keyValue = rs.getLong(column);
		if (rs.wasNull())
			return NullKey;

		return createDbKey(keyValue);
	}

	/** Factory method to create a key from a long integer key value */
	public static synchronized DbKey createDbKey(long keyValue)
	{
		// Special value for null key, always the same.
		if (keyValue == -1L)
			return NullKey;

		// See if this key value is already created.
		Long kv = new Long(keyValue);
		DbKey ret = createdKeys.get(kv);
		if (ret != null)
		{
			return ret;
		}
		// Not in key hash. Have to create new key.
		if (createdKeys.size() >= MaxHashSize)
			trimHash();
		
		ret = new DbKey(kv);
		ret.createOrder = createCounter++;
		createdKeys.put(kv, ret);
		return ret;
	}
	
	/** @return the internal immutable long integer key value */
	public long getValue() { return value; }
	
	/** @return true if this is considered a null key */
	public boolean isNull() { return value == -1L; }
	
	/** 
	 * @return the numeric key as a string, or the word "null" if the key is undefined.
	 */
	public String toString()
	{
		return isNull() ? "null" : ("" + value);
	}
	
	/** @return true if this key has the same internal value as the right-hand-side key */
	public boolean equals(Object rhs)
	{
		if (rhs == null)
			return false;
		if (rhs == this)
			return true;
		if (!(rhs instanceof DbKey))
			return false;
		DbKey rhk = (DbKey)rhs;
		return value == rhk.value;
	}
	
	/** @return hash code for this key */
	public int hashCode()
	{
		return _hashCode;
	}

	@Override
	public int compareTo(DbKey rhs)
	{
		long x = this.value - rhs.value;
		return x < 0 ? -1 : x > 0 ? 1 : 0;
	}

	/**
	 * Hopefully this method never gets called. But for CWMS (which has
	 * myriad keys) with a long-running daemon, it is possible that the
	 * hash will accumulate more than MaxHashSize keys. When this happens
	 * we remove the oldest half.
	 * The danger is that an existing key will be recreated and thus
	 * key1 == key2 may be false when it should be true.
	 */
	private static void trimHash()
	{
		Logger.instance().warning("TRIMMING THE DBKEY HASH size="
			+ createdKeys.size() + ", createCounter=" + createCounter);
		long removeBefore = createCounter - (MaxHashSize/2);
		for(Iterator<Entry<Long, DbKey>> ckit = createdKeys.entrySet().iterator(); 
			ckit.hasNext(); )
		{
			Entry<Long, DbKey> pair = ckit.next();
			if (pair.getValue().createOrder < removeBefore)
				ckit.remove();
		}
	}

	private DbKey(Long keyValue)
	{
		this.value = keyValue.longValue();
		_hashCode = keyValue.hashCode();
	}
	
	
	private void setValue(long v) 
	{
		// Never implement this method!!! Keys must be immutable!!!
		// Changing key values will destroy the createdKeys hash and probably
		// cause lots of code to break in unpredictable ways.
		// If you need to change some object's (e.g. a Platform's) key, then
		// call its setId method with a newly created key.
		// Really. I mean it. Don't implement this method.
		// Okay???
		throw new IllegalArgumentException("Broken Java Code. Somebody tried to change DbKey!!!");
	}

	public static boolean isNull(DbKey key)
	{
		return key == null || key.isNull();
	}
}
