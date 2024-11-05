package decodes.db;

import java.util.ArrayList;

import decodes.sql.DbKey;
import opendcs.opentsdb.Interval;

/**
 * Encapsulates a list of Interval objects stored in the time series 
 * database. It extends DatabaseObject so that it can be imported/
 * exported via the normal XML framework.
 * 
 * @author mmaloney Mike Maloney, Cove Software, LLC
 */
public class IntervalList extends DatabaseObject
{
	private ArrayList<Interval> intervals = new ArrayList<Interval>();
	
	/** The singleton instance used by computation and time series apps. */
	private static IntervalList _instance = null;

	/** Separate instance used for XML import and editing apps. */
	private static IntervalList _editInstance = null;
	
	/**
	 * Most apps will use the singleton instance. However the constructor
	 * is public to enable some apps, like import program and editor to hold
	 * a temporary instance.
	 */
	private IntervalList()
	{
		super();
	}
	
	/**
	 * @return the singleton IntervalList. Most apps will use this.
	 */
	public static IntervalList instance()
	{
		if (_instance == null)
			_instance = new IntervalList();
		return _instance;
	}

	/**
	 * @return the singleton IntervalList. Most apps will use this.
	 */
	public static IntervalList editInstance()
	{
		if (_editInstance == null)
			_editInstance = new IntervalList();
		return _editInstance;
	}

	public void add(Interval interval)
	{
		for(Interval existing : intervals)
			if (existing.getName().equalsIgnoreCase(interval.getName())
			 || (!interval.getKey().isNull()
			  && interval.getKey().equals(existing.getKey())))
			{
				existing.setKey(interval.getKey());
				existing.setName(interval.getName());
				existing.setCalConstant(interval.getCalConstant());
				existing.setCalMultiplier(interval.getCalMultiplier());
				return;
			}
		intervals.add(interval);
	}
	
	public ArrayList<Interval> getList() { return intervals; }
	
	/**
	 * Return the interval in the list by its database surrogate key
	 * @param key the surrogate key
	 * @return the matching interval, or null if no match in list.
	 */
	public Interval getById(DbKey key)
	{
		for (Interval intv : intervals)
			if (key.equals(intv.getKey()))
				return intv;
		return null;
	}
	
	/**
	 * Return the interval in the list by its unique name.
	 * A case INsensitive compare is done.
	 * @param name the interval name.
	 * @return the matching interval, or null if no match in list.
	 */
	public Interval getByName(String name)
	{
		for (Interval intv : intervals)
			if (name.equalsIgnoreCase(intv.getName()))
				return intv;
		return null;
	}
	
	@Override
	public String getObjectType()
	{
		return "Interval";
	}

	@Override
	public void prepareForExec() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
		// Nothing to do
	}

	@Override
	public boolean isPrepared()
	{
		return true;
	}

	@Override
	public void validate() throws IncompleteDatabaseException,
		InvalidDatabaseException
	{
		// Nothing to do
	}

	@Override
	public void read() throws DatabaseException
	{
		// IntervalList cannot read itself.
	}

	@Override
	public void write() throws DatabaseException
	{
		// IntervalList cannot write itself.
	}

}
